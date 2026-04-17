package com.example.alohi.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.alohi.ui.components.GradientButton
import com.example.alohi.ui.theme.AloHiTheme

/**
 * AloHi Onboarding Screen
 * Features:
 * - 3 beautiful slides with icons
 * - Smooth slide transitions
 * - Dot indicators
 * - Skip button + Get Started button
 * - Clean Apple-like design
 */

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val gradientColors: List<Color>
)

@Composable
fun OnboardingScreen(
    onNavigateToLogin: () -> Unit,
) {
    val colors = AloHiTheme.extendedColors

    val pages = listOf(
        OnboardingPage(
            icon = Icons.AutoMirrored.Filled.Chat,
            title = "Nhắn tin siêu tốc",
            subtitle = "Gửi tin nhắn văn bản, ảnh, video\nvà sticker nhanh chóng, mượt mà",
            gradientColors = listOf(Color(0xFF0A84FF), Color(0xFF00C6FB))
        ),
        OnboardingPage(
            icon = Icons.Filled.VideoCall,
            title = "Gọi điện miễn phí",
            subtitle = "Gọi thoại và video call chất lượng cao\nvới bạn bè và người thân",
            gradientColors = listOf(Color(0xFF5856D6), Color(0xFFAF52DE))
        ),
        OnboardingPage(
            icon = Icons.Filled.Lock,
            title = "Bảo mật tuyệt đối",
            subtitle = "Mã hóa đầu cuối cho mọi tin nhắn\nRiêng tư, an toàn, luôn được bảo vệ",
            gradientColors = listOf(Color(0xFF34C759), Color(0xFF00C7BE))
        ),
    )

    var currentPage by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Skip button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onNavigateToLogin) {
                Text(
                    text = "Bỏ qua",
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Content area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it } + fadeOut())
                },
                label = "onboardingSlide"
            ) { page ->
                OnboardingPageContent(page = pages[page])
            }
        }

        // Dot indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            pages.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(
                            width = if (index == currentPage) 24.dp else 8.dp,
                            height = 8.dp
                        )
                        .clip(CircleShape)
                        .background(
                            if (index == currentPage)
                                MaterialTheme.colorScheme.primary
                            else
                                colors.border
                        )
                )
            }
        }

        // Button
        Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
            GradientButton(
                text = if (currentPage == pages.lastIndex) "Bắt đầu ngay" else "Tiếp tục",
                onClick = {
                    if (currentPage < pages.lastIndex) {
                        currentPage++
                    } else {
                        onNavigateToLogin()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Icon with gradient background circle
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(page.gradientColors)
                ),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = page.icon,
                contentDescription = page.title,
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Title
        Text(
            text = page.title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Subtitle
        Text(
            text = page.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
        )
    }
}
