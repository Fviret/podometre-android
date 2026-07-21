package com.fviret.podometre.data.health

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.ReadRecordsResponse
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Teste [HealthConnectRepository.computeStreak] : calcul du streak de jours consécutifs.
 * `isEmulator()` est mocké statiquement à `false` pour exercer la vraie logique de calcul
 * (sur émulateur réel, la méthode court-circuite avec une valeur mock — cf. DeviceUtils.kt).
 */
class HealthConnectRepositoryTest {

    private val client = mockk<HealthConnectClient>()
    private lateinit var repository: HealthConnectRepository
    private val zone = ZoneId.systemDefault()

    @BeforeEach
    fun setup() {
        mockkStatic("com.fviret.podometre.util.DeviceUtilsKt")
        io.mockk.every { com.fviret.podometre.util.isEmulator() } returns false
        repository = HealthConnectRepository(dagger.Lazy { client }, mockk(relaxed = true))
    }

    @AfterEach
    fun teardown() {
        unmockkStatic("com.fviret.podometre.util.DeviceUtilsKt")
    }

    private fun stepsRecordFor(day: LocalDate, count: Long): StepsRecord {
        val start = day.atStartOfDay(zone).toInstant()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant()
        return StepsRecord(
            startTime = start,
            startZoneOffset = ZoneOffset.UTC,
            endTime = end,
            endZoneOffset = ZoneOffset.UTC,
            count = count,
            metadata = Metadata(),
        )
    }

    private fun mockStepsForDays(stepsByDay: Map<LocalDate, Long>) {
        val records = stepsByDay.map { (day, count) -> stepsRecordFor(day, count) }
        coEvery {
            client.readRecords(any<ReadRecordsRequest<StepsRecord>>())
        } returns ReadRecordsResponse(records, null)
    }

    @Test
    fun `computeStreak est 0 quand aucun jour n atteint l objectif`() = runTest {
        val today = LocalDate.now(zone)
        mockStepsForDays(mapOf(today to 1_000L))

        val streak = repository.computeStreak(goalSteps = 10_000L)

        assertEquals(0, streak)
    }

    @Test
    fun `computeStreak compte les jours consecutifs incluant aujourd hui`() = runTest {
        val today = LocalDate.now(zone)
        mockStepsForDays(
            mapOf(
                today to 12_000L,
                today.minusDays(1) to 11_000L,
                today.minusDays(2) to 10_500L,
            )
        )

        val streak = repository.computeStreak(goalSteps = 10_000L)

        assertEquals(3, streak)
    }

    @Test
    fun `computeStreak s arrete au premier jour sous l objectif`() = runTest {
        val today = LocalDate.now(zone)
        mockStepsForDays(
            mapOf(
                today to 12_000L,
                today.minusDays(1) to 5_000L, // rompt la série
                today.minusDays(2) to 10_500L,
            )
        )

        val streak = repository.computeStreak(goalSteps = 10_000L)

        assertEquals(1, streak)
    }

    @Test
    fun `computeStreak exclut aujourd hui s il n atteint pas encore l objectif`() = runTest {
        val today = LocalDate.now(zone)
        mockStepsForDays(
            mapOf(
                today to 3_000L, // objectif pas encore atteint aujourd'hui
                today.minusDays(1) to 11_000L,
            )
        )

        val streak = repository.computeStreak(goalSteps = 10_000L)

        assertEquals(0, streak)
    }

    /**
     * KAN-92 — le streak part toujours d'aujourd'hui, indépendamment du jour affiché.
     * Si l'objectif d'aujourd'hui n'est pas atteint, le streak est 0, même si plusieurs
     * jours passés l'atteignaient (scénario : utilisateur navigue vers hier où il avait
     * atteint son objectif, puis ouvre les Paramètres).
     */
    @Test
    fun `KAN-92 streak reste 0 quand objectif non atteint aujourd hui meme si jours passes atteignaient l objectif`() = runTest {
        val today = LocalDate.now(zone)
        mockStepsForDays(
            mapOf(
                today to 2_000L,                        // aujourd'hui : objectif non atteint
                today.minusDays(1) to 15_000L,          // hier : objectif atteint
                today.minusDays(2) to 12_000L,          // avant-hier : objectif atteint
                today.minusDays(3) to 11_000L,
            )
        )

        val streak = repository.computeStreak(goalSteps = 10_000L)

        // La série doit être 0 car aujourd'hui n'atteint pas l'objectif
        assertEquals(0, streak)
    }
}
