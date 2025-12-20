/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAApp.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.work.WorkManager
import ke.txasplit.vk.core.TXAPermissions
import ke.txasplit.vk.core.TXATranslation
import ke.txasplit.vk.update.TXAUpdateScheduler

class TXAApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // NOTE: Cleanup APK sẽ được thực hiện sau khi verify update thành công (post-install),
        // để tránh xoá file APK trước khi user hoàn tất cài đặt.

        // Apply app locale ASAP (so UI can recreate with chosen locale).
        val localeTag = TXATranslation.getSavedLocaleTag(this)
        if (localeTag.isNotBlank()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeTag))
        } else {
            // Reset về system default locale
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        }

        // Init translation cache (cache-first).
        TXATranslation.init(this)

        // Cancel legacy periodic worker (12h) nếu còn tồn tại.
        WorkManager.getInstance(this).cancelUniqueWork("txa_update_worker")

        // Background update check (3 phút/lần) - chỉ schedule nếu đã cấp đủ quyền.
        if (TXAPermissions.hasAllFilesAccess(this) && TXAPermissions.isIgnoringBatteryOptimizations(this)) {
            TXAUpdateScheduler.ensureScheduledNow(this)
        } else {
            TXAUpdateScheduler.cancel(this)
        }
    }
}
