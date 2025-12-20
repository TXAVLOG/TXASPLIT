/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAConstants.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk

object TXAConstants {
    const val BASE_URL: String = "https://soft.nrotxa.online/txasplit/api/"

    const val LANG_DIR_NAME: String = "languages"
    const val UPDATES_DIR_NAME: String = "updates"

    const val PREFS_NAME: String = "txa_prefs"
    const val PREF_LOCALE: String = "txa_locale"
    const val PREF_PENDING_APK_PATH: String = "txa_pending_apk_path"

    // Update flow (post-install verification)
    const val PREF_UPDATE_INSTALLED_PENDING: String = "txa_update_installed_pending"
    const val PREF_UPDATE_VERSION_CODE: String = "txa_update_version_code"
    const val PREF_UPDATE_VERSION_NAME: String = "txa_update_version_name"
}
