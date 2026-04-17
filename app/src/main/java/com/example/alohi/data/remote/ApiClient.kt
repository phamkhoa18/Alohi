package com.example.alohi.data.remote

import android.util.Log
import com.example.alohi.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
/**
 * Configuration:
 * - Base URL: http://10.0.2.2:3000/api/ (Android emulator → localhost)
 * - OkHttp: logging, auth interceptor, timeouts
 * - Gson converter for JSON parsing
 */
object ApiClient {

    private const val TAG = "ApiClient"

    // 10.0.2.2 = Android emulator's localhost
    // For physical device, use your PC's local IP (e.g., 192.168.1.x)
    const val BASE_URL = "http://172.16.1.76:3000/api/"

    private var tokenManager: TokenManager? = null

    fun init(tokenManager: TokenManager) {
        this.tokenManager = tokenManager
    }

    /**
     * Auth interceptor — adds Bearer token to requests
     */
    private val authInterceptor = Interceptor { chain ->
        val request = chain.request()

        // Skip auth for public endpoints
        val publicPaths = listOf("auth/send-otp", "auth/verify-otp", "auth/register", "auth/login", "auth/refresh-token")
        val path = request.url.encodedPath
        val isPublic = publicPaths.any { path.contains(it) }

        if (isPublic || request.header("Authorization") != null) {
            chain.proceed(request)
        } else {
            val token = tokenManager?.getAccessTokenSync()
            if (token != null) {
                val newRequest = request.newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                chain.proceed(newRequest)
            } else {
                chain.proceed(request)
            }
        }
    }

    /**
     * Authenticator — auto-refreshes token on 401 Unauthorized
     */
    private val tokenAuthenticator = okhttp3.Authenticator { _, response ->
        if (response.request.header("Authorization") == null) {
            return@Authenticator null // Give up, not auth request
        }

        synchronized(this) {
            val currentToken = tokenManager?.getAccessTokenSync()
            // If token has refreshed by another thread
            if (currentToken != null && response.request.header("Authorization") != "Bearer $currentToken") {
                return@Authenticator response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val refreshToken = tokenManager?.getRefreshTokenSync()

            if (refreshToken != null) {
                try {
                    // MUST use a separate naked OkHttpClient to avoid deadlocking the thread!
                    val isolatedClient = okhttp3.OkHttpClient()
                    val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                    val body = "{\"refreshToken\":\"$refreshToken\"}".toRequestBody(mediaType)
                    
                    val request = okhttp3.Request.Builder()
                        .url("${BASE_URL}auth/refresh-token")
                        .post(body)
                        .build()

                    val refreshResponse = isolatedClient.newCall(request).execute()

                    if (refreshResponse.isSuccessful) {
                        val responseBody = refreshResponse.body?.string()
                        if (responseBody != null) {
                            // Extract JSON manually to avoid Gson deadlock or just use simple regex/JSONObject
                            val jsonObject = org.json.JSONObject(responseBody)
                            if (jsonObject.has("data")) {
                                val dataObj = jsonObject.getJSONObject("data")
                                val newAccessToken = dataObj.getString("accessToken")
                                val newRefreshToken = dataObj.getString("refreshToken")
                                
                                kotlinx.coroutines.runBlocking {
                                    tokenManager?.saveTokens(newAccessToken, newRefreshToken)
                                }
                                
                                // Tell socket to reconnect using the fresh token
                                com.example.alohi.data.remote.SocketManager.reconnect()
                                return@Authenticator response.request.newBuilder()
                                    .header("Authorization", "Bearer $newAccessToken")
                                    .build()
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Failed to refresh token", e)
                }
            }

            // Refresh failed or no refresh token → Force logout
            kotlinx.coroutines.runBlocking {
                tokenManager?.clearAll() // Triggers isLoggedIn Flow to false
            }

            null
        }
    }

    /**
     * Logging interceptor for debugging
     */
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d(TAG, message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /** Auto-retry for flaky networks (like physical device waking up) */
    private val retryInterceptor = Interceptor { chain ->
        var tryCount = 0
        while (tryCount < 3) {
            try {
                return@Interceptor chain.proceed(chain.request())
            } catch (e: Exception) {
                tryCount++
                if (tryCount >= 3) throw e
                try {
                    Thread.sleep(500)
                } catch (ie: InterruptedException) {
                    throw e
                }
            }
        }
        throw java.io.IOException("Failed after 3 retries")
    }

    /**
     * OkHttp client
     */
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(retryInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(tokenAuthenticator)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Lenient Gson — handles backend type mismatches gracefully.
     * e.g. lastMessage might be a String when model expects Object.
     */
    private val gson: com.google.gson.Gson by lazy {
        com.google.gson.GsonBuilder()
            .registerTypeAdapter(
                com.example.alohi.data.model.LastMessage::class.java,
                com.google.gson.JsonDeserializer<com.example.alohi.data.model.LastMessage?> { json, _, context ->
                    if (json.isJsonObject) {
                        context.deserialize(json, com.example.alohi.data.model.LastMessage::class.java)
                    } else {
                        null // Backend returned a string/null instead of object — skip
                    }
                }
            )
            .create()
    }

    /**
     * Retrofit instance
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /**
     * Auth API service
     */
    val authApi: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }

    /**
     * User/Conversations/Friends API service
     */
    val userApi: UserApiService by lazy {
        retrofit.create(UserApiService::class.java)
    }
}
