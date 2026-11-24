package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ai.ViralAiService
import com.example.data.ViralRadarRepository
import com.example.firebase.configureAppCheck
import com.example.model.UiState
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.OnboardingProfileScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RadarHomeScreen
import com.example.ui.screens.ScriptsLibraryScreen
import com.example.ui.screens.TrendScriptScreen
import com.example.ui.theme.RadarViralTheme
import com.example.ui.viewmodel.RadarViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

private const val ROUTE_RADAR = "radar"
private const val ROUTE_SCRIPTS = "scripts"
private const val ROUTE_PROFILE = "profile"
private const val ROUTE_TREND_DETAIL = "trend_detail/{trendId}"

private data class TopLevelTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TOP_LEVEL_TABS = listOf(
    TopLevelTab(ROUTE_RADAR, "Radar Hoje", Icons.Filled.Radar, Icons.Outlined.Radar, "nav_tab_radar"),
    TopLevelTab(ROUTE_SCRIPTS, "Roteiros", Icons.Filled.Bookmarks, Icons.Outlined.Bookmarks, "nav_tab_scripts"),
    TopLevelTab(ROUTE_PROFILE, "Perfil", Icons.Filled.Person, Icons.Outlined.Person, "nav_tab_profile")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            RadarViralTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent()
                }
            }
        }
    }
}

@Composable
fun AppContent() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    } else {
        key(user.uid) {
            AuthenticatedRadarApp(currentUserId = user.uid)
        }
    }
}

@Composable
private fun AuthenticatedRadarApp(currentUserId: String) {
    val viewModel: RadarViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                RadarViewModel(
                    repository = ViralRadarRepository(db),
                    aiService = ViralAiService(),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val summaryState by viewModel.dailySummaryState.collectAsStateWithLifecycle()
    val trendsState by viewModel.todayTrendsState.collectAsStateWithLifecycle()
    val scriptsState by viewModel.savedScriptsState.collectAsStateWithLifecycle()
    val analysisActionState by viewModel.analysisActionState.collectAsStateWithLifecycle()
    val adaptActionState by viewModel.adaptActionState.collectAsStateWithLifecycle()
    val selectedPlatformFilter by viewModel.selectedPlatformFilter.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(toastMessage) {
        val msg = toastMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeToast()
        }
    }

    val currentProfile = (profileState as? UiState.Success)?.data
    val currentSummary = (summaryState as? UiState.Success)?.data
    val summaryLoaded = summaryState is UiState.Success

    LaunchedEffect(currentProfile, summaryLoaded, currentSummary) {
        if (currentProfile != null && summaryLoaded && currentSummary == null) {
            viewModel.maybeTriggerDailyAutoAnalysis(currentProfile, null)
        }
    }

    when (val pState = profileState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = pState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        is UiState.Success -> {
            val profile = pState.data
            if (profile == null) {
                OnboardingProfileScreen(
                    onSaveProfile = { name, niche, audience, tone, platforms, goal ->
                        viewModel.saveCreatorProfile(
                            displayName = name,
                            niche = niche,
                            targetAudience = audience,
                            toneStyle = tone,
                            preferredPlatforms = platforms,
                            weeklyGoal = goal,
                            triggerInitialRadar = true
                        )
                    }
                )
            } else {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val isTopLevelRoute = TOP_LEVEL_TABS.any { tab ->
                    currentDestination?.hierarchy?.any { it.route == tab.route } == true
                }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useNavigationRail = maxWidth >= 600.dp

                    Row(modifier = Modifier.fillMaxSize()) {
                        if (useNavigationRail && isTopLevelRoute) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.statusBarsPadding()
                            ) {
                                TOP_LEVEL_TABS.forEach { tab ->
                                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = {
                                            navController.navigate(tab.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        },
                                        label = { Text(tab.label) },
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
                            }
                        }

                        Scaffold(
                            modifier = Modifier.weight(1f),
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            bottomBar = {
                                if (!useNavigationRail && isTopLevelRoute) {
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ) {
                                        TOP_LEVEL_TABS.forEach { tab ->
                                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                                            NavigationBarItem(
                                                selected = selected,
                                                onClick = {
                                                    navController.navigate(tab.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                },
                                                icon = {
                                                    Icon(
                                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                        contentDescription = tab.label
                                                    )
                                                },
                                                label = { Text(tab.label) },
                                                modifier = Modifier.testTag(tab.testTag)
                                            )
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            NavHost(
                                navController = navController,
                                startDestination = ROUTE_RADAR,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                composable(ROUTE_RADAR) {
                                    RadarHomeScreen(
                                        profile = profile,
                                        todayFormatted = viewModel.todayFormattedPt,
                                        summaryState = summaryState,
                                        trendsState = trendsState,
                                        analysisActionState = analysisActionState,
                                        selectedPlatformFilter = selectedPlatformFilter,
                                        onSelectPlatformFilter = viewModel::setPlatformFilter,
                                        onRunDailyAnalysis = { focusTopic ->
                                            viewModel.runDailyAnalysis(profile, focusTopic)
                                        },
                                        onDismissError = viewModel::clearAnalysisError,
                                        onOpenTrendScript = { trend ->
                                            if (trend.id.isNotBlank()) {
                                                navController.navigate("trend_detail/${trend.id}")
                                            }
                                        },
                                        onSaveTrendToLibrary = viewModel::saveTrendToLibrary
                                    )
                                }

                                composable(ROUTE_SCRIPTS) {
                                    ScriptsLibraryScreen(
                                        scriptsState = scriptsState,
                                        weeklyGoal = profile.weeklyGoal,
                                        onUpdateStatus = viewModel::updateScriptStatus,
                                        onUpdateScriptContent = viewModel::updateSavedScript,
                                        onDeleteScript = viewModel::deleteScript,
                                        onCopiedFeedback = { msg ->
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        }
                                    )
                                }

                                composable(ROUTE_PROFILE) {
                                    ProfileScreen(
                                        profile = profile,
                                        onSaveProfile = { name, niche, audience, tone, platforms, goal ->
                                            viewModel.saveCreatorProfile(
                                                displayName = name,
                                                niche = niche,
                                                targetAudience = audience,
                                                toneStyle = tone,
                                                preferredPlatforms = platforms,
                                                weeklyGoal = goal,
                                                triggerInitialRadar = false
                                            )
                                        }
                                    )
                                }

                                composable(ROUTE_TREND_DETAIL) { backStackEntry ->
                                    val trendId = backStackEntry.arguments?.getString("trendId").orEmpty()
                                    val trendList = (trendsState as? UiState.Success)?.data.orEmpty()
                                    val selectedTrend = trendList.firstOrNull { it.id == trendId }

                                    if (selectedTrend == null) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    } else {
                                        TrendScriptScreen(
                                            trend = selectedTrend,
                                            adaptState = adaptActionState,
                                            onBack = { navController.popBackStack() },
                                            onSaveToLibrary = { viewModel.saveTrendToLibrary(selectedTrend) },
                                            onAdaptWithAi = { topic ->
                                                viewModel.adaptTrendScript(profile, selectedTrend, topic)
                                            },
                                            onDismissAdaptError = viewModel::clearAdaptError,
                                            onCopiedFeedback = { msg ->
                                                scope.launch { snackbarHostState.showSnackbar(msg) }
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
}
