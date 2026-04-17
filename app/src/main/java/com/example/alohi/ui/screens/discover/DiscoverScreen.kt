package com.example.alohi.ui.screens.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.alohi.ui.components.AvatarImage
import com.example.alohi.ui.theme.AloHiTheme
import com.example.alohi.ui.viewmodel.MainViewModel
import com.example.alohi.data.model.UserProfile

/**
 * AloHi Discover Screen (Tab 3)
 * Features:
 * - Search bar
 * - Friend suggestions (horizontal cards)
 * - Feature grid (QR, Stickers, Call Logs, Backup)
 * - Clean Apple-like card design
 */

private data class FeatureItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val color: Color
)

private val features = listOf(
    FeatureItem(Icons.Default.QrCodeScanner, "Quét QR", "Thêm bạn nhanh", Color(0xFF0A84FF)),
    FeatureItem(Icons.Default.StickyNote2, "Sticker", "Kho sticker vui", Color(0xFFFF9500)),
    FeatureItem(Icons.Default.History, "Lịch sử gọi", "Cuộc gọi gần đây", Color(0xFF34C759)),
    FeatureItem(Icons.Default.Backup, "Sao lưu", "Cloud backup", Color(0xFF5856D6)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(mainViewModel: MainViewModel) {
    val colors = AloHiTheme.extendedColors
    val uiState by mainViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar (extends behind status bar)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(colors.gradientStart, colors.gradientEnd)
                    )
                )
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = "Khám phá",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Search, "Tìm kiếm", tint = Color.White)
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            // Friend suggestions
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(top = 20.dp)
                ) {
                    Text(
                        text = "Gợi ý kết bạn",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.friendSuggestions.isEmpty()) {
                        Text(
                            text = "Đang tìm kiếm gợi ý...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AloHiTheme.extendedColors.textSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
                        )
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.friendSuggestions) { suggestion ->
                                SuggestionCard(
                                    user = suggestion,
                                    onAddFriend = { mainViewModel.sendFriendRequest(suggestion.id) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                HorizontalDivider(color = colors.divider, thickness = 6.dp)
            }

            // Features grid
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Tính năng",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 2x2 grid
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            features.take(2).forEach { feature ->
                                FeatureCard(
                                    feature = feature,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            features.drop(2).forEach { feature ->
                                FeatureCard(
                                    feature = feature,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionCard(user: UserProfile, onAddFriend: () -> Unit) {
    Card(
        modifier = Modifier.width(140.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AvatarImage(imageUrl = user.avatar?.url, name = user.displayName, size = 56.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = "Có thể bạn quen",
                style = MaterialTheme.typography.labelSmall,
                color = AloHiTheme.extendedColors.textSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .clickable { onAddFriend() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Kết bạn",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(
    feature: FeatureItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { },
        colors = CardDefaults.cardColors(
            containerColor = feature.color.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = feature.title,
                tint = feature.color,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = feature.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = AloHiTheme.extendedColors.textSecondary
                )
            }
        }
    }
}
