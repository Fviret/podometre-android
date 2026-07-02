package com.fviret.podometre.data.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.fviret.podometre.util.isEmulator
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Accès aux données de santé via Health Connect.
 * Ne stocke jamais les données localement — toujours lu depuis la source.
 * [client] est injecté en [Lazy] : sa construction réelle (`HealthConnectClient.getOrCreate`,
 * qui lève `IllegalStateException` si Health Connect n'est pas disponible sur l'appareil)
 * n'est déclenchée qu'au premier appel effectif, jamais à l'injection — sinon n'importe quel
 * composant dépendant de ce repository (y compris indirectement, via WorkManager/Hilt)
 * crasherait au démarrage sur un appareil sans Health Connect, même s'il ne l'utilise jamais.
 * Équivalent iOS : StepCountViewModel / HealthKit queries.
 */
@Singleton
class HealthConnectRepository @Inject constructor(
    private val client: Lazy<HealthConnectClient>,
    @ApplicationContext private val context: Context
) {

    /**
     * Lit le nombre total de pas entre [from] et [to].
     * Requête idempotente : recalcule depuis [from], ne jamais incrémenter.
     */
    suspend fun readSteps(from: Instant, to: Instant = Instant.now()): Long {
        val request = ReadRecordsRequest(
            recordType = StepsRecord::class,
            timeRangeFilter = TimeRangeFilter.between(from, to)
        )
        return runCatching {
            client.get().readRecords(request).records.sumOf { it.count }
        }.onFailure { Log.w(TAG, "readSteps a échoué, retour à 0", it) }
            .getOrDefault(0L)
    }

    /**
     * Lit les pas par jour pour la plage [from]–[to].
     * Retourne une map LocalDate → nombre de pas (jours sans données absents de la map).
     * Requête idempotente : recalcule depuis [from].
     */
    suspend fun readStepsByDay(from: Instant, to: Instant): Map<LocalDate, Long> {
        val request = ReadRecordsRequest(
            recordType = StepsRecord::class,
            timeRangeFilter = TimeRangeFilter.between(from, to)
        )
        return runCatching {
            client.get().readRecords(request).records
                .groupBy { it.startTime.atZone(ZoneId.systemDefault()).toLocalDate() }
                .mapValues { (_, records) -> records.sumOf { it.count } }
        }.onFailure { Log.w(TAG, "readStepsByDay a échoué, retour à une map vide", it) }
            .getOrDefault(emptyMap())
    }

    /**
     * Lit la distance totale parcourue (en km) entre [from] et [to].
     * Requête idempotente : recalcule depuis [from], ne jamais incrémenter.
     */
    suspend fun readDistance(from: Instant, to: Instant = Instant.now()): Double {
        val request = ReadRecordsRequest(
            recordType = DistanceRecord::class,
            timeRangeFilter = TimeRangeFilter.between(from, to)
        )
        return runCatching {
            client.get().readRecords(request).records.sumOf { it.distance.inKilometers }
        }.onFailure { Log.w(TAG, "readDistance a échoué, retour à 0.0", it) }
            .getOrDefault(0.0)
    }

    /**
     * Compte, pour chaque seuil de [thresholds], le nombre de jours dans tout l'historique
     * Health Connect où le nombre de pas a atteint ou dépassé ce seuil.
     * Retourne une map seuil → nombre de jours (0 si jamais atteint).
     * Sur émulateur, retourne des valeurs mock réalistes.
     * Équivalent iOS : BadgeData.swift countDaysAboveThreshold()
     */
    suspend fun readStepBadgeCounts(thresholds: List<Long>): Map<Long, Int> {
        if (isEmulator()) {
            return mapOf(5_000L to 45, 10_000L to 12, 20_000L to 3, 30_000L to 0, 50_000L to 0, 100_000L to 0)
        }

        val zone = ZoneId.systemDefault()
        val from = LocalDate.of(2020, 1, 1).atStartOfDay(zone).toInstant()
        val to = ZonedDateTime.now(zone).toInstant()

        val stepsByDay = readStepsByDay(from, to)
        return thresholds.associateWith { threshold ->
            stepsByDay.values.count { it >= threshold }
        }
    }

    /**
     * Calcule le streak de jours consécutifs où le nombre de pas >= [goalSteps].
     * Remonte depuis aujourd'hui jusqu'à 365 jours en arrière.
     * Aujourd'hui est inclus uniquement si ses pas atteignent l'objectif.
     * Sur émulateur, retourne une valeur mock (5 jours).
     * Équivalent iOS : computeStreak() dans StepCountViewModel.swift
     */
    suspend fun computeStreak(goalSteps: Long): Int {
        if (isEmulator()) return 5

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val from = today.minusDays(364).atStartOfDay(zone).toInstant()
        val to = ZonedDateTime.now(zone).toInstant()

        val stepsByDay = readStepsByDay(from, to)

        var streak = 0
        var day = today
        while (day >= today.minusDays(364)) {
            val steps = stepsByDay[day] ?: 0L
            if (steps >= goalSteps) {
                streak++
                day = day.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }

    /** Retourne true si Health Connect est installé et disponible sur cet appareil. */
    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    /**
     * Vérifie si toutes les permissions de lecture requises (pas, distance) sont accordées.
     */
    suspend fun hasAllPermissions(): Boolean =
        runCatching {
            client.get().permissionController.getGrantedPermissions().containsAll(PERMISSIONS)
        }.onFailure { Log.w(TAG, "hasAllPermissions a échoué, retour à false", it) }
            .getOrDefault(false)

    companion object {
        private const val TAG = "HealthConnectRepository"
        /** Permissions de lecture demandées à l'onboarding (KAN-18). */
        val PERMISSIONS: Set<String> = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
        )

        /** Contrat système pour la demande de permissions Health Connect. */
        fun requestPermissionsContract() = PermissionController.createRequestPermissionResultContract()
    }
}
