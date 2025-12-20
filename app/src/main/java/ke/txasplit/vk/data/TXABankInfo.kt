package ke.txasplit.vk.data

import kotlinx.serialization.Serializable

/**
 * Data class for bank information stored in JSON format
 */
@Serializable
data class TXABankInfo(
    val bankId: String,
    val accountNumber: String,
    val accountName: String
) {
    /**
     * Generate VietQR URL for this bank account
     */
    fun generateVietQRUrl(amount: Long, addInfo: String): String {
        return "https://img.vietqr.io/image/${bankId}-${accountNumber}-compact2.png?amount=${amount}&addInfo=${addInfo}&accountName=${accountName}"
    }
    
    /**
     * Check if bank information is complete and valid
     */
    fun isValid(): Boolean {
        return bankId.isNotBlank() && 
               accountNumber.isNotBlank() && 
               accountName.isNotBlank()
    }
}

// Conversion functions between data and core TXABankInfo
fun convertDataToCoreBankInfo(dataBankInfo: ke.txasplit.vk.data.TXABankInfo): ke.txasplit.vk.core.TXABankInfo {
    return ke.txasplit.vk.core.TXABankInfo(
        bankId = dataBankInfo.bankId,
        accountNumber = dataBankInfo.accountNumber,
        accountName = dataBankInfo.accountName
    )
}

fun convertCoreToDataBankInfo(coreBankInfo: ke.txasplit.vk.core.TXABankInfo): ke.txasplit.vk.data.TXABankInfo {
    return ke.txasplit.vk.data.TXABankInfo(
        bankId = coreBankInfo.bankId,
        accountNumber = coreBankInfo.accountNumber,
        accountName = coreBankInfo.accountName
    )
}
