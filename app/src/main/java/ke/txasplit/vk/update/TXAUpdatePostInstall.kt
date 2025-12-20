/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAUpdatePostInstall.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import ke.txasplit.vk.R
import ke.txasplit.vk.TXAConstants
import ke.txasplit.vk.txa

object TXAUpdatePostInstall {

    fun maybeShowUpdateSuccessDialog(activity: AppCompatActivity) {
        val prefs = activity.getSharedPreferences(TXAConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val pending = prefs.getBoolean(TXAConstants.PREF_UPDATE_INSTALLED_PENDING, false)
        if (!pending) return

        val targetVersionCode = prefs.getInt(TXAConstants.PREF_UPDATE_VERSION_CODE, 0)
        if (targetVersionCode <= 0) return

        val currentVersionCode = getCurrentVersionCode(activity)
        if (currentVersionCode < targetVersionCode) {
            // Chưa cài xong (hoặc user huỷ cài đặt) -> giữ cờ để lần sau kiểm tra lại
            return
        }

        val versionName = prefs.getString(TXAConstants.PREF_UPDATE_VERSION_NAME, "")?.ifBlank { null }

        val view = LayoutInflater.from(activity).inflate(R.layout.txa_dialog_update_success, null, false)
        val msg = view.findViewById<TextView>(R.id.txaUpdateSuccessMessage)
        msg.text = if (versionName != null) {
            txa("txasplit_update_success_message", versionName)
        } else {
            txa("txasplit_update_success_message_generic")
        }

        AlertDialog.Builder(activity)
            .setTitle(txa("txasplit_update_success_title"))
            .setView(view)
            .setCancelable(false)
            .setPositiveButton(txa("txasplit_ok")) { _, _ ->
                // Cleanup APK + reset flags
                TXAInstall.cleanupPendingApk(activity)
                prefs.edit()
                    .putBoolean(TXAConstants.PREF_UPDATE_INSTALLED_PENDING, false)
                    .remove(TXAConstants.PREF_UPDATE_VERSION_CODE)
                    .remove(TXAConstants.PREF_UPDATE_VERSION_NAME)
                    .apply()
            }
            .show()
    }

    private fun getCurrentVersionCode(context: Context): Int {
        val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode
        }
    }
}
