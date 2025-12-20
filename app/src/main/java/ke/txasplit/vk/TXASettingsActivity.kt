/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXASettingsActivity.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.appset.AppSet
import ke.txasplit.vk.core.TXATranslation
import ke.txasplit.vk.databinding.TxaActivitySettingsBinding
import ke.txasplit.vk.update.TXAUpdateDelegate
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class TXASettingsActivity : AppCompatActivity() {

    private lateinit var vb: TxaActivitySettingsBinding
    private val updateDelegate by lazy { TXAUpdateDelegate(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = TxaActivitySettingsBinding.inflate(layoutInflater)
        setContentView(vb.root)

        vb.toolbar.title = txa("txasplit_settings")
        vb.toolbar.setNavigationOnClickListener { finish() }

        vb.btnLanguage.text = txa("txasplit_language")
        vb.btnCheckUpdate.text = txa("txasplit_check_update")
        vb.btnViewUpdateLogs.text = txa("txasplit_update_view_logs")
        vb.btnExport.text = txa("txasplit_export_data")

        vb.txtVersion.text = txa("txasplit_version_display", packageManager.getPackageInfo(packageName, 0).versionName)
        vb.txtAppSet.text = txa("txasplit_app_set_id_loading")

        loadAppSetId()

        vb.btnLanguage.setOnClickListener {
            showLanguagePicker()
        }

        vb.btnCheckUpdate.setOnClickListener {
            updateDelegate.checkNow(manual = true)
        }

        vb.btnViewUpdateLogs.setOnClickListener {
            openUpdateLogsViewer()
        }

        vb.btnExport.setOnClickListener {
            Toast.makeText(this, txa("txasplit_backup_data"), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Auto-resume update flow sau khi user cấp quyền Unknown Sources
        updateDelegate.onResume()
    }

    private fun loadAppSetId() {
        val client = AppSet.getClient(this)
        client.appSetIdInfo
            .addOnSuccessListener { info ->
                vb.txtAppSet.text = txa("txasplit_app_set_id", info.id)
            }
            .addOnFailureListener {
                vb.txtAppSet.text = txa("txasplit_app_set_id", txa("txasplit_unknown_error"))
            }
    }

    private fun showLanguagePicker() {
        // Hiển thị loading dialog
        val loadingDialog = AlertDialog.Builder(this)
            .setMessage(txa("txasplit_language_checking"))
            .setCancelable(false)
            .create()
        loadingDialog.show()

        lifecycleScope.launch {
            try {
                // Load danh sách locales từ API
                val availableLocales = TXATranslation.getAvailableLocales(this@TXASettingsActivity)
                
                // Tạo options với System Default + các locales từ API
                val options = mutableListOf<Pair<String, String>>(
                    "" to txa("txasplit_language_system_default")
                )
                
                // Map locale code sang display name (chỉ hiển thị en, vi, ja)
                availableLocales.forEach { locale ->
                    if (locale !in listOf("en", "vi", "ja")) return@forEach
                    val displayName = when (locale) {
                        "en" -> txa("txasplit_lang_en")
                        "vi" -> txa("txasplit_lang_vi")
                        "ja" -> txa("txasplit_lang_ja")
                        else -> locale.uppercase()
                    }
                    options.add(locale to displayName)
                }

                loadingDialog.dismiss()

                val current = TXATranslation.getSavedLocaleTag(this@TXASettingsActivity)
                val currentIndex = options.indexOfFirst { it.first == current }.coerceAtLeast(0)

                AlertDialog.Builder(this@TXASettingsActivity)
                    .setTitle(txa("txasplit_language"))
                    .setSingleChoiceItems(options.map { it.second }.toTypedArray(), currentIndex) { dialog, which ->
                        handleLocaleSelection(options[which].first)
                        dialog.dismiss()
                    }
                    .setNegativeButton(txa("txasplit_cancel"), null)
                    .show()
            } catch (e: Exception) {
                loadingDialog.dismiss()
                // Fallback về danh sách mặc định nếu API fail
                showLanguagePickerFallback()
            }
        }
    }
    
    private fun showLanguagePickerFallback() {
        // Fallback: dùng danh sách mặc định (chỉ en, vi, ja)
        val options = listOf(
            "" to txa("txasplit_language_system_default"),
            "en" to txa("txasplit_lang_en"),
            "vi" to txa("txasplit_lang_vi"),
            "ja" to txa("txasplit_lang_ja"),
        )

        val current = TXATranslation.getSavedLocaleTag(this)
        val currentIndex = options.indexOfFirst { it.first == current }.coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle(txa("txasplit_language"))
            .setSingleChoiceItems(options.map { it.second }.toTypedArray(), currentIndex) { dialog, which ->
                handleLocaleSelection(options[which].first)
                dialog.dismiss()
            }
            .setNegativeButton(txa("txasplit_cancel"), null)
            .show()
    }

    private fun handleLocaleSelection(localeTag: String) {
        val oldLocale = TXATranslation.getSavedLocaleTag(this)
        TXATranslation.saveLocaleTag(this, localeTag)

        if (localeTag == oldLocale) return

        lifecycleScope.launch {
            val effectiveLocale = if (localeTag.isBlank()) {
                TXATranslation.resolvePreferredLocale(this@TXASettingsActivity)
            } else {
                localeTag
            }
            TXATranslation.init(this@TXASettingsActivity)
            TXATranslation.syncIfNewer(this@TXASettingsActivity, effectiveLocale)
            restartApp()
        }
    }

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finishAffinity()
    }

    private fun openUpdateLogsViewer() {
        lifecycleScope.launch {
            val logResult = try {
                withContext(Dispatchers.IO) {
                    val file = findLatestUpdateLogFile() ?: return@withContext null
                    val content = file.takeIf { it.exists() }?.readText()
                    content?.let { file to it }
                }
            } catch (t: Throwable) {
                Toast.makeText(this@TXASettingsActivity, txa("txasplit_update_logs_read_error"), Toast.LENGTH_LONG).show()
                return@launch
            }

            if (logResult == null) {
                Toast.makeText(this@TXASettingsActivity, txa("txasplit_update_logs_not_found"), Toast.LENGTH_LONG).show()
                return@launch
            }

            val (file, content) = logResult
            val textView = TextView(this@TXASettingsActivity).apply {
                setPadding(32, 32, 32, 32)
                setTextIsSelectable(true)
                text = content
            }
            val scrollView = ScrollView(this@TXASettingsActivity).apply {
                addView(textView)
            }

            AlertDialog.Builder(this@TXASettingsActivity)
                .setTitle(file.name)
                .setView(scrollView)
                .setPositiveButton(txa("txasplit_ok"), null)
                .show()
        }
    }

    private fun findLatestUpdateLogFile(): File? {
        val candidates = mutableListOf<File>()

        fun collect(dir: File?) {
            if (dir == null || !dir.exists()) return
            val files = dir.listFiles { _, name ->
                name.startsWith("update_check_log_") && name.endsWith(".txt")
            } ?: return
            candidates += files
        }

        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            collect(File(downloadsDir, "TXASPLIT"))
        } catch (_: Throwable) {
        }

        val appFilesDir = getExternalFilesDir(null) ?: filesDir
        collect(File(appFilesDir, "logs"))

        return candidates.maxByOrNull { it.lastModified() }
    }

    // Update flow đã được chuyển sang TXAUpdateDelegate (Phase 1→4 + UI đầy đủ).
}
