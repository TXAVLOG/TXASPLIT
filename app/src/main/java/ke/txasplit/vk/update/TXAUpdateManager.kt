/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAUpdateManager.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AlertDialog
import ke.txasplit.vk.TXAConstants
import ke.txasplit.vk.core.TXAHttp
import ke.txasplit.vk.core.TXATranslation
import ke.txasplit.vk.txa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@Serializable
data class TXAUpdateCheckResponse(
    @SerialName("ok") val ok: Boolean = false,
    @SerialName("update_available") val updateAvailable: Boolean = false,
    @SerialName("latest") val latest: Latest? = null,
) {
    @Serializable
    data class Latest(
        @SerialName("versionCode") val versionCode: Int = 0,
        @SerialName("versionName") val versionName: String = "",
        @SerialName("downloadUrl") val downloadUrl: String = "",
        @SerialName("releaseDate") val releaseDate: String? = null,
        @SerialName("mandatory") val mandatory: Boolean = false,
        @SerialName("changelog") val changelog: String = "",
    )
}

@Serializable
data class TXAChangelogResponse(
    @SerialName("ok") val ok: Boolean = true,
    @SerialName("versionCode") val versionCode: Int = 0,
    @SerialName("versionName") val versionName: String = "",
    @SerialName("date") val date: String? = null,
    @SerialName("updated_at") val updatedAtSnake: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
    @SerialName("changelog") val changelog: String = "",
)

data class TXAUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseDate: String?,
    val changelog: String,
)

sealed class TXAUpdatePhase {
    data object Check : TXAUpdatePhase()
    data object Permission : TXAUpdatePhase()
    data class Download(val progressText: String, val etaText: String) : TXAUpdatePhase()
    data object Install : TXAUpdatePhase()
}

object TXAUpdateManager {

    private fun getCurrentVersion(context: Context): Pair<Int, String> {
        val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }

        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode
        }
        val versionName = packageInfo.versionName ?: ""
        return versionCode to versionName
    }

    private fun buildCheckUrls(baseUrl: String, versionCode: Int, versionName: String, locale: String): List<String> {
        val base = baseUrl.trimEnd('/')
        val hostBase = base.substringBefore("/txasplit/api").trimEnd('/')
        val qs = "versionCode=$versionCode&versionName=${Uri.encode(versionName)}&locale=$locale"

        return buildList {
            // TXASplit dedicated endpoint (phải ưu tiên)
            add("$base/update/check?$qs")

            // Legacy API fallback (chung với app khác)
            add("$base/txa/upd/check?$qs")
            if (hostBase.isNotBlank() && hostBase != base) add("$hostBase/api/txa/upd/check?$qs")
        }
    }

    private fun formatReleaseDate(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val zone = ZoneId.systemDefault()
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss d/M/yy", Locale.getDefault())
        val trimmed = raw.trim()

        fun fromInstant(str: String): ZonedDateTime? =
            runCatching { Instant.parse(str) }.map { it.atZone(zone) }.getOrNull()

        fun fromOffset(str: String): ZonedDateTime? =
            runCatching { OffsetDateTime.parse(str) }.map { it.atZoneSameInstant(zone) }.getOrNull()

        fun fromLocalDateTime(str: String): ZonedDateTime? =
            runCatching { LocalDateTime.parse(str) }.map { it.atZone(zone) }.getOrNull()

        fun fromIsoDate(str: String): ZonedDateTime? =
            runCatching { LocalDate.parse(str) }.map { it.atStartOfDay(zone) }.getOrNull()

        fun fromCustom(pattern: String): ZonedDateTime? {
            val fmt = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            return runCatching { LocalDate.parse(trimmed, fmt) }.map { it.atStartOfDay(zone) }.getOrNull()
        }

        val zoned = fromInstant(trimmed)
            ?: fromOffset(trimmed)
            ?: fromLocalDateTime(trimmed)
            ?: fromIsoDate(trimmed)
            ?: fromCustom("dd/MM/yyyy")
            ?: fromCustom("d/M/yyyy")
            ?: return trimmed

        return zoned.format(formatter)
    }

    private fun buildChangelogUrls(baseUrl: String, versionCode: Int, locale: String): List<String> {
        val base = baseUrl.trimEnd('/')
        val hostBase = base.substringBefore("/txasplit/api").trimEnd('/')

        return buildList {
            add("$base/changelog/$versionCode?locale=$locale")
            add("$base/txa/upd/changelog/$versionCode?locale=$locale")
            if (hostBase.isNotBlank() && hostBase != base) add("$hostBase/changelog/$versionCode?locale=$locale")
            if (hostBase.isNotBlank() && hostBase != base) add("$hostBase/api/txa/upd/changelog/$versionCode?locale=$locale")
        }
    }

    @Serializable
    private data class TXAUpdateErrorBody(
        @SerialName("error") val error: String? = null,
        @SerialName("message") val message: String? = null,
    )

    private fun resolveServerErrorMessage(statusCode: Int, rawBody: String): String {
        val trimmed = rawBody.trim()
        val parsed = if (trimmed.isNotBlank()) {
            runCatching { TXAHttp.json.decodeFromString(TXAUpdateErrorBody.serializer(), trimmed) }.getOrNull()
        } else {
            null
        }

        val code = (parsed?.error ?: parsed?.message ?: trimmed).lowercase(Locale.ROOT)

        val friendly = when {
            code.contains("metadata_unavailable") -> "Update metadata unavailable (metadata_unavailable)"
            code.contains("metadata_invalid") -> "Update metadata invalid (metadata_invalid)"
            code.contains("download_url_missing") -> "Download URL missing (download_url_missing)"
            code.contains("internal_error") -> "Server internal error (internal_error)"
            else -> null
        }

        return friendly ?: if (trimmed.isNotBlank()) {
            "HTTP $statusCode: $trimmed"
        } else {
            "HTTP $statusCode"
        }
    }

    suspend fun check(context: Context): TXAUpdateCheckResponse {
        val locale = TXATranslation.resolvePreferredLocale(context)
        val (versionCode, versionName) = getCurrentVersion(context)

        val urls = buildCheckUrls(TXAConstants.BASE_URL, versionCode, versionName, locale)
        return withContext(Dispatchers.IO) {
            var last: Throwable? = null
            for (url in urls.distinct()) {
                val req: Request = TXAHttp.buildGet(url)
                try {
                    logApiCall(context, "update/check", url)
                    logUpdateCheckToSeparateFile(context, "REQUEST", "URL: $url")

                    TXAHttp.client.newCall(req).execute().use { resp ->
                        val body = resp.body?.string().orEmpty()
                        if (!resp.isSuccessful) {
                            val message = resolveServerErrorMessage(resp.code, body)
                            val err = IllegalStateException(message)
                            logApiError(context, "update/check", err)
                            logUpdateCheckToSeparateFile(context, "ERROR", "HTTP ${resp.code} Body: $body")
                            last = err
                            return@use
                        }
                        logApiResponse(context, "update/check", body)
                        logUpdateCheckToSeparateFile(context, "RESPONSE", "Body: $body")

                        // Handle legacy raw format (TXAUPD1|...)
                        if (body.startsWith("TXAUPD1|")) {
                            return@withContext parseLegacyResponse(body)
                        }

                        return@withContext TXAHttp.json.decodeFromString(TXAUpdateCheckResponse.serializer(), body)
                    }
                } catch (t: Throwable) {
                    last = t
                    logApiError(context, "update/check", t)
                    logUpdateCheckToSeparateFile(context, "ERROR", "Exception: ${t.message}\nStack: ${t.stackTraceToString()}")
                }
            }
            val error = last ?: IllegalStateException("Update check failed")
            logUpdateCheckToSeparateFile(context, "FINAL_ERROR", "All attempts failed. Last error: ${error.message}")
            throw error
        }
    }
    
    private fun parseLegacyResponse(body: String): TXAUpdateCheckResponse {
        try {
            // Format: TXAUPD1|v=3.0.0_txa|d=14/12/2025|u=...|f=1|c=md
            val parts = body.split("|").associate { 
                val kv = it.split("=", limit = 2)
                if (kv.size == 2) kv[0] to kv[1] else "" to ""
            }
            
            val versionName = parts["v"] ?: ""
            val downloadUrl = parts["u"] ?: ""
            val mandatory = parts["f"] == "1"
            val changelogType = parts["c"] ?: "txt" // md or txt
            
            // Calculate versionCode from versionName
            val versionCode = parseVersionCodeFromVersionName(versionName)
            
            return TXAUpdateCheckResponse(
                ok = true,
                updateAvailable = true,
                latest = TXAUpdateCheckResponse.Latest(
                    versionCode = versionCode,
                    versionName = versionName,
                    downloadUrl = downloadUrl,
                    mandatory = mandatory,
                    changelog = "", // Legacy response doesn't include full changelog usually
                    releaseDate = parts["d"]
                )
            )
        } catch (e: Exception) {
            throw IllegalStateException("Failed to parse legacy response: ${e.message}")
        }
    }
    
    private fun parseVersionCodeFromVersionName(versionName: String): Int {
        // Format: X.Y.Z_txa
        return try {
            val clean = versionName.substringBefore("_")
            val parts = clean.split(".")
            if (parts.size >= 3) {
                val major = parts[0].toInt()
                val minor = parts[1].toInt()
                val patch = parts[2].toInt()
                major * 10000 + minor * 100 + patch
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Hàm log riêng cho update check theo yêu cầu
     * Ghi vào file update_check_log_YYYY-MM-DD.txt
     */
    private fun logUpdateCheckToSeparateFile(context: Context, type: String, message: String) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dateStr = dateFormat.format(Date())
            
            val logEntry = "[$timestamp] [$type]\n$message\n========================================\n"
            
            val logFile = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    )
                    val logDir = File(downloadsDir, "TXASPLIT")
                    logDir.mkdirs()
                    File(logDir, "update_check_log_$dateStr.txt")
                } else {
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    )
                    val logDir = File(downloadsDir, "TXASPLIT")
                    logDir.mkdirs()
                    File(logDir, "update_check_log_$dateStr.txt")
                }
            } catch (_: Throwable) {
                val appFilesDir = context.getExternalFilesDir(null) ?: context.filesDir
                val logDir = File(appFilesDir, "logs")
                logDir.mkdirs()
                File(logDir, "update_check_log_$dateStr.txt")
            }
            
            logFile.appendText(logEntry)
        } catch (_: Throwable) {
            // Ignore logging errors
        }
    }

    /**
     * Phase 1: Check & Verify -> Fetch Details (changelog/date).
     */
    suspend fun checkAndFetchUpdateInfo(context: Context): TXAUpdateInfo? {
        val locale = TXATranslation.resolvePreferredLocale(context)
        val (localVersionCode, localVersionName) = getCurrentVersion(context)

        val check = check(context)
        val latest = check.latest ?: return null
        val remoteVersionCode = latest.versionCode
        val hasDifferentName = latest.versionName.isNotBlank() && latest.versionName != localVersionName
        val shouldUpdate = remoteVersionCode > localVersionCode || hasDifferentName

        if (!check.ok || !check.updateAvailable || !shouldUpdate) return null

        val details = fetchChangelog(context, remoteVersionCode, locale)
        val releaseDateRaw = details?.date ?: details?.updatedAt ?: details?.updatedAtSnake ?: latest.releaseDate
        val releaseDate = formatReleaseDate(releaseDateRaw)
        val changelog = details?.changelog?.ifBlank { null } ?: latest.changelog
        val versionName = details?.versionName?.ifBlank { null } ?: latest.versionName

        return TXAUpdateInfo(
            versionCode = remoteVersionCode,
            versionName = versionName,
            downloadUrl = latest.downloadUrl,
            releaseDate = releaseDate,
            changelog = changelog,
        )
    }

    suspend fun fetchChangelog(context: Context, versionCode: Int, locale: String): TXAChangelogResponse? {
        val urls = buildChangelogUrls(TXAConstants.BASE_URL, versionCode, locale)
        return withContext(Dispatchers.IO) {
            for (url in urls.distinct()) {
                val req = TXAHttp.buildGet(url)
                try {
                    logApiCall(context, "update/changelog", url)
                    TXAHttp.client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) {
                            logApiError(context, "update/changelog", IllegalStateException("HTTP ${resp.code}"))
                            return@use
                        }
                        val body = resp.body?.string().orEmpty()
                        logApiResponse(context, "update/changelog", body)
                        return@withContext TXAHttp.json.decodeFromString(TXAChangelogResponse.serializer(), body)
                    }
                } catch (t: Throwable) {
                    logApiError(context, "update/changelog", t)
                }
            }
            // Optional endpoint -> fallback ở caller
            null
        }
    }

    fun ensureInstallPermissionOrOpenSettings(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 26) return true
        if (context.packageManager.canRequestPackageInstalls()) return true

        val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return false
    }

    suspend fun downloadAndInstall(context: Context, downloadUrl: String): Flow<TXAUpdatePhase> {
        val dest = run {
            val base = context.getExternalFilesDir(null) ?: context.filesDir
            File(File(base, TXAConstants.UPDATES_DIR_NAME), "TXASPLIT-UPDATE.apk")
        }

        return TXADownload.downloadApk(TXAHttp.client, downloadUrl, dest)
            .map { p ->
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

                TXAUpdatePhase.Download(progressText, etaText)
            }
    }

    fun showChangelogDialog(context: Context, title: String, changelog: String) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(changelog)
            .setPositiveButton(txa("txasplit_ok"), null)
            .show()
    }
    
    /**
     * Log API calls vào Downloads/TXASPLIT folder
     */
    private fun logApiCall(context: Context, endpoint: String, url: String) {
        logToFile(context, "CALL", endpoint, "URL: $url")
    }
    
    /**
     * Log API responses vào Downloads/TXASPLIT folder
     */
    private fun logApiResponse(context: Context, endpoint: String, response: String) {
        val responsePreview = if (response.length > 500) {
            response.take(500) + "... (truncated)"
        } else {
            response
        }
        logToFile(context, "RESPONSE", endpoint, "Response: $responsePreview")
    }
    
    /**
     * Log API errors vào Downloads/TXASPLIT folder
     */
    private fun logApiError(context: Context, endpoint: String, error: Throwable) {
        logToFile(context, "ERROR", endpoint, "Error: ${error.message}\nStack: ${error.stackTraceToString()}")
    }
    
    /**
     * Helper function để log vào file
     */
    private fun logToFile(context: Context, type: String, endpoint: String, message: String) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dateStr = dateFormat.format(Date())
            
            val logEntry = "[$timestamp] $type - Endpoint: $endpoint\n$message\n---\n"
            
            val logFile = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    )
                    val logDir = File(downloadsDir, "TXASPLIT")
                    logDir.mkdirs()
                    File(logDir, "api_log_$dateStr.txt")
                } else {
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    )
                    val logDir = File(downloadsDir, "TXASPLIT")
                    logDir.mkdirs()
                    File(logDir, "api_log_$dateStr.txt")
                }
            } catch (_: Throwable) {
                val appFilesDir = context.getExternalFilesDir(null) ?: context.filesDir
                val logDir = File(appFilesDir, "logs")
                logDir.mkdirs()
                File(logDir, "api_log_$dateStr.txt")
            }
            
            logFile.appendText(logEntry)
        } catch (_: Throwable) {
            // Ignore logging errors
        }
    }

    fun isNetworkError(t: Throwable): Boolean {
        return t is IOException ||
            t.cause is IOException ||
            t is java.net.SocketTimeoutException ||
            t is java.net.UnknownHostException ||
            t is java.net.ConnectException
    }
}
