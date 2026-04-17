package com.example.alohi.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.alohi.ui.screens.auth.LoginScreen
import com.example.alohi.ui.screens.auth.OtpScreen
import com.example.alohi.ui.screens.auth.RegisterScreen
import com.example.alohi.ui.screens.conversation.ConversationScreen
import com.example.alohi.ui.screens.main.MainScreen
import com.example.alohi.ui.screens.onboarding.OnboardingScreen
import com.example.alohi.ui.screens.splash.SplashScreen
import com.example.alohi.ui.viewmodel.AuthViewModel
import com.example.alohi.ui.viewmodel.MainViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
/**
 * AloHi Navigation Graph
 * Handles all navigation routes with smooth transitions
 * Shares AuthViewModel across auth screens via activity-scoped viewModel
 */
@Composable
fun AloHiNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route,
) {
    // Shared ViewModels
    val authViewModel: AuthViewModel = viewModel()
    val mainViewModel: MainViewModel = viewModel()
    val callViewModel: com.example.alohi.ui.viewmodel.CallViewModel = viewModel()
    val uiState by mainViewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    androidx.compose.runtime.LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            mainViewModel.clearSuccess()
        }
    }

    androidx.compose.runtime.LaunchedEffect(uiState.error) {
        uiState.error?.let { err ->
            android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
            mainViewModel.clearError()
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(350)
            ) + fadeIn(tween(350))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(350)
            ) + fadeOut(tween(200))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(350)
            ) + fadeIn(tween(350))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(350)
            ) + fadeOut(tween(200))
        }
    ) {
        // ═══════ SPLASH ═══════
        composable(
            route = Screen.Splash.route,
            enterTransition = { fadeIn(tween(0)) },
            exitTransition = { fadeOut(tween(500)) }
        ) {
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // ═══════ ONBOARDING ═══════
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // ═══════ LOGIN ═══════
        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToOtp = { phone ->
                    navController.navigate(Screen.Otp.createRoute(phone))
                },
                onNavigateToMain = {
                    // Handled automatically by Reactive Navigation in MainActivity
                }
            )
        }

        // ═══════ OTP ═══════
        composable(
            route = Screen.Otp.route,
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            OtpScreen(
                phone = phone,
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onVerifySuccess = {
                    // After OTP verified, go to Register screen
                    navController.navigate(Screen.Register.createRoute(phone)) {
                        popUpTo(Screen.Otp.route) { inclusive = true }
                    }
                }
            )
        }

        // ═══════ REGISTER ═══════
        composable(
            route = Screen.Register.route,
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            RegisterScreen(
                phone = phone,
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = {
                    // Handled automatically by Reactive Navigation in MainActivity
                }
            )
        }

        // ═══════ MAIN (with Bottom Nav) ═══════
        composable(
            route = Screen.Main.route,
            enterTransition = { fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(200)) }
        ) {
            androidx.compose.runtime.LaunchedEffect(Unit) {
                mainViewModel.refreshAllData()
            }
            MainScreen(
                mainViewModel = mainViewModel,
                onChatClick = { conversationId, name ->
                    navController.navigate(
                        Screen.Conversation.createRoute(conversationId, name)
                    )
                },
                onNavigateToAddFriend = {
                    navController.navigate(Screen.AddFriend.route)
                },
                onNavigateToCreateGroup = {
                    navController.navigate(Screen.CreateGroup.route)
                },
                onLogout = {
                    authViewModel.logout()
                }
            )
        }

        // ═══════ CONVERSATION ═══════
        composable(
            route = Screen.Conversation.route,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            val rawName = backStackEntry.arguments?.getString("name") ?: ""
            val name = Screen.Conversation.decodeName(rawName)
            ConversationScreen(
                conversationId = conversationId,
                partnerName = name,
                mainViewModel = mainViewModel,
                onNavigateBack = { navController.popBackStack() },
                onCallClick = {
                    // Extract recipient ID from participants if 1-on-1
                    val conv = mainViewModel.uiState.value.conversations.find { it.id == conversationId }
                    val partnerId = conv?.participants?.find { it.user?.id != mainViewModel.uiState.value.currentUser?.id }?.user?.id
                    if (partnerId != null) {
                        callViewModel.initCall(partnerId, isVideo = false)
                    } else {
                        android.widget.Toast.makeText(context, "Không thể gọi nhóm", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                onVideoCallClick = {
                    val conv = mainViewModel.uiState.value.conversations.find { it.id == conversationId }
                    val partnerId = conv?.participants?.find { it.user?.id != mainViewModel.uiState.value.currentUser?.id }?.user?.id
                    if (partnerId != null) {
                        callViewModel.initCall(partnerId, isVideo = true)
                    } else {
                        android.widget.Toast.makeText(context, "Không thể gọi nhóm", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                onNavigateToDetail = {
                    navController.navigate(Screen.ConversationDetail.createRoute(conversationId, name))
                }
            )
        }

        // ═══════ CONVERSATION DETAIL ═══════
        composable(
            route = Screen.ConversationDetail.route,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType }
            ),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            val rawName = backStackEntry.arguments?.getString("name") ?: ""
            val name = Screen.ConversationDetail.decodeName(rawName)
            com.example.alohi.ui.screens.conversation.ConversationDetailScreen(
                conversationId = conversationId,
                partnerName = name,
                mainViewModel = mainViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ═══════ ADD FRIEND ═══════
        composable(
            route = Screen.AddFriend.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
        ) {
            com.example.alohi.ui.screens.contacts.AddFriendScreen(
                mainViewModel = mainViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.UserProfile.createRoute(userId))
                }
            )
        }

        // ═══════ USER PROFILE (Other user) ═══════
        composable(
            route = Screen.UserProfile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            com.example.alohi.ui.screens.profile.UserProfileScreen(
                userId = userId,
                mainViewModel = mainViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { convoId, name ->
                    // Normally if convo isn't created we let MainViewModel handle createConversation
                    mainViewModel.createConversation(userId) { newConvoId ->
                        navController.navigate(Screen.Conversation.createRoute(newConvoId, name)) {
                            popUpTo(Screen.Main.route)
                        }
                    }
                }
            )
        }

        // ═══════ CREATE GROUP ═══════
        composable(
            route = Screen.CreateGroup.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
        ) {
            com.example.alohi.ui.screens.group.CreateGroupScreen(
                mainViewModel = mainViewModel,
                onNavigateBack = { navController.popBackStack() },
                onGroupCreated = { convoId, groupName ->
                    navController.navigate(Screen.Conversation.createRoute(convoId, groupName)) {
                        popUpTo(Screen.Main.route)
                    }
                }
            )
        }
    }
}
