package com.example.alohi.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alohi.data.local.AloHiDatabase
import com.example.alohi.data.local.TokenManager
import com.example.alohi.data.model.LoginResponse
import com.example.alohi.data.remote.ApiClient
import com.example.alohi.data.remote.SocketManager
import com.example.alohi.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * AloHi Auth ViewModel
 * Manages the complete authentication flow state:
 * 1. Send OTP → 2. Verify OTP → 3. Register (if new) → 4. Login
 *
 * All operations emit UI state via StateFlow.
 */

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,

    // OTP
    val otpSent: Boolean = false,
    val otpVerified: Boolean = false,
    val otpExpiresIn: Int = 300,

    // Registration
    val isRegistered: Boolean = false,
    val registeredUserId: String? = null,

    // Login
    val isLoggedIn: Boolean = false,
    val loginResponse: LoginResponse? = null,

    // Registration form
    val needsRegistration: Boolean = false,
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)
    private val repository = AuthRepository(tokenManager)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Initialize API client with token manager
        ApiClient.init(tokenManager)

        // Check if already logged in
        viewModelScope.launch {
            val loggedIn = repository.isLoggedIn()
            _uiState.value = _uiState.value.copy(isLoggedIn = loggedIn)
        }
    }

    // ═══════════════════════════════════════════════════════
    // STEP 1: SEND OTP
    // ═══════════════════════════════════════════════════════
    fun sendOtp(phone: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = repository.sendOtp(phone)
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        otpSent = true,
                        otpExpiresIn = response.expiresIn,
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message,
                    )
                }
            )
        }
    }

    // ═══════════════════════════════════════════════════════
    // STEP 2: VERIFY OTP
    // ═══════════════════════════════════════════════════════
    fun verifyOtp(phone: String, code: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = repository.verifyOtp(phone, code)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        otpVerified = true,
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message,
                    )
                }
            )
        }
    }

    // ═══════════════════════════════════════════════════════
    // STEP 3: REGISTER
    // ═══════════════════════════════════════════════════════
    fun register(
        phone: String,
        password: String,
        displayName: String,
        gender: String = "other",
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = repository.register(phone, password, displayName, gender)
            result.fold(
                onSuccess = { userId ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRegistered = true,
                        registeredUserId = userId,
                    )
                    // Auto-login after registration
                    login(phone, password)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message,
                    )
                }
            )
        }
    }

    // ═══════════════════════════════════════════════════════
    // STEP 4: LOGIN
    // ═══════════════════════════════════════════════════════
    fun login(phone: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = repository.login(phone, password)
            result.fold(
                onSuccess = { loginData ->
                    // Force socket to reconnect immediately with the new token
                    SocketManager.reconnect()
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        loginResponse = loginData,
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message,
                    )
                }
            )
        }
    }

    // ═══════════════════════════════════════════════════════
    // LOGOUT
    // ═══════════════════════════════════════════════════════
    fun logout(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            // Clear Room database cache (messages, conversations)
            val db = AloHiDatabase.getInstance(getApplication())
            db.messageDao().clearAll()
            db.conversationDao().clearAll()

            // Disconnect realtime socket
            SocketManager.disconnect()

            // Clear auth tokens
            repository.logout()

            // Destroy DB singleton
            AloHiDatabase.destroyInstance()

            _uiState.value = AuthUiState() // Reset to initial state
            onComplete()
        }
    }

    // ═══════════════════════════════════════════════════════
    // UI HELPERS
    // ═══════════════════════════════════════════════════════
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun setNeedsRegistration(needs: Boolean) {
        _uiState.value = _uiState.value.copy(needsRegistration = needs)
    }

    fun resetOtpState() {
        _uiState.value = _uiState.value.copy(
            otpSent = false,
            otpVerified = false,
        )
    }
}
