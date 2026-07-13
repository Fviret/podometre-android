package com.fviret.podometre.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fviret.podometre.data.aphorism.Aphorism
import com.fviret.podometre.data.aphorism.AphorismRepository
import com.fviret.podometre.data.health.HealthConnectRepository
import com.fviret.podometre.data.journey.JourneyProgressRepository
import com.fviret.podometre.data.preferences.UserPreferences
import com.fviret.podometre.data.preferences.UserPreferencesRepository
import com.fviret.podometre.domain.JourneyData
import com.fviret.podometre.ui.theme.AppColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Valeurs autorisées pour l'objectif quotidien de pas (5 000 à 20 000 par pas de 500). */
val STEP_GOAL_OPTIONS: List<Int> = (5_000..20_000 step 500).toList()

/** Seuils de pas pour les 6 badges de performance. */
val STEP_BADGE_THRESHOLDS: List<Long> = listOf(5_000L, 10_000L, 20_000L, 30_000L, 50_000L, 100_000L)

/**
 * ViewModel de l'écran Paramètres.
 * Expose les préférences utilisateur et les méthodes de mise à jour.
 * Équivalent iOS : SettingsViewModel.swift
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val healthConnectRepository: HealthConnectRepository,
    private val journeyProgressRepository: JourneyProgressRepository,
    private val aphorismRepository: AphorismRepository,
) : ViewModel() {

    /** Préférences utilisateur en lecture seule pour les Composables. */
    val userPreferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferences(),
        )

    /**
     * Nombre de jours par seuil de pas dans tout l'historique Health Connect.
     * Chargé une seule fois au démarrage du ViewModel.
     * Map seuil (Long) → nombre de jours (Int).
     */
    val stepBadgeCounts: StateFlow<Map<Long, Int>> = MutableStateFlow<Map<Long, Int>>(emptyMap()).also { flow ->
        viewModelScope.launch {
            flow.value = healthConnectRepository.readStepBadgeCounts(STEP_BADGE_THRESHOLDS)
        }
    }.asStateFlow()

    /**
     * IDs des trajets entièrement complétés (totalKm >= Journey.totalKm).
     * Déterminé en croisant [JourneyProgressRepository.progressMap] avec [JourneyData.all].
     */
    val completedJourneyIds: StateFlow<Set<String>> = journeyProgressRepository.progressMap
        .map { map ->
            map.entries
                .filter { (journeyId, progress) ->
                    val journey = JourneyData.findById(journeyId) ?: return@filter false
                    progress.totalKm >= journey.totalKm
                }
                .map { it.key }
                .toSet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    @OptIn(ExperimentalCoroutinesApi::class)
    val streak: StateFlow<Int> = userPreferences
        .flatMapLatest { prefs ->
            flow { emit(healthConnectRepository.computeStreak(prefs.dailyStepGoal.toLong())) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )

    /**
     * Persiste le nouvel objectif quotidien de pas dans DataStore.
     * Déclenche une mise à jour réactive dans tous les ViewModels qui observent les préférences.
     */
    fun updateDailyStepGoal(goal: Int) {
        if (goal !in STEP_GOAL_OPTIONS) return
        viewModelScope.launch {
            userPreferencesRepository.setDailyStepGoal(goal)
        }
    }

    /**
     * Persiste l'identifiant de couleur de l'anneau dans DataStore.
     * Seuls les IDs présents dans [AppColors.ringColorOptions] sont acceptés.
     */
    fun updateRingColorId(colorId: String) {
        if (colorId !in AppColors.ringColorOptions) return
        viewModelScope.launch {
            userPreferencesRepository.setRingColorId(colorId)
        }
    }

    /**
     * Bascule le mode sombre / clair et le persiste dans DataStore.
     * La valeur est propagée immédiatement via [com.fviret.podometre.ThemeViewModel] au thème racine.
     */
    fun updateDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDarkMode(enabled)
        }
    }

    /**
     * Active ou désactive les notifications d'objectif journalier.
     * Appelé après vérification de la permission POST_NOTIFICATIONS dans le Composable.
     */
    fun onToggleGoalNotifications(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setNotificationsEnabled(enabled) }
    }

    /**
     * Active ou désactive les notifications de progression des trajets.
     * Appelé après vérification de la permission POST_NOTIFICATIONS dans le Composable.
     */
    fun onToggleJourneyNotifications(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setJourneyNotificationsEnabled(enabled) }
    }

    /** Affiche ou masque la bannière météo + prévisions 7 jours sur l'écran Activité. */
    fun updateShowWeatherForecast(show: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setShowWeatherForecast(show) }
    }

    /** Affiche ou masque le calendrier mensuel sur l'écran Activité. */
    fun updateShowMonthCalendar(show: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setShowMonthCalendar(show) }
    }

    /** Affiche ou masque le graphe comparatif hebdomadaire sur l'écran Activité. */
    fun updateShowWeeklyChart(show: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setShowWeeklyChart(show) }
    }

    /**
     * Active ou désactive la popup "Pensée du jour".
     * Si réactivée, remet à zéro la garde "1×/jour" afin que la popup se ré-affiche
     * le jour même au retour sur l'écran principal (sans réinstall ni redémarrage).
     */
    fun updateAphorismEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAphorismEnabled(enabled)
            if (enabled) userPreferencesRepository.setLastAphorismDate("")
        }
    }

    /** L'aphorisme du jour, chargé une seule fois au démarrage. */
    val todayAphorism: Aphorism = aphorismRepository.todayAphorism()
}
