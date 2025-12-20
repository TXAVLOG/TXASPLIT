/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXASplashActivity.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ke.txasplit.vk.core.SyncResult
import ke.txasplit.vk.core.TXAAndroidVersion
import ke.txasplit.vk.core.TXATranslation
import ke.txasplit.vk.databinding.TxaActivitySplashBinding
import ke.txasplit.vk.update.TXAUpdatePostInstall
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TXASplashActivity : AppCompatActivity() {

    private lateinit var vb: TxaActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = TxaActivitySplashBinding.inflate(layoutInflater)
        setContentView(vb.root)

        // Đảm bảo translation đã được load trước khi hiển thị bất kỳ dialog nào
        // (kể cả khi Android version thấp) - init() sẽ load fallback nếu chưa có cache
        TXATranslation.init(this)

        // Nếu vừa cài update xong -> show dialog thành công + cleanup APK
        TXAUpdatePostInstall.maybeShowUpdateSuccessDialog(this)

        if (Build.VERSION.SDK_INT < 34) {
            // Trên Android < 14: Vẫn cần sync translation từ cache/API trước khi hiển thị modal
            // để đảm bảo modal hiển thị đúng ngôn ngữ (không phải fallback default)
            lifecycleScope.launch {
                val locale = TXATranslation.resolvePreferredLocale(this@TXASplashActivity)
                // Sync từ cache hoặc API (nếu có internet)
                TXATranslation.syncIfNewer(this@TXASplashActivity, locale)
                // Sau khi sync xong mới hiển thị modal
                showUnsupportedVersionAndExit()
            }
            return
        }

        vb.status.text = txa("txasplit_splash_checking")

        lifecycleScope.launch {
            // Cache-first already loaded in Application.
            val locale = TXATranslation.resolvePreferredLocale(this@TXASplashActivity)

            vb.status.text = txa("txasplit_splash_checking")
            val result = TXATranslation.syncIfNewer(this@TXASplashActivity, locale)

            when (result) {
                is SyncResult.Updated -> vb.status.text = txa("txasplit_splash_downloading")
                is SyncResult.UpToDate -> vb.status.text = txa("txasplit_splash_ready")
                is SyncResult.Error -> vb.status.text = txa("txasplit_splash_error", result.message)
            }

            delay(300)
            startActivity(Intent(this@TXASplashActivity, TXAMainActivity::class.java))
            finish()
        }
    }

    private fun showUnsupportedVersionAndExit() {
        val versionName = TXAAndroidVersion.currentMarketingName()
        val message = txa("txasplit_version_guard_error", versionName)

        AlertDialog.Builder(this)
            .setTitle(txa("txasplit_version_guard_title"))
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(txa("txasplit_ok")) { _, _ ->
                finishAffinity()
            }
            .show()
    }
}
