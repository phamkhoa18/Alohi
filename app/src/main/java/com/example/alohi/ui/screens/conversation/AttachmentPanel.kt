package com.example.alohi.ui.screens.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * AloHi Attachment Panel — Zalo-Style
 * Features:
 * - 4-column grid of attachment options
 * - Each item: colored circle icon + label
 * - Matching Zalo's exact layout:
 *   Row 1: Vị trí | Tài liệu | Nhắc hẹn | Bình chọn
 *   Row 2: Chia tiền | Danh thiếp | Cloud | Tin nhanh
 *   Row 3: Tài khoản | @GIF | Vẽ hình | Kiểu chữ
 */

private data class AttachOption(
    val label: String,
    val icon: ImageVector?,
    val textIcon: String? = null, // For text-based icons like "GIF", "Aa"
    val bgColor: Color,
    val actionTag: String = "", // programmatic tag for dispatch
)

private val attachOptions = listOf(
    AttachOption("Vị trí", Icons.Default.LocationOn, bgColor = Color(0xFFFF6B6B), actionTag = "location"),
    AttachOption("Tài liệu", Icons.Default.AttachFile, bgColor = Color(0xFF5B9BD5), actionTag = "file"),
    AttachOption("Nhắc hẹn", Icons.Default.AccessTime, bgColor = Color(0xFFFF9F43), actionTag = "reminder"),
    AttachOption("Bình chọn", Icons.Default.Poll, bgColor = Color(0xFF2ED573), actionTag = "poll"),
    AttachOption("Ảnh", Icons.Default.ContactPhone, bgColor = Color(0xFF1ABC9C), actionTag = "photo"),
    AttachOption("Video", Icons.Default.ContactPhone, bgColor = Color(0xFF3867D6), actionTag = "video"),
    AttachOption("My\nDocuments", Icons.Default.CloudUpload, bgColor = Color(0xFF74B9FF), actionTag = "file"),
    AttachOption("Tin nhắn\nnhanh", Icons.Default.FlashOn, bgColor = Color(0xFF4B7BEC), actionTag = "quick"),
    AttachOption("Gửi số\ntài khoản", Icons.Default.AccountBalance, bgColor = Color(0xFF26DE81), actionTag = "bank"),
    AttachOption("@GIF", null, textIcon = "GIF", bgColor = Color(0xFFFC5C65), actionTag = "gif"),
    AttachOption("Vẽ hình", Icons.Default.Brush, bgColor = Color(0xFFA55EEA), actionTag = "draw"),
    AttachOption("Kiểu chữ", null, textIcon = "Aa", bgColor = Color(0xFF8854D0), actionTag = "font"),
)

@Composable
fun AttachmentPanel(
    modifier: Modifier = Modifier,
    onOptionClick: (String) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color.White)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(attachOptions) { option ->
                AttachOptionItem(
                    option = option,
                    onClick = { onOptionClick(option.actionTag) }
                )
            }
        }
    }
}

@Composable
private fun AttachOptionItem(
    option: AttachOption,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Colored circle icon
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(option.bgColor),
            contentAlignment = Alignment.Center
        ) {
            if (option.icon != null) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = option.label,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            } else if (option.textIcon != null) {
                Text(
                    text = option.textIcon,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Label
        Text(
            text = option.label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF636366),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp
        )
    }
}
