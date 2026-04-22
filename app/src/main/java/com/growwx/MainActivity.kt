package com.growwx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.growwx.data.local.UserPreferences
import com.growwx.ui.navigation.*
import com.growwx.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

// ─── Bottom Nav Items ─────────────────────────────────────────────────────────

data class NavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    NavItem(Screen.Dashboard, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    NavItem(Screen.Watchlist, "Watchlist", Icons.Filled.Bookmarks, Icons.Outlined.Bookmarks),
    NavItem(Screen.Simulator, "Simulate", Icons.Filled.ShowChart, Icons.Outlined.ShowChart),
    NavItem(Screen.Alerts, "Alerts", Icons.Filled.NotificationsActive, Icons.Outlined.NotificationsNone),
    NavItem(Screen.Portfolio, "Portfolio", Icons.Filled.PieChart, Icons.Outlined.PieChart),
)

// ─── Root ViewModel ───────────────────────────────────────────────────────────

@HiltViewModel
class MainViewModel @Inject constructor(
    private val prefs: UserPreferences
) : ViewModel() {

    data class AppState(
        val isLoggedIn: Boolean = false,
        val hasOnboarded: Boolean = false,
        val isDarkMode: Boolean = false,
        val isReady: Boolean = false
    )

    val appState: StateFlow<AppState> = combine(
        prefs.isLoggedIn,
        prefs.hasOnboarded,
        prefs.isDarkMode
    ) { loggedIn, onboarded, dark ->
        AppState(isLoggedIn = loggedIn, hasOnboarded = onboarded, isDarkMode = dark, isReady = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppState())
}

// ─── MainActivity ─────────────────────────────────────────────────────────────

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { GrowwXRoot() }
    }
}

// ─── Root Composable ──────────────────────────────────────────────────────────

@Composable
fun GrowwXRoot(viewModel: MainViewModel = hiltViewModel()) {
    val appState by viewModel.appState.collectAsState()

    GrowwXTheme(darkTheme = appState.isDarkMode) {
        if (!appState.isReady) {
            // Splash / loading
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📈", fontSize = 52.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("GrowwX", style = MaterialTheme.typography.displayLarge, color = GrowwXColor.Green)
                    Spacer(Modifier.height(24.dp))
                    CircularProgressIndicator(color = GrowwXColor.Green, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                }
            }
            return@GrowwXTheme
        }

        val startDestination = when {
            !appState.hasOnboarded -> Screen.Onboarding.route
            !appState.isLoggedIn -> Screen.Auth.route
            else -> Screen.Dashboard.route
        }

        GrowwXApp(startDestination = startDestination)
    }
}

// ─── Main App Shell with Bottom Nav ──────────────────────────────────────────

@Composable
fun GrowwXApp(startDestination: String) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Screens that should show bottom nav
    val bottomNavRoutes = bottomNavItems.map { it.screen.route }
    val showBottomNav = currentDestination?.route in bottomNavRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomNav,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                GrowwXBottomNavBar(
                    items = bottomNavItems,
                    currentDestination = currentDestination,
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            GrowwXNavGraph(
                navController = navController,
                startDestination = startDestination
            )
        }
    }
}

// ─── Bottom Nav Bar ───────────────────────────────────────────────────────────

@Composable
fun GrowwXBottomNavBar(
    items: List<NavItem>,
    currentDestination: androidx.navigation.NavDestination?,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier.height(72.dp)
    ) {
        items.forEach { item ->
            val isSelected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.screen) },
                icon = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            modifier = Modifier.size(22.dp)
                        )
                        // Active indicator dot
                        AnimatedVisibility(visible = isSelected) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 3.dp)
                                    .size(4.dp)
                                    .background(GrowwXColor.Green, androidx.compose.foundation.shape.CircleShape)
                            )
                        }
                    }
                },
                label = {
                    Text(
                        item.label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = GrowwXColor.Green,
                    selectedTextColor = GrowwXColor.Green,
                    unselectedIconColor = MaterialTheme.extendedColors.textMuted,
                    unselectedTextColor = MaterialTheme.extendedColors.textMuted,
                    indicatorColor = GrowwXColor.GreenLight
                )
            )
        }
    }
}
