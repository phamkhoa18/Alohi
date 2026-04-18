package com.example.alohi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.alohi.data.remote.SocketManager
import com.example.alohi.ui.theme.AloHiTheme

/**
 * AloHi Avatar Image
 * Features:
 * - Circular clipping with optional border
 * - Fallback with initials or icon
 * - Online indicator dot overlay
 * - Size variants (small, medium, large)
 */
@Composable
fun AvatarImage(
    name: String,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    size: Dp = 48.dp,
    showOnlineIndicator: Boolean = false,
    isOnline: Boolean = false,
    showBorder: Boolean = false,
) {
    val colors = AloHiTheme.extendedColors
    val initials = name.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    // Generate a consistent color from name
    val avatarColors = listOf(
        Color(0xFF0A84FF), Color(0xFF5856D6), Color(0xFFFF2D55),
        Color(0xFFFF9500), Color(0xFF34C759), Color(0xFFAF52DE),
        Color(0xFF00C7BE), Color(0xFFFF6482),
    )
    val colorIndex = kotlin.math.abs(name.hashCode()) % avatarColors.size
    val avatarColor = avatarColors[colorIndex]

    val finalUrl = if (imageUrl != null && imageUrl.startsWith("/")) {
        com.example.alohi.data.remote.ApiClient.BASE_URL.replace("/api/", "") + imageUrl
    } else {
        imageUrl
    }

    Box(modifier = modifier.size(size)) {
        // Avatar circle
        if (!finalUrl.isNullOrBlank()) {
            // Real avatar from URL
            coil3.compose.AsyncImage(
                model = finalUrl,
                contentDescription = "$name avatar",
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .then(
                        if (showBorder) Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ) else Modifier
                    ),
                contentScale = ContentScale.Crop,
            )
        } else {
            // Fallback: initials on gradient
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .then(
                        if (showBorder) Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ) else Modifier
                    )
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                avatarColor,
                                avatarColor.copy(alpha = 0.7f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (initials.isNotEmpty()) {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontSize = (size.value * 0.36f).sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(size * 0.5f)
                    )
                }
            }
        }

        // Online indicator
        if (showOnlineIndicator) {
            OnlineIndicator(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-1).dp, y = (-1).dp),
                size = size * 0.28f,
                isOnline = isOnline
            )
        }
    }
}

/**
 * Online indicator dot (Green for online, Gray for offline)
 */
@Composable
fun OnlineIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    isOnline: Boolean = true
) {
    val socketState by SocketManager.socketState.collectAsState()
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val indicatorColor = if (isOnline && socketState == SocketManager.SocketState.CONNECTED) {
            AloHiTheme.extendedColors.online
        } else {
            // Gray color for offline similar to Messenger
            Color(0xFFD1D1D6)
        }
        
        Box(
            modifier = Modifier
                .size(size - 4.dp)
                .clip(CircleShape)
                .background(indicatorColor)
        )
    }
}
