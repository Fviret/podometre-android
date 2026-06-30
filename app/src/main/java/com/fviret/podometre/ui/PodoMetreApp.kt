package com.fviret.podometre.ui

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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fviret.podometre.R
import com.fviret.podometre.ui.activity.ActivityScreen
import com.fviret.podometre.ui.journey.JourneyListScreen
import com.fviret.podometre.ui.settings.SettingsScreen

/**
 * Racine de l'application Compose.
 * Gère la BottomNavigationBar à 3 onglets et le NavHost principal.
 * Équivalent iOS : TabView dans ContentView.swift
 */
@Composable
fun PodoMetreApp() {
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
            composable(NavRoutes.JOURNEYS) { JourneyListScreen() }
            composable(NavRoutes.SETTINGS) { SettingsScreen() }
        }
    }
}
