package com.fviret.podometre.data.health

import android.util.Log
import java.time.Instant
import java.time.LocalDate
import java.time.Month

/**
 * Implémentation mock de [HealthConnectRepository], utilisée sur émulateur
 * ([com.fviret.podometre.util.isEmulator]). Délègue toutes les valeurs à
 * [HealthConnectEmulatorMocks] — aucune valeur mock n'est modifiée par ce refactor,
 * uniquement leur point de sélection (KAN-160).
 *
 * Pour les méthodes qui, dans l'implémentation historique, n'avaient jamais de branche
 * émulateur (elles interrogeaient toujours Health Connect réel, y compris sur émulateur —
 * `readSteps`, `readStepsByDay`, `isAvailable`, `isInstalled`, `hasAllPermissions`),
 * ce décorateur délègue à [real] pour préserver exactement le même comportement.
 *
 * Le choix entre cette implémentation et [HealthConnectRepositoryImpl] est fait une seule
 * fois, à l'injection, dans `HealthConnectModule` — c'est le point de décision unique demandé
 * par KAN-160.
 */
internal class EmulatorHealthConnectRepository(
    private val real: HealthConnectRepositoryImpl
) : HealthConnectRepository {

    override suspend fun readSteps(from: Instant, to: Instant): Long =
        real.readSteps(from, to)

    override suspend fun readStepsByDay(from: Instant, to: Instant): Map<LocalDate, Long> =
        real.readStepsByDay(from, to)

    override suspend fun readDistance(from: Instant, to: Instant): Double =
        real.readDistance(from, to)

    override suspend fun readDistanceForDay(date: LocalDate): Double =
        HealthConnectEmulatorMocks.distanceForDay(date)

    override suspend fun readActiveCaloriesForDay(date: LocalDate): Int =
        HealthConnectEmulatorMocks.activeCaloriesForDay(date)

    override suspend fun readActiveMinutesForDay(date: LocalDate, stepsFallback: Long): Int =
        HealthConnectEmulatorMocks.activeMinutesForDay(date)

    override suspend fun readActiveCaloriesForRange(from: Instant, to: Instant): Int =
        HealthConnectEmulatorMocks.activeCaloriesForRange(from, to)

    override suspend fun readActiveMinutesForRange(from: Instant, to: Instant): Int =
        HealthConnectEmulatorMocks.activeMinutesForRange(from, to)

    override suspend fun readStepBadgeCounts(thresholds: List<Long>): Map<Long, Int> =
        HealthConnectEmulatorMocks.stepBadgeCounts

    override suspend fun readStepBadgeFirstEarnedDates(thresholds: List<Long>): Map<Long, LocalDate?> =
        HealthConnectEmulatorMocks.stepBadgeFirstEarnedDates()

    override suspend fun computeStreak(goalSteps: Long): Int =
        HealthConnectEmulatorMocks.STREAK

    override suspend fun writeStepsToday(steps: Long) {
        Log.d(TAG, "writeStepsToday (émulateur) : $steps pas — écriture simulée")
    }

    override suspend fun readAverageDailyStepsLast6Days(): Long? =
        HealthConnectEmulatorMocks.AVERAGE_DAILY_STEPS_LAST_6_DAYS

    override suspend fun readWeeklyStepTotals(nWeeks: Int): List<Pair<LocalDate, Long>> =
        HealthConnectEmulatorMocks.weeklyStepTotals(nWeeks)

    override suspend fun readMonthlyStepsByYear(year: Int): Map<Month, Long> =
        HealthConnectEmulatorMocks.monthlyStepsByYear(year)

    override suspend fun readBestDay(): Pair<Long, LocalDate?> =
        HealthConnectEmulatorMocks.bestDay()

    override suspend fun readBestWeek(): Pair<Long, LocalDate?> =
        HealthConnectEmulatorMocks.bestWeek()

    override suspend fun readBestMonth(): Pair<Long, LocalDate?> =
        HealthConnectEmulatorMocks.bestMonth()

    override suspend fun readLongestStreak(goalSteps: Long): Pair<Int, LocalDate?> =
        HealthConnectEmulatorMocks.longestStreak()

    override suspend fun readTotalCumulativeSteps(): Long =
        HealthConnectEmulatorMocks.TOTAL_CUMULATIVE_STEPS

    override suspend fun readTotalCumulativeDistance(): Double =
        HealthConnectEmulatorMocks.TOTAL_CUMULATIVE_DISTANCE

    override fun isAvailable(): Boolean = real.isAvailable()

    override fun isInstalled(): Boolean = real.isInstalled()

    override suspend fun hasAllPermissions(): Boolean = real.hasAllPermissions()

    companion object {
        private const val TAG = "EmulatorHCRepository"
    }
}
