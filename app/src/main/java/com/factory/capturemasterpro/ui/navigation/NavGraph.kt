package com.factory.capturemasterpro.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.factory.capturemasterpro.CaptureMasterApp
import com.factory.capturemasterpro.ui.screens.EditorScreen
import com.factory.capturemasterpro.ui.screens.GalleryScreen
import com.factory.capturemasterpro.ui.screens.HomeScreen
import com.factory.capturemasterpro.ui.screens.PaywallScreen
import com.factory.capturemasterpro.ui.screens.SettingsScreen

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object Record : Screen("record", "Record", Icons.Filled.FiberManualRecord, Icons.Outlined.FiberManualRecord)
    data object Gallery : Screen("gallery", "Gallery", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    data object Editor : Screen("editor/{recordingId}", "Editor", Icons.Filled.Settings, Icons.Outlined.Settings) {
        fun createRoute(recordingId: Long) = "editor/$recordingId"
    }
    data object Paywall : Screen("paywall", "Paywall", Icons.Filled.Settings, Icons.Outlined.Settings)
}

private val bottomNavItems = listOf(
    Screen.Home,
    Screen.Gallery,
    Screen.Settings
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val context = LocalContext.current
    val app = context.applicationContext as CaptureMasterApp
    val isPremium by app.premiumManager.isPremium.collectAsState(initial = false)

    val showBottomBar = bottomNavItems.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == screen.route
                        } == true

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    isPremium = isPremium,
                    onNavigateToGallery = {
                        navController.navigate(Screen.Gallery.route)
                    },
                    onNavigateToEditor = { recordingId ->
                        navController.navigate(Screen.Editor.createRoute(recordingId))
                    },
                    onNavigateToPaywall = {
                        navController.navigate(Screen.Paywall.route)
                    }
                )
            }

            composable(Screen.Gallery.route) {
                GalleryScreen(
                    onNavigateToEditor = { recordingId ->
                        navController.navigate(Screen.Editor.createRoute(recordingId))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    isPremium = isPremium,
                    onNavigateToPaywall = {
                        navController.navigate(Screen.Paywall.route)
                    }
                )
            }

            composable(
                route = Screen.Editor.route,
                arguments = listOf(
                    navArgument("recordingId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val recordingId = backStackEntry.arguments?.getLong("recordingId") ?: 0L
                EditorScreen(
                    recordingId = recordingId,
                    isPremium = isPremium,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPaywall = {
                        navController.navigate(Screen.Paywall.route)
                    }
                )
            }

            composable(Screen.Paywall.route) {
                PaywallScreen(
                    onDismiss = { navController.popBackStack() }
                )
            }
        }
    }
}
