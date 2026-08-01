package com.fviret.podometre.ui.activity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.fviret.podometre.data.aphorism.Aphorism
import com.fviret.podometre.data.aphorism.AphorismRepository
import com.fviret.podometre.data.health.HealthConnectRepository
import com.fviret.podometre.data.preferences.UserPreferences
import com.fviret.podometre.data.preferences.UserPreferencesRepository
import com.fviret.podometre.data.weather.DailyForecast
import com.fviret.podometre.data.weather.HourlyForecast
import com.fviret.podometre.data.weather.WeatherRepository
import com.fviret.podometre.data.weather.WeatherState
import com.fviret.podometre.R
import com.fviret.podometre.util.isEmulator
import com.fviret.podometre.worker.SyncStepsWorker
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

/** Nombre de mois en arrière maximum pour la navigation calendrier. */
private const val MAX_CALENDAR_MONTHS_BACK = 12

/**
 * Calcule le nombre de pas live en combinant la référence Health Connect et le delta capteur.
 *
 * @param hcBaseline Nombre de pas HC au démarrage du capteur (source de vérité).
 * @param sensorStart Valeur cumulative du capteur au démarrage (-1 si pas encore initialisé).
 * @param sensorCurrent Valeur cumulative courante du capteur.
 * @return Pas estimés depuis minuit : [hcBaseline] + delta capteur, jamais négatif.
 */
internal fun computeLiveSteps(hcBaseline: Long, sensorStart: Long, sensorCurrent: Long): Long {
    if (sensorStart < 0L) return hcBaseline
    val delta = (sensorCurrent - sensorStart).coerceAtLeast(0L)
    return hcBaseline + delta
}

/** Pas mockés par décalage en jours (0 = aujourd'hui) pour l'émulateur. */
private val EMULATOR_STEPS_BY_OFFSET = mapOf(
    0 to 7_430L,
    -1 to 6_200L,
    -2 to 9_100L,
    -3 to 4_850L,
    -4 to 11_200L,
    -5 to 7_760L,
    -6 to 3_100L,
)

/** Génère un nombre de pas réaliste pour un jour donné sur émulateur (déterministe par jour). */
private fun emulatorStepsForDay(date: LocalDate): Long {
    val seed = date.dayOfMonth + date.monthValue * 31
    val bases = longArrayOf(3_200, 8_500, 6_700, 11_200, 4_900, 9_800, 7_100, 5_500, 12_000, 2_800)
    return bases[seed % bases.size]
}

/** Coordonnées mockées pour l'émulateur (Paris). */
private const val EMULATOR_LATITUDE = 48.8566
private const val EMULATOR_LONGITUDE = 2.3522

/**
 * État de l'écran Activité.
 * Sera enrichi au fil des tickets KAN-19 à KAN-25.
 */
data class ActivityUiState(
    val stepGoal: Int = 10_000,
    val stepsToday: Long = 0L,
    val weatherState: WeatherState? = null,
    val dailyForecasts: List<DailyForecast> = emptyList(),
    val cityName: String? = null,
    val isHealthConnectAvailable: Boolean = false,
    /** Décalage en jours par rapport à aujourd'hui (0 = aujourd'hui, -1 = hier, etc.). */
    val selectedDayOffset: Int = 0,
    /** Label affiché au-dessus de l'anneau : résolu depuis les ressources selon la locale. */
    val selectedDateLabel: String = "",
    /** Mois affiché dans le calendrier. */
    val calendarMonth: YearMonth = YearMonth.now(),
    /** Pas par jour pour le mois affiché (absents de la map = aucun pas). */
    val calendarSteps: Map<LocalDate, Long> = emptyMap(),
    /** Total de pas pour le mois calendrier affiché. */
    val calendarTotal: Long = 0L,
    /** Pas des 7 derniers jours (index 0 = il y a 6 jours, index 6 = aujourd'hui). */
    val currentWeekSteps: List<Long> = List(7) { 0L },
    /** Pas des 7 jours précédents (index 0 = il y a 13 jours, index 6 = il y a 7 jours). */
    val previousWeekSteps: List<Long> = List(7) { 0L },
    /** Labels abrégés de la semaine ISO courante (lundi → dimanche). */
    val weekDayLabels: List<String> = List(7) { "" },
    /** Indice du jour courant dans la semaine ISO (0=lundi … 6=dimanche). */
    val weekTodayIndex: Int = 6,
    /** Nombre de jours consécutifs avec l'objectif atteint (streak courant). 0 si aucun. */
    val streak: Int = 0,
    /** Distance parcourue (en km) pour le jour sélectionné. */
    val distanceKm: Double = 0.0,
    /** Temps actif estimé (en minutes) pour le jour sélectionné. */
    val activeMinutes: Int = 0,
    /** Calories actives brûlées (en kcal) pour le jour sélectionné. */
    val caloriesKcal: Int = 0,
)

/**
 * ViewModel de l'écran Activité.
 * Reçoit ses dépendances via Hilt (@HiltViewModel).
 * Équivalent iOS : ActivityViewModel.swift
 */
@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val healthConnectRepository: HealthConnectRepository,
    private val weatherRepository: WeatherRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val aphorismRepository: AphorismRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    /** Indique si la permission ACCESS_FINE_LOCATION / COARSE_LOCATION a été accordée au runtime. */
    @Volatile private var locationGranted = false

    /** Préférences utilisateur exposées en StateFlow pour les Composables. */
    val userPreferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferences()
        )

    private val _uiState = MutableStateFlow(
        ActivityUiState(
            isHealthConnectAvailable = healthConnectRepository.isAvailable(),
            selectedDateLabel = labelForOffset(0),
        )
    )

    /** État complet de l'écran. */
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    /** L'aphorisme du jour, chargé une seule fois au démarrage. */
    val todayAphorism: Aphorism = aphorismRepository.todayAphorism()

    /**
     * Vrai si la popup "Pensée du jour" doit être affichée.
     * Se réinitialise à false quand l'utilisateur ferme la popup.
     */
    private val _showAphorismDialog = MutableStateFlow(false)
    val showAphorismDialog: StateFlow<Boolean> = _showAphorismDialog.asStateFlow()

    // ── Capteur de pas live ───────────────────────────────────────────────────

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    /** Référence HC au moment du démarrage du capteur — ne recule jamais. */
    @Volatile private var hcStepsRef: Long = 0L

    /** Valeur cumulative du capteur TYPE_STEP_COUNTER au démarrage de la session. -1 = non initialisé. */
    @Volatile private var sensorStart: Long = -1L

    /** Timestamp (ms) de la dernière écriture dans Health Connect. Throttle à 60 s minimum. */
    @Volatile private var lastHcWriteMs: Long = 0L

    /** Nombre de pas lors de la dernière écriture HC. Throttle par palier de 50 pas. */
    @Volatile private var lastHcWriteSteps: Long = 0L

    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (_uiState.value.selectedDayOffset != 0) return
            val raw = event.values[0].toLong()
            if (sensorStart < 0L) sensorStart = raw
            val live = computeLiveSteps(hcStepsRef, sensorStart, raw)
            _uiState.value = _uiState.value.copy(stepsToday = maxOf(_uiState.value.stepsToday, live))
            maybeWriteStepsToHealthConnect(live)
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    /**
     * Écrit les pas dans Health Connect si le throttle le permet.
     * Conditions : au moins 60 secondes depuis la dernière écriture OU au moins 50 pas de plus.
     * Appellé uniquement pour aujourd'hui (selectedDayOffset == 0).
     */
    private fun maybeWriteStepsToHealthConnect(currentSteps: Long) {
        val nowMs = System.currentTimeMillis()
        val elapsed = nowMs - lastHcWriteMs
        val stepDelta = currentSteps - lastHcWriteSteps
        if (elapsed < 60_000L && stepDelta < 50L) return
        lastHcWriteMs = nowMs
        lastHcWriteSteps = currentSteps
        viewModelScope.launch(Dispatchers.IO) {
            healthConnectRepository.writeStepsToday(currentSteps)
        }
    }

    /**
     * Démarre l'écoute du capteur TYPE_STEP_COUNTER.
     * Sans effet sur émulateur ou si l'utilisateur est sur un jour passé.
     * Appelé depuis [ActivityScreen] sur ON_RESUME (permission déjà vérifiée côté UI).
     */
    fun startLiveSensor() {
        if (_uiState.value.selectedDayOffset != 0) return
        if (isEmulator()) return
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return
        sensorStart = -1L
        sensorManager.registerListener(stepListener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    /**
     * Arrête l'écoute du capteur (arrière-plan, jour passé, destruction du ViewModel).
     * Réinitialise [sensorStart] pour forcer un re-calibrage au prochain démarrage.
     */
    fun stopLiveSensor() {
        sensorManager.unregisterListener(stepListener)
        sensorStart = -1L
    }

    override fun onCleared() {
        super.onCleared()
        stopLiveSensor()
    }

    /**
     * Appelé à l'affichage de la popup (via LaunchedEffect dans ActivityScreen).
     * Délègue [AphorismRepository.markDisplayed] pour éviter un second affichage
     * même si l'utilisateur force-close l'app sans taper "Make my day".
     */
    fun onAphorismShown() {
        viewModelScope.launch {
            aphorismRepository.markDisplayed()
        }
    }

    /** Ferme la popup (bouton "Make my day" ou tap extérieur). */
    fun dismissAphorism() {
        _showAphorismDialog.value = false
    }

    init {
        viewModelScope.launch {
            userPreferences.collect { prefs ->
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val cachedSteps = if (prefs.cachedStepsTodayDate == today) prefs.cachedStepsToday else 0L
                _uiState.value = _uiState.value.copy(
                    stepGoal = prefs.dailyStepGoal,
                    stepsToday = maxOf(_uiState.value.stepsToday, cachedSteps)
                )
            }
        }
        loadStepsForOffset(0)
        loadCalendarMonth(YearMonth.now())
        loadWeeklyData()
        loadStreak()
        SyncStepsWorker.schedule(WorkManager.getInstance(context))
        checkAphorismVisibility()
        // Ré-affiche la popup quand l'utilisateur réactive la feature depuis les Paramètres.
        viewModelScope.launch {
            userPreferences
                .map { it.aphorismEnabled }
                .distinctUntilChanged()
                .drop(1) // valeur initiale déjà traitée par checkAphorismVisibility() ci-dessus
                .collect { enabled -> if (enabled) checkAphorismVisibility() }
        }
    }

    /**
     * Détermine si la popup "Pensée du jour" doit être affichée.
     * Rendue [internal] pour être appelée depuis [ActivityScreen] lors du retour en premier plan.
     */
    internal fun checkAphorismVisibility() {
        viewModelScope.launch {
            _showAphorismDialog.value = aphorismRepository.shouldShowPopup()
        }
    }

    /**
     * Charge le streak de jours consécutifs depuis Health Connect.
     * Appelé uniquement à l'initialisation et au retour en foreground — jamais lors d'un
     * changement de jour (KAN-92). [computeStreak] part toujours d'aujourd'hui : la valeur
     * est donc indépendante du jour affiché. L'affichage est de plus conditionné par
     * [StepRing] à `isToday && steps >= goal && streak > 0`.
     */
    private fun loadStreak() {
        viewModelScope.launch {
            val goal = userPreferences.value.dailyStepGoal.toLong()
            val streak = healthConnectRepository.computeStreak(goal)
            _uiState.value = _uiState.value.copy(streak = streak)
        }
    }

    /** Rafraîchit les pas au retour en foreground (ON_RESUME). */
    fun refreshSteps() {
        loadStepsForOffset(_uiState.value.selectedDayOffset)
    }

    /** Rafraîchit le calendrier au retour en foreground (recharge le mois affiché). */
    fun refreshCalendar() {
        loadCalendarMonth(_uiState.value.calendarMonth)
        loadWeeklyData()
    }

    /**
     * Appelé par [ActivityScreen] après le résultat de la demande de permission localisation.
     * Déclenche le chargement météo uniquement si la permission est accordée.
     */
    fun onLocationPermissionResult(granted: Boolean) {
        locationGranted = granted
        if (granted) loadWeather()
    }

    /** Rafraîchit la météo et les prévisions au retour en foreground (gard permission). */
    fun refreshWeather() {
        if (locationGranted) loadWeather()
    }

    /** Navigue vers le jour précédent (décalage - 1). */
    fun goToPreviousDay() {
        val newOffset = _uiState.value.selectedDayOffset - 1
        _uiState.value = _uiState.value.copy(
            selectedDayOffset = newOffset,
            selectedDateLabel = labelForOffset(newOffset),
        )
        loadStepsForOffset(newOffset)
    }

    /** Navigue vers le mois précédent dans le calendrier (limité à [MAX_CALENDAR_MONTHS_BACK]). */
    fun navigateCalendarPrevious() {
        val newMonth = _uiState.value.calendarMonth.minusMonths(1)
        val limit = YearMonth.now().minusMonths(MAX_CALENDAR_MONTHS_BACK.toLong())
        if (newMonth < limit) return
        _uiState.value = _uiState.value.copy(calendarMonth = newMonth)
        loadCalendarMonth(newMonth)
    }

    /** Navigue vers le mois suivant dans le calendrier (sans dépasser le mois courant). */
    fun navigateCalendarNext() {
        val newMonth = _uiState.value.calendarMonth.plusMonths(1)
        if (newMonth > YearMonth.now()) return
        _uiState.value = _uiState.value.copy(calendarMonth = newMonth)
        loadCalendarMonth(newMonth)
    }

    /**
     * Appelé quand l'utilisateur tape sur un jour du calendrier.
     * Navigue l'anneau vers ce jour (offset depuis aujourd'hui).
     */
    fun onCalendarDayTap(date: LocalDate) {
        val today = LocalDate.now()
        if (date.isAfter(today)) return
        val offset = (date.toEpochDay() - today.toEpochDay()).toInt()
        _uiState.value = _uiState.value.copy(
            selectedDayOffset = offset,
            selectedDateLabel = labelForOffset(offset),
        )
        loadStepsForOffset(offset)
    }

    /**
     * Navigue vers le jour suivant (décalage + 1).
     * Sans effet si on est déjà sur aujourd'hui ([selectedDayOffset] == 0).
     */
    fun goToNextDay() {
        val current = _uiState.value.selectedDayOffset
        if (current >= 0) return
        val newOffset = current + 1
        _uiState.value = _uiState.value.copy(
            selectedDayOffset = newOffset,
            selectedDateLabel = labelForOffset(newOffset),
        )
        loadStepsForOffset(newOffset)
    }

    /**
     * Charge les pas depuis Health Connect pour le jour correspondant à [offset] (0 = aujourd'hui).
     * Requête idempotente : recalcule depuis le début du jour cible.
     * Déclenche également [loadMetrics] pour mettre à jour distance, temps actif et calories.
     */
    private fun loadStepsForOffset(offset: Int) {
        viewModelScope.launch {
            val steps = if (isEmulator()) {
                EMULATOR_STEPS_BY_OFFSET[offset] ?: (5_000L + (offset * -137L).coerceAtLeast(0))
            } else {
                val targetDate = LocalDate.now().plusDays(offset.toLong())
                val from = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                val to = if (offset == 0) Instant.now()
                else targetDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                healthConnectRepository.readSteps(from = from, to = to)
            }
            // Pour aujourd'hui : mémorise la référence HC pour le calcul live et ne recule jamais.
            // Pour aujourd'hui : max pour ne jamais reculer en dessous du live/cache.
            // Pour les jours passés : valeur brute (pas de max — chaque jour a ses propres pas).
            if (offset == 0) {
                hcStepsRef = maxOf(hcStepsRef, steps)
                _uiState.value = _uiState.value.copy(stepsToday = maxOf(_uiState.value.stepsToday, steps))
            } else {
                _uiState.value = _uiState.value.copy(stepsToday = steps)
            }
            loadMetrics(offset, stepsFallback = steps)
        }
    }

    /**
     * Charge distance, temps actif et calories pour le jour correspondant à [offset].
     * [stepsFallback] est utilisé pour estimer le temps actif si aucune session d'exercice n'est trouvée.
     */
    private suspend fun loadMetrics(offset: Int, stepsFallback: Long = 0L) {
        val targetDate = LocalDate.now().plusDays(offset.toLong())
        val distanceKm = healthConnectRepository.readDistanceForDay(targetDate)
        val caloriesKcal = healthConnectRepository.readActiveCaloriesForDay(targetDate)
        val activeMinutes = healthConnectRepository.readActiveMinutesForDay(targetDate, stepsFallback)
        _uiState.value = _uiState.value.copy(
            distanceKm = distanceKm,
            activeMinutes = activeMinutes,
            caloriesKcal = caloriesKcal,
        )
    }

    /**
     * Charge les pas par jour pour le mois [month] depuis Health Connect.
     * Sur émulateur, génère des données mock réalistes basées sur le jour du mois.
     */
    private fun loadCalendarMonth(month: YearMonth) {
        viewModelScope.launch {
            val today = LocalDate.now()
            val stepsMap: Map<LocalDate, Long> = if (isEmulator()) {
                (1..month.lengthOfMonth())
                    .map { month.atDay(it) }
                    .filter { !it.isAfter(today) }
                    .associateWith { date -> emulatorStepsForDay(date) }
            } else {
                val from = month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                val to = if (month == YearMonth.now()) Instant.now()
                else month.atEndOfMonth().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                healthConnectRepository.readStepsByDay(from, to)
            }
            val total = stepsMap.values.sum()
            _uiState.value = _uiState.value.copy(
                calendarSteps = stepsMap,
                calendarTotal = total,
            )
        }
    }

    /**
     * Charge les pas sur la semaine ISO courante (lundi → dimanche) :
     * - currentWeek  : lundi → dimanche de la semaine en cours (index 0=Lu … 6=Di)
     * - previousWeek : lundi → dimanche de la semaine précédente
     * [weekTodayIndex] : position du jour courant dans la grille (0=lundi … 6=dimanche),
     * utilisé pour surligner le bon label en vert sur l'axe X.
     * Cohérent avec le calendrier mensuel qui commence également le lundi.
     */
    private fun loadWeeklyData() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val dayFmt = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())

            // Lundi de la semaine ISO courante
            val weekStart = today.with(java.time.DayOfWeek.MONDAY)
            val prevWeekStart = weekStart.minusWeeks(1)

            // Labels fixes Lu Ma Me Je Ve Sa Di (ne dépendent pas de la date du jour)
            val labels = List(7) { i ->
                val d = weekStart.plusDays(i.toLong())
                d.format(dayFmt).replaceFirstChar { it.uppercaseChar() }.take(2)
            }

            // Indice du jour courant dans la semaine ISO (MONDAY=1 → index 0, SUNDAY=7 → index 6)
            val todayIndex = today.dayOfWeek.value - 1

            if (isEmulator()) {
                val mockCurrent = List(7) { i ->
                    emulatorStepsForDay(weekStart.plusDays(i.toLong()))
                }
                val mockPrev = List(7) { i ->
                    emulatorStepsForDay(prevWeekStart.plusDays(i.toLong()))
                }
                _uiState.value = _uiState.value.copy(
                    currentWeekSteps = mockCurrent,
                    previousWeekSteps = mockPrev,
                    weekDayLabels = labels,
                    weekTodayIndex = todayIndex,
                )
            } else {
                val zone = ZoneId.systemDefault()
                val currFrom = weekStart.atStartOfDay(zone).toInstant()
                val prevFrom = prevWeekStart.atStartOfDay(zone).toInstant()
                val prevTo = weekStart.atStartOfDay(zone).toInstant()

                val currMap = healthConnectRepository.readStepsByDay(currFrom, Instant.now())
                val prevMap = healthConnectRepository.readStepsByDay(prevFrom, prevTo)

                val currentWeek = List(7) { i ->
                    currMap[weekStart.plusDays(i.toLong())] ?: 0L
                }
                val previousWeek = List(7) { i ->
                    prevMap[prevWeekStart.plusDays(i.toLong())] ?: 0L
                }
                _uiState.value = _uiState.value.copy(
                    currentWeekSteps = currentWeek,
                    previousWeekSteps = previousWeek,
                    weekDayLabels = labels,
                    weekTodayIndex = todayIndex,
                )
            }
        }
    }

    /**
     * Charge l'état météo, les prévisions 7 jours et le nom de ville.
     * Sur émulateur : données mock statiques (pas d'appel réseau).
     * Ne fait rien si la permission de localisation n'est pas accordée (hors émulateur).
     */
    private fun loadWeather() {
        viewModelScope.launch {
            if (isEmulator()) {
                _uiState.value = _uiState.value.copy(
                    weatherState = WeatherState.NO_RAIN,
                    dailyForecasts = emulatorDailyForecasts(),
                    cityName = "Paris",
                )
                return@launch
            }

            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return@launch

            val coords = getLastKnownLocation()
            val (lat, lon) = coords ?: (EMULATOR_LATITUDE to EMULATOR_LONGITUDE)

            val state = weatherRepository.getWeatherState(lat, lon)
            val forecasts = weatherRepository.getDailyForecasts(lat, lon)
            val city = getCityName(lat, lon)

            _uiState.value = _uiState.value.copy(
                weatherState = state,
                dailyForecasts = forecasts,
                cityName = city,
            )
        }
    }

    /**
     * Prévisions météo mock sur 7 jours glissants pour l'émulateur.
     * Codes WMO variés pour tester tous les emojis et cas de précipitations.
     */
    private fun emulatorDailyForecasts(): List<DailyForecast> {
        val today = java.time.LocalDate.now()
        val codes  = listOf(0, 1, 3, 61, 80, 2, 95)
        val maxT   = listOf(24.0, 22.0, 19.0, 17.0, 21.0, 25.0, 23.0)
        val minT   = listOf(14.0, 13.0, 11.0, 10.0, 12.0, 15.0, 14.0)
        val precip = listOf(0.0, 0.0, 0.0, 5.2, 1.8, 0.0, 3.0)
        return List(7) { i ->
            DailyForecast(
                date = today.plusDays(i.toLong()),
                weatherCode = codes[i],
                tempMaxCelsius = maxT[i],
                tempMinCelsius = minT[i],
                precipitationMm = precip[i],
                hourlyForecasts = emulatorHourlyForecasts(codes[i], minT[i], maxT[i]),
            )
        }
    }

    /**
     * Génère des créneaux horaires mock (0h–23h) pour l'émulateur.
     * La température évolue entre [minT] et [maxT] selon une courbe sinusoïdale simplifiée.
     */
    private fun emulatorHourlyForecasts(
        dayCode: Int,
        minT: Double,
        maxT: Double,
    ): List<HourlyForecast> = List(24) { h ->
        val ratio = ((h - 6).coerceIn(0, 12) / 12.0)
        val temp = minT + (maxT - minT) * ratio
        val precip = if (dayCode in 51..99) (20 + h % 3 * 10).coerceAtMost(80) else 0
        HourlyForecast(
            hour = h,
            tempCelsius = temp,
            weatherCode = dayCode,
            precipProbability = precip,
        )
    }

    /**
     * Récupère la dernière position connue via FusedLocationProviderClient.
     * Retourne null si indisponible ou si la permission a été révoquée entre-temps.
     */
    private suspend fun getLastKnownLocation(): Pair<Double, Double>? =
        suspendCancellableCoroutine { cont ->
            try {
                LocationServices.getFusedLocationProviderClient(context)
                    .lastLocation
                    .addOnSuccessListener { location ->
                        cont.resume(location?.let { it.latitude to it.longitude })
                    }
                    .addOnFailureListener {
                        cont.resume(null)
                    }
            } catch (e: SecurityException) {
                cont.resume(null)
            }
        }

    /**
     * Reverse geocoding via Android [Geocoder] pour obtenir le nom de la ville.
     * Retourne null si le Geocoder est indisponible ou si aucun résultat n'est trouvé.
     */
    @Suppress("DEPRECATION")
    private suspend fun getCityName(lat: Double, lon: Double): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!Geocoder.isPresent()) return@runCatching null
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                addresses?.firstOrNull()?.locality
                    ?: addresses?.firstOrNull()?.subAdminArea
            }.onFailure { Log.w(TAG, "getCityName a échoué pour ($lat, $lon)", it) }
                .getOrNull()
        }

    /**
     * Construit le label de date affiché au-dessus de l'anneau selon le décalage en jours.
     * Résout les libellés "Aujourd'hui" / "Hier" depuis les ressources pour respecter la locale.
     * Délègue le formatage des dates passées à [labelForOffset] (companion).
     */
    internal fun labelForOffset(offset: Int): String =
        labelForOffset(
            offset = offset,
            todayLabel = context.getString(R.string.activity_day_today),
            yesterdayLabel = context.getString(R.string.activity_day_yesterday),
        )

    companion object {
        private const val TAG = "ActivityViewModel"

        /**
         * Version testable de labelForOffset sans dépendance au contexte Android.
         * Exemples : 0 → [todayLabel], -1 → [yesterdayLabel], -5 → "Lun. 23 juin" (locale système).
         *
         * @param offset Décalage en jours par rapport à aujourd'hui (0, -1, -2…).
         * @param todayLabel Libellé localisé pour aujourd'hui (ex. "Aujourd'hui", "Today").
         * @param yesterdayLabel Libellé localisé pour hier (ex. "Hier", "Yesterday").
         */
        internal fun labelForOffset(offset: Int, todayLabel: String, yesterdayLabel: String): String =
            when (offset) {
                0 -> todayLabel
                -1 -> yesterdayLabel
                else -> {
                    val date = LocalDate.now().plusDays(offset.toLong())
                    val formatter = DateTimeFormatter.ofPattern("EEE d MMMM", Locale.getDefault())
                    date.format(formatter).replaceFirstChar { it.uppercaseChar() }
                }
            }
    }
}
