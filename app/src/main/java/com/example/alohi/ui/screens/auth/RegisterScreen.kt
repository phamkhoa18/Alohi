package com.example.alohi.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.example.alohi.ui.components.AloHiToast
import com.example.alohi.ui.components.ToastData
import com.example.alohi.ui.components.ToastType
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.alohi.ui.components.GradientButton
import com.example.alohi.ui.theme.AloHiBlue
import com.example.alohi.ui.theme.AloHiCyan
import com.example.alohi.ui.theme.AloHiTheme
import com.example.alohi.ui.viewmodel.AuthViewModel

/**
 * AloHi Register Screen
 * Fields: Tên hiển thị, SĐT (pre-filled), Mật khẩu, Giới tính
 * Auto-login after successful registration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    phone: String,
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onRegisterSuccess: () -> Unit,
) {
    val uiState by authViewModel.uiState.collectAsState()
    val colors = AloHiTheme.extendedColors
    var toastData by remember { mutableStateOf<ToastData?>(null) }

    var displayName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf(phone) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var gender by remember { mutableStateOf("other") }
    var genderExpanded by remember { mutableStateOf(false) }

    val genderOptions = listOf(
        "male" to "Nam",
        "female" to "Nữ",
        "other" to "Khác"
    )

    val isFormValid = displayName.length >= 2
            && phoneNumber.length >= 9
            && password.length >= 6
            && password == confirmPassword

    // Navigate on successful login (auto-login after register)
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn && uiState.isRegistered) {
            onRegisterSuccess()
        }
    }

    // Show errors as toast
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            toastData = ToastData(it, ToastType.ERROR)
            authViewModel.clearError()
        }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = colors.border,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Gradient Header ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(AloHiBlue, AloHiCyan.copy(alpha = 0.9f))
                        )
                    ),
            ) {
                // Back button
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.padding(top = 40.dp, start = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChatBubble,
                        contentDescription = "AloHi",
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tạo tài khoản",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Điền thông tin để bắt đầu",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // ── Form ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
            ) {
                // Tên hiển thị
                Text(
                    text = "Tên hiển thị",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { if (it.length <= 50) displayName = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Nguyễn Văn A", color = colors.textTertiary) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = textFieldColors,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // SĐT
                Text(
                    text = "Số điện thoại",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) phoneNumber = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("0912 345 678", color = colors.textTertiary) },
                    prefix = { Text("+84  ", fontWeight = FontWeight.Medium) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = textFieldColors,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Mật khẩu
                Text(
                    text = "Mật khẩu",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Tối thiểu 6 ký tự", color = colors.textTertiary) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Ẩn" else "Hiện",
                                tint = colors.textTertiary
                            )
                        }
                    },
                    colors = textFieldColors,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Xác nhận mật khẩu
                Text(
                    text = "Xác nhận mật khẩu",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Nhập lại mật khẩu", color = colors.textTertiary) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                    supportingText = if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                        { Text("Mật khẩu không khớp", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    colors = textFieldColors,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Giới tính
                Text(
                    text = "Giới tính",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ExposedDropdownMenuBox(
                    expanded = genderExpanded,
                    onExpandedChange = { genderExpanded = !genderExpanded }
                ) {
                    OutlinedTextField(
                        value = genderOptions.find { it.first == gender }?.second ?: "Khác",
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        readOnly = true,
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                        colors = textFieldColors,
                    )
                    ExposedDropdownMenu(
                        expanded = genderExpanded,
                        onDismissRequest = { genderExpanded = false }
                    ) {
                        genderOptions.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    gender = value
                                    genderExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Register button
                GradientButton(
                    text = "Đăng ký",
                    onClick = {
                        authViewModel.register(
                            phone = phoneNumber,
                            password = password,
                            displayName = displayName.trim(),
                            gender = gender,
                        )
                    },
                    enabled = isFormValid,
                    isLoading = uiState.isLoading,
                )
            }
        }

        // AloHi Toast
        AloHiToast(
            toastData = toastData,
            onDismiss = { toastData = null },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
