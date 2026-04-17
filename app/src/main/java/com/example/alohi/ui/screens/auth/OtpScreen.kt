package com.example.alohi.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.alohi.ui.components.AloHiToast
import com.example.alohi.ui.components.GradientButton
import com.example.alohi.ui.components.OtpInputField
import com.example.alohi.ui.components.ToastData
import com.example.alohi.ui.components.ToastType
import com.example.alohi.ui.theme.AloHiTheme
import com.example.alohi.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

/**
 * AloHi OTP Verification Screen — Connected to Backend
 * Uses AloHiToast for beautiful Zalo-style notifications
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpScreen(
    phone: String,
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onVerifySuccess: () -> Unit,
) {
    val uiState by authViewModel.uiState.collectAsState()
    var otpValue by remember { mutableStateOf("") }
    var countdown by remember { mutableIntStateOf(60) }
    var canResend by remember { mutableStateOf(false) }
    val colors = AloHiTheme.extendedColors

    // Toast state
    var toastData by remember { mutableStateOf<ToastData?>(null) }

    // Countdown timer
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        } else {
            canResend = true
        }
    }

    // Navigate on OTP verified
    LaunchedEffect(uiState.otpVerified) {
        if (uiState.otpVerified) {
            toastData = ToastData("Xác thực thành công!", ToastType.SUCCESS)
            delay(800)
            onVerifySuccess()
        }
    }

    // Show errors as toast
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            toastData = ToastData(it, ToastType.ERROR)
            authViewModel.clearError()
        }
    }

    // Mask phone: 0912***678
    val maskedPhone = if (phone.length >= 7) {
        phone.take(4) + "***" + phone.takeLast(3)
    } else phone

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
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Nhập mã xác thực",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Mã 6 chữ số đã được gửi đến",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = maskedPhone,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Dev hint
                Text(
                    text = "(Xem OTP trong server console)",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                // OTP Input
                OtpInputField(
                    otpValue = otpValue,
                    onOtpChange = { otpValue = it },
                    onComplete = { otp ->
                        authViewModel.verifyOtp(phone, otp)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Resend timer
                if (canResend) {
                    Text(
                        text = "Gửi lại mã",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            countdown = 60
                            canResend = false
                            authViewModel.sendOtp(phone)
                            toastData = ToastData("Đã gửi lại mã OTP", ToastType.INFO)
                        }
                    )
                } else {
                    Text(
                        text = buildAnnotatedString {
                            append("Gửi lại mã sau ")
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)) {
                                append("${countdown}s")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Verify button
                GradientButton(
                    text = "Xác nhận",
                    onClick = {
                        authViewModel.verifyOtp(phone, otpValue)
                    },
                    enabled = otpValue.length == 6,
                    isLoading = uiState.isLoading
                )
            }
        }

        // ── AloHi Toast ──
        AloHiToast(
            toastData = toastData,
            onDismiss = { toastData = null },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
