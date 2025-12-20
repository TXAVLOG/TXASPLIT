/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXABillWorker.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import ke.txasplit.vk.data.dao.TXABillDao
import ke.txasplit.vk.data.dao.TXAMemberDao
import ke.txasplit.vk.data.entity.TXABillEntity
import ke.txasplit.vk.notification.TXANotificationManager

/**
 * WorkManager worker for periodic bill notifications
 * Runs daily to check for due bills and send notifications
 */
@HiltWorker
class TXABillWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val billDao: TXABillDao,
    private val memberDao: TXAMemberDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            checkAndSendNotifications()
            Result.success()
        } catch (e: Exception) {
            // Log error and retry with exponential backoff
            e.printStackTrace()
            Result.retry()
        }
    }

    /**
     * Check for due bills and send appropriate notifications
     */
    private suspend fun checkAndSendNotifications() {
        val now = System.currentTimeMillis()
        val threeDaysFromNow = now + TimeUnit.DAYS.toMillis(3)
        
        // Get all active bills
        val activeBills = billDao.getAllActiveBills()
        
        for (bill in activeBills) {
            when {
                bill.isOverdue() -> {
                    sendOverdueBillNotification(bill)
                }
                bill.isDueSoon() -> {
                    sendDueSoonNotification(bill)
                }
                bill.dueDate <= threeDaysFromNow && bill.dueDate > now -> {
                    sendPaymentReminderNotification(bill)
                }
            }
        }
    }

    /**
     * Send notification for overdue bills
     */
    private suspend fun sendOverdueBillNotification(bill: TXABillEntity) {
        val group = billDao.getGroupForBill(bill.id)
        val overdueDays = calculateOverdueDays(bill.dueDate)
        
        if (group != null) {
            TXANotificationManager.showOverdueBillNotification(
                context = applicationContext,
                billTitle = bill.title,
                groupName = group.name,
                amount = bill.totalAmount,
                overdueDays = overdueDays,
                billId = bill.id
            )
        }
    }

    /**
     * Send notification for bills due soon (within 3 days)
     */
    private suspend fun sendDueSoonNotification(bill: TXABillEntity) {
        val group = billDao.getGroupForBill(bill.id)
        
        if (group != null) {
            TXANotificationManager.showBillDueNotification(
                context = applicationContext,
                billTitle = bill.title,
                groupName = group.name,
                amount = bill.totalAmount,
                dueDate = bill.dueDate,
                billId = bill.id
            )
        }
    }

    /**
     * Send payment reminder notification
     */
    private suspend fun sendPaymentReminderNotification(bill: TXABillEntity) {
        val group = billDao.getGroupForBill(bill.id)
        val daysLeft = calculateDaysLeft(bill.dueDate)
        
        if (group != null) {
            TXANotificationManager.showPaymentReminderNotification(
                context = applicationContext,
                billTitle = bill.title,
                groupName = group.name,
                amount = bill.totalAmount,
                daysLeft = daysLeft,
                billId = bill.id
            )
        }
    }

    /**
     * Calculate how many days a bill is overdue
     */
    private fun calculateOverdueDays(dueDate: Long): Int {
        val now = System.currentTimeMillis()
        return if (dueDate < now) {
            TimeUnit.MILLISECONDS.toDays(now - dueDate).toInt()
        } else {
            0
        }
    }

    /**
     * Calculate days left until due date
     */
    private fun calculateDaysLeft(dueDate: Long): Int {
        val now = System.currentTimeMillis()
        return if (dueDate > now) {
            TimeUnit.MILLISECONDS.toDays(dueDate - now).toInt()
        } else {
            0
        }
    }

    companion object {
        /**
         * Unique work name for bill notifications
         */
        const val BILL_NOTIFICATION_WORK = "TXABillNotificationWork"

        /**
         * Schedule periodic bill notifications
         * Runs daily at 9:00 AM
         */
        fun scheduleBillNotifications(context: Context) {
            val workRequest = androidx.work.PeriodicWorkRequestBuilder<TXABillWorker>(
                1, // Repeat interval
                TimeUnit.DAYS
            )
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

            androidx.work.WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    BILL_NOTIFICATION_WORK,
                    androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
        }

        /**
         * Cancel bill notifications
         */
        fun cancelBillNotifications(context: Context) {
            androidx.work.WorkManager.getInstance(context)
                .cancelUniqueWork(BILL_NOTIFICATION_WORK)
        }

        /**
         * Calculate initial delay to 9:00 AM tomorrow
         */
        private fun calculateInitialDelay(): Long {
            val now = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = now
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 9)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)

            // If it's already past 9:00 AM, schedule for tomorrow
            if (calendar.timeInMillis <= now) {
                calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
            }

            return calendar.timeInMillis - now
        }
    }
}
