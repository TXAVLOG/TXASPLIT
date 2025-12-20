/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAVietQRView.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.ui

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import ke.txasplit.vk.R
import ke.txasplit.vk.core.TXAVietQRManager

/**
 * Custom view for displaying VietQR codes with security features
 * Shows blurred QR with overlay when bank info is not configured
 * Automatically updates when bank info becomes available
 */
class TXAVietQRView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val qrImageView: ImageView
    private val overlayView: TextView
    private val loadingView: TextView
    
    private var currentBankInfo: String = ""
    private var currentAmount: Long = 0
    private var currentAddInfo: String = ""
    
    init {
        LayoutInflater.from(context).inflate(R.layout.view_vietqr, this, true)
        
        qrImageView = findViewById(R.id.qrImageView)
        overlayView = findViewById(R.id.overlayView)
        loadingView = findViewById(R.id.loadingView)
        
        setupInitialState()
    }
    
    /**
     * Setup initial state with blur and overlay
     */
    private fun setupInitialState() {
        showBankInfoRequiredState()
    }
    
    /**
     * Load QR code with bank information
     * @param bankInfoJson Bank information as JSON string
     * @param amount Payment amount
     * @param addInfo Additional transfer information
     */
    fun loadQRCode(bankInfoJson: String, amount: Long = 0, addInfo: String = "") {
        currentBankInfo = bankInfoJson
        currentAmount = amount
        currentAddInfo = addInfo
        
        if (TXAVietQRManager.isBankInfoConfigured(bankInfoJson)) {
            showQRCode(bankInfoJson, amount, addInfo)
        } else {
            showBankInfoRequiredState()
        }
    }
    
    /**
     * Update QR code with new amount
     * @param amount New payment amount
     */
    fun updateAmount(amount: Long) {
        currentAmount = amount
        if (TXAVietQRManager.isBankInfoConfigured(currentBankInfo)) {
            loadQRCode(currentBankInfo, amount, currentAddInfo)
        }
    }
    
    /**
     * Update QR code with new additional info
     * @param addInfo New additional information
     */
    fun updateAddInfo(addInfo: String) {
        currentAddInfo = addInfo
        if (TXAVietQRManager.isBankInfoConfigured(currentBankInfo)) {
            loadQRCode(currentBankInfo, currentAmount, addInfo)
        }
    }
    
    /**
     * Show actual QR code (unblurred)
     */
    private fun showQRCode(bankInfoJson: String, amount: Long, addInfo: String) {
        val qrUrl = TXAVietQRManager.generateVietQRUrl(bankInfoJson, amount, addInfo)
        
        if (qrUrl != null) {
            // Hide overlay and loading
            overlayView.isVisible = false
            loadingView.isVisible = false
            
            // Remove blur effect
            removeBlurEffect()
            
            // Load QR code image
            Glide.with(context)
                .load(qrUrl)
                .placeholder(R.drawable.ic_qr_placeholder)
                .error(R.drawable.ic_qr_error)
                .into(qrImageView)
        } else {
            showErrorState()
        }
    }
    
    /**
     * Show bank info required state with blur and overlay
     */
    private fun showBankInfoRequiredState() {
        // Show overlay message
        overlayView.isVisible = true
        overlayView.text = context.getString(R.string.txa_vietqr_setup_required)
        
        // Hide loading
        loadingView.isVisible = false
        
        // Apply blur effect
        applyBlurEffect()
        
        // Show placeholder
        qrImageView.setImageResource(R.drawable.ic_qr_placeholder)
    }
    
    /**
     * Show loading state
     */
    private fun showLoadingState() {
        loadingView.isVisible = true
        loadingView.text = context.getString(R.string.txa_vietqr_loading)
        
        overlayView.isVisible = false
        qrImageView.setImageResource(R.drawable.ic_qr_placeholder)
    }
    
    /**
     * Show error state
     */
    private fun showErrorState() {
        overlayView.isVisible = true
        overlayView.text = context.getString(R.string.txa_vietqr_error)
        
        loadingView.isVisible = false
        qrImageView.setImageResource(R.drawable.ic_qr_error)
    }
    
    /**
     * Apply blur effect to QR image (for security)
     */
    private fun applyBlurEffect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            qrImageView.setRenderEffect(
                RenderEffect.createBlurEffect(
                    10f, 10f,
                    Shader.TileMode.CLAMP
                )
            )
        } else {
            // For older versions, we can use a different approach
            // such as a dim overlay or alpha transparency
            qrImageView.alpha = 0.3f
        }
    }
    
    /**
     * Remove blur effect when bank info is configured
     */
    private fun removeBlurEffect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            qrImageView.setRenderEffect(null)
        } else {
            qrImageView.alpha = 1.0f
        }
    }
    
    /**
     * Check if QR code is ready for display (bank info configured)
     */
    fun isQRReady(): Boolean {
        return TXAVietQRManager.isBankInfoConfigured(currentBankInfo)
    }
    
    /**
     * Get current bank information
     */
    fun getCurrentBankInfo(): String {
        return currentBankInfo
    }
    
    /**
     * Get current payment amount
     */
    fun getCurrentAmount(): Long {
        return currentAmount
    }
    
    /**
     * Get current additional info
     */
    fun getCurrentAddInfo(): String {
        return currentAddInfo
    }
}
