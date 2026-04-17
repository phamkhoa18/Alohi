package com.example.alohi.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.alohi.ui.components.AloHiToast
import com.example.alohi.ui.components.GradientButton
import com.example.alohi.ui.components.ToastData
import com.example.alohi.ui.components.ToastType
import com.example.alohi.ui.theme.AloHiTheme
import com.example.alohi.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    phone: String,
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val uiState by authViewModel.uiState.collectAsState()
    val colors = AloHiTheme.extendedColors

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var toastData by remember { mutableStateOf<ToastData?>(null) }

    // Navigation trigger on success
    LaunchedEffect(uiState.passwordResetSuccess) {
        if (uiState.passwordResetSuccess) {
            toastData = ToastData("Mật khẩu đã được thiết lập lại", ToastType.SUCCESS)
            kotlinx.coroutines.delay(1000)
            onNavigateToLogin()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            toastData = ToastData(it, ToastType.ERROR)
            authViewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
        ) {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Tạo mật khẩu mới",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Vui lòng tạo một mật khẩu mới cho tài khoản liên kết với số: $phone",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Mật khẩu mới
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Nhập mật khẩu mới") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = colors.border,
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Xác nhận mật khẩu mới
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Xác nhận mật khẩu mới") },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = colors.border,
                    )
                )

                Spacer(modifier = Modifier.height(40.dp))

                GradientButton(
                    text = "Cập nhật mật khẩu",
                    onClick = {
                        if (newPassword != confirmPassword) {
                            toastData = ToastData("Mật khẩu không khớp", ToastType.ERROR)
                            return@GradientButton
                        }
                        if (newPassword.length < 6) {
                            toastData = ToastData("Mật khẩu phải từ 6 ký tự", ToastType.WARNING)
                            return@GradientButton
                        }
                        authViewModel.resetPassword(phone, otpCode = "123456", newPassword = newPassword)
                    },
                    enabled = newPassword.isNotEmpty() && confirmPassword.isNotEmpty(),
                    isLoading = uiState.isLoading
                )
            }
        }

        AloHiToast(
            toastData = toastData,
            onDismiss = { toastData = null },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
