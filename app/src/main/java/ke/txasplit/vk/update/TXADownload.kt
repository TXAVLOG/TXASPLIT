/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXADownload.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.update

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

data class TXADownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Double,
    val etaSeconds: Long,
)

object TXADownload {

    suspend fun downloadApk(
        client: OkHttpClient,
        url: String,
        destFile: File,
    ): Flow<TXADownloadProgress> = flow {
        // Resolve direct download URL for platforms like MediaFire, GitHub, Google Drive
        val directUrl = TXADownloadUrlResolver.resolveDirectDownloadUrl(client, url)
        
        val req = Request.Builder().url(directUrl).get().build()
        client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")

                val body = resp.body ?: throw IllegalStateException("Empty body")
                val contentType = (resp.header("Content-Type", "") ?: "").lowercase()
                val contentLength = body.contentLength()
                
                // Validate Content-Type - APK files should have application/vnd.android.package-archive or application/octet-stream
                if (contentType.contains("text/html") || contentType.contains("text/plain")) {
                    throw IllegalStateException("Invalid file type: $contentType. Expected APK file.")
                }
                
                // Validate file size - APK files are typically >1MB, HTML pages are usually <100KB
                if (contentLength > 0 && contentLength < 100 * 1024) {
                    throw IllegalStateException("File too small for APK: ${contentLength} bytes. Possible HTML page.")
                }
                
                val total = contentLength.coerceAtLeast(0L)

                destFile.parentFile?.mkdirs()
                if (destFile.exists()) destFile.delete()

                body.byteStream().use { input ->
                    FileOutputStream(destFile).use { out ->
                        val buf = ByteArray(DEFAULT_BUFFER_SIZE)
                        var read: Int
                        var downloaded = 0L

                        var lastTimeNs = System.nanoTime()
                        var lastBytes = 0L

                        while (true) {
                            read = input.read(buf)
                            if (read <= 0) break

                            out.write(buf, 0, read)
                            downloaded += read.toLong()

                            val nowNs = System.nanoTime()
                            val dtSec = (nowNs - lastTimeNs) / 1_000_000_000.0
                            if (dtSec >= 0.25) {
                                val delta = downloaded - lastBytes
                                val speed = if (dtSec > 0) delta / dtSec else 0.0
                                val remaining = if (total > 0) max(0L, total - downloaded) else -1L
                                val eta = if (speed > 1.0 && remaining >= 0) (remaining / speed).toLong() else -1L

                                emit(
                                    TXADownloadProgress(
                                        downloadedBytes = downloaded,
                                        totalBytes = total,
                                        speedBytesPerSec = speed,
                                        etaSeconds = eta,
                                    )
                                )

                                lastTimeNs = nowNs
                                lastBytes = downloaded
                            }
                        }

                        // final emit
                        emit(
                            TXADownloadProgress(
                                downloadedBytes = downloaded,
                                totalBytes = total,
                                speedBytesPerSec = 0.0,
                                etaSeconds = 0,
                            )
                        )
                    }
                }
            }
        }.flowOn(Dispatchers.IO)

    fun validateApkFile(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            return false
        }

        return try {
            val packageInfo = context.packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.GET_ACTIVITIES
            )
            packageInfo != null && packageInfo.packageName.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
