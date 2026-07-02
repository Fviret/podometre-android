package com.fviret.podometre.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fviret.podometre.data.health.HealthConnectRepository
import com.fviret.podometre.data.preferences.UserPreferencesRepository
import com.fviret.podometre.util.isEmulator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Worker périodique (1h) qui lit le nombre de pas du jour via Health Connect
 * et le met en cache dans DataStore pour que l'UI puisse se rafraîchir sans requête HC synchrone.
 * Ne stocke pas de données HC de manière définitive — le cache est invalidé chaque jour.
 * Équivalent iOS : HKObserverQuery + enableBackgroundDelivery.
 */
@HiltWorker
class SyncStepsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val healthConnectRepository: HealthConnectRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val goalNotificationService: GoalNotificationService,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val steps = if (isEmulator()) {
            EMULATOR_MOCK_STEPS
        } else {
            val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
            healthConnectRepository.readSteps(from = startOfDay, to = Instant.now())
        }
        userPreferencesRepository.updateCachedSteps(steps, todayStr)
        checkAndNotifyGoalReached(steps, today)
        return Result.success()
    }

    /**
     * Envoie la notification "Objectif atteint !" si :
     * - les notifications sont activées
     * - le nombre de pas atteint l'objectif
     * - la notification n'a pas encore été envoyée aujourd'hui
     * Met à jour [goalNotifiedDate] dans DataStore après envoi.
     */
    private suspend fun checkAndNotifyGoalReached(steps: Long, today: LocalDate) {
        val prefs = userPreferencesRepository.userPreferences.first()
        if (!prefs.notificationsEnabled) return
        if (steps < prefs.dailyStepGoal) return

        val lastNotifiedMs = prefs.goalNotifiedDate
        val lastNotifiedDay = if (lastNotifiedMs > 0L) {
            Instant.ofEpochMilli(lastNotifiedMs)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        } else null

        if (lastNotifiedDay == today) return

        goalNotificationService.notifyGoalReached(steps)
        userPreferencesRepository.setGoalNotifiedDate(System.currentTimeMillis())
    }

    companion object {
        private const val WORK_NAME = "sync_steps_hourly"
        private const val EMULATOR_MOCK_STEPS = 7_430L

        /**
         * Planifie (ou maintient) le worker périodique horaire de synchronisation des pas.
         * Utilise [ExistingPeriodicWorkPolicy.KEEP] pour ne pas réinitialiser le timer si déjà actif.
         */
        fun schedule(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<SyncStepsWorker>(1, TimeUnit.HOURS).build()
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
