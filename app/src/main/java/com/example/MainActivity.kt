package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.TapRepository
import com.example.ui.AuthScreen
import com.example.ui.AuthUiState
import com.example.ui.AuthViewModel
import com.example.ui.TapViewModel
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AiCoinStudioDialog
import com.example.ui.screens.AiStrategistDialog
import com.example.ui.screens.BoostersScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.TAPTheme
import com.example.ui.theme.TapDarkCharcoal
import com.example.ui.theme.TapGoldPrimary
import com.example.ui.theme.TapTextMuted
import com.example.util.AnalyticsHelper
import com.example.util.AudioHapticHelper
import com.example.util.NetworkMonitor
import com.example.util.configureAppCheck
import com.google.firebase.auth.FirebaseUser

enum class TapScreen(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    BOOSTERS("Boosters", Icons.Default.Bolt),
    TASKS("Tasks", Icons.Default.TaskAlt),
    WALLET("Wallet", Icons.Default.AccountBalanceWallet),
    LEADERBOARD("Top", Icons.Default.Leaderboard),
    PROFILE("Profile", Icons.Default.Person),
    ADMIN("Admin", Icons.Default.Person)
}

class MainActivity : ComponentActivity() {

    private lateinit var audioHapticHelper: AudioHapticHelper
    private lateinit var analyticsHelper: AnalyticsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure App Check before any Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        audioHapticHelper = AudioHapticHelper(this)
        analyticsHelper = AnalyticsHelper(this)

        setContent {
            TAPTheme {
                TapAppRoot(
                    audioHapticHelper = audioHapticHelper,
                    analyticsHelper = analyticsHelper
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioHapticHelper.release()
    }
}

@Composable
fun TapAppRoot(
    audioHapticHelper: AudioHapticHelper,
    analyticsHelper: AnalyticsHelper
) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.attemptAutoSignIn(context)
    }

    when (val state = authState) {
        is AuthUiState.Authenticated -> {
            LaunchedEffect(state.user.uid) {
                analyticsHelper.logLogin(state.user.providerData.firstOrNull()?.providerId ?: "password")
            }
            AuthenticatedApp(
                user = state.user,
                audioHapticHelper = audioHapticHelper,
                analyticsHelper = analyticsHelper,
                onSignOut = { authViewModel.signOut(context) }
            )
        }
        else -> {
            AuthScreen(
                authState = state,
                onGoogleSignInClick = {
                    analyticsHelper.logLogin("google")
                    authViewModel.signInWithGoogle(context)
                },
                onEmailSignInClick = { email, password ->
                    analyticsHelper.logLogin("password")
                    authViewModel.signInWithEmail(email, password)
                },
                onEmailSignUpClick = { email, password, username ->
                    analyticsHelper.logSignUp("password")
                    authViewModel.signUpWithEmail(email, password, username)
                },
                onClearError = {
                    authViewModel.clearError()
                }
            )
        }
    }
}

@Composable
fun AuthenticatedApp(
    user: FirebaseUser,
    audioHapticHelper: AudioHapticHelper,
    analyticsHelper: AnalyticsHelper,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { TapRepository(context) }
    val networkMonitor = remember(repository) { NetworkMonitor(context, repository.database) }
    DisposableEffect(networkMonitor) {
        onDispose { networkMonitor.release() }
    }
    val isOnline by networkMonitor.isOnline.collectAsState()

    val tapViewModel: TapViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                TapViewModel(
                    repository = repository,
                    analyticsHelper = analyticsHelper,
                    currentUserId = user.uid,
                    userEmail = user.email.orEmpty(),
                    displayName = user.displayName ?: "Tapper",
                    photoUrl = user.photoUrl?.toString()
                )
            }
        }
    )

    // Sync network state into ViewModel
    LaunchedEffect(isOnline) {
        tapViewModel.setOnlineStatus(isOnline)
    }

    val uiState by tapViewModel.uiState.collectAsState()
    var currentScreen by remember { mutableStateOf(TapScreen.HOME) }

    LaunchedEffect(currentScreen) {
        analyticsHelper.logScreenView(currentScreen.name)
    }

    var showAiCoinDialog by remember { mutableStateOf(false) }
    var showAiStrategistDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = currentScreen != TapScreen.HOME) {
        currentScreen = TapScreen.HOME
    }

    Scaffold(
        bottomBar = {
            if (currentScreen != TapScreen.ADMIN) {
                NavigationBar(
                    containerColor = TapDarkCharcoal,
                    contentColor = TapGoldPrimary,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("main_navigation_bar")
                ) {
                    val navItems = listOf(
                        TapScreen.HOME,
                        TapScreen.BOOSTERS,
                        TapScreen.TASKS,
                        TapScreen.WALLET,
                        TapScreen.LEADERBOARD,
                        TapScreen.PROFILE
                    )

                    navItems.forEach { screen ->
                        val isSelected = currentScreen == screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentScreen = screen },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.label,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = screen.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = TapGoldPrimary,
                                unselectedIconColor = TapTextMuted,
                                unselectedTextColor = TapTextMuted,
                                indicatorColor = TapGoldPrimary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Online-first Network Status Banner
            AnimatedVisibility(
                visible = !isOnline,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = Color(0xFF4A1015),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("offline_network_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFFF5252), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Offline Mode - Reconnecting to TAP real-time network...",
                            color = Color(0xFFFFCDD2),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (currentScreen) {
                    TapScreen.HOME -> HomeScreen(
                        uiState = uiState,
                        viewModel = tapViewModel,
                        audioHapticHelper = audioHapticHelper,
                        onNavigateToTasks = { currentScreen = TapScreen.TASKS },
                        onNavigateToBoosters = { currentScreen = TapScreen.BOOSTERS },
                        onOpenAiStrategist = { showAiStrategistDialog = true }
                    )
                    TapScreen.BOOSTERS -> BoostersScreen(
                        uiState = uiState,
                        viewModel = tapViewModel
                    )
                    TapScreen.TASKS -> TasksScreen(
                        uiState = uiState,
                        viewModel = tapViewModel
                    )
                    TapScreen.WALLET -> WalletScreen(
                        uiState = uiState,
                        viewModel = tapViewModel
                    )
                    TapScreen.LEADERBOARD -> LeaderboardScreen(
                        uiState = uiState,
                        currentUserId = user.uid
                    )
                    TapScreen.PROFILE -> ProfileScreen(
                        uiState = uiState,
                        audioHapticHelper = audioHapticHelper,
                        onOpenAiCoinStudio = { showAiCoinDialog = true },
                        onOpenAdminDashboard = { currentScreen = TapScreen.ADMIN },
                        onSignOutClick = onSignOut
                    )
                    TapScreen.ADMIN -> AdminScreen(
                        uiState = uiState,
                        viewModel = tapViewModel,
                        onBackClick = { currentScreen = TapScreen.PROFILE }
                    )
                }
            }
        }
    }

    if (showAiCoinDialog) {
        AiCoinStudioDialog(
            uiState = uiState,
            viewModel = tapViewModel,
            onDismiss = { showAiCoinDialog = false }
        )
    }

    if (showAiStrategistDialog) {
        AiStrategistDialog(
            uiState = uiState,
            viewModel = tapViewModel,
            onDismiss = { showAiStrategistDialog = false }
        )
    }
}
