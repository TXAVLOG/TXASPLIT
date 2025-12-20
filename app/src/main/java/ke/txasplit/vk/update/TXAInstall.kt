/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAInstall.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import ke.txasplit.vk.TXAConstants
import java.io.File

object TXAInstall {

    fun installApk(context: Context, apkFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            apkFile,
        )

        // Remember temp apk to cleanup next launch.
        context.getSharedPreferences(TXAConstants.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(TXAConstants.PREF_PENDING_APK_PATH, apkFile.absolutePath)
            .apply()

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(intent)
    }

    fun cleanupPendingApk(context: Context) {
        val prefs = context.getSharedPreferences(TXAConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val path = prefs.getString(TXAConstants.PREF_PENDING_APK_PATH, null)
        if (path.isNullOrBlank()) return

        runCatching {
            val f = File(path)
            if (f.exists()) f.delete()
        }

        prefs.edit().remove(TXAConstants.PREF_PENDING_APK_PATH).apply()
    }
}
