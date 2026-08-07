package com.fviret.podometre.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fviret.podometre.data.health.HealthConnectRepository
import com.fviret.podometre.data.health.SensorStepHistoryRepository
import com.fviret.podometre.data.journey.JourneySyncResult
import com.fviret.podometre.data.journey.JourneyProgressRepository
import com.fviret.podometre.data.preferences.UserPreferencesRepository
import com.fviret.podometre.domain.JourneyData
import com.fviret.podometre.domain.model.stepsToKm
import com.fviret.podometre.util.isEmulator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Worker périodique (15 min) qui lit la distance parcourue depuis Health Connect
 * et met à jour la progression du trajet actif.
 * Sur émulateur, injecte des données mock réalistes sans appel HC.
 * Équivalent iOS : JourneyProgressService + HKObserverQuery background delivery.
 *
 * Limites connues (Android) :
 * - Doze mode : WorkManager respecte les fenêtres Doze ; les exécutions peuvent être regroupées
 *   et retardées de plusieurs heures si l'appareil est immobile et non branché.
 * - Quota d'exécution en arrière-plan (API 26+) : le système limite le temps CPU disponible
 *   pour les apps en arrière-plan ; un worker trop long peut être tué.
 * - App fermée par l'utilisateur (Force Stop) : WorkManager ne peut plus s'exécuter jusqu'à
 *   la prochaine ouverture manuelle de l'app.
 * - Intervalle minimum Android : 15 minutes (WorkManager ignore les intervalles plus courts).
 * - Notifications de jalons : une seule notification par exécution du worker ; si plusieurs jalons
 *   sont franchis entre deux exécutions, ils sont tous notifiés dans la même passe (avec délai
 *   inter-notification [INTER_NOTIFICATION_DELAY_MS]).
 */
@HiltWorker
class SyncJourneyWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val healthConnectRepository: HealthConnectRepository,
    private val sensorStepHistoryRepository: SensorStepHistoryRepository,
    private val journeyProgressRepository: JourneyProgressRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val journeyNotificationService: JourneyNotificationService,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (isEmulator()) {
            journeyProgressRepository.injectEmulatorMock()
            return Result.success()
        }

        val prefs = userPreferencesRepository.userPreferences.first()
        val activeJourneyId = prefs.activeJourneyId ?: return Result.success()

        val journey = JourneyData.findById(activeJourneyId) ?: return Result.success()
        val progress = journeyProgressRepository.getProgress(activeJourneyId) ?: return Result.success()

        val startInstant = Instant.ofEpochMilli(progress.startDateMs)
        val hcKm = healthConnectRepository.readDistance(from = startInstant)
        val fallbackKm = fallbackDistanceFromSensorSteps(startInstant)
        val newKm = maxOf(hcKm, fallbackKm)

        val result = journeyProgressRepository.syncJourney(journey, newKm)

        if (prefs.journeyNotificationsEnabled) {
            result.newlyUnlockedMilestones.forEachIndexed { index, milestone ->
                if (index > 0) delay(INTER_NOTIFICATION_DELAY_MS)
                journeyNotificationService.notifyMilestoneUnlocked(milestone)
            }
            if (result.isNewlyCompleted) {
                if (result.newlyUnlockedMilestones.isNotEmpty()) delay(INTER_NOTIFICATION_DELAY_MS)
                journeyNotificationService.notifyJourneyCompleted(journey)
            }
        }

        return Result.success()
    }

    /**
     * Estime une distance de repli (km) depuis les pas capteur locaux ([SensorStepHistoryRepository])
     * entre [startInstant] et maintenant, via la constante [com.fviret.podometre.domain.model.KM_PER_STEP].
     * Utilisé quand Health Connect ne progresse pas (appareil OEM sans écriture HC, voir KAN-156/KAN-158) —
     * Health Connect reste toujours prioritaire (donnée GPS plus précise), ce fallback ne fait
     * qu'apporter un plancher (`maxOf`) sans jamais faire régresser la progression.
     */
    private suspend fun fallbackDistanceFromSensorSteps(startInstant: Instant): Double {
        val startDate = startInstant.atZone(ZoneId.systemDefault()).toLocalDate()
        val steps = sensorStepHistoryRepository.readStepsSince(from = startDate)
        return stepsToKm(steps)
    }

    companion object {
        private const val WORK_NAME = "sync_journey_periodic"
        private const val INTER_NOTIFICATION_DELAY_MS = 1_000L

        /**
         * Détermine si des notifications de progression de trajet doivent être envoyées.
         * Fonction pure extraite de [SyncJourneyWorker] pour être testable indépendamment
         * de WorkManager et Hilt.
         * Retourne vrai si les notifications sont activées et qu'il y a au moins
         * un jalon nouvellement débloqué ou que le trajet vient d'être complété.
         */
        fun shouldNotifyJourneyProgress(
            journeyNotificationsEnabled: Boolean,
            syncResult: JourneySyncResult,
        ): Boolean = journeyNotificationsEnabled &&
            (syncResult.newlyUnlockedMilestones.isNotEmpty() || syncResult.isNewlyCompleted)

        /**
         * Planifie (ou met à jour) le worker périodique de synchronisation des trajets.
         * Intervalle : 15 minutes (minimum Android WorkManager).
         * Utilise [ExistingPeriodicWorkPolicy.UPDATE] pour appliquer immédiatement tout changement
         * d'intervalle ou de contraintes sans attendre l'expiration du cycle en cours.
         * Les contraintes désactivent explicitement les prérequis batterie/charge pour que le
         * worker s'exécute même en mode économie d'énergie (hors Doze complet).
         */
        fun schedule(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .build()
            val request = PeriodicWorkRequestBuilder<SyncJourneyWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
