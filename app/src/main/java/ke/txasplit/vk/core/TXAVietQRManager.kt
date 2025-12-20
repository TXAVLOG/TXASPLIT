/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAVietQRManager.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Manager for VietQR 2.0 integration
 * Handles QR code generation, validation, and bank information management
 */
object TXAVietQRManager {
    
    /**
     * VietQR API base URL
     */
    private const val VIETQR_BASE_URL = "https://img.vietqr.io/image"
    
    /**
     * JSON serializer for bank info
     */
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Generate VietQR URL for payment
     * @param bankInfo Bank information object
     * @param amount Payment amount in VND
     * @param addInfo Additional information for transfer
     * @return Complete VietQR image URL
     */
    fun generateVietQRUrl(bankInfo: TXABankInfo, amount: Long, addInfo: String = ""): String {
        if (!bankInfo.isValid()) {
            throw IllegalArgumentException("Invalid bank information")
        }
        
        val encodedAddInfo = URLEncoder.encode(addInfo, StandardCharsets.UTF_8.toString())
        val encodedAccountName = URLEncoder.encode(bankInfo.accountName, StandardCharsets.UTF_8.toString())
        
        return "$VIETQR_BASE_URL/${bankInfo.bankId}-${bankInfo.accountNumber}-compact2.png" +
               "?amount=$amount" +
               "&addInfo=$encodedAddInfo" +
               "&accountName=$encodedAccountName"
    }
    
    /**
     * Generate VietQR URL for payment with JSON bank info string
     * @param bankInfoJson Bank information as JSON string
     * @param amount Payment amount in VND
     * @param addInfo Additional information for transfer
     * @return Complete VietQR image URL or null if invalid
     */
    fun generateVietQRUrl(bankInfoJson: String, amount: Long, addInfo: String = ""): String? {
        return try {
            val bankInfo = parseBankInfo(bankInfoJson)
            generateVietQRUrl(bankInfo, amount, addInfo)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Parse bank information from JSON string
     * @param bankInfoJson JSON string containing bank information
     * @return TXABankInfo object
     */
    fun parseBankInfo(bankInfoJson: String): TXABankInfo {
        return try {
            json.decodeFromString(TXABankInfo.serializer(), bankInfoJson)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid bank information format", e)
        }
    }
    
    /**
     * Serialize bank information to JSON string
     * @param bankInfo Bank information object
     * @return JSON string
     */
    fun serializeBankInfo(bankInfo: TXABankInfo): String {
        return json.encodeToString(TXABankInfo.serializer(), bankInfo)
    }
    
    /**
     * Validate bank ID format
     * @param bankId Bank ID to validate
     * @return True if valid, false otherwise
     */
    fun isValidBankId(bankId: String): Boolean {
        // Vietnamese bank IDs are typically 3-6 characters, alphanumeric
        return bankId.matches(Regex("^[A-Z0-9]{3,6}$"))
    }
    
    /**
     * Validate Vietnamese bank account number
     * @param accountNumber Account number to validate
     * @return True if valid, false otherwise
     */
    fun isValidAccountNumber(accountNumber: String): Boolean {
        // Vietnamese bank account numbers are typically 6-20 digits
        return accountNumber.matches(Regex("^\\d{6,20}$"))
    }
    
    /**
     * Validate account name
     * @param accountName Account name to validate
     * @return True if valid, false otherwise
     */
    fun isValidAccountName(accountName: String): Boolean {
        // Account names should be 2-50 characters, letters and spaces only
        return accountName.trim().matches(Regex("^[A-Za-zÀ-ỹ\\s]{2,50}$"))
    }
    
    /**
     * Validate payment amount
     * @param amount Amount in VND
     * @return True if valid, false otherwise
     */
    fun isValidAmount(amount: Long): Boolean {
        // Amount should be between 1,000 VND and 1,000,000,000 VND
        return amount >= 1000 && amount <= 1_000_000_000L
    }
    
    /**
     * Generate default additional information for transfer
     * @param groupName Group name
     * @param payerName Payer name
     * @return Formatted additional information
     */
    fun generateDefaultAddInfo(groupName: String, payerName: String): String {
        return "TXASplit - $groupName - $payerName"
    }
    
    /**
     * Check if bank information is configured and valid
     * @param bankInfoJson Bank information as JSON string
     * @return True if configured and valid, false otherwise
     */
    fun isBankInfoConfigured(bankInfoJson: String): Boolean {
        return try {
            val bankInfo = parseBankInfo(bankInfoJson)
            bankInfo.isValid()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get bank display name from bank ID
     * @param bankId Bank ID
     * @return Display name or original ID if not found
     */
    fun getBankDisplayName(bankId: String): String {
        return BANK_NAMES[bankId] ?: bankId
    }
    
    /**
     * Get list of supported Vietnamese banks
     * @return Map of bank ID to bank name
     */
    fun getSupportedBanks(): Map<String, String> {
        return BANK_NAMES
    }
    
    /**
     * Popular Vietnamese banks for quick selection
     */
    private val BANK_NAMES = mapOf(
        "VCB" to "Vietcombank",
        "TCB" to "Techcombank", 
        "MB" to "MB Bank",
        "VTB" to "VietinBank",
        "STB" to "Sacombank",
        "ACB" to "Asia Commercial Bank",
        "HDB" to "HDBank",
        "TPB" to "TPBank",
        "VAB" to "VietABank",
        "BIDV" to "BIDV",
        "AGR" to "Agribank",
        "CIMB" to "CIMB Bank",
        "HSBC" to "HSBC Vietnam",
        "ANZ" to "ANZ Bank",
        "CITI" to "Citibank",
        "UOB" to "UOB Vietnam",
        "SHB" to "SHB Bank",
        "VPB" to "VP Bank",
        "SCB" to "Sacombank",
        "EXB" to "Export Import Bank",
        "NAB" to "National Bank",
        "PGB" to "PGBank",
        "MSB" to "Maritime Bank",
        "NCB" to "National Citizen Bank",
        "OCB" to "Ocean Bank",
        "SEAB" to "Southeast Asia Bank",
        "KLB" to "Kienlongbank",
        "BVB" to "Baoviet Bank",
        "PG" to "Petrolimex Group Bank",
        "GB" to "Global Bank"
    )
}

/**
 * Bank information data class for VietQR
 */
@Serializable
data class TXABankInfo(
    val bankId: String,
    val accountNumber: String,
    val accountName: String
) {
    /**
     * Check if bank information is complete and valid
     */
    fun isValid(): Boolean {
        return TXAVietQRManager.isValidBankId(bankId) &&
               TXAVietQRManager.isValidAccountNumber(accountNumber) &&
               TXAVietQRManager.isValidAccountName(accountName)
    }
    
    /**
     * Get bank display name
     */
    fun getBankDisplayName(): String {
        return TXAVietQRManager.getBankDisplayName(bankId)
    }
    
    /**
     * Generate VietQR URL for payment
     */
    fun generateVietQRUrl(amount: Long, addInfo: String = ""): String {
        return TXAVietQRManager.generateVietQRUrl(this, amount, addInfo)
    }
}
