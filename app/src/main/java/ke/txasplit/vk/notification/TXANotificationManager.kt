/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXANotificationManager.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ke.txasplit.vk.R
import ke.txasplit.vk.txa

/**
 * Manager for handling all notifications in TXASplit
 * Provides categorized notification channels for different types of alerts
 */
object TXANotificationManager {
    
    /**
     * Notification channel IDs
     */
    const val CHANNEL_BILLS_DUE = "txa_bills_due"
    const val CHANNEL_PAYMENT_REMINDERS = "txa_payment_reminders"
    const val CHANNEL_OVERDUE_BILLS = "txa_overdue_bills"
    const val CHANNEL_PAYMENT_VERIFICATION = "txa_payment_verification"
    const val CHANNEL_GROUP_UPDATES = "txa_group_updates"
    
    /**
     * Notification request codes
     */
    private const val NOTIFICATION_BILL_DUE = 1001
    private const val NOTIFICATION_PAYMENT_REMINDER = 1002
    private const val NOTIFICATION_OVERDUE_BILL = 1003
    private const val NOTIFICATION_PAYMENT_VERIFICATION = 1004
    private const val NOTIFICATION_GROUP_UPDATE = 1005
    
    /**
     * Initialize notification channels (called on app startup)
     */
    fun initializeChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Bills Due Channel - High importance
            val billsDueChannel = NotificationChannel(
                CHANNEL_BILLS_DUE,
                txa("txa_notification_bills_due"),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = txa("txa_notification_bills_due_description")
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            
            // Payment Reminders Channel - Default importance
            val paymentRemindersChannel = NotificationChannel(
                CHANNEL_PAYMENT_REMINDERS,
                txa("txa_notification_payment_reminders"),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = txa("txa_notification_payment_reminders_description")
                enableLights(true)
                enableVibration(false)
                setShowBadge(true)
            }
            
            // Overdue Bills Channel - Urgent importance
            val overdueBillsChannel = NotificationChannel(
                CHANNEL_OVERDUE_BILLS,
                txa("txa_notification_overdue_bills"),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = txa("txa_notification_overdue_bills_description")
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            
            // Payment Verification Channel - Default importance
            val paymentVerificationChannel = NotificationChannel(
                CHANNEL_PAYMENT_VERIFICATION,
                txa("txa_notification_payment_verification"),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = txa("txa_notification_payment_verification_description")
                enableLights(true)
                enableVibration(false)
                setShowBadge(true)
            }
            
            // Group Updates Channel - Low importance
            val groupUpdatesChannel = NotificationChannel(
                CHANNEL_GROUP_UPDATES,
                txa("txa_notification_group_updates"),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = txa("txa_notification_group_updates_description")
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
            }
            
            notificationManager.createNotificationChannels(listOf(
                billsDueChannel,
                paymentRemindersChannel,
                overdueBillsChannel,
                paymentVerificationChannel,
                groupUpdatesChannel
            ))
        }
    }
    
    /**
     * Show bill due notification
     */
    fun showBillDueNotification(
        context: Context,
        billTitle: String,
        groupName: String,
        amount: Long,
        dueDate: Long,
        billId: String
    ) {
        // DISABLED: Missing Activity
        /*
        // DISABLED: Missing Activity
        /*
        val intent = createBillDetailIntent(context, billId)
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_BILL_DUE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        */
        val pendingIntent: PendingIntent? = null
        */
        val pendingIntent: PendingIntent? = null
        
        val notification = NotificationCompat.Builder(context, CHANNEL_BILLS_DUE)
            .setSmallIcon(R.drawable.ic_notification_bill)
            .setContentTitle(txa("txa_notification_bill_due_title"))
            .setContentText(txa("txa_notification_bill_due_content", billTitle, groupName))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(txa("txa_notification_bill_due_big_content", billTitle, groupName, formatAmount(amount), formatDate(dueDate))))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            // DISABLED: Missing Activity
            /*.addAction(
                R.drawable.ic_payment,
                txa("txa_notification_pay_now"),
                createPaymentIntent(context, billId)
            )*/
            .build()
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_BILL_DUE, notification)
    }
    
    /**
     * Show payment reminder notification
     */
    fun showPaymentReminderNotification(
        context: Context,
        billTitle: String,
        groupName: String,
        amount: Long,
        daysLeft: Int,
        billId: String
    ) {
        // DISABLED: Missing Activity
        val pendingIntent: PendingIntent? = null
        
        val notification = NotificationCompat.Builder(context, CHANNEL_PAYMENT_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle(txa("txa_notification_payment_reminder_title"))
            .setContentText(txa("txa_notification_payment_reminder_content", billTitle, daysLeft))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(txa("txa_notification_payment_reminder_big_content", billTitle, groupName, formatAmount(amount), daysLeft)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            // DISABLED: Missing Activity
            /*.addAction(
                R.drawable.ic_payment,
                txa("txa_notification_pay_now"),
                createPaymentIntent(context, billId)
            )*/
            .build()
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_PAYMENT_REMINDER, notification)
    }
    
    /**
     * Show overdue bill notification
     */
    fun showOverdueBillNotification(
        context: Context,
        billTitle: String,
        groupName: String,
        amount: Long,
        overdueDays: Int,
        billId: String
    ) {
        // DISABLED: Missing Activity
        /*
        val intent = createBillDetailIntent(context, billId)
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_OVERDUE_BILL,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        */
        val pendingIntent: PendingIntent? = null
        
        val notification = NotificationCompat.Builder(context, CHANNEL_OVERDUE_BILLS)
            .setSmallIcon(R.drawable.ic_notification_overdue)
            .setContentTitle(txa("txa_notification_overdue_bill_title"))
            .setContentText(txa("txa_notification_overdue_bill_content", billTitle, overdueDays))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(txa("txa_notification_overdue_bill_big_content", billTitle, groupName, formatAmount(amount), overdueDays)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            // DISABLED: Missing Activity
            /*.addAction(
                R.drawable.ic_payment,
                txa("txa_notification_pay_now"),
                createPaymentIntent(context, billId)
            )*/
            .build()
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_OVERDUE_BILL, notification)
    }
    
    /**
     * Show payment verification notification
     */
    fun showPaymentVerificationNotification(
        context: Context,
        payerName: String,
        billTitle: String,
        amount: Long,
        paymentId: String
    ) {
        // DISABLED: Missing Activity
        val pendingIntent: PendingIntent? = null
        
        val notification = NotificationCompat.Builder(context, CHANNEL_PAYMENT_VERIFICATION)
            .setSmallIcon(R.drawable.ic_notification_verification)
            .setContentTitle(txa("txa_notification_payment_verification_title"))
            .setContentText(txa("txa_notification_payment_verification_content", payerName, billTitle))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(txa("txa_notification_payment_verification_big_content", payerName, billTitle, formatAmount(amount))))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            // DISABLED: Missing Activity
            /*.addAction(
                R.drawable.ic_check,
                txa("txa_notification_verify"),
                createVerifyIntent(context, paymentId)
            )*/
            .build()
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_PAYMENT_VERIFICATION, notification)
    }
    
    /**
     * Create intent for bill detail screen - DISABLED: Missing Activity
     */
    /*
    private fun createBillDetailIntent(context: Context, billId: String): Intent {
        return Intent(context, ke.txasplit.vk.ui.TXABillDetailActivity::class.java).apply {
            putExtra("billId", billId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }
    */
    
    /*
     * Create intent for payment detail screen - DISABLED: Missing Activity
     */
    /*
    private fun createPaymentDetailIntent(context: Context, paymentId: String): Intent {
        return Intent(context, ke.txasplit.vk.ui.TXAPaymentDetailActivity::class.java).apply {
            putExtra("paymentId", paymentId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }
    */
    
    /*
     * Create intent for payment action - DISABLED: Missing Activity
     */
    /*
    private fun createPaymentIntent(context: Context, billId: String): PendingIntent {
        val intent = Intent(context, ke.txasplit.vk.ui.TXAPaymentActivity::class.java).apply {
            putExtra("billId", billId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            NOTIFICATION_BILL_DUE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    */
    
    /*
     * Create intent for verification action - DISABLED: Missing Activity
     */
    /*
    private fun createVerifyIntent(context: Context, paymentId: String): PendingIntent {
        val intent = Intent(context, ke.txasplit.vk.ui.TXAVerificationActivity::class.java).apply {
            putExtra("paymentId", paymentId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            NOTIFICATION_PAYMENT_VERIFICATION,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    */
    
    /**
     * Format amount for display
     */
    private fun formatAmount(amount: Long): String {
        return String.format("%,d VND", amount)
    }
    
    /**
     * Format date for display
     */
    private fun formatDate(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        return format.format(date)
    }
}
