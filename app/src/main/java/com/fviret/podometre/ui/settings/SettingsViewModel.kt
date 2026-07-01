package com.fviret.podometre.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fviret.podometre.data.preferences.UserPreferences
import com.fviret.podometre.data.preferences.UserPreferencesRepository
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
}
