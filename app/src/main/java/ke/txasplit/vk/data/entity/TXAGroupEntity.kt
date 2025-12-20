/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAGroupEntity.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import ke.txasplit.vk.data.TXABankInfo
import kotlinx.serialization.Serializable

/**
 * Room entity representing a TXASplit group
 * Groups contain members with different roles and financial data
 */
@Entity(
    tableName = "txa_groups",
    indices = [
        Index(value = ["inviteCode"], unique = true)
    ]
)
@Serializable
data class TXAGroupEntity(
    /**
     * Primary key - unique group identifier
     */
    @PrimaryKey
    val id: String,
    
    /**
     * Human-readable group name
     */
    val name: String,
    
    /**
     * Unique 4-character invite code (format: TXAS-XXXX)
     * Used for quick group joining
     */
    val inviteCode: String,
    
    /**
     * ID of the user who created this group (initial Host)
     */
    val createdBy: String,
    
    /**
     * Current Host user ID
     */
    val hostId: String,
    
    /**
     * Group description (optional)
     */
    val description: String = "",
    
    /**
     * Group avatar URL (optional)
     */
    val avatarUrl: String = "",
    
    /**
     * Bank information for VietQR payments (JSON string)
     * Contains: bankId, accountNumber, accountName
     * Empty string means not configured
     */
    val bankInfo: String = "",
    
    /**
     * Default currency code (e.g., "VND", "USD")
     */
    val currency: String = "VND",
    
    /**
     * Whether the group is active
     * Inactive groups don't show in main list but preserve data
     */
    val isActive: Boolean = true,
    
    /**
     * Group creation timestamp (milliseconds since epoch)
     */
    val createdAt: Long = System.currentTimeMillis(),
    
    /**
     * Last update timestamp (milliseconds since epoch)
     */
    val updatedAt: Long = System.currentTimeMillis(),
    
    /**
     * Group settings (JSON string)
     * Contains: notification preferences, privacy settings, etc.
     */
    val settings: String = "{}"
) {
    /**
     * Check if bank information is configured
     */
    fun hasBankInfo(): Boolean {
        return bankInfo.isNotBlank()
    }
    
    /**
     * Check if a user is the host of this group
     */
    fun isHost(userId: String): Boolean {
        return hostId == userId
    }
    
    /**
     * Check if a user created this group
     */
    fun isCreator(userId: String): Boolean {
        return createdBy == userId
    }
}
