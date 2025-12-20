/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAMainActivity.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.tabs.TabLayoutMediator
import ke.txasplit.vk.core.TXAPermissions
import ke.txasplit.vk.databinding.TxaActivityMainBinding
import ke.txasplit.vk.ui.TXASimpleTextFragment
import ke.txasplit.vk.ui.adapter.TXATabPagerAdapter
import ke.txasplit.vk.ui.fragment.TXAGroupsFragment
import ke.txasplit.vk.update.TXAUpdateDelegate
import ke.txasplit.vk.update.TXAUpdatePostInstall
import ke.txasplit.vk.update.TXAUpdateScheduler

class TXAMainActivity : AppCompatActivity() {

    private lateinit var vb: TxaActivityMainBinding
    private var allFilesDialog: AlertDialog? = null
    private var batteryDialog: AlertDialog? = null
    private var batteryRequestFailedOnce: Boolean = false
    private var batterySettingsOpenedOnce: Boolean = false
    private val updateDelegate by lazy { TXAUpdateDelegate(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = TxaActivityMainBinding.inflate(layoutInflater)
        setContentView(vb.root)

        vb.toolbar.title = txa("txasplit_app_name")
        vb.toolbar.menu.add(txa("txasplit_settings")).setOnMenuItemClickListener {
            startActivity(Intent(this, TXASettingsActivity::class.java))
            true
        }

        val fragments = listOf(
            TXAGroupsFragment(),
            TXASimpleTextFragment.newInstance(txa("txasplit_tab_summary")),
            TXASimpleTextFragment.newInstance(txa("txasplit_tab_profile")),
        )

        vb.pager.adapter = TXATabPagerAdapter(this, fragments)

        val titles = listOf(
            txa("txasplit_tab_groups"),
            txa("txasplit_tab_summary"),
            txa("txasplit_tab_profile"),
        )
        
        val icons = listOf(
            R.drawable.ic_tab_groups,
            R.drawable.ic_tab_summary,
            R.drawable.ic_tab_profile,
        )

        TabLayoutMediator(vb.tabs, vb.pager) { tab, pos ->
            tab.text = titles.getOrNull(pos) ?: ""
            // Thêm icon cho tab
            icons.getOrNull(pos)?.let { iconRes ->
                tab.icon = ContextCompat.getDrawable(this@TXAMainActivity, iconRes)
            }
        }.attach()
    }

    override fun onResume() {
        super.onResume()
        // Phase 4: nếu vừa update xong, show dialog \"Update Successful\" và cleanup APK
        TXAUpdatePostInstall.maybeShowUpdateSuccessDialog(this)

        // Phase 2 auto-resume: nếu user vừa cấp Unknown Sources permission
        updateDelegate.onResume()

        // Auto-check update khi mở app (1 lần / process)
        updateDelegate.maybeAutoCheckOnAppOpen()

        // Request WRITE_EXTERNAL_STORAGE cho Android 9 trở xuống để ghi log API
        ensureStoragePermissionForLogging()
        ensureBackgroundUpdatePermissionsAndSchedule()
    }
    
    private fun ensureStoragePermissionForLogging() {
        // Chỉ cần trên Android 6.0-9 (API 23-28)
        // Android 10+ dùng scoped storage, Android 11+ cần MANAGE_EXTERNAL_STORAGE (đã check ở ensureBackgroundUpdatePermissionsAndSchedule)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // Request permission để ghi log vào Downloads/TXASPLIT
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CODE_WRITE_STORAGE
                )
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_WRITE_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, logs sẽ được ghi vào Downloads/TXASPLIT
            } else {
                // Permission denied, logs sẽ fallback về app files directory
                // Không cần thông báo vì đây là tính năng phụ (logging)
            }
        }
    }
    
    companion object {
        private const val REQUEST_CODE_WRITE_STORAGE = 1001
    }

    private fun ensureBackgroundUpdatePermissionsAndSchedule() {
        // 1) All files access (Android 11+)
        if (!TXAPermissions.hasAllFilesAccess(this)) {
            TXAUpdateScheduler.cancel(this)
            showAllFilesAccessDialog()
            return
        } else {
            allFilesDialog?.dismiss()
            allFilesDialog = null
        }

        // 2) Ignore battery optimizations
        if (!TXAPermissions.isIgnoringBatteryOptimizations(this)) {
            TXAUpdateScheduler.cancel(this)
            if (batterySettingsOpenedOnce) {
                batterySettingsOpenedOnce = false
                Toast.makeText(this, txa("txasplit_perm_battery_toast_required"), Toast.LENGTH_LONG).show()
            }
            showBatteryOptimizationDialog()
            return
        } else {
            batteryDialog?.dismiss()
            batteryDialog = null
            batteryRequestFailedOnce = false
            batterySettingsOpenedOnce = false
        }

        // 3) Đủ quyền -> schedule update worker chạy 3 phút/lần
        TXAUpdateScheduler.ensureScheduledNow(this)
    }

    private fun showAllFilesAccessDialog() {
        if (allFilesDialog?.isShowing == true) return

        allFilesDialog = AlertDialog.Builder(this)
            .setTitle(txa("txasplit_perm_all_files_title"))
            .setMessage(txa("txasplit_perm_all_files_message"))
            .setCancelable(false)
            .setPositiveButton(txa("txasplit_grant")) { _, _ ->
                try {
                    val intent = TXAPermissions.createManageAllFilesAccessIntent(this)
                    if (intent == null) {
                        Toast.makeText(this, txa("txasplit_error_open_settings"), Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }
                    startActivity(intent)
                } catch (_: Throwable) {
                    Toast.makeText(this, txa("txasplit_error_open_settings"), Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(txa("txasplit_cancel")) { d, _ -> d.dismiss() }
            .show()
    }

    private fun showBatteryOptimizationDialog() {
        if (batteryDialog?.isShowing == true) return

        val message = if (!batteryRequestFailedOnce) {
            txa("txasplit_perm_battery_title_message")
        } else {
            txa("txasplit_perm_battery_title_message_failed")
        }

        val positiveText = if (!batteryRequestFailedOnce) {
            txa("txasplit_grant")
        } else {
            txa("txasplit_open_settings")
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(txa("txasplit_perm_battery_title"))
            .setMessage(message)
            .setCancelable(false)
            // set null listener để tự control click (đổi nội dung / đổi hành vi)
            .setPositiveButton(positiveText, null)
            .setNegativeButton(txa("txasplit_cancel")) { d, _ -> d.dismiss() }
            .create()

        dialog.setOnShowListener {
            val btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            btn.setOnClickListener {
                if (!batteryRequestFailedOnce) {
                    // Try open system popup allowlist
                    try {
                        val intent = TXAPermissions.createRequestIgnoreBatteryOptimizationsIntent(this)
                        if (intent == null) {
                            batteryRequestFailedOnce = true
                            dialog.dismiss()
                            showBatteryOptimizationDialog()
                            return@setOnClickListener
                        }
                        startActivity(intent)
                        dialog.dismiss()
                    } catch (_: Throwable) {
                        // Popup không mở được -> đổi nội dung + đổi nút sang "Mở cài đặt"
                        batteryRequestFailedOnce = true
                        dialog.dismiss()
                        showBatteryOptimizationDialog()
                    }
                } else {
                    // Fallback: mở đúng trang cài đặt app để user tự set "Don't optimize/Unrestricted"
                    try {
                        batterySettingsOpenedOnce = true
                        startActivity(TXAPermissions.createBatteryOptimizationSettingsIntent(this))
                        dialog.dismiss()
                    } catch (_: Throwable) {
                        Toast.makeText(
                            this,
                            txa("txasplit_perm_battery_toast_unavailable"),
                            Toast.LENGTH_LONG
                        ).show()
                        TXAUpdateScheduler.cancel(this)
                        dialog.dismiss()
                    }
                }
            }
        }

        batteryDialog = dialog
        dialog.show()
    }
}
