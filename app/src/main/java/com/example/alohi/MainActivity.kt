package com.example.alohi

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.alohi.data.local.TokenManager
import com.example.alohi.data.remote.ApiClient
import com.example.alohi.ui.navigation.AloHiNavGraph
import com.example.alohi.ui.theme.AloHiTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import com.example.alohi.ui.screens.call.CallScreen
import com.example.alohi.ui.viewmodel.CallViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS on Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Initialize API client with TokenManager
        val tokenManager = TokenManager(applicationContext)
        ApiClient.init(tokenManager)

        // Retrieve and register FCM token
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                android.util.Log.d("AlohiFCM", "Fetched FCM token: $token")
                
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        if (tokenManager.isLoggedIn.first()) {
                            val deviceId = tokenManager.deviceId.first() ?: java.util.UUID.randomUUID().toString()
                            val request = com.example.alohi.data.model.FcmTokenRequest(
                                token = token,
                                deviceId = deviceId,
                                deviceType = "android"
                            )
                            ApiClient.userApi.registerFcmToken(request)
                            android.util.Log.d("AlohiFCM", "Startup FCM token registered")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AlohiFCM", "Failed to register Startup FCM token", e)
                    }
                }
            }
        }

        try {
            androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.addObserver(
                androidx.lifecycle.LifecycleEventObserver { _, event ->
                    when (event) {
                        androidx.lifecycle.Lifecycle.Event.ON_START -> {
                            // App came to foreground
                            com.example.alohi.data.remote.SocketManager.connect()
                        }
                        androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                            // App went to background 
                            com.example.alohi.data.remote.SocketManager.disconnect()
                        }
                        else -> {}
                    }
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to setup ProcessLifecycleOwner", e)
        }

        enableEdgeToEdge()
        setContent {
            AloHiTheme {
                val isLoggedInFlow = remember { tokenManager.isLoggedIn }
                val isLoggedInState by isLoggedInFlow.collectAsState(initial = null)
                val context = androidx.compose.ui.platform.LocalContext.current
                
                var hasBeenLoggedIn by remember { androidx.compose.runtime.mutableStateOf(false) }
                
                LaunchedEffect(isLoggedInState) {
                    if (isLoggedInState == true) {
                        hasBeenLoggedIn = true
                    } else if (isLoggedInState == false && hasBeenLoggedIn) {
                        val intent = android.content.Intent(context, MainActivity::class.java).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        context.startActivity(intent)
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (isLoggedInState == null) {
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF5856D6)))
                    } else {
                        val navController = rememberNavController()
                        val startDest = if (isLoggedInState == true) {
                            com.example.alohi.ui.navigation.Screen.Main.route
                        } else {
                            com.example.alohi.ui.navigation.Screen.Onboarding.route
                        }
                        Box(modifier = Modifier.fillMaxSize()) {
                            AloHiNavGraph(navController = navController, startDestination = startDest)
                            
                            val callViewModel: CallViewModel = viewModel()
                            val callState by callViewModel.callState.collectAsState()
                            
                            LaunchedEffect(intent) {
                                if (intent?.getStringExtra("action") == "incoming_call") {
                                    val callId = intent.getStringExtra("callId") ?: ""
                                    val callerName = intent.getStringExtra("callerName") ?: ""
                                    val sdpOffer = intent.getStringExtra("sdpOffer") ?: ""
                                    val callType = intent.getStringExtra("type") ?: "voice"
                                    callViewModel.notifyIncomingCall(callId, callerName, sdpOffer, callType)
                                    // Clear intent action so it doesn't refire
                                    intent.removeExtra("action")
                                }
                            }

                            if (callState.isRinging || callState.isActive) {
                                CallScreen(callViewModel = callViewModel, onCallEnded = { 
                                    // Handle any cleanup here
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}