/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXADatabase.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ke.txasplit.vk.data.converter.TXADatabaseConverters
import ke.txasplit.vk.data.dao.TXABillDao
import ke.txasplit.vk.data.dao.TXAGroupDao
import ke.txasplit.vk.data.dao.TXAMemberDao
import ke.txasplit.vk.data.dao.TXAPaymentDao
import ke.txasplit.vk.data.entity.TXABillEntity
import ke.txasplit.vk.data.entity.TXAGroupEntity
import ke.txasplit.vk.data.entity.TXAMemberEntity
import ke.txasplit.vk.data.entity.TXAPaymentEntity

/**
 * Room database for TXASplit application
 * Contains all entities, DAOs, and database configuration
 */
@Database(
    entities = [
        TXAGroupEntity::class,
        TXAMemberEntity::class,
        TXABillEntity::class,
        TXAPaymentEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(TXADatabaseConverters::class)
abstract class TXADatabase : RoomDatabase() {
    
    /**
     * DAO for group operations
     */
    abstract fun groupDao(): TXAGroupDao
    
    /**
     * DAO for member operations
     */
    abstract fun memberDao(): TXAMemberDao
    
    /**
     * DAO for bill operations
     */
    abstract fun billDao(): TXABillDao
    
    /**
     * DAO for payment operations
     */
    abstract fun paymentDao(): TXAPaymentDao
    
    companion object {
        /**
         * Database name
         */
        const val DATABASE_NAME = "txasplit_database"
    }
}

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
