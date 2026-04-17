package com.example.alohi.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.example.alohi.data.model.DeviceSession
import com.example.alohi.ui.components.AvatarImage
import com.example.alohi.ui.theme.AloHiTheme
import com.example.alohi.data.model.UserProfile

/**
 * AloHi Profile Screen (Tab 4)
 * Features:
 * - Header with gradient + large avatar
 * - User info (name, phone, status)
 * - QR code button
 * - Settings sections (grouped Apple-style)
 * - Dark mode toggle
 * - Logout button
 */
@Composable
fun ProfileScreen(
    currentUser: UserProfile? = null,
    sessions: List<DeviceSession> = emptyList(),
    onUpdateProfile: (Map<String, String>) -> Unit = {},
    onUpdateAvatar: (Uri) -> Unit = {},
    onLoadSessions: () -> Unit = {},
    onLogoutSession: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    onRefresh: () -> Unit = {},
) {
    val colors = AloHiTheme.extendedColors
    var isDarkMode by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showSessionsDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { onUpdateAvatar(it) }
    }

    val displayName = currentUser?.displayName ?: "AloHi User"
    val statusText = currentUser?.customStatusText?.takeIf { it.isNotBlank() }
        ?: currentUser?.bio?.takeIf { it.isNotBlank() }
        ?: "Đang hoạt động"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Profile Header ──
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(colors.gradientStart, colors.gradientEnd)
                        )
                    )
                    .windowInsetsPadding(WindowInsets.statusBars),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 24.dp, bottom = 24.dp)
                ) {
                    // Avatar
                    Box(modifier = Modifier.clickable { avatarPicker.launch("image/*") }) {
                        AvatarImage(
                            name = displayName,
                            imageUrl = currentUser?.avatar?.url,
                            size = 84.dp,
                            showBorder = true
                        )
                        // Edit icon
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable { showEditDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Sửa",
                                tint = colors.gradientStart,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // ── QR Code Card ──
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp)
                    .clickable { showQrDialog = true },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "QR",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mã QR của tôi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Chia sẻ để bạn bè thêm nhanh",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                        contentDescription = null,
                        tint = colors.textTertiary
                    )
                }
            }
        }

        // ── Settings Section 1 ──
        item {
            SettingsSection(
                title = "Cài đặt chung",
                modifier = Modifier.padding(top = 20.dp)
            ) {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "Chế độ tối",
                    iconColor = Color(0xFF5856D6),
                    trailing = {
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { isDarkMode = it },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Thông báo",
                    iconColor = Color(0xFFFF3B30),
                    onClick = { }
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Quyền riêng tư",
                    iconColor = Color(0xFF0A84FF),
                    onClick = { }
                )
            }
        }

        // ── Settings Section 2 ──
        item {
            SettingsSection(title = "Dữ liệu") {
                SettingsItem(
                    icon = Icons.Default.Backup,
                    title = "Sao lưu & Khôi phục",
                    iconColor = Color(0xFF34C759),
                    onClick = { showBackupDialog = true }
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.PhoneAndroid,
                    title = "Thiết bị đăng nhập",
                    iconColor = Color(0xFFFF9500),
                    onClick = { 
                        onLoadSessions()
                        showSessionsDialog = true
                    }
                )
            }
        }

        // ── Settings Section 3 ──
        item {
            SettingsSection(title = "Khác") {
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Help,
                    title = "Trợ giúp & Hỗ trợ",
                    iconColor = Color(0xFF00C7BE),
                    onClick = { }
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Về AloHi",
                    subtitle = "Phiên bản 1.0.0",
                    iconColor = Color(0xFF8E8E93),
                    onClick = { }
                )
            }
        }

        // ── Logout ──
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clickable { onLogout() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Đăng xuất",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Đăng xuất",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            currentUser = currentUser,
            onDismiss = { showEditDialog = false },
            onSave = { newName, newBio, newStatus ->
                val updates = mutableMapOf<String, String>()
                updates["displayName"] = newName
                updates["bio"] = newBio
                updates["customStatusText"] = newStatus
                onUpdateProfile(updates)
                showEditDialog = false
            }
        )
    }

    if (showQrDialog) {
        QRCodeDialog(
            phone = currentUser?.phone ?: "",
            displayName = displayName,
            onDismiss = { showQrDialog = false }
        )
    }

    if (showSessionsDialog) {
        SessionsDialog(
            sessions = sessions,
            onLogoutSession = { onLogoutSession(it) },
            onDismiss = { showSessionsDialog = false }
        )
    }

    if (showBackupDialog) {
        BackupDialog(
            onDismiss = { showBackupDialog = false }
        )
    }
}

@Composable
private fun SessionsDialog(
    sessions: List<DeviceSession>,
    onLogoutSession: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thiết bị đăng nhập", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (sessions.isEmpty()) {
                    item { Text("Đang tải hoặc không có thiết bị...") }
                } else {
                    items(sessions.size) { index ->
                        val session = sessions[index]
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFFF2F2F7), RoundedCornerShape(12.dp)).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(session.deviceName ?: session.deviceModel ?: "Thiết bị không xác định", fontWeight = FontWeight.Bold, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, maxLines = 1, fontSize = 16.sp)
                                Text("${session.platform} • Cuối: ${session.lastActiveAt?.take(10) ?: "N/A"}", fontSize = 12.sp, color = Color.Gray)
                            }
                            TextButton(onClick = { onLogoutSession(session.id) }) {
                                Text("Thoát", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}

@Composable
private fun BackupDialog(onDismiss: () -> Unit) {
    var isBackingUp by remember { mutableStateOf(false) }
    var backupProgress by remember { mutableStateOf(0f) }
    var lastBackup by remember { mutableStateOf("Hôm qua lúc 14:00") }

    androidx.compose.runtime.LaunchedEffect(isBackingUp) {
        if (isBackingUp) {
            backupProgress = 0f
            while (backupProgress < 1f) {
                kotlinx.coroutines.delay(300)
                backupProgress += 0.1f
            }
            isBackingUp = false
            lastBackup = "Vừa xong"
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isBackingUp) onDismiss() },
        title = { Text("Sao lưu và Khôi phục", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Backup, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(64.dp).padding(bottom = 16.dp))
                Text("Bảo vệ tin nhắn của bạn bằng cách sao lưu lên Cloud.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F7)), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Bản sao lưu gần nhất", fontWeight = FontWeight.Bold)
                        Text(lastBackup, color = Color.Gray)
                        Text("Kích thước: 14.5 MB", color = Color.Gray)
                    }
                }
                if (isBackingUp) {
                    androidx.compose.material3.LinearProgressIndicator(progress = { backupProgress }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
                    Text("Đang sao lưu... ${(backupProgress * 100).toInt()}%", fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = { isBackingUp = true }, enabled = !isBackingUp) {
                Text("Sao lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBackingUp) {
                Text("Đóng")
            }
        }
    )
}

@Composable
private fun QRCodeDialog(
    phone: String,
    displayName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // For Zalo/AloHi style, QR code could just encode a deep link
    val qrContent = "alohi://add-friend?phone=$phone"
    
    val qrBitmap = remember(qrContent) {
        try {
            val size = 512
            val bitMatrix = QRCodeWriter().encode(qrContent, BarcodeFormat.QR_CODE, size, size)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mã QR của $displayName", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.size(200.dp)
                    )
                } else {
                    Text("Lỗi tạo mã QR")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Quét mã này để kết bạn với tôi trên AloHi", style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}

@Composable
private fun EditProfileDialog(
    currentUser: UserProfile?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var bio by remember { mutableStateOf(currentUser?.bio ?: "") }
    var statusText by remember { mutableStateOf(currentUser?.customStatusText ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chỉnh sửa thông tin", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Tên hiển thị") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Tiểu sử") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                OutlinedTextField(
                    value = statusText,
                    onValueChange = { statusText = it },
                    label = { Text("Trạng thái tùy chỉnh") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(displayName, bio, statusText) }) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = AloHiTheme.extendedColors.textSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    iconColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AloHiTheme.extendedColors.textSecondary
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                contentDescription = null,
                tint = AloHiTheme.extendedColors.textTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 58.dp),
        color = AloHiTheme.extendedColors.divider,
        thickness = 0.5.dp
    )
}
