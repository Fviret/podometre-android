package com.fviret.podometre.ui.activity

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fviret.podometre.ui.theme.AppColors

/**
 * Écran Activité — affiche l'anneau de progression journalier.
 * Rafraîchit les pas à chaque retour en foreground (ON_RESUME).
 * Sera enrichi avec météo, calendrier et graphe dans KAN-21 à KAN-25.
 * Équivalent iOS : ActivityView.swift.
 */
@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Relit les pas depuis Health Connect à chaque retour en foreground (équivalent HKObserverQuery)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshSteps()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        StepRing(
            steps = uiState.stepsToday,
            goal = uiState.stepGoal,
            ringColor = AppColors.colorForId(prefs.ringColorId)
        )
    }
}
