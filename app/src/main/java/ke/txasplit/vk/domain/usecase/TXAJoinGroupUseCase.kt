/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAJoinGroupUseCase.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.domain.usecase

import ke.txasplit.vk.data.repository.TXAGroupRepository
import ke.txasplit.vk.data.entity.TXAGroupEntity
import javax.inject.Inject

/**
 * Use case for joining a group using invite code
 * Handles invite code validation and group joining logic
 */
class TXAJoinGroupUseCase @Inject constructor(
    private val groupRepository: TXAGroupRepository
) {
    
    /**
     * Join a group using invite code
     * @param inviteCode The TXAS-XXXX invite code
     * @param userId ID of the user joining
     * @param displayName Display name for the user in the group
     * @return Result containing the joined group or error
     */
    suspend operator fun invoke(
        inviteCode: String,
        userId: String,
        displayName: String
    ): Result<TXAGroupEntity> {
        // Validate invite code format
        if (!isValidInviteCode(inviteCode)) {
            return Result.failure(IllegalArgumentException("Invalid invite code format"))
        }
        
        // Validate user input
        if (userId.isBlank()) {
            return Result.failure(IllegalArgumentException("User ID cannot be empty"))
        }
        
        if (displayName.isBlank()) {
            return Result.failure(IllegalArgumentException("Display name cannot be empty"))
        }
        
        if (displayName.length > 50) {
            return Result.failure(IllegalArgumentException("Display name too long (max 50 characters)"))
        }
        
        // Delegate to repository
        return groupRepository.joinGroupByInviteCode(
            inviteCode.trim().uppercase(),
            userId.trim(),
            displayName.trim()
        )
    }
    
    /**
     * Validate invite code format (TXAS-XXXX)
     */
    private fun isValidInviteCode(inviteCode: String): Boolean {
        val trimmed = inviteCode.trim().uppercase()
        return trimmed.matches(Regex("^TXAS-[A-Z0-9]{4}$"))
    }
}
