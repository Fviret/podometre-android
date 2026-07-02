package com.fviret.podometre.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fviret.podometre.data.health.HealthConnectRepository
import com.fviret.podometre.data.journey.JourneyProgressRepository
import com.fviret.podometre.data.preferences.UserPreferencesRepository
import com.fviret.podometre.domain.JourneyData
import com.fviret.podometre.util.isEmulator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Worker périodique (1h) qui lit la distance parcourue depuis Health Connect
 * et met à jour la progression du trajet actif.
 * Sur émulateur, injecte des données mock réalistes sans appel HC.
 * Équivalent iOS : JourneyProgressService + HKObserverQuery background delivery.
 */
@HiltWorker
class SyncJourneyWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val healthConnectRepository: HealthConnectRepository,
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
        val newKm = healthConnectRepository.readDistance(from = startInstant)

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

    companion object {
        private const val WORK_NAME = "sync_journey_hourly"
        private const val INTER_NOTIFICATION_DELAY_MS = 1_000L

        /**
         * Planifie (ou maintient) le worker périodique horaire de synchronisation des trajets.
         * Utilise [ExistingPeriodicWorkPolicy.KEEP] pour ne pas réinitialiser le timer si déjà actif.
         */
        fun schedule(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<SyncJourneyWorker>(1, TimeUnit.HOURS).build()
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
