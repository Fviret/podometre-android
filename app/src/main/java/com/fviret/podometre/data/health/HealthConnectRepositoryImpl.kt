package com.fviret.podometre.data.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.Lazy
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Implémentation réelle de [HealthConnectRepository], adossée à Health Connect.
 * Aucun branchement émulateur ici — voir [EmulatorHealthConnectRepository] pour le mode mock.
 * Le choix entre les deux implémentations est fait une seule fois, à l'injection, dans
 * `HealthConnectModule` (KAN-160).
 * [client] est injecté en [Lazy] : sa construction réelle (`HealthConnectClient.getOrCreate`,
 * qui lève `IllegalStateException` si Health Connect n'est pas disponible sur l'appareil)
 * n'est déclenchée qu'au premier appel effectif, jamais à l'injection — sinon n'importe quel
 * composant dépendant de ce repository (y compris indirectement, via WorkManager/Hilt)
 * crasherait au démarrage sur un appareil sans Health Connect, même s'il ne l'utilise jamais.
 */
class HealthConnectRepositoryImpl(
    private val client: Lazy<HealthConnectClient>,
    private val context: Context
) : HealthConnectRepository {

    override suspend fun readSteps(from: Instant, to: Instant): Long {
        val request = ReadRecordsRequest(
            recordType = StepsRecord::class,
            timeRangeFilter = TimeRangeFilter.between(from, to)
        )
        return runCatching {
            client.get().readRecords(request).records.sumOf { it.count }
        }.onFailure { Log.w(TAG, "readSteps a échoué, retour à 0", it) }
            .getOrDefault(0L)
    }

    override suspend fun readStepsByDay(from: Instant, to: Instant): Map<LocalDate, Long> {
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

    override suspend fun readDistance(from: Instant, to: Instant): Double {
        val request = ReadRecordsRequest(
            recordType = DistanceRecord::class,
            timeRangeFilter = TimeRangeFilter.between(from, to)
        )
        return runCatching {
            client.get().readRecords(request).records.sumOf { it.distance.inKilometers }
        }.onFailure { Log.w(TAG, "readDistance a échoué, retour à 0.0", it) }
            .getOrDefault(0.0)
    }

    override suspend fun readDistanceForDay(date: LocalDate): Double {
        val zone = ZoneId.systemDefault()
        val from = date.atStartOfDay(zone).toInstant()
        val to = date.plusDays(1).atStartOfDay(zone).toInstant()
        return readDistance(from = from, to = to)
    }

    override suspend fun readActiveCaloriesForDay(date: LocalDate): Int {
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

    override suspend fun readActiveMinutesForDay(date: LocalDate, stepsFallback: Long): Int {
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

    override suspend fun readActiveCaloriesForRange(from: Instant, to: Instant): Int {
        val request = ReadRecordsRequest(
            recordType = ActiveCaloriesBurnedRecord::class,
            timeRangeFilter = TimeRangeFilter.between(from, to)
        )
        return runCatching {
            client.get().readRecords(request).records.sumOf { it.energy.inKilocalories }.toInt()
        }.onFailure { Log.w(TAG, "readActiveCaloriesForRange a échoué, retour à 0", it) }
            .getOrDefault(0)
    }

    override suspend fun readActiveMinutesForRange(from: Instant, to: Instant): Int {
        val request = ReadRecordsRequest(
            recordType = ExerciseSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(from, to)
        )
        return runCatching {
            client.get().readRecords(request).records.sumOf { record ->
                (record.endTime.toEpochMilli() - record.startTime.toEpochMilli()) / 60_000L
            }.toInt()
        }.onFailure { Log.w(TAG, "readActiveMinutesForRange a échoué", it) }
            .getOrDefault(0)
    }

    override suspend fun readStepBadgeCounts(thresholds: List<Long>): Map<Long, Int> {
        val zone = ZoneId.systemDefault()
        val from = LocalDate.of(2020, 1, 1).atStartOfDay(zone).toInstant()
        val to = ZonedDateTime.now(zone).toInstant()

        val stepsByDay = readStepsByDay(from, to)
        return thresholds.associateWith { threshold ->
            stepsByDay.values.count { it >= threshold }
        }
    }

    override suspend fun readStepBadgeFirstEarnedDates(thresholds: List<Long>): Map<Long, LocalDate?> {
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

    override suspend fun computeStreak(goalSteps: Long): Int {
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

    override suspend fun writeStepsToday(steps: Long) {
        if (steps <= 0L) return
        if (!isAvailable()) return
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

    override suspend fun readAverageDailyStepsLast6Days(): Long? {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val from = today.minusDays(6).atStartOfDay(zone).toInstant()
        val to = today.atStartOfDay(zone).toInstant()

        val stepsByDay = readStepsByDay(from, to)
        if (stepsByDay.isEmpty()) return null
        return stepsByDay.values.sum() / stepsByDay.size
    }

    override suspend fun readWeeklyStepTotals(nWeeks: Int): List<Pair<LocalDate, Long>> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val monday = today.with(java.time.DayOfWeek.MONDAY)
        val from = monday.minusWeeks(nWeeks.toLong()).atStartOfDay(zone).toInstant()
        val to = ZonedDateTime.now(zone).toInstant()
        val stepsByDay = readStepsByDay(from, to)
        return (0 until nWeeks).map { i ->
            val weekStart = monday.minusWeeks(i.toLong())
            val weekTotal = (0..6).sumOf { d ->
                stepsByDay[weekStart.plusDays(d.toLong())] ?: 0L
            }
            weekStart to weekTotal
        }.reversed()
    }

    override suspend fun readMonthlyStepsByYear(year: Int): Map<java.time.Month, Long> {
        val zone = ZoneId.systemDefault()
        val from = LocalDate.of(year, 1, 1).atStartOfDay(zone).toInstant()
        val to = LocalDate.of(year + 1, 1, 1).atStartOfDay(zone).toInstant()
        val stepsByDay = readStepsByDay(from, to)
        return java.time.Month.values().associate { month ->
            val total = stepsByDay.entries
                .filter { it.key.year == year && it.key.month == month }
                .sumOf { it.value }
            month to total
        }
    }

    override suspend fun readBestDay(): Pair<Long, LocalDate?> {
        val zone = ZoneId.systemDefault()
        val from = LocalDate.of(2020, 1, 1).atStartOfDay(zone).toInstant()
        val to = ZonedDateTime.now(zone).toInstant()
        val stepsByDay = readStepsByDay(from, to)
        val best = stepsByDay.maxByOrNull { it.value } ?: return 0L to null
        return best.value to best.key
    }

    override suspend fun readBestWeek(): Pair<Long, LocalDate?> {
        val zone = ZoneId.systemDefault()
        val from = LocalDate.of(2020, 1, 1).atStartOfDay(zone).toInstant()
        val to = ZonedDateTime.now(zone).toInstant()
        val stepsByDay = readStepsByDay(from, to)
        // Grouper par semaine ISO (lundi)
        val byWeek = stepsByDay.entries.groupBy { entry ->
            entry.key.with(java.time.DayOfWeek.MONDAY)
        }.mapValues { (_, days) -> days.sumOf { it.value } }
        val best = byWeek.maxByOrNull { it.value } ?: return 0L to null
        return best.value to best.key
    }

    override suspend fun readBestMonth(): Pair<Long, LocalDate?> {
        val zone = ZoneId.systemDefault()
        val from = LocalDate.of(2020, 1, 1).atStartOfDay(zone).toInstant()
        val to = ZonedDateTime.now(zone).toInstant()
        val stepsByDay = readStepsByDay(from, to)
        val byMonth = stepsByDay.entries.groupBy { entry ->
            entry.key.withDayOfMonth(1)
        }.mapValues { (_, days) -> days.sumOf { it.value } }
        val best = byMonth.maxByOrNull { it.value } ?: return 0L to null
        return best.value to best.key
    }

    override suspend fun readLongestStreak(goalSteps: Long): Pair<Int, LocalDate?> {
        val zone = ZoneId.systemDefault()
        val from = LocalDate.of(2020, 1, 1).atStartOfDay(zone).toInstant()
        val to = ZonedDateTime.now(zone).toInstant()
        val stepsByDay = readStepsByDay(from, to)
        if (stepsByDay.isEmpty()) return 0 to null
        val sortedDays = stepsByDay.keys.sorted()
        var best = 0
        var bestStart: LocalDate? = null
        var current = 0
        var currentStart: LocalDate? = null
        for (day in sortedDays) {
            if ((stepsByDay[day] ?: 0L) >= goalSteps) {
                if (current == 0) currentStart = day
                current++
                if (current > best) { best = current; bestStart = currentStart }
            } else { current = 0; currentStart = null }
        }
        return best to bestStart
    }

    override suspend fun readTotalCumulativeSteps(): Long {
        val zone = ZoneId.systemDefault()
        val from = LocalDate.of(2020, 1, 1).atStartOfDay(zone).toInstant()
        val to = ZonedDateTime.now(zone).toInstant()
        return readSteps(from, to)
    }

    override suspend fun readTotalCumulativeDistance(): Double {
        val zone = ZoneId.systemDefault()
        val from = LocalDate.of(2020, 1, 1).atStartOfDay(zone).toInstant()
        val to = ZonedDateTime.now(zone).toInstant()
        return readDistance(from, to)
    }

    override fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    override fun isInstalled(): Boolean =
        HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_UNAVAILABLE

    override suspend fun hasAllPermissions(): Boolean =
        runCatching {
            client.get().permissionController.getGrantedPermissions().containsAll(HealthConnectRepository.PERMISSIONS)
        }.onFailure { Log.w(TAG, "hasAllPermissions a échoué, retour à false", it) }
            .getOrDefault(false)

    companion object {
        private const val TAG = "HealthConnectRepositoryImpl"
    }
}
