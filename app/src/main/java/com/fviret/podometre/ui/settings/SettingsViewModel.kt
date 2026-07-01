package com.fviret.podometre.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fviret.podometre.data.preferences.UserPreferences
import com.fviret.podometre.data.preferences.UserPreferencesRepository
import com.fviret.podometre.ui.theme.AppColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Valeurs autorisées pour l'objectif quotidien de pas (5 000 à 20 000 par pas de 500). */
val STEP_GOAL_OPTIONS: List<Int> = (5_000..20_000 step 500).toList()

/**
 * ViewModel de l'écran Paramètres.
 * Expose les préférences utilisateur et les méthodes de mise à jour.
 * Équivalent iOS : SettingsViewModel.swift
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    /** Préférences utilisateur en lecture seule pour les Composables. */
    val userPreferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferences(),
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
     * La valeur est propagée immédiatement via [MainViewModel] au thème racine.
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
}
