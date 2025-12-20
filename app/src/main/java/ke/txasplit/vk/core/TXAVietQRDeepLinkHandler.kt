/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAVietQRDeepLinkHandler.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import ke.txasplit.vk.R
import ke.txasplit.vk.txa

/**
 * Handler for VietQR deep links and payment actions
 * Provides both banking app deep links and copy-to-clipboard functionality
 */
object TXAVietQRDeepLinkHandler {
    
    /**
     * Handle payment button click - opens banking app or shows copy option
     * @param context Current context
     * @param bankInfo Bank information for payment
     * @param amount Payment amount
     * @param addInfo Additional transfer information
     * @param onCopyRequested Callback when user wants to copy payment details
     */
    fun handlePaymentAction(
        context: Context,
        bankInfo: TXABankInfo,
        amount: Long,
        addInfo: String,
        onCopyRequested: (String) -> Unit = {}
    ) {
        try {
            val bankingAppIntent = createBankingAppIntent(bankInfo)
            
            if (bankingAppIntent != null) {
                // Try to open banking app directly
                context.startActivity(bankingAppIntent)
                Toast.makeText(context, txa("txa_vietqr_opening_banking_app"), Toast.LENGTH_SHORT).show()
            } else {
                // Fallback to copy payment details
                val paymentDetails = formatPaymentDetails(bankInfo, amount, addInfo)
                onCopyRequested(paymentDetails)
                copyToClipboard(context, paymentDetails)
                Toast.makeText(context, txa("txa_vietqr_payment_details_copied"), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            // Error handling - fallback to copy
            val paymentDetails = formatPaymentDetails(bankInfo, amount, addInfo)
            onCopyRequested(paymentDetails)
            copyToClipboard(context, paymentDetails)
            Toast.makeText(context, txa("txa_vietqr_error_fallback_copy"), Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Create intent to open specific banking app
     * @param bankInfo Bank information
     * @return Intent for banking app or null if not found
     */
    private fun createBankingAppIntent(bankInfo: TXABankInfo): Intent? {
        val packageName = getBankingAppPackage(bankInfo.bankId)
            ?: return null
        
        return try {
            Intent().apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get banking app package name from bank ID
     * @param bankId Bank ID
     * @return Package name or null if not found
     */
    private fun getBankingAppPackage(bankId: String): String? {
        return BANKING_APP_PACKAGES[bankId]
    }
    
    /**
     * Format payment details for copying
     * @param bankInfo Bank information
     * @param amount Payment amount
     * @param addInfo Additional information
     * @return Formatted payment details string
     */
    fun formatPaymentDetails(bankInfo: TXABankInfo, amount: Long, addInfo: String): String {
        val bankName = TXAVietQRManager.getBankDisplayName(bankInfo.bankId)
        val formattedAmount = formatAmount(amount)
        
        return buildString {
            appendLine("=== TXASplit Payment Details ===")
            appendLine("Bank: $bankName")
            appendLine("Account Number: ${bankInfo.accountNumber}")
            appendLine("Account Name: ${bankInfo.accountName}")
            appendLine("Amount: $formattedAmount")
            if (addInfo.isNotBlank()) {
                appendLine("Note: $addInfo")
            }
            appendLine("===============================")
        }
    }
    
    /**
     * Copy text to clipboard
     * @param context Current context
     * @param text Text to copy
     */
    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("TXASplit Payment", text)
        clipboard.setPrimaryClip(clip)
    }
    
    /**
     * Format amount with currency symbol
     * @param amount Amount in VND
     * @return Formatted amount string
     */
    private fun formatAmount(amount: Long): String {
        return String.format("%,d VND", amount)
    }
    
    /**
     * Check if banking app is available
     * @param context Current context
     * @param bankId Bank ID
     * @return True if app is available, false otherwise
     */
    fun isBankingAppAvailable(context: Context, bankId: String): Boolean {
        val packageName = getBankingAppPackage(bankId) ?: return false
        
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get available payment options for a bank
     * @param context Current context
     * @param bankId Bank ID
     * @return List of available payment options
     */
    fun getAvailablePaymentOptions(context: Context, bankId: String): List<PaymentOption> {
        val options = mutableListOf<PaymentOption>()
        
        // Always add copy option
        options.add(PaymentOption.COPY_DETAILS)
        
        // Add banking app option if available
        if (isBankingAppAvailable(context, bankId)) {
            options.add(PaymentOption.OPEN_BANKING_APP)
        }
        
        return options
    }
    
    /**
     * Payment options enum
     */
    enum class PaymentOption {
        OPEN_BANKING_APP,
        COPY_DETAILS
    }
    
    /**
     * Popular Vietnamese banking app package names
     */
    private val BANKING_APP_PACKAGES = mapOf(
        "VCB" to "com.vcb.mobile",
        "TCB" to "com.techcombank.mpbank",
        "MB" to "com.mb.mobile",
        "VTB" to "com.vietinbank.ipay",
        "STB" to "com.sacombank.smart",
        "ACB" to "com.acb.digital",
        "HDB" to "com.hdbank.mobilebank",
        "TPB" to "com.tpb.mb",
        "VAB" to "com.vietabank",
        "BIDV" to "com.bidv.mobilebanking",
        "AGR" to "com.agribank.ebanking",
        "CIMB" to "com.cimbniaga.mobile",
        "HSBC" to "com.hsbc.hsbckey",
        "ANZ" to "com.anz.android.gomobile",
        "CITI" to "com.citi.mobile",
        "UOB" to "com.uob.mobile",
        "SHB" to "com.shb.mb",
        "VPB" to "com.vpbank.online",
        "SCB" to "com.sacombank.smart", // Same as STB
        "EXB" to "com.eximbank.mobilebanking",
        "NAB" to "com.nab.mobile",
        "PGB" to "com.pgbank.mobile",
        "MSB" to "com.maritimebank.mobilebanking",
        "NCB" to "com.ncb.mb",
        "OCB" to "com.ocb.omni",
        "SEAB" to "com.seabank.mobile",
        "KLB" to "com.kienlongbank.mobile",
        "BVB" to "com.baovietbank.mobilebanking",
        "PG" to "com.pgbank.mobile"
    )
}
