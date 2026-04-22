package com.growwx.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.*
import androidx.navigation.compose.*
import com.growwx.ui.auth.AuthScreen
import com.growwx.ui.dashboard.DashboardScreen
import com.growwx.ui.watchlist.WatchlistScreen
import com.growwx.ui.simulator.SimulatorScreen
import com.growwx.ui.alerts.AlertsScreen
import com.growwx.ui.portfolio.PortfolioScreen
import com.growwx.ui.profile.ProfileScreen
import com.growwx.ui.onboarding.OnboardingScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Auth : Screen("auth")
    object Dashboard : Screen("dashboard")
    object Watchlist : Screen("watchlist")
    object Simulator : Screen("simulator")
    object Alerts : Screen("alerts")
    object Portfolio : Screen("portfolio")
    object Profile : Screen("profile")
    object StockDetail : Screen("stock/{symbol}") {
        fun createRoute(symbol: String) = "stock/$symbol"
    }
}

@Composable
fun GrowwXNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(tween(200)) + slideInHorizontally { it / 4 } },
        exitTransition = { fadeOut(tween(150)) },
        popEnterTransition = { fadeIn(tween(200)) },
        popExitTransition = { fadeOut(tween(150)) + slideOutHorizontally { it / 4 } }
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onFinished = {
                navController.navigate(Screen.Auth.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Auth.route) {
            AuthScreen(onSuccess = {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToSimulator = { navController.navigate(Screen.Simulator.route) },
                onNavigateToWatchlist = { navController.navigate(Screen.Watchlist.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
            )
        }

        composable(Screen.Watchlist.route) {
            WatchlistScreen(
                onStockClick = { symbol -> navController.navigate(Screen.StockDetail.createRoute(symbol)) }
            )
        }

        composable(Screen.Simulator.route) {
            SimulatorScreen()
        }

        composable(Screen.Alerts.route) {
            AlertsScreen()
        }

        composable(Screen.Portfolio.route) {
            PortfolioScreen()
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onLogout = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
