/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAGroupDao.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ke.txasplit.vk.data.entity.TXAGroupEntity

/**
 * Data Access Object for TXAGroupEntity operations
 * Provides CRUD operations and queries for group management
 */
@Dao
interface TXAGroupDao {
    
    /**
     * Insert a new group
     * @return The row ID of the inserted group
     */
    @Insert
    suspend fun insertGroup(group: TXAGroupEntity): Long
    
    /**
     * Insert multiple groups
     */
    @Insert
    suspend fun insertGroups(groups: List<TXAGroupEntity>): List<Long>
    
    /**
     * Update an existing group
     * @return Number of rows affected
     */
    @Update
    suspend fun updateGroup(group: TXAGroupEntity): Int
    
    /**
     * Delete a group
     * @return Number of rows affected
     */
    @Delete
    suspend fun deleteGroup(group: TXAGroupEntity): Int
    
    /**
     * Delete group by ID
     * @return Number of rows affected
     */
    @Query("DELETE FROM txa_groups WHERE id = :groupId")
    suspend fun deleteGroupById(groupId: String): Int
    
    /**
     * Get group by ID
     * @return The group entity or null if not found
     */
    @Query("SELECT * FROM txa_groups WHERE id = :groupId")
    suspend fun getGroupById(groupId: String): TXAGroupEntity?
    
    /**
     * Get group by invite code
     * @return The group entity or null if not found
     */
    @Query("SELECT * FROM txa_groups WHERE inviteCode = :inviteCode")
    suspend fun getGroupByInviteCode(inviteCode: String): TXAGroupEntity?
    
    /**
     * Check if invite code exists
     * @return True if invite code exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM txa_groups WHERE inviteCode = :inviteCode)")
    suspend fun inviteCodeExists(inviteCode: String): Boolean
    
    /**
     * Get all groups for a specific user (where user is a member)
     * This requires joining with members table - will be handled in Repository
     */
    @Query("SELECT g.* FROM txa_groups g INNER JOIN txa_members m ON g.id = m.groupId WHERE m.userId = :userId AND m.isActive = 1 AND g.isActive = 1 ORDER BY g.updatedAt DESC")
    suspend fun getGroupsForUser(userId: String): List<TXAGroupEntity>
    
    /**
     * Get Flow of groups for a user (for reactive UI)
     */
    @Query("SELECT g.* FROM txa_groups g INNER JOIN txa_members m ON g.id = m.groupId WHERE m.userId = :userId AND m.isActive = 1 AND g.isActive = 1 ORDER BY g.updatedAt DESC")
    fun getGroupsForUserFlow(userId: String): Flow<List<TXAGroupEntity>>
    
    /**
     * Get all active groups
     */
    @Query("SELECT * FROM txa_groups WHERE isActive = 1 ORDER BY updatedAt DESC")
    suspend fun getAllActiveGroups(): List<TXAGroupEntity>
    
    /**
     * Get Flow of all active groups
     */
    @Query("SELECT * FROM txa_groups WHERE isActive = 1 ORDER BY updatedAt DESC")
    fun getAllActiveGroupsFlow(): Flow<List<TXAGroupEntity>>
    
    /**
     * Get groups created by a specific user
     */
    @Query("SELECT * FROM txa_groups WHERE createdBy = :userId ORDER BY createdAt DESC")
    suspend fun getGroupsCreatedByUser(userId: String): List<TXAGroupEntity>
    
    /**
     * Get groups where user is the host
     */
    @Query("SELECT g.* FROM txa_groups g INNER JOIN txa_members m ON g.id = m.groupId WHERE g.hostId = :userId AND m.isActive = 1 AND g.isActive = 1 ORDER BY g.updatedAt DESC")
    suspend fun getGroupsWhereUserIsHost(userId: String): List<TXAGroupEntity>
    
    /**
     * Update group bank information
     * @return Number of rows affected
     */
    @Query("UPDATE txa_groups SET bankInfo = :bankInfo, updatedAt = :updatedAt WHERE id = :groupId")
    suspend fun updateBankInfo(groupId: String, bankInfo: String, updatedAt: Long = System.currentTimeMillis()): Int
    
    /**
     * Update group host
     * @return Number of rows affected
     */
    @Query("UPDATE txa_groups SET hostId = :newHostId, updatedAt = :updatedAt WHERE id = :groupId")
    suspend fun updateHost(groupId: String, newHostId: String, updatedAt: Long = System.currentTimeMillis()): Int
    
    /**
     * Update group basic info
     * @return Number of rows affected
     */
    @Query("UPDATE txa_groups SET name = :name, description = :description, avatarUrl = :avatarUrl, updatedAt = :updatedAt WHERE id = :groupId")
    suspend fun updateGroupInfo(
        groupId: String, 
        name: String, 
        description: String, 
        avatarUrl: String, 
        updatedAt: Long = System.currentTimeMillis()
    ): Int
    
    /**
     * Activate/deactivate a group
     * @return Number of rows affected
     */
    @Query("UPDATE txa_groups SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :groupId")
    suspend fun setGroupActive(groupId: String, isActive: Boolean, updatedAt: Long = System.currentTimeMillis()): Int
    
    /**
     * Get group count for user
     */
    @Query("SELECT COUNT(*) FROM txa_groups g INNER JOIN txa_members m ON g.id = m.groupId WHERE m.userId = :userId AND m.isActive = 1 AND g.isActive = 1")
    suspend fun getGroupCountForUser(userId: String): Int
    
    /**
     * Search groups by name for a user
     */
    @Query("SELECT g.* FROM txa_groups g INNER JOIN txa_members m ON g.id = m.groupId WHERE m.userId = :userId AND m.isActive = 1 AND g.isActive = 1 AND g.name LIKE '%' || :query || '%' ORDER BY g.name ASC")
    suspend fun searchGroupsForUser(userId: String, query: String): List<TXAGroupEntity>
    
    /**
     * Get groups that need bank info setup
     */
    @Query("SELECT g.* FROM txa_groups g INNER JOIN txa_members m ON g.id = m.groupId WHERE m.userId = :userId AND m.isActive = 1 AND g.isActive = 1 AND (g.bankInfo IS NULL OR g.bankInfo = '')")
    suspend fun getGroupsWithoutBankInfo(userId: String): List<TXAGroupEntity>
}
