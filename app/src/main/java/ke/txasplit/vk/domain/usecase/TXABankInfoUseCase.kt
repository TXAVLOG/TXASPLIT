/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXABankInfoUseCase.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.domain.usecase

import ke.txasplit.vk.data.TXABankInfo
import ke.txasplit.vk.data.convertDataToCoreBankInfo
import ke.txasplit.vk.data.convertCoreToDataBankInfo
import ke.txasplit.vk.data.repository.TXAGroupRepository
import ke.txasplit.vk.core.TXAVietQRManager
import javax.inject.Inject

/**
 * Use case for managing bank information in groups
 * Handles validation, storage, and QR code generation for VietQR payments
 */
class TXABankInfoUseCase @Inject constructor(
    private val groupRepository: TXAGroupRepository
) {
    
    /**
     * Update bank information for a group
     * @param groupId ID of the group
     * @param bankId Bank ID (e.g., "VCB" for Vietcombank)
     * @param accountNumber Bank account number
     * @param accountName Account holder name
     * @param requestedBy ID of the user making the request
     * @return Result indicating success or error
     */
    suspend fun updateBankInfo(
        groupId: String,
        bankId: String,
        accountNumber: String,
        accountName: String,
        requestedBy: String
    ): Result<Unit> {
        // Validate input
        if (groupId.isBlank() || requestedBy.isBlank()) {
            return Result.failure(IllegalArgumentException("Group ID and requester ID cannot be empty"))
        }
        
        if (!ke.txasplit.vk.core.TXAVietQRManager.isValidBankId(bankId)) {
            return Result.failure(IllegalArgumentException("Invalid bank ID format"))
        }
        
        if (!ke.txasplit.vk.core.TXAVietQRManager.isValidAccountNumber(accountNumber)) {
            return Result.failure(IllegalArgumentException("Invalid account number format"))
        }
        
        if (!ke.txasplit.vk.core.TXAVietQRManager.isValidAccountName(accountName)) {
            return Result.failure(IllegalArgumentException("Invalid account name format"))
        }
        
        // Check if requester has permission (Host or Co-host)
        val requesterRole = groupRepository.getUserRoleInGroup(requestedBy, groupId)
        if (requesterRole != ke.txasplit.vk.data.TXAMemberRole.HOST && 
            requesterRole != ke.txasplit.vk.data.TXAMemberRole.CO_HOST) {
            return Result.failure(IllegalArgumentException("Only Host or Co-host can update bank information"))
        }
        
        // Create bank info object
        val bankInfo = TXABankInfo(
            bankId = bankId.trim().uppercase(),
            accountNumber = accountNumber.trim(),
            accountName = accountName.trim()
        )
        
        // Serialize to JSON
        val bankInfoJson = ke.txasplit.vk.core.TXAVietQRManager.serializeBankInfo(convertDataToCoreBankInfo(bankInfo))
        
        // Update group bank info
        return groupRepository.updateBankInfo(groupId, bankInfoJson, requestedBy)
    }
    
    /**
     * Get bank information for a group
     * @param groupId ID of the group
     * @return Result containing bank info or error
     */
    suspend fun getBankInfo(groupId: String): Result<TXABankInfo?> {
        if (groupId.isBlank()) {
            return Result.failure(IllegalArgumentException("Group ID cannot be empty"))
        }
        
        return try {
            val group = groupRepository.getGroupById(groupId)
                ?: return Result.failure(IllegalArgumentException("Group not found"))
            
            val bankInfo = if (group.hasBankInfo()) {
                convertCoreToDataBankInfo(ke.txasplit.vk.core.TXAVietQRManager.parseBankInfo(group.bankInfo))
            } else {
                null
            }
            
            Result.success(bankInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generate VietQR URL for payment
     * @param groupId ID of the group
     * @param amount Payment amount in VND
     * @param addInfo Additional transfer information
     * @return Result containing VietQR URL or error
     */
    suspend fun generateVietQRUrl(
        groupId: String,
        amount: Long,
        addInfo: String = ""
    ): Result<String> {
        if (groupId.isBlank()) {
            return Result.failure(IllegalArgumentException("Group ID cannot be empty"))
        }
        
        if (!TXAVietQRManager.isValidAmount(amount)) {
            return Result.failure(IllegalArgumentException("Invalid payment amount"))
        }
        
        return try {
            val group = groupRepository.getGroupById(groupId)
                ?: return Result.failure(IllegalArgumentException("Group not found"))
            
            if (!group.hasBankInfo()) {
                return Result.failure(IllegalArgumentException("Bank information not configured"))
            }
            
            val qrUrl = ke.txasplit.vk.core.TXAVietQRManager.generateVietQRUrl(group.bankInfo, amount, addInfo)
            Result.success(qrUrl ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Remove bank information from a group
     * @param groupId ID of the group
     * @param requestedBy ID of the user making the request
     * @return Result indicating success or error
     */
    suspend fun removeBankInfo(
        groupId: String,
        requestedBy: String
    ): Result<Unit> {
        if (groupId.isBlank() || requestedBy.isBlank()) {
            return Result.failure(IllegalArgumentException("Group ID and requester ID cannot be empty"))
        }
        
        // Check if requester has permission (Host only)
        val requesterRole = groupRepository.getUserRoleInGroup(requestedBy, groupId)
        if (requesterRole != ke.txasplit.vk.data.TXAMemberRole.HOST) {
            return Result.failure(IllegalArgumentException("Only Host can remove bank information"))
        }
        
        // Clear bank info
        return groupRepository.updateBankInfo(groupId, "", requestedBy)
    }
    
    /**
     * Check if bank information is configured for a group
     * @param groupId ID of the group
     * @return True if configured and valid, false otherwise
     */
    suspend fun isBankInfoConfigured(groupId: String): Boolean {
        return try {
            val group = groupRepository.getGroupById(groupId)
            group != null && group.hasBankInfo() && 
                   ke.txasplit.vk.core.TXAVietQRManager.isBankInfoConfigured(group.bankInfo)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get supported Vietnamese banks
     * @return Map of bank ID to bank name
     */
    fun getSupportedBanks(): Map<String, String> {
        return ke.txasplit.vk.core.TXAVietQRManager.getSupportedBanks()
    }
    
    /**
     * Generate default additional information for transfer
     * @param groupId ID of the group
     * @param payerName Name of the person paying
     * @return Formatted additional information
     */
    suspend fun generateDefaultAddInfo(groupId: String, payerName: String): Result<String> {
        if (groupId.isBlank()) {
            return Result.failure(IllegalArgumentException("Group ID cannot be empty"))
        }
        
        if (payerName.isBlank()) {
            return Result.failure(IllegalArgumentException("Payer name cannot be empty"))
        }
        
        return try {
            val group = groupRepository.getGroupById(groupId)
                ?: return Result.failure(IllegalArgumentException("Group not found"))
            
            val addInfo = ke.txasplit.vk.core.TXAVietQRManager.generateDefaultAddInfo(group.name, payerName)
            Result.success(addInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
