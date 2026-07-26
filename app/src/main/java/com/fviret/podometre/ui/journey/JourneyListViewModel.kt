package com.fviret.podometre.ui.journey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fviret.podometre.data.health.HealthConnectRepository
import com.fviret.podometre.data.journey.JourneyProgressRepository
import com.fviret.podometre.data.preferences.UserPreferences
import com.fviret.podometre.data.preferences.UserPreferencesRepository
import com.fviret.podometre.domain.JourneyData
import com.fviret.podometre.domain.model.Journey
import com.fviret.podometre.domain.model.JourneyProgress
import com.fviret.podometre.domain.model.Milestone
import com.fviret.podometre.domain.model.progressPercent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.ceil

/**
 * Données calculées pour la card du trajet en cours épinglée en tête du catalogue.
 *
 * @property journey Le trajet en cours
 * @property progress La progression actuelle
 * @property globalPercent Pourcentage global du trajet (0.0 à 1.0)
 * @property lastMilestoneLabel Label du dernier jalon franchi (ou "Départ" si avant le 1er)
 * @property nextMilestoneLabel Label du prochain jalon (ou "Arrivée" si plus de jalons)
 * @property nextMilestoneKm Distance du prochain jalon depuis le départ (ou totalKm)
 * @property lastMilestoneKm Distance du dernier jalon franchi (ou 0.0)
 * @property segmentProgress Position dans le segment courant (0.0 à 1.0)
 * @property kmToNextMilestone Distance restante jusqu'au prochain jalon (>= 0)
 * @property etaDate Date d'arrivée estimée, null si historique insuffisant
 */
data class ActiveJourneyCardData(
    val journey: Journey,
    val progress: JourneyProgress,
    val globalPercent: Float,
    val lastMilestoneLabel: String,
    val nextMilestoneLabel: String,
    val nextMilestoneKm: Double,
    val lastMilestoneKm: Double,
    val segmentProgress: Float,
    val kmToNextMilestone: Double,
    val etaDate: LocalDate?
)

/**
 * ViewModel du catalogue des trajets.
 * Expose la progression de chaque trajet, les préférences utiles (trajets complétés, trajet actif),
 * et les données calculées pour la card du trajet en cours.
 * Équivalent iOS : JourneyPickerView + JourneyPickerViewModel.
 */
@HiltViewModel
class JourneyListViewModel @Inject constructor(
    private val journeyProgressRepository: JourneyProgressRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val healthConnectRepository: HealthConnectRepository,
) : ViewModel() {

    /** Map journeyId → progression courante. */
    val progressMap: StateFlow<Map<String, JourneyProgress>> = journeyProgressRepository.progressMap
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Préférences utilisateur (completedJourneyIds, activeJourneyId…). */
    val preferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    /** Données calculées pour la card du trajet en cours, null si aucun trajet actif. */
    private val _activeJourneyCard = MutableStateFlow<ActiveJourneyCardData?>(null)
    val activeJourneyCard: StateFlow<ActiveJourneyCardData?> = _activeJourneyCard

    init {
        // Recalculer la card dès que la progression ou les préférences changent
        viewModelScope.launch {
            combine(progressMap, preferences) { progress, prefs -> Pair(progress, prefs) }
                .collect { (progress, prefs) ->
                    _activeJourneyCard.value = computeActiveJourneyCard(progress, prefs)
                }
        }
    }

    /**
     * Calcule les données de la card du trajet en cours.
     * Le trajet actif est celui ayant une progression, non terminé, < 100 %,
     * et le plus récemment mis à jour s'il y en a plusieurs.
     * Retourne null si aucun trajet actif n'est trouvé.
     */
    private suspend fun computeActiveJourneyCard(
        progressMap: Map<String, JourneyProgress>,
        prefs: UserPreferences
    ): ActiveJourneyCardData? {
        // Trouver le trajet actif : progression existante, non terminé, < 100 %
        val activeEntry = progressMap.entries
            .filter { (journeyId, prog) ->
                journeyId !in prefs.completedJourneyIds &&
                    prog.totalKm < (JourneyData.all.find { it.id.toString() == journeyId }?.totalKm ?: Double.MAX_VALUE)
            }
            .maxByOrNull { (_, prog) -> prog.lastUpdatedMs }
            ?: return null

        val (journeyId, progress) = activeEntry
        val journey = JourneyData.all.find { it.id.toString() == journeyId } ?: return null

        val globalPercent = journey.progressPercent(progress).toFloat()

        // Calculer les bornes du segment courant
        val sortedMilestones = journey.milestones.sortedBy { it.km }
        val lastUnlocked: Milestone? = sortedMilestones
            .filter { it.id.toString() in progress.unlockedMilestoneIds }
            .maxByOrNull { it.km }
        val nextMilestone: Milestone? = sortedMilestones
            .firstOrNull { it.id.toString() !in progress.unlockedMilestoneIds }

        val lastMilestoneKm = lastUnlocked?.km ?: 0.0
        val lastMilestoneLabel = lastUnlocked?.label ?: "Départ"
        val nextMilestoneKm = nextMilestone?.km ?: journey.totalKm
        val nextMilestoneLabel = nextMilestone?.label ?: "Arrivée"

        // Position dans le segment courant (bornée 0…1)
        val segmentLength = (nextMilestoneKm - lastMilestoneKm).coerceAtLeast(0.001)
        val progressInSegment = (progress.totalKm - lastMilestoneKm).coerceAtLeast(0.0)
        val segmentProgress = (progressInSegment / segmentLength).coerceIn(0.0, 1.0).toFloat()

        // Distance restante jusqu'au prochain jalon (jamais négative)
        val kmToNextMilestone = (nextMilestoneKm - progress.totalKm).coerceAtLeast(0.0)

        // Calcul ETA : moyenne pas 6 derniers jours pleins
        val avgDailySteps = healthConnectRepository.readAverageDailyStepsLast6Days()
        val etaDate = avgDailySteps?.let { avg ->
            if (avg <= 0L) return@let null
            val kmRemaining = journey.totalKm - progress.totalKm
            val daysRemaining = ceil(kmRemaining / (avg * 0.0008)).toLong()
            LocalDate.now().plusDays(daysRemaining)
        }

        return ActiveJourneyCardData(
            journey = journey,
            progress = progress,
            globalPercent = globalPercent,
            lastMilestoneLabel = lastMilestoneLabel,
            nextMilestoneLabel = nextMilestoneLabel,
            nextMilestoneKm = nextMilestoneKm,
            lastMilestoneKm = lastMilestoneKm,
            segmentProgress = segmentProgress,
            kmToNextMilestone = kmToNextMilestone,
            etaDate = etaDate
        )
    }

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
