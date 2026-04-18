package com.example.alohi.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.alohi.ui.components.AvatarImage
import com.example.alohi.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    mainViewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String, String) -> Unit,
) {
    val uiState by mainViewModel.uiState.collectAsState()
    val viewedProfile = uiState.viewedProfile

    LaunchedEffect(userId) {
        mainViewModel.getUserById(userId)
    }

    val colors = com.example.alohi.ui.theme.AloHiTheme.extendedColors
    val primaryColor = MaterialTheme.colorScheme.primary

    if (viewedProfile == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = primaryColor)
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color(0xFFF2F2F7) // iOS style background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F7))
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            // ── PREMIUM HEADER (Cover + Avatar) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                // Cover Photo
                if (!viewedProfile.coverPhoto?.url.isNullOrEmpty()) {
                    AsyncImage(
                        model = viewedProfile.coverPhoto?.url,
                        contentDescription = "Cover Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF5856D6), Color(0xFF0A84FF))
                                )
                            )
                    )
                }

                // Avatar container
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF2F2F7),
                        modifier = Modifier.size(130.dp)
                    ) {
                        Box(Modifier.padding(6.dp)) {
                            AvatarImage(
                                name = viewedProfile.displayName,
                                imageUrl = viewedProfile.avatar?.url,
                                size = 118.dp,
                                showOnlineIndicator = true,
                                isOnline = viewedProfile.isOnline == true
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── USER INFO ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            ) {
                Text(
                    text = viewedProfile.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = Color.Black
                )
                
                if (viewedProfile.bio?.isNotBlank() == true) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = viewedProfile.bio,
                        color = Color(0xFF666666),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── PREMIUM ACTION BUTTONS ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Primary Action Button (Add Friend / Friend Status)
                val statusButtonModifier = Modifier.weight(1f).height(48.dp)
                
                when (viewedProfile.friendStatus) {
                    "friend" -> {
                        Button(
                            onClick = { /* TODO: Unfriend dialog */ },
                            modifier = statusButtonModifier,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5E5EA)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.People, contentDescription = null, tint = Color.Black)
                            Spacer(Modifier.width(8.dp))
                            Text("Bạn bè", color = Color.Black, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    "sent" -> {
                        Button(
                            onClick = { mainViewModel.cancelFriendRequest(viewedProfile.id) },
                            modifier = statusButtonModifier,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5E5EA)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PersonRemove, contentDescription = null, tint = Color.Black)
                            Spacer(Modifier.width(8.dp))
                            Text("Hủy lời mời", color = Color.Black, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    "received" -> {
                        Button(
                            onClick = { mainViewModel.acceptFriendRequestByUserId(viewedProfile.id) },
                            modifier = statusButtonModifier,
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Chấp nhận", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    else -> {
                        Button(
                            onClick = { mainViewModel.sendFriendRequest(viewedProfile.id) },
                            modifier = statusButtonModifier,
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Kết bạn", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Message Button
                Button(
                    onClick = { onNavigateToChat(viewedProfile.id, viewedProfile.displayName) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5E5EA)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ChatBubble, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Nhắn tin", color = Color.Black, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── PERSONAL INFORMATION CARD ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Thông tin cá nhân",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Phone Number
                    if (!viewedProfile.phone.isNullOrEmpty()) {
                        ProfileInfoRow(icon = Icons.Default.Phone, title = "Điện thoại", value = viewedProfile.phone)
                    } else {
                        ProfileInfoRow(icon = Icons.Default.Phone, title = "Điện thoại", value = "Đã ẩn")
                    }

                    HorizontalDivider(color = Color(0xFFF2F2F7), modifier = Modifier.padding(vertical = 12.dp))

                    // Gender
                    val genderText = when(viewedProfile.gender) {
                        "male" -> "Nam"
                        "female" -> "Nữ"
                        else -> "Khác"
                    }
                    ProfileInfoRow(icon = Icons.Default.Person, title = "Giới tính", value = genderText)

                    // Date of Birth
                    if (!viewedProfile.dateOfBirth.isNullOrEmpty()) {
                        HorizontalDivider(color = Color(0xFFF2F2F7), modifier = Modifier.padding(vertical = 12.dp))
                        val dob = viewedProfile.dateOfBirth.substringBefore("T")
                        ProfileInfoRow(icon = Icons.Default.Cake, title = "Ngày sinh", value = dob)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            
            // Block user option
            Text(
                text = "Chặn người dùng này",
                color = Color(0xFFFF3B30),
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { 
                        mainViewModel.blockUser(viewedProfile.id, false)
                    }
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun ProfileInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFF2F2F7), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF666666), modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(text = title, fontSize = 13.sp, color = Color(0xFF8E8E93))
            Spacer(Modifier.height(2.dp))
            Text(text = value, fontSize = 16.sp, color = Color.Black, fontWeight = FontWeight.Medium)
        }
    }
}
