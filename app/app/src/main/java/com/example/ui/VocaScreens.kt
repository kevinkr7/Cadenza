package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.models.AvatarPreferences
import com.example.models.ChatMessage
import com.example.models.UserSession
import com.example.ui.components.AvatarCompanion
import com.example.ui.components.neumorphic
import com.example.viewmodel.AuthState
import com.example.viewmodel.VocaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- 1. SPLASH SCREEN ---
@Composable
fun SplashScreen(
    viewModel: VocaViewModel,
    onNavigateNext: (destination: String) -> Unit
) {
    var startAnim by remember { mutableStateOf(false) }
    val scaleVal by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    LaunchedEffect(key1 = true) {
        startAnim = true
        delay(2000)
        val destination = viewModel.getInitialDestination()
        onNavigateNext(destination)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.ui.theme.CadenzaOffWhite), // Hardware surface
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(scaleVal).padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .neumorphic(isPressed = false, cornerRadius = 70.dp, elevation = 12.dp)
                    .clip(CircleShape)
                    .background(com.example.ui.theme.CadenzaWhite),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.cadenza_logo),
                    contentDescription = "Cadenza Logo",
                    modifier = Modifier.size(90.dp).clip(RoundedCornerShape(22.dp))
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "CADENZA",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = com.example.ui.theme.CadenzaTextPrimary,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Premium Vocal Analysis",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = com.example.ui.theme.CadenzaTextSecondary,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("splash_tagline")
            )
        }
    }
}

// --- 2. ONBOARDING SCREEN (First-Time User Walkthrough) ---
@Composable
fun OnboardingScreen(
    viewModel: VocaViewModel,
    onFinishOnboarding: () -> Unit
) {
    val pagerState = remember { mutableStateOf(0) }
    val onboardingData = listOf(
        Triple(
            "Precision Pitch Analysis",
            "Cadenza uses advanced DSP algorithms to track your fundamental frequency, note pitch, and vocal stability in real time.",
            Icons.Default.Mic
        ),
        Triple(
            "Personalized AI Coaching",
            "Train with Aria or Leo, your customizable AI companions powered by Gemini, for tailored vocal exercises and projection tips.",
            Icons.Default.Face
        ),
        Triple(
            "Track Your Vocal Evolution",
            "Monitor your growth with comprehensive session reports, pitch distribution charts, practice streaks, and historical metrics.",
            Icons.Default.Star
        )
    )

    fun finishWalkthrough() {
        viewModel.completeOnboarding()
        onFinishOnboarding()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.ui.theme.CadenzaOffWhite)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.cadenza_logo),
                        contentDescription = "Cadenza Logo",
                        modifier = Modifier
                            .size(40.dp)
                            .shadow(4.dp, RoundedCornerShape(10.dp), spotColor = com.example.ui.theme.CadenzaShadowDark)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Text(
                        text = "CADENZA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp,
                        color = com.example.ui.theme.CadenzaTextPrimary
                    )
                }

                TextButton(onClick = { finishWalkthrough() }) {
                    Text("SKIP", color = com.example.ui.theme.CadenzaTextSecondary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.65f),
                contentAlignment = Alignment.Center
            ) {
                val currentSlide = onboardingData[pagerState.value]
                
                com.example.ui.components.CadenzaCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        // Recessed icon area
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .neumorphic(isPressed = true, cornerRadius = 45.dp, elevation = 6.dp)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = currentSlide.third,
                                contentDescription = "Onboarding Icon",
                                tint = com.example.ui.theme.CadenzaHyperMagenta,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = currentSlide.first,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.CadenzaTextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = currentSlide.second,
                            fontSize = 14.sp,
                            color = com.example.ui.theme.CadenzaTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 0..2) {
                        val isCurrent = (i == pagerState.value)
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (isCurrent) 24.dp else 8.dp)
                                .background(
                                    color = if (isCurrent) com.example.ui.theme.CadenzaDeepSkyBlue else com.example.ui.theme.CadenzaRecessedBase,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                com.example.ui.components.CadenzaButton(
                    text = if (pagerState.value == 2) "START" else "NEXT",
                    onClick = {
                        if (pagerState.value < 2) {
                            pagerState.value += 1
                        } else {
                            finishWalkthrough()
                        }
                    },
                    modifier = Modifier.testTag("onboarding_next_button")
                )
            }
        }
    }
}

// --- 3. AUTHENTICATION MODULE (Login & Register) ---
@Composable
fun AuthScreen(
    viewModel: VocaViewModel,
    onLoginSuccess: (isFirstTime: Boolean) -> Unit
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    var isLoginTab by remember { mutableStateOf(true) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var noticeMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authState) {
        if (authState is AuthState.LoggedIn) {
            val isFirstTime = !viewModel.hasCompletedOnboarding()
            onLoginSuccess(isFirstTime)
        } else if (authState is AuthState.Error) {
            errorMessage = (authState as AuthState.Error).message
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.ui.theme.CadenzaOffWhite)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Image(
                painter = painterResource(id = R.drawable.cadenza_logo),
                contentDescription = "Cadenza Logo",
                modifier = Modifier
                    .size(80.dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = com.example.ui.theme.CadenzaShadowDark)
                    .clip(RoundedCornerShape(20.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "CADENZA",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = com.example.ui.theme.CadenzaTextPrimary,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isLoginTab) "Welcome back! Sign in to train." else "Create your account & unlock your voice",
                fontSize = 14.sp,
                color = com.example.ui.theme.CadenzaTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Hardware Toggle Tab
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(com.example.ui.theme.CadenzaRecessedBase, shape = RoundedCornerShape(14.dp))
                    .border(1.dp, com.example.ui.theme.CadenzaShadowDark.copy(alpha=0.3f), RoundedCornerShape(14.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            isLoginTab = true
                            errorMessage = null
                            noticeMessage = null
                            viewModel.clearAuthError()
                        }
                        .background(if (isLoginTab) com.example.ui.theme.CadenzaWhite else Color.Transparent)
                        .padding(vertical = 12.dp)
                        .then(if (isLoginTab) Modifier.shadow(2.dp, RoundedCornerShape(10.dp), spotColor = com.example.ui.theme.CadenzaShadowDark) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SIGN IN", color = if (isLoginTab) com.example.ui.theme.CadenzaHyperMagenta else com.example.ui.theme.CadenzaTextSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            isLoginTab = false
                            errorMessage = null
                            noticeMessage = null
                            viewModel.clearAuthError()
                        }
                        .background(if (!isLoginTab) com.example.ui.theme.CadenzaWhite else Color.Transparent)
                        .padding(vertical = 12.dp)
                        .then(if (!isLoginTab) Modifier.shadow(2.dp, RoundedCornerShape(10.dp), spotColor = com.example.ui.theme.CadenzaShadowDark) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    Text("REGISTER", color = if (!isLoginTab) com.example.ui.theme.CadenzaHyperMagenta else com.example.ui.theme.CadenzaTextSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (errorMessage != null) {
                Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(bottom = 16.dp))
            }
            if (noticeMessage != null) {
                Text(noticeMessage ?: "", color = com.example.ui.theme.CadenzaDeepSkyBlue, fontSize = 13.sp, modifier = Modifier.padding(bottom = 16.dp))
            }

            com.example.ui.components.CadenzaCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    if (!isLoginTab) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; errorMessage = null },
                            label = { Text("Artist Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = com.example.ui.theme.CadenzaDeepSkyBlue) },
                            modifier = Modifier.fillMaxWidth().testTag("name_field"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = com.example.ui.theme.CadenzaRecessedBase,
                                unfocusedContainerColor = com.example.ui.theme.CadenzaRecessedBase,
                                focusedBorderColor = com.example.ui.theme.CadenzaHyperMagenta,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = com.example.ui.theme.CadenzaDeepSkyBlue) },
                        modifier = Modifier.fillMaxWidth().testTag("email_field"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = com.example.ui.theme.CadenzaRecessedBase,
                            unfocusedContainerColor = com.example.ui.theme.CadenzaRecessedBase,
                            focusedBorderColor = com.example.ui.theme.CadenzaHyperMagenta,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = com.example.ui.theme.CadenzaDeepSkyBlue) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password",
                                    tint = com.example.ui.theme.CadenzaTextSecondary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("password_field"),
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = com.example.ui.theme.CadenzaRecessedBase,
                            unfocusedContainerColor = com.example.ui.theme.CadenzaRecessedBase,
                            focusedBorderColor = com.example.ui.theme.CadenzaHyperMagenta,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )

                    if (isLoginTab) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { noticeMessage = viewModel.doForgotPassword(email) }) {
                                Text("Forgot Password?", fontSize = 12.sp, color = com.example.ui.theme.CadenzaTextSecondary)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    com.example.ui.components.CadenzaButton(
                        text = if (isLoginTab) "SIGN IN" else "CREATE ACCOUNT",
                        onClick = {
                            errorMessage = null
                            noticeMessage = null
                            if (isLoginTab) {
                                viewModel.doLogin(email, password) { onLoginSuccess(it) }
                            } else {
                                viewModel.doRegister(email, name, password) { onLoginSuccess(it) }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("auth_submit_button"),
                        isLoading = authState is AuthState.Loading,
                        color = com.example.ui.theme.CadenzaHyperMagenta
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(" OR ", fontSize = 12.sp, color = com.example.ui.theme.CadenzaTextSecondary, modifier = Modifier.padding(horizontal = 8.dp))

            Spacer(modifier = Modifier.height(20.dp))

            com.example.ui.components.CadenzaButton(
                text = "CONTINUE WITH GOOGLE",
                onClick = { viewModel.doGoogleSignIn { onLoginSuccess(it) } },
                modifier = Modifier.fillMaxWidth(),
                color = com.example.ui.theme.CadenzaDeepSkyBlue
            )
        }
    }
}

// --- 4. HOME DASHBOARD SCREEN ---
@Composable
fun DashboardScreen(
    viewModel: VocaViewModel,
    onNavigate: (String) -> Unit
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()

    val userName = when (val auth = authState) {
        is AuthState.LoggedIn -> auth.displayName
        else -> "Kecin"
    }

    // Calculated metrics
    val totalSessionsCount = sessions.size
    val averageScore = if (sessions.isNotEmpty()) sessions.map { it.overallScore }.average().toInt() else 0
    val highestScore = if (sessions.isNotEmpty()) sessions.maxOf { it.overallScore } else 0
    val improvementPercent = if (sessions.size >= 2) {
        val last = sessions.first().overallScore
        val prev = sessions[1].overallScore
        val diff = last - prev
        if (prev != 0) ((diff.toFloat() / prev) * 100).toInt() else 0
    } else {
        0
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.ui.theme.CadenzaBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Profile Avatar
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .neumorphic(isPressed = false, cornerRadius = 18.dp, elevation = 6.dp)
                            .clip(RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = com.example.ui.theme.CadenzaSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "WELCOME BACK",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = com.example.ui.theme.CadenzaTextSecondary
                        )
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = com.example.ui.theme.CadenzaTextPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Ready to sing?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = com.example.ui.theme.CadenzaSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Day Streak Pill
                Box(
                    modifier = Modifier
                        .height(60.dp)
                        .width(90.dp)
                        .neumorphic(isPressed = false, cornerRadius = 16.dp, elevation = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = "Streak", tint = com.example.ui.theme.CadenzaPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("3", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = com.example.ui.theme.CadenzaTextPrimary)
                        }
                        Text("DAY STREAK", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.CadenzaTextSecondary, letterSpacing = 0.5.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // --- AI COMPANION BANNER ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(isPressed = false, cornerRadius = 20.dp, elevation = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    // Sparkle in recessed hole
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .neumorphic(isPressed = true, cornerRadius = 12.dp, elevation = 6.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = com.example.ui.theme.CadenzaPrimary, modifier = Modifier.size(24.dp))
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ready when you are.", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = com.example.ui.theme.CadenzaTextPrimary)
                        Text("Let's make today's session count.", style = MaterialTheme.typography.bodySmall, color = com.example.ui.theme.CadenzaTextSecondary)
                    }
                    
                    // Large robot face filler
                    Icon(Icons.Default.Face, contentDescription = "Robot", tint = com.example.ui.theme.CadenzaSecondary, modifier = Modifier.size(48.dp))
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // --- SESSION ACTION ---
            com.example.ui.components.CadenzaCard(
                modifier = Modifier.fillMaxWidth().height(260.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = com.example.ui.theme.CadenzaPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("YOUR SESSION", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = com.example.ui.theme.CadenzaPrimary, letterSpacing = 1.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).fillMaxWidth()) {
                        // Waveform behind
                        com.example.ui.components.CadenzaWaveform(modifier = Modifier.fillMaxSize())
                        
                        // New Custom Dial with Tick Marks
                        com.example.ui.components.CadenzaSessionDial(
                            onClick = { onNavigate("singing_session") },
                            modifier = Modifier.testTag("action_session")
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Start a vocal session", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = com.example.ui.theme.CadenzaTextPrimary)
                    Text("Pitch & note analysis", fontSize = 14.sp, color = com.example.ui.theme.CadenzaTextSecondary)
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))

            // --- VOCAL METRICS ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BarChart, contentDescription = null, tint = com.example.ui.theme.CadenzaPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("VOCAL METRICS", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = com.example.ui.theme.CadenzaPrimary, letterSpacing = 1.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 2x2 Grid
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    NeumorphicStatWidget(
                        icon = Icons.Default.CalendarToday,
                        title = "Sessions",
                        value = String.format("%02d", totalSessionsCount),
                        subtitle = "Total sessions",
                        modifier = Modifier.weight(1f)
                    )
                    NeumorphicStatWidget(
                        icon = Icons.Default.Star,
                        title = "Avg Score",
                        value = if (averageScore > 0) averageScore.toString() else "--",
                        subtitle = "Average performance",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    NeumorphicStatWidget(
                        icon = Icons.Default.EmojiEvents,
                        title = "High Score",
                        value = if (highestScore > 0) highestScore.toString() else "--",
                        subtitle = "Your best performance",
                        modifier = Modifier.weight(1f)
                    )
                    NeumorphicStatWidget(
                        icon = Icons.Default.TrendingUp,
                        title = "Improvement",
                        value = if (improvementPercent >= 0) "+${improvementPercent}%" else "${improvementPercent}%",
                        subtitle = "From last 7 days",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Add padding for bottom navigation
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun NeumorphicStatWidget(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    com.example.ui.components.CadenzaCard(modifier = modifier.height(100.dp)) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .neumorphic(isPressed = true, cornerRadius = 12.dp, elevation = 4.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = com.example.ui.theme.CadenzaPrimary, modifier = Modifier.size(20.dp))
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(verticalArrangement = Arrangement.Center) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = com.example.ui.theme.CadenzaTextPrimary)
                Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = com.example.ui.theme.CadenzaPrimary)
                Text(subtitle, fontSize = 10.sp, color = com.example.ui.theme.CadenzaTextSecondary)
            }
        }
    }
}

// --- 5. SINGING SESSION SCREEN (Audio Tracking Visualizer) ---
@Composable
fun SingingSessionScreen(
    viewModel: VocaViewModel,
    onNavigateBack: () -> Unit,
    onNavigateAnalysis: () -> Unit
) {
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val recordingDurationSec by viewModel.recordingDurationSec.collectAsStateWithLifecycle()
    val activePitchFreqHz by viewModel.activePitchFreqHz.collectAsStateWithLifecycle()
    val activePitchNote by viewModel.activePitchNote.collectAsStateWithLifecycle()
    val pitchStability by viewModel.pitchStability.collectAsStateWithLifecycle()
    val vocalAccuracy by viewModel.vocalAccuracy.collectAsStateWithLifecycle()
    val pitchPoints by viewModel.pitchPoints.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()

    // Format duration
    val minutes = recordingDurationSec / 60
    val seconds = recordingDurationSec % 60
    val timeLabel = String.format("%02d:%02d", minutes, seconds)

    // Watch analysis trigger inside ViewModel
    LaunchedEffect(isAnalyzing) {
        if (!isRecording && !isAnalyzing && viewModel.lastSessionResult.value != null) {
            onNavigateAnalysis()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.ui.theme.CadenzaOffWhite)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        if (isAnalyzing) {
            // Processing state (Hardware loading)
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                com.example.ui.components.CadenzaDisplay(modifier = Modifier.size(140.dp)) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp), color = com.example.ui.theme.CadenzaDeepSkyBlue, strokeWidth = 6.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("PROCESSING", fontSize = 12.sp, color = com.example.ui.theme.CadenzaDisplayGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Back header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.example.ui.components.CadenzaButton(
                        text = "BACK",
                        onClick = onNavigateBack,
                        modifier = Modifier.width(80.dp).height(36.dp)
                    )
                    Text("ACTIVE CORE RECON", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = com.example.ui.theme.CadenzaTextSecondary, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.width(80.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Real-time Pitch visualizer graph canvas card
                com.example.ui.components.CadenzaCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.1f)
                ) {
                    Column(modifier = Modifier.padding(20.dp).fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("OSCILLOSCOPE", color = com.example.ui.theme.CadenzaTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            if (isRecording) com.example.ui.theme.CadenzaHyperMagenta else com.example.ui.theme.CadenzaTextSecondary.copy(alpha=0.5f),
                                            shape = CircleShape
                                        )
                                        .border(1.dp, com.example.ui.theme.CadenzaShadowDark, CircleShape)
                                        .then(if (isRecording) Modifier.shadow(4.dp, CircleShape, spotColor = com.example.ui.theme.CadenzaHyperMagenta) else Modifier)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (isRecording) "LIVE" else "STBY",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRecording) com.example.ui.theme.CadenzaHyperMagenta else com.example.ui.theme.CadenzaTextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom drawing of visual tracking waveforms
                        com.example.ui.components.CadenzaDisplay(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            PitchGraphCanvas(
                                points = pitchPoints,
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                isRecording = isRecording
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Track and metrics indicators panel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    com.example.ui.components.CadenzaCard(modifier = Modifier.weight(1f)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                            Text("NOTE", fontSize = 10.sp, color = com.example.ui.theme.CadenzaTextSecondary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            com.example.ui.components.CadenzaDisplay(modifier = Modifier.fillMaxWidth()) {
                                Text(activePitchNote, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.CadenzaDisplayGreen, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    com.example.ui.components.CadenzaCard(modifier = Modifier.weight(1f)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                            Text("FREQ (Hz)", fontSize = 10.sp, color = com.example.ui.theme.CadenzaTextSecondary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            com.example.ui.components.CadenzaDisplay(modifier = Modifier.fillMaxWidth()) {
                                Text(if (activePitchFreqHz > 0) "${activePitchFreqHz.toInt()}" else "0", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.CadenzaDisplayGreen, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    com.example.ui.components.CadenzaCard(modifier = Modifier.weight(1f)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                            Text("STAB %", fontSize = 10.sp, color = com.example.ui.theme.CadenzaTextSecondary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            com.example.ui.components.CadenzaDisplay(modifier = Modifier.fillMaxWidth()) {
                                Text(if (pitchStability > 0) "$pitchStability" else "0", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.CadenzaDisplayGreen, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(0.1f))

                // Hardware Microphone Controller
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .neumorphic(isPressed = false, cornerRadius = 50.dp, elevation = 8.dp)
                            .clip(CircleShape)
                            .background(com.example.ui.theme.CadenzaWhite)
                            .clickable { viewModel.toggleRecording() }
                            .testTag("record_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRecording) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(com.example.ui.theme.CadenzaHyperMagenta, shape = RoundedCornerShape(6.dp))
                                    .shadow(6.dp, spotColor = com.example.ui.theme.CadenzaHyperMagenta)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Start",
                                tint = com.example.ui.theme.CadenzaHyperMagenta,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                com.example.ui.components.CadenzaDisplay(modifier = Modifier.wrapContentWidth()) {
                    Text(
                        text = if (isRecording) timeLabel else "00:00",
                        fontSize = 24.sp,
                        color = com.example.ui.theme.CadenzaDisplayGreen,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isRecording && recordingDurationSec > 0) {
                    Text(
                        "PROCESS ENGAGED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.CadenzaHyperMagenta,
                        letterSpacing = 1.sp
                    )
                } else {
                    Text(
                        "MAINTAIN STABLE AIRFLOW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.CadenzaTextSecondary,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

// Draw the Custom Animated Pitch-Time waveform on Android
@Composable
fun PitchGraphCanvas(
    points: List<Float>,
    modifier: Modifier = Modifier,
    isRecording: Boolean
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Hardware grid lines
        val octaves = listOf(0.2f, 0.4f, 0.6f, 0.8f)
        
        octaves.forEach { factor ->
            val y = height * factor
            drawLine(
                color = com.example.ui.theme.CadenzaDisplayGreen.copy(alpha = 0.2f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        if (points.isNotEmpty()) {
            val stepX = width / 26f
            val path = Path()
            
            val rangeMin = 100f
            val rangeMax = 600f

            points.forEachIndexed { index, freq ->
                val x = index * stepX
                val normalized = ((freq - rangeMin) / (rangeMax - rangeMin)).coerceIn(0f, 1f)
                val y = height - (normalized * height)

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = if (isRecording) com.example.ui.theme.CadenzaDisplayGreen else com.example.ui.theme.CadenzaDisplayGreen.copy(alpha = 0.5f),
                style = Stroke(
                    width = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )

            if (isRecording) {
                val lastFreq = points.last()
                val lastX = (points.size - 1) * stepX
                val lastNorm = ((lastFreq - rangeMin) / (rangeMax - rangeMin)).coerceIn(0f, 1f)
                val lastY = height - (lastNorm * height)

                drawCircle(
                    color = com.example.ui.theme.CadenzaDisplayGreen,
                    radius = 6f,
                    center = Offset(lastX, lastY)
                )
            }
        } else {
            // Draw static hardware baseline
            val path = Path()
            val stepSize = width / 100f
            for (i in 0..100) {
                val x = i * stepSize
                val angle = (i / 100f) * Math.PI * 4
                val y = height / 2f + (Math.sin(angle) * 20f).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = com.example.ui.theme.CadenzaDisplayGreen.copy(alpha = 0.15f),
                style = Stroke(width = 2f)
            )
        }
    }
}

// --- 6. AI COACH CHAT SCREEN (Active Companion & Chat) ---
@Composable
fun CoachChatScreen(
    viewModel: VocaViewModel,
    onNavigateBack: () -> Unit
) {
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val avatarConfig by viewModel.avatarConfig.collectAsStateWithLifecycle()
    val isAILoading by viewModel.isAILoading.collectAsStateWithLifecycle()
    val isRecordingState by viewModel.isRecording.collectAsStateWithLifecycle()

    var chatText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Configuration Sheet Toggle
    var showConfigSheet by remember { mutableStateOf(false) }

    // Scroll to latest message on updates
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.ui.theme.CadenzaOffWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Coach Profile header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(com.example.ui.theme.CadenzaPanelLight)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .shadow(4.dp, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp), spotColor = com.example.ui.theme.CadenzaShadowDark),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    com.example.ui.components.CadenzaButton(
                        text = "BACK",
                        onClick = onNavigateBack,
                        modifier = Modifier.width(70.dp).height(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        val coachName = if (avatarConfig.gender == "female") "ARIA" else "LEO"
                        Text(coachName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.CadenzaTextPrimary, letterSpacing = 1.sp)
                        Text("Active Vocal Companion", fontSize = 11.sp, color = com.example.ui.theme.CadenzaTextSecondary)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { showConfigSheet = true }) {
                        Icon(Icons.Default.Settings, "Customize Avatar", tint = com.example.ui.theme.CadenzaTextSecondary)
                    }
                    IconButton(onClick = { viewModel.clearChatHistory() }) {
                        Icon(Icons.Default.Delete, "Clear Chat", tint = com.example.ui.theme.CadenzaTextSecondary)
                    }
                }
            }

            // Customizable Dynamic Companion Visualizer Panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .neumorphic(isPressed = true, cornerRadius = 80.dp, elevation = 6.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AvatarCompanion(
                        prefs = avatarConfig,
                        isListening = isAILoading || isRecordingState,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Scrollable Chat area (LazyColumn)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(chatHistory) { message ->
                    ChatMessageBubble(message = message)
                }

                if (isAILoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = com.example.ui.theme.CadenzaDeepSkyBlue
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Companion is translating...", color = com.example.ui.theme.CadenzaTextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Quick suggestion chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val prompts = listOf(
                    "Give me warm ups" to "Could you teach me a 30-second breathing exercise for diaphragm support?",
                    "How to hit high notes?" to "What tricks can I use to sing high head voice notes confidently?",
                    "Dry throat tips" to "What's the best way to relax my throat and voice when it feels dry?",
                    "Post-singing cooling" to "Describe some quick post-singing cooling down tips."
                )
                prompts.forEach { pair ->
                    com.example.ui.components.CadenzaCard(
                        modifier = Modifier.clickable { viewModel.sendChatMessage(pair.second) }.padding(0.dp)
                    ) {
                        Text(pair.first, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.CadenzaTextSecondary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }
            }

            // TextInput bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(com.example.ui.theme.CadenzaPanelLight)
                    .padding(12.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = chatText,
                    onValueChange = { chatText = it },
                    placeholder = { Text("Speak to your companion...", fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = com.example.ui.theme.CadenzaRecessedBase,
                        unfocusedContainerColor = com.example.ui.theme.CadenzaRecessedBase,
                        focusedBorderColor = com.example.ui.theme.CadenzaHyperMagenta,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.width(12.dp))

                com.example.ui.components.CadenzaButton(
                    text = "SEND",
                    onClick = {
                        if (chatText.isNotBlank()) {
                            viewModel.sendChatMessage(chatText)
                            chatText = ""
                        }
                    },
                    modifier = Modifier.height(48.dp).width(80.dp).testTag("chat_send_button"),
                    color = com.example.ui.theme.CadenzaDeepSkyBlue
                )
            }
        }

        // Avatar configuration modal dialogue sheet if toggled
        if (showConfigSheet) {
            AvatarCustomizerDialog(
                currentPrefs = avatarConfig,
                onDismiss = { showConfigSheet = false },
                onSave = { gender, hair, outfit, theme ->
                    viewModel.updateAvatar(gender, hair, outfit, theme)
                    showConfigSheet = false
                }
            )
        }
    }
}

// Chat bubble Composable
@Composable
fun ChatMessageBubble(message: ChatMessage) {
    val isUser = message.sender == "user"
    val systemAlert = message.messageText.startsWith("System:")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (systemAlert) {
            com.example.ui.components.CadenzaDisplay(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp).fillMaxWidth()
            ) {
                Text(
                    text = message.messageText,
                    fontSize = 11.sp,
                    color = com.example.ui.theme.CadenzaDisplayGreen,
                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            com.example.ui.components.CadenzaCard(
                modifier = Modifier.widthIn(max = 290.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                    Text(
                        text = if (isUser) "YOU" else "COACH",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) com.example.ui.theme.CadenzaHyperMagenta else com.example.ui.theme.CadenzaDeepSkyBlue,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = message.messageText,
                        fontSize = 14.sp,
                        color = com.example.ui.theme.CadenzaTextPrimary,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

// Dialog for Customizing Assistant Companion
@Composable
fun AvatarCustomizerDialog(
    currentPrefs: AvatarPreferences,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var gender by remember { mutableStateOf(currentPrefs.gender) }
    var hair by remember { mutableStateOf(currentPrefs.hairstyle) }
    var outfit by remember { mutableStateOf(currentPrefs.outfit) }
    var theme by remember { mutableStateOf(currentPrefs.colorTheme) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("CONFIGURE COMPANION", fontWeight = FontWeight.Bold, color = com.example.ui.theme.CadenzaTextPrimary, fontSize = 16.sp, letterSpacing = 1.sp) },
        containerColor = com.example.ui.theme.CadenzaPanelLight,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Gender Selection
                Column {
                    Text("PERSONA TYPE", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = com.example.ui.theme.CadenzaTextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        listOf("female" to "Aria", "male" to "Leo").forEach { item ->
                            val selected = gender == item.first
                            com.example.ui.components.CadenzaButton(
                                text = item.second,
                                onClick = { gender = item.first },
                                color = if (selected) com.example.ui.theme.CadenzaDeepSkyBlue else com.example.ui.theme.CadenzaTextSecondary,
                                modifier = Modifier.height(36.dp)
                            )
                        }
                    }
                }

                // Hairstyle Selection
                Column {
                    Text("HAIRSTYLE", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = com.example.ui.theme.CadenzaTextSecondary)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("classic" to "Classic", "vibrant" to "Vibrant", "modern" to "Sleek", "retro" to "Retro").forEach { item ->
                            val selected = hair == item.first
                            com.example.ui.components.CadenzaButton(
                                text = item.second,
                                onClick = { hair = item.first },
                                color = if (selected) com.example.ui.theme.CadenzaDeepSkyBlue else com.example.ui.theme.CadenzaTextSecondary,
                                modifier = Modifier.height(36.dp)
                            )
                        }
                    }
                }

                // Outfit Selection
                Column {
                    Text("OUTFIT STYLE", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = com.example.ui.theme.CadenzaTextSecondary)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("casual" to "Casual", "formal" to "Formal", "studio" to "Studio", "rockstar" to "Rockstar").forEach { item ->
                            val selected = outfit == item.first
                            com.example.ui.components.CadenzaButton(
                                text = item.second,
                                onClick = { outfit = item.first },
                                color = if (selected) com.example.ui.theme.CadenzaDeepSkyBlue else com.example.ui.theme.CadenzaTextSecondary,
                                modifier = Modifier.height(36.dp)
                            )
                        }
                    }
                }

                // Accent Theme selected
                Column {
                    Text("ACCENT COLOR", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = com.example.ui.theme.CadenzaTextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        listOf("teal" to "Mint", "purple" to "Amethyst", "coral" to "Coral", "indigo" to "Indigo").forEach { item ->
                            val selected = theme == item.first
                            com.example.ui.components.CadenzaButton(
                                text = item.second,
                                onClick = { theme = item.first },
                                color = if (selected) com.example.ui.theme.CadenzaDeepSkyBlue else com.example.ui.theme.CadenzaTextSecondary,
                                modifier = Modifier.height(36.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            com.example.ui.components.CadenzaButton(onClick = { onSave(gender, hair, outfit, theme) }, text = "APPLY", color = com.example.ui.theme.CadenzaHyperMagenta)
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = com.example.ui.theme.CadenzaTextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

// --- 7. PERFORMANCE ANALYSIS SCREEN (Detail metrics of last run) ---
@Composable
fun PerformanceAnalysisScreen(
    viewModel: VocaViewModel,
    onNavigateBack: () -> Unit,
    onNavigateProgress: () -> Unit
) {
    val lastSession by viewModel.lastSessionResult.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.ui.theme.CadenzaOffWhite)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.example.ui.components.CadenzaButton(
                    text = "CLOSE",
                    onClick = onNavigateBack,
                    modifier = Modifier.width(80.dp).height(36.dp)
                )
                Text("SESSION REPORT", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = com.example.ui.theme.CadenzaTextPrimary, letterSpacing = 1.sp)
                com.example.ui.components.CadenzaButton(
                    text = "LOGS",
                    onClick = onNavigateProgress,
                    modifier = Modifier.width(80.dp).height(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (lastSession == null) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Warning, "No Run", tint = com.example.ui.theme.CadenzaTextSecondary, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("NO SESSION DATA", color = com.example.ui.theme.CadenzaTextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    com.example.ui.components.CadenzaButton(text = "RETURN", onClick = onNavigateBack, modifier = Modifier.height(48.dp).width(120.dp))
                }
            } else {
                val session = lastSession!!

                // Large overall ring dashboard
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
                    com.example.ui.components.CadenzaScoreGauge(
                        score = session.overallScore,
                        modifier = Modifier.fillMaxSize()
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${session.overallScore}",
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.CadenzaTextPrimary
                        )
                        Text(
                            text = "OVERALL ACCURACY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.CadenzaTextSecondary,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Breakdown metrics Cards list
                com.example.ui.components.CadenzaCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text("METRIC BREAKDOWN", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.CadenzaTextPrimary, letterSpacing = 1.sp)

                        // 1. Pitch Cent Metric
                        ScoreProgressSegment(
                            title = "Pitch Center Fit",
                            percent = session.pitchAccuracy,
                            color = com.example.ui.theme.CadenzaHyperMagenta
                        )

                        // 2. Timing/Duration Metric
                        ScoreProgressSegment(
                            title = "Timing & Duration",
                            percent = session.timingAccuracy,
                            color = com.example.ui.theme.CadenzaDeepSkyBlue
                        )

                        // 3. Stabilization Wave index
                        ScoreProgressSegment(
                            title = "Vibrato Stability",
                            percent = session.vocalStability,
                            color = com.example.ui.theme.CadenzaDisplayGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Educational Companion Advice Suggestions
                com.example.ui.components.CadenzaCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .neumorphic(isPressed = true, cornerRadius = 12.dp, elevation = 4.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, "Coach Insight", tint = com.example.ui.theme.CadenzaHyperMagenta, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("COACH INSIGHT", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = com.example.ui.theme.CadenzaTextPrimary, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            com.example.ui.components.CadenzaDisplay(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = session.feedbackSuggestion,
                                    fontSize = 12.sp,
                                    color = com.example.ui.theme.CadenzaDisplayGreen,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                com.example.ui.components.CadenzaButton(
                    text = "RETURN TO HUB",
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                )
            }
        }
    }
}

// Progress Bar with dynamic slider
@Composable
fun ScoreProgressSegment(
    title: String,
    percent: Int,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.CadenzaTextSecondary)
            Text("$percent%", fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(com.example.ui.theme.CadenzaRecessedBase, RoundedCornerShape(6.dp))
                .border(1.dp, com.example.ui.theme.CadenzaShadowDark, RoundedCornerShape(6.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percent / 100f)
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(6.dp))
                    .shadow(4.dp, spotColor = color)
            )
        }
    }
}

// Custom Draw Circle Ring - Redesigned to use CadenzaScoreGauge directly
@Composable
fun CircularIndicatorProgressWheel(
    value: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    com.example.ui.components.CadenzaScoreGauge(score = (value * 100).toInt(), modifier = modifier)
}

// --- 8. PROGRESS TRACKING SCREEN (Historical logs & nice multi-day chart) ---
@Composable
fun LearnScreen(
    viewModel: VocaViewModel,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Vocal Lessons & Theory", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Coming Soon: Interactive Breathing & Pitch theory", color = Color.Gray)
        }
    }
}

@Composable
fun CommunityScreen(
    viewModel: VocaViewModel,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Singer's Community Hub", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Coming Soon: Share your sessions & Get feedback", color = Color.Gray)
        }
    }
}

// --- 8. PROGRESS TRACKING SCREEN (Historical logs & nice multi-day chart) ---
@Composable
fun ProgressTrackingScreen(
    viewModel: VocaViewModel,
    onNavigateBack: () -> Unit
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.ui.theme.CadenzaOffWhite)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.example.ui.components.CadenzaButton(
                    text = "BACK",
                    onClick = onNavigateBack,
                    modifier = Modifier.width(80.dp).height(36.dp)
                )
                Text("ACTIVITY LOG", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = com.example.ui.theme.CadenzaTextPrimary, letterSpacing = 1.sp)
                com.example.ui.components.CadenzaButton(
                    text = "FLUSH",
                    onClick = { viewModel.clearAllSessionHistory() },
                    modifier = Modifier.width(80.dp).height(36.dp),
                    color = com.example.ui.theme.CadenzaDeepSkyBlue
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (sessions.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Star, "Empty Charts", tint = com.example.ui.theme.CadenzaTextSecondary, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("NO LOG DATA", color = com.example.ui.theme.CadenzaTextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Complete training runs to plot coordinates.", color = com.example.ui.theme.CadenzaTextSecondary, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Score chart card
                    item {
                        com.example.ui.components.CadenzaCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("WEEKLY PERFORMANCE", fontWeight = FontWeight.Bold, color = com.example.ui.theme.CadenzaTextPrimary, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Historical metrics (Scale 0-100%)", fontSize = 11.sp, color = com.example.ui.theme.CadenzaTextSecondary)
                                
                                Spacer(modifier = Modifier.height(24.dp))

                                // Render nice custom bar charts using Canvas!
                                com.example.ui.components.CadenzaDisplay(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                ) {
                                    AnalyticsBarChart(
                                        sessions = sessions,
                                        modifier = Modifier.fillMaxSize().padding(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Stat Row
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                            com.example.ui.components.CadenzaCard(modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("LONGEST RUN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.CadenzaTextSecondary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val longestTime = sessions.maxOfOrNull { it.durationSec } ?: 0
                                    com.example.ui.components.CadenzaDisplay(modifier = Modifier.fillMaxWidth()) {
                                        Text("${longestTime}s", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.CadenzaDisplayGreen, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                            com.example.ui.components.CadenzaCard(modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("PEAK SCORE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.CadenzaTextSecondary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val peakVal = sessions.maxOfOrNull { it.overallScore } ?: 0
                                    com.example.ui.components.CadenzaDisplay(modifier = Modifier.fillMaxWidth()) {
                                        Text("${peakVal}%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.CadenzaDisplayGreen, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                        }
                    }

                    // Logs heading list
                    item {
                        Text("DATALOG ENTRIES", fontWeight = FontWeight.Bold, color = com.example.ui.theme.CadenzaTextPrimary, fontSize = 13.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 16.dp))
                    }

                    items(sessions) { run ->
                        HistoryRowItem(run = run)
                    }
                }
            }
        }
    }
}

// Canvas drawn multi bar analytics graph
@Composable
fun AnalyticsBarChart(
    sessions: List<UserSession>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val maxSessions = 6
        val dataList = sessions.takeLast(maxSessions).asReversed()

        if (dataList.isNotEmpty()) {
            val totalBars = dataList.size
            val spacingFactor = 0.35f
            val barWidth = width / (totalBars + (totalBars - 1) * spacingFactor)
            val barSpacing = barWidth * spacingFactor

            // Draw baseline coordinate line (LCD grid style)
            drawLine(
                color = com.example.ui.theme.CadenzaDisplayGreen.copy(alpha = 0.2f),
                start = Offset(0f, height - 12f),
                end = Offset(width, height - 12f),
                strokeWidth = 2f
            )

            dataList.forEachIndexed { i, session ->
                val x = i * (barWidth + barSpacing)
                val barHeight = ((session.overallScore / 100f) * (height - 30f)).coerceAtLeast(10f)
                val topLeftY = height - 12f - barHeight

                // Draw LCD bars
                drawRect(
                    color = com.example.ui.theme.CadenzaDisplayGreen,
                    topLeft = Offset(x, topLeftY),
                    size = Size(barWidth, barHeight)
                )

                // Render value labels above columns
                drawCircle(
                    color = com.example.ui.theme.CadenzaDisplayGreen.copy(alpha = 0.5f),
                    radius = 3f,
                    center = Offset(x + barWidth / 2f, topLeftY - 6f)
                )
            }
        }
    }
}

// History individual list card cell
@Composable
fun HistoryRowItem(run: UserSession) {
    com.example.ui.components.CadenzaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .neumorphic(isPressed = true, cornerRadius = 10.dp, elevation = 3.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = com.example.ui.theme.CadenzaHyperMagenta, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(run.songTitle, fontWeight = FontWeight.Bold, color = com.example.ui.theme.CadenzaTextPrimary, fontSize = 14.sp)
                    Text("Duration: ${run.durationSec}s | Notes matched: ${run.detectedNotes}", fontSize = 11.sp, color = com.example.ui.theme.CadenzaTextSecondary)
                }
            }

            // Score tag pill (LCD display)
            com.example.ui.components.CadenzaDisplay() {
                Text("${run.overallScore}%", fontWeight = FontWeight.Bold, color = com.example.ui.theme.CadenzaDisplayGreen, fontSize = 13.sp)
            }
        }
    }
}

// --- 9. PROFILE & VOCAL CONFIGURATION SCREEN ---
@Composable
fun ProfileScreen(
    viewModel: VocaViewModel,
    onNavigateBack: () -> Unit,
    onNavigateOnboard: () -> Unit
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    var skillLevel by remember { mutableStateOf("Intermediate") }
    var vocalClassification by remember { mutableStateOf("Tenor") }

    val userEmail = when (val auth = authState) {
        is AuthState.LoggedIn -> auth.email
        else -> "singer@vocaai.app"
    }
    val userName = when (val auth = authState) {
        is AuthState.LoggedIn -> auth.displayName
        else -> "Vocal Artist"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.ui.theme.CadenzaOffWhite)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.example.ui.components.CadenzaButton(
                    text = "BACK",
                    onClick = onNavigateBack,
                    modifier = Modifier.width(70.dp).height(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text("USER PREFERENCES", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = com.example.ui.theme.CadenzaTextPrimary, letterSpacing = 1.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Profile info box
            com.example.ui.components.CadenzaCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                com.example.ui.theme.CadenzaRecessedBase,
                                shape = CircleShape
                            )
                            .border(2.dp, com.example.ui.theme.CadenzaShadowDark, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.take(1).uppercase(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.CadenzaHyperMagenta
                        )
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(userName, fontWeight = FontWeight.Bold, color = com.example.ui.theme.CadenzaTextPrimary, fontSize = 16.sp)
                        Text(userEmail, fontSize = 12.sp, color = com.example.ui.theme.CadenzaTextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Vocal parameters configuration tuning
            Text("VOCAL TARGETS", fontWeight = FontWeight.Bold, color = com.example.ui.theme.CadenzaTextPrimary, fontSize = 13.sp, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(12.dp))

            // Skill level selection
            com.example.ui.components.CadenzaCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("SKILL LEVEL", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = com.example.ui.theme.CadenzaTextSecondary)
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf("Beginner", "Intermediate", "Pro").forEach { level ->
                            val selected = skillLevel == level
                            com.example.ui.components.CadenzaButton(
                                text = level,
                                onClick = { skillLevel = level },
                                color = if (selected) com.example.ui.theme.CadenzaHyperMagenta else com.example.ui.theme.CadenzaTextSecondary,
                                modifier = Modifier.height(40.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Vocal registers options
            com.example.ui.components.CadenzaCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("REGISTER TYPE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = com.example.ui.theme.CadenzaTextSecondary)
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf("Soprano", "Mezzo-Soprano", "Alto", "Tenor", "Baritone", "Bass").forEach { reg ->
                            val selected = vocalClassification == reg
                            com.example.ui.components.CadenzaButton(
                                text = reg,
                                onClick = { vocalClassification = reg },
                                color = if (selected) com.example.ui.theme.CadenzaDeepSkyBlue else com.example.ui.theme.CadenzaTextSecondary,
                                modifier = Modifier.height(40.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Account controls
            Text("SYSTEM", fontWeight = FontWeight.Bold, color = com.example.ui.theme.CadenzaTextPrimary, fontSize = 13.sp, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(12.dp))

            com.example.ui.components.CadenzaButton(
                text = "REPLAY INITIALIZATION",
                onClick = onNavigateOnboard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            com.example.ui.components.CadenzaButton(
                text = "POWER OFF (SIGN OUT)",
                onClick = { viewModel.doLogout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("logout_button"),
                color = Color(0xFFFF3B30) // Red warning action
            )
        }
    }
}

// --- CORE UTILITY DESIGN HELPERS ---
@Composable
fun StatsCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    com.example.ui.components.CadenzaCard(
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    color = com.example.ui.theme.CadenzaTextSecondary
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            com.example.ui.components.CadenzaDisplay(modifier = Modifier.fillMaxWidth()) {
                Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun MainActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit,
    testTagId: String
) {
    com.example.ui.components.CadenzaCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTagId)
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title.uppercase(), fontWeight = FontWeight.Bold, color = com.example.ui.theme.CadenzaTextPrimary, fontSize = 14.sp, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, color = com.example.ui.theme.CadenzaTextSecondary, fontSize = 11.sp, letterSpacing = 0.5.sp)
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(com.example.ui.theme.CadenzaRecessedBase, shape = CircleShape)
                    .border(1.dp, com.example.ui.theme.CadenzaShadowDark, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = gradient.first())
            }
        }
    }
}

@Composable
fun MainSecondaryRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    com.example.ui.components.CadenzaCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .neumorphic(isPressed = true, cornerRadius = 10.dp, elevation = 3.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = com.example.ui.theme.CadenzaHyperMagenta, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(title.uppercase(), fontWeight = FontWeight.Bold, color = com.example.ui.theme.CadenzaTextPrimary, fontSize = 13.sp, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(subtitle, color = com.example.ui.theme.CadenzaTextSecondary, fontSize = 11.sp)
                }
            }
            Icon(Icons.Default.Share, null, tint = com.example.ui.theme.CadenzaTextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}
