/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAPaymentEntity.kt
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
 * Room entity representing a payment for a bill
 * Tracks individual payments and their verification status
 */
@Entity(
    tableName = "txa_payments",
    foreignKeys = [
        ForeignKey(
            entity = TXABillEntity::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TXAMemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["payerMemberId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["billId"]),
        Index(value = ["payerMemberId"]),
        Index(value = ["payerUserId"]),
        Index(value = ["status"]),
        Index(value = ["paymentDate"]),
        Index(value = ["verificationStatus"]),
        Index(value = ["billId", "payerUserId"])
    ]
)
@Serializable
data class TXAPaymentEntity(
    /**
     * Primary key - unique payment identifier
     */
    @PrimaryKey
    val id: String,
    
    /**
     * ID of the bill this payment belongs to
     */
    val billId: String,
    
    /**
     * ID of the member who made the payment
     */
    val payerMemberId: String,
    
    /**
     * ID of the user who made the payment (for quick lookup)
     */
    val payerUserId: String,
    
    /**
     * Payment amount (in VND)
     */
    val amount: Long,
    
    /**
     * Payment date timestamp (milliseconds since epoch)
     */
    val paymentDate: Long,
    
    /**
     * Payment method
     */
    val paymentMethod: PaymentMethod,
    
    /**
     * Payment status
     */
    val status: PaymentStatus,
    
    /**
     * Verification status (for Host/Co-host to verify payments)
     */
    val verificationStatus: VerificationStatus,
    
    /**
     * ID of the user who verified this payment
     */
    val verifiedBy: String? = null,
    
    /**
     * Verification timestamp
     */
    val verifiedAt: Long = 0,
    
    /**
     * Transaction reference (from banking app, etc.)
     */
    val transactionReference: String = "",
    
    /**
     * Payment notes
     */
    val notes: String = "",
    
    /**
     * Attachments (JSON string array of URLs)
     */
    val attachments: String = "[]",
    
    /**
     * Payment metadata (JSON string)
     * Contains: bank info, transfer details, etc.
     */
    val metadata: String = "{}",
    
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
     * Check if payment is verified
     */
    fun isVerified(): Boolean {
        return verificationStatus == VerificationStatus.VERIFIED
    }
    
    /**
     * Check if payment is pending verification
     */
    fun isPendingVerification(): Boolean {
        return verificationStatus == VerificationStatus.PENDING
    }
    
    /**
     * Check if payment is rejected
     */
    fun isRejected(): Boolean {
        return verificationStatus == VerificationStatus.REJECTED
    }
    
    /**
     * Check if payment is completed
     */
    fun isCompleted(): Boolean {
        return status == PaymentStatus.COMPLETED
    }
    
    /**
     * Check if payment is cancelled
     */
    fun isCancelled(): Boolean {
        return status == PaymentStatus.CANCELLED
    }
    
    /**
     * Check if payment is refunded
     */
    fun isRefunded(): Boolean {
        return status == PaymentStatus.REFUNDED
    }
    
    /**
     * Get payment method display name
     */
    fun getPaymentMethodDisplayName(): String {
        return when (paymentMethod) {
            PaymentMethod.CASH -> "Tiền mặt"
            PaymentMethod.BANK_TRANSFER -> "Chuyển khoản"
            PaymentMethod.VIETQR -> "VietQR"
            PaymentMethod.E_WALLET -> "Ví điện tử"
            PaymentMethod.CREDIT_CARD -> "Thẻ tín dụng"
            PaymentMethod.OTHER -> "Khác"
        }
    }
    
    /**
     * Get verification status display name
     */
    fun getVerificationStatusDisplayName(): String {
        return when (verificationStatus) {
            VerificationStatus.PENDING -> "Chờ xác nhận"
            VerificationStatus.VERIFIED -> "Đã xác nhận"
            VerificationStatus.REJECTED -> "Bị từ chối"
        }
    }
}

/**
 * Payment methods
 */
@Serializable
enum class PaymentMethod {
    CASH,
    BANK_TRANSFER,
    VIETQR,
    E_WALLET,
    CREDIT_CARD,
    OTHER
}

/**
 * Payment status
 */
@Serializable
enum class PaymentStatus {
    PENDING,
    COMPLETED,
    CANCELLED,
    REFUNDED,
    FAILED
}

/**
 * Payment verification status
 */
@Serializable
enum class VerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED
}
