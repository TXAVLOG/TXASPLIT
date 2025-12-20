/*
████████ ██   ██  █████   █████  ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██   ██ ██   ██
   ██      ███   ███████ ███████ ██████  ██████ 
   ██     ██ ██  ██   ██ ██   ██ ██      ██     
   ██    ██   ██ ██   ██ ██   ██ ██      ██     

TXASplit - TXADatabaseModule.kt
Build by TXA
Contact: FB: https://fb.com/vlog.txa.2311, GMAIL: txavlog7@gmail.com!
*/

package ke.txasplit.vk.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ke.txasplit.vk.data.TXADatabase
import ke.txasplit.vk.data.dao.TXAGroupDao
import ke.txasplit.vk.data.dao.TXAMemberDao
import javax.inject.Singleton

/**
 * Hilt module for providing database dependencies
 * Configures Room database and DAOs for dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
object TXADatabaseModule {
    
    /**
     * Provide the Room database instance
     * @param context Application context
     * @return Singleton instance of TXADatabase
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TXADatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            TXADatabase::class.java,
            TXADatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration() // For development - consider proper migrations for production
        .build()
    }
    
    /**
     * Provide the Group DAO
     * @param database Room database instance
     * @return TXAGroupDao instance
     */
    @Provides
    fun provideGroupDao(database: TXADatabase): TXAGroupDao {
        return database.groupDao()
    }
    
    /**
     * Provide the Member DAO
     * @param database Room database instance
     * @return TXAMemberDao instance
     */
    @Provides
    fun provideMemberDao(database: TXADatabase): TXAMemberDao {
        return database.memberDao()
    }
    
    /**
     * Provide the Bill DAO
     * @param database Room database instance
     * @return TXABillDao instance
     */
    @Provides
    fun provideBillDao(database: TXADatabase): ke.txasplit.vk.data.dao.TXABillDao {
        return database.billDao()
    }
    
    /**
     * Provide the Payment DAO
     * @param database Room database instance
     * @return TXAPaymentDao instance
     */
    @Provides
    fun providePaymentDao(database: TXADatabase): ke.txasplit.vk.data.dao.TXAPaymentDao {
        return database.paymentDao()
    }
}
