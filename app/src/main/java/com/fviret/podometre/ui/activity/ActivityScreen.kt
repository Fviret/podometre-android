package com.fviret.podometre.ui.activity

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fviret.podometre.ui.theme.AppColors

/**
 * Écran Activité — affiche l'anneau de progression journalier (KAN-19).
 * Sera enrichi avec météo, calendrier et graphe dans KAN-20 à KAN-25.
 * Équivalent iOS : ActivityView.swift.
 */
@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
