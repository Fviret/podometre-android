package com.fviret.podometre.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fviret.podometre.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel racine de l'application.
 * Détermine si l'onboarding doit être affiché au démarrage.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    /**
     * true si l'utilisateur n'a pas encore complété l'onboarding.
     * null = état non encore chargé (splash implicite).
     */
    val showOnboarding = userPreferencesRepository.userPreferences
        .map { it.hasCompletedOnboarding.not() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )
}
