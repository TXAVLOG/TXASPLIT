/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXADemoCompleteActivity.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ke.txasplit.vk.databinding.TxaActivityDemoCompleteBinding

class TXADemoCompleteActivity : AppCompatActivity() {

    private lateinit var vb: TxaActivityDemoCompleteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = TxaActivityDemoCompleteBinding.inflate(layoutInflater)
        setContentView(vb.root)

        setupUI()
    }

    private fun setupUI() {
        // Set title
        vb.toolbar.title = txa("txasplit_demo_complete_title")
        
        // Set message
        vb.messageText.text = txa("txasplit_demo_complete_message")
        
        // Set sub message
        vb.subMessageText.text = txa("txasplit_demo_complete_sub_message")
        
        // Set thank you message
        vb.thankYouText.text = txa("txasplit_demo_complete_thank_you")
        
        // Set contact info
        vb.contactInfo.text = txa("txasplit_demo_complete_contact")
        
        // Close button
        vb.closeButton.text = txa("txasplit_demo_complete_close")
        vb.closeButton.setOnClickListener {
            finishAffinity()
        }
    }
}
