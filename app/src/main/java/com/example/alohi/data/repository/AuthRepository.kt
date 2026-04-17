package com.example.alohi.data.repository

import android.os.Build
import android.util.Log
import com.example.alohi.data.local.TokenManager
import com.example.alohi.data.model.*
import com.example.alohi.data.remote.ApiClient
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import java.util.UUID

/**
 * AloHi Auth Repository
 * Handles all authentication operations and token persistence.
 *
 * Auth Flow:
 * 1. sendOtp(phone) → OTP sent to phone (dev: logged in server console)
 * 2. verifyOtp(phone, code) → OTP verified
 * 3. register(phone, password, name) → Account created
 * 4. login(phone, password) → Tokens + user profile returned
 */
class AuthRepository(
    private val tokenManager: TokenManager,
) {
    private val api = ApiClient.authApi
    private val gson = Gson()

    companion object {
        private const val TAG = "AuthRepository"
    }

    // ═══════════════════════════════════════════════════════
    // CHECK PHONE (Zalo Flow)
    // ═══════════════════════════════════════════════════════
    suspend fun checkPhone(phone: String): Result<UserProfile> {
        return try {
            val formattedPhone = formatPhone(phone)
            val response = api.checkPhone(SendOtpRequest(formattedPhone))

            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                if (data != null) {
                    Result.success(data)
                } else {
                    Result.failure(Exception("Không có dữ liệu user"))
                }
            } else {
                val errorMsg = parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkPhone error", e)
            Result.failure(Exception("Lỗi kết nối: ${e.localizedMessage}"))
        }
    }

    // ═══════════════════════════════════════════════════════
    // SEND OTP
    // ═══════════════════════════════════════════════════════
    suspend fun sendOtp(phone: String): Result<SendOtpResponse> {
        return try {
            val formattedPhone = formatPhone(phone)
            val response = api.sendOtp(SendOtpRequest(formattedPhone))

            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                Result.success(data ?: SendOtpResponse("OTP đã gửi", 300))
            } else {
                val errorMsg = parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendOtp error", e)
            Result.failure(Exception("Lỗi kết nối: ${e.localizedMessage}"))
        }
    }

    // ═══════════════════════════════════════════════════════
    // VERIFY OTP
    // ═══════════════════════════════════════════════════════
    suspend fun verifyOtp(phone: String, code: String): Result<Unit> {
        return try {
            val formattedPhone = formatPhone(phone)
            val response = api.verifyOtp(VerifyOtpRequest(formattedPhone, code))

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                val errorMsg = parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "verifyOtp error", e)
            Result.failure(Exception("Lỗi kết nối: ${e.localizedMessage}"))
        }
    }

    // ═══════════════════════════════════════════════════════
    // FORGOT / RESET PASSWORD
    // ═══════════════════════════════════════════════════════
    suspend fun forgotPassword(phone: String): Result<Unit> {
        return try {
            val formattedPhone = formatPhone(phone)
            val response = api.forgotPassword(SendOtpRequest(formattedPhone))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                val errorMsg = parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi kết nối: ${e.localizedMessage}"))
        }
    }

    suspend fun resetPassword(phone: String, otpCode: String, newPassword: String): Result<Unit> {
        return try {
            val formattedPhone = formatPhone(phone)
            val response = api.resetPassword(ResetPasswordRequest(formattedPhone, otpCode, newPassword))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                val errorMsg = parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi kết nối: ${e.localizedMessage}"))
        }
    }

    // ═══════════════════════════════════════════════════════
    // REGISTER
    // ═══════════════════════════════════════════════════════
    suspend fun register(
        phone: String,
        password: String,
        displayName: String,
        gender: String = "other",
    ): Result<String> {
        return try {
            val formattedPhone = formatPhone(phone)
            val request = RegisterRequest(
                phone = formattedPhone,
                password = password,
                displayName = displayName,
                gender = gender,
            )
            val response = api.register(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val userId = response.body()?.data?.userId ?: ""
                Result.success(userId)
            } else {
                val errorMsg = parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "register error", e)
            Result.failure(Exception("Lỗi kết nối: ${e.localizedMessage}"))
        }
    }

    // ═══════════════════════════════════════════════════════
    // LOGIN
    // ═══════════════════════════════════════════════════════
    suspend fun login(phone: String, password: String): Result<LoginResponse> {
        return try {
            val formattedPhone = formatPhone(phone)
            val deviceId = getOrCreateDeviceId()

            val request = LoginRequest(
                phone = formattedPhone,
                password = password,
                deviceId = deviceId,
                deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
                deviceModel = Build.MODEL,
                platform = "android",
                osVersion = "Android ${Build.VERSION.RELEASE}",
                appVersion = "1.0.0",
            )

            val response = api.login(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val loginData = response.body()?.data!!

                // Persist tokens
                tokenManager.saveTokens(loginData.accessToken, loginData.refreshToken)

                // Persist user info
                tokenManager.saveUserInfo(
                    userId = loginData.user.id,
                    displayName = loginData.user.displayName,
                    phone = loginData.user.phone ?: formattedPhone,
                )

                Log.i(TAG, "Login success: ${loginData.user.displayName}")
                Result.success(loginData)
            } else {
                val errorMsg = parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "login error", e)
            Result.failure(Exception("Lỗi kết nối: ${e.localizedMessage}"))
        }
    }

    // ═══════════════════════════════════════════════════════
    // REFRESH TOKEN
    // ═══════════════════════════════════════════════════════
    suspend fun refreshToken(): Result<Unit> {
        return try {
            val currentRefresh = tokenManager.refreshToken.first()
                ?: return Result.failure(Exception("No refresh token"))

            val response = api.refreshToken(RefreshTokenRequest(currentRefresh))

            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data!!
                tokenManager.saveTokens(data.accessToken, data.refreshToken)
                Result.success(Unit)
            } else {
                // Token invalid → force logout
                tokenManager.clearAll()
                Result.failure(Exception("Phiên đăng nhập hết hạn"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "refreshToken error", e)
            Result.failure(Exception("Không thể làm mới token"))
        }
    }

    // ═══════════════════════════════════════════════════════
    // LOGOUT
    // ═══════════════════════════════════════════════════════
    suspend fun logout(): Result<Unit> {
        return try {
            val token = tokenManager.accessToken.first()
            if (token != null) {
                api.logout()
            }
            tokenManager.clearAll()
            Result.success(Unit)
        } catch (e: Exception) {
            // Always clear local data even if API fails
            tokenManager.clearAll()
            Result.success(Unit)
        }
    }

    // ═══════════════════════════════════════════════════════
    // CHECK LOGIN STATUS
    // ═══════════════════════════════════════════════════════
    suspend fun isLoggedIn(): Boolean {
        return tokenManager.isLoggedIn.first()
    }

    // ═══════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════

    /**
     * Format phone: "0912345678" → "+84912345678"
     */
    private fun formatPhone(phone: String): String {
        val cleaned = phone.trim().replace(" ", "")
        return if (cleaned.startsWith("0")) {
            "+84${cleaned.substring(1)}"
        } else if (cleaned.startsWith("+84")) {
            cleaned
        } else {
            "+84$cleaned"
        }
    }

    /**
     * Get or create persistent device ID
     */
    private suspend fun getOrCreateDeviceId(): String {
        val existing = tokenManager.deviceId.first()
        if (!existing.isNullOrEmpty()) return existing

        val newId = UUID.randomUUID().toString()
        tokenManager.saveDeviceId(newId)
        return newId
    }

    /**
     * Parse error message from API error body
     */
    private fun parseError(errorBody: String?): String {
        if (errorBody == null) return "Có lỗi xảy ra"
        return try {
            val error = gson.fromJson(errorBody, ApiError::class.java)
            error.message
        } catch (e: Exception) {
            "Có lỗi xảy ra"
        }
    }
}
