package com.fviret.podometre.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fviret.podometre.data.health.HealthConnectRepository
import com.fviret.podometre.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/**
 * Modèle d'état UI pour l'écran Historique.
 */
data class HistoryUiState(
    /** Totaux de pas par semaine (lundi, total) — 10 dernières semaines. */
    val weeklyTotals: List<Pair<LocalDate, Long>> = emptyList(),
    /** Indice de la semaine sélectionnée dans [weeklyTotals] pour le détail. */
    val selectedWeekIndex: Int? = null,
    /** Totaux mensuels par mois pour [displayedYear]. */
    val monthlyTotals: Map<Month, Long> = emptyMap(),
    /** Année affichée dans la section mensuelle. */
    val displayedYear: Int = LocalDate.now().year,
    /** Premier jour de l'historique disponible (pour savoir jusqu'où reculer). */
    val oldestYear: Int = LocalDate.now().year,
    /** Record : meilleur jour (pas, date). */
    val bestDay: Pair<Long, LocalDate?> = 0L to null,
    /** Record : meilleure semaine (pas, lundi de la semaine). */
    val bestWeek: Pair<Long, LocalDate?> = 0L to null,
    /** Record : meilleur mois (pas, premier jour du mois). */
    val bestMonth: Pair<Long, LocalDate?> = 0L to null,
    /** Plus longue série de jours où l'objectif a été atteint. */
    val longestStreak: Pair<Int, LocalDate?> = 0 to null,
    /** Total cumulé de tous les pas enregistrés dans Health Connect. */
    val totalCumulativeSteps: Long = 0L,
    /** Total cumulé de distance en km. */
    val totalCumulativeKm: Double = 0.0,
    /** Objectif de pas journalier de l'utilisateur. */
    val dailyGoal: Int = 10_000,
    /** État de chargement global. */
    val isLoading: Boolean = true,
)

/**
 * ViewModel de l'écran Historique (KAN-135–139).
 * Charge les données d'historique depuis [HealthConnectRepository].
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val healthRepo: HealthConnectRepository,
    private val prefsRepo: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    /** Charge toutes les données en parallèle. */
    private fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val prefs = prefsRepo.userPreferences.first()
            val goal = prefs.dailyStepGoal.toLong()

            val weeklyDef = async { healthRepo.readWeeklyStepTotals(10) }
            val monthlyDef = async { healthRepo.readMonthlyStepsByYear(LocalDate.now().year) }
            val bestDayDef = async { healthRepo.readBestDay() }
            val bestWeekDef = async { healthRepo.readBestWeek() }
            val bestMonthDef = async { healthRepo.readBestMonth() }
            val longestStreakDef = async { healthRepo.readLongestStreak(goal) }
            val totalStepsDef = async { healthRepo.readTotalCumulativeSteps() }
            val totalKmDef = async { healthRepo.readTotalCumulativeDistance() }
            val oldestYearDef = async { healthRepo.readEarliestDataYear() }

            _uiState.value = HistoryUiState(
                weeklyTotals = weeklyDef.await(),
                monthlyTotals = monthlyDef.await(),
                displayedYear = LocalDate.now().year,
                oldestYear = oldestYearDef.await(),
                bestDay = bestDayDef.await(),
                bestWeek = bestWeekDef.await(),
                bestMonth = bestMonthDef.await(),
                longestStreak = longestStreakDef.await(),
                totalCumulativeSteps = totalStepsDef.await(),
                totalCumulativeKm = totalKmDef.await(),
                dailyGoal = prefs.dailyStepGoal,

                isLoading = false,
            )
        }
    }

    /** Sélectionne/désélectionne une semaine dans le graphe de tendance. */
    fun selectWeek(index: Int) {
        val current = _uiState.value.selectedWeekIndex
        _uiState.value = _uiState.value.copy(selectedWeekIndex = if (current == index) null else index)
    }

    /** Navigue vers l'année précédente dans la section mensuelle. */
    fun previousYear() {
        val year = _uiState.value.displayedYear
        if (year <= _uiState.value.oldestYear) return
        val newYear = year - 1
        _uiState.value = _uiState.value.copy(displayedYear = newYear, isLoading = true)
        viewModelScope.launch {
            val monthly = healthRepo.readMonthlyStepsByYear(newYear)
            _uiState.value = _uiState.value.copy(monthlyTotals = monthly, isLoading = false)
        }
    }

    /** Navigue vers l'année suivante dans la section mensuelle. */
    fun nextYear() {
        val year = _uiState.value.displayedYear
        if (year >= LocalDate.now().year) return
        val newYear = year + 1
        _uiState.value = _uiState.value.copy(displayedYear = newYear, isLoading = true)
        viewModelScope.launch {
            val monthly = healthRepo.readMonthlyStepsByYear(newYear)
            _uiState.value = _uiState.value.copy(monthlyTotals = monthly, isLoading = false)
        }
    }
}
