/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAMemberDao.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ke.txasplit.vk.data.entity.TXAMemberEntity
import ke.txasplit.vk.data.TXAMemberRole

/**
 * Data Access Object for TXAMemberEntity operations
 * Provides CRUD operations and queries for member management
 */
@Dao
interface TXAMemberDao {
    
    /**
     * Insert a new member
     * @return The row ID of the inserted member
     */
    @Insert
    suspend fun insertMember(member: TXAMemberEntity): Long
    
    /**
     * Insert multiple members
     */
    @Insert
    suspend fun insertMembers(members: List<TXAMemberEntity>): List<Long>
    
    /**
     * Update an existing member
     * @return Number of rows affected
     */
    @Update
    suspend fun updateMember(member: TXAMemberEntity): Int
    
    /**
     * Delete a member
     * @return Number of rows affected
     */
    @Delete
    suspend fun deleteMember(member: TXAMemberEntity): Int
    
    /**
     * Delete member by ID
     * @return Number of rows affected
     */
    @Query("DELETE FROM txa_members WHERE id = :memberId")
    suspend fun deleteMemberById(memberId: String): Int
    
    /**
     * Get member by ID
     * @return The member entity or null if not found
     */
    @Query("SELECT * FROM txa_members WHERE id = :memberId")
    suspend fun getMemberById(memberId: String): TXAMemberEntity?
    
    /**
     * Get member by user ID and group ID
     * @return The member entity or null if not found
     */
    @Query("SELECT * FROM txa_members WHERE userId = :userId AND groupId = :groupId")
    suspend fun getMemberByUserAndGroup(userId: String, groupId: String): TXAMemberEntity?
    
    /**
     * Get all members in a group
     */
    @Query("SELECT * FROM txa_members WHERE groupId = :groupId AND isActive = 1 ORDER BY joinedAt ASC")
    suspend fun getMembersInGroup(groupId: String): List<TXAMemberEntity>
    
    /**
     * Get Flow of members in a group (for reactive UI)
     */
    @Query("SELECT * FROM txa_members WHERE groupId = :groupId AND isActive = 1 ORDER BY joinedAt ASC")
    fun getMembersInGroupFlow(groupId: String): Flow<List<TXAMemberEntity>>
    
    /**
     * Get all members (including inactive) in a group
     */
    @Query("SELECT * FROM txa_members WHERE groupId = :groupId ORDER BY joinedAt ASC")
    suspend fun getAllMembersInGroup(groupId: String): List<TXAMemberEntity>
    
    /**
     * Get members by role in a group
     */
    @Query("SELECT * FROM txa_members WHERE groupId = :groupId AND role = :role AND isActive = 1 ORDER BY joinedAt ASC")
    suspend fun getMembersByRole(groupId: String, role: TXAMemberRole): List<TXAMemberEntity>
    
    /**
     * Get host of a group
     * @return The host member or null if not found
     */
    @Query("SELECT m.* FROM txa_members m INNER JOIN txa_groups g ON m.groupId = g.id WHERE g.id = :groupId AND g.hostId = m.userId AND m.isActive = 1")
    suspend fun getGroupHost(groupId: String): TXAMemberEntity?
    
    /**
     * Get co-hosts in a group
     */
    @Query("SELECT * FROM txa_members WHERE groupId = :groupId AND role = 'CO_HOST' AND isActive = 1 ORDER BY joinedAt ASC")
    suspend fun getCoHostsInGroup(groupId: String): List<TXAMemberEntity>
    
    /**
     * Get regular members in a group
     */
    @Query("SELECT * FROM txa_members WHERE groupId = :groupId AND role = 'MEMBER' AND isActive = 1 ORDER BY joinedAt ASC")
    suspend fun getMembersInGroupOnly(groupId: String): List<TXAMemberEntity>
    
    /**
     * Update member role
     * @return Number of rows affected
     */
    @Query("UPDATE txa_members SET role = :role, lastActiveAt = :lastActiveAt WHERE id = :memberId")
    suspend fun updateMemberRole(memberId: String, role: TXAMemberRole, lastActiveAt: Long = System.currentTimeMillis()): Int
    
    /**
     * Update member display name
     * @return Number of rows affected
     */
    @Query("UPDATE txa_members SET displayName = :displayName, lastActiveAt = :lastActiveAt WHERE id = :memberId")
    suspend fun updateMemberDisplayName(memberId: String, displayName: String, lastActiveAt: Long = System.currentTimeMillis()): Int
    
    /**
     * Activate/deactivate a member
     * @return Number of rows affected
     */
    @Query("UPDATE txa_members SET isActive = :isActive, lastActiveAt = :lastActiveAt WHERE id = :memberId")
    suspend fun setMemberActive(memberId: String, isActive: Boolean, lastActiveAt: Long = System.currentTimeMillis()): Int
    
    /**
     * Update last activity timestamp
     * @return Number of rows affected
     */
    @Query("UPDATE txa_members SET lastActiveAt = :lastActiveAt WHERE id = :memberId")
    suspend fun updateLastActive(memberId: String, lastActiveAt: Long = System.currentTimeMillis()): Int
    
    /**
     * Count members in a group
     */
    @Query("SELECT COUNT(*) FROM txa_members WHERE groupId = :groupId AND isActive = 1")
    suspend fun getMemberCount(groupId: String): Int
    
    /**
     * Count members by role in a group
     */
    @Query("SELECT COUNT(*) FROM txa_members WHERE groupId = :groupId AND role = :role AND isActive = 1")
    suspend fun getMemberCountByRole(groupId: String, role: TXAMemberRole): Int
    
    /**
     * Check if user is member of group
     */
    @Query("SELECT EXISTS(SELECT 1 FROM txa_members WHERE userId = :userId AND groupId = :groupId AND isActive = 1)")
    suspend fun isUserInGroup(userId: String, groupId: String): Boolean
    
    /**
     * Check if user is host of group
     */
    @Query("SELECT EXISTS(SELECT 1 FROM txa_members m INNER JOIN txa_groups g ON m.groupId = g.id WHERE m.userId = :userId AND g.id = :groupId AND g.hostId = m.userId AND m.isActive = 1)")
    suspend fun isUserHostOfGroup(userId: String, groupId: String): Boolean
    
    /**
     * Get user's role in group
     * @return The user's role or null if not a member
     */
    @Query("SELECT role FROM txa_members WHERE userId = :userId AND groupId = :groupId AND isActive = 1")
    suspend fun getUserRoleInGroup(userId: String, groupId: String): TXAMemberRole?
    
    /**
     * Get all groups for a user (simplified version)
     */
    @Query("SELECT groupId FROM txa_members WHERE userId = :userId AND isActive = 1")
    suspend fun getGroupIdsForUser(userId: String): List<String>
    
    /**
     * Search members by display name in a group
     */
    @Query("SELECT * FROM txa_members WHERE groupId = :groupId AND isActive = 1 AND displayName LIKE '%' || :query || '%' ORDER BY displayName ASC")
    suspend fun searchMembersInGroup(groupId: String, query: String): List<TXAMemberEntity>
    
    /**
     * Remove all members from a group (when group is deleted)
     * @return Number of rows affected
     */
    @Query("DELETE FROM txa_members WHERE groupId = :groupId")
    suspend fun removeAllMembersFromGroup(groupId: String): Int
    
    /**
     * Get member statistics for a group
     * Returns counts for each role
     */
    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN role = 'HOST' THEN 1 ELSE 0 END) as hostCount,
            SUM(CASE WHEN role = 'CO_HOST' THEN 1 ELSE 0 END) as coHostCount,
            SUM(CASE WHEN role = 'MEMBER' THEN 1 ELSE 0 END) as memberCount
        FROM txa_members 
        WHERE groupId = :groupId AND isActive = 1
    """)
    suspend fun getMemberStats(groupId: String): MemberStats?
}

/**
 * Data class for member statistics
 */
data class MemberStats(
    val total: Int,
    val hostCount: Int,
    val coHostCount: Int,
    val memberCount: Int
)
