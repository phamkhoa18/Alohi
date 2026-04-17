package com.example.alohi.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * AloHi Toast — Zalo-style notification bar
 *
 * ┌────────────────────────────────────────┐
 * │  ✅  Đăng ký thành công!               │
 * └────────────────────────────────────────┘
 *
 * Types: SUCCESS (green), ERROR (red), WARNING (orange), INFO (blue)
 * Auto-dismiss after duration (default 3s)
 * Slides in from top with fade animation
 */

enum class ToastType {
    SUCCESS, ERROR, WARNING, INFO
}

data class ToastData(
    val message: String,
    val type: ToastType = ToastType.INFO,
    val durationMs: Long = 3000L,
)

@Composable
fun AloHiToast(
    toastData: ToastData?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(toastData) {
        if (toastData != null) {
            isVisible = true
            delay(toastData.durationMs)
            isVisible = false
            delay(300) // wait for exit animation
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = isVisible && toastData != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
    ) {
        toastData?.let { data ->
            val (bgColor, iconColor, icon) = getToastStyle(data.type)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(14.dp),
                            ambientColor = Color.Black.copy(alpha = 0.1f)
                        )
                        .clip(RoundedCornerShape(14.dp))
                        .background(bgColor)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    // Icon
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Message
                    Text(
                        text = data.message,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private data class ToastStyle(
    val bgColor: Color,
    val iconColor: Color,
    val icon: ImageVector,
)

private fun getToastStyle(type: ToastType): ToastStyle {
    return when (type) {
        ToastType.SUCCESS -> ToastStyle(
            bgColor = Color(0xFF2ED573),
            iconColor = Color.White,
            icon = Icons.Default.CheckCircle
        )
        ToastType.ERROR -> ToastStyle(
            bgColor = Color(0xFFFF4757),
            iconColor = Color.White,
            icon = Icons.Default.Error
        )
        ToastType.WARNING -> ToastStyle(
            bgColor = Color(0xFFFF9F43),
            iconColor = Color.White,
            icon = Icons.Default.Warning
        )
        ToastType.INFO -> ToastStyle(
            bgColor = Color(0xFF3867D6),
            iconColor = Color.White,
            icon = Icons.Default.Info
        )
    }
}
