/*
████████ ██   ██  █████   █████  ██████  ██████  
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██ 
   ██      ███   ███████ ███████ ██████  ██████  
   ██     ██ ██  ██   ██ ██   ██ ██      ██      
   ██    ██   ██ ██   ██ ██   ██ ██      ██      
                                                 
                                                
TXASplit - TXADownloadUrlResolver.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.update

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.regex.Pattern

object TXADownloadUrlResolver {

    private const val TAG = "TXADownloadResolver"

    sealed class ResolveResult {
        data class Success(val directUrl: String, val fileName: String? = null) : ResolveResult()
        data class Error(val message: String, val originalUrl: String) : ResolveResult()
    }

    enum class LinkType {
        DIRECT,
        MEDIAFIRE,
        GOOGLE_DRIVE,
        GITHUB,
        UNKNOWN,
    }

    fun detectLinkType(url: String): LinkType {
        val normalized = url.lowercase(Locale.ROOT)
        return when {
            normalized.contains("mediafire.com") -> LinkType.MEDIAFIRE
            normalized.contains("drive.google.com") || normalized.contains("docs.google.com") -> LinkType.GOOGLE_DRIVE
            normalized.contains("github.com") && (
                normalized.contains("/releases/") ||
                    normalized.contains("/blob/") ||
                    normalized.contains("/raw/")
                ) -> LinkType.GITHUB
            normalized.substringBefore("?").endsWith(".apk") -> LinkType.DIRECT
            else -> LinkType.UNKNOWN
        }
    }

    suspend fun resolve(client: OkHttpClient, url: String): ResolveResult = withContext(Dispatchers.IO) {
        try {
            when (detectLinkType(url)) {
                LinkType.DIRECT -> ResolveResult.Success(url, extractFileName(url))
                LinkType.MEDIAFIRE -> resolveMediaFire(client, url)
                LinkType.GOOGLE_DRIVE -> resolveGoogleDrive(url)
                LinkType.GITHUB -> resolveGitHub(client, url)
                LinkType.UNKNOWN -> resolveByFollowingRedirects(client, url)
            }
        } catch (e: Exception) {
            logError("Failed to resolve: $url", e)
            ResolveResult.Error(e.message ?: "Unknown error", url)
        }
    }

    suspend fun resolveDirectDownloadUrl(client: OkHttpClient, url: String): String {
        return when (val result = resolve(client, url)) {
            is ResolveResult.Success -> result.directUrl
            is ResolveResult.Error -> throw IllegalStateException("Unable to resolve download link: ${result.message}")
        }
    }

    private suspend fun resolveMediaFire(client: OkHttpClient, url: String): ResolveResult {
        logInfo("Resolving MediaFire link: $url")
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            )
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return ResolveResult.Error("HTTP ${response.code}", url)
            }

            val html = response.body?.string().orEmpty()
            if (html.isBlank()) return ResolveResult.Error("Empty response", url)

            val patterns = listOf(
                Pattern.compile("href=\"(https://download[^\"]+\\.mediafire\\.com/[^\"]+)\"", Pattern.CASE_INSENSITIVE),
                Pattern.compile("id=\"downloadButton\"[^>]*href=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE),
                Pattern.compile("href=\"([^\"]+)\"[^>]*id=\"downloadButton\"", Pattern.CASE_INSENSITIVE),
                Pattern.compile("aria-label=\"Download file\"[^>]*href=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE),
                Pattern.compile("href=\"([^\"]+)\"[^>]*aria-label=\"Download file\"", Pattern.CASE_INSENSITIVE),
            )

            for (pattern in patterns) {
                val matcher = pattern.matcher(html)
                if (matcher.find()) {
                    val directUrl = matcher.group(1)
                    if (!directUrl.isNullOrBlank()) {
                        val fileName = extractFileName(directUrl) ?: extractFileName(url)
                        logInfo("MediaFire resolved: $directUrl")
                        return ResolveResult.Success(directUrl, fileName)
                    }
                }
            }

            val apkPattern = Pattern.compile("href=\"(https?://[^\"]+\\.apk[^\"]*)\"", Pattern.CASE_INSENSITIVE)
            val apkMatcher = apkPattern.matcher(html)
            if (apkMatcher.find()) {
                val directUrl = apkMatcher.group(1)
                if (!directUrl.isNullOrBlank()) {
                    logInfo("MediaFire APK fallback: $directUrl")
                    return ResolveResult.Success(directUrl, extractFileName(directUrl))
                }
            }
        }

        logError("MediaFire resolve failed for $url", null)
        return ResolveResult.Error("Could not find download link", url)
    }

    private fun resolveGoogleDrive(url: String): ResolveResult {
        logInfo("Resolving Google Drive link: $url")
        val fileId = when {
            url.contains("/file/d/") -> Regex("/file/d/([a-zA-Z0-9_-]+)").find(url)?.groupValues?.getOrNull(1)
            url.contains("id=") -> Regex("id=([a-zA-Z0-9_-]+)").find(url)?.groupValues?.getOrNull(1)
            else -> null
        }

        if (fileId.isNullOrBlank()) {
            return ResolveResult.Error("Could not extract file ID", url)
        }

        val directUrl = "https://drive.google.com/uc?export=download&id=$fileId&confirm=t"
        logInfo("Google Drive resolved: $directUrl")
        return ResolveResult.Success(directUrl, "$fileId.apk")
    }

    private suspend fun resolveGitHub(client: OkHttpClient, url: String): ResolveResult {
        logInfo("Resolving GitHub link: $url")
        val normalized = url.trim()

        if (normalized.contains("/blob/")) {
            val rawUrl = normalized.replace("/blob/", "/raw/")
            return resolveByFollowingRedirects(client, rawUrl)
        }
        if (normalized.contains("/raw/") || normalized.contains("/releases/download/")) {
            return resolveByFollowingRedirects(client, normalized)
        }

        val request = Request.Builder()
            .url(normalized)
            .header("User-Agent", "Mozilla/5.0")
            .build()

        client.newCall(request).execute().use { response ->
            val finalUrl = response.request.url.toString()
            if (finalUrl.endsWith(".apk", ignoreCase = true)) {
                return ResolveResult.Success(finalUrl, extractFileName(finalUrl))
            }

            val html = response.body?.string().orEmpty()
            if (html.isBlank()) return ResolveResult.Error("Empty response", url)

            val pattern = Pattern.compile("href=\"(/[^\"]+\\.apk)\"", Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(html)
            if (matcher.find()) {
                val path = matcher.group(1)
                val directUrl = "https://github.com$path"
                logInfo("GitHub APK asset found: $directUrl")
                return resolveByFollowingRedirects(client, directUrl)
            }
        }

        return ResolveResult.Error("Could not find APK asset on GitHub", url)
    }

    private suspend fun resolveByFollowingRedirects(client: OkHttpClient, url: String): ResolveResult {
        logInfo("Following redirects for: $url")
        val noRedirectClient = client.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()

        var currentUrl = url

        repeat(10) {
            val request = Request.Builder()
                .url(currentUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            noRedirectClient.newCall(request).execute().use { response ->
                when (response.code) {
                    in 300..399 -> {
                        val location = response.header("Location")
                            ?: return ResolveResult.Error("Missing redirect Location header", url)
                        currentUrl = resolveLocation(response.request.url.toString(), location)
                        logInfo("Redirect -> $currentUrl")
                    }
                    in 200..299 -> {
                        val finalUrl = response.request.url.toString()
                        return ResolveResult.Success(finalUrl, extractFileName(finalUrl))
                    }
                    else -> return ResolveResult.Error("HTTP ${response.code}", url)
                }
            }
        }

        return if (currentUrl != url) {
            ResolveResult.Success(currentUrl, extractFileName(currentUrl))
        } else {
            ResolveResult.Error("Too many redirects", url)
        }
    }

    private fun resolveLocation(baseUrl: String, location: String): String {
        if (location.startsWith("http", ignoreCase = true)) return location
        val base = baseUrl.toHttpUrlOrNull()
        val resolved = base?.resolve(location)
        return resolved?.toString() ?: location
    }

    private fun extractFileName(url: String): String? {
        return try {
            val clean = url.substringBefore("?")
            val fileName = clean.substringAfterLast("/", "")
            if (fileName.contains(".")) fileName else null
        } catch (_: Throwable) {
            null
        }
    }

    private fun logInfo(message: String) {
        Log.i(TAG, message)
    }

    private fun logError(message: String, throwable: Throwable?) {
        Log.e(TAG, message, throwable)
    }
}
