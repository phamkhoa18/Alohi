package com.example.alohi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.example.alohi.ui.screens.conversation.ChatPanel

/**
 * AloHi Message Composer — Zalo-Style
 *
 * Layout (no text):
 *   [😀]  [___Tin nhắn___]  [•••]  [🎤]  [🖼]
 *
 * Layout (has text):
 *   [😀]  [___Hello___]  [➤ Send]
 *
 * Active panel icons are highlighted blue.
 * Emoji icon switches to keyboard icon when sticker panel is open.
 */
@Composable
fun MessageComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
    activePanel: ChatPanel = ChatPanel.NONE,
    onStickerToggle: () -> Unit = {},
    onAttachToggle: () -> Unit = {},
    onMicToggle: () -> Unit = {},
    onGalleryToggle: () -> Unit = {},
    onTextFieldFocused: () -> Unit = {},
    placeholder: String = "Tin nhắn",
) {
    val hasText = value.isNotBlank()
    val focusRequester = remember { FocusRequester() }
    val inactiveColor = Color(0xFF8E8E93)
    val activeColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // ═══════════════════════════════════════
        // LEFT: Emoji/Sticker toggle
        // Shows keyboard icon when sticker panel is active
        // ═══════════════════════════════════════
        IconButton(
            onClick = onStickerToggle,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (activePanel == ChatPanel.STICKER)
                    Icons.Default.Keyboard
                else
                    Icons.Default.EmojiEmotions,
                contentDescription = if (activePanel == ChatPanel.STICKER) "Bàn phím" else "Sticker",
                tint = if (activePanel == ChatPanel.STICKER) activeColor else inactiveColor,
                modifier = Modifier.size(26.dp)
            )
        }

        // ═══════════════════════════════════════
        // CENTER: Text Input Field
        // ═══════════════════════════════════════
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 40.dp, max = 140.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF2F2F7))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFAEAEB2)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onTextFieldFocused()
                        }
                    },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.Black
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                maxLines = 5
            )
        }

        // ═══════════════════════════════════════
        // RIGHT: Action buttons
        // ═══════════════════════════════════════
        if (hasText) {
            // ── Send Button (blue circle) ──
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSendClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Gửi",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            // ── ••• (More/Attachment) ──
            IconButton(
                onClick = onAttachToggle,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "Thêm tùy chọn",
                    tint = if (activePanel == ChatPanel.ATTACHMENT) activeColor else inactiveColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            // ── 🎤 (Mic/Voice) ──
            IconButton(
                onClick = onMicToggle,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Ghi âm",
                    tint = if (activePanel == ChatPanel.VOICE) activeColor else inactiveColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            // ── 🖼 (Gallery) ──
            IconButton(
                onClick = onGalleryToggle,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Thư viện ảnh",
                    tint = if (activePanel == ChatPanel.GALLERY) activeColor else inactiveColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
