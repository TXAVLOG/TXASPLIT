/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXABillEntity.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable

/**
 * Room entity representing a bill in a TXASplit group
 * Supports recurring bills and payment tracking
 */
@Entity(
    tableName = "txa_bills",
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
        Index(value = ["createdBy"]),
        Index(value = ["dueDate"]),
        Index(value = ["isActive"]),
        Index(value = ["isRecurring"]),
        Index(value = ["groupId", "dueDate"])
    ]
)
@Serializable
data class TXABillEntity(
    /**
     * Primary key - unique bill identifier
     */
    @PrimaryKey
    val id: String,
    
    /**
     * ID of the group this bill belongs to
     */
    val groupId: String,
    
    /**
     * ID of the user who created this bill
     */
    val createdBy: String,
    
    /**
     * Bill title/name
     */
    val title: String,
    
    /**
     * Bill description
     */
    val description: String = "",
    
    /**
     * Total amount of the bill (in VND)
     */
    val totalAmount: Long,
    
    /**
     * Due date timestamp (milliseconds since epoch)
     */
    val dueDate: Long,
    
    /**
     * Whether this bill is active
     */
    val isActive: Boolean = true,
    
    /**
     * Whether this bill is recurring
     */
    val isRecurring: Boolean = false,
    
    /**
     * Recurrence type (if recurring)
     */
    val recurrenceType: BillRecurrenceType = BillRecurrenceType.NONE,
    
    /**
     * Recurrence interval (e.g., every 2 weeks, every 3 months)
     */
    val recurrenceInterval: Int = 1,
    
    /**
     * End date for recurring bills (optional)
     * 0 means no end date
     */
    val recurrenceEndDate: Long = 0,
    
    /**
     * Payment status
     */
    val status: BillStatus = BillStatus.PENDING,
    
    /**
     * Amount already paid
     */
    val paidAmount: Long = 0,
    
    /**
     * Bill category
     */
    val category: BillCategory = BillCategory.OTHER,
    
    /**
     * Bill tags (JSON string array)
     */
    val tags: String = "[]",
    
    /**
     * Attachments (JSON string array of URLs)
     */
    val attachments: String = "[]",
    
    /**
     * Bill settings (JSON string)
     * Contains: notification preferences, split rules, etc.
     */
    val settings: String = "{}",
    
    /**
     * Creation timestamp (milliseconds since epoch)
     */
    val createdAt: Long = System.currentTimeMillis(),
    
    /**
     * Last update timestamp (milliseconds since epoch)
     */
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Check if bill is overdue
     */
    fun isOverdue(): Boolean {
        val now = System.currentTimeMillis()
        return status != BillStatus.PAID && dueDate < now && isActive
    }
    
    /**
     * Check if bill is due soon (within 3 days)
     */
    fun isDueSoon(): Boolean {
        val now = System.currentTimeMillis()
        val threeDaysFromNow = now + (3 * 24 * 60 * 60 * 1000)
        return status != BillStatus.PAID && dueDate <= threeDaysFromNow && dueDate > now && isActive
    }
    
    /**
     * Get remaining amount to be paid
     */
    fun getRemainingAmount(): Long {
        return totalAmount - paidAmount
    }
    
    /**
     * Check if bill is fully paid
     */
    fun isFullyPaid(): Boolean {
        return paidAmount >= totalAmount
    }
    
    /**
     * Get payment progress percentage
     */
    fun getPaymentProgress(): Float {
        return if (totalAmount > 0) (paidAmount.toFloat() / totalAmount.toFloat()) * 100f else 0f
    }
    
    /**
     * Check if bill has next recurrence date
     */
    fun hasNextRecurrence(): Boolean {
        if (!isRecurring || recurrenceEndDate > 0) {
            return false
        }
        return true
    }
    
    /**
     * Calculate next due date for recurring bills
     */
    fun calculateNextDueDate(): Long {
        if (!isRecurring) return dueDate
        
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = dueDate
        
        when (recurrenceType) {
            BillRecurrenceType.DAILY -> {
                calendar.add(java.util.Calendar.DAY_OF_MONTH, recurrenceInterval)
            }
            BillRecurrenceType.WEEKLY -> {
                calendar.add(java.util.Calendar.WEEK_OF_YEAR, recurrenceInterval)
            }
            BillRecurrenceType.MONTHLY -> {
                calendar.add(java.util.Calendar.MONTH, recurrenceInterval)
            }
            BillRecurrenceType.YEARLY -> {
                calendar.add(java.util.Calendar.YEAR, recurrenceInterval)
            }
            BillRecurrenceType.NONE -> {
                return dueDate
            }
        }
        
        return calendar.timeInMillis
    }
}

/**
 * Bill recurrence types
 */
@Serializable
enum class BillRecurrenceType {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

/**
 * Bill status enum
 */
@Serializable
enum class BillStatus {
    PENDING,
    PARTIALLY_PAID,
    PAID,
    OVERDUE,
    CANCELLED
}

/**
 * Bill categories
 */
@Serializable
enum class BillCategory {
    FOOD,
    HOUSING,
    TRANSPORTATION,
    UTILITIES,
    ENTERTAINMENT,
    HEALTHCARE,
    EDUCATION,
    SHOPPING,
    TRAVEL,
    BUSINESS,
    OTHER
}
