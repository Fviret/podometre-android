package com.fviret.podometre.data.health

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

/**
 * Centralise toutes les valeurs mock retournées par [HealthConnectRepository] quand
 * l'app tourne sur émulateur ([com.fviret.podometre.util.isEmulator]).
 *
 * Avant ce refactor (KAN-160), le pattern `if (isEmulator()) return <valeur en dur>`
 * était dupliqué dans chaque méthode publique du repository, chacune avec sa propre
 * valeur mock éparpillée dans le code. Ce fichier regroupe désormais toutes ces
 * valeurs à un seul endroit documenté — le repository se contente de déléguer ici.
 *
 * Aucune valeur mock n'a été modifiée par ce refactor : uniquement leur organisation.
 */
internal object HealthConnectEmulatorMocks {

    /** Mock pour [HealthConnectRepository.readDistanceForDay]. */
    fun distanceForDay(date: LocalDate): Double {
        val seed = date.dayOfMonth + date.monthValue * 31
        val mocks = doubleArrayOf(3.2, 6.8, 5.1, 8.4, 2.7, 7.3, 4.5, 9.1, 1.9, 6.0)
        return mocks[seed % mocks.size]
    }

    /** Mock pour [HealthConnectRepository.readActiveCaloriesForDay]. */
    fun activeCaloriesForDay(date: LocalDate): Int {
        val seed = date.dayOfMonth + date.monthValue * 31
        val mocks = intArrayOf(180, 320, 245, 410, 135, 370, 220, 455, 95, 290)
        return mocks[seed % mocks.size]
    }

    /** Mock pour [HealthConnectRepository.readActiveMinutesForDay]. */
    fun activeMinutesForDay(date: LocalDate): Int {
        val seed = date.dayOfMonth + date.monthValue * 31
        val mocks = intArrayOf(42, 75, 58, 92, 25, 85, 48, 105, 18, 63)
        return mocks[seed % mocks.size]
    }

    /** Mock pour [HealthConnectRepository.readActiveCaloriesForRange] (~255 kcal/jour). */
    fun activeCaloriesForRange(from: Instant, to: Instant): Int {
        val days = ((to.toEpochMilli() - from.toEpochMilli()) / 86_400_000.0).toInt().coerceAtLeast(1)
        return days * 255
    }

    /** Mock pour [HealthConnectRepository.readActiveMinutesForRange] (~55 min/jour). */
    fun activeMinutesForRange(from: Instant, to: Instant): Int {
        val days = ((to.toEpochMilli() - from.toEpochMilli()) / 86_400_000.0).toInt().coerceAtLeast(1)
        return days * 55
    }

    /** Mock pour [HealthConnectRepository.readStepBadgeCounts]. */
    val stepBadgeCounts: Map<Long, Int> =
        mapOf(5_000L to 45, 10_000L to 12, 20_000L to 3, 30_000L to 0, 50_000L to 0, 100_000L to 0)

    /** Mock pour [HealthConnectRepository.readStepBadgeFirstEarnedDates]. */
    fun stepBadgeFirstEarnedDates(today: LocalDate = LocalDate.now()): Map<Long, LocalDate?> =
        mapOf(
            5_000L to today.minusDays(90),
            10_000L to today.minusDays(60),
            20_000L to today.minusDays(30),
            30_000L to null,
            50_000L to null,
            100_000L to null,
        )

    /** Mock pour [HealthConnectRepository.computeStreak]. */
    const val STREAK: Int = 5

    /** Mock pour [HealthConnectRepository.readAverageDailyStepsLast6Days]. */
    const val AVERAGE_DAILY_STEPS_LAST_6_DAYS: Long = 8_500L

    /** Mock pour [HealthConnectRepository.readWeeklyStepTotals]. */
    fun weeklyStepTotals(nWeeks: Int, today: LocalDate = LocalDate.now()): List<Pair<LocalDate, Long>> {
        val monday = today.with(DayOfWeek.MONDAY)
        return (0 until nWeeks).map { i ->
            val weekStart = monday.minusWeeks(i.toLong())
            val mockSteps = when (i) {
                0 -> 42_000L; 1 -> 58_000L; 2 -> 51_000L; 3 -> 63_000L; 4 -> 47_000L
                5 -> 55_000L; 6 -> 44_000L; 7 -> 60_000L; 8 -> 38_000L; else -> 52_000L
            }
            weekStart to mockSteps
        }.reversed()
    }

    /** Mock pour [HealthConnectRepository.readMonthlyStepsByYear]. */
    fun monthlyStepsByYear(year: Int, today: LocalDate = LocalDate.now()): Map<Month, Long> =
        Month.values().associate { month ->
            val ym = YearMonth.of(year, month)
            val isFuture = year == today.year && month.value > today.monthValue
            val mockVal = if (isFuture) 0L else {
                val base = 200_000L + (month.value * 17_000L)
                if (ym.year == today.year && ym.monthValue == today.monthValue)
                    base / 3 else base
            }
            month to mockVal
        }

    /** Mock pour [HealthConnectRepository.readBestDay]. */
    fun bestDay(today: LocalDate = LocalDate.now()): Pair<Long, LocalDate?> =
        24_853L to today.minusDays(45)

    /** Mock pour [HealthConnectRepository.readBestWeek]. */
    fun bestWeek(today: LocalDate = LocalDate.now()): Pair<Long, LocalDate?> =
        112_400L to today.minusWeeks(8).with(DayOfWeek.MONDAY)

    /** Mock pour [HealthConnectRepository.readBestMonth]. */
    fun bestMonth(today: LocalDate = LocalDate.now()): Pair<Long, LocalDate?> =
        385_000L to today.minusMonths(3).withDayOfMonth(1)

    /** Mock pour [HealthConnectRepository.readLongestStreak]. */
    fun longestStreak(today: LocalDate = LocalDate.now()): Pair<Int, LocalDate?> =
        21 to today.minusDays(60)

    /** Mock pour [HealthConnectRepository.readTotalCumulativeSteps]. */
    const val TOTAL_CUMULATIVE_STEPS: Long = 1_234_567L

    /** Mock pour [HealthConnectRepository.readTotalCumulativeDistance] (en km). */
    const val TOTAL_CUMULATIVE_DISTANCE: Double = 987.6
}
