package com.example.alohi.ui.screens.story

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.alohi.data.model.StoryGroup
import com.example.alohi.ui.components.AvatarImage
import com.example.alohi.ui.viewmodel.StoryViewModel

@Composable
fun StoryRow(
    storyViewModel: StoryViewModel = viewModel(),
    onNavigateToCreateStory: () -> Unit,
    onNavigateToStoryViewer: (String) -> Unit // pass author id or story id
) {
    val uiState by storyViewModel.uiState.collectAsState()

    // Refresh when first load
    LaunchedEffect(Unit) {
        storyViewModel.loadFeed()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp)
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Add native "Create Story" item
            item {
                CreateStoryItem(onClick = onNavigateToCreateStory)
            }

            // My Stories (if any)
            if (!uiState.feed?.myStories.isNullOrEmpty()) {
                val group = uiState.feed!!.myStories!![0]
                item {
                    StoryGroupItem(
                        group = group,
                        isMe = true,
                        onClick = { onNavigateToStoryViewer(group.author.id ?: "") }
                    )
                }
            }

            // Friends Stories
            if (!uiState.feed?.friendsStories.isNullOrEmpty()) {
                items(uiState.feed!!.friendsStories!!) { group ->
                    StoryGroupItem(
                        group = group,
                        isMe = false,
                        onClick = { onNavigateToStoryViewer(group.author.id ?: "") }
                    )
                }
            }
        }
    }
}

@Composable
fun CreateStoryItem(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Thêm Story",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Tin của bạn",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StoryGroupItem(group: StoryGroup, isMe: Boolean, onClick: () -> Unit) {
    val borderColor = if (group.hasUnread == true) Color(0xFF0084FF) else Color.LightGray
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .border(2.dp, borderColor, CircleShape)
                .clickable { onClick() }
                .padding(4.dp) // inner padding for ring gap
        ) {
            AvatarImage(
                imageUrl = group.author.avatar?.thumbnailUrl ?: group.author.avatar?.url ?: "",
                name = group.author.displayName ?: "User",
                size = 56.dp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isMe) "Tin của bạn" else (group.author.displayName ?: "User"),
            fontSize = 12.sp,
            fontWeight = if (group.hasUnread == true) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
