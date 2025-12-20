/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXACreateGroupUseCase.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.domain.usecase

import ke.txasplit.vk.data.repository.TXAGroupRepository
import ke.txasplit.vk.data.entity.TXAGroupEntity
import javax.inject.Inject

/**
 * Use case for creating a new group
 * Encapsulates business logic for group creation
 */
class TXACreateGroupUseCase @Inject constructor(
    private val groupRepository: TXAGroupRepository
) {
    
    /**
     * Create a new group with the specified parameters
     * @param userId ID of the user who will become the host
     * @param name Group name (must not be empty)
     * @param description Optional group description
     * @return Result containing the created group or error
     */
    suspend operator fun invoke(
        userId: String,
        name: String,
        description: String = ""
    ): Result<TXAGroupEntity> {
        // Validate input
        if (userId.isBlank()) {
            return Result.failure(IllegalArgumentException("User ID cannot be empty"))
        }
        
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Group name cannot be empty"))
        }
        
        if (name.length > 100) {
            return Result.failure(IllegalArgumentException("Group name too long (max 100 characters)"))
        }
        
        if (description.length > 500) {
            return Result.failure(IllegalArgumentException("Description too long (max 500 characters)"))
        }
        
        // Delegate to repository
        return groupRepository.createGroup(userId, name.trim(), description.trim())
    }
}
