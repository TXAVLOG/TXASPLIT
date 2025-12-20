/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAUpdateNotificationManager.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ke.txasplit.vk.R
import ke.txasplit.vk.txa
import ke.txasplit.vk.update.TXAUpdateService

/**
 * Manager for handling update notifications
 * Provides notifications for update availability, download progress, and installation
 */
object TXAUpdateNotificationManager {
    
    /**
     * Notification channel IDs
     */
    const val CHANNEL_UPDATE_AVAILABLE = "txa_update_available"
    const val CHANNEL_UPDATE_DOWNLOAD = "txa_update_download"
    const val CHANNEL_UPDATE_COMPLETE = "txa_update_complete"
    
    /**
     * Notification IDs
     */
    const val NOTIFICATION_UPDATE_AVAILABLE = 3001
    const val NOTIFICATION_UPDATE_DOWNLOADING = 3002
    const val NOTIFICATION_UPDATE_COMPLETE = 3003
    
    /**
     * Action constants
     */
    const val ACTION_DOWNLOAD_UPDATE = "ke.txasplit.vk.ACTION_DOWNLOAD_UPDATE"
    const val ACTION_OPEN_APP = "ke.txasplit.vk.ACTION_OPEN_APP"
    const val ACTION_INSTALL_UPDATE = "ke.txasplit.vk.ACTION_INSTALL_UPDATE"
    const val ACTION_RETRY_DOWNLOAD = "ke.txasplit.vk.ACTION_RETRY_DOWNLOAD"
    const val ACTION_DISMISS_NOTIFICATION = "ke.txasplit.vk.ACTION_DISMISS_NOTIFICATION"
    
    /**
     * Extra keys
     */
    const val EXTRA_VERSION_NAME = "version_name"
    const val EXTRA_VERSION_CODE = "version_code"
    const val EXTRA_DOWNLOAD_URL = "download_url"
    const val EXTRA_APK_PATH = "apk_path"
    
    /**
     * Initialize notification channels
     */
    fun initializeChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Update Available Channel - High importance
            val updateAvailableChannel = NotificationChannel(
                CHANNEL_UPDATE_AVAILABLE,
                txa("txa_notification_update_available"),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = txa("txa_notification_update_available_description")
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            
            // Update Download Channel - Default importance
            val updateDownloadChannel = NotificationChannel(
                CHANNEL_UPDATE_DOWNLOAD,
                txa("txa_notification_update_download"),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = txa("txa_notification_update_download_description")
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
            }
            
            // Update Complete Channel - High importance
            val updateCompleteChannel = NotificationChannel(
                CHANNEL_UPDATE_COMPLETE,
                txa("txa_notification_update_complete"),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = txa("txa_notification_update_complete_description")
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannels(listOf(
                updateAvailableChannel,
                updateDownloadChannel,
                updateCompleteChannel
            ))
        }
    }
    
    /**
     * Show update available notification
     */
    fun showUpdateAvailableNotification(
        context: Context,
        versionName: String,
        versionCode: Int,
        downloadUrl: String,
        changelog: String? = null
    ) {
        val downloadIntent = Intent(context, UpdateActionReceiver::class.java).apply {
            action = ACTION_DOWNLOAD_UPDATE
            putExtra(EXTRA_VERSION_NAME, versionName)
            putExtra(EXTRA_VERSION_CODE, versionCode)
            putExtra(EXTRA_DOWNLOAD_URL, downloadUrl)
        }
        val downloadPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            downloadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val openAppIntent = Intent(context, UpdateActionReceiver::class.java).apply {
            action = ACTION_OPEN_APP
        }
        val openAppPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATE_AVAILABLE)
            .setSmallIcon(getNotificationIcon(context))
            .setContentTitle(txa("txa_notification_update_available_title", versionName))
            .setContentText(txa("txa_notification_update_available_content"))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(changelog ?: txa("txa_notification_update_available_big_content", versionName)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .addAction(
                R.drawable.ic_download,
                txa("txa_notification_download"),
                downloadPendingIntent
            )
            .addAction(
                R.drawable.ic_open_app,
                txa("txa_notification_open_app"),
                openAppPendingIntent
            )
            .build()
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_UPDATE_AVAILABLE, notification)
    }
    
    /**
     * Show download progress notification
     */
    fun showDownloadProgressNotification(
        context: Context,
        versionName: String,
        progress: Int,
        downloaded: Long,
        total: Long,
        speed: String
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATE_DOWNLOAD)
            .setSmallIcon(getNotificationIcon(context))
            .setContentTitle(txa("txa_notification_downloading_update", versionName))
            .setContentText(txa("txa_notification_download_progress", formatBytes(downloaded), formatBytes(total), speed))
            .setProgress(100, progress, false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_UPDATE_DOWNLOADING, notification)
    }
    
    /**
     * Show download success notification
     */
    fun showDownloadSuccessNotification(
        context: Context,
        versionName: String,
        apkPath: String
    ) {
        // Cancel download progress notification
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_UPDATE_DOWNLOADING)
        
        val installIntent = Intent(context, UpdateActionReceiver::class.java).apply {
            action = ACTION_INSTALL_UPDATE
            putExtra(EXTRA_APK_PATH, apkPath)
            putExtra(EXTRA_VERSION_NAME, versionName)
        }
        val installPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val dismissIntent = Intent(context, UpdateActionReceiver::class.java).apply {
            action = ACTION_DISMISS_NOTIFICATION
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            3,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATE_COMPLETE)
            .setSmallIcon(getNotificationIcon(context))
            .setContentTitle(txa("txa_notification_download_success_title"))
            .setContentText(txa("txa_notification_download_success_content", versionName))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(txa("txa_notification_download_success_big_content", versionName)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .addAction(
                R.drawable.ic_install,
                txa("txa_notification_install"),
                installPendingIntent
            )
            .addAction(
                R.drawable.ic_close,
                txa("txa_notification_dismiss"),
                dismissPendingIntent
            )
            .build()
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_UPDATE_COMPLETE, notification)
    }
    
    /**
     * Show download failed notification
     */
    fun showDownloadFailedNotification(
        context: Context,
        versionName: String,
        downloadUrl: String,
        errorMessage: String
    ) {
        // Cancel download progress notification
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_UPDATE_DOWNLOADING)
        
        val retryIntent = Intent(context, UpdateActionReceiver::class.java).apply {
            action = ACTION_RETRY_DOWNLOAD
            putExtra(EXTRA_VERSION_NAME, versionName)
            putExtra(EXTRA_DOWNLOAD_URL, downloadUrl)
        }
        val retryPendingIntent = PendingIntent.getBroadcast(
            context,
            4,
            retryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val dismissIntent = Intent(context, UpdateActionReceiver::class.java).apply {
            action = ACTION_DISMISS_NOTIFICATION
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            5,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATE_COMPLETE)
            .setSmallIcon(getNotificationIcon(context))
            .setContentTitle(txa("txa_notification_download_failed_title"))
            .setContentText(txa("txa_notification_download_failed_content"))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(txa("txa_notification_download_failed_big_content", errorMessage)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .addAction(
                R.drawable.ic_retry,
                txa("txa_notification_retry"),
                retryPendingIntent
            )
            .addAction(
                R.drawable.ic_close,
                txa("txa_notification_dismiss"),
                dismissPendingIntent
            )
            .build()
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_UPDATE_COMPLETE, notification)
    }
    
    /**
     * Show download started notification
     */
    fun showDownloadStarted(context: Context, versionName: String) {
        showDownloadProgressNotification(context, versionName, 0, 0, 0, "")
    }
    
    /**
     * Show download complete notification
     */
    fun showDownloadComplete(context: Context, versionName: String, apkPath: String) {
        showDownloadSuccessNotification(context, versionName, apkPath)
    }
    
    /**
     * Show download failed notification
     */
    fun showDownloadFailed(context: Context, versionName: String, errorMessage: String) {
        showDownloadFailedNotification(context, versionName, "", errorMessage)
    }
    
    /**
     * Update download status text
     */
    fun updateDownloadStatus(context: Context, status: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATE_DOWNLOAD)
            .setSmallIcon(getNotificationIcon(context))
            .setContentTitle(status)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_UPDATE_DOWNLOADING, notification)
    }
    
    /**
     * Update download progress
     */
    fun updateDownloadProgress(
        context: Context,
        versionName: String,
        progress: Int,
        progressText: String,
        etaText: String
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATE_DOWNLOAD)
            .setSmallIcon(getNotificationIcon(context))
            .setContentTitle(txa("txa_notification_downloading_update", versionName))
            .setContentText(progressText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$progressText\n$etaText"))
            .setProgress(100, progress, false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_UPDATE_DOWNLOADING, notification)
    }
    
    /**
     * Cancel all update notifications
     */
    fun cancelAll(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(NOTIFICATION_UPDATE_AVAILABLE)
        notificationManager.cancel(NOTIFICATION_UPDATE_DOWNLOADING)
        notificationManager.cancel(NOTIFICATION_UPDATE_COMPLETE)
    }
    
    /**
     * Get notification icon based on screen density
     */
    private fun getNotificationIcon(context: Context): Int {
        return R.drawable.ic_stat_txaboard
    }
    
    /**
     * Format bytes to human-readable string
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
    
    /**
     * Broadcast receiver for handling notification actions
     */
    class UpdateActionReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_DOWNLOAD_UPDATE -> {
                    val versionName = intent.getStringExtra(EXTRA_VERSION_NAME) ?: return
                    val downloadUrl = intent.getStringExtra(EXTRA_DOWNLOAD_URL) ?: return
                    
                    // Cancel update available notification
                    NotificationManagerCompat.from(context).cancel(NOTIFICATION_UPDATE_AVAILABLE)
                    
                    // Start download service
                    val serviceIntent = Intent(context, TXAUpdateService::class.java).apply {
                        action = TXAUpdateService.ACTION_START_DOWNLOAD
                        putExtra(EXTRA_VERSION_NAME, versionName)
                        putExtra(EXTRA_DOWNLOAD_URL, downloadUrl)
                    }
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
                
                ACTION_OPEN_APP -> {
                    // Cancel update available notification
                    NotificationManagerCompat.from(context).cancel(NOTIFICATION_UPDATE_AVAILABLE)
                    
                    // Open main activity
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    context.startActivity(launchIntent)
                }
                
                ACTION_INSTALL_UPDATE -> {
                    val apkPath = intent.getStringExtra(EXTRA_APK_PATH) ?: return
                    
                    // Cancel complete notification
                    NotificationManagerCompat.from(context).cancel(NOTIFICATION_UPDATE_COMPLETE)
                    
                    // Install APK
                    TXAUpdateService.installApk(context, apkPath)
                }
                
                ACTION_RETRY_DOWNLOAD -> {
                    val versionName = intent.getStringExtra(EXTRA_VERSION_NAME) ?: return
                    val downloadUrl = intent.getStringExtra(EXTRA_DOWNLOAD_URL) ?: return
                    
                    // Cancel failed notification
                    NotificationManagerCompat.from(context).cancel(NOTIFICATION_UPDATE_COMPLETE)
                    
                    // Start download service
                    val serviceIntent = Intent(context, TXAUpdateService::class.java).apply {
                        action = TXAUpdateService.ACTION_START_DOWNLOAD
                        putExtra(EXTRA_VERSION_NAME, versionName)
                        putExtra(EXTRA_DOWNLOAD_URL, downloadUrl)
                    }
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
                
                ACTION_DISMISS_NOTIFICATION -> {
                    // Cancel all notifications
                    val notificationManager = NotificationManagerCompat.from(context)
                    notificationManager.cancel(NOTIFICATION_UPDATE_AVAILABLE)
                    notificationManager.cancel(NOTIFICATION_UPDATE_DOWNLOADING)
                    notificationManager.cancel(NOTIFICATION_UPDATE_COMPLETE)
                }
            }
        }
    }
}
