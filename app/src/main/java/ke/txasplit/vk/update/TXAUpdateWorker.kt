/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAUpdateWorker.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ke.txasplit.vk.core.TXAPermissions

class TXAUpdateWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Gate: nếu chưa cấp quyền thì dừng background worker ngay.
        val ctx = applicationContext
        val hasAllFiles = TXAPermissions.hasAllFilesAccess(ctx)
        val ignoreBattery = TXAPermissions.isIgnoringBatteryOptimizations(ctx)
        if (!hasAllFiles || !ignoreBattery) {
            TXAUpdateScheduler.cancel(ctx)
            return Result.success()
        }

        return try {
            // Background check (silent).
            TXAUpdateManager.check(ctx)
            Result.success()
        } catch (_: Throwable) {
            // Không retry theo backoff (vì cần 3 phút/lần). Sẽ lên lịch lại ở finally.
            Result.success()
        } finally {
            // Lên lịch lần tiếp theo sau 3 phút.
            // Nếu user vừa revoke quyền trong lúc chạy thì cũng sẽ bị cancel ở lần sau.
            if (TXAPermissions.hasAllFilesAccess(ctx) && TXAPermissions.isIgnoringBatteryOptimizations(ctx)) {
                TXAUpdateScheduler.scheduleNext(ctx)
            } else {
                TXAUpdateScheduler.cancel(ctx)
            }
        }
    }
}
