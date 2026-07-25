package com.fviret.podometre.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import android.Manifest
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fviret.podometre.R
import com.fviret.podometre.data.weather.DailyForecast
import com.fviret.podometre.ui.theme.AppColors

/**
 * Écran Activité — anneau en haut, puis météo, calendrier, graphe en scrollant.
 * Rafraîchit les données à chaque retour en foreground (ON_RESUME).
 * Équivalent iOS : ActivityView.swift.
 */
@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showAphorism by viewModel.showAphorismDialog.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val context = LocalContext.current

    /** Prévision sélectionnée — non null → bottom sheet de détail météo ouvert. */
    var selectedForecast by remember { mutableStateOf<DailyForecast?>(null) }

    // Demande ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION pour la météo.
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onLocationPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED) {
            locationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            viewModel.onLocationPermissionResult(true)
        }
    }

    // Demande ACTIVITY_RECOGNITION (nécessaire sur Android 10+) puis démarre le capteur.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.startLiveSensor() }

    fun startSensorIfAllowed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) viewModel.startLiveSensor() else permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            viewModel.startLiveSensor()
        }
    }

    // Détecte le franchissement de l'objectif (< 100 % → ≥ 100 %), aujourd'hui uniquement.
    // -1L = sentinelle : on ne tire pas de haptic lors de la première composition.
    val prevStepsRef = remember { mutableLongStateOf(-1L) }

    // Réinitialise la référence à chaque changement de jour pour éviter un haptic fantôme
    // au retour sur aujourd'hui : sans reset, les pas du jour passé (< objectif) ferait
    // croire que l'objectif vient d'être franchi alors qu'il l'était déjà.
    LaunchedEffect(uiState.selectedDayOffset) {
        prevStepsRef.longValue = -1L
    }

    LaunchedEffect(uiState.stepsToday, uiState.stepGoal) {
        val prev = prevStepsRef.longValue
        val current = uiState.stepsToday
        val goal = uiState.stepGoal.toLong()
        if (uiState.selectedDayOffset == 0 && prev >= 0L && prev < goal && current >= goal) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
        prevStepsRef.longValue = current
    }

    // Bottom sheet de détail météo horaire
    selectedForecast?.let { forecast ->
        WeatherDetailBottomSheet(
            forecast = forecast,
            onDismiss = { selectedForecast = null },
        )
    }

    if (showAphorism) {
        // markDisplayed() appelé à l'affichage (pas à la fermeture) pour robustesse
        LaunchedEffect(Unit) { viewModel.onAphorismShown() }
        com.fviret.podometre.ui.aphorism.AphorismPopup(
            aphorism = viewModel.todayAphorism,
            onDismiss = viewModel::dismissAphorism,
        )
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshSteps()
        viewModel.refreshWeather()
        viewModel.refreshCalendar()
        // Retente l'affichage à chaque retour en premier plan (garde 1×/jour conservée).
        viewModel.checkAphorismVisibility()
        // Démarre le capteur live pour aujourd'hui (permission vérifiée avant).
        startSensorIfAllowed()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.stopLiveSensor()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        // ── 1. Anneau de progression (en haut) ────────────────────────────────
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
                ringColor = AppColors.colorForId(prefs.ringColorId),
                streak = uiState.streak,
                isToday = uiState.selectedDayOffset == 0,
            )

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

        Spacer(modifier = Modifier.height(24.dp))

        // ── 2. Métriques du jour (distance · temps actif · calories) ──────────
        if (prefs.showTodayMetrics) {
            TodayMetricsView(
                ringColor = AppColors.colorForId(prefs.ringColorId),
                distanceKm = uiState.distanceKm,
                activeMinutes = uiState.activeMinutes,
                caloriesKcal = uiState.caloriesKcal,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── 4. Bannière météo + prévisions 7 jours ────────────────────────────
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
                    modifier = Modifier.fillMaxWidth(),
                    onDayClick = { forecast -> selectedForecast = forecast },
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── 5. Calendrier mensuel ─────────────────────────────────────────────
        if (prefs.showMonthCalendar) {
            MonthCalendarView(
                month = uiState.calendarMonth,
                stepsPerDay = uiState.calendarSteps,
                goal = uiState.stepGoal,
                total = uiState.calendarTotal,
                accentColor = AppColors.colorForId(prefs.ringColorId),
                onPreviousMonth = { viewModel.navigateCalendarPrevious() },
                onNextMonth = { viewModel.navigateCalendarNext() },
                onDayTap = { date -> viewModel.onCalendarDayTap(date) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── 6. Graphe comparaison hebdomadaire ────────────────────────────────
        if (prefs.showWeeklyChart) {
            WeeklyChartView(
                currentWeek = uiState.currentWeekSteps,
                previousWeek = uiState.previousWeekSteps,
                dayLabels = uiState.weekDayLabels,
                todayIndex = uiState.weekTodayIndex,
                accentColor = AppColors.colorForId(prefs.ringColorId),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
