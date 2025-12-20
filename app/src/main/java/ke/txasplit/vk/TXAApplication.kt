/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAApplication.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main Application class for TXASplit
 * Required for Hilt dependency injection setup
 */
@HiltAndroidApp
class TXAApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize core components
        initializeCoreComponents()
    }
    
    /**
     * Initialize core application components
     */
    private fun initializeCoreComponents() {
        // Initialize translation system
        ke.txasplit.vk.core.TXATranslation.init(this)
        
        // Other initializations can be added here:
        // - Notification channels
        // - WorkManager setup
        // - Analytics
        // - Error reporting
    }
}
