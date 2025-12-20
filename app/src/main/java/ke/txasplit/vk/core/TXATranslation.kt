/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXATranslation.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.core

import android.content.Context
import android.content.pm.PackageManager
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import ke.txasplit.vk.TXAConstants
import ke.txasplit.vk.txa
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import okhttp3.Request
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class TXALocalePayload(
    @SerialName("l") val locale: String,
    @SerialName("v") val version: String? = null,
    @SerialName("s") val strings: Map<String, String> = emptyMap(),
    @SerialName("ts") val ts: Long = 0,
    @SerialName("updated_at") @JsonNames("updatedAt") val updatedAt: String? = null,
)

@Serializable
data class TXALocalesResponse(
    @SerialName("locales") val locales: List<String> = emptyList(),
    @SerialName("default") val default: String = "en",
    @SerialName("updated_at") val updatedAt: String? = null,
)

object TXATranslation {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val inMemory = ConcurrentHashMap<String, String>()
    @Volatile private var currentLocale: String = "en"
    @Volatile private var currentTs: Long = 0

    fun getSavedLocaleTag(context: Context): String {
        return context.getSharedPreferences(TXAConstants.PREFS_NAME, Context.MODE_PRIVATE)
            .getString(TXAConstants.PREF_LOCALE, "")
            ?.trim()
            .orEmpty()
    }

    fun saveLocaleTag(context: Context, localeTag: String) {
        context.getSharedPreferences(TXAConstants.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(TXAConstants.PREF_LOCALE, localeTag)
            .apply()

        if (localeTag.isBlank()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeTag))
        }
    }

    fun init(context: Context) {
        val chosen = resolvePreferredLocale(context)
        currentLocale = chosen

        // Cache-first: load local JSON (if exists).
        val local = readLocalLocale(context, chosen)
        if (local != null) {
            applyPayload(local)
        } else {
            // Nếu chưa có cache, load fallback strings để đảm bảo luôn có translation
            // (quan trọng cho version guard dialog)
            loadFallbackStrings(chosen)
        }
    }
    
    /**
     * Load fallback strings mặc định để đảm bảo luôn có translation
     * (đặc biệt quan trọng cho version guard dialog khi chưa có cache)
     */
    private fun loadFallbackStrings(locale: String) {
        val fallback = when (locale) {
            "vi" -> getFallbackStringsVi()
            "ja" -> getFallbackStringsJa()
            "ko" -> getFallbackStringsKo()
            "zh" -> getFallbackStringsZh()
            else -> getFallbackStringsEn()
        }
        inMemory.putAll(fallback)
    }
    
    private fun getFallbackStringsEn(): Map<String, String> = mapOf(
        // Core
        "txasplit_app_name" to "TXASplit",
        "txasplit_ok" to "OK",
        "txasplit_cancel" to "Cancel",
        "txasplit_unknown_error" to "Unknown error",
        "txasplit_error_generic" to "Something went wrong. Please try again.",
        
        // Version Guard
        "txasplit_version_guard_title" to "Unsupported Android Version",
        "txasplit_version_guard_error" to "This app does not support %1\$s. Please upgrade to Android 14 or later for security and performance.",
        
        // Splash
        "txasplit_splash_checking" to "Checking language...",
        "txasplit_splash_downloading" to "Downloading language...",
        "txasplit_splash_ready" to "Ready",
        "txasplit_splash_error" to "Error: %1\$s",
        "txasplit_splash_using_cache" to "Using cached translations",
        
        // Tabs
        "txasplit_tab_groups" to "Groups",
        "txasplit_tab_summary" to "Summary",
        "txasplit_tab_profile" to "Profile",
        
        // Dashboard
        "txasplit_dashboard_net_balance" to "Net Balance: %1\$s",
        "txasplit_groups_hint_open_sample" to "Tap the sample group to open Group Detail",
        "txasplit_groups_sample_group" to "Sample Group",
        
        // Settings
        "txasplit_settings" to "Settings",
        "txasplit_language" to "Language",
        "txasplit_language_system_default" to "System Default",
        "txasplit_language_checking" to "Checking for updates...",
        "txasplit_language_downloading" to "Downloading language...",
        "txasplit_check_update" to "Check Update",
        "txasplit_update_view_logs" to "View Update Logs",
        "txasplit_export_data" to "Export Data",
        "txasplit_backup_data" to "Backup Data (Under Development)",
        "txasplit_version_display" to "Version %s",
        "txasplit_app_set_id_loading" to "App Set ID: loading...",
        "txasplit_app_set_id" to "App Set ID: %s",

        // Permissions (Background Update)
        "txasplit_grant" to "Grant",
        "txasplit_open_settings" to "Open Settings",
        "txasplit_error_open_settings" to "Can't open Settings. Please open it manually.",
        "txasplit_perm_all_files_title" to "Storage access required",
        "txasplit_perm_all_files_message" to "To save API logs into Downloads/TXASPLIT and enable background update checks, please allow 'All files access' for TXASplit.",
        "txasplit_perm_battery_title" to "Disable battery optimization",
        "txasplit_perm_battery_title_message" to "To check updates in background every 3 minutes, please allow TXASplit to ignore battery optimization. Otherwise, the system may stop background updates.",
        "txasplit_perm_battery_title_message_failed" to "We couldn't open the permission popup. Please open Settings → Battery and set TXASplit to 'Unrestricted/Don't optimize' to enable background updates.",
        "txasplit_perm_battery_toast_unavailable" to "Can't open battery settings. Background updates won't work without disabling battery optimization.",
        "txasplit_perm_battery_toast_required" to "Battery optimization is still enabled. Background update checks won't work until you set TXASplit to 'Don't optimize/Unrestricted'.",
        
        // Permissions (Core Features)
        "txasplit_perm_all_files_access_message" to "All files access is required to export CSV/Excel reports and securely store invoice data.",
        "txasplit_perm_battery_optimization_message" to "Disable battery optimization to ensure stable app operation and send timely payment reminders even when in background.",
        "txasplit_perm_exact_alarms_message" to "Exact alarm permission is required to send payment reminders accurate to the minute, never missing important bills.",
        "txasplit_perm_post_notifications_message" to "Allow push notifications for new bills, payment reminders, and important group updates.",
        
        // Language names
        "txasplit_lang_en" to "English",
        "txasplit_lang_vi" to "Tiếng Việt",
        "txasplit_lang_ja" to "日本語",
        "txasplit_lang_ko" to "한국어",
        "txasplit_lang_zh" to "中文",
        
        // Update
        "txasplit_update_title_no_update" to "No update",
        "txasplit_update_no_update" to "You are on the latest version.",
        "txasplit_update_title_available" to "Update available (%1\$s)",
        "txasplit_update_need_permission" to "Please allow installing unknown apps to continue.",
        "txasplit_update_downloading" to "Downloading update...",
        "txasplit_update_download" to "Download",
        "txasplit_close" to "Close",
        "txasplit_update_new_version" to "New version: %1\$s (%2\$s)",
        "txasplit_update_release_date" to "Updated on: %1\$s",
        "txasplit_update_release_date_unknown" to "Updated on: (unknown)",
        "txasplit_update_changelog_empty" to "No changelog provided.",
        "txasplit_update_unknown_sources_title" to "Permission required",
        "txasplit_update_unknown_sources_message" to "To install updates, please allow TXASplit to install unknown apps for this device. Tap Grant to open system settings.",
        "txasplit_update_retrying_in" to "Retrying in %1\$s s...",
        "txasplit_update_download_failed_title" to "Download failed",
        "txasplit_update_download_failed_message" to "Can't download update. %1\$s",
        "txasplit_update_success_title" to "Update successful",
        "txasplit_update_success_message" to "Update successful! You're now on %1\$s.",
        "txasplit_update_success_message_generic" to "Update successful!",
        "txasplit_update_logs_not_found" to "No update logs found yet.",
        "txasplit_update_logs_read_error" to "Can't read update log file. Please try again later.",
        
        // Download Validation Errors
        "txasplit_update_invalid_file" to "Invalid File",
        "txasplit_update_invalid_file_message" to "Downloaded file is not a valid APK. Please try again.",
        "txasplit_update_file_too_small" to "File Too Small",
        "txasplit_update_file_too_small_message" to "Downloaded file is too small to be a valid APK. This might be an HTML page instead of the actual APK.",
        "txasplit_update_invalid_content_type" to "Invalid Content Type",
        "txasplit_update_invalid_content_type_message" to "Server returned an invalid file type. Expected APK file but got: %s",
        "txasplit_update_resolving_url" to "Resolving download URL...",
        
        // Settings - Additional
        "txasplit_profile" to "Profile",
        "txasplit_edit_profile" to "Edit Profile",
        "txasplit_notifications" to "Notifications",
        "txasplit_ai_key_settings" to "AI Configuration (Under Development)",
        "txasplit_ai_key_saved" to "AI key saved successfully (Feature not yet active)",
        "txasplit_ai_key_required" to "AI key required. Please add key in Settings. (Feature under development)",
        "txasplit_privacy_policy" to "Privacy Policy",
        "txasplit_change_password" to "Change Password",
        "txasplit_sync_data" to "Sync Data (Under Development)",
        "txasplit_help" to "Help",
        "txasplit_faq" to "Frequently Asked Questions",
        "txasplit_about" to "About",
        "txasplit_logout" to "Logout",
        "txasplit_confirm_logout" to "Are you sure you want to logout?",
        
        // Time Format
        "txasplit_time_just_now" to "Just now",
        "txasplit_time_minutes_ago" to "%s min ago",
        "txasplit_time_hours_ago" to "%s hours ago",
        "txasplit_time_days_ago" to "%s days ago",
        "txasplit_last_backup" to "Last: %s",
        "txasplit_last_sync" to "Last: %s",
        
        // Format
        "txasplit_format_calculating" to "Calculating...",
        "txasplit_format_remaining_prefix" to "Remaining",
        "txasplit_format_remaining_seconds" to "%1\$s s",
        "txasplit_format_remaining_min_sec" to "%1\$s m %2\$s s",
        "txasplit_format_remaining_hour_min_sec" to "%1\$s h %2\$s m %3\$s s",
        "txasplit_format_remaining_day_hour_min_sec" to "%1\$s d %2\$s h %3\$s m %4\$s s",
        "txasplit_format_remaining_month_day_hour" to "%1\$s mo %2\$s d %3\$s h",
        "txasplit_format_remaining_year_month_day" to "%1\$s y %2\$s mo %3\$s d",
        "txasplit_format_bytes" to "%s B",
        "txasplit_format_kilobytes" to "%s KB",
        "txasplit_format_megabytes" to "%s MB",
        "txasplit_format_gigabytes" to "%s GB",
        "txasplit_format_speed_bytes" to "%s B/s",
        "txasplit_format_speed_kilobytes" to "%s KB/s",
        "txasplit_format_speed_megabytes" to "%s MB/s",
        "txasplit_format_speed_gigabytes" to "%s GB/s",
        "txasplit_format_percent" to "%s%%",
        "txasplit_format_download_progress" to "%1\$s / %2\$s (%3\$s)",
        "txasplit_format_download_progress_speed" to "%1\$s / %2\$s (%3\$s) - %4\$s",
        
        // Language Download
        "txasplit_language_download_success" to "Language downloaded successfully",
        "txasplit_language_download_failed" to "Failed to download language",
        "txasplit_language_using_fallback" to "Using fallback language",
        "txasplit_language_apply_now" to "Apply Now",
        
        // Group Detail
        "txasplit_group_tab_bills" to "Bills",
        "txasplit_group_tab_members" to "Members",
        "txasplit_group_tab_stats" to "Stats",
        
        // Generic
        "txasplit_app_description" to "Smart group expense management platform",
    )
    
    private fun getFallbackStringsVi(): Map<String, String> = mapOf(
        // Core
        "txasplit_app_name" to "TXASplit",
        "txasplit_ok" to "Đồng ý",
        "txasplit_cancel" to "Hủy",
        "txasplit_unknown_error" to "Lỗi không xác định",
        "txasplit_error_generic" to "Đã xảy ra lỗi. Vui lòng thử lại.",
        
        // Version Guard
        "txasplit_version_guard_title" to "Phiên bản Android không được hỗ trợ",
        "txasplit_version_guard_error" to "Ứng dụng chưa hỗ trợ %1\$s. Vui lòng nâng cấp lên Android 14 trở lên để đảm bảo tính bảo mật và hiệu suất.",
        
        // Splash
        "txasplit_splash_checking" to "Đang kiểm tra...",
        "txasplit_splash_downloading" to "Đang tải xuống...",
        "txasplit_splash_ready" to "Sẵn sàng",
        "txasplit_splash_error" to "Lỗi: %1\$s",
        "txasplit_splash_using_cache" to "Đang dùng bản dịch đã lưu",
        
        // Tabs
        "txasplit_tab_groups" to "Nhóm",
        "txasplit_tab_summary" to "Tóm tắt",
        "txasplit_tab_profile" to "Hồ sơ",
        
        // Dashboard
        "txasplit_dashboard_net_balance" to "Số dư ròng: %1\$s",
        "txasplit_groups_hint_open_sample" to "Chạm vào nhóm mẫu để mở Chi tiết nhóm",
        "txasplit_groups_sample_group" to "Nhóm mẫu",
        
        // Settings
        "txasplit_settings" to "Cài đặt",
        "txasplit_language" to "Ngôn ngữ",
        "txasplit_language_system_default" to "Mặc định hệ thống",
        "txasplit_language_checking" to "Đang kiểm tra cập nhật...",
        "txasplit_language_downloading" to "Đang tải ngôn ngữ...",
        "txasplit_check_update" to "Kiểm tra cập nhật",
        "txasplit_export_data" to "Xuất dữ liệu",
        "txasplit_backup_data" to "Sao lưu dữ liệu (Đang phát triển)",
        "txasplit_version_display" to "Phiên bản %s",
        "txasplit_app_set_id_loading" to "App Set ID: đang tải...",
        "txasplit_app_set_id" to "App Set ID: %s",

        // Permissions (Background Update)
        "txasplit_grant" to "Cấp quyền",
        "txasplit_open_settings" to "Mở cài đặt",
        "txasplit_error_open_settings" to "Không thể mở Cài đặt. Vui lòng tự mở trong hệ thống.",
        "txasplit_perm_all_files_title" to "Cần quyền truy cập bộ nhớ",
        "txasplit_perm_all_files_message" to "Để lưu log API vào Downloads/TXASPLIT và bật kiểm tra cập nhật nền, vui lòng cấp quyền 'Truy cập tất cả tệp' cho TXASplit.",
        "txasplit_perm_battery_title" to "Bỏ qua tối ưu hoá pin",
        "txasplit_perm_battery_title_message" to "Để app check update nền mỗi 3 phút, vui lòng cho TXASplit bỏ qua tối ưu hoá pin. Nếu không, hệ thống có thể chặn cập nhật nền.",
        "txasplit_perm_battery_title_message_failed" to "Không thể mở popup cấp quyền tự động. Vui lòng vào Cài đặt → Pin và đặt TXASplit ở chế độ 'Không tối ưu/Không giới hạn' để bật cập nhật nền.",
        "txasplit_perm_battery_toast_unavailable" to "Không thể mở cài đặt pin. Nếu không bỏ qua tối ưu hoá pin thì cập nhật nền sẽ không hoạt động.",
        "txasplit_perm_battery_toast_required" to "Bạn vẫn chưa tắt tối ưu hoá pin. Update nền sẽ không chạy cho đến khi đặt TXASplit ở chế độ 'Không tối ưu/Không giới hạn'.",
        
        // Language names
        "txasplit_lang_en" to "English",
        "txasplit_lang_vi" to "Tiếng Việt",
        "txasplit_lang_ja" to "日本語",
        "txasplit_lang_ko" to "한국어",
        "txasplit_lang_zh" to "中文",
        
        // Update
        "txasplit_update_title_no_update" to "Không có cập nhật",
        "txasplit_update_no_update" to "Bạn đang dùng phiên bản mới nhất.",
        "txasplit_update_title_available" to "Có cập nhật (%1\$s)",
        "txasplit_update_need_permission" to "Vui lòng cho phép cài đặt ứng dụng không rõ nguồn gốc để tiếp tục.",
        "txasplit_update_downloading" to "Đang tải cập nhật...",
        "txasplit_update_download" to "Tải xuống",
        "txasplit_close" to "Đóng",
        "txasplit_update_new_version" to "Bản mới: %1\$s (%2\$s)",
        "txasplit_update_release_date" to "Ngày cập nhật: %1\$s",
        "txasplit_update_release_date_unknown" to "Ngày cập nhật: (không rõ)",
        "txasplit_update_changelog_empty" to "Không có changelog.",
        "txasplit_update_unknown_sources_title" to "Cần quyền cài đặt",
        "txasplit_update_unknown_sources_message" to "Để cài đặt bản cập nhật, vui lòng cho phép TXASplit cài đặt ứng dụng không rõ nguồn gốc. Nhấn Cấp quyền để mở cài đặt hệ thống.",
        "txasplit_update_retrying_in" to "Đang thử lại sau %1\$s giây...",
        "txasplit_update_download_failed_title" to "Tải xuống thất bại",
        "txasplit_update_download_failed_message" to "Không thể tải bản cập nhật. %1\$s",
        "txasplit_update_success_title" to "Cập nhật thành công",
        "txasplit_update_success_message" to "Cập nhật thành công! Bạn đang ở %1\$s.",
        "txasplit_update_success_message_generic" to "Cập nhật thành công!",
        
        // Download Validation Errors
        "txasplit_update_invalid_file" to "File Không Hợp Lệ",
        "txasplit_update_invalid_file_message" to "File tải về không phải là APK hợp lệ. Vui lòng thử lại.",
        "txasplit_update_file_too_small" to "File Quá Nhỏ",
        "txasplit_update_file_too_small_message" to "File tải về quá nhỏ để là APK hợp lệ. Có thể đây là trang HTML thay vì file APK thực.",
        "txasplit_update_invalid_content_type" to "Loại File Không Hợp Lệ",
        "txasplit_update_invalid_content_type_message" to "Server trả về loại file không hợp lệ. Cần file APK nhưng nhận được: %s",
        "txasplit_update_resolving_url" to "Đang giải quyết URL tải xuống...",
        
        // Settings - Additional
        "txasplit_profile" to "Hồ sơ",
        "txasplit_edit_profile" to "Chỉnh sửa hồ sơ",
        "txasplit_notifications" to "Thông báo",
        "txasplit_ai_key_settings" to "Cấu hình AI (Đang phát triển)",
        "txasplit_ai_key_saved" to "Đã lưu khóa AI thành công (Tính năng chưa kích hoạt)",
        "txasplit_ai_key_required" to "Cần khóa AI. Vui lòng thêm khóa trong Cài đặt. (Tính năng đang phát triển)",
        "txasplit_privacy_policy" to "Chính sách bảo mật",
        "txasplit_change_password" to "Đổi mật khẩu",
        "txasplit_sync_data" to "Đồng bộ dữ liệu (Đang phát triển)",
        "txasplit_help" to "Trợ giúp",
        "txasplit_faq" to "Câu hỏi thường gặp",
        "txasplit_about" to "Giới thiệu",
        "txasplit_logout" to "Đăng xuất",
        "txasplit_confirm_logout" to "Bạn có chắc chắn muốn đăng xuất?",
        
        // Time Format
        "txasplit_time_just_now" to "Vừa xong",
        "txasplit_time_minutes_ago" to "%s phút trước",
        "txasplit_time_hours_ago" to "%s giờ trước",
        "txasplit_time_days_ago" to "%s ngày trước",
        "txasplit_last_backup" to "Lần cuối: %s",
        "txasplit_last_sync" to "Lần cuối: %s",
        
        // Format
        "txasplit_format_calculating" to "Đang tính toán...",
        "txasplit_format_remaining_prefix" to "Còn lại",
        "txasplit_format_remaining_seconds" to "%1\$s giây",
        "txasplit_format_remaining_min_sec" to "%1\$s phút %2\$s giây",
        "txasplit_format_remaining_hour_min_sec" to "%1\$s giờ %2\$s phút %3\$s giây",
        "txasplit_format_remaining_day_hour_min_sec" to "%1\$s ngày %2\$s giờ %3\$s phút %4\$s giây",
        "txasplit_format_remaining_month_day_hour" to "%1\$s tháng %2\$s ngày %3\$s giờ",
        "txasplit_format_remaining_year_month_day" to "%1\$s năm %2\$s tháng %3\$s ngày",
        "txasplit_format_bytes" to "%s B",
        "txasplit_format_kilobytes" to "%s KB",
        "txasplit_format_megabytes" to "%s MB",
        "txasplit_format_gigabytes" to "%s GB",
        "txasplit_format_speed_bytes" to "%s B/s",
        "txasplit_format_speed_kilobytes" to "%s KB/s",
        "txasplit_format_speed_megabytes" to "%s MB/s",
        "txasplit_format_speed_gigabytes" to "%s GB/s",
        "txasplit_format_percent" to "%s%%",
        "txasplit_format_download_progress" to "%1\$s / %2\$s (%3\$s)",
        "txasplit_format_download_progress_speed" to "%1\$s / %2\$s (%3\$s) - %4\$s",
        
        // Language Download
        "txasplit_language_download_success" to "Đã tải ngôn ngữ thành công",
        "txasplit_language_download_failed" to "Tải ngôn ngữ thất bại",
        "txasplit_language_using_fallback" to "Đang dùng ngôn ngữ dự phòng",
        "txasplit_language_apply_now" to "Áp dụng ngay",
        
        // Group Detail
        "txasplit_group_tab_bills" to "Hóa đơn",
        "txasplit_group_tab_members" to "Thành viên",
        "txasplit_group_tab_stats" to "Thống kê",
        
        // Generic
        "txasplit_app_description" to "Nền tảng quản lý chi tiêu nhóm thông minh",
    )
    
    private fun getFallbackStringsJa(): Map<String, String> = mapOf(
        // Core
        "txasplit_app_name" to "TXASplit",
        "txasplit_ok" to "OK",
        "txasplit_cancel" to "キャンセル",
        "txasplit_unknown_error" to "不明なエラー",
        "txasplit_error_generic" to "エラーが発生しました。もう一度お試しください。",
        "txasplit_version_guard_title" to "サポートされていないAndroidバージョン",
        "txasplit_version_guard_error" to "このアプリは%1\$sをサポートしていません。セキュリティとパフォーマンスのために、Android 14以降にアップグレードしてください。",
        "txasplit_splash_checking" to "確認中...",
        "txasplit_splash_downloading" to "ダウンロード中...",
        "txasplit_splash_ready" to "準備完了",
        "txasplit_splash_error" to "エラー: %1\$s",
        "txasplit_splash_using_cache" to "キャッシュされた翻訳を使用中",
        "txasplit_tab_groups" to "グループ",
        "txasplit_tab_summary" to "概要",
        "txasplit_tab_profile" to "プロフィール",
        "txasplit_dashboard_net_balance" to "純残高: %1\$s",
        "txasplit_groups_hint_open_sample" to "サンプルグループをタップしてグループ詳細を開く",
        "txasplit_groups_sample_group" to "サンプルグループ",
        "txasplit_settings" to "設定",
        "txasplit_profile" to "プロフィール",
        "txasplit_edit_profile" to "プロフィール編集",
        "txasplit_language" to "言語",
        "txasplit_notifications" to "通知",
        "txasplit_ai_key_settings" to "AI設定（開発中）",
        "txasplit_ai_key_saved" to "AIキーが正常に保存されました（機能はまだアクティブではありません）",
        "txasplit_ai_key_required" to "AIキーが必要です。設定でキーを追加してください。（機能開発中）",
        "txasplit_privacy_policy" to "プライバシーポリシー",
        "txasplit_change_password" to "パスワード変更",
        "txasplit_backup_data" to "データのバックアップ（開発中）",
        "txasplit_sync_data" to "データの同期（開発中）",
        "txasplit_help" to "ヘルプ",
        "txasplit_faq" to "よくある質問",
        "txasplit_about" to "について",
        "txasplit_logout" to "ログアウト",
        "txasplit_confirm_logout" to "ログアウトしてもよろしいですか？",
        "txasplit_language_system_default" to "システムデフォルト",
        "txasplit_language_checking" to "更新を確認中...",
        "txasplit_language_downloading" to "言語をダウンロード中...",
        "txasplit_language_download_success" to "言語のダウンロードが完了しました",
        "txasplit_language_download_failed" to "言語のダウンロードに失敗しました",
        "txasplit_language_using_fallback" to "フォールバック言語を使用中",
        "txasplit_language_apply_now" to "今すぐ適用",
        "txasplit_check_update" to "更新を確認",
        "txasplit_export_data" to "データのエクスポート",
        "txasplit_version_display" to "バージョン %s",
        "txasplit_app_set_id_loading" to "App Set ID: 読み込み中...",
        "txasplit_app_set_id" to "App Set ID: %s",

        // Permissions (Background Update)
        "txasplit_grant" to "許可",
        "txasplit_open_settings" to "設定を開く",
        "txasplit_error_open_settings" to "設定を開けません。手動で開いてください。",
        "txasplit_perm_all_files_title" to "ストレージ権限が必要です",
        "txasplit_perm_all_files_message" to "Downloads/TXASPLIT にAPIログを保存し、バックグラウンド更新チェックを有効にするには、TXASplit に「すべてのファイルへのアクセス」を許可してください。",
        "txasplit_perm_battery_title" to "電池の最適化を無効化",
        "txasplit_perm_battery_title_message" to "3分ごとにバックグラウンドで更新を確認するには、TXASplit を電池の最適化から除外してください。そうしないとシステムにより停止される可能性があります。",
        "txasplit_perm_battery_title_message_failed" to "権限ポップアップを開けませんでした。設定→バッテリーでTXASplitを「最適化しない/制限なし」に設定してください。",
        "txasplit_perm_battery_toast_unavailable" to "バッテリー設定を開けません。電池最適化を無効にしないとバックグラウンド更新は動作しません。",
        "txasplit_perm_battery_toast_required" to "電池最適化が有効のままです。TXASplitを「最適化しない/制限なし」に設定するまでバックグラウンド更新は動作しません。",
        "txasplit_lang_en" to "English",
        "txasplit_lang_vi" to "Tiếng Việt",
        "txasplit_lang_ja" to "日本語",
        "txasplit_lang_ko" to "한국어",
        "txasplit_lang_zh" to "中文",
        "txasplit_update_title_no_update" to "更新なし",
        "txasplit_update_no_update" to "最新バージョンを使用しています。",
        "txasplit_update_title_available" to "更新が利用可能（%1\$s）",
        "txasplit_update_need_permission" to "続行するには、不明なアプリのインストールを許可してください。",
        "txasplit_update_downloading" to "更新をダウンロード中...",
        "txasplit_update_download" to "ダウンロード",
        "txasplit_close" to "閉じる",
        "txasplit_update_new_version" to "新しいバージョン: %1\$s（%2\$s）",
        "txasplit_update_release_date" to "更新日: %1\$s",
        "txasplit_update_release_date_unknown" to "更新日:（不明）",
        "txasplit_update_changelog_empty" to "変更ログはありません。",
        "txasplit_update_unknown_sources_title" to "権限が必要",
        "txasplit_update_unknown_sources_message" to "更新をインストールするには、TXASplitに不明なアプリのインストールを許可してください。「許可」をタップして設定を開きます。",
        "txasplit_update_retrying_in" to "%1\$s秒後に再試行...",
        "txasplit_update_download_failed_title" to "ダウンロード失敗",
        "txasplit_update_download_failed_message" to "更新をダウンロードできません。%1\$s",
        "txasplit_update_success_title" to "更新完了",
        "txasplit_update_success_message" to "更新が完了しました！現在は%1\$sです。",
        "txasplit_update_success_message_generic" to "更新が完了しました！",
        
        // Download Validation Errors
        "txasplit_update_invalid_file" to "無効なファイル",
        "txasplit_update_invalid_file_message" to "ダウンロードしたファイルは有効なAPKではありません。もう一度お試しください。",
        "txasplit_update_file_too_small" to "ファイルが小さすぎます",
        "txasplit_update_file_too_small_message" to "ダウンロードしたファイルは有効なAPKとしては小さすぎます。実際のAPKではなくHTMLページである可能性があります。",
        "txasplit_update_invalid_content_type" to "無効なコンテンツタイプ",
        "txasplit_update_invalid_content_type_message" to "サーバーが無効なファイルタイプを返しました。APKファイルが必要ですが、%sを受信しました。",
        "txasplit_update_resolving_url" to "ダウンロードURLを解決中...",
        "txasplit_last_sync" to "最終: %s",
        "txasplit_format_calculating" to "計算中...",
        "txasplit_format_remaining_prefix" to "残り",
        "txasplit_format_remaining_seconds" to "%1\$s秒",
        "txasplit_format_remaining_min_sec" to "%1\$s分 %2\$s秒",
        "txasplit_format_remaining_hour_min_sec" to "%1\$s時間 %2\$s分 %3\$s秒",
        "txasplit_format_remaining_day_hour_min_sec" to "%1\$s日 %2\$s時間 %3\$s分 %4\$s秒",
        "txasplit_format_remaining_month_day_hour" to "%1\$sヶ月 %2\$s日 %3\$s時間",
        "txasplit_format_remaining_year_month_day" to "%1\$s年 %2\$sヶ月 %3\$s日",
        "txasplit_format_bytes" to "%s B",
        "txasplit_format_kilobytes" to "%s KB",
        "txasplit_format_megabytes" to "%s MB",
        "txasplit_format_gigabytes" to "%s GB",
        "txasplit_format_speed_bytes" to "%s B/s",
        "txasplit_format_speed_kilobytes" to "%s KB/s",
        "txasplit_format_speed_megabytes" to "%s MB/s",
        "txasplit_format_speed_gigabytes" to "%s GB/s",
        "txasplit_format_percent" to "%s%%",
        "txasplit_format_download_progress" to "%1\$s / %2\$s (%3\$s)",
        "txasplit_format_download_progress_speed" to "%1\$s / %2\$s (%3\$s) - %4\$s",
        "txasplit_group_tab_bills" to "請求書",
        "txasplit_group_tab_members" to "メンバー",
        "txasplit_group_tab_stats" to "統計",
        "txasplit_app_description" to "スマートグループ経費管理プラットフォーム",
    )
    
    private fun getFallbackStringsKo(): Map<String, String> = mapOf(
        // Core
        "txasplit_app_name" to "TXASplit",
        "txasplit_ok" to "확인",
        "txasplit_cancel" to "취소",
        "txasplit_unknown_error" to "알 수 없는 오류",
        "txasplit_error_generic" to "문제가 발생했습니다. 다시 시도해 주세요.",
        "txasplit_version_guard_title" to "지원되지 않는 Android 버전",
        "txasplit_version_guard_error" to "이 앱은 %1\$s을(를) 지원하지 않습니다. 보안 및 성능을 위해 Android 14 이상으로 업그레이드하세요.",
        "txasplit_splash_checking" to "확인 중...",
        "txasplit_splash_downloading" to "다운로드 중...",
        "txasplit_splash_ready" to "준비됨",
        "txasplit_splash_error" to "오류: %1\$s",
        "txasplit_splash_using_cache" to "캐시된 번역 사용 중",
        "txasplit_tab_groups" to "그룹",
        "txasplit_tab_summary" to "요약",
        "txasplit_tab_profile" to "프로필",
        "txasplit_dashboard_net_balance" to "순 잔액: %1\$s",
        "txasplit_groups_hint_open_sample" to "샘플 그룹을 탭하여 그룹 세부 정보 열기",
        "txasplit_groups_sample_group" to "샘플 그룹",
        "txasplit_settings" to "설정",
        "txasplit_profile" to "프로필",
        "txasplit_edit_profile" to "프로필 편집",
        "txasplit_language" to "언어",
        "txasplit_notifications" to "알림",
        "txasplit_ai_key_settings" to "AI 구성 (개발 중)",
        "txasplit_ai_key_saved" to "AI 키가 성공적으로 저장되었습니다 (기능이 아직 활성화되지 않음)",
        "txasplit_ai_key_required" to "AI 키가 필요합니다. 설정에서 키를 추가하세요. (기능 개발 중)",
        "txasplit_privacy_policy" to "개인정보 보호정책",
        "txasplit_change_password" to "비밀번호 변경",
        "txasplit_backup_data" to "데이터 백업 (개발 중)",
        "txasplit_sync_data" to "데이터 동기화 (개발 중)",
        "txasplit_help" to "도움말",
        "txasplit_faq" to "자주 묻는 질문",
        "txasplit_about" to "정보",
        "txasplit_logout" to "로그아웃",
        "txasplit_confirm_logout" to "로그아웃하시겠습니까?",
        "txasplit_language_system_default" to "시스템 기본값",
        "txasplit_language_checking" to "업데이트 확인 중...",
        "txasplit_language_downloading" to "언어 다운로드 중...",
        "txasplit_language_download_success" to "언어 다운로드 성공",
        "txasplit_language_download_failed" to "언어 다운로드 실패",
        "txasplit_language_using_fallback" to "폴백 언어 사용 중",
        "txasplit_language_apply_now" to "지금 적용",
        "txasplit_check_update" to "업데이트 확인",
        "txasplit_export_data" to "데이터 내보내기",
        "txasplit_version_display" to "버전 %s",
        "txasplit_app_set_id_loading" to "App Set ID: 로딩 중...",
        "txasplit_app_set_id" to "App Set ID: %s",

        // Permissions (Background Update)
        "txasplit_grant" to "허용",
        "txasplit_open_settings" to "설정 열기",
        "txasplit_error_open_settings" to "설정을 열 수 없습니다. 수동으로 열어 주세요.",
        "txasplit_perm_all_files_title" to "저장소 권한 필요",
        "txasplit_perm_all_files_message" to "Downloads/TXASPLIT에 API 로그를 저장하고 백그라운드 업데이트 확인을 활성화하려면 TXASplit에 '모든 파일 액세스'를 허용해 주세요.",
        "txasplit_perm_battery_title" to "배터리 최적화 해제",
        "txasplit_perm_battery_title_message" to "3분마다 백그라운드에서 업데이트를 확인하려면 TXASplit의 배터리 최적화를 해제해 주세요. 그렇지 않으면 시스템이 백그라운드 업데이트를 중지할 수 있습니다.",
        "txasplit_perm_battery_title_message_failed" to "권한 팝업을 열 수 없습니다. 설정→배터리에서 TXASplit을 '최적화 안 함/제한 없음'으로 설정해 주세요.",
        "txasplit_perm_battery_toast_unavailable" to "배터리 설정을 열 수 없습니다. 배터리 최적화를 해제하지 않으면 백그라운드 업데이트가 동작하지 않습니다.",
        "txasplit_perm_battery_toast_required" to "배터리 최적화가 아직 활성화되어 있습니다. TXASplit을 '최적화 안 함/제한 없음'으로 설정해야 백그라운드 업데이트가 동작합니다.",
        "txasplit_lang_en" to "English",
        "txasplit_lang_vi" to "Tiếng Việt",
        "txasplit_lang_ja" to "日本語",
        "txasplit_lang_ko" to "한국어",
        "txasplit_lang_zh" to "中文",
        "txasplit_update_title_no_update" to "업데이트 없음",
        "txasplit_update_no_update" to "최신 버전을 사용하고 있습니다.",
        "txasplit_update_title_available" to "업데이트 사용 가능 (%1\$s)",
        "txasplit_update_need_permission" to "계속하려면 알 수 없는 앱 설치를 허용하세요.",
        "txasplit_update_downloading" to "업데이트 다운로드 중...",
        "txasplit_update_download" to "다운로드",
        "txasplit_close" to "닫기",
        "txasplit_update_new_version" to "새 버전: %1\$s (%2\$s)",
        "txasplit_update_release_date" to "업데이트 날짜: %1\$s",
        "txasplit_update_release_date_unknown" to "업데이트 날짜: (알 수 없음)",
        "txasplit_update_changelog_empty" to "변경 사항이 없습니다.",
        "txasplit_update_unknown_sources_title" to "권한 필요",
        "txasplit_update_unknown_sources_message" to "업데이트를 설치하려면 TXASplit에 '알 수 없는 앱 설치' 권한을 허용해야 합니다. '허용'을 눌러 설정을 여세요.",
        "txasplit_update_retrying_in" to "%1\$s초 후 재시도...",
        "txasplit_update_download_failed_title" to "다운로드 실패",
        "txasplit_update_download_failed_message" to "업데이트를 다운로드할 수 없습니다. %1\$s",
        "txasplit_update_success_title" to "업데이트 완료",
        "txasplit_update_success_message" to "업데이트 완료! 현재 버전은 %1\$s입니다.",
        "txasplit_update_success_message_generic" to "업데이트 완료!",
        
        // Download Validation Errors
        "txasplit_update_invalid_file" to "잘못된 파일",
        "txasplit_update_invalid_file_message" to "다운로드한 파일이 유효한 APK가 아닙니다. 다시 시도해 주세요.",
        "txasplit_update_file_too_small" to "파일이 너무 작습니다",
        "txasplit_update_file_too_small_message" to "다운로드한 파일이 유효한 APK로는 너무 작습니다. 실제 APK가 아닌 HTML 페이지일 수 있습니다.",
        "txasplit_update_invalid_content_type" to "잘못된 콘텐츠 유형",
        "txasplit_update_invalid_content_type_message" to "서버가 잘못된 파일 유형을 반환했습니다. APK 파일이 필요하지만 %s를 받았습니다.",
        "txasplit_update_resolving_url" to "다운로드 URL 확인 중...",
        "txasplit_time_just_now" to "방금",
        "txasplit_time_minutes_ago" to "%s분 전",
        "txasplit_time_hours_ago" to "%s시간 전",
        "txasplit_time_days_ago" to "%s일 전",
        "txasplit_last_backup" to "마지막: %s",
        "txasplit_last_sync" to "마지막: %s",
        "txasplit_format_calculating" to "계산 중...",
        "txasplit_format_remaining_prefix" to "남은 시간",
        "txasplit_format_remaining_seconds" to "%1\$s초",
        "txasplit_format_remaining_min_sec" to "%1\$s분 %2\$s초",
        "txasplit_format_remaining_hour_min_sec" to "%1\$s시간 %2\$s분 %3\$s초",
        "txasplit_format_remaining_day_hour_min_sec" to "%1\$s일 %2\$s시간 %3\$s분 %4\$s초",
        "txasplit_format_remaining_month_day_hour" to "%1\$s개월 %2\$s일 %3\$s시간",
        "txasplit_format_remaining_year_month_day" to "%1\$s년 %2\$s개월 %3\$s일",
        "txasplit_format_bytes" to "%s B",
        "txasplit_format_kilobytes" to "%s KB",
        "txasplit_format_megabytes" to "%s MB",
        "txasplit_format_gigabytes" to "%s GB",
        "txasplit_format_speed_bytes" to "%s B/s",
        "txasplit_format_speed_kilobytes" to "%s KB/s",
        "txasplit_format_speed_megabytes" to "%s MB/s",
        "txasplit_format_speed_gigabytes" to "%s GB/s",
        "txasplit_format_percent" to "%s%%",
        "txasplit_format_download_progress" to "%1\$s / %2\$s (%3\$s)",
        "txasplit_format_download_progress_speed" to "%1\$s / %2\$s (%3\$s) - %4\$s",
        "txasplit_group_tab_bills" to "청구서",
        "txasplit_group_tab_members" to "멤버",
        "txasplit_group_tab_stats" to "통계",
        "txasplit_app_description" to "스마트 그룹 지출 관리 플랫폼",
    )
    
    private fun getFallbackStringsZh(): Map<String, String> = mapOf(
        // Core
        "txasplit_app_name" to "TXASplit",
        "txasplit_ok" to "确定",
        "txasplit_cancel" to "取消",
        "txasplit_unknown_error" to "未知错误",
        "txasplit_error_generic" to "出错了。请重试。",
        "txasplit_version_guard_title" to "不支持的Android版本",
        "txasplit_version_guard_error" to "此应用不支持%1\$s。请升级到Android 14或更高版本以确保安全性和性能。",
        "txasplit_splash_checking" to "检查中...",
        "txasplit_splash_downloading" to "下载中...",
        "txasplit_splash_ready" to "就绪",
        "txasplit_splash_error" to "错误: %1\$s",
        "txasplit_splash_using_cache" to "使用缓存的翻译",
        "txasplit_tab_groups" to "群组",
        "txasplit_tab_summary" to "摘要",
        "txasplit_tab_profile" to "个人资料",
        "txasplit_dashboard_net_balance" to "净余额: %1\$s",
        "txasplit_groups_hint_open_sample" to "点击示例组以打开组详情",
        "txasplit_groups_sample_group" to "示例组",
        "txasplit_settings" to "设置",
        "txasplit_profile" to "个人资料",
        "txasplit_edit_profile" to "编辑个人资料",
        "txasplit_language" to "语言",
        "txasplit_notifications" to "通知",
        "txasplit_ai_key_settings" to "AI配置（开发中）",
        "txasplit_ai_key_saved" to "AI密钥已成功保存（功能尚未激活）",
        "txasplit_ai_key_required" to "需要AI密钥。请在设置中添加密钥。（功能开发中）",
        "txasplit_privacy_policy" to "隐私政策",
        "txasplit_change_password" to "更改密码",
        "txasplit_backup_data" to "备份数据（开发中）",
        "txasplit_sync_data" to "同步数据（开发中）",
        "txasplit_help" to "帮助",
        "txasplit_faq" to "常见问题",
        "txasplit_about" to "关于",
        "txasplit_logout" to "退出登录",
        "txasplit_confirm_logout" to "您确定要退出登录吗？",
        "txasplit_language_system_default" to "系统默认",
        "txasplit_language_checking" to "正在检查更新...",
        "txasplit_language_downloading" to "正在下载语言...",
        "txasplit_language_download_success" to "语言下载成功",
        "txasplit_language_download_failed" to "语言下载失败",
        "txasplit_language_using_fallback" to "使用备用语言",
        "txasplit_language_apply_now" to "立即应用",
        "txasplit_check_update" to "检查更新",
        "txasplit_export_data" to "导出数据",
        "txasplit_version_display" to "版本 %s",
        "txasplit_app_set_id_loading" to "App Set ID: 加载中...",
        "txasplit_app_set_id" to "App Set ID: %s",

        // Permissions (Background Update)
        "txasplit_grant" to "授权",
        "txasplit_open_settings" to "打开设置",
        "txasplit_error_open_settings" to "无法打开设置。请手动打开。",
        "txasplit_perm_all_files_title" to "需要存储权限",
        "txasplit_perm_all_files_message" to "为将API日志保存到 Downloads/TXASPLIT 并启用后台更新检查，请为 TXASplit 授予“所有文件访问权限”。",
        "txasplit_perm_battery_title" to "关闭电池优化",
        "txasplit_perm_battery_title_message" to "为每3分钟在后台检查更新，请允许 TXASplit 忽略电池优化。否则系统可能会停止后台更新。",
        "txasplit_perm_battery_title_message_failed" to "无法打开授权弹窗。请到 设置→电池，将 TXASplit 设置为“不优化/不受限制”以启用后台更新。",
        "txasplit_perm_battery_toast_unavailable" to "无法打开电池设置。不关闭电池优化则后台更新无法工作。",
        "txasplit_perm_battery_toast_required" to "电池优化仍处于启用状态。请将 TXASplit 设为“不优化/不受限制”后后台更新才会工作。",
        "txasplit_lang_en" to "English",
        "txasplit_lang_vi" to "Tiếng Việt",
        "txasplit_lang_ja" to "日本語",
        "txasplit_lang_ko" to "한국어",
        "txasplit_lang_zh" to "中文",
        "txasplit_update_title_no_update" to "无更新",
        "txasplit_update_no_update" to "您使用的是最新版本。",
        "txasplit_update_title_available" to "有更新可用（%1\$s）",
        "txasplit_update_need_permission" to "请允许安装未知应用以继续。",
        "txasplit_update_downloading" to "正在下载更新...",
        "txasplit_update_download" to "下载",
        "txasplit_close" to "关闭",
        "txasplit_update_new_version" to "新版本：%1\$s（%2\$s）",
        "txasplit_update_release_date" to "更新日期：%1\$s",
        "txasplit_update_release_date_unknown" to "更新日期：（未知）",
        "txasplit_update_changelog_empty" to "暂无更新内容。",
        "txasplit_update_unknown_sources_title" to "需要权限",
        "txasplit_update_unknown_sources_message" to "要安装更新，请允许 TXASplit 安装未知应用。点击“授权”打开系统设置。",
        "txasplit_update_retrying_in" to "%1\$s秒后重试...",
        "txasplit_update_download_failed_title" to "下载失败",
        "txasplit_update_download_failed_message" to "无法下载更新。%1\$s",
        "txasplit_update_success_title" to "更新成功",
        "txasplit_update_success_message" to "更新成功！当前版本为%1\$s。",
        "txasplit_update_success_message_generic" to "更新成功！",
        "txasplit_time_just_now" to "刚刚",
        "txasplit_time_minutes_ago" to "%s分钟前",
        "txasplit_time_hours_ago" to "%s小时前",
        "txasplit_time_days_ago" to "%s天前",
        "txasplit_last_backup" to "最后：%s",
        "txasplit_last_sync" to "最后: %s",
        "txasplit_format_calculating" to "计算中...",
        "txasplit_format_remaining_prefix" to "剩余",
        "txasplit_format_remaining_seconds" to "%1\$s秒",
        "txasplit_format_remaining_min_sec" to "%1\$s分钟 %2\$s秒",
        "txasplit_format_remaining_hour_min_sec" to "%1\$s小时 %2\$s分钟 %3\$s秒",
        "txasplit_format_remaining_day_hour_min_sec" to "%1\$s天 %2\$s小时 %3\$s分钟 %4\$s秒",
        "txasplit_format_remaining_month_day_hour" to "%1\$s个月 %2\$s天 %3\$s小时",
        "txasplit_format_remaining_year_month_day" to "%1\$s年 %2\$s个月 %3\$s天",
        "txasplit_format_bytes" to "%s B",
        "txasplit_format_kilobytes" to "%s KB",
        "txasplit_format_megabytes" to "%s MB",
        "txasplit_format_gigabytes" to "%s GB",
        "txasplit_format_speed_bytes" to "%s B/s",
        "txasplit_format_speed_kilobytes" to "%s KB/s",
        "txasplit_format_speed_megabytes" to "%s MB/s",
        "txasplit_format_speed_gigabytes" to "%s GB/s",
        "txasplit_format_percent" to "%s%%",
        "txasplit_format_download_progress" to "%1\$s / %2\$s (%3\$s)",
        "txasplit_format_download_progress_speed" to "%1\$s / %2\$s (%3\$s) - %4\$s",
        "txasplit_group_tab_bills" to "账单",
        "txasplit_group_tab_members" to "成员",
        "txasplit_group_tab_stats" to "统计",
        "txasplit_app_description" to "智能群组支出管理平台",
    )

    fun resolvePreferredLocale(context: Context): String {
        val saved = getSavedLocaleTag(context)
        val systemTag = if (saved.isBlank()) {
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                LocaleList.getDefault().get(0)?.toLanguageTag()
            } else {
                Locale.getDefault().toLanguageTag()
            }
        } else {
            saved
        }

        val lang = systemTag?.substringBefore('-')?.lowercase(Locale.US).orEmpty()
        return when (lang) {
            "vi" -> "vi"
            "ja" -> "ja"
            "en" -> "en"
            else -> "en"
        }
    }

    fun txa(key: String): String {
        return inMemory[key] ?: key
    }

    fun txa(key: String, vararg args: Any?): String {
        val raw = txa(key)
        return try {
            String.format(Locale.getDefault(), raw, *args)
        } catch (_: Throwable) {
            raw
        }
    }

    suspend fun syncIfNewer(context: Context, locale: String = currentLocale): SyncResult {
        val localTs = currentTs
        val url = TXAConstants.BASE_URL.trimEnd('/') + "/tXALocale/$locale"
        val req: Request = TXAHttp.buildGet(url)

        return withContext(Dispatchers.IO) {
            try {
                logApiCall(context, "tXALocale/$locale", url)
                TXAHttp.client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        logApiError(context, "tXALocale/$locale", Exception("HTTP ${resp.code}"))
                        logLocaleSync(context, buildLocaleSyncLog(locale, localTs, null, null, "HTTP_${resp.code}", null))
                        return@withContext SyncResult.Error("HTTP ${resp.code}")
                    }
                    val body = resp.body?.string().orEmpty()
                    logApiResponse(context, "tXALocale/$locale", body)
                    val payload = TXAHttp.json.decodeFromString(TXALocalePayload.serializer(), body)
                    val serverTs = payload.ts

                    if (serverTs > localTs) {
                        writeLocalLocale(context, locale, payload)
                        applyPayload(payload)
                        logLocaleSync(
                            context,
                            buildLocaleSyncLog(
                                locale = locale,
                                localTs = localTs,
                                payload = payload,
                                effectiveTs = serverTs,
                                action = "UPDATED",
                                error = null,
                            ),
                        )
                        return@withContext SyncResult.Updated(serverTs)
                    }
                    logLocaleSync(
                        context,
                        buildLocaleSyncLog(
                            locale = locale,
                            localTs = localTs,
                            payload = payload,
                            effectiveTs = serverTs,
                            action = "SKIPPED_UP_TO_DATE",
                            error = null,
                        ),
                    )
                    return@withContext SyncResult.UpToDate(localTs)
                }
            } catch (t: Throwable) {
                logApiError(context, "tXALocale/$locale", t)
                logLocaleSync(
                    context,
                    buildLocaleSyncLog(
                        locale = locale,
                        localTs = localTs,
                        payload = null,
                        effectiveTs = null,
                        action = "ERROR",
                        error = t,
                    ),
                )
                return@withContext SyncResult.Error(t.message ?: txa("txasplit_unknown_error"))
            }
        }
    }

    private fun applyPayload(payload: TXALocalePayload) {
        currentLocale = payload.locale
        currentTs = payload.ts
        inMemory.clear()
        inMemory.putAll(payload.strings)
    }

    private fun langDir(context: Context): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(base, TXAConstants.LANG_DIR_NAME).apply { mkdirs() }
    }

    private fun localeFile(context: Context, locale: String): File {
        return File(langDir(context), "$locale.json")
    }

    private fun readLocalLocale(context: Context, locale: String): TXALocalePayload? {
        return try {
            val f = localeFile(context, locale)
            if (!f.exists()) return null
            val raw = f.readText()
            TXAHttp.json.decodeFromString(TXALocalePayload.serializer(), raw)
        } catch (_: Throwable) {
            null
        }
    }

    private fun writeLocalLocale(context: Context, locale: String, payload: TXALocalePayload) {
        val f = localeFile(context, locale)
        f.writeText(TXAHttp.json.encodeToString(TXALocalePayload.serializer(), payload))
    }

    private fun buildLocaleSyncLog(
        locale: String,
        localTs: Long,
        payload: TXALocalePayload?,
        effectiveTs: Long?,
        action: String,
        error: Throwable?,
    ): String {
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
            .format(java.util.Date())
        val builder = StringBuilder()
        builder.appendLine("[$timestamp] Locale Sync Debug")
        builder.appendLine("Locale: $locale")
        builder.appendLine("Action: $action")
        builder.appendLine("Local ts: $localTs")
        builder.appendLine("Server raw ts: ${payload?.ts ?: "(null)"}")
        builder.appendLine("Server updated_at: ${payload?.updatedAt ?: "(null)"}")
        builder.appendLine("Effective ts: ${effectiveTs ?: "(null)"}")
        builder.appendLine("Strings count: ${payload?.strings?.size ?: 0}")
        if (error != null) {
            builder.appendLine("Error: ${error.message}")
            builder.appendLine("Stack: ${error.stackTraceToString()}")
        }
        return builder.toString().trimEnd()
    }
    
    private fun logLocaleSync(context: Context, content: String) {
        try {
            val logFile = getLocaleSyncLogFile(context)
            logFile.writeText(content)
        } catch (_: Throwable) {
            // Ignore logging failure
        }
    }
    
    private fun getLocaleSyncLogFile(context: Context): File {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                val logDir = File(downloadsDir, "TXASPLIT")
                logDir.mkdirs()
                File(logDir, "locale_sync_debug.txt")
            } else {
                val hasWritePermission = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ) == PackageManager.PERMISSION_GRANTED
                if (hasWritePermission) {
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    )
                    val logDir = File(downloadsDir, "TXASPLIT")
                    logDir.mkdirs()
                    File(logDir, "locale_sync_debug.txt")
                } else {
                    throw SecurityException("WRITE_EXTERNAL_STORAGE not granted")
                }
            }
        } catch (_: Throwable) {
            val appFilesDir = context.getExternalFilesDir(null) ?: context.filesDir
            val logDir = File(appFilesDir, "logs")
            logDir.mkdirs()
            File(logDir, "locale_sync_debug.txt")
        }
    }
    
    /**
     * Load danh sách locales khả dụng từ API
     * Fallback về danh sách mặc định nếu API fail
     */
    suspend fun getAvailableLocales(context: Context): List<String> {
        val url = TXAConstants.BASE_URL.trimEnd('/') + "/locales"
        val req: Request = TXAHttp.buildGet(url)
        
        return withContext(Dispatchers.IO) {
            try {
                logApiCall(context, "locales", url)
                TXAHttp.client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        logApiError(context, "locales", Exception("HTTP ${resp.code}"))
                        return@withContext getDefaultLocales()
                    }
                    val body = resp.body?.string().orEmpty()
                    logApiResponse(context, "locales", body)
                    val response = TXAHttp.json.decodeFromString(TXALocalesResponse.serializer(), body)
                    if (response.locales.isNotEmpty()) {
                        return@withContext response.locales
                    }
                    return@withContext getDefaultLocales()
                }
            } catch (t: Throwable) {
                // Log error nhưng vẫn trả về default
                logApiError(context, "locales", t)
                return@withContext getDefaultLocales()
            }
        }
    }
    
    private fun getDefaultLocales(): List<String> {
        return listOf("en", "vi", "ja")
    }
    
    /**
     * Log API calls vào Downloads/TXASPLIT folder (hoặc app files nếu không có permission)
     */
    private fun logApiCall(context: Context, endpoint: String, url: String) {
        logToFile(context, "CALL", endpoint, "URL: $url")
    }
    
    /**
     * Log API responses vào Downloads/TXASPLIT folder
     */
    private fun logApiResponse(context: Context, endpoint: String, response: String) {
        // Giới hạn response length để tránh file quá lớn
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
     * Thử ghi vào Downloads/TXASPLIT, nếu fail thì ghi vào app files
     */
    private fun logToFile(context: Context, type: String, endpoint: String, message: String) {
        try {
            val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                .format(java.util.Date())
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val dateStr = dateFormat.format(java.util.Date())
            
            val logEntry = "[$timestamp] $type - Endpoint: $endpoint\n$message\n---\n"
            
            // Thử ghi vào Downloads/TXASPLIT
            val logFile = try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    // Android 11+: Cần MANAGE_EXTERNAL_STORAGE (đã check ở TXAPermissions)
                    // Nếu không có permission thì fallback
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    )
                    val logDir = java.io.File(downloadsDir, "TXASPLIT")
                    logDir.mkdirs()
                    java.io.File(logDir, "api_log_$dateStr.txt")
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    // Android 10: Scoped storage, nhưng vẫn có thể thử ghi vào Downloads
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    )
                    val logDir = java.io.File(downloadsDir, "TXASPLIT")
                    logDir.mkdirs()
                    java.io.File(logDir, "api_log_$dateStr.txt")
                } else {
                    // Android 9 trở xuống: Cần WRITE_EXTERNAL_STORAGE permission
                    val hasWritePermission = ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                    
                    if (hasWritePermission) {
                        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_DOWNLOADS
                        )
                        val logDir = java.io.File(downloadsDir, "TXASPLIT")
                        logDir.mkdirs()
                        java.io.File(logDir, "api_log_$dateStr.txt")
                    } else {
                        // Không có permission, fallback về app files
                        throw SecurityException("WRITE_EXTERNAL_STORAGE not granted")
                    }
                }
            } catch (_: Throwable) {
                // Fallback: Ghi vào app files directory
                val appFilesDir = context.getExternalFilesDir(null) ?: context.filesDir
                val logDir = java.io.File(appFilesDir, "logs")
                logDir.mkdirs()
                java.io.File(logDir, "api_log_$dateStr.txt")
            }
            
            logFile.appendText(logEntry)
        } catch (_: Throwable) {
            // Ignore logging errors
        }
    }
}

sealed class SyncResult {
    data class Updated(val ts: Long) : SyncResult()
    data class UpToDate(val ts: Long) : SyncResult()
    data class Error(val message: String) : SyncResult()
}
