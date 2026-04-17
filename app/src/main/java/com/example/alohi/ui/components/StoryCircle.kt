package com.example.alohi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.alohi.ui.theme.AloHiTheme

/**
 * AloHi Story Circle — Full-Featured
 * Features:
 * - Gradient ring (unseen story) / grey ring (seen)
 * - "Add" variant with camera icon + blue "+" badge
 * - Story count badge
 * - Name label below
 * - Click handler
 * - Zalo/Instagram-like design
 */
@Composable
fun StoryCircle(
    name: String,
    modifier: Modifier = Modifier,
    hasSeen: Boolean = false,
    isAddStory: Boolean = false,
    storyCount: Int = 1,
    onClick: () -> Unit = {},
) {
    val colors = AloHiTheme.extendedColors

    Column(
        modifier = modifier
            .width(76.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar with ring
        Box(
            modifier = Modifier.size(66.dp),
            contentAlignment = Alignment.Center
        ) {
            // ── Outer Ring ──
            val ringModifier = if (!isAddStory && !hasSeen) {
                // Gradient ring for unseen stories
                Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                colors.gradientStart,
                                colors.gradientEnd,
                                Color(0xFF5856D6),
                                colors.gradientStart,
                            )
                        )
                    )
            } else if (!isAddStory && hasSeen) {
                // Grey ring for seen stories
                Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD1D1D6))
            } else {
                // No ring for add-story (just border)
                Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.5.dp,
                        color = Color(0xFFE5E5EA),
                        shape = CircleShape
                    )
            }

            Box(modifier = ringModifier)

            // ── White gap between ring and avatar ──
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                // Inner avatar
                if (isAddStory) {
                    // Camera icon for "add story"
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF2F2F7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Tạo tin",
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    AvatarImage(
                        name = name,
                        size = 56.dp,
                        showOnlineIndicator = false
                    )
                }
            }

            // ── Add Story "+" badge ──
            if (isAddStory) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // ── Story count badge (for multiple stories) ──
            if (!isAddStory && storyCount > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            if (hasSeen) Color(0xFF8E8E93)
                            else MaterialTheme.colorScheme.primary
                        )
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$storyCount",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Name
        Text(
            text = if (isAddStory) "Tin của tôi" else name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isAddStory)
                MaterialTheme.colorScheme.onSurfaceVariant
            else if (hasSeen)
                Color(0xFFAEAEB2)
            else
                MaterialTheme.colorScheme.onSurface,
            fontWeight = if (!hasSeen && !isAddStory) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}
