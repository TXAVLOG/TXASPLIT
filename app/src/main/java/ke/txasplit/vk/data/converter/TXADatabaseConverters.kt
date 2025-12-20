package ke.txasplit.vk.data.converter

import ke.txasplit.vk.data.TXAMemberRole

/**
 * Type converters for Room database
 * Handles serialization of complex types
 */
class TXADatabaseConverters {
    
    /**
     * Convert TXAMemberRole enum to String
     */
    @androidx.room.TypeConverter
    fun fromMemberRole(role: TXAMemberRole): String {
        return role.name
    }
    
    /**
     * Convert String to TXAMemberRole enum
     */
    @androidx.room.TypeConverter
    fun toMemberRole(role: String): TXAMemberRole {
        return TXAMemberRole.valueOf(role)
    }
}
