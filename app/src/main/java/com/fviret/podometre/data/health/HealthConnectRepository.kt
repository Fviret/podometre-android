package com.fviret.podometre.data.health

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import java.time.Instant
import java.time.LocalDate

/**
 * Accès aux données de santé.
 * Interface — deux implémentations : [HealthConnectRepositoryImpl] (Health Connect réel)
 * et [EmulatorHealthConnectRepository] (mock, sur émulateur). Le choix entre les deux est
 * fait une seule fois, à l'injection, dans `HealthConnectModule` — voir KAN-160.
 * Ne stocke jamais les données localement — toujours lu depuis la source.
 * Équivalent iOS : StepCountViewModel / HealthKit queries.
 */
interface HealthConnectRepository {

    /**
     * Lit le nombre total de pas entre [from] et [to].
     * Requête idempotente : recalcule depuis [from], ne jamais incrémenter.
     */
    suspend fun readSteps(from: Instant, to: Instant = Instant.now()): Long

    /**
     * Lit les pas par jour pour la plage [from]–[to].
     * Retourne une map LocalDate → nombre de pas (jours sans données absents de la map).
     * Requête idempotente : recalcule depuis [from].
     */
    suspend fun readStepsByDay(from: Instant, to: Instant): Map<LocalDate, Long>

    /**
     * Lit la distance totale parcourue (en km) entre [from] et [to].
     * Requête idempotente : recalcule depuis [from], ne jamais incrémenter.
     */
    suspend fun readDistance(from: Instant, to: Instant = Instant.now()): Double

    /** Lit la distance totale parcourue (en km) pour un [date] précis. */
    suspend fun readDistanceForDay(date: LocalDate): Double

    /** Lit les calories actives brûlées (en kcal, arrondi) pour un [date] précis. */
    suspend fun readActiveCaloriesForDay(date: LocalDate): Int

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
     * Équivalent iOS : CMMotionActivityManager / HKWorkoutType.
     */
    suspend fun readActiveMinutesForDay(date: LocalDate, stepsFallback: Long = 0L): Int

    /** Somme les calories actives brûlées (en kcal) sur une plage [from] → [to]. */
    suspend fun readActiveCaloriesForRange(from: Instant, to: Instant): Int

    /** Somme le temps actif (en minutes) depuis les [ExerciseSessionRecord] sur [from] → [to]. */
    suspend fun readActiveMinutesForRange(from: Instant, to: Instant): Int

    /**
     * Compte, pour chaque seuil de [thresholds], le nombre de jours dans tout l'historique
     * Health Connect où le nombre de pas a atteint ou dépassé ce seuil.
     * Retourne une map seuil → nombre de jours (0 si jamais atteint).
     * Équivalent iOS : BadgeData.swift countDaysAboveThreshold()
     */
    suspend fun readStepBadgeCounts(thresholds: List<Long>): Map<Long, Int>

    /**
     * Retourne la première date à laquelle chaque seuil de pas a été atteint.
     * Parcourt toutes les journées depuis le 1er janvier 2020.
     * Retourne null pour les seuils jamais atteints.
     */
    suspend fun readStepBadgeFirstEarnedDates(thresholds: List<Long>): Map<Long, LocalDate?>

    /**
     * Calcule le streak de jours consécutifs où le nombre de pas >= [goalSteps].
     * Remonte depuis aujourd'hui jusqu'à 365 jours en arrière.
     * Aujourd'hui est inclus uniquement si ses pas atteignent l'objectif.
     * Équivalent iOS : computeStreak() dans StepCountViewModel.swift
     */
    suspend fun computeStreak(goalSteps: Long): Int

    /**
     * Écrit les pas du jour courant dans Health Connect.
     * Idempotent : le même identifiant client par jour évite les doublons — Health Connect fusionne
     * automatiquement les enregistrements portant le même identifiant client.
     * Ne s'exécute que si [steps] > 0 et que Health Connect est disponible.
     */
    suspend fun writeStepsToday(steps: Long)

    /**
     * Calcule la moyenne de pas quotidiens sur les 6 jours pleins précédant aujourd'hui
     * (le jour en cours est exclu car il est partiel).
     * Retourne null si aucune donnée n'est disponible (historique vide) — dans ce cas,
     * l'ETA ne doit pas être affiché pour éviter une estimation trompeuse.
     */
    suspend fun readAverageDailyStepsLast6Days(): Long?

    /**
     * Lit les totaux de pas par semaine ISO sur les [nWeeks] dernières semaines.
     * Retourne une liste de paires (lundi de la semaine, total de pas).
     */
    suspend fun readWeeklyStepTotals(nWeeks: Int = 10): List<Pair<LocalDate, Long>>

    /**
     * Lit les totaux mensuels pour une [year] donnée (janvier à décembre).
     * Retourne une map Month → total (0L pour les mois sans données ou futurs).
     */
    suspend fun readMonthlyStepsByYear(year: Int): Map<java.time.Month, Long>

    /**
     * Lit le meilleur jour de tout l'historique Health Connect.
     * Retourne une paire (nombre de pas, date) ou null si aucune donnée.
     */
    suspend fun readBestDay(): Pair<Long, LocalDate?>

    /**
     * Lit la meilleure semaine ISO de tout l'historique.
     * Retourne une paire (total de la semaine, lundi de la semaine) ou null.
     */
    suspend fun readBestWeek(): Pair<Long, LocalDate?>

    /**
     * Lit le meilleur mois de tout l'historique.
     * Retourne une paire (total du mois, premier jour du mois) ou null.
     */
    suspend fun readBestMonth(): Pair<Long, LocalDate?>

    /**
     * Calcule la plus longue série de jours consécutifs où les pas >= [goalSteps].
     * Retourne une paire (nombre de jours, premier jour de la série).
     */
    suspend fun readLongestStreak(goalSteps: Long): Pair<Int, LocalDate?>

    /** Lit le total cumulé de tous les pas dans Health Connect (depuis 2020). */
    suspend fun readTotalCumulativeSteps(): Long

    /** Lit la distance totale cumulée dans Health Connect (en km, depuis 2020). */
    suspend fun readTotalCumulativeDistance(): Double

    /** Retourne true si Health Connect est installé et disponible sur cet appareil. */
    fun isAvailable(): Boolean

    /**
     * Retourne true si Health Connect est installé (même s'il nécessite une mise à jour).
     * Utilisé dans l'onboarding pour lancer le dialogue de permission même quand le SDK
     * signale [HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED] — Health Connect
     * gère alors lui-même la redirection vers le Play Store.
     */
    fun isInstalled(): Boolean

    /** Vérifie si toutes les permissions de lecture requises (pas, distance) sont accordées. */
    suspend fun hasAllPermissions(): Boolean

    companion object {
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
