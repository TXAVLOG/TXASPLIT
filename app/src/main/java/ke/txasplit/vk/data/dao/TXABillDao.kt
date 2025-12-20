/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXABillDao.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ke.txasplit.vk.data.entity.TXABillEntity
import ke.txasplit.vk.data.entity.BillStatus
import ke.txasplit.vk.data.entity.BillCategory

/**
 * Data Access Object for TXABillEntity operations
 * Provides CRUD operations and queries for bill management
 */
@Dao
interface TXABillDao {
    
    /**
     * Insert a new bill
     * @return The row ID of the inserted bill
     */
    @Insert
    suspend fun insertBill(bill: TXABillEntity): Long
    
    /**
     * Insert multiple bills
     */
    @Insert
    suspend fun insertBills(bills: List<TXABillEntity>): List<Long>
    
    /**
     * Update an existing bill
     * @return Number of rows affected
     */
    @Update
    suspend fun updateBill(bill: TXABillEntity): Int
    
    /**
     * Delete a bill
     * @return Number of rows affected
     */
    @Delete
    suspend fun deleteBill(bill: TXABillEntity): Int
    
    /**
     * Delete bill by ID
     * @return Number of rows affected
     */
    @Query("DELETE FROM txa_bills WHERE id = :billId")
    suspend fun deleteBillById(billId: String): Int
    
    /**
     * Get bill by ID
     * @return The bill entity or null if not found
     */
    @Query("SELECT * FROM txa_bills WHERE id = :billId")
    suspend fun getBillById(billId: String): TXABillEntity?
    
    /**
     * Get all bills in a group
     */
    @Query("SELECT * FROM txa_bills WHERE groupId = :groupId ORDER BY dueDate ASC")
    suspend fun getBillsInGroup(groupId: String): List<TXABillEntity>
    
    /**
     * Get Flow of bills in a group (for reactive UI)
     */
    @Query("SELECT * FROM txa_bills WHERE groupId = :groupId ORDER BY dueDate ASC")
    fun getBillsInGroupFlow(groupId: String): Flow<List<TXABillEntity>>
    
    /**
     * Get all active bills
     */
    @Query("SELECT * FROM txa_bills WHERE isActive = 1 ORDER BY dueDate ASC")
    suspend fun getAllActiveBills(): List<TXABillEntity>
    
    /**
     * Get Flow of all active bills
     */
    @Query("SELECT * FROM txa_bills WHERE isActive = 1 ORDER BY dueDate ASC")
    fun getAllActiveBillsFlow(): Flow<List<TXABillEntity>>
    
    /**
     * Get bills created by a specific user
     */
    @Query("SELECT * FROM txa_bills WHERE createdBy = :userId ORDER BY createdAt DESC")
    suspend fun getBillsCreatedByUser(userId: String): List<TXABillEntity>
    
    /**
     * Get bills by status
     */
    @Query("SELECT * FROM txa_bills WHERE status = :status ORDER BY dueDate ASC")
    suspend fun getBillsByStatus(status: BillStatus): List<TXABillEntity>
    
    /**
     * Get bills by category
     */
    @Query("SELECT * FROM txa_bills WHERE category = :category ORDER BY dueDate ASC")
    suspend fun getBillsByCategory(category: BillCategory): List<TXABillEntity>
    
    /**
     * Get overdue bills
     */
    @Query("SELECT * FROM txa_bills WHERE dueDate < :currentTime AND status != 'PAID' AND isActive = 1 ORDER BY dueDate ASC")
    suspend fun getOverdueBills(currentTime: Long = System.currentTimeMillis()): List<TXABillEntity>
    
    /**
     * Get bills due soon (within specified days)
     */
    @Query("SELECT * FROM txa_bills WHERE dueDate <= :endTime AND dueDate > :startTime AND status != 'PAID' AND isActive = 1 ORDER BY dueDate ASC")
    suspend fun getBillsDueSoon(startTime: Long, endTime: Long): List<TXABillEntity>
    
    /**
     * Get recurring bills
     */
    @Query("SELECT * FROM txa_bills WHERE isRecurring = 1 AND isActive = 1 ORDER BY dueDate ASC")
    suspend fun getRecurringBills(): List<TXABillEntity>
    
    /**
     * Get bills by date range
     */
    @Query("SELECT * FROM txa_bills WHERE dueDate >= :startDate AND dueDate <= :endDate AND isActive = 1 ORDER BY dueDate ASC")
    suspend fun getBillsByDateRange(startDate: Long, endDate: Long): List<TXABillEntity>
    
    /**
     * Update bill status
     * @return Number of rows affected
     */
    @Query("UPDATE txa_bills SET status = :status, updatedAt = :updatedAt WHERE id = :billId")
    suspend fun updateBillStatus(billId: String, status: BillStatus, updatedAt: Long = System.currentTimeMillis()): Int
    
    /**
     * Update bill paid amount
     * @return Number of rows affected
     */
    @Query("UPDATE txa_bills SET paidAmount = :paidAmount, updatedAt = :updatedAt WHERE id = :billId")
    suspend fun updatePaidAmount(billId: String, paidAmount: Long, updatedAt: Long = System.currentTimeMillis()): Int
    
    /**
     * Activate/deactivate a bill
     * @return Number of rows affected
     */
    @Query("UPDATE txa_bills SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :billId")
    suspend fun setBillActive(billId: String, isActive: Boolean, updatedAt: Long = System.currentTimeMillis()): Int
    
    /**
     * Update bill due date
     * @return Number of rows affected
     */
    @Query("UPDATE txa_bills SET dueDate = :dueDate, updatedAt = :updatedAt WHERE id = :billId")
    suspend fun updateDueDate(billId: String, dueDate: Long, updatedAt: Long = System.currentTimeMillis()): Int
    
    /**
     * Get bill count for group
     */
    @Query("SELECT COUNT(*) FROM txa_bills WHERE groupId = :groupId AND isActive = 1")
    suspend fun getBillCount(groupId: String): Int
    
    /**
     * Get total amount for group
     */
    @Query("SELECT COALESCE(SUM(totalAmount), 0) FROM txa_bills WHERE groupId = :groupId AND isActive = 1")
    suspend fun getTotalAmount(groupId: String): Long
    
    /**
     * Get total paid amount for group
     */
    @Query("SELECT COALESCE(SUM(paidAmount), 0) FROM txa_bills WHERE groupId = :groupId AND isActive = 1")
    suspend fun getTotalPaidAmount(groupId: String): Long
    
    /**
     * Search bills by title in a group
     */
    @Query("SELECT * FROM txa_bills WHERE groupId = :groupId AND isActive = 1 AND title LIKE '%' || :query || '%' ORDER BY dueDate ASC")
    suspend fun searchBillsInGroup(groupId: String, query: String): List<TXABillEntity>
    
    /**
     * Get bills that need payment reminders
     */
    @Query("SELECT * FROM txa_bills WHERE dueDate <= :reminderTime AND dueDate > :currentTime AND status != 'PAID' AND isActive = 1 ORDER BY dueDate ASC")
    suspend fun getBillsNeedingReminders(currentTime: Long, reminderTime: Long): List<TXABillEntity>
    
    /**
     * Get group for a bill (for notifications)
     */
    @Query("SELECT g.* FROM txa_groups g INNER JOIN txa_bills b ON g.id = b.groupId WHERE b.id = :billId")
    suspend fun getGroupForBill(billId: String): ke.txasplit.vk.data.entity.TXAGroupEntity?
    
    /**
     * Get bill statistics for a group
     */
    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pending,
            SUM(CASE WHEN status = 'PARTIALLY_PAID' THEN 1 ELSE 0 END) as partiallyPaid,
            SUM(CASE WHEN status = 'PAID' THEN 1 ELSE 0 END) as paid,
            SUM(CASE WHEN status = 'OVERDUE' THEN 1 ELSE 0 END) as overdue,
            COALESCE(SUM(totalAmount), 0) as totalAmount,
            COALESCE(SUM(paidAmount), 0) as totalPaid
        FROM txa_bills 
        WHERE groupId = :groupId AND isActive = 1
    """)
    suspend fun getBillStatistics(groupId: String): BillStatistics?
}

/**
 * Data class for bill statistics
 */
data class BillStatistics(
    val total: Int,
    val pending: Int,
    val partiallyPaid: Int,
    val paid: Int,
    val overdue: Int,
    val totalAmount: Long,
    val totalPaid: Long
) {
    /**
     * Get remaining amount to be paid
     */
    fun getRemainingAmount(): Long {
        return totalAmount - totalPaid
    }
    
    /**
     * Get payment progress percentage
     */
    fun getPaymentProgress(): Float {
        return if (totalAmount > 0) (totalPaid.toFloat() / totalAmount.toFloat()) * 100f else 0f
    }
}
