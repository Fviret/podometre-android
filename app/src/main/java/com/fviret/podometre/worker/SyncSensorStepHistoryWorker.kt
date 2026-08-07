package com.fviret.podometre.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fviret.podometre.data.health.SensorStepHistoryRepository
import com.fviret.podometre.util.isEmulator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Worker quotidien qui relève la valeur brute du capteur `TYPE_STEP_COUNTER` et la transmet
 * à [SensorStepHistoryRepository] pour reconstituer un historique local de secours quand
 * Health Connect ne retourne aucune donnée (OEM sans app santé écrivant dans HC).
 *
 * Sans effet sur émulateur (pas de capteur de pas fiable) et si `ACTIVITY_RECOGNITION`
 * n'est pas accordée — dans ce cas rien à faire tant que l'utilisateur n'a pas autorisé
 * le capteur live (voir [com.fviret.podometre.ui.activity.ActivityViewModel.startLiveSensor]).
 *
 * Limites connues (identiques à [SyncStepsWorker]) : Doze mode, quotas d'exécution en
 * arrière-plan, App force-stoppée. Un décalage de quelques heures dans l'exécution ne casse
 * pas la logique — seul l'attribution du delta au bon jour calendaire peut légèrement dériver.
 */
@HiltWorker
class SyncSensorStepHistoryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val sensorStepHistoryRepository: SensorStepHistoryRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (isEmulator()) return Result.success()

        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else true
        if (!hasPermission) return Result.success()

        val rawValue = readCurrentStepCounter(applicationContext) ?: return Result.retry()
        sensorStepHistoryRepository.captureDailySnapshot(rawValue)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "sync_sensor_step_history_daily"

        /**
         * Planifie (ou met à jour) le worker quotidien.
         * Délai initial calé sur le prochain 00h05 local pour attribuer le delta au bon jour ;
         * en cas de retard (Doze…), l'attribution reste correcte car le delta est toujours
         * rattaché à la date du relevé précédent, pas à l'heure d'exécution du worker.
         */
        fun schedule(workManager: WorkManager) {
            val zone = ZoneId.systemDefault()
            val now = ZonedDateTime.now(zone)
            val nextRun = now.toLocalDate().plusDays(1).atStartOfDay(zone).plusMinutes(5)
            val initialDelayMs = (nextRun.toInstant().toEpochMilli() - now.toInstant().toEpochMilli())
                .coerceAtLeast(0L)

            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .build()
            val request = PeriodicWorkRequestBuilder<SyncSensorStepHistoryWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
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
 * Lit une seule fois la valeur cumulative courante du capteur `TYPE_STEP_COUNTER`.
 * `TYPE_STEP_COUNTER` délivre un premier événement immédiatement après l'enregistrement
 * du listener — pas besoin de rester à l'écoute plus longtemps.
 * Retourne null si aucun capteur n'est disponible ou si aucune valeur n'arrive sous 5 s.
 */
internal suspend fun readCurrentStepCounter(context: Context): Long? {
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return null
    val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return null

    return withTimeoutOrNull(5_000L) {
        suspendCancellableCoroutine { cont ->
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    sensorManager.unregisterListener(this)
                    if (cont.isActive) cont.resume(event.values[0].toLong())
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            cont.invokeOnCancellation { sensorManager.unregisterListener(listener) }
            sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }
}
