/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAStatisticsUseCase.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.domain.usecase

import ke.txasplit.vk.statistics.TXAStatisticsManager
import ke.txasplit.vk.export.TXAExportManager
import ke.txasplit.vk.statistics.ChartPeriod
import ke.txasplit.vk.data.repository.TXAGroupRepository
import ke.txasplit.vk.data.dao.TXAMemberDao
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Use case for statistics and export operations
 * Handles business logic for data visualization and report generation
 */
class TXAStatisticsUseCase @Inject constructor(
    private val statisticsManager: TXAStatisticsManager,
    private val exportManager: TXAExportManager,
    private val groupRepository: TXAGroupRepository,
    private val memberDao: TXAMemberDao
) {
    
    /**
     * Get spending trend data for chart visualization
     * @param groupId Group ID (optional)
     * @param period Chart period (daily, weekly, monthly)
     * @param monthsBack Number of months to look back
     * @return Result containing spending trend data
     */
    suspend fun getSpendingTrend(
        groupId: String? = null,
        period: ChartPeriod = ChartPeriod.MONTHLY,
        monthsBack: Int = 6
    ): Result<List<ke.txasplit.vk.statistics.SpendingTrendData>> {
        // Validate input
        if (monthsBack < 1 || monthsBack > 24) {
            return Result.failure(IllegalArgumentException("monthsBack must be between 1 and 24"))
        }
        
        // Validate group access if groupId provided
        if (groupId != null) {
            val hasAccess = memberDao.isUserInGroup(getCurrentUserId(), groupId)
            if (!hasAccess) {
                return Result.failure(IllegalArgumentException("Access denied to group"))
            }
        }
        
        // Calculate date range
        val endDate = System.currentTimeMillis()
        val startDate = endDate - TimeUnit.DAYS.toMillis(monthsBack.toLong() * 30L)
        
        return try {
            val trendData = statisticsManager.getSpendingTrend(groupId, startDate, endDate, period)
            Result.success(trendData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get payment distribution by category
     * @param groupId Group ID (optional)
     * @param monthsBack Number of months to look back
     * @return Result containing category distribution data
     */
    suspend fun getPaymentDistribution(
        groupId: String? = null,
        monthsBack: Int = 12
    ): Result<List<ke.txasplit.vk.statistics.CategoryDistributionData>> {
        if (monthsBack < 1 || monthsBack > 24) {
            return Result.failure(IllegalArgumentException("monthsBack must be between 1 and 24"))
        }
        
        if (groupId != null) {
            val hasAccess = memberDao.isUserInGroup(getCurrentUserId(), groupId)
            if (!hasAccess) {
                return Result.failure(IllegalArgumentException("Access denied to group"))
            }
        }
        
        val endDate = System.currentTimeMillis()
        val startDate = endDate - TimeUnit.DAYS.toMillis(monthsBack.toLong() * 30L)
        
        return try {
            val distributionData = statisticsManager.getPaymentDistribution(groupId, startDate, endDate)
            
            // Calculate percentages
            val total = distributionData.sumOf { it.totalAmount }
            val dataWithPercentage = distributionData.map { it.copy(percentage = if (total > 0) (it.totalAmount.toFloat() / total.toFloat()) * 100f else 0f) }
            
            Result.success(dataWithPercentage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get payment method distribution
     * @param groupId Group ID (optional)
     * @param monthsBack Number of months to look back
     * @return Result containing payment method distribution data
     */
    suspend fun getPaymentMethodDistribution(
        groupId: String? = null,
        monthsBack: Int = 12
    ): Result<List<ke.txasplit.vk.statistics.PaymentMethodDistributionData>> {
        if (monthsBack < 1 || monthsBack > 24) {
            return Result.failure(IllegalArgumentException("monthsBack must be between 1 and 24"))
        }
        
        if (groupId != null) {
            val hasAccess = memberDao.isUserInGroup(getCurrentUserId(), groupId)
            if (!hasAccess) {
                return Result.failure(IllegalArgumentException("Access denied to group"))
            }
        }
        
        val endDate = System.currentTimeMillis()
        val startDate = endDate - TimeUnit.DAYS.toMillis(monthsBack.toLong() * 30L)
        
        return try {
            val distributionData = statisticsManager.getPaymentMethodDistribution(groupId, startDate, endDate)
            Result.success(distributionData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get monthly summary for a year
     * @param groupId Group ID (optional)
     * @param year Year to get data for
     * @return Result containing monthly summary data
     */
    suspend fun getMonthlySummary(
        groupId: String? = null,
        year: Int
    ): Result<List<ke.txasplit.vk.statistics.MonthlySummaryData>> {
        if (year < 2020 || year > 2030) {
            return Result.failure(IllegalArgumentException("Invalid year"))
        }
        
        if (groupId != null) {
            val hasAccess = memberDao.isUserInGroup(getCurrentUserId(), groupId)
            if (!hasAccess) {
                return Result.failure(IllegalArgumentException("Access denied to group"))
            }
        }
        
        return try {
            val summaryData = statisticsManager.getMonthlySummary(groupId, year)
            Result.success(summaryData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get top spenders in a group
     * @param groupId Group ID
     * @param limit Maximum number of top spenders
     * @return Result containing top spender data
     */
    suspend fun getTopSpenders(
        groupId: String,
        limit: Int = 10
    ): Result<List<ke.txasplit.vk.statistics.TopSpenderData>> {
        if (limit < 1 || limit > 50) {
            return Result.failure(IllegalArgumentException("limit must be between 1 and 50"))
        }
        
        val hasAccess = memberDao.isUserInGroup(getCurrentUserId(), groupId)
        if (!hasAccess) {
            return Result.failure(IllegalArgumentException("Access denied to group"))
        }
        
        return try {
            val topSpenders = statisticsManager.getTopSpenders(groupId, limit)
            Result.success(topSpenders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Export bills to CSV
     * @param context Application context
     * @param groupId Group ID (optional)
     * @param monthsBack Number of months to include
     * @return Result containing file path
     */
    suspend fun exportBillsToCSV(
        context: android.content.Context,
        groupId: String? = null,
        monthsBack: Int = 12
    ): Result<String> {
        if (monthsBack < 1 || monthsBack > 24) {
            return Result.failure(IllegalArgumentException("monthsBack must be between 1 and 24"))
        }
        
        if (groupId != null) {
            val hasAccess = memberDao.isUserInGroup(getCurrentUserId(), groupId)
            if (!hasAccess) {
                return Result.failure(IllegalArgumentException("Access denied to group"))
            }
        }
        
        val endDate = System.currentTimeMillis()
        val startDate = endDate - TimeUnit.DAYS.toMillis(monthsBack.toLong() * 30L)
        
        return exportManager.exportBillsToCSV(context, groupId, startDate, endDate)
    }
    
    /**
     * Export payments to CSV
     * @param context Application context
     * @param groupId Group ID (optional)
     * @param monthsBack Number of months to include
     * @return Result containing file path
     */
    suspend fun exportPaymentsToCSV(
        context: android.content.Context,
        groupId: String? = null,
        monthsBack: Int = 12
    ): Result<String> {
        if (monthsBack < 1 || monthsBack > 24) {
            return Result.failure(IllegalArgumentException("monthsBack must be between 1 and 24"))
        }
        
        if (groupId != null) {
            val hasAccess = memberDao.isUserInGroup(getCurrentUserId(), groupId)
            if (!hasAccess) {
                return Result.failure(IllegalArgumentException("Access denied to group"))
            }
        }
        
        val endDate = System.currentTimeMillis()
        val startDate = endDate - TimeUnit.DAYS.toMillis(monthsBack.toLong() * 30L)
        
        return exportManager.exportPaymentsToCSV(context, groupId, startDate, endDate)
    }
    
    /**
     * Export comprehensive report to Excel
     * @param context Application context
     * @param groupId Group ID (optional)
     * @param monthsBack Number of months to include
     * @return Result containing file path
     */
    suspend fun exportReportToExcel(
        context: android.content.Context,
        groupId: String? = null,
        monthsBack: Int = 12
    ): Result<String> {
        if (monthsBack < 1 || monthsBack > 24) {
            return Result.failure(IllegalArgumentException("monthsBack must be between 1 and 24"))
        }
        
        if (groupId != null) {
            val hasAccess = memberDao.isUserInGroup(getCurrentUserId(), groupId)
            if (!hasAccess) {
                return Result.failure(IllegalArgumentException("Access denied to group"))
            }
        }
        
        val endDate = System.currentTimeMillis()
        val startDate = endDate - TimeUnit.DAYS.toMillis(monthsBack.toLong() * 30L)
        
        return exportManager.exportReportToExcel(context, groupId, startDate, endDate)
    }
    
    /**
     * Export group summary to Excel
     * @param context Application context
     * @param groupId Group ID
     * @return Result containing file path
     */
    suspend fun exportGroupSummaryToExcel(
        context: android.content.Context,
        groupId: String
    ): Result<String> {
        val hasAccess = memberDao.isUserInGroup(getCurrentUserId(), groupId)
        if (!hasAccess) {
            return Result.failure(IllegalArgumentException("Access denied to group"))
        }
        
        return exportManager.exportGroupSummaryToExcel(context, groupId)
    }
    
    /**
     * Clear statistics cache
     */
    fun clearCache() {
        statisticsManager.clearCache()
    }
    
    /**
     * Get current user ID
     * This should be implemented based on your authentication system
     */
    private fun getCurrentUserId(): String {
        // TODO: Implement based on your authentication system
        return "current_user_id"
    }
}
