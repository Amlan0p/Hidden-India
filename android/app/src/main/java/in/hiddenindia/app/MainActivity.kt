package `in`.hiddenindia.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import `in`.hiddenindia.app.ui.theme.DarkSurface
import `in`.hiddenindia.app.ui.theme.LightPrimary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import `in`.hiddenindia.app.ui.components.AppBottomNavigation
import `in`.hiddenindia.app.ui.components.FloatingAiButtons
import `in`.hiddenindia.app.ui.components.HiddenIndiaTopBar
import `in`.hiddenindia.app.ui.components.Screen
import `in`.hiddenindia.app.ui.screens.AskPlaceScreen
import `in`.hiddenindia.app.ui.screens.DestinationDetailScreen
import `in`.hiddenindia.app.ui.screens.DiscoverScreen
import `in`.hiddenindia.app.ui.screens.EmergencySOSScreen
import `in`.hiddenindia.app.ui.screens.FavoritesScreen
import `in`.hiddenindia.app.ui.screens.HomeScreen
import `in`.hiddenindia.app.ui.screens.InteractiveMapScreen
import `in`.hiddenindia.app.ui.screens.ItineraryResultScreen
import `in`.hiddenindia.app.ui.screens.LoginScreen
import `in`.hiddenindia.app.ui.screens.OfflineVaultScreen
import `in`.hiddenindia.app.ui.screens.ProfileScreen
import `in`.hiddenindia.app.ui.screens.ResponsibleTourismScreen
import `in`.hiddenindia.app.ui.screens.SignupScreen
import `in`.hiddenindia.app.ui.screens.TripPlannerScreen
import `in`.hiddenindia.app.ui.screens.TranslatorScreen
import `in`.hiddenindia.app.ui.screens.WelcomeScreen
import `in`.hiddenindia.app.ui.theme.HiddenIndiaTheme
import `in`.hiddenindia.app.ui.theme.TerracottaPrimary
import `in`.hiddenindia.app.ui.theme.ThemeController
import `in`.hiddenindia.app.ui.theme.rememberThemeController
import `in`.hiddenindia.app.ui.viewmodel.AuthState
import `in`.hiddenindia.app.ui.viewmodel.AuthViewModel
import `in`.hiddenindia.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep the native splash screen up while we check secure storage /
        // verify a stored session with the backend, so the user never sees
        // a flash of the login screen before a valid session is restored.
        val authViewModelHolder = androidx.lifecycle.ViewModelProvider(this).get(AuthViewModel::class.java)
        splashScreen.setKeepOnScreenCondition {
            authViewModelHolder.authState.value is AuthState.Loading
        }

        enableEdgeToEdge()
        setContent {
            val themeController = rememberThemeController()
            val isDark = themeController.isDark
            val topBarColor = if (isDark) DarkSurface else LightPrimary

            HiddenIndiaTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top bar strip: matches dark blue in dark mode, orange in light mode
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsTopHeight(WindowInsets.statusBars)
                                .background(topBarColor)
                        )
                        // Rest of the application content safely below the phone notification bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            LocationPermissionRequester()
                            HiddenIndiaRoot(authViewModel = authViewModelHolder, themeController = themeController)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationPermissionRequester() {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Handled by OS and location-dependent features */ }

    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}

@Composable
fun HiddenIndiaRoot(authViewModel: AuthViewModel, themeController: ThemeController) {
    val authState by authViewModel.authState.collectAsState()

    when (val state = authState) {
        is AuthState.Loading -> {
            // Covered by the native splash screen on cold start; shown briefly
            // otherwise (e.g. after a manual retry).
            FullScreenLoading()
        }

        is AuthState.SessionCheckFailed -> {
            SessionCheckErrorScreen(
                message = state.error.message,
                onRetry = { authViewModel.restoreSession() }
            )
        }

        is AuthState.Unauthenticated -> {
            AuthNavHost(authViewModel = authViewModel, themeController = themeController)
        }

        is AuthState.Authenticated -> {
            val mainViewModel: MainViewModel = viewModel()
            val navController = rememberNavController()
            AppNavigationHost(
                viewModel = mainViewModel,
                authViewModel = authViewModel,
                navController = navController,
                themeController = themeController
            )
        }
    }
}

@Composable
private fun FullScreenLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = TerracottaPrimary)
    }
}

@Composable
private fun SessionCheckErrorScreen(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Couldn't reach Hidden India",
                style = MaterialTheme.typography.titleLarge
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(12.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
fun AuthNavHost(authViewModel: AuthViewModel, themeController: ThemeController) {
    val navController = rememberNavController()
    // Destinations are local data (bundled JSON via DestinationRepository), not
    // auth-gated, so WelcomeScreen can safely use the same MainViewModel that
    // AppNavigationHost creates post-login — `viewModel()` resolves to the same
    // instance for the whole Activity either way, so nothing gets loaded twice.
    val mainViewModel: MainViewModel = viewModel()
    NavHost(navController = navController, startDestination = "welcome") {
        composable("welcome") {
            WelcomeScreen(
                viewModel = mainViewModel,
                themeController = themeController,
                onLogin = { navController.navigate("login") },
                onSignup = { navController.navigate("signup") }
            )
        }
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                themeController = themeController,
                onBackToWelcome = { navController.popBackStack("welcome", inclusive = false) },
                onNavigateToSignup = { navController.navigate("signup") }
            )
        }
        composable("signup") {
            SignupScreen(
                authViewModel = authViewModel,
                themeController = themeController,
                onBackToWelcome = { navController.popBackStack("welcome", inclusive = false) },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun AppNavigationHost(
    viewModel: MainViewModel,
    authViewModel: AuthViewModel,
    navController: NavHostController,
    themeController: ThemeController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Discover.route
    val selectedDestination by viewModel.selectedDestination.collectAsState()

    // Port of App.tsx's `isPublicStandalone`: every authenticated route (this whole nav
    // host, since login/signup live in the separate AuthNavHost) gets the Header + Navbar
    // chrome — not just the 6 bottom-tab destinations. The bottom nav still only
    // *highlights* a tab for the 6 routes it actually represents (AppBottomNavigation
    // simply shows no active state on routes like detail/profile/itinerary, same as the
    // web Navbar's `isActive` check).
    val searchQuery by viewModel.searchQuery.collectAsState()
    val allDestinations by viewModel.allDestinations.collectAsState()
    val darkTheme = themeController.isDark
    val isOfflineModeState by viewModel.isOfflineMode.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val density = LocalDensity.current
    val isImeOpen = WindowInsets.ime.getBottom(density) > 0

    Scaffold(
        topBar = {
            HiddenIndiaTopBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { query ->
                    viewModel.searchQuery.value = query
                },
                onSearchSubmit = {
                    // If user is on a detail, chat, or settings screen, take them back to the Discover feed
                    if (currentRoute != Screen.Discover.route && currentRoute != "discover") {
                        navController.navigate(Screen.Discover.route) {
                            popUpTo(Screen.Discover.route) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                },
                isDarkTheme = darkTheme,
                onToggleTheme = { themeController.toggle() },
                onAvatarClick = { navController.navigate("profile") },
                onSosClick = { navController.navigate("emergency") },
                onOfflineStripClick = { navController.navigate(Screen.Vault.route) },
                avatarSrc = currentUser?.avatar,
                avatarName = currentUser?.name,
                isOfflineMode = isOfflineModeState && (currentRoute == Screen.Vault.route),
                onLogoClick = {
                    navController.navigate(Screen.Discover.route) {
                        popUpTo(Screen.Discover.route) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                },
                destinations = allDestinations,
                onDestinationClick = { destId ->
                    viewModel.selectDestinationById(destId)
                    navController.navigate("detail/$destId")
                }
            )
        },
        bottomBar = {
            if (!isImeOpen) {
                AppBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route == Screen.Discover.route) {
                            // Tapping Discover tab ALWAYS takes the user straight to Discover / Home,
                            // clearing overlays and sub-screens so the user is never stuck
                            navController.navigate(Screen.Discover.route) {
                                popUpTo(Screen.Discover.route) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        } else {
                            navController.navigate(route) {
                                popUpTo(Screen.Discover.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Screen.Discover.route
            ) {
            // Screen.Discover.route == "home" now (see BottomNavBar.kt) — this is the
            // bottom-nav "Discover" tab's actual target, matching web Navbar's `to: '/'`
            // linking to HomePage, not DiscoverPage.
            composable(Screen.Discover.route) {
                HomeScreen(
                    viewModel = viewModel,
                    authViewModel = authViewModel,
                    onDestinationClick = { destId ->
                        viewModel.selectDestinationById(destId)
                        navController.navigate("detail/$destId")
                    },
                    onOpenAiPlanner = {
                        navController.navigate(Screen.TripPlanner.route)
                    },
                    onOpenSearch = {
                        navController.navigate("discover")
                    }
                )
            }

            // The web's separate '/discover' search/filter results page (Navbar still
            // shows this under the "Discover" tab per its `match: ['/', '/discover']`, but
            // it's reached via the search bar / "all filters" icon, not the tab itself).
            composable("discover") {
                DiscoverScreen(
                    viewModel = viewModel,
                    authViewModel = authViewModel,
                    onDestinationClick = { destId ->
                        viewModel.selectDestinationById(destId)
                        navController.navigate("detail/$destId")
                    },
                    onOpenAiPlanner = {
                        navController.navigate(Screen.TripPlanner.route)
                    },
                    onOpenAskAi = {
                        navController.navigate("chat")
                    },
                    onOpenTranslator = {
                        navController.navigate("translator")
                    },
                    onOpenMap = {
                        navController.navigate(Screen.MapExplorer.route)
                    }
                )
            }

            composable(
                route = "detail/{destinationId}?offline={offline}",
                arguments = listOf(
                    navArgument("destinationId") { type = NavType.StringType },
                    navArgument("offline") { type = NavType.BoolType; defaultValue = false }
                )
            ) { backStackEntry ->
                val destId = backStackEntry.arguments?.getString("destinationId") ?: ""
                val isOfflineOnly = backStackEntry.arguments?.getBoolean("offline") ?: false
                val dest = viewModel.allDestinations.collectAsState().value.find { it.id == destId }
                    ?: selectedDestination

                if (dest != null) {
                    DestinationDetailScreen(
                        destination = dest,
                        viewModel = viewModel,
                        authViewModel = authViewModel,
                        onBack = { navController.popBackStack() },
                        onPlanTrip = { destination ->
                            viewModel.selectedDestination.value = destination
                            navController.navigate(Screen.TripPlanner.route)
                        },
                        onAskAi = { destination ->
                            viewModel.selectedDestination.value = destination
                            navController.navigate("chat")
                        },
                        isOfflineOnly = isOfflineOnly
                    )
                }
            }

            composable(Screen.TripPlanner.route) {
                TripPlannerScreen(
                    viewModel = viewModel,
                    preselectedDestination = selectedDestination,
                    onItineraryGenerated = {
                        navController.navigate("itinerary_result")
                    },
                    onOpenDestination = { destId ->
                        navController.navigate("detail/$destId")
                    }
                )
            }

            composable("itinerary_result") {
                ItineraryResultScreen(
                    viewModel = viewModel,
                    authViewModel = authViewModel,
                    onBack = { navController.popBackStack() },
                    onViewDestination = { destId ->
                        viewModel.selectDestinationById(destId)
                        navController.navigate("detail/$destId")
                    }
                )
            }

            composable(
                route = "${Screen.MapExplorer.route}?offline={offline}",
                arguments = listOf(
                    navArgument("offline") { type = NavType.BoolType; defaultValue = false }
                )
            ) { backStackEntry ->
                val isOfflineOnly = backStackEntry.arguments?.getBoolean("offline") ?: false
                InteractiveMapScreen(
                    viewModel = viewModel,
                    onDestinationSelected = { destId ->
                        viewModel.selectDestinationById(destId)
                        navController.navigate("detail/$destId?offline=$isOfflineOnly")
                    },
                    offlineOnly = isOfflineOnly
                )
            }

            composable(Screen.Vault.route) {
                OfflineVaultScreen(
                    viewModel = viewModel,
                    onOpenDestination = { destId ->
                        viewModel.selectDestinationById(destId)
                        navController.navigate("detail/$destId?offline=$isOfflineModeState")
                    },
                    onOpenMap = { navController.navigate("${Screen.MapExplorer.route}?offline=$isOfflineModeState") },
                    onOpenTranslator = { navController.navigate("translator") },
                    onOpenEmergency = { navController.navigate("emergency") }
                )
            }

            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    viewModel = viewModel,
                    authViewModel = authViewModel,
                    onOpenDestination = { destId ->
                        viewModel.selectDestinationById(destId)
                        navController.navigate("detail/$destId")
                    },
                    // literal "discover" (search/filter screen) — matches web's Favorites
                    // empty-state CTA, which links to '/discover', not '/'.
                    onOpenDiscover = { navController.navigate("discover") },
                    onOpenPlanner = { navController.navigate(Screen.TripPlanner.route) },
                    onOpenItinerary = { itineraryId ->
                        val saved = authViewModel.savedItineraries.value.find { it.id == itineraryId }
                        if (saved != null) {
                            viewModel.currentItinerary.value = saved
                            navController.navigate("itinerary_result")
                        }
                    }
                )
            }

            composable("emergency") {
                EmergencySOSScreen(
                    onBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Discover.route)
                        }
                    },
                    viewModel = viewModel
                )
            }

            composable("profile") {
                ProfileScreen(
                    viewModel = viewModel,
                    authViewModel = authViewModel,
                    themeController = themeController,
                    onOpenDestination = { destId ->
                        viewModel.selectDestinationById(destId)
                        navController.navigate("detail/$destId")
                    },
                    onOpenItinerary = {
                        navController.navigate("itinerary_result")
                    },
                    onOpenVault = { navController.navigate(Screen.Vault.route) },
                    onOpenFavorites = { navController.navigate(Screen.Favorites.route) },
                    onOpenTranslator = { navController.navigate("translator") },
                    onOpenEmergency = { navController.navigate("emergency") },
                    onOpenResponsibleTourism = { navController.navigate("responsible_tourism") },
                    onLogout = {
                        authViewModel.logout()
                    },
                    onBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Discover.route)
                        }
                    }
                )
            }

            composable("responsible_tourism") {
                ResponsibleTourismScreen(
                    onBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Discover.route)
                        }
                    }
                )
            }

            composable("chat") {
                AskPlaceScreen(
                    viewModel = viewModel,
                    onBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Discover.route)
                        }
                    }
                )
            }

            composable("translator") {
                TranslatorScreen(
                    viewModel = viewModel,
                    onBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Discover.route)
                        }
                    }
                )
            }
        }

            // Port of web FloatingAIButtons.tsx: only on the Discover views — Home
            // ("home", Screen.Discover.route) and the search screen ("discover") — same
            // scoping web uses for '/' and '/discover'. Hidden on every other tab/screen.
            if (currentRoute == Screen.Discover.route || currentRoute == "discover") {
                FloatingAiButtons(
                    onOpenTranslator = { navController.navigate("translator") },
                    onOpenAskAi = { navController.navigate("chat") },
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
    }
}
