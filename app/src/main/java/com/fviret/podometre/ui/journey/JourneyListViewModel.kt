package com.fviret.podometre.ui.journey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fviret.podometre.data.journey.JourneyProgressRepository
import com.fviret.podometre.data.preferences.UserPreferences
import com.fviret.podometre.data.preferences.UserPreferencesRepository
import com.fviret.podometre.domain.model.JourneyProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel du catalogue des trajets.
 * Expose la progression de chaque trajet et les préférences utiles (trajets complétés, trajet actif).
 * Équivalent iOS : JourneyPickerView + JourneyPickerViewModel.
 */
@HiltViewModel
class JourneyListViewModel @Inject constructor(
    private val journeyProgressRepository: JourneyProgressRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    /** Map journeyId → progression courante. */
    val progressMap: StateFlow<Map<String, JourneyProgress>> = journeyProgressRepository.progressMap
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Préférences utilisateur (completedJourneyIds, activeJourneyId…). */
    val preferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    /**
     * Démarre un nouveau trajet :
     * - Crée la [JourneyProgress] initiale si inexistante
     * - Persiste l'ID comme trajet actif dans DataStore
     */
    fun startJourney(journeyId: String) {
        viewModelScope.launch {
            journeyProgressRepository.startJourney(journeyId)
            userPreferencesRepository.setActiveJourneyId(journeyId)
        }
    }

    /**
     * Abandonne le trajet actif et en démarre un nouveau.
     * Efface la progression de l'ancien trajet, démarre le nouveau.
     */
    fun switchJourney(abandonId: String, newJourneyId: String) {
        viewModelScope.launch {
            journeyProgressRepository.deleteProgress(abandonId)
            userPreferencesRepository.setActiveJourneyId(null)
            journeyProgressRepository.startJourney(newJourneyId)
            userPreferencesRepository.setActiveJourneyId(newJourneyId)
        }
    }
}
