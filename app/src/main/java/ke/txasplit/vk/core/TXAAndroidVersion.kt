/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAAndroidVersion.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.core

import android.os.Build

object TXAAndroidVersion {

    fun sdkToMarketingName(sdkInt: Int): String {
        return when (sdkInt) {
            34 -> "Android 14 (Upside Down Cake)"
            33 -> "Android 13 (Tiramisu)"
            32 -> "Android 12L (Snow Cone v2)"
            31 -> "Android 12 (Snow Cone)"
            30 -> "Android 11 (Red Velvet Cake)"
            29 -> "Android 10"
            28 -> "Android 9 (Pie)"
            27 -> "Android 8.1 (Oreo)"
            26 -> "Android 8.0 (Oreo)"
            25 -> "Android 7.1 (Nougat)"
            24 -> "Android 7.0 (Nougat)"
            23 -> "Android 6.0 (Marshmallow)"
            22 -> "Android 5.1 (Lollipop)"
            21 -> "Android 5.0 (Lollipop)"
            20 -> "Android 4.4W (KitKat Wear)"
            19 -> "Android 4.4 (KitKat)"
            18 -> "Android 4.3 (Jelly Bean)"
            17 -> "Android 4.2 (Jelly Bean)"
            16 -> "Android 4.1 (Jelly Bean)"
            else -> "Android (API $sdkInt)"
        }
    }

    fun currentMarketingName(): String = sdkToMarketingName(Build.VERSION.SDK_INT)
}
