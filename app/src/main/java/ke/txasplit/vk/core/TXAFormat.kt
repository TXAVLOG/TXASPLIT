/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAFormat.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.core

import ke.txasplit.vk.txa
import kotlin.math.max

object TXAFormat {

    fun twoDigits(value: Long): String = value.toString().padStart(2, '0')

    fun formatPercent(value: Double): String {
        val p = "%.2f".format(value)
        return txa("txasplit_format_percent", p)
    }

    fun formatSize(bytes: Long): String {
        val b = max(0, bytes)
        return when {
            b < 1024 -> txa("txasplit_format_bytes", b.toString())
            b < 1024L * 1024L -> txa("txasplit_format_kilobytes", "%.2f".format(b / 1024.0))
            b < 1024L * 1024L * 1024L -> txa("txasplit_format_megabytes", "%.2f".format(b / (1024.0 * 1024.0)))
            else -> txa("txasplit_format_gigabytes", "%.2f".format(b / (1024.0 * 1024.0 * 1024.0)))
        }
    }

    fun formatSpeed(bytesPerSec: Double): String {
        val bps = max(0.0, bytesPerSec)
        return when {
            bps < 1024.0 -> txa("txasplit_format_speed_bytes", "%.2f".format(bps))
            bps < 1024.0 * 1024.0 -> txa("txasplit_format_speed_kilobytes", "%.2f".format(bps / 1024.0))
            bps < 1024.0 * 1024.0 * 1024.0 -> txa("txasplit_format_speed_megabytes", "%.2f".format(bps / (1024.0 * 1024.0)))
            else -> txa("txasplit_format_speed_gigabytes", "%.2f".format(bps / (1024.0 * 1024.0 * 1024.0)))
        }
    }

    fun formatRemainingSeconds(seconds: Long): String {
        if (seconds < 0) return txa("txasplit_format_calculating")

        val s = seconds
        return when {
            s < 60 -> txa("txasplit_format_remaining_seconds", twoDigits(s))
            s < 60 * 60 -> {
                val m = s / 60
                val ss = s % 60
                txa("txasplit_format_remaining_min_sec", twoDigits(m), twoDigits(ss))
            }
            s < 24 * 60 * 60 -> {
                val h = s / 3600
                val rem = s % 3600
                val m = rem / 60
                val ss = rem % 60
                txa("txasplit_format_remaining_hour_min_sec", twoDigits(h), twoDigits(m), twoDigits(ss))
            }
            else -> {
                val d = s / (24 * 3600)
                val rem1 = s % (24 * 3600)
                val h = rem1 / 3600
                val rem2 = rem1 % 3600
                val m = rem2 / 60
                val ss = rem2 % 60
                txa("txasplit_format_remaining_day_hour_min_sec", twoDigits(d), twoDigits(h), twoDigits(m), twoDigits(ss))
            }
        }
    }

    fun formatProgress(downloadedBytes: Long, totalBytes: Long, percent: Double): String {
        return txa(
            "txasplit_format_download_progress",
            formatSize(downloadedBytes),
            formatSize(totalBytes),
            formatPercent(percent),
        )
    }

    fun formatProgressWithSpeed(downloadedBytes: Long, totalBytes: Long, percent: Double, speedBps: Double): String {
        return txa(
            "txasplit_format_download_progress_speed",
            formatSize(downloadedBytes),
            formatSize(totalBytes),
            formatPercent(percent),
            formatSpeed(speedBps),
        )
    }
}
