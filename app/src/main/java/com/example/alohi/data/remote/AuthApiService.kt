package com.example.alohi.data.remote

import com.example.alohi.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Header
import retrofit2.http.Path

/**
 * AloHi Auth API Service
 * Matches backend routes in auth.routes.js
 */
interface AuthApiService {

    /** Step 1: Send OTP to phone number */
    @POST("auth/send-otp")
    suspend fun sendOtp(
        @Body request: SendOtpRequest,
    ): Response<ApiResponse<SendOtpResponse>>

    /** Step 2: Verify OTP code */
    @POST("auth/verify-otp")
    suspend fun verifyOtp(
        @Body request: VerifyOtpRequest,
    ): Response<ApiResponse<Unit>>

    /** Step 3: Register new account */
    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest,
    ): Response<ApiResponse<RegisterResponse>>

    /** Login with phone + password */
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest,
    ): Response<ApiResponse<LoginResponse>>

    /** Refresh access token */
    @POST("auth/refresh-token")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest,
    ): Response<ApiResponse<RefreshTokenResponse>>

    /** Logout current device */
    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    @GET("auth/sessions")
    suspend fun getSessions(): Response<ApiResponse<List<DeviceSession>>>

    @POST("auth/sessions/{id}/logout")
    suspend fun logoutSession(@Path("id") id: String): Response<ApiResponse<Unit>>

    /** Get current user profile */
    @GET("users/me")
    suspend fun getMe(
        @Header("Authorization") token: String,
    ): Response<ApiResponse<UserProfile>>
}
