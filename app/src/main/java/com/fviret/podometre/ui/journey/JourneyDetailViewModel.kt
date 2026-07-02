package com.fviret.podometre.ui.journey

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fviret.podometre.data.journey.JourneyProgressRepository
import com.fviret.podometre.data.preferences.UserPreferences
import com.fviret.podometre.data.preferences.UserPreferencesRepository
import com.fviret.podometre.domain.JourneyData
import com.fviret.podometre.domain.model.Journey
import com.fviret.podometre.domain.model.JourneyProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel de l'écran de détail d'un trajet en cours.
 * Expose la progression en temps réel et les jalons nouvellement débloqués.
 * Équivalent iOS : JourneyDetailView + JourneyPickerViewModel.
 */
@HiltViewModel
class JourneyDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val journeyProgressRepository: JourneyProgressRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val journeyId: String = checkNotNull(savedStateHandle["journeyId"])

    /** Trajet correspondant à l'ID de navigation. Null si introuvable (ne devrait pas arriver). */
    val journey: Journey? = JourneyData.findById(journeyId)

    /** Progression courante du trajet, mise à jour à chaque sync. */
    val progress: StateFlow<JourneyProgress?> = journeyProgressRepository.progressMap
        .map { it[journeyId] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Préférences utilisateur (completedJourneyIds…). */
    val preferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    /**
     * Émet l'ID d'un jalon nouvellement débloqué pour ce trajet.
     * Le Composable écoute ce flow pour afficher la BottomSheet de notification.
     */
    val newlyUnlockedMilestoneId = journeyProgressRepository.milestoneUnlocked
        .filter { (jid, _) -> jid == journeyId }
        .map { (_, milestoneId) -> milestoneId }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000))
}
