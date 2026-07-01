package com.fviret.podometre.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fviret.podometre.R
import com.fviret.podometre.ui.theme.AppColors

/**
 * Écran Activité — anneau de progression, navigation par chevrons, bannière météo.
 * Rafraîchit les pas et la météo à chaque retour en foreground (ON_RESUME).
 * Sera enrichi avec calendrier et graphe dans KAN-23 à KAN-25.
 * Équivalent iOS : ActivityView.swift.
 */
@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshSteps()
        viewModel.refreshWeather()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            // Bannière météo + prévisions 7 jours — masquées si showWeatherForecast=false
            if (prefs.showWeatherForecast) {
                WeatherBanner(
                    state = uiState.weatherState,
                    modifier = Modifier.fillMaxWidth()
                )
                if (uiState.dailyForecasts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    WeeklyForecastBanner(
                        forecasts = uiState.dailyForecasts,
                        cityName = uiState.cityName,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = uiState.selectedDateLabel,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Chevron gauche — toujours visible (pas de limite de jours)
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.goToPreviousDay()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.activity_chevron_prev_desc),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                StepRing(
                    steps = uiState.stepsToday,
                    goal = uiState.stepGoal,
                    ringColor = AppColors.colorForId(prefs.ringColorId)
                )

                // Chevron droit — ghost slot (Spacer) quand on est sur aujourd'hui
                if (uiState.selectedDayOffset < 0) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.goToNextDay()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.activity_chevron_next_desc),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
        }
    }
}
