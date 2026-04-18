package com.example.alohi.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alohi.data.remote.SocketManager
import com.example.alohi.utils.NetworkMonitor
import com.example.alohi.utils.NetworkStatus

@Composable
fun NetworkStatusBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val networkMonitor = remember { NetworkMonitor(context) }
    
    val networkStatus by networkMonitor.networkStatus.collectAsState(initial = NetworkStatus.Available)
    val socketState by SocketManager.socketState.collectAsState(initial = SocketManager.SocketState.CONNECTED)
    
    // Priority: No Internet > Connecting to Server > Disconnected Server
    val textMessage: String?
    val backgroundColor: Color
    val textColor: Color
    
    when {
        networkStatus == NetworkStatus.Unavailable -> {
            textMessage = "Không có kết nối mạng"
            backgroundColor = MaterialTheme.colorScheme.errorContainer
            textColor = MaterialTheme.colorScheme.onErrorContainer
        }
        socketState == SocketManager.SocketState.CONNECTING -> {
            textMessage = "Đang kết nối..."
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant
            textColor = MaterialTheme.colorScheme.onSurfaceVariant
        }
        socketState == SocketManager.SocketState.DISCONNECTED -> {
            textMessage = "Mất kết nối máy chủ"
            backgroundColor = MaterialTheme.colorScheme.errorContainer
            textColor = MaterialTheme.colorScheme.onErrorContainer
        }
        else -> {
            textMessage = null
            backgroundColor = Color.Transparent
            textColor = Color.Transparent
        }
    }
    
    AnimatedVisibility(
        visible = textMessage != null,
        enter = expandVertically(animationSpec = tween(150)),
        exit = shrinkVertically(animationSpec = tween(300)),
        modifier = modifier
    ) {
        if (textMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = textMessage,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
