package com.fviret.podometre.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
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

/**
 * Racine de l'application Compose.
 * Gère la BottomNavigationBar à 3 onglets et le NavHost principal.
 * Équivalent iOS : TabView dans ContentView.swift
 */
@Composable
fun PodoMetreApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                NavigationBarItem(
                    icon = { Icon(Icons.Default.DirectionsWalk, contentDescription = "Activité") },
                    label = { Text("Activité") },
                    selected = currentDestination?.hierarchy?.any { it.route == "activity" } == true,
                    onClick = {
                        navController.navigate("activity") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Map, contentDescription = "Trajets") },
                    label = { Text("Trajets") },
                    selected = currentDestination?.hierarchy?.any { it.route == "journeys" } == true,
                    onClick = {
                        navController.navigate("journeys") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Paramètres") },
                    label = { Text("Paramètres") },
                    selected = currentDestination?.hierarchy?.any { it.route == "settings" } == true,
                    onClick = {
                        navController.navigate("settings") {
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
            startDestination = "activity",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("activity") {
                // TODO : ActivityScreen()
                Text("Écran Activité — à implémenter (KAN-19 à KAN-25)")
            }
            composable("journeys") {
                // TODO : JourneyListScreen()
                Text("Catalogue Trajets — à implémenter (KAN-28 à KAN-31)")
            }
            composable("settings") {
                // TODO : SettingsScreen()
                Text("Paramètres — à implémenter (KAN-32 à KAN-39)")
            }
        }
    }
}
