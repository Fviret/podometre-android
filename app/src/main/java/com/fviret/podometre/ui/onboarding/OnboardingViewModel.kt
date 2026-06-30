package com.fviret.podometre.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fviret.podometre.data.health.HealthConnectRepository
import com.fviret.podometre.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val currentPage: Int = 0,
    val selectedGoal: Int = 8_000,
    val isCompleted: Boolean = false,
    val healthPermissionsGranted: Boolean = false,
)

/**
 * ViewModel de l'écran d'onboarding.
 * Gère la progression entre les 4 slides et persiste le choix d'objectif
 * et le flag [hasCompletedOnboarding] à la fin du flux.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val healthConnectRepository: HealthConnectRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    companion object {
        const val TOTAL_PAGES = 4

        /** Index de la slide des permissions (slide 3) — déclenche la demande Health Connect. */
        const val PERMISSIONS_PAGE_INDEX = 2
        val GOAL_OPTIONS = listOf(5_000, 8_000, 10_000, 15_000, 20_000)
    }

    /** Retourne true si Health Connect est disponible — utilisé pour décider de lancer la demande de permissions. */
    fun isHealthConnectAvailable(): Boolean = healthConnectRepository.isAvailable()

    /** Avance à la slide suivante. */
    fun nextPage() {
        val current = _uiState.value.currentPage
        if (current < TOTAL_PAGES - 1) {
            _uiState.value = _uiState.value.copy(currentPage = current + 1)
        }
    }

    /**
     * Enregistre le résultat de la demande de permissions Health Connect
     * (accordées ou refusées) puis avance à la slide suivante.
     * Si refusé, l'app continuera en mode dégradé (compteur à 0).
     */
    fun onHealthPermissionsResult(granted: Set<String>) {
        _uiState.value = _uiState.value.copy(
            healthPermissionsGranted = granted.containsAll(HealthConnectRepository.PERMISSIONS)
        )
        nextPage()
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
