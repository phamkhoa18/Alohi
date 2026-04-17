package com.example.alohi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.alohi.ui.theme.AloHiTheme

/**
 * AloHi Chat List Item
 * Features:
 * - Avatar with online indicator
 * - Name + last message preview
 * - Timestamp (right-aligned)
 * - Unread badge (red circle)
 * - Muted/Pinned indicators
 * - Swipe actions ready
 *
 * Layout: [Avatar] [Name + LastMsg] [Time + Badge]
 */
@Composable
fun ChatListItem(
    name: String,
    lastMessage: String,
    timestamp: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    isOnline: Boolean = false,
    unreadCount: Int = 0,
    isMuted: Boolean = false,
    isTyping: Boolean = false,
    isGroup: Boolean = false,
    senderPrefix: String? = null, // "Bạn: " or "Minh: " for groups
    lastMessageIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    val colors = AloHiTheme.extendedColors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        AvatarImage(
            name = name,
            imageUrl = avatarUrl,
            size = 56.dp,
            showOnlineIndicator = !isGroup,
            isOnline = isOnline
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Content (name + last message)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // Name
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (unreadCount > 0) FontWeight.SemiBold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Last message or typing indicator
            if (isTyping) {
                Text(
                    text = "Đang nhập...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (lastMessageIcon != null) {
                        androidx.compose.material3.Icon(
                            imageVector = lastMessageIcon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (unreadCount > 0) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f) else colors.textSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = buildString {
                            if (senderPrefix != null) append(senderPrefix)
                            append(lastMessage)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (unreadCount > 0)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        else
                            colors.textSecondary,
                        fontWeight = if (unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right side (timestamp + badge)
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            // Timestamp
            Text(
                text = timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = if (unreadCount > 0)
                    MaterialTheme.colorScheme.primary
                else
                    colors.textTertiary
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Unread badge
            if (unreadCount > 0) {
                UnreadBadge(count = unreadCount)
            }
        }
    }
}

/**
 * Unread message badge
 * Red circle with count
 */
@Composable
fun UnreadBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    val displayText = if (count > 99) "99+" else count.toString()
    val minSize = if (count > 9) 22.dp else 20.dp

    Box(
        modifier = modifier
            .size(width = minSize, height = 20.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.error),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onError,
            fontWeight = FontWeight.Bold,
        )
    }
}
