/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAAlarmScheduler.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import ke.txasplit.vk.notification.TXANotificationManager
import ke.txasplit.vk.txa

/**
 * Scheduler for exact alarms for bill due dates
 * Uses AlarmManager for precise timing of bill notifications
 */
object TXAAlarmScheduler {

    /**
     * Request codes for different alarm types
     */
    private const val ALARM_BILL_DUE = 2001
    private const val ALARM_BILL_REMINDER = 2002
    private const val ALARM_BILL_OVERDUE = 2003

    /**
     * Schedule exact alarm for bill due date
     */
    fun scheduleBillDueAlarm(
        context: Context,
        billId: String,
        billTitle: String,
        groupName: String,
        amount: Long,
        dueDate: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = createAlarmIntent(context, billId, "BILL_DUE").apply {
            putExtra("billTitle", billTitle)
            putExtra("groupName", groupName)
            putExtra("amount", amount)
            putExtra("dueDate", dueDate)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_BILL_DUE + billId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Check if we can schedule exact alarms
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        dueDate,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact alarm
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        dueDate,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    dueDate,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Handle permission denied for exact alarms
            e.printStackTrace()
        }
    }

    /**
     * Schedule reminder alarm (e.g., 3 days before due date)
     */
    fun scheduleBillReminderAlarm(
        context: Context,
        billId: String,
        billTitle: String,
        groupName: String,
        amount: Long,
        reminderDate: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = createAlarmIntent(context, billId, "BILL_REMINDER").apply {
            putExtra("billTitle", billTitle)
            putExtra("groupName", groupName)
            putExtra("amount", amount)
            putExtra("reminderDate", reminderDate)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_BILL_REMINDER + billId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderDate,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderDate,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminderDate,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * Cancel alarm for a specific bill
     */
    fun cancelBillAlarm(context: Context, billId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Cancel due date alarm
        val dueIntent = createAlarmIntent(context, billId, "BILL_DUE")
        val duePendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_BILL_DUE + billId.hashCode(),
            dueIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(duePendingIntent)
        
        // Cancel reminder alarm
        val reminderIntent = createAlarmIntent(context, billId, "BILL_REMINDER")
        val reminderPendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_BILL_REMINDER + billId.hashCode(),
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(reminderPendingIntent)
    }

    /**
     * Reschedule all alarms (called on app restart or after permission changes)
     */
    suspend fun rescheduleAllAlarms(context: Context) {
        // This would typically be called with a repository to get all active bills
        // For now, we'll implement the structure
        // In a real implementation, you'd:
        // 1. Get all active bills from repository
        // 2. Cancel existing alarms
        // 3. Schedule new alarms based on due dates
    }

    /**
     * Create alarm intent
     */
    private fun createAlarmIntent(context: Context, billId: String, action: String): Intent {
        return Intent(context, TXAAlarmReceiver::class.java).apply {
            this.action = action
            putExtra("billId", billId)
        }
    }

    /**
     * Check if exact alarm permission is granted
     */
    fun hasExactAlarmPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Exact alarms don't require special permission before Android 12
        }
    }

    /**
     * Calculate reminder date (3 days before due date by default)
     */
    fun calculateReminderDate(dueDate: Long, daysBefore: Int = 3): Long {
        return dueDate - (daysBefore * 24 * 60 * 60 * 1000L)
    }

    /**
     * Broadcast receiver for handling alarm events
     */
    class TXAAlarmReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            val billId = intent.getStringExtra("billId") ?: return
            
            when (action) {
                "BILL_DUE" -> handleBillDueAlarm(context, intent)
                "BILL_REMINDER" -> handleBillReminderAlarm(context, intent)
                "BILL_OVERDUE" -> handleBillOverdueAlarm(context, intent)
            }
        }

        private fun handleBillDueAlarm(context: Context, intent: Intent) {
            val billTitle = intent.getStringExtra("billTitle") ?: return
            val groupName = intent.getStringExtra("groupName") ?: return
            val amount = intent.getLongExtra("amount", 0L)
            val dueDate = intent.getLongExtra("dueDate", 0L)
            val billId = intent.getStringExtra("billId") ?: return

            TXANotificationManager.showBillDueNotification(
                context = context,
                billTitle = billTitle,
                groupName = groupName,
                amount = amount,
                dueDate = dueDate,
                billId = billId
            )
        }

        private fun handleBillReminderAlarm(context: Context, intent: Intent) {
            val billTitle = intent.getStringExtra("billTitle") ?: return
            val groupName = intent.getStringExtra("groupName") ?: return
            val amount = intent.getLongExtra("amount", 0L)
            val reminderDate = intent.getLongExtra("reminderDate", 0L)
            val billId = intent.getStringExtra("billId") ?: return

            val daysLeft = calculateDaysLeft(reminderDate)

            TXANotificationManager.showPaymentReminderNotification(
                context = context,
                billTitle = billTitle,
                groupName = groupName,
                amount = amount,
                daysLeft = daysLeft,
                billId = billId
            )
        }

        private fun handleBillOverdueAlarm(context: Context, intent: Intent) {
            val billTitle = intent.getStringExtra("billTitle") ?: return
            val groupName = intent.getStringExtra("groupName") ?: return
            val amount = intent.getLongExtra("amount", 0L)
            val billId = intent.getStringExtra("billId") ?: return

            val overdueDays = calculateOverdueDays(intent.getLongExtra("dueDate", 0L))

            TXANotificationManager.showOverdueBillNotification(
                context = context,
                billTitle = billTitle,
                groupName = groupName,
                amount = amount,
                overdueDays = overdueDays,
                billId = billId
            )
        }

        private fun calculateDaysLeft(targetDate: Long): Int {
            val now = System.currentTimeMillis()
            return if (targetDate > now) {
                java.util.concurrent.TimeUnit.MILLISECONDS.toDays(targetDate - now).toInt()
            } else {
                0
            }
        }

        private fun calculateOverdueDays(dueDate: Long): Int {
            val now = System.currentTimeMillis()
            return if (dueDate < now) {
                java.util.concurrent.TimeUnit.MILLISECONDS.toDays(now - dueDate).toInt()
            } else {
                0
            }
        }
    }
}
