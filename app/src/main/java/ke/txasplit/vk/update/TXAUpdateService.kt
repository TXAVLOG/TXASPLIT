package ke.txasplit.vk.update

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ke.txasplit.vk.TXAConstants
import ke.txasplit.vk.core.TXAHttp
import ke.txasplit.vk.notification.TXAUpdateNotificationManager
import ke.txasplit.vk.txa
import java.io.File

class TXAUpdateService : Service() {

    private var job: Job? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val versionName = intent.getStringExtra(EXTRA_VERSION_NAME) ?: return START_NOT_STICKY
                val downloadUrl = intent.getStringExtra(EXTRA_DOWNLOAD_URL) ?: return START_NOT_STICKY
                startDownload(versionName, downloadUrl)
            }
        }
        return START_NOT_STICKY
    }

    private fun startDownload(versionName: String, downloadUrl: String) {
        job?.cancel()
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Show initial notification
                TXAUpdateNotificationManager.showDownloadStarted(this@TXAUpdateService, versionName)
                
                val dest = getUpdateApkFile(this@TXAUpdateService)
                
                // Use existing TXADownload with retry logic like TXAUpdateDelegate
                downloadWithRetry(
                    url = downloadUrl,
                    dest = dest,
                    versionName = versionName
                )
                
                // Validate downloaded APK
                if (!TXADownload.validateApkFile(this@TXAUpdateService, dest)) {
                    if (dest.exists()) dest.delete()
                    throw IllegalStateException("Downloaded file is not a valid APK")
                }
                
                // Show completion notification with install button
                TXAUpdateNotificationManager.showDownloadComplete(this@TXAUpdateService, versionName, dest.absolutePath)
                
            } catch (t: Throwable) {
                TXAUpdateNotificationManager.showDownloadFailed(this@TXAUpdateService, versionName, t.message ?: "Unknown error")
            } finally {
                stopSelf()
            }
        }
    }
    
    private suspend fun downloadWithRetry(
        url: String,
        dest: File,
        versionName: String,
    ) {
        var attempt = 0
        val maxRetries = 20

        while (true) {
            try {
                TXAUpdateNotificationManager.updateDownloadStatus(this@TXAUpdateService, txa("txasplit_update_resolving_url"))
                
                var lastNotificationMs = 0L
                TXADownload.downloadApk(TXAHttp.client, url, dest)
                    .collect { p ->
                        val now = System.currentTimeMillis()
                        val shouldUpdate = (now - lastNotificationMs) >= 1000L || p.downloadedBytes == p.totalBytes
                        if (!shouldUpdate) return@collect
                        lastNotificationMs = now

                        val percent = if (p.totalBytes > 0) (p.downloadedBytes * 100.0 / p.totalBytes) else 0.0
                        
                        val progressText = ke.txasplit.vk.core.TXAFormat.formatProgressWithSpeed(
                            downloadedBytes = p.downloadedBytes,
                            totalBytes = p.totalBytes,
                            percent = percent,
                            speedBps = p.speedBytesPerSec,
                        )

                        val etaText = if (p.etaSeconds >= 0) {
                            txa("txasplit_format_remaining_prefix") + ": " + ke.txasplit.vk.core.TXAFormat.formatRemainingSeconds(p.etaSeconds)
                        } else {
                            txa("txasplit_format_calculating")
                        }
                        
                        TXAUpdateNotificationManager.updateDownloadProgress(
                            this@TXAUpdateService,
                            versionName = versionName,
                            progress = percent.toInt(),
                            progressText = progressText,
                            etaText = etaText
                        )
                    }

                // Success
                return
            } catch (t: Throwable) {
                if (TXAUpdateManager.isNetworkError(t) && attempt < maxRetries) {
                    attempt += 1

                    // Retry with countdown
                    for (sec in 5 downTo 1) {
                        TXAUpdateNotificationManager.updateDownloadStatus(
                            this@TXAUpdateService,
                            txa("txasplit_update_retrying_in", sec.toString())
                        )
                        delay(1000)
                    }
                    continue
                }

                throw t
            }
        }
    }
    
    private fun hasInstallPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT < 26) {
            true
        } else {
            packageManager.canRequestPackageInstalls()
        }
    }
    
    private fun getUpdateApkFile(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(File(base, TXAConstants.UPDATES_DIR_NAME), "TXASPLIT-UPDATE.apk")
    }

    override fun onDestroy() {
        job?.cancel()
        TXAUpdateNotificationManager.cancelAll(this@TXAUpdateService)
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_DOWNLOAD = "ke.txasplit.vk.update.ACTION_START_DOWNLOAD"
        const val EXTRA_VERSION_NAME = "version_name"
        const val EXTRA_DOWNLOAD_URL = "download_url"

        fun installApk(context: Context, apkPath: String) {
            TXAInstall.installApk(context, File(apkPath))
        }
    }
}
