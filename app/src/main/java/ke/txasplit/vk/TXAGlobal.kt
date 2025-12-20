/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAGlobal.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk

import ke.txasplit.vk.core.TXATranslation

// Global helper as requested: button.text = txa("txasplit_btn_save")
fun txa(key: String): String = TXATranslation.txa(key)

fun txa(key: String, vararg args: Any?): String = TXATranslation.txa(key, *args)
