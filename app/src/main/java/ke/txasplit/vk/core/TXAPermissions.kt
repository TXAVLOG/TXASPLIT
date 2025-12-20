/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAPermissions.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.core

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.Manifest
import androidx.core.content.ContextCompat
import ke.txasplit.vk.txa

object TXAPermissions {

    // Permission types for easy reference
    enum class PermissionType {
        ALL_FILES_ACCESS,
        BATTERY_OPTIMIZATION,
        EXACT_ALARMS,
        POST_NOTIFICATIONS
    }

    /**
     * Android 11+ (API 30+): All files access ("Manage all files")
     * Doc: https://developer.android.com/training/data-storage/manage-all-files
     */
    fun hasAllFilesAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            // MinSdk 26: trên Android < 11 app có thể hoạt động không cần special access này.
            true
        }
    }

    fun createManageAllFilesAccessIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        val pkgUri = Uri.parse("package:${context.packageName}")
        return Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).setData(pkgUri)
    }

    /**
     * Battery optimization allowlist ("Ignore battery optimizations")
     * Doc: https://developer.android.com/training/monitoring-device-state/doze-standby
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun createRequestIgnoreBatteryOptimizationsIntent(context: Context): Intent? {
        val pkgUri = Uri.parse("package:${context.packageName}")
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(pkgUri)
    }

    /**
     * Không có intent "đúng app" 100% cho mọi máy, nhưng:
     * - ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS mở danh sách tối ưu hoá pin
     * - ACTION_APPLICATION_DETAILS_SETTINGS mở trang cài đặt app (từ đó vào Battery)
     */
    fun createBatteryOptimizationSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:${context.packageName}"))
    }

    /**
     * Android 12+ (API 31+): Exact alarms permission
     * Doc: https://developer.android.com/training/scheduling/alarms#exact-permission
     */
    fun hasExactAlarmsPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true // Android < 12 không cần permission
        }
    }

    fun createExactAlarmsIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).setData(Uri.parse("package:${context.packageName}"))
        } else {
            null
        }
    }

    /**
     * Android 13+ (API 33+): Post notifications permission
     * Doc: https://developer.android.com/develop/ui/views/notifications/notification-permission
     */
    fun hasPostNotificationsPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android < 13 không cần runtime permission
        }
    }

    /**
     * Check all required permissions for TXASplit core features
     */
    fun getAllPermissionsStatus(context: Context): Map<PermissionType, Boolean> {
        return mapOf(
            PermissionType.ALL_FILES_ACCESS to hasAllFilesAccess(context),
            PermissionType.BATTERY_OPTIMIZATION to isIgnoringBatteryOptimizations(context),
            PermissionType.EXACT_ALARMS to hasExactAlarmsPermission(context),
            PermissionType.POST_NOTIFICATIONS to hasPostNotificationsPermission(context)
        )
    }

    /**
     * Get missing permissions that need user action
     */
    fun getMissingPermissions(context: Context): List<PermissionType> {
        return getAllPermissionsStatus(context).filter { !it.value }.keys.toList()
    }

    /**
     * Get appropriate intent for permission request
     */
    fun getIntentForPermission(context: Context, type: PermissionType): Intent? {
        return when (type) {
            PermissionType.ALL_FILES_ACCESS -> createManageAllFilesAccessIntent(context)
            PermissionType.BATTERY_OPTIMIZATION -> createBatteryOptimizationSettingsIntent(context)
            PermissionType.EXACT_ALARMS -> createExactAlarmsIntent(context)
            PermissionType.POST_NOTIFICATIONS -> {
                // For notifications, we need to request runtime permission first
                // This will be handled in UI layer
                null
            }
        }
    }

    /**
     * Get user-friendly explanation for each permission
     */
    fun getPermissionExplanation(context: Context, type: PermissionType): String {
        return when (type) {
            PermissionType.ALL_FILES_ACCESS -> 
                txa("txasplit_perm_all_files_access_message")
            PermissionType.BATTERY_OPTIMIZATION -> 
                txa("txasplit_perm_battery_optimization_message")
            PermissionType.EXACT_ALARMS -> 
                txa("txasplit_perm_exact_alarms_message")
            PermissionType.POST_NOTIFICATIONS -> 
                txa("txasplit_perm_post_notifications_message")
        }
    }
}

