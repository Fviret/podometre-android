package com.fviret.podometre.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fviret.podometre.R
import com.fviret.podometre.data.preferences.HomeSection
import com.fviret.podometre.data.weather.DailyForecast
import com.fviret.podometre.ui.theme.AppColors

/**
 * Écran Activité — anneau en haut, puis météo, calendrier, graphe en scrollant.
 * Rafraîchit les données à chaque retour en foreground (ON_RESUME).
 * Équivalent iOS : ActivityView.swift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel = hiltViewModel(),
    onNavigateToHistory: () -> Unit = {},
    onNavigateToJourneys: () -> Unit = {},
) {
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showAphorism by viewModel.showAphorismDialog.collectAsStateWithLifecycle()
    val weeklyRecapData by viewModel.weeklyRecapData.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val context = LocalContext.current

    /** Prévision sélectionnée — non null → bottom sheet de détail météo ouvert. */
    var selectedForecast by remember { mutableStateOf<DailyForecast?>(null) }

    /**
     * Vrai une seule fois au franchissement de l'objectif du jour (one-shot).
     * Remis à false par [onCelebrationEnd] dans [StepRing] à la fin de l'animation.
     */
    var showCelebration by remember { mutableStateOf(false) }

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
            // Déclenche l'overlay de célébration (badge + particules)
            showCelebration = true
            // Séquence haptique en crescendo : léger → moyen → fort → succès.
            // Sur API 31+ (Android 12), on compose des primitives VibrationEffect pour un
            // rendu précis. Sur API 26–30, on séquence des HapticFeedbackConstants avec délais.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibrator = context.getSystemService(Vibrator::class.java)
                vibrator?.let { vib ->
                    val fx = VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.3f)
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.6f, 80)
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1.0f, 80)
                        .compose()
                    vib.vibrate(fx)
                }
            } else {
                // Séquence manuelle via coroutine
                launch {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    delay(100)
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    delay(100)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    }
                }
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

    weeklyRecapData?.let { recap ->
        WeeklyRecapSheet(
            data = recap,
            accentColor = AppColors.colorForId(prefs.ringColorId),
            onDismiss = viewModel::dismissWeeklyRecap,
            onNavigateToJourneys = onNavigateToJourneys,
        )
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshSteps()
        viewModel.refreshWeather()
        viewModel.refreshCalendar()
        // Retente l'affichage à chaque retour en premier plan (garde 1×/jour conservée).
        viewModel.checkAphorismVisibility()
        // Vérifie si le récapitulatif hebdo doit s'afficher (lundi, 1×/semaine).
        viewModel.checkWeeklyRecap()
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

        val density = LocalDensity.current
        /** Seuil minimal en pixels pour valider un swipe horizontal sur l'anneau (~44 dp). */
        val swipeThresholdPx = with(density) { 44.dp.toPx() }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(uiState.selectedDayOffset) {
                    var totalDx = 0f
                    var totalDy = 0f
                    detectHorizontalDragGestures(
                        onDragStart = {
                            totalDx = 0f
                            totalDy = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            // Consomme l'événement seulement si la dominance horizontale est confirmée
                            totalDx += dragAmount
                            change.consume()
                        },
                        onDragEnd = {
                            // Déclenche la navigation uniquement si le swipe dépasse le seuil
                            when {
                                totalDx > swipeThresholdPx -> {
                                    // Swipe vers la droite → jour précédent
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.goToPreviousDay()
                                }
                                totalDx < -swipeThresholdPx && uiState.selectedDayOffset < 0 -> {
                                    // Swipe vers la gauche → jour suivant, bloqué sur aujourd'hui
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.goToNextDay()
                                }
                            }
                        }
                    )
                },
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
                showCelebration = showCelebration,
                onCelebrationEnd = { showCelebration = false },
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

        // Sections optionnelles dans l'ordre choisi par l'utilisateur.
        // Une section n'est rendue que si son toggle est actif ET qu'elle a du contenu réel.
        val ringColor = AppColors.colorForId(prefs.ringColorId)
        val visibleSections = prefs.sectionOrder.filter { section ->
            when (section) {
                HomeSection.METRICS -> prefs.showTodayMetrics
                HomeSection.WEATHER -> prefs.showWeatherForecast
                HomeSection.CALENDAR -> prefs.showMonthCalendar
                HomeSection.CHART -> prefs.showWeeklyChart
            }
        }
        visibleSections.forEachIndexed { _, section ->
            when (section) {
                HomeSection.METRICS -> {
                    TodayMetricsView(
                        ringColor = ringColor,
                        distanceKm = uiState.distanceKm,
                        activeMinutes = uiState.activeMinutes,
                        caloriesKcal = uiState.caloriesKcal,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                HomeSection.WEATHER -> {
                    WeatherBanner(
                        state = uiState.weatherState,
                        modifier = Modifier.fillMaxWidth(),
                        isStale = uiState.isWeatherStale,
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
                }
                HomeSection.CALENDAR -> {
                    MonthCalendarView(
                        month = uiState.calendarMonth,
                        stepsPerDay = uiState.calendarSteps,
                        goal = uiState.stepGoal,
                        total = uiState.calendarTotal,
                        accentColor = ringColor,
                        onPreviousMonth = { viewModel.navigateCalendarPrevious() },
                        onNextMonth = { viewModel.navigateCalendarNext() },
                        onDayTap = { date -> viewModel.onCalendarDayTap(date) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                HomeSection.CHART -> {
                    WeeklyChartView(
                        currentWeek = uiState.currentWeekSteps,
                        previousWeek = uiState.previousWeekSteps,
                        dayLabels = uiState.weekDayLabels,
                        todayIndex = uiState.weekTodayIndex,
                        accentColor = ringColor,
                        modifier = Modifier.fillMaxWidth(),
                        onTap = onNavigateToHistory,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
