/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAMemberEntity.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import ke.txasplit.vk.data.TXAMemberRole
import kotlinx.serialization.Serializable

/**
 * Room entity representing a member in a TXASplit group
 * Each user can be in multiple groups with different roles
 */
@Entity(
    tableName = "txa_members",
    foreignKeys = [
        ForeignKey(
            entity = TXAGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["userId"]),
        Index(value = ["groupId", "userId"], unique = true),
        Index(value = ["groupId", "role"])
    ]
)
@Serializable
data class TXAMemberEntity(
    /**
     * Primary key - unique member identifier
     */
    @PrimaryKey
    val id: String,
    
    /**
     * ID of the group this member belongs to
     */
    val groupId: String,
    
    /**
     * ID of the user (can be from authentication system)
     */
    val userId: String,
    
    /**
     * Member's role in this group
     */
    val role: TXAMemberRole,
    
    /**
     * Member's display name in this group
     * Can be different from their global profile name
     */
    val displayName: String,
    
    /**
     * Member's avatar URL in this group (optional)
     */
    val avatarUrl: String = "",
    
    /**
     * Whether this member is active in the group
     * Inactive members are kept for historical data but don't count in validation
     */
    val isActive: Boolean = true,
    
    /**
     * When this member joined the group (milliseconds since epoch)
     */
    val joinedAt: Long = System.currentTimeMillis(),
    
    /**
     * Last activity timestamp (milliseconds since epoch)
     */
    val lastActiveAt: Long = System.currentTimeMillis(),
    
    /**
     * Member-specific settings (JSON string)
     * Contains: notification preferences, privacy settings, etc.
     */
    val settings: String = "{}"
) {
    /**
     * Check if this member has Host privileges
     */
    fun isHost(): Boolean = role == TXAMemberRole.HOST
    
    /**
     * Check if this member has Co-host privileges
     */
    fun isCoHost(): Boolean = role == TXAMemberRole.CO_HOST
    
    /**
     * Check if this member is a regular Member
     */
    fun isMember(): Boolean = role == TXAMemberRole.MEMBER
    
    /**
     * Check if this member can manage group settings
     */
    fun canManageGroup(): Boolean = isHost()
    
    /**
     * Check if this member can add/edit bills
     */
    fun canManageBills(): Boolean = isHost() || isCoHost()
    
    /**
     * Check if this member can remove other members
     */
    fun canRemoveMembers(): Boolean = isHost()
    
    /**
     * Check if this member can change roles
     */
    fun canChangeRoles(): Boolean = isHost()
    
    /**
     * Check if this member can view all financial data
     */
    fun canViewFinancialData(): Boolean = true // All members can view
    
    /**
     * Check if this member can mark payments
     */
    fun canMarkPayments(): Boolean = true // All members can mark their own payments
}

/**
 * Data class representing a member with their group information
 * Used for joined queries with @Relation
 */
@Serializable
data class TXAMemberWithGroup(
    val member: TXAMemberEntity,
    val group: TXAGroupEntity
) {
    /**
     * Convenience method to get group name
     */
    fun getGroupName(): String = group.name
    
    /**
     * Convenience method to get invite code
     */
    fun getInviteCode(): String = group.inviteCode
    
    /**
     * Check if user is host of their group
     */
    fun isGroupHost(): Boolean = member.isHost() && member.userId == group.hostId
}
