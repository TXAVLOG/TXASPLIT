/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAPaymentDao.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ke.txasplit.vk.data.entity.TXAPaymentEntity
import ke.txasplit.vk.data.entity.PaymentStatus
import ke.txasplit.vk.data.entity.PaymentMethod
import ke.txasplit.vk.data.entity.VerificationStatus

/**
 * Data Access Object for TXAPaymentEntity operations
 * Provides CRUD operations and queries for payment management
 */
@Dao
interface TXAPaymentDao {
    
    /**
     * Insert a new payment
     * @return The row ID of the inserted payment
     */
    @Insert
    suspend fun insertPayment(payment: TXAPaymentEntity): Long
    
    /**
     * Insert multiple payments
     */
    @Insert
    suspend fun insertPayments(payments: List<TXAPaymentEntity>): List<Long>
    
    /**
     * Update an existing payment
     * @return Number of rows affected
     */
    @Update
    suspend fun updatePayment(payment: TXAPaymentEntity): Int
    
    /**
     * Delete a payment
     * @return Number of rows affected
     */
    @Delete
    suspend fun deletePayment(payment: TXAPaymentEntity): Int
    
    /**
     * Delete payment by ID
     * @return Number of rows affected
     */
    @Query("DELETE FROM txa_payments WHERE id = :paymentId")
    suspend fun deletePaymentById(paymentId: String): Int
    
    /**
     * Get payment by ID
     * @return The payment entity or null if not found
     */
    @Query("SELECT * FROM txa_payments WHERE id = :paymentId")
    suspend fun getPaymentById(paymentId: String): TXAPaymentEntity?
    
    /**
     * Get all payments for a bill
     */
    @Query("SELECT * FROM txa_payments WHERE billId = :billId ORDER BY paymentDate DESC")
    suspend fun getPaymentsForBill(billId: String): List<TXAPaymentEntity>
    
    /**
     * Get Flow of payments for a bill (for reactive UI)
     */
    @Query("SELECT * FROM txa_payments WHERE billId = :billId ORDER BY paymentDate DESC")
    fun getPaymentsForBillFlow(billId: String): Flow<List<TXAPaymentEntity>>
    
    /**
     * Get payments by user
     */
    @Query("SELECT * FROM txa_payments WHERE payerUserId = :userId ORDER BY paymentDate DESC")
    suspend fun getPaymentsByUser(userId: String): List<TXAPaymentEntity>
    
    /**
     * Get Flow of payments by user
     */
    @Query("SELECT * FROM txa_payments WHERE payerUserId = :userId ORDER BY paymentDate DESC")
    fun getPaymentsByUserFlow(userId: String): Flow<List<TXAPaymentEntity>>
    
    /**
     * Get payments by status
     */
    @Query("SELECT * FROM txa_payments WHERE status = :status ORDER BY paymentDate DESC")
    suspend fun getPaymentsByStatus(status: PaymentStatus): List<TXAPaymentEntity>
    
    /**
     * Get payments by verification status
     */
    @Query("SELECT * FROM txa_payments WHERE verificationStatus = :verificationStatus ORDER BY paymentDate DESC")
    suspend fun getPaymentsByVerificationStatus(verificationStatus: VerificationStatus): List<TXAPaymentEntity>
    
    /**
     * Get payments by method
     */
    @Query("SELECT * FROM txa_payments WHERE paymentMethod = :method ORDER BY paymentDate DESC")
    suspend fun getPaymentsByMethod(method: PaymentMethod): List<TXAPaymentEntity>
    
    /**
     * Get payments pending verification
     */
    @Query("SELECT * FROM txa_payments WHERE verificationStatus = 'PENDING' ORDER BY paymentDate ASC")
    suspend fun getPaymentsPendingVerification(): List<TXAPaymentEntity>
    
    /**
     * Get payments pending verification for a group
     */
    @Query("""
        SELECT p.* FROM txa_payments p 
        INNER JOIN txa_bills b ON p.billId = b.id 
        WHERE b.groupId = :groupId AND p.verificationStatus = 'PENDING' 
        ORDER BY p.paymentDate ASC
    """)
    suspend fun getPaymentsPendingVerificationForGroup(groupId: String): List<TXAPaymentEntity>
    
    /**
     * Get completed payments
     */
    @Query("SELECT * FROM txa_payments WHERE status = 'COMPLETED' ORDER BY paymentDate DESC")
    suspend fun getCompletedPayments(): List<TXAPaymentEntity>
    
    /**
     * Get payments by date range
     */
    @Query("SELECT * FROM txa_payments WHERE paymentDate >= :startDate AND paymentDate <= :endDate ORDER BY paymentDate DESC")
    suspend fun getPaymentsByDateRange(startDate: Long, endDate: Long): List<TXAPaymentEntity>
    
    /**
     * Update payment status
     * @return Number of rows affected
     */
    @Query("UPDATE txa_payments SET status = :status, updatedAt = :updatedAt WHERE id = :paymentId")
    suspend fun updatePaymentStatus(paymentId: String, status: PaymentStatus, updatedAt: Long = System.currentTimeMillis()): Int
    
    /**
     * Update payment verification status
     * @return Number of rows affected
     */
    @Query("UPDATE txa_payments SET verificationStatus = :verificationStatus, verifiedBy = :verifiedBy, verifiedAt = :verifiedAt, updatedAt = :updatedAt WHERE id = :paymentId")
    suspend fun updateVerificationStatus(
        paymentId: String, 
        verificationStatus: VerificationStatus, 
        verifiedBy: String? = null,
        verifiedAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis()
    ): Int
    
    /**
     * Verify a payment
     * @return Number of rows affected
     */
    @Query("UPDATE txa_payments SET verificationStatus = 'VERIFIED', verifiedBy = :verifiedBy, verifiedAt = :verifiedAt, updatedAt = :updatedAt WHERE id = :paymentId")
    suspend fun verifyPayment(paymentId: String, verifiedBy: String, verifiedAt: Long = System.currentTimeMillis(), updatedAt: Long = System.currentTimeMillis()): Int
    
    /**
     * Reject a payment
     * @return Number of rows affected
     */
    @Query("UPDATE txa_payments SET verificationStatus = 'REJECTED', verifiedBy = :verifiedBy, verifiedAt = :verifiedAt, updatedAt = :updatedAt WHERE id = :paymentId")
    suspend fun rejectPayment(paymentId: String, verifiedBy: String, verifiedAt: Long = System.currentTimeMillis(), updatedAt: Long = System.currentTimeMillis()): Int
    
    /**
     * Get total paid amount for a bill
     */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM txa_payments WHERE billId = :billId AND status = 'COMPLETED'")
    suspend fun getTotalPaidAmountForBill(billId: String): Long
    
    /**
     * Get total paid amount by user in a group
     */
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0) FROM txa_payments p 
        INNER JOIN txa_bills b ON p.billId = b.id 
        WHERE b.groupId = :groupId AND p.payerUserId = :userId AND p.status = 'COMPLETED'
    """)
    suspend fun getTotalPaidAmountByUserInGroup(groupId: String, userId: String): Long
    
    /**
     * Get payment count for bill
     */
    @Query("SELECT COUNT(*) FROM txa_payments WHERE billId = :billId")
    suspend fun getPaymentCount(billId: String): Int
    
    /**
     * Search payments by notes in a bill
     */
    @Query("SELECT * FROM txa_payments WHERE billId = :billId AND notes LIKE '%' || :query || '%' ORDER BY paymentDate DESC")
    suspend fun searchPaymentsInBill(billId: String, query: String): List<TXAPaymentEntity>
    
    /**
     * Get payment statistics for a bill
     */
    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pending,
            SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed,
            SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelled,
            SUM(CASE WHEN verificationStatus = 'PENDING' THEN 1 ELSE 0 END) as pendingVerification,
            SUM(CASE WHEN verificationStatus = 'VERIFIED' THEN 1 ELSE 0 END) as verified,
            SUM(CASE WHEN verificationStatus = 'REJECTED' THEN 1 ELSE 0 END) as rejected,
            COALESCE(SUM(amount), 0) as totalAmount
        FROM txa_payments 
        WHERE billId = :billId
    """)
    suspend fun getPaymentStatistics(billId: String): PaymentStatistics?
    
    /**
     * Get payment statistics for a user in a group
     */
    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN p.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed,
            COALESCE(SUM(p.amount), 0) as totalAmount
        FROM txa_payments p 
        INNER JOIN txa_bills b ON p.billId = b.id 
        WHERE b.groupId = :groupId AND p.payerUserId = :userId
    """)
    suspend fun getUserPaymentStatsInGroup(groupId: String, userId: String): UserPaymentStats?
    
    /**
     * Delete all payments for a bill
     * @return Number of rows affected
     */
    @Query("DELETE FROM txa_payments WHERE billId = :billId")
    suspend fun deleteAllPaymentsForBill(billId: String): Int
}

/**
 * Data class for payment statistics
 */
data class PaymentStatistics(
    val total: Int,
    val pending: Int,
    val completed: Int,
    val cancelled: Int,
    val pendingVerification: Int,
    val verified: Int,
    val rejected: Int,
    val totalAmount: Long
) {
    /**
     * Get completion rate percentage
     */
    fun getCompletionRate(): Float {
        return if (total > 0) (completed.toFloat() / total.toFloat()) * 100f else 0f
    }
    
    /**
     * Get verification rate percentage
     */
    fun getVerificationRate(): Float {
        return if (total > 0) (verified.toFloat() / total.toFloat()) * 100f else 0f
    }
}

/**
 * Data class for user payment statistics in a group
 */
data class UserPaymentStats(
    val total: Int,
    val completed: Int,
    val totalAmount: Long
) {
    /**
     * Get completion rate percentage
     */
    fun getCompletionRate(): Float {
        return if (total > 0) (completed.toFloat() / total.toFloat()) * 100f else 0f
    }
}
