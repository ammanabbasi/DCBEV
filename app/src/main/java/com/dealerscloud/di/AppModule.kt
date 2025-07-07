package com.dealerscloud.di

import android.content.Context
import androidx.room.Room
import com.dealerscloud.admin.AccountDatabase
import com.dealerscloud.admin.AccountDAO
import com.dealerscloud.admin.AccountRepository
import com.dealerscloud.admin.AccountRepositoryImpl
import com.dealerscloud.integration.DealersCloudApiClient
import com.dealerscloud.integration.DealersCloudAuthManager
import com.dealerscloud.integration.FirebaseAuthManager
import com.dealerscloud.integration.DCBEVApiClient
import com.dealerscloud.integration.JWTAuthManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.dealerscloud.ui.repository.ChatRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAccountDatabase(
        @ApplicationContext context: Context
    ): AccountDatabase {
        return Room.databaseBuilder(
            context,
            AccountDatabase::class.java,
            "dcbev_accounts.db"
        ).build()
    }

    @Provides
    fun provideAccountDAO(database: AccountDatabase): AccountDAO {
        return database.accountDao()
    }

    @Provides
    @Singleton
    fun provideAccountRepository(
        @ApplicationContext context: Context
    ): AccountRepository {
        return AccountRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideDealersCloudAuthManager(
        @ApplicationContext context: Context,
        accountRepository: AccountRepository
    ): DealersCloudAuthManager {
        return DealersCloudAuthManager(context, accountRepository)
    }

    @Provides
    @Singleton
    fun provideDealerSubdomainInterceptor(
        authManager: DealersCloudAuthManager
    ): com.dealerscloud.integration.DealerSubdomainInterceptor {
        return com.dealerscloud.integration.DealerSubdomainInterceptor(authManager)
    }

    @Provides
    @Singleton
    fun provideDealersCloudApiClient(
        authManager: DealersCloudAuthManager,
        subdomainInterceptor: com.dealerscloud.integration.DealerSubdomainInterceptor
    ): DealersCloudApiClient {
        return DealersCloudApiClient(authManager, subdomainInterceptor)
    }

    // Firebase dependencies
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging {
        return FirebaseMessaging.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }

    // Unified Firebase Auth Manager
    @Provides
    @Singleton
    fun provideFirebaseAuthManager(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore,
        dealersCloudAuthManager: DealersCloudAuthManager,
        accountRepository: AccountRepository
    ): FirebaseAuthManager {
        return FirebaseAuthManager(
            firebaseAuth,
            firestore,
            dealersCloudAuthManager,
            accountRepository
        )
    }
    
    // DCBEV Chat Dependencies
    @Provides
    @Singleton
    fun provideDCBEVApiClient(): DCBEVApiClient {
        return DCBEVApiClient()
    }
    
    @Provides
    @Singleton
    fun provideJWTAuthManager(
        @ApplicationContext context: Context,
        apiClient: DCBEVApiClient
    ): JWTAuthManager {
        val authManager = JWTAuthManager(context, apiClient)
        
        // Set up bidirectional relationship
        apiClient.setAuthToken(authManager.getAccessToken())
        
        return authManager
    }
    
    @Provides
    @Singleton
    fun provideChatRepository(
        apiClient: DCBEVApiClient
    ): ChatRepository {
        return ChatRepository(apiClient)
    }
}