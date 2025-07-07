package com.dealerscloud.integration

import com.dealerscloud.models.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

interface DCBEVApiService {
    @POST("/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
    
    @POST("/auth/firebase-login")
    suspend fun loginWithFirebase(@Body request: FirebaseLoginRequest): LoginResponse
    
    @POST("/auth/refresh")
    suspend fun refreshToken(@Header("Authorization") token: String): RefreshTokenResponse
    
    @GET("/api/ai/health")
    suspend fun checkHealth(): HealthResponse
    
    @POST("/api/ai/chat")
    suspend fun chat(
        @Header("Authorization") token: String,
        @Body request: ChatRequest
    ): ChatResponse
    
    @GET("/api/ai/conversations/{conversationId}")
    suspend fun getConversation(
        @Header("Authorization") token: String,
        @Path("conversationId") conversationId: String
    ): ConversationResponse
    
    @GET("/api/ai/conversations")
    suspend fun listConversations(
        @Header("Authorization") token: String
    ): List<ConversationResponse>
}

data class HealthResponse(
    val status: String,
    val version: String,
    val timestamp: String
)

data class ChatResponse(
    val response: String,
    val conversation_id: String,
    val timestamp: String
)

data class ConversationResponse(
    val id: String,
    val title: String,
    val created_at: String,
    val updated_at: String,
    val messages: List<MessageResponse>
)

data class MessageResponse(
    val id: String,
    val content: String,
    val sender: String,
    val timestamp: String
)

@Singleton
class DCBEVApiClient @Inject constructor() {
    
    companion object {
        // Use 10.0.2.2 for Android emulator to reach host machine
        private const val EMULATOR_BACKEND_URL = "http://10.0.2.2:8000"
        private const val TAG = "DCBEVApiClient"
    }
    
    private var authToken: String? = null
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
            
            // Add authorization header if token is available
            authToken?.let {
                requestBuilder.header("Authorization", "Bearer $it")
            }
            
            chain.proceed(requestBuilder.build())
        }
        .build()
    
    private val gson = Gson()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(EMULATOR_BACKEND_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    
    private val apiService = retrofit.create(DCBEVApiService::class.java)
    
    /**
     * Update the auth token for authenticated requests
     */
    fun setAuthToken(token: String?) {
        authToken = token
    }
    
    /**
     * Login endpoint
     */
    suspend fun login(request: LoginRequest): LoginResponse {
        return apiService.login(request)
    }
    
    /**
     * Login with Firebase ID token
     */
    suspend fun loginWithFirebase(idToken: String, dealershipUrl: String): LoginResponse {
        val request = FirebaseLoginRequest(idToken, dealershipUrl)
        return apiService.loginWithFirebase(request)
    }
    
    /**
     * Refresh token endpoint
     */
    suspend fun refreshToken(): RefreshTokenResponse {
        val token = authToken ?: throw Exception("No auth token available")
        return apiService.refreshToken("Bearer $token")
    }
    
    /**
     * Stream chat completions using Server-Sent Events
     */
    fun streamChatCompletion(request: ChatRequest): Flow<String> = flow {
        val json = gson.toJson(request)
        val requestBody = json.toRequestBody("application/json".toMediaType())
        
        val httpRequest = Request.Builder()
            .url("$EMULATOR_BACKEND_URL/api/ai/chat/stream")
            .post(requestBody)
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .apply {
                // Add auth header if available
                authToken?.let {
                    header("Authorization", "Bearer $it")
                }
            }
            .build()
        
        try {
            val response = okHttpClient.newCall(httpRequest).execute()
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }
            
            val source = response.body?.source()
            source?.use { bufferedSource ->
                while (!bufferedSource.exhausted()) {
                    val line = bufferedSource.readUtf8Line() ?: break
                    
                    if (line.startsWith("data: ")) {
                        val data = line.substring(6)
                        if (data == "[DONE]") break
                        
                        try {
                            val chunk = gson.fromJson(data, Map::class.java)
                            val content = chunk["content"] as? String
                            if (!content.isNullOrEmpty()) {
                                emit(content)
                            }
                        } catch (e: Exception) {
                            // If JSON parsing fails, treat as plain text
                            if (data.isNotEmpty()) {
                                emit(data)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get conversation history
     */
    suspend fun getConversation(conversationId: String): ConversationResponse {
        val token = authToken ?: throw Exception("No auth token available")
        return apiService.getConversation("Bearer $token", conversationId)
    }
    
    /**
     * List all conversations
     */
    suspend fun listConversations(): List<ConversationResponse> {
        val token = authToken ?: throw Exception("No auth token available")
        return apiService.listConversations("Bearer $token")
    }
    
    /**
     * Test backend connection
     */
    suspend fun testConnection(): Boolean {
        return try {
            val response = apiService.checkHealth()
            response.status == "healthy"
        } catch (e: Exception) {
            false
        }
    }
}