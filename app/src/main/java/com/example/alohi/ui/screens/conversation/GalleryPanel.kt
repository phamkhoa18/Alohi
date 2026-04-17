package com.example.alohi.ui.screens.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * AloHi Gallery Panel — Zalo-Style
 * Features:
 * - Camera button (large, first item)
 * - Grid of recent photos (placeholders)
 * - "Chụp ảnh" label
 * - 4-column grid layout
 * - Height matches keyboard (~280dp)
 */

// Mock photo colors (simulating recent photos)
private val photoPlaceholderColors = listOf(
    Color(0xFFE0C3FC), Color(0xFF8EC5FC), Color(0xFFFFA69E),
    Color(0xFFA8E6CF), Color(0xFFDFBB9D), Color(0xFFCDB4DB),
    Color(0xFFBDE0FE), Color(0xFFFFC8DD), Color(0xFFCFBAF0),
    Color(0xFFA2D2FF), Color(0xFFFFD6A5), Color(0xFFFDFFB6),
    Color(0xFFCAFFBF), Color(0xFF9BF6FF), Color(0xFFF1C0E8),
)

@Composable
fun GalleryPanel(
    modifier: Modifier = Modifier,
    onCameraClick: () -> Unit = {},
    onPhotoClick: (Int) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color.White)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Camera button (first item, spans full cell)
            item {
                Column(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF2F2F7))
                        .clickable(onClick = onCameraClick),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE5E5EA)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Chụp ảnh",
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Chụp ảnh",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF636366),
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                }
            }

            // Photo grid (placeholders)
            items(photoPlaceholderColors.size) { index ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(photoPlaceholderColors[index])
                        .clickable { onPhotoClick(index) },
                    contentAlignment = Alignment.Center
                ) {
                    // Photo placeholder — in production, use Coil/AsyncImage
                    Text(
                        text = "📷",
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}
