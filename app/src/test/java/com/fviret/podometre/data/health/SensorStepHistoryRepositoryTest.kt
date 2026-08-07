package com.fviret.podometre.data.health

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.LocalDate

/**
 * Tests pour [computeDailyDelta] (fonction pure) et [SensorStepHistoryRepository]
 * (state machine de capture + persistance JSON, KAN-156).
 * Le `Context` est mocké pour rediriger `filesDir` vers un répertoire temporaire réel,
 * comme [com.fviret.podometre.data.journey.JourneyProgressRepositoryTest].
 */
class SensorStepHistoryRepositoryTest {

    // ── computeDailyDelta ──────────────────────────────────────────────────

    @Test
    fun `calcule le delta normal entre deux releves croissants`() {
        assertEquals(1_500L, computeDailyDelta(previousRaw = 10_000L, currentRaw = 11_500L))
    }

    @Test
    fun `retourne 0 quand les deux releves sont identiques`() {
        assertEquals(0L, computeDailyDelta(previousRaw = 5_000L, currentRaw = 5_000L))
    }

    @Test
    fun `retourne 0 quand le capteur a ete reinitialise par un reboot`() {
        // Reboot entre les deux relevés : le compteur cumulatif repart de 0.
        assertEquals(0L, computeDailyDelta(previousRaw = 20_000L, currentRaw = 300L))
    }

    @Test
    fun `gere un premier releve a partir de zero`() {
        assertEquals(8_000L, computeDailyDelta(previousRaw = 0L, currentRaw = 8_000L))
    }

    // ── SensorStepHistoryRepository — recordSnapshot / lectures ──────────────

    @TempDir
    lateinit var tempDir: File

    private lateinit var repository: SensorStepHistoryRepository

    private val day1 = LocalDate.of(2026, 8, 1)
    private val day2 = LocalDate.of(2026, 8, 2)
    private val day3 = LocalDate.of(2026, 8, 3)

    @BeforeEach
    fun setup() {
        val context = mockk<android.content.Context>()
        every { context.filesDir } returns tempDir
        repository = SensorStepHistoryRepository(context)
    }

    @Test
    fun `premier releve etablit seulement la baseline, sans historique`() = runTest {
        repository.recordSnapshot(rawCounterValue = 100_000L, today = day1)
        assertEquals(100_000L, repository.getTodayBaseline(day1))
        assertEquals(0L, repository.readTodayStepsEstimate(day1))
        assertEquals(emptyMap<LocalDate, Long>(), repository.readStepsByDay(day1, day1))
    }

    @Test
    fun `releve du meme jour met a jour l estimation sans deplacer la baseline`() = runTest {
        repository.recordSnapshot(rawCounterValue = 100_000L, today = day1)
        repository.recordSnapshot(rawCounterValue = 103_500L, today = day1)
        assertEquals(100_000L, repository.getTodayBaseline(day1))
        assertEquals(3_500L, repository.readTodayStepsEstimate(day1))
    }

    @Test
    fun `changement de jour finalise la veille dans l historique`() = runTest {
        repository.recordSnapshot(rawCounterValue = 100_000L, today = day1)
        repository.recordSnapshot(rawCounterValue = 107_200L, today = day1)
        repository.recordSnapshot(rawCounterValue = 108_000L, today = day2)

        val history = repository.readStepsByDay(day1, day1)
        assertEquals(7_200L, history[day1])
        // La nouvelle baseline part du relevé qui a détecté le changement de jour.
        assertEquals(108_000L, repository.getTodayBaseline(day2))
        assertEquals(0L, repository.readTodayStepsEstimate(day2))
    }

    @Test
    fun `enchainement de plusieurs jours produit un historique correct`() = runTest {
        repository.recordSnapshot(rawCounterValue = 0L, today = day1)
        repository.recordSnapshot(rawCounterValue = 6_000L, today = day1)
        repository.recordSnapshot(rawCounterValue = 15_000L, today = day2) // finalise day1 = 6 000
        repository.recordSnapshot(rawCounterValue = 22_500L, today = day2)
        repository.recordSnapshot(rawCounterValue = 26_000L, today = day3) // finalise day2 = 7 500

        val history = repository.readStepsByDay(day1, day2)
        assertEquals(6_000L, history[day1])
        assertEquals(7_500L, history[day2])
        assertEquals(13_500L, repository.readSteps(day1, day2))
    }

    @Test
    fun `getTodayBaseline retourne null si aucun releve n a ete fait aujourd hui`() = runTest {
        repository.recordSnapshot(rawCounterValue = 5_000L, today = day1)
        assertNull(repository.getTodayBaseline(day2))
        assertNull(repository.readTodayStepsEstimate(day2))
    }

    @Test
    fun `reboot en cours de journee finalise 0 pas plutot qu une valeur negative`() = runTest {
        repository.recordSnapshot(rawCounterValue = 50_000L, today = day1)
        repository.recordSnapshot(rawCounterValue = 200L, today = day1) // reboot intra-jour
        repository.recordSnapshot(rawCounterValue = 300L, today = day2)

        val history = repository.readStepsByDay(day1, day1)
        assertEquals(0L, history[day1])
    }

    // ── readStepsSince (KAN-158 — fallback distance trajets) ─────────────────

    @Test
    fun `readStepsSince combine jours finalises et estimation du jour courant`() = runTest {
        repository.recordSnapshot(rawCounterValue = 0L, today = day1)
        repository.recordSnapshot(rawCounterValue = 6_000L, today = day1)
        repository.recordSnapshot(rawCounterValue = 15_000L, today = day2) // finalise day1 = 6 000
        repository.recordSnapshot(rawCounterValue = 22_500L, today = day2) // estimation day2 = 7 500

        assertEquals(13_500L, repository.readStepsSince(from = day1, today = day2))
    }

    @Test
    fun `readStepsSince retourne 0 si from est apres aujourd hui`() = runTest {
        repository.recordSnapshot(rawCounterValue = 1_000L, today = day1)
        assertEquals(0L, repository.readStepsSince(from = day2, today = day1))
    }

    @Test
    fun `readStepsSince retourne 0 sans aucun releve`() = runTest {
        assertEquals(0L, repository.readStepsSince(from = day1, today = day1))
    }
}
