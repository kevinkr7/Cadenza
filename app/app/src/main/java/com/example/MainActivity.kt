package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.*
import com.example.ui.components.CadenzaNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AuthState
import com.example.viewmodel.VocaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fully enable transparent system status and gesture bars
        enableEdgeToEdge()

        // Hide the natural system navigation bar (Immersive Mode)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())


        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val vocaViewModel: VocaViewModel = viewModel()
                val authState by vocaViewModel.authState.collectAsStateWithLifecycle()

                // Bottom bar visibility state based on scroll
                var bottomBarVisible by remember { mutableStateOf(false) }

                val nestedScrollConnection = remember {
                    object : NestedScrollConnection {
                        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                            if (available.y < -10f) {
                                // Scrolled down -> Hide bottom bar
                                bottomBarVisible = false
                            } else if (available.y > 10f) {
                                // Scrolled up -> Show bottom bar
                                bottomBarVisible = true
                            }
                            return Offset.Zero
                        }
                    }
                }

                // Check and pipe dynamic auth status redirects
                LaunchedEffect(authState) {
                    if (authState is AuthState.SignedOut) {
                        val currentRoute = navController.currentBackStackEntry?.destination?.route
                        if (currentRoute != null && currentRoute != "splash" && currentRoute != "login") {
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize().nestedScroll(nestedScrollConnection),
                    bottomBar = { 
                        AnimatedVisibility(
                            visible = bottomBarVisible,
                            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)),
                            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300))
                        ) {
                            CadenzaNavigation(navController) 
                        }
                    },
                    containerColor = com.example.ui.theme.CadenzaOffWhite
                ) { innerPadding ->
                    // Set up modern animated type-safe routing container
                    NavHost(
                        navController = navController,
                        startDestination = "splash",
                        modifier = Modifier.fillMaxSize().padding(innerPadding)
                    ) {
                        // 1. Splash Screen
                        composable("splash") {
                            SplashScreen(
                                viewModel = vocaViewModel,
                                onNavigateNext = { destination ->
                                    navController.navigate(destination) {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 2. Tutorial Walkthrough (Only for first-time users after register/login)
                        composable("onboarding") {
                            OnboardingScreen(
                                viewModel = vocaViewModel,
                                onFinishOnboarding = {
                                    navController.navigate("dashboard") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 3. Authenticator forms
                        composable("login") {
                            AuthScreen(
                                viewModel = vocaViewModel,
                                onLoginSuccess = { isFirstTime ->
                                    val destination = if (isFirstTime) "onboarding" else "dashboard"
                                    navController.navigate(destination) {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 4. Main Hub Dashboard
                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = vocaViewModel,
                                onNavigate = { destination ->
                                    navController.navigate(destination)
                                }
                            )
                        }

                        // New: Learn Screen
                        composable("learn") {
                            LearnScreen(
                                viewModel = vocaViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // New: Community Screen
                        composable("community") {
                            CommunityScreen(
                                viewModel = vocaViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 5. Singing Oscilloscope Pitch Recorder
                        composable("singing_session") {
                            CadenzaPracticeScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 6. Conversational Companion Chat
                        composable("ai_coach") {
                            CoachChatScreen(
                                viewModel = vocaViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 7. Results feedback breakdowns
                        composable("performance_analysis") {
                            PerformanceAnalysisScreen(
                                viewModel = vocaViewModel,
                                onNavigateBack = {
                                    // Pop back to central hub dashboard directly
                                    navController.navigate("dashboard") {
                                        popUpTo("dashboard") { inclusive = false }
                                    }
                                },
                                onNavigateProgress = {
                                    navController.navigate("progress_tracking") {
                                        popUpTo("performance_analysis") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 8. Progress analytics lines
                        composable("progress_tracking") {
                            ProgressTrackingScreen(
                                viewModel = vocaViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 9. Profile controls
                        composable("profile") {
                            ProfileScreen(
                                viewModel = vocaViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateOnboard = {
                                    navController.navigate("onboarding")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
