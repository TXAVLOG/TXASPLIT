/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAStatisticsManager.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.statistics

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ke.txasplit.vk.data.dao.TXABillDao
import ke.txasplit.vk.data.dao.TXAPaymentDao
import ke.txasplit.vk.data.entity.BillCategory
import ke.txasplit.vk.data.entity.PaymentMethod
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for processing and caching statistics data
 * Provides aggregated data for charts and exports
 */
@Singleton
class TXAStatisticsManager @Inject constructor(
    private val billDao: TXABillDao,
    private val paymentDao: TXAPaymentDao
) {
    
    /**
     * Cache for statistics data with TTL
     */
    private var statisticsCache: StatisticsCache? = null
    private val cacheTTL = TimeUnit.MINUTES.toMillis(5) // 5 minutes cache
    
    /**
     * Get spending trend data for line chart
     * @param groupId Group ID (optional, if null returns all groups)
     * @param startDate Start date timestamp
     * @param endDate End date timestamp
     * @param period Period type (daily, weekly, monthly)
     * @return List of spending trend data points
     */
    suspend fun getSpendingTrend(
        groupId: String? = null,
        startDate: Long,
        endDate: Long,
        period: ChartPeriod = ChartPeriod.DAILY
    ): List<SpendingTrendData> {
        val cacheKey = "spending_trend_${groupId}_${startDate}_${endDate}_${period}"
        
        // Check cache first
        statisticsCache?.let { cache ->
            if (System.currentTimeMillis() - cache.timestamp < cacheTTL && cache.data.containsKey(cacheKey)) {
                @Suppress("UNCHECKED_CAST")
                return cache.data[cacheKey] as List<SpendingTrendData>
            }
        }
        
        // Calculate from database
        val payments = if (groupId != null) {
            paymentDao.getPaymentsForBill(groupId) // This needs to be fixed - should get payments by group
        } else {
            paymentDao.getPaymentsByDateRange(startDate, endDate)
        }
        
        val trendData = processSpendingTrend(payments, startDate, endDate, period)
        
        // Update cache
        updateCache(cacheKey, trendData)
        
        return trendData
    }
    
    /**
     * Get payment distribution data for pie chart
     * @param groupId Group ID (optional)
     * @param startDate Start date timestamp
     * @param endDate End date timestamp
     * @return Payment distribution by category
     */
    suspend fun getPaymentDistribution(
        groupId: String? = null,
        startDate: Long,
        endDate: Long
    ): List<CategoryDistributionData> {
        val cacheKey = "payment_dist_${groupId}_${startDate}_${endDate}"
        
        // Check cache
        statisticsCache?.let { cache ->
            if (System.currentTimeMillis() - cache.timestamp < cacheTTL && cache.data.containsKey(cacheKey)) {
                @Suppress("UNCHECKED_CAST")
                return cache.data[cacheKey] as List<CategoryDistributionData>
            }
        }
        
        // Get bills by category
        val bills = if (groupId != null) {
            billDao.getBillsInGroup(groupId)
        } else {
            billDao.getBillsByDateRange(startDate, endDate)
        }
        
        val distributionData = processCategoryDistribution(bills, startDate, endDate)
        
        // Update cache
        updateCache(cacheKey, distributionData)
        
        return distributionData
    }
    
    /**
     * Get payment method distribution
     * @param groupId Group ID (optional)
     * @param startDate Start date timestamp
     * @param endDate End date timestamp
     * @return Payment method distribution
     */
    suspend fun getPaymentMethodDistribution(
        groupId: String? = null,
        startDate: Long,
        endDate: Long
    ): List<PaymentMethodDistributionData> {
        val payments = if (groupId != null) {
            paymentDao.getPaymentsByDateRange(startDate, endDate) // Filter by group needed
        } else {
            paymentDao.getPaymentsByDateRange(startDate, endDate)
        }
        
        return processPaymentMethodDistribution(payments)
    }
    
    /**
     * Get monthly summary data
     * @param groupId Group ID (optional)
     * @param year Year to get data for
     * @return Monthly summary data
     */
    suspend fun getMonthlySummary(
        groupId: String? = null,
        year: Int = Calendar.getInstance().get(Calendar.YEAR)
    ): List<MonthlySummaryData> {
        val calendar = Calendar.getInstance()
        calendar.set(year, 0, 1, 0, 0, 0)
        val startDate = calendar.timeInMillis
        
        calendar.set(year, 11, 31, 23, 59, 59)
        val endDate = calendar.timeInMillis
        
        val bills = if (groupId != null) {
            billDao.getBillsInGroup(groupId)
        } else {
            billDao.getBillsByDateRange(startDate, endDate)
        }
        
        return processMonthlySummary(bills, year)
    }
    
    /**
     * Get top spenders in a group
     * @param groupId Group ID
     * @param limit Maximum number of top spenders
     * @return List of top spender data
     */
    suspend fun getTopSpenders(
        groupId: String,
        limit: Int = 10
    ): List<TopSpenderData> {
        val members = emptyList<ke.txasplit.vk.data.entity.TXAMemberEntity>() // Need to get from memberDao
        val topSpenders = mutableListOf<TopSpenderData>()
        
        // This needs proper implementation with memberDao
        // For now, return empty list
        
        return topSpenders
    }
    
    /**
     * Clear statistics cache
     */
    fun clearCache() {
        statisticsCache = null
    }
    
    /**
     * Process spending trend data
     */
    private fun processSpendingTrend(
        payments: List<ke.txasplit.vk.data.entity.TXAPaymentEntity>,
        startDate: Long,
        endDate: Long,
        period: ChartPeriod
    ): List<SpendingTrendData> {
        val groupedPayments = payments
            .filter { it.status == ke.txasplit.vk.data.entity.PaymentStatus.COMPLETED }
            .groupBy { payment ->
                when (period) {
                    ChartPeriod.DAILY -> getDayKey(payment.paymentDate)
                    ChartPeriod.WEEKLY -> getWeekKey(payment.paymentDate)
                    ChartPeriod.MONTHLY -> getMonthKey(payment.paymentDate)
                }
            }
        
        val calendar = Calendar.getInstance()
        val trendData = mutableListOf<SpendingTrendData>()
        
        // Generate all periods in range
        var current = startDate
        while (current <= endDate) {
            val key = when (period) {
                ChartPeriod.DAILY -> getDayKey(current)
                ChartPeriod.WEEKLY -> getWeekKey(current)
                ChartPeriod.MONTHLY -> getMonthKey(current)
            }
            
            val total = groupedPayments[key]?.sumOf { it.amount } ?: 0L
            trendData.add(SpendingTrendData(key, total, current))
            
            // Move to next period
            calendar.timeInMillis = current
            when (period) {
                ChartPeriod.DAILY -> calendar.add(Calendar.DAY_OF_MONTH, 1)
                ChartPeriod.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                ChartPeriod.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            }
            current = calendar.timeInMillis
        }
        
        return trendData
    }
    
    /**
     * Process category distribution
     */
    private fun processCategoryDistribution(
        bills: List<ke.txasplit.vk.data.entity.TXABillEntity>,
        startDate: Long,
        endDate: Long
    ): List<CategoryDistributionData> {
        return bills
            .filter { it.dueDate in startDate..endDate }
            .groupBy { it.category }
            .map { (category, categoryBills) ->
                val total = categoryBills.sumOf { it.totalAmount }
                CategoryDistributionData(
                    category = category,
                    totalAmount = total,
                    count = categoryBills.size,
                    percentage = 0f // Will be calculated later
                )
            }
            .sortedByDescending { it.totalAmount }
    }
    
    /**
     * Process payment method distribution
     */
    private fun processPaymentMethodDistribution(
        payments: List<ke.txasplit.vk.data.entity.TXAPaymentEntity>
    ): List<PaymentMethodDistributionData> {
        return payments
            .filter { it.status == ke.txasplit.vk.data.entity.PaymentStatus.COMPLETED }
            .groupBy { it.paymentMethod }
            .map { (method, methodPayments) ->
                val total = methodPayments.sumOf { it.amount }
                PaymentMethodDistributionData(
                    method = method,
                    totalAmount = total,
                    count = methodPayments.size
                )
            }
            .sortedByDescending { it.totalAmount }
    }
    
    /**
     * Process monthly summary
     */
    private fun processMonthlySummary(
        bills: List<ke.txasplit.vk.data.entity.TXABillEntity>,
        year: Int
    ): List<MonthlySummaryData> {
        val monthlyData = mutableListOf<MonthlySummaryData>()
        
        for (month in 1..12) {
            val calendar = Calendar.getInstance()
            calendar.set(year, month - 1, 1, 0, 0, 0)
            val monthStart = calendar.timeInMillis
            
            calendar.set(year, month - 1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
            val monthEnd = calendar.timeInMillis
            
            val monthBills = bills.filter { it.dueDate in monthStart..monthEnd }
            val totalAmount = monthBills.sumOf { it.totalAmount }
            val paidAmount = monthBills.sumOf { it.paidAmount }
            
            monthlyData.add(
                MonthlySummaryData(
                    month = month,
                    year = year,
                    totalAmount = totalAmount,
                    paidAmount = paidAmount,
                    billCount = monthBills.size
                )
            )
        }
        
        return monthlyData
    }
    
    /**
     * Update cache with new data
     */
    private fun updateCache(key: String, data: Any) {
        val currentCache = statisticsCache ?: StatisticsCache(System.currentTimeMillis(), mutableMapOf())
        currentCache.data[key] = data
        statisticsCache = currentCache
    }
    
    /**
     * Get day key for grouping
     */
    private fun getDayKey(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.DAY_OF_YEAR)}"
    }
    
    /**
     * Get week key for grouping
     */
    private fun getWeekKey(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.WEEK_OF_YEAR)}"
    }
    
    /**
     * Get month key for grouping
     */
    private fun getMonthKey(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}"
    }
}

/**
 * Cache for statistics data
 */
data class StatisticsCache(
    val timestamp: Long,
    val data: MutableMap<String, Any>
)

/**
 * Chart period types
 */
enum class ChartPeriod {
    DAILY,
    WEEKLY,
    MONTHLY
}

/**
 * Spending trend data point
 */
data class SpendingTrendData(
    val period: String,
    val amount: Long,
    val timestamp: Long
)

/**
 * Category distribution data
 */
data class CategoryDistributionData(
    val category: BillCategory,
    val totalAmount: Long,
    val count: Int,
    var percentage: Float
)

/**
 * Payment method distribution data
 */
data class PaymentMethodDistributionData(
    val method: PaymentMethod,
    val totalAmount: Long,
    val count: Int
)

/**
 * Monthly summary data
 */
data class MonthlySummaryData(
    val month: Int,
    val year: Int,
    val totalAmount: Long,
    val paidAmount: Long,
    val billCount: Int
)

/**
 * Top spender data
 */
data class TopSpenderData(
    val userId: String,
    val displayName: String,
    val totalAmount: Long,
    val paymentCount: Int
)
