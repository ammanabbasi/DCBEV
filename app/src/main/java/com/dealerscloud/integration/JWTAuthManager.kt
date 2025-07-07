package com.dealerscloud.integration

import android.content.Context
import android.content.SharedPreferences
import com.dealerscloud.models.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JWTAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiClient: DCBEVApiClient
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        AuthPreferences.PREFS_NAME, 
        Context.MODE_PRIVATE
    )
    
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    data class AuthState(
        val isAuthenticated: Boolean = false,
        val user: UserData? = null,
        val accessToken: String? = null
    )
    
    init {
        // Check for existing token on initialization
        val token = prefs.getString(AuthPreferences.KEY_ACCESS_TOKEN, null)
        if (!token.isNullOrEmpty()) {
            val userData = UserData(
                sub = prefs.getString(AuthPreferences.KEY_USER_ID, "") ?: "",
                email = prefs.getString(AuthPreferences.KEY_USER_EMAIL, "") ?: "",
                dealershipUrl = prefs.getString(AuthPreferences.KEY_DEALERSHIP_URL, "") ?: "",
                role = prefs.getString(AuthPreferences.KEY_USER_ROLE, "user") ?: "user"
            )
            _authState.value = AuthState(
                isAuthenticated = true,
                user = userData,
                accessToken = token
            )
        }
    }
    
    suspend fun login(email: String, password: String, dealershipUrl: String? = null): Result<LoginResponse> {
        return try {
            val request = LoginRequest(email, password, dealershipUrl)
            val response = apiClient.login(request)
            
            // Save authentication data
            saveAuthData(response)
            
            // Update auth state
            _authState.value = AuthState(
                isAuthenticated = true,
                user = response.user,
                accessToken = response.accessToken
            )
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun loginAsAdmin(): Result<LoginResponse> {
        return login(
            AdminCredentials.ADMIN_USERNAME,
            AdminCredentials.ADMIN_PASSWORD,
            "https://yahauto.autodealerscloud.com"
        )
    }
    
    suspend fun loginWithFirebase(idToken: String, dealershipUrl: String): Result<LoginResponse> {
        return try {
            // Use a special Firebase login endpoint
            val response = apiClient.loginWithFirebase(idToken, dealershipUrl)
            
            // Save authentication data
            saveAuthData(response)
            
            // Update auth state
            _authState.value = AuthState(
                isAuthenticated = true,
                user = response.user,
                accessToken = response.accessToken
            )
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun refreshToken(): Result<RefreshTokenResponse> {
        return try {
            val response = apiClient.refreshToken()
            
            // Update stored token
            prefs.edit()
                .putString(AuthPreferences.KEY_ACCESS_TOKEN, response.accessToken)
                .apply()
            
            // Update auth state
            _authState.value = _authState.value.copy(
                accessToken = response.accessToken
            )
            
            Result.success(response)
        } catch (e: Exception) {
            // If refresh fails, log out
            logout()
            Result.failure(e)
        }
    }
    
    fun logout() {
        // Clear stored data
        prefs.edit().clear().apply()
        
        // Reset auth state
        _authState.value = AuthState()
    }
    
    fun getAccessToken(): String? {
        return _authState.value.accessToken
    }
    
    fun isAuthenticated(): Boolean {
        return _authState.value.isAuthenticated
    }
    
    fun getCurrentUser(): UserData? {
        return _authState.value.user
    }
    
    private fun saveAuthData(response: LoginResponse) {
        prefs.edit()
            .putString(AuthPreferences.KEY_ACCESS_TOKEN, response.accessToken)
            .putString(AuthPreferences.KEY_USER_ID, response.user.sub)
            .putString(AuthPreferences.KEY_USER_EMAIL, response.user.email)
            .putString(AuthPreferences.KEY_USER_ROLE, response.user.role)
            .putString(AuthPreferences.KEY_DEALERSHIP_URL, response.user.dealershipUrl)
            .apply()
    }
}