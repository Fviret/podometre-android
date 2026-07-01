package com.fviret.podometre.ui.activity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.fviret.podometre.data.health.HealthConnectRepository
import com.fviret.podometre.data.preferences.UserPreferences
import com.fviret.podometre.data.preferences.UserPreferencesRepository
import com.fviret.podometre.data.weather.DailyForecast
import com.fviret.podometre.data.weather.WeatherRepository
import com.fviret.podometre.data.weather.WeatherState
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

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
    /** Label affiché au-dessus de l'anneau : "Aujourd'hui" / "Hier" / "Lun. 23 juin". */
    val selectedDateLabel: String = "Aujourd'hui",
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
    @ApplicationContext private val context: Context,
) : ViewModel() {

    /** Préférences utilisateur exposées en StateFlow pour les Composables. */
    val userPreferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferences()
        )

    private val _uiState = MutableStateFlow(
        ActivityUiState(isHealthConnectAvailable = healthConnectRepository.isAvailable())
    )

    /** État complet de l'écran. */
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

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
        loadWeather()
        SyncStepsWorker.schedule(WorkManager.getInstance(context))
    }

    /** Rafraîchit les pas au retour en foreground (ON_RESUME). */
    fun refreshSteps() {
        loadStepsForOffset(_uiState.value.selectedDayOffset)
    }

    /** Rafraîchit la météo et les prévisions au retour en foreground. */
    fun refreshWeather() {
        loadWeather()
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
            _uiState.value = _uiState.value.copy(stepsToday = steps)
        }
    }

    /**
     * Charge l'état météo, les prévisions 7 jours et le nom de ville.
     * Ne fait rien si la permission de localisation n'est pas accordée.
     */
    private fun loadWeather() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return

        viewModelScope.launch {
            val coords = getLastKnownLocation()
            val (lat, lon) = if (isEmulator() || coords == null) {
                EMULATOR_LATITUDE to EMULATOR_LONGITUDE
            } else {
                coords
            }

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
            }.getOrNull()
        }

    companion object {
        /**
         * Construit le label de date affiché au-dessus de l'anneau selon le décalage en jours.
         * Exemples : 0 → "Aujourd'hui", -1 → "Hier", -5 → "Lun. 23 juin".
         */
        fun labelForOffset(offset: Int): String = when (offset) {
            0 -> "Aujourd'hui"
            -1 -> "Hier"
            else -> {
                val date = LocalDate.now().plusDays(offset.toLong())
                val formatter = DateTimeFormatter.ofPattern("EEE d MMMM", Locale.FRENCH)
                date.format(formatter).replaceFirstChar { it.uppercaseChar() }
            }
        }
    }
}
