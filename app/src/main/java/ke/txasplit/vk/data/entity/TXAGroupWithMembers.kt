/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAGroupWithMembers.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.data.entity

import androidx.room.Embedded
import androidx.room.Relation
import ke.txasplit.vk.data.TXAMemberRole
import kotlinx.serialization.Serializable

/**
 * Data class representing a group with all its members
 * Used for UI consumption with Room @Relation for efficient queries
 */
@Serializable
data class TXAGroupWithMembers(
    /**
     * The group entity
     */
    @Embedded
    val group: TXAGroupEntity,
    
    /**
     * List of all members in this group (active only)
     * Ordered by join date (earliest first)
     */
    @Relation(
        parentColumn = "id",
        entityColumn = "groupId",
        entity = TXAMemberEntity::class
    )
    val members: List<TXAMemberEntity>
) {
    /**
     * Get the host member
     * @return Host member or null if not found
     */
    fun getHost(): TXAMemberEntity? {
        return members.find { it.role == TXAMemberRole.HOST }
    }
    
    /**
     * Get all co-hosts
     * @return List of co-host members
     */
    fun getCoHosts(): List<TXAMemberEntity> {
        return members.filter { it.role == TXAMemberRole.CO_HOST }
    }
    
    /**
     * Get all regular members
     * @return List of regular members
     */
    fun getRegularMembers(): List<TXAMemberEntity> {
        return members.filter { it.role == TXAMemberRole.MEMBER }
    }
    
    /**
     * Get total member count
     */
    fun getMemberCount(): Int = members.size
    
    /**
     * Check if a specific user is a member
     * @param userId User ID to check
     * @return True if user is a member, false otherwise
     */
    fun isUserMember(userId: String): Boolean {
        return members.any { it.userId == userId }
    }
    
    /**
     * Get user's role in this group
     * @param userId User ID to check
     * @return User's role or null if not a member
     */
    fun getUserRole(userId: String): TXAMemberRole? {
        return members.find { it.userId == userId }?.role
    }
    
    /**
     * Check if user is host
     * @param userId User ID to check
     * @return True if user is host, false otherwise
     */
    fun isUserHost(userId: String): Boolean {
        return getUserRole(userId) == TXAMemberRole.HOST
    }
    
    /**
     * Check if user is co-host
     * @param userId User ID to check
     * @return True if user is co-host, false otherwise
     */
    fun isUserCoHost(userId: String): Boolean {
        return getUserRole(userId) == TXAMemberRole.CO_HOST
    }
    
    /**
     * Check if user can manage group settings
     * @param userId User ID to check
     * @return True if user can manage group, false otherwise
     */
    fun canUserManageGroup(userId: String): Boolean {
        return isUserHost(userId)
    }
    
    /**
     * Check if user can manage bills
     * @param userId User ID to check
     * @return True if user can manage bills, false otherwise
     */
    fun canUserManageBills(userId: String): Boolean {
        val role = getUserRole(userId)
        return role == TXAMemberRole.HOST || role == TXAMemberRole.CO_HOST
    }
    
    /**
     * Check if user can remove members
     * @param userId User ID to check
     * @return True if user can remove members, false otherwise
     */
    fun canUserRemoveMembers(userId: String): Boolean {
        return isUserHost(userId)
    }
    
    /**
     * Check if group has bank information configured
     */
    fun hasBankInfo(): Boolean {
        return group.hasBankInfo()
    }
    
    /**
     * Get member display name
     * @param userId User ID
     * @return Display name or user ID if not found
     */
    fun getMemberDisplayName(userId: String): String {
        return members.find { it.userId == userId }?.displayName ?: userId
    }
    
    /**
     * Get group statistics
     * @return GroupStats object with member counts
     */
    fun getGroupStats(): GroupStats {
        return GroupStats(
            totalMembers = members.size,
            hostCount = if (getHost() != null) 1 else 0,
            coHostCount = getCoHosts().size,
            memberCount = getRegularMembers().size
        )
    }
}

/**
 * Statistics about group members
 */
@Serializable
data class GroupStats(
    val totalMembers: Int,
    val hostCount: Int,
    val coHostCount: Int,
    val memberCount: Int
) {
    /**
     * Check if group has minimum oversight (at least 1 member when total >= 2)
     */
    fun hasOversight(): Boolean {
        return totalMembers < 2 || memberCount >= 1
    }
    
    /**
     * Check if group can add more co-hosts
     */
    fun canAddMoreCoHosts(): Boolean {
        return coHostCount < 3
    }
}
