package com.fviret.podometre.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.fviret.podometre.R
import com.fviret.podometre.ui.activity.ActivityScreen
import com.fviret.podometre.ui.journey.JourneyDetailScreen
import com.fviret.podometre.ui.journey.JourneyListScreen
import com.fviret.podometre.ui.onboarding.OnboardingScreen
import com.fviret.podometre.ui.settings.SettingsScreen

/**
 * Racine de l'application Compose.
 * Affiche l'onboarding si [hasCompletedOnboarding] est false,
 * sinon la BottomNavigationBar à 3 onglets.
 * Équivalent iOS : ContentView + fullScreenCover OnboardingView.
 */
@Composable
fun PodoMetreApp(
    viewModel: MainViewModel = hiltViewModel()
) {
    val showOnboarding by viewModel.showOnboarding.collectAsStateWithLifecycle()

    // null = état initial non encore chargé → rien (évite le flash)
    when (showOnboarding) {
        null -> Box(modifier = Modifier.fillMaxSize())
        true -> OnboardingScreen(onComplete = { /* DataStore mis à jour → recomposition auto */ })
        false -> MainContent()
    }
}

@Composable
private fun MainContent() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DirectionsWalk,
                            contentDescription = stringResource(R.string.nav_tab_activity_desc)
                        )
                    },
                    label = { Text(stringResource(R.string.nav_tab_activity)) },
                    selected = currentDestination?.hierarchy?.any { it.route == NavRoutes.ACTIVITY } == true,
                    onClick = {
                        navController.navigate(NavRoutes.ACTIVITY) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = stringResource(R.string.nav_tab_journeys_desc)
                        )
                    },
                    label = { Text(stringResource(R.string.nav_tab_journeys)) },
                    selected = currentDestination?.hierarchy?.any { it.route == NavRoutes.JOURNEYS } == true,
                    onClick = {
                        navController.navigate(NavRoutes.JOURNEYS) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.nav_tab_settings_desc)
                        )
                    },
                    label = { Text(stringResource(R.string.nav_tab_settings)) },
                    selected = currentDestination?.hierarchy?.any { it.route == NavRoutes.SETTINGS } == true,
                    onClick = {
                        navController.navigate(NavRoutes.SETTINGS) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.ACTIVITY,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoutes.ACTIVITY) { ActivityScreen() }
            composable(NavRoutes.JOURNEYS) {
                JourneyListScreen(
                    onNavigateToDetail = { journeyId ->
                        navController.navigate(NavRoutes.journeyDetail(journeyId))
                    }
                )
            }
            composable(
                route = NavRoutes.JOURNEY_DETAIL,
                arguments = listOf(navArgument("journeyId") { type = NavType.StringType })
            ) {
                JourneyDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(NavRoutes.SETTINGS) { SettingsScreen() }
        }
    }
}
