/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXAMemberRole.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.data

/**
 * Enum representing member roles in TXASplit groups
 * with transparency and oversight requirements
 */
enum class TXAMemberRole {
    /**
     * Host (1 per group): Full control
     * - Can delete group
     * - Can change roles
     * - Can add/remove members
     * - Can create/edit/delete bills
     */
    HOST,
    
    /**
     * Co-host (Max 3 per group): Limited control
     * - Cannot delete group
     * - Cannot change Host role
     * - Can add/edit bills
     * - Can remove Members (not Host/Co-hosts)
     */
    CO_HOST,
    
    /**
     * Member: View and pay only
     * - Can view all bills and balances
     * - Can mark payments as made
     * - Cannot create/edit bills
     * - Cannot remove other members
     * Provides oversight of Host/Co-host actions
     */
    MEMBER
}

/**
 * Validation rules for member roles
 */
object TXAMemberRoleValidator {
    
    /**
     * Maximum number of co-hosts allowed per group
     */
    const val MAX_CO_HOSTS = 3
    
    /**
     * Minimum number of members required when total members >= 2
     * (for transparency/oversight)
     */
    const val MIN_MEMBERS_FOR_OVERSIGHT = 1
    
    /**
     * Validate role change constraints
     */
    fun validateRoleChange(
        currentRole: TXAMemberRole,
        newRole: TXAMemberRole,
        groupStats: GroupRoleStats
    ): ValidationResult {
        return when {
            // Cannot change from Host to anything else without transferring
            currentRole == TXAMemberRole.HOST && newRole != TXAMemberRole.HOST -> {
                ValidationResult.Error("Host must transfer leadership before changing role")
            }
            
            // Check co-host limit
            newRole == TXAMemberRole.CO_HOST && 
            currentRole != TXAMemberRole.CO_HOST &&
            groupStats.coHostCount >= MAX_CO_HOSTS -> {
                ValidationResult.Error("Maximum $MAX_CO_HOSTS co-hosts allowed")
            }
            
            // Check member oversight requirement
            currentRole == TXAMemberRole.MEMBER && 
            newRole != TXAMemberRole.MEMBER &&
            groupStats.totalMembers >= 2 &&
            groupStats.memberCount <= MIN_MEMBERS_FOR_OVERSIGHT -> {
                ValidationResult.Error("At least $MIN_MEMBERS_FOR_OVERSIGHT member required for group oversight")
            }
            
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Validate member removal constraints
     */
    fun validateMemberRemoval(
        memberRole: TXAMemberRole,
        groupStats: GroupRoleStats
    ): ValidationResult {
        return when {
            // Cannot remove Host
            memberRole == TXAMemberRole.HOST -> {
                ValidationResult.Error("Cannot remove group host")
            }
            
            // Check member oversight requirement
            memberRole == TXAMemberRole.MEMBER &&
            groupStats.totalMembers >= 2 &&
            groupStats.memberCount <= MIN_MEMBERS_FOR_OVERSIGHT -> {
                ValidationResult.Error("Cannot remove last member - at least $MIN_MEMBERS_FOR_OVERSIGHT member required for oversight")
            }
            
            else -> ValidationResult.Success
        }
    }
}

/**
 * Statistics about roles in a group for validation
 */
data class GroupRoleStats(
    val totalMembers: Int,
    val hostCount: Int,
    val coHostCount: Int,
    val memberCount: Int
)

/**
 * Validation result
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
