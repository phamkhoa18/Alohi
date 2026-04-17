package com.example.alohi.ui.screens.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * AloHi Sticker Panel — Zalo-Style
 * Features:
 * - Tab row: GIF | 😀 Emoji | 🕐 Recent | Sticker categories
 * - Grid of stickers with tap-to-send
 * - "Gần đây" section header
 * - Scrollable category tabs
 * - Height matches keyboard (~280dp)
 */

private data class StickerTab(
    val label: String,
    val emoji: String,
)

private val stickerTabs = listOf(
    StickerTab("GIF", "GIF"),
    StickerTab("Emoji", "😀"),
    StickerTab("Gần đây", "🕐"),
    StickerTab("Mèo", "🐱"),
    StickerTab("Gấu", "🐻"),
    StickerTab("Thỏ", "🐰"),
    StickerTab("Cún", "🐶"),
    StickerTab("Chim", "🐥"),
    StickerTab("Cửa hàng", "🏪"),
)

// Mock sticker emojis (representing sticker images)
private val recentStickers = listOf(
    "😂", "😍", "🤣", "👍", "😭", "🙏", "😘", "🥰",
    "😊", "🥺", "❤️", "😅", "🔥", "💕", "😁", "🎉",
)

private val catStickers = listOf(
    "😸", "😹", "😺", "😻", "😼", "😽", "😾", "😿",
    "🙀", "🐱", "🐈", "🐈‍⬛", "😸", "😹", "😺", "😻",
)

private val emojiList = listOf(
    "😀", "😃", "😄", "😁", "😆", "🥹", "😅", "😂",
    "🤣", "🥲", "☺️", "😊", "😇", "🙂", "🙃", "😉",
    "😌", "😍", "🥰", "😘", "😗", "😙", "😚", "😋",
    "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎",
    "🥸", "🤩", "🥳", "😏", "😒", "😞", "😔", "😟",
    "😕", "🙁", "☹️", "😣", "😖", "😫", "😩", "🥺",
    "😢", "😭", "😮‍💨", "😤", "😠", "😡", "🤬", "🤯",
    "😳", "🥵", "🥶", "😱", "😨", "😰", "😥", "😓",
)

@Composable
fun StickerPanel(
    modifier: Modifier = Modifier,
    onStickerClick: (String) -> Unit = {},
) {
    var selectedTab by remember { mutableIntStateOf(2) } // Default: "Gần đây"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color.White)
    ) {
        // ═══════════════════════════════════════
        // TAB ROW
        // ═══════════════════════════════════════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8F8F8))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            stickerTabs.forEachIndexed { index, tab ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) Color(0xFFE8E8ED)
                            else Color.Transparent
                        )
                        .clickable { selectedTab = index }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (tab.label == "GIF") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color(0xFFAEAEB2)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "GIF",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = tab.emoji,
                            fontSize = 22.sp
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)

        // ═══════════════════════════════════════
        // CONTENT
        // ═══════════════════════════════════════
        val currentStickers = when (selectedTab) {
            0 -> recentStickers // GIF tab shows recent for now
            1 -> emojiList
            2 -> recentStickers
            3 -> catStickers
            else -> recentStickers
        }

        val sectionTitle = when (selectedTab) {
            0 -> "GIF phổ biến"
            1 -> "Emoji"
            2 -> "Gần đây"
            3 -> "Mèo"
            4 -> "Gấu"
            5 -> "Thỏ"
            else -> "Sticker"
        }

        // Section header
        Text(
            text = sectionTitle,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF8E8E93),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Sticker grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (selectedTab == 1) 8 else 4),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items(currentStickers) { sticker ->
                Box(
                    modifier = Modifier
                        .size(if (selectedTab == 1) 44.dp else 72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onStickerClick(sticker) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = sticker,
                        fontSize = if (selectedTab == 1) 28.sp else 40.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
