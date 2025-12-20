/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAUpdateDelegate.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ke.txasplit.vk.R
import ke.txasplit.vk.TXAConstants
import ke.txasplit.vk.core.TXAFormat
import ke.txasplit.vk.txa
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class TXAUpdateDelegate(
    private val activity: AppCompatActivity,
) {

    private var pendingForUnknownSources: TXAUpdateInfo? = null
    private var running: Boolean = false

    private var availableDialog: AlertDialog? = null
    private var permissionDialog: AlertDialog? = null
    private var progressDialog: AlertDialog? = null

    private var downloadJob: Job? = null

    fun onResume() {
        // Auto-resume Phase 3 sau khi user cấp quyền Unknown Sources
        val pending = pendingForUnknownSources
        if (pending != null && hasInstallPermission(activity)) {
            permissionDialog?.dismiss()
            permissionDialog = null
            pendingForUnknownSources = null
            startDownloadFlow(pending)
        }
    }

    fun checkNow(manual: Boolean) {
        if (running) return
        running = true

        activity.lifecycleScope.launch {
            try {
                val info = TXAUpdateManager.checkAndFetchUpdateInfo(activity)
                if (info == null) {
                    if (manual) {
                        showSimpleDialog(
                            title = txa("txasplit_update_title_no_update"),
                            message = txa("txasplit_update_no_update"),
                        )
                    }
                    return@launch
                }

                showUpdateAvailableDialog(info)
            } catch (_: Throwable) {
                if (manual) {
                    Toast.makeText(activity, txa("txasplit_error_generic"), Toast.LENGTH_LONG).show()
                }
            } finally {
                running = false
            }
        }
    }

    fun maybeAutoCheckOnAppOpen() {
        if (didAutoCheckThisProcess) return
        didAutoCheckThisProcess = true
        checkNow(manual = false)
    }

    private fun showUpdateAvailableDialog(info: TXAUpdateInfo) {
        if (availableDialog?.isShowing == true) return

        val view = LayoutInflater.from(activity).inflate(R.layout.txa_dialog_update_available, null, false)
        val txtVersion = view.findViewById<TextView>(R.id.txaUpdateVersion)
        val txtDate = view.findViewById<TextView>(R.id.txaUpdateDate)
        val webChangelog = view.findViewById<WebView>(R.id.txaUpdateChangelog)

        txtVersion.text = txa("txasplit_update_new_version", info.versionName, info.versionCode.toString())

        val date = info.releaseDate
        txtDate.text = if (!date.isNullOrBlank()) {
            txa("txasplit_update_release_date", date)
        } else {
            txa("txasplit_update_release_date_unknown")
        }

        // Build HTML content với footer tự động
        val changelogBody = info.changelog.ifBlank { txa("txasplit_update_changelog_empty") }
        val footerDate = if (!date.isNullOrBlank()) date else txa("txasplit_update_release_date_unknown")
        val footer = "<p style='text-align: center; color: #999; font-size: 12px; margin: 16px 0 8px 0;'>-----</p>" +
                "<p style='text-align: center; color: #666; font-size: 12px; margin: 8px 0;'>" +
                "(${info.versionName}, $footerDate) - ©️APP BY TXA" +
                "</p>"
        
        // Kiểm tra xem changelog đã có CSS chưa (có thẻ <style>, <link rel="stylesheet">, hoặc <html>/<head>)
        val hasCss = changelogBody.contains("<style", ignoreCase = true) ||
                changelogBody.contains("<link", ignoreCase = true) ||
                changelogBody.contains("<html", ignoreCase = true) ||
                changelogBody.contains("<head", ignoreCase = true)
        
        val htmlContent = if (hasCss) {
            // Changelog đã có CSS, thêm footer vào đúng vị trí (trước </body> hoặc cuối nếu không có)
            val bodyEndIndex = changelogBody.lastIndexOf("</body>", ignoreCase = true)
            if (bodyEndIndex >= 0) {
                // Có thẻ </body>, chèn footer trước đó
                changelogBody.substring(0, bodyEndIndex) + footer + changelogBody.substring(bodyEndIndex)
            } else {
                // Không có </body>, thêm footer vào cuối
                changelogBody + footer
            }
        } else {
            // Changelog chưa có CSS, wrap trong HTML với CSS mặc định
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        font-size: 14px;
                        line-height: 1.6;
                        color: #212121;
                        padding: 8px;
                        margin: 0;
                    }
                    p { margin: 8px 0; }
                    ul, ol { margin: 8px 0; padding-left: 24px; }
                    h1, h2, h3, h4, h5, h6 { margin: 12px 0 8px 0; }
                    code { background: #f5f5f5; padding: 2px 4px; border-radius: 3px; }
                    pre { background: #f5f5f5; padding: 12px; border-radius: 4px; overflow-x: auto; }
                    a { color: #1976D2; }
                    img { max-width: 100%; height: auto; }
                </style>
            </head>
            <body>
                $changelogBody
                $footer
            </body>
            </html>
            """.trimIndent()
        }

        webChangelog.settings.apply {
            javaScriptEnabled = false
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
        }
        webChangelog.setVerticalScrollBarEnabled(true)
        webChangelog.setHorizontalScrollBarEnabled(false)
        // WebViewClient để xử lý link (nếu có) - không mở browser ngoài
        webChangelog.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                // Không mở link trong browser ngoài, giữ trong WebView
                return false
            }
        }
        webChangelog.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)

        availableDialog = AlertDialog.Builder(activity)
            .setTitle(txa("txasplit_update_title_available", info.versionName))
            .setView(view)
            .setCancelable(true)
            .setNegativeButton(txa("txasplit_close")) { d, _ -> d.dismiss() }
            .setPositiveButton(txa("txasplit_update_download")) { _, _ ->
                // Phase 2: Permission guard
                if (!hasInstallPermission(activity)) {
                    showUnknownSourcesPermissionDialog(info)
                } else {
                    startDownloadFlow(info)
                }
            }
            .show()
    }

    private fun showUnknownSourcesPermissionDialog(info: TXAUpdateInfo) {
        if (permissionDialog?.isShowing == true) return

        val view = LayoutInflater.from(activity).inflate(R.layout.txa_dialog_update_permission, null, false)
        val msg = view.findViewById<TextView>(R.id.txaUpdatePermissionMessage)
        msg.text = txa("txasplit_update_unknown_sources_message")

        permissionDialog = AlertDialog.Builder(activity)
            .setTitle(txa("txasplit_update_unknown_sources_title"))
            .setView(view)
            .setCancelable(false)
            .setNegativeButton(txa("txasplit_close")) { d, _ ->
                pendingForUnknownSources = null
                d.dismiss()
            }
            .setPositiveButton(txa("txasplit_grant")) { _, _ ->
                pendingForUnknownSources = info
                openUnknownSourcesSettings(activity)
            }
            .show()
    }

    private fun startDownloadFlow(info: TXAUpdateInfo) {
        if (progressDialog?.isShowing == true) return

        val view = LayoutInflater.from(activity).inflate(R.layout.txa_dialog_update_progress, null, false)
        val bar = view.findViewById<ProgressBar>(R.id.txaUpdateProgressBar)
        val txtProgress = view.findViewById<TextView>(R.id.txaUpdateProgressText)
        val txtEta = view.findViewById<TextView>(R.id.txaUpdateEtaText)
        val txtStatus = view.findViewById<TextView>(R.id.txaUpdateStatusText)

        bar.progress = 0
        txtProgress.text = txa("txasplit_format_calculating")
        txtEta.text = txa("txasplit_format_calculating")
        txtStatus.visibility = View.GONE

        progressDialog = AlertDialog.Builder(activity)
            .setTitle(txa("txasplit_update_downloading"))
            .setView(view)
            .setCancelable(false)
            .create()

        progressDialog?.show()

        downloadJob?.cancel()
        downloadJob = activity.lifecycleScope.launch {
            try {
                val dest = getUpdateApkFile(activity)
                downloadWithRetry(
                    url = info.downloadUrl,
                    dest = dest,
                    bar = bar,
                    txtProgress = txtProgress,
                    txtEta = txtEta,
                    txtStatus = txtStatus,
                )

                // Validate downloaded APK file
                if (!TXADownload.validateApkFile(activity, dest)) {
                    // Clean up invalid file
                    if (dest.exists()) {
                        dest.delete()
                    }
                    throw IllegalStateException("Downloaded file is not a valid APK")
                }

                // Phase 4: Installation & post-install flag
                markInstallPending(activity, info, dest)

                progressDialog?.dismiss()
                progressDialog = null

                TXAInstall.installApk(activity, dest)
            } catch (t: Throwable) {
                progressDialog?.dismiss()
                progressDialog = null

                showSimpleDialog(
                    title = txa("txasplit_update_download_failed_title"),
                    message = txa("txasplit_update_download_failed_message", t.message ?: ""),
                )
            }
        }
    }

    private suspend fun downloadWithRetry(
        url: String,
        dest: File,
        bar: ProgressBar,
        txtProgress: TextView,
        txtEta: TextView,
        txtStatus: TextView,
    ) {
        var attempt = 0
        val maxRetries = 20

        while (true) {
            try {
                txtStatus.visibility = View.VISIBLE
                txtStatus.text = txa("txasplit_update_resolving_url")

                var lastUiMs = 0L
                TXADownload.downloadApk(ke.txasplit.vk.core.TXAHttp.client, url, dest)
                    .collect { p ->
                        val now = SystemClock.elapsedRealtime()
                        val shouldUpdate = (now - lastUiMs) >= 500L || p.downloadedBytes == p.totalBytes
                        if (!shouldUpdate) return@collect
                        lastUiMs = now

                        val percent = if (p.totalBytes > 0) (p.downloadedBytes * 100.0 / p.totalBytes) else 0.0
                        bar.progress = percent.toInt().coerceIn(0, 100)

                        txtProgress.text = TXAFormat.formatProgressWithSpeed(
                            downloadedBytes = p.downloadedBytes,
                            totalBytes = p.totalBytes,
                            percent = percent,
                            speedBps = p.speedBytesPerSec,
                        )

                        txtEta.text = if (p.etaSeconds >= 0) {
                            txa("txasplit_format_remaining_prefix") + ": " + TXAFormat.formatRemainingSeconds(p.etaSeconds)
                        } else {
                            txa("txasplit_format_calculating")
                        }
                    }

                // success
                return
            } catch (t: Throwable) {
                if (TXAUpdateManager.isNetworkError(t) && attempt < maxRetries) {
                    attempt += 1

                    // Phase 3: Network error -> auto retry với countdown
                    txtStatus.visibility = View.VISIBLE
                    for (sec in 5 downTo 1) {
                        txtStatus.text = txa("txasplit_update_retrying_in", sec.toString())
                        delay(1000)
                    }
                    continue
                }

                throw t
            }
        }
    }

    private fun markInstallPending(context: Context, info: TXAUpdateInfo, apk: File) {
        context.getSharedPreferences(TXAConstants.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(TXAConstants.PREF_UPDATE_INSTALLED_PENDING, true)
            .putInt(TXAConstants.PREF_UPDATE_VERSION_CODE, info.versionCode)
            .putString(TXAConstants.PREF_UPDATE_VERSION_NAME, info.versionName)
            // apk path vẫn được lưu bởi TXAInstall.installApk(), nhưng lưu lại ở đây cũng ok
            .putString(TXAConstants.PREF_PENDING_APK_PATH, apk.absolutePath)
            .apply()
    }

    private fun showSimpleDialog(title: String, message: String) {
        AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(txa("txasplit_ok"), null)
            .show()
    }

    private fun hasInstallPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 26) return true
        return context.packageManager.canRequestPackageInstalls()
    }

    private fun openUnknownSourcesSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Throwable) {
            Toast.makeText(context, txa("txasplit_error_open_settings"), Toast.LENGTH_LONG).show()
        }
    }

    private fun getUpdateApkFile(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(File(base, TXAConstants.UPDATES_DIR_NAME), "TXASPLIT-UPDATE.apk")
    }

    companion object {
        @Volatile private var didAutoCheckThisProcess: Boolean = false
    }
}
