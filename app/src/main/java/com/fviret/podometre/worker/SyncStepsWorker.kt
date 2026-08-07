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
 * Worker périodique (15 min) qui lit le nombre de pas du jour via Health Connect
 * et le met en cache dans DataStore pour que l'UI puisse se rafraîchir sans requête HC synchrone.
 * Ne stocke pas de données HC de manière définitive — le cache est invalidé chaque jour.
 * Équivalent iOS : HKObserverQuery + enableBackgroundDelivery.
 *
 * Limites connues (Android) :
 * - Doze mode : WorkManager respecte les fenêtres Doze ; les exécutions peuvent être regroupées
 *   et retardées de plusieurs heures si l'appareil est immobile et non branché.
 * - Quota d'exécution en arrière-plan (API 26+) : le système limite le temps CPU disponible
 *   pour les apps en arrière-plan ; un worker trop long peut être tué.
 * - App fermée par l'utilisateur (Force Stop) : WorkManager ne peut plus s'exécuter jusqu'à
 *   la prochaine ouverture manuelle de l'app.
 * - Intervalle minimum Android : 15 minutes (WorkManager ignore les intervalles plus courts).
 */
@HiltWorker
class SyncStepsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val healthConnectRepository: HealthConnectRepository,
    private val sensorStepHistoryRepository: SensorStepHistoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val goalNotificationService: GoalNotificationService,
) : CoroutineWorker(context, workerParams) {

    /**
     * Lit les pas du jour : capteur local en source primaire (KAN-156), Health Connect en
     * secours uniquement si le capteur n'a encore rien d'exploitable (avant la première
     * capture, permission `ACTIVITY_RECOGNITION` refusée…). Ne contourne plus jamais le
     * capteur pour interroger Health Connect en direct (KAN-157).
     */
    private suspend fun readTodaySteps(today: LocalDate): Long {
        val sensorSteps = sensorStepHistoryRepository.readTodayStepsEstimate(today) ?: 0L
        if (sensorSteps > 0L) return sensorSteps
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        return healthConnectRepository.readSteps(from = startOfDay, to = Instant.now())
    }

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val steps = if (isEmulator()) {
            EMULATOR_MOCK_STEPS
        } else {
            readTodaySteps(today)
        }
        // Guard : si HC est indisponible, readSteps() renvoie 0L.
        // On retente plus tard plutôt que d'écraser un cache valide avec 0.
        if (!isStepsValueValid(steps)) return Result.retry()
        userPreferencesRepository.updateCachedSteps(steps, todayStr)
        checkAndNotifyGoalReached(steps, today)
        return Result.success()
    }

    /**
     * Envoie la notification "Objectif atteint !" si [shouldNotifyGoalReached] l'autorise.
     * Met à jour [goalNotifiedDate] dans DataStore après envoi.
     */
    private suspend fun checkAndNotifyGoalReached(steps: Long, today: LocalDate) {
        val prefs = userPreferencesRepository.userPreferences.first()
        val shouldNotify = shouldNotifyGoalReached(
            steps = steps,
            dailyStepGoal = prefs.dailyStepGoal,
            notificationsEnabled = prefs.notificationsEnabled,
            goalNotifiedDateMs = prefs.goalNotifiedDate,
            today = today,
        )
        if (!shouldNotify) return

        goalNotificationService.notifyGoalReached(steps)
        userPreferencesRepository.setGoalNotifiedDate(System.currentTimeMillis())
    }

    companion object {
        private const val WORK_NAME = "sync_steps_periodic"
        private const val EMULATOR_MOCK_STEPS = 7_430L

        /**
         * Planifie (ou met à jour) le worker périodique de synchronisation des pas.
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
            val request = PeriodicWorkRequestBuilder<SyncStepsWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}

/**
 * Détermine si la notification "Objectif atteint !" doit être envoyée aujourd'hui.
 * Fonction pure (aucun effet de bord) extraite de [SyncStepsWorker] pour être testable
 * indépendamment de WorkManager/Hilt :
 * - les notifications doivent être activées
 * - le nombre de pas doit atteindre l'objectif
 * - la notification ne doit pas avoir déjà été envoyée aujourd'hui
 */
fun shouldNotifyGoalReached(
    steps: Long,
    dailyStepGoal: Int,
    notificationsEnabled: Boolean,
    goalNotifiedDateMs: Long,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
): Boolean {
    if (!notificationsEnabled) return false
    if (steps < dailyStepGoal) return false

    val lastNotifiedDay = if (goalNotifiedDateMs > 0L) {
        Instant.ofEpochMilli(goalNotifiedDateMs).atZone(zone).toLocalDate()
    } else {
        null
    }
    return lastNotifiedDay != today
}

/**
 * Retourne true si la valeur de pas lue depuis Health Connect est exploitable (non nulle).
 * Lorsque HC est temporairement indisponible, [HealthConnectRepository.readSteps] renvoie 0L —
 * dans ce cas [SyncStepsWorker] doit replanifier plutôt qu'écraser le cache existant.
 */
internal fun isStepsValueValid(steps: Long): Boolean = steps > 0L
