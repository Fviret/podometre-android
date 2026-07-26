package com.fviret.podometre.data.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.fviret.podometre.util.isEmulator
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
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
     * Lit la distance totale parcourue (en km) pour un [date] précis.
     * Sur émulateur, retourne une valeur mock réaliste selon le jour.
     */
    suspend fun readDistanceForDay(date: java.time.LocalDate): Double {
        if (isEmulator()) {
            val seed = date.dayOfMonth + date.monthValue * 31
            val mocks = doubleArrayOf(3.2, 6.8, 5.1, 8.4, 2.7, 7.3, 4.5, 9.1, 1.9, 6.0)
            return mocks[seed % mocks.size]
        }
        val zone = ZoneId.systemDefault()
        val from = date.atStartOfDay(zone).toInstant()
        val to = date.plusDays(1).atStartOfDay(zone).toInstant()
        return readDistance(from = from, to = to)
    }

    /**
     * Lit les calories actives brûlées (en kcal, arrondi) pour un [date] précis.
     * Sur émulateur, retourne une valeur mock réaliste selon le jour.
     */
    suspend fun readActiveCaloriesForDay(date: java.time.LocalDate): Int {
        if (isEmulator()) {
            val seed = date.dayOfMonth + date.monthValue * 31
            val mocks = intArrayOf(180, 320, 245, 410, 135, 370, 220, 455, 95, 290)
            return mocks[seed % mocks.size]
        }
        val zone = ZoneId.systemDefault()
        val from = date.atStartOfDay(zone).toInstant()
        val to = date.plusDays(1).atStartOfDay(zone).toInstant()
        val request = ReadRecordsRequest(
            recordType = ActiveCaloriesBurnedRecord::class,
            timeRangeFilter = TimeRangeFilter.between(from, to)
        )
        return runCatching {
            client.get().readRecords(request).records
                .sumOf { it.energy.inKilocalories }
                .toInt()
        }.onFailure { Log.w(TAG, "readActiveCaloriesForDay a échoué, retour à 0", it) }
            .getOrDefault(0)
    }

    /**
     * Calcule le temps actif (en minutes) pour un [date] précis.
     *
     * Stratégie :
     * 1. Tente de lire [ExerciseSessionRecord] (marche, course, cyclisme, fitness…) depuis Health Connect.
     *    Somme les durées de toutes les sessions du jour.
     * 2. Si la lecture échoue (permission refusée, HK indisponible), retourne -1 → fallback.
     * 3. Si la lecture réussit mais retourne 0 sessions (pas de wearable ni d'app fitness),
     *    applique l'estimation via les pas : [stepsFallback] × 0.01 min/pas.
     *    (Exemple : 7 000 pas → ~70 min d'activité légère.)
     *
     * Sur émulateur, retourne une valeur mock réaliste selon le jour.
     *
     * Équivalent iOS : CMMotionActivityManager / HKWorkoutType.
     */
    suspend fun readActiveMinutesForDay(date: java.time.LocalDate, stepsFallback: Long = 0L): Int {
        if (isEmulator()) {
            val seed = date.dayOfMonth + date.monthValue * 31
            val mocks = intArrayOf(42, 75, 58, 92, 25, 85, 48, 105, 18, 63)
            return mocks[seed % mocks.size]
        }
        val zone = ZoneId.systemDefault()
        val from = date.atStartOfDay(zone).toInstant()
        val to = date.plusDays(1).atStartOfDay(zone).toInstant()
        val request = ReadRecordsRequest(
            recordType = ExerciseSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(from, to)
        )
        val exerciseMinutes = runCatching {
            client.get().readRecords(request).records.sumOf { record ->
                val durationMs = record.endTime.toEpochMilli() - record.startTime.toEpochMilli()
                durationMs / 60_000L
            }.toInt()
        }.onFailure { Log.w(TAG, "readActiveMinutesForDay (ExerciseSession) a échoué", it) }
            .getOrDefault(-1)

        return when {
            // Lecture réussie avec au moins une session : donnée réelle
            exerciseMinutes > 0 -> exerciseMinutes
            // Lecture réussie mais 0 session OU lecture échouée : estimation par les pas
            else -> (stepsFallback * 0.01).toInt()
        }
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
     * Retourne la première date à laquelle chaque seuil de pas a été atteint.
     * Parcourt toutes les journées depuis le 1er janvier 2020.
     * Retourne null pour les seuils jamais atteints.
     * Sur émulateur, retourne des dates mock réalistes.
     */
    suspend fun readStepBadgeFirstEarnedDates(thresholds: List<Long>): Map<Long, LocalDate?> {
        if (isEmulator()) {
            val today = LocalDate.now()
            return mapOf(
                5_000L  to today.minusDays(90),
                10_000L to today.minusDays(60),
                20_000L to today.minusDays(30),
                30_000L to null,
                50_000L to null,
                100_000L to null,
            )
        }

        val zone = ZoneId.systemDefault()
        val from = LocalDate.of(2020, 1, 1).atStartOfDay(zone).toInstant()
        val to = ZonedDateTime.now(zone).toInstant()

        val stepsByDay = readStepsByDay(from, to)
        return thresholds.associateWith { threshold ->
            stepsByDay.entries
                .filter { it.value >= threshold }
                .minByOrNull { it.key }
                ?.key
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

    /**
     * Écrit les pas du jour courant dans Health Connect.
     * Idempotent : le même [clientRecordId] par jour évite les doublons — Health Connect fusionne
     * automatiquement les enregistrements portant le même identifiant client.
     * Ne s'exécute que si [steps] > 0 et que Health Connect est disponible.
     * Sur émulateur, simule uniquement un log (Health Connect émulateur est instable).
     */
    suspend fun writeStepsToday(steps: Long) {
        if (steps <= 0L) return
        if (!isAvailable()) return
        if (isEmulator()) {
            Log.d(TAG, "writeStepsToday (émulateur) : $steps pas — écriture simulée")
            return
        }
        runCatching {
            val today = LocalDate.now()
            val zone = ZoneId.systemDefault()
            val startOfDay = today.atStartOfDay(zone).toInstant()
            val now = Instant.now()
            val record = StepsRecord(
                startTime = startOfDay,
                endTime = now,
                count = steps,
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(startOfDay),
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(now),
                metadata = Metadata.Companion.unknownRecordingMethod()
            )
            client.get().insertRecords(listOf(record))
        }.onFailure { Log.w(TAG, "writeStepsToday a échoué", it) }
    }

    /**
     * Calcule la moyenne de pas quotidiens sur les 6 jours pleins précédant aujourd'hui
     * (le jour en cours est exclu car il est partiel).
     * Retourne null si aucune donnée n'est disponible (historique vide) — dans ce cas,
     * l'ETA ne doit pas être affiché pour éviter une estimation trompeuse.
     * Sur émulateur, retourne une valeur mock réaliste (8 500 pas/jour).
     */
    suspend fun readAverageDailyStepsLast6Days(): Long? {
        if (isEmulator()) return 8_500L

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val from = today.minusDays(6).atStartOfDay(zone).toInstant()
        val to = today.atStartOfDay(zone).toInstant()

        val stepsByDay = readStepsByDay(from, to)
        if (stepsByDay.isEmpty()) return null
        return stepsByDay.values.sum() / stepsByDay.size
    }

    /** Retourne true si Health Connect est installé et disponible sur cet appareil. */
    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    /**
     * Retourne true si Health Connect est installé (même s'il nécessite une mise à jour).
     * Utilisé dans l'onboarding pour lancer le dialogue de permission même quand le SDK
     * signale [HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED] — Health Connect
     * gère alors lui-même la redirection vers le Play Store.
     */
    fun isInstalled(): Boolean =
        HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_UNAVAILABLE

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
        /**
         * Permissions demandées à l'onboarding.
         * READ_EXERCISE (KAN-82) pour les sessions d'exercice (temps actif).
         * WRITE_STEPS (KAN-102) pour écrire les pas capteur dans HC sur les appareils
         * sans app fitness tierce (Google Fit, etc.).
         */
        val PERMISSIONS: Set<String> = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getWritePermission(StepsRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        )

        /** Contrat système pour la demande de permissions Health Connect. */
        fun requestPermissionsContract() = PermissionController.createRequestPermissionResultContract()
    }
}
