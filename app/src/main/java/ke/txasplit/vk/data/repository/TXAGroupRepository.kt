/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAGroupRepository.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ke.txasplit.vk.data.dao.TXAGroupDao
import ke.txasplit.vk.data.dao.TXAMemberDao
import ke.txasplit.vk.data.entity.TXAGroupEntity
import ke.txasplit.vk.data.entity.TXAMemberEntity
import ke.txasplit.vk.data.TXAMemberRole
import ke.txasplit.vk.data.TXAMemberRoleValidator
import ke.txasplit.vk.data.GroupRoleStats
import ke.txasplit.vk.data.ValidationResult
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository implementing business logic for group management
 * Handles validation and coordination between Group and Member entities
 */
@Singleton
class TXAGroupRepository @Inject constructor(
    private val groupDao: TXAGroupDao,
    private val memberDao: TXAMemberDao
) {
    
    /**
     * Create a new group with the user as host
     * @param userId User ID who will be the host
     * @param name Group name
     * @param description Group description
     * @return Result containing the created group or error
     */
    suspend fun createGroup(
        userId: String,
        name: String,
        description: String = ""
    ): Result<TXAGroupEntity> {
        return try {
            val groupId = UUID.randomUUID().toString()
            val inviteCode = generateInviteCode()
            
            // Check if invite code already exists (very rare collision)
            if (groupDao.inviteCodeExists(inviteCode)) {
                return createGroup(userId, name, description) // Recursive retry with new code
            }
            
            val group = TXAGroupEntity(
                id = groupId,
                name = name,
                inviteCode = inviteCode,
                createdBy = userId,
                hostId = userId,
                description = description
            )
            
            val groupIdResult = groupDao.insertGroup(group)
            
            // Add creator as host member
            val hostMember = TXAMemberEntity(
                id = UUID.randomUUID().toString(),
                groupId = groupId,
                userId = userId,
                role = TXAMemberRole.HOST,
                displayName = "Host" // Can be updated later
            )
            
            memberDao.insertMember(hostMember)
            
            Result.success(group.copy(id = groupId)) // Return with actual ID
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Add a member to a group with validation
     */
    suspend fun addMember(
        groupId: String,
        userId: String,
        displayName: String,
        role: TXAMemberRole = TXAMemberRole.MEMBER
    ): Result<TXAMemberEntity> {
        return try {
            // Check if user is already in group
            if (memberDao.isUserInGroup(userId, groupId)) {
                return Result.failure(IllegalArgumentException("User is already a member of this group"))
            }
            
            // Get current group stats for validation
            val stats = getGroupRoleStats(groupId)
            
            // Validate role assignment
            val validation = TXAMemberRoleValidator.validateRoleChange(
                currentRole = TXAMemberRole.MEMBER, // New member starts as Member conceptually
                newRole = role,
                groupStats = stats
            )
            
            if (validation is ValidationResult.Error) {
                return Result.failure(IllegalArgumentException(validation.message))
            }
            
            val member = TXAMemberEntity(
                id = UUID.randomUUID().toString(),
                groupId = groupId,
                userId = userId,
                role = role,
                displayName = displayName
            )
            
            val memberId = memberDao.insertMember(member)
            Result.success(member.copy(id = memberId.toString()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Change member role with validation
     */
    suspend fun changeMemberRole(
        memberId: String,
        newRole: TXAMemberRole,
        requestedBy: String
    ): Result<TXAMemberEntity> {
        return try {
            val member = memberDao.getMemberById(memberId)
                ?: return Result.failure(IllegalArgumentException("Member not found"))
            
            // Check if requester has permission (only Host can change roles)
            val requesterRole = memberDao.getUserRoleInGroup(requestedBy, member.groupId)
            if (requesterRole != TXAMemberRole.HOST) {
                return Result.failure(IllegalArgumentException("Only Host can change roles"))
            }
            
            // Get current group stats for validation
            val stats = getGroupRoleStats(member.groupId)
            
            // Validate role change
            val validation = TXAMemberRoleValidator.validateRoleChange(
                currentRole = member.role,
                newRole = newRole,
                groupStats = stats
            )
            
            if (validation is ValidationResult.Error) {
                return Result.failure(IllegalArgumentException(validation.message))
            }
            
            // Update member role
            memberDao.updateMemberRole(memberId, newRole)
            
            // If changing to Host, update group host
            if (newRole == TXAMemberRole.HOST) {
                groupDao.updateHost(member.groupId, member.userId)
            }
            
            Result.success(member.copy(role = newRole))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Remove a member from a group with validation
     */
    suspend fun removeMember(
        memberId: String,
        requestedBy: String
    ): Result<Unit> {
        return try {
            val member = memberDao.getMemberById(memberId)
                ?: return Result.failure(IllegalArgumentException("Member not found"))
            
            // Check if requester has permission
            val requesterRole = memberDao.getUserRoleInGroup(requestedBy, member.groupId)
            val canRemove = requesterRole == TXAMemberRole.HOST || 
                           (requesterRole == TXAMemberRole.CO_HOST && member.role == TXAMemberRole.MEMBER)
            
            if (!canRemove) {
                return Result.failure(IllegalArgumentException("Insufficient permissions to remove member"))
            }
            
            // Get current group stats for validation
            val stats = getGroupRoleStats(member.groupId)
            
            // Validate member removal
            val validation = TXAMemberRoleValidator.validateMemberRemoval(
                memberRole = member.role,
                groupStats = stats
            )
            
            if (validation is ValidationResult.Error) {
                return Result.failure(IllegalArgumentException(validation.message))
            }
            
            // Deactivate member (soft delete)
            memberDao.setMemberActive(memberId, false)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Join group using invite code
     */
    suspend fun joinGroupByInviteCode(
        inviteCode: String,
        userId: String,
        displayName: String
    ): Result<TXAGroupEntity> {
        return try {
            val group = groupDao.getGroupByInviteCode(inviteCode)
                ?: return Result.failure(IllegalArgumentException("Invalid invite code"))
            
            // Check if user is already in group
            if (memberDao.isUserInGroup(userId, group.id)) {
                return Result.failure(IllegalArgumentException("You are already a member of this group"))
            }
            
            // Add user as member
            addMember(group.id, userId, displayName, TXAMemberRole.MEMBER)
            
            Result.success(group)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update group bank information
     */
    suspend fun updateBankInfo(
        groupId: String,
        bankInfo: String,
        requestedBy: String
    ): Result<Unit> {
        return try {
            // Check if requester has permission (Host or Co-host)
            val requesterRole = memberDao.getUserRoleInGroup(requestedBy, groupId)
            if (requesterRole != TXAMemberRole.HOST && requesterRole != TXAMemberRole.CO_HOST) {
                return Result.failure(IllegalArgumentException("Only Host or Co-host can update bank information"))
            }
            
            groupDao.updateBankInfo(groupId, bankInfo)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get groups for user
     */
    fun getGroupsForUserFlow(userId: String): Flow<List<TXAGroupEntity>> {
        return groupDao.getGroupsForUserFlow(userId)
    }
    
    /**
     * Get members in group
     */
    fun getMembersInGroupFlow(groupId: String): Flow<List<TXAMemberEntity>> {
        return memberDao.getMembersInGroupFlow(groupId)
    }
    
    /**
     * Get group by ID
     */
    suspend fun getGroupById(groupId: String): TXAGroupEntity? {
        return groupDao.getGroupById(groupId)
    }
    
    /**
     * Get user's role in group
     */
    suspend fun getUserRoleInGroup(userId: String, groupId: String): TXAMemberRole? {
        return memberDao.getUserRoleInGroup(userId, groupId)
    }
    
    /**
     * Get group role statistics for validation
     */
    private suspend fun getGroupRoleStats(groupId: String): GroupRoleStats {
        val stats = memberDao.getMemberStats(groupId)
        return if (stats != null) {
            GroupRoleStats(
                totalMembers = stats.total,
                hostCount = stats.hostCount,
                coHostCount = stats.coHostCount,
                memberCount = stats.memberCount
            )
        } else {
            GroupRoleStats(0, 0, 0, 0)
        }
    }
    
    /**
     * Generate unique 4-character invite code
     * Format: TXAS-XXXX where X is alphanumeric
     */
    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val randomPart = (1..4).map { chars.random() }.joinToString("")
        return "TXAS-$randomPart"
    }
}
