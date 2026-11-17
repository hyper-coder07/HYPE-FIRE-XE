package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.firebase.PlayerRepository
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.signOut
import com.example.data.model.MatchRecord
import com.example.data.model.PlayerProfile
import com.example.game.BattleRoyaleScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.LobbyScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.DropZoneTheme
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.StormCrimson
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.GameHubViewModel
import com.example.ui.viewmodel.StudioViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DropZoneTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = ObsidianBg) {
                    AppNavigation()
                }
            }
        }
    }
}

private enum class HubDestination(val labelRes: Int, val icon: ImageVector, val tag: String) {
    LOBBY(R.string.nav_lobby, Icons.Default.Home, "nav_lobby"),
    SHOP(R.string.nav_shop, Icons.Default.ShoppingBag, "nav_shop"),
    STUDIO(R.string.nav_studio, Icons.Default.AutoAwesome, "nav_studio"),
    OPTIMIZER(R.string.nav_optimizer, Icons.Default.Tune, "nav_optimizer")
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = { })
    } else {
        key(user.uid) {
            DropZoneAuthenticatedHub(
                currentUserId = user.uid,
                displayName = user.displayName
            )
        }
    }
}

@Composable
private fun DropZoneAuthenticatedHub(
    currentUserId: String,
    displayName: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val hubViewModel: GameHubViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                GameHubViewModel(
                    repository = PlayerRepository(db),
                    currentUserId = currentUserId,
                    initialDisplayName = displayName
                )
            }
        }
    )

    val studioViewModel: StudioViewModel = viewModel(
        key = "studio_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                StudioViewModel(app.applicationContext)
            }
        }
    )

    val profileState by hubViewModel.profileState.collectAsStateWithLifecycle()
    val matchesState by hubViewModel.matchesState.collectAsStateWithLifecycle()
    val selectedMode by hubViewModel.selectedMode.collectAsStateWithLifecycle()
    val feedback by hubViewModel.actionFeedback.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedback) {
        val msg = feedback
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            hubViewModel.clearFeedback()
        }
    }

    var inMatch by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    when (val pState = profileState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize().background(ObsidianBg),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = FlameOrange)
                    Text(
                        text = "Loading DropZone Profile...",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBg)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Could not load profile",
                        style = MaterialTheme.typography.headlineMedium,
                        color = StormCrimson
                    )
                    Text(
                        text = pState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }
        }

        is UiState.Success<PlayerProfile> -> {
            val profile = pState.data
            val matches: List<MatchRecord> = (matchesState as? UiState.Success)?.data ?: emptyList()

            if (inMatch) {
                BattleRoyaleScreen(
                    profile = profile,
                    mode = selectedMode,
                    onMatchFinished = { telemetry ->
                        hubViewModel.submitMatchResult(
                            profile = profile,
                            placement = telemetry.placement,
                            eliminations = telemetry.kills,
                            damageDealt = telemetry.damageDealt,
                            survivalSeconds = telemetry.survivalSeconds,
                            goldEarned = telemetry.goldEarned,
                            diamondsEarned = telemetry.diamondsEarned,
                            modeId = selectedMode.id
                        )
                        inMatch = false
                    },
                    onExitMatch = { inMatch = false }
                )
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useRail = maxWidth >= 680.dp
                    val destinations = HubDestination.entries

                    Scaffold(
                        containerColor = ObsidianBg,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        contentWindowInsets = WindowInsets.safeDrawing,
                        bottomBar = {
                            if (!useRail) {
                                NavigationBar(
                                    containerColor = ObsidianSurface,
                                    windowInsets = WindowInsets.navigationBars
                                ) {
                                    destinations.forEachIndexed { idx, dest ->
                                        NavigationBarItem(
                                            selected = selectedTab == idx,
                                            onClick = { selectedTab = idx },
                                            icon = {
                                                Icon(
                                                    dest.icon,
                                                    contentDescription = stringResource(dest.labelRes)
                                                )
                                            },
                                            label = { Text(stringResource(dest.labelRes)) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = ObsidianBg,
                                                selectedTextColor = FlameOrange,
                                                indicatorColor = FlameOrange,
                                                unselectedIconColor = TextSecondary,
                                                unselectedTextColor = TextSecondary
                                            ),
                                            modifier = Modifier.testTag(dest.tag)
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (useRail) {
                                NavigationRail(
                                    containerColor = ObsidianSurface,
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    destinations.forEachIndexed { idx, dest ->
                                        NavigationRailItem(
                                            selected = selectedTab == idx,
                                            onClick = { selectedTab = idx },
                                            icon = {
                                                Icon(
                                                    dest.icon,
                                                    contentDescription = stringResource(dest.labelRes)
                                                )
                                            },
                                            label = { Text(stringResource(dest.labelRes)) },
                                            colors = NavigationRailItemDefaults.colors(
                                                selectedIconColor = ObsidianBg,
                                                selectedTextColor = FlameOrange,
                                                indicatorColor = FlameOrange,
                                                unselectedIconColor = TextSecondary,
                                                unselectedTextColor = TextSecondary
                                            ),
                                            modifier = Modifier.testTag(dest.tag)
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .background(ObsidianSurfaceVariant.copy(alpha = 0.2f))
                            ) {
                                when (destinations[selectedTab]) {
                                    HubDestination.LOBBY -> LobbyScreen(
                                        profile = profile,
                                        selectedMode = selectedMode,
                                        recentMatches = matches,
                                        onSelectMode = hubViewModel::selectMode,
                                        onDropInClicked = { inMatch = true },
                                        onOpenOptimizer = { selectedTab = HubDestination.OPTIMIZER.ordinal }
                                    )
                                    HubDestination.SHOP -> ShopScreen(
                                        profile = profile,
                                        onBuyOrEquip = { item ->
                                            hubViewModel.buyOrEquipCosmetic(profile, item)
                                        }
                                    )
                                    HubDestination.STUDIO -> StudioScreen(studioViewModel = studioViewModel)
                                    HubDestination.OPTIMIZER -> SettingsScreen(
                                        profile = profile,
                                        onSaveSettings = { cfg, name ->
                                            hubViewModel.saveGraphicsAndControls(cfg, name)
                                        },
                                        onSignOut = {
                                            signOut(credentialManager, scope)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
