package com.example.alohi.ui.screens.contacts

import android.Manifest
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.alohi.data.model.UserProfile
import com.example.alohi.ui.components.AvatarImage
import com.example.alohi.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendScreen(
    mainViewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val uiState by mainViewModel.uiState.collectAsState()
    var phoneQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val colors = com.example.alohi.ui.theme.AloHiTheme.extendedColors

    // Launchers for Contact Permission
    val contactPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val phones = getDeviceContacts(context)
            if (phones.isNotEmpty()) {
                mainViewModel.syncContacts(phones)
            }
        }
    }

    LaunchedEffect(Unit) {
        mainViewModel.clearSearchUser()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
    ) {
        // ── Gradient Top Bar ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(colors.gradientStart, colors.gradientEnd)
                    )
                )
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            TopAppBar(
                title = { Text("Thêm bạn", fontWeight = FontWeight.SemiBold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                            val phones = getDeviceContacts(context)
                            if (phones.isNotEmpty()) mainViewModel.syncContacts(phones)
                        } else {
                            contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    }) {
                        Icon(Icons.Default.Sync, contentDescription = "Đồng bộ danh bạ", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F7))
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // My QR Code (Zalo style)
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A)), // Dark blue
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .aspectRatio(0.85f),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        uiState.currentUser?.displayName ?: "AloHi User",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.size(140.dp).background(Color.White, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(100.dp), tint = Color.Black)
                        // Mock central logo
                        Box(modifier = Modifier.size(32.dp).background(Color.White, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Text("Alohi", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Quét mã để thêm bạn AloHi với tôi",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Phone Search Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("+84", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                VerticalDivider(modifier = Modifier.height(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = phoneQuery,
                    onValueChange = { phoneQuery = it.filter { ch -> ch.isDigit() }.take(15) },
                    placeholder = { Text("Nhập số điện thoại", color = Color.Gray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { mainViewModel.searchUserByPhone(phoneQuery) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { mainViewModel.searchUserByPhone(phoneQuery) },
                    modifier = Modifier.background(Color(0xFFE5E5EA), RoundedCornerShape(50))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Search", tint = Color.Gray)
                }
            }

            // Search Result Area
            if (uiState.isSearchingUser) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            uiState.searchUserError?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }

            uiState.searchResultUser?.let { user ->
                Spacer(modifier = Modifier.height(16.dp))
                SearchResultItem(
                    user = user,
                    onClick = { onNavigateToProfile(user.id) },
                    onAddFriend = { mainViewModel.sendFriendRequest(user.id) },
                    onCancelFriend = { mainViewModel.cancelFriendRequest(user.id) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Options list
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                OptionItem(icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, text = "Quét mã QR")
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                OptionItem(icon = { Icon(Icons.Outlined.AccountBox, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, text = "Bạn bè có thể quen")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Xem lời mời kết bạn đã gửi tại trang Danh bạ AloHi",
                color = Color.Gray,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SearchResultItem(
    user: UserProfile,
    onClick: () -> Unit,
    onAddFriend: () -> Unit,
    onCancelFriend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(name = user.displayName, imageUrl = user.avatar?.url, size = 48.dp)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (user.bio?.isNotBlank() == true) {
                Text(user.bio, fontSize = 13.sp, color = Color.Gray, maxLines = 1)
            }
        }
        if (user.friendStatus == "friend") {
            Text("Bạn bè", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        } else if (user.friendStatus == "sent") {
            TextButton(onClick = onCancelFriend) {
                Text("Hủy yêu cầu", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        } else {
            IconButton(onClick = onAddFriend, modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Friend", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun OptionItem(icon: @Composable () -> Unit, text: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, fontSize = 16.sp)
    }
}

// Minimal implementation to query Android Contacts
fun getDeviceContacts(context: android.content.Context): List<String> {
    val phones = mutableListOf<String>()
    val cursor = context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
        null, null, null
    )
    cursor?.use {
        val numIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        while (it.moveToNext()) {
            val phone = it.getString(numIndex).replace(Regex("[^0-9+]"), "")
            if (phone.isNotBlank()) phones.add(phone)
        }
    }
    return phones.distinct()
}
    
