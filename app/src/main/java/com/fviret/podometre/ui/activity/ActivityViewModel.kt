package com.fviret.podometre.ui.activity

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.fviret.podometre.data.health.HealthConnectRepository
import com.fviret.podometre.data.preferences.UserPreferences
import com.fviret.podometre.data.preferences.UserPreferencesRepository
import com.fviret.podometre.data.weather.WeatherData
import com.fviret.podometre.data.weather.WeatherRepository
import com.fviret.podometre.util.isEmulator
import com.fviret.podometre.worker.SyncStepsWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** Pas mockés pour l'émulateur, faute de Health Connect réellement alimenté (KAN-19/20). */
private const val EMULATOR_MOCK_STEPS = 7_430L

/**
 * État de l'écran Activité.
 * Sera enrichi au fil des tickets KAN-19 à KAN-25.
 */
data class ActivityUiState(
    val stepGoal: Int = 10_000,
    val stepsToday: Long = 0L,
    val weather: WeatherData? = null,
    val isHealthConnectAvailable: Boolean = false,
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

    /** État complet de l'écran — sera enrichi dans KAN-20 à KAN-25. */
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferences.collect { prefs ->
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                // Si le worker a déjà mis à jour le cache aujourd'hui et que l'UI n'a pas encore de données,
                // on utilise la valeur cachée le temps que la requête HC aboutisse.
                val cachedSteps = if (prefs.cachedStepsTodayDate == today) prefs.cachedStepsToday else 0L
                _uiState.value = _uiState.value.copy(
                    stepGoal = prefs.dailyStepGoal,
                    stepsToday = maxOf(_uiState.value.stepsToday, cachedSteps)
                )
            }
        }
        loadStepsToday()
        SyncStepsWorker.schedule(WorkManager.getInstance(context))
    }

    /**
     * Relit les pas du jour depuis Health Connect.
     * Appelé au lancement, au retour en foreground (ON_RESUME dans ActivityScreen), et par le worker via le cache DataStore.
     */
    fun refreshSteps() {
        loadStepsToday()
    }

    /** Charge le nombre de pas du jour courant depuis Health Connect (requête idempotente). */
    private fun loadStepsToday() {
        viewModelScope.launch {
            val steps = if (isEmulator()) {
                EMULATOR_MOCK_STEPS
            } else {
                val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
                healthConnectRepository.readSteps(from = startOfDay, to = Instant.now())
            }
            _uiState.value = _uiState.value.copy(stepsToday = steps)
        }
    }
}
