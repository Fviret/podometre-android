package com.fviret.podometre.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fviret.podometre.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val currentPage: Int = 0,
    val selectedGoal: Int = 10_000,
    val isCompleted: Boolean = false,
)

/**
 * ViewModel de l'écran d'onboarding.
 * Gère la progression entre les 4 slides et persiste le choix d'objectif
 * et le flag [hasCompletedOnboarding] à la fin du flux.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    companion object {
        const val TOTAL_PAGES = 4
        val GOAL_OPTIONS = listOf(5_000, 6_000, 7_000, 8_000, 9_000, 10_000,
            11_000, 12_000, 15_000, 20_000)
    }

    /** Avance à la slide suivante. */
    fun nextPage() {
        val current = _uiState.value.currentPage
        if (current < TOTAL_PAGES - 1) {
            _uiState.value = _uiState.value.copy(currentPage = current + 1)
        }
    }

    /** Met à jour l'objectif choisi par l'utilisateur sur la slide 4. */
    fun setGoal(goal: Int) {
        _uiState.value = _uiState.value.copy(selectedGoal = goal)
    }

    /**
     * Termine l'onboarding : persiste l'objectif et le flag hasCompletedOnboarding,
     * puis signale la complétion au Composable via [uiState.isCompleted].
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            userPreferencesRepository.setDailyStepGoal(_uiState.value.selectedGoal)
            userPreferencesRepository.setOnboardingCompleted()
            _uiState.value = _uiState.value.copy(isCompleted = true)
        }
    }
}
