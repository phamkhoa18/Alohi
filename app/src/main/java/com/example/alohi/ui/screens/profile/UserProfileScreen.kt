package com.example.alohi.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Removed Coil import
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
    ) {
        // ── Gradient Top Bar ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(colors.gradientStart, colors.gradientEnd)
                    )
                )
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            TopAppBar(
                title = { Text(viewedProfile?.displayName ?: "Cá nhân", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }

        if (viewedProfile == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F7))
        ) {
            // Cover Photo
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                // Placeholder gradient for cover photo
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color(0xFF5856D6), Color(0xFF0A84FF))
                            )
                        )
                )
                
                // Avatar positioned over cover
                Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp).offset(y = 40.dp)) {
                    Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(86.dp)) {
                        Box(Modifier.padding(3.dp)) {
                            AvatarImage(name = viewedProfile.displayName, imageUrl = viewedProfile.avatar?.url, size = 80.dp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(48.dp))

            // User Info
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(viewedProfile.displayName, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                if (viewedProfile.bio?.isNotBlank() == true) {
                    Text(viewedProfile.bio, color = Color.Gray, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Action Buttons
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { onNavigateToChat(viewedProfile.id, viewedProfile.displayName) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Nhắn tin", color = MaterialTheme.colorScheme.primary)
                }
                
                if (viewedProfile.friendStatus == "friend") {
                    // Could add unfriend action later
                } else if (viewedProfile.friendStatus == "sent") {
                    Button(
                        onClick = { mainViewModel.cancelFriendRequest(viewedProfile.id) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Icon(Icons.Default.PersonRemove, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text("Hủy lời mời", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Button(
                        onClick = { mainViewModel.sendFriendRequest(viewedProfile.id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Kết bạn")
                    }
                }
            }
        }
    }
}
