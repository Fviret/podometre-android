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
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Worker périodique (15 min, aligné sur [SyncStepsWorker]) qui relève la valeur brute du capteur
 * `TYPE_STEP_COUNTER` et la transmet à [SensorStepHistoryRepository].
 *
 * Le capteur est désormais la source **primaire** des pas de l'app (KAN-156) : ce worker
 * garantit qu'un relevé existe même quand l'app n'est pas au premier plan (le capteur live de
 * [com.fviret.podometre.ui.activity.ActivityViewModel] ne tourne qu'en foreground). Sans lui,
 * l'historique capteur resterait figé dès que l'app passe en arrière-plan.
 *
 * Sans effet sur émulateur (pas de capteur de pas fiable) et si `ACTIVITY_RECOGNITION`
 * n'est pas accordée — dans ce cas rien à faire tant que l'utilisateur n'a pas autorisé
 * le capteur live (voir [com.fviret.podometre.ui.activity.ActivityViewModel.startLiveSensor]).
 *
 * Limites connues (identiques à [SyncStepsWorker]) : Doze mode, quotas d'exécution en
 * arrière-plan, App force-stoppée. Un décalage de quelques heures dans l'exécution ne casse
 * pas la logique — seul l'attribution du delta au bon jour calendaire peut légèrement dériver
 * (l'historique passé reste correct, seule l'estimation du jour en cours peut être obsolète).
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
        sensorStepHistoryRepository.recordSnapshot(rawValue)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "sync_sensor_step_history_periodic"

        /**
         * Planifie (ou met à jour) le worker périodique (15 min — minimum WorkManager).
         * Le changement de jour calendaire est détecté par [SensorStepHistoryRepository.recordSnapshot]
         * lui-même à chaque appel, pas par le scheduling — aucun calage horaire nécessaire ici.
         */
        fun schedule(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .build()
            val request = PeriodicWorkRequestBuilder<SyncSensorStepHistoryWorker>(15, TimeUnit.MINUTES)
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
