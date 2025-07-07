package com.dealerscloud.integration

import android.util.Log
import com.dealerscloud.admin.AccountRepository
import com.dealerscloud.admin.DCBEVUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthManager @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val dealersCloudAuthManager: DealersCloudAuthManager,
    private val accountRepository: AccountRepository
) {
    companion object {
        private const val TAG = "FirebaseAuthManager"
    }

    /**
     * Enhanced authentication flow:
     * 1. Sign in with Firebase (email/password)
     * 2. Get Firebase ID token
     * 3. Use ID token to authenticate with DealersCloud
     * 4. Store user session in Firestore for cross-platform sync
     */
    suspend fun authenticateWithDCBEV(
        email: String,
        password: String,
        dealershipUrl: String
    ): Result<DealersCloudUser> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting Firebase authentication for $email")

            // Step 1: Firebase Authentication
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Firebase authentication failed")
            
            Log.i(TAG, "Firebase auth successful: ${firebaseUser.uid}")

            // Step 2: Get Firebase ID token for DealersCloud API
            val idToken = firebaseUser.getIdToken(false).await().token
                ?: throw Exception("Failed to get Firebase ID token")

            // Step 3: Authenticate with DealersCloud using the existing flow
            val dcbevResult = dealersCloudAuthManager.authenticateWithDCBEV(email, dealershipUrl)
            if (dcbevResult.isFailure) {
                throw dcbevResult.exceptionOrNull() ?: Exception("DealersCloud authentication failed")
            }

            val dealersCloudUser = dcbevResult.getOrThrow()

            // Step 4: Store session in Firestore for cross-platform access
            saveUserSessionToFirestore(firebaseUser, dealersCloudUser, idToken)

            Log.i(TAG, "Complete authentication successful")
            Result.success(dealersCloudUser)

        } catch (e: Exception) {
            Log.e(TAG, "Authentication failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Register new DCBEV user with Firebase Auth
     */
    suspend fun registerDCBEVUser(
        email: String,
        password: String,
        username: String,
        dealershipUrl: String,
        dealershipName: String
    ): Result<DCBEVUser> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Registering new DCBEV user: $email")

            // Step 1: Create Firebase user
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Firebase user creation failed")

            // Step 2: Register with DCBEV system
            val dcbevResult = dealersCloudAuthManager.registerDCBEVUser(
                username, email, dealershipUrl, dealershipName
            )
            
            if (dcbevResult.isFailure) {
                // Rollback Firebase user if DCBEV registration fails
                firebaseUser.delete().await()
                throw dcbevResult.exceptionOrNull() ?: Exception("DCBEV registration failed")
            }

            val dcbevUser = dcbevResult.getOrThrow()

            // Step 3: Store user profile in Firestore
            saveUserProfileToFirestore(firebaseUser, dcbevUser)

            Log.i(TAG, "User registration successful")
            Result.success(dcbevUser)

        } catch (e: Exception) {
            Log.e(TAG, "Registration failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Check if user is authenticated with both Firebase and DealersCloud
     */
    fun isAuthenticated(): Boolean {
        val firebaseAuthenticated = firebaseAuth.currentUser != null
        val dcbevAuthenticated = dealersCloudAuthManager.isAuthenticated()
        return firebaseAuthenticated && dcbevAuthenticated
    }

    /**
     * Get current Firebase user
     */
    fun getCurrentFirebaseUser(): FirebaseUser? = firebaseAuth.currentUser

    /**
     * Get current DCBEV user (from existing auth manager)
     */
    fun getCurrentDCBEVUser(): DealersCloudUser? = dealersCloudAuthManager.getCurrentUser()

    /**
     * Sign out from both Firebase and DCBEV
     */
    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Sign out from Firebase
            firebaseAuth.signOut()
            
            // Sign out from DCBEV
            dealersCloudAuthManager.logout()
            
            Log.i(TAG, "Complete sign out successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Store user session in Firestore for cross-platform sync
     */
    private suspend fun saveUserSessionToFirestore(
        firebaseUser: FirebaseUser,
        dealersCloudUser: DealersCloudUser,
        idToken: String
    ) {
        try {
            val sessionData = mapOf(
                "uid" to firebaseUser.uid,
                "email" to firebaseUser.email,
                "dealershipId" to dealersCloudUser.dealershipId,
                "dealershipUrl" to dealersCloudUser.dealerUrl,
                "dealershipName" to dealersCloudUser.dealerInfo.name,
                "role" to dealersCloudUser.role,
                "accessToken" to dealersCloudUser.accessToken,
                "firebaseToken" to idToken,
                "lastLoginAt" to System.currentTimeMillis(),
                "isActive" to true
            )

            firestore.collection("user_sessions")
                .document(firebaseUser.uid)
                .set(sessionData)
                .await()

            Log.i(TAG, "User session saved to Firestore")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save session to Firestore: ${e.message}", e)
        }
    }

    /**
     * Store user profile in Firestore
     */
    private suspend fun saveUserProfileToFirestore(
        firebaseUser: FirebaseUser,
        dcbevUser: DCBEVUser
    ) {
        try {
            val userProfile = mapOf(
                "uid" to firebaseUser.uid,
                "username" to dcbevUser.username,
                "email" to dcbevUser.email,
                "dealershipUrl" to dcbevUser.dealershipUrl,
                "dealershipName" to dcbevUser.dealershipName,
                "isActive" to dcbevUser.isActive,
                "createdAt" to dcbevUser.createdAt,
                "lastLoginAt" to dcbevUser.lastLoginAt
            )

            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(userProfile)
                .await()

            Log.i(TAG, "User profile saved to Firestore")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save profile to Firestore: ${e.message}", e)
        }
    }
}