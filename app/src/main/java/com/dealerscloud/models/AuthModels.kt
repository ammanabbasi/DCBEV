package com.dealerscloud.models

import com.google.gson.annotations.SerializedName

// JWT Authentication Models
data class LoginRequest(
    val email: String,
    val password: String,
    @SerializedName("dealership_url") val dealershipUrl: String? = null
)

data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    val user: UserData
)

data class UserData(
    val sub: String,
    val email: String,
    @SerializedName("dealership_url") val dealershipUrl: String,
    val role: String
)

data class RefreshTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String
)

data class FirebaseLoginRequest(
    @SerializedName("id_token") val idToken: String,
    @SerializedName("dealership_url") val dealershipUrl: String? = null
)

// Admin credentials for DCBEV
object AdminCredentials {
    const val ADMIN_USERNAME = "dc_amman"
    const val ADMIN_PASSWORD = "Amman1050"
    const val ADMIN_EMAIL = "admin@dealerscloud.com"
}

// Token storage preferences keys
object AuthPreferences {
    const val PREFS_NAME = "dcbev_auth_prefs"
    const val KEY_ACCESS_TOKEN = "access_token"
    const val KEY_USER_EMAIL = "user_email"
    const val KEY_USER_ROLE = "user_role"
    const val KEY_DEALERSHIP_URL = "dealership_url"
    const val KEY_USER_ID = "user_id"
}