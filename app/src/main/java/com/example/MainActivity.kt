package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.google.firebase.auth.FirebaseUser
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
import com.example.ui.theme.TapBlack
import com.example.ui.theme.TapDarkCharcoal
import com.example.ui.theme.TapGoldContainer
import com.example.ui.theme.TapGoldPrimary
import com.example.ui.theme.TapOutline
import com.example.ui.theme.TapTextMuted
import com.example.ui.theme.TapTextSecondary
import com.example.util.AudioHapticHelper
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // MANDATORY: Step 1. Configure App Check with launch intent before any UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        audioHapticHelper = AudioHapticHelper(this)

        setContent {
            TAPTheme {
                TapAppRoot(audioHapticHelper = audioHapticHelper)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioHapticHelper.release()
    }
}

@Composable
fun TapAppRoot(audioHapticHelper: AudioHapticHelper) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.attemptAutoSignIn(context)
    }

    when (val state = authState) {
        is AuthUiState.Authenticated -> {
            AuthenticatedApp(
                user = state.user,
                audioHapticHelper = audioHapticHelper,
                onSignOut = { authViewModel.signOut(context) }
            )
        }
        else -> {
            AuthScreen(
                authState = state,
                onSignInClick = { authViewModel.signInWithGoogle(context) }
            )
        }
    }
}

@Composable
fun AuthenticatedApp(
    user: com.google.firebase.auth.FirebaseUser,
    audioHapticHelper: AudioHapticHelper,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val databaseId = context.getString(R.string.firestore_database_id)
    val firestore = remember(databaseId) { FirebaseFirestore.getInstance(databaseId) }
    val repository = remember(firestore) { TapRepository(firestore) }

    val tapViewModel: TapViewModel = viewModel(
        key = user.uid,
        factory = androidx.lifecycle.viewmodel.viewModelFactory {
            initializer {
                TapViewModel(
                    repository = repository,
                    currentUserId = user.uid,
                    userEmail = user.email.orEmpty(),
                    displayName = user.displayName ?: "Tapper",
                    photoUrl = user.photoUrl?.toString()
                )
            }
        }
    )

    val uiState by tapViewModel.uiState.collectAsState()
    var currentScreen by remember { mutableStateOf(TapScreen.HOME) }

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
