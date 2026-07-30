package com.fviret.podometre.ui.history

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

/**
 * Tests unitaires pour les fonctions pures extraites de [HistoryScreen].
 *
 * Ces fonctions sont `internal` top-level et donc testables sans Android runtime.
 */
class HistoryScreenLogicTest {

    // ── formatSteps ──────────────────────────────────────────────────────────

    @Test
    fun `formatSteps zero retourne 0`() {
        assertEquals("0", formatSteps(0L))
    }

    @Test
    fun `formatSteps inferieur a 1000 sans separateur`() {
        assertEquals("999", formatSteps(999L))
    }

    @Test
    fun `formatSteps 1000 avec separateur`() {
        val result = formatSteps(1_000L)
        assertTrue(result.contains("1"), "doit contenir '1' : $result")
        assertTrue(result.contains("000"), "doit contenir '000' : $result")
    }

    @Test
    fun `formatSteps 10000 pas de virgule dans le resultat`() {
        val result = formatSteps(10_000L)
        assertFalse(result.contains(','), "ne doit pas contenir de virgule : $result")
    }

    @Test
    fun `formatSteps grand nombre est formate`() {
        val result = formatSteps(1_234_567L)
        assertFalse(result.contains(','), "ne doit pas contenir de virgule : $result")
        assertTrue(result.contains("234"), "doit contenir '234' : $result")
    }

    // ── worldTourPercent ─────────────────────────────────────────────────────

    @Test
    fun `worldTourPercent zero km retourne 0`() {
        assertEquals(0.0, worldTourPercent(0.0), 0.001)
    }

    @Test
    fun `worldTourPercent demi tour retourne 50`() {
        assertEquals(50.0, worldTourPercent(20_037.5), 0.1)
    }

    @Test
    fun `worldTourPercent tour complet retourne 100`() {
        assertEquals(100.0, worldTourPercent(40_075.0), 0.001)
    }

    @Test
    fun `worldTourPercent au dela du tour plafonne a 100`() {
        assertEquals(100.0, worldTourPercent(50_000.0), 0.001)
    }

    // ── kmRemaining ──────────────────────────────────────────────────────────

    @Test
    fun `kmRemaining zero km retourne circumference`() {
        assertEquals(40_075.0, kmRemaining(0.0), 0.001)
    }

    @Test
    fun `kmRemaining demi tour retourne moitie circumference`() {
        assertEquals(20_037.5, kmRemaining(20_037.5), 0.1)
    }

    @Test
    fun `kmRemaining tour complet retourne 0`() {
        assertEquals(0.0, kmRemaining(40_075.0), 0.001)
    }

    @Test
    fun `kmRemaining au dela du tour retourne 0 et ne va pas negatif`() {
        assertEquals(0.0, kmRemaining(50_000.0), 0.001)
    }

    // ── weekAverage ──────────────────────────────────────────────────────────

    @Test
    fun `weekAverage zero retourne 0`() {
        assertEquals(0L, weekAverage(0L))
    }

    @Test
    fun `weekAverage negatif retourne 0`() {
        assertEquals(0L, weekAverage(-1L))
    }

    @Test
    fun `weekAverage 70000 retourne 10000`() {
        assertEquals(10_000L, weekAverage(70_000L))
    }

    @Test
    fun `weekAverage 7 pas retourne 1`() {
        assertEquals(1L, weekAverage(7L))
    }

    @Test
    fun `weekAverage non divisible retourne division entiere`() {
        // 10 / 7 = 1 (division entière)
        assertEquals(1L, weekAverage(10L))
    }

    // ── isFutureMonth ────────────────────────────────────────────────────────

    @Test
    fun `isFutureMonth mois courant retourne false`() {
        val today = LocalDate.of(2026, 7, 15)
        assertFalse(isFutureMonth(2026, Month.JULY, today))
    }

    @Test
    fun `isFutureMonth mois passe retourne false`() {
        val today = LocalDate.of(2026, 7, 15)
        assertFalse(isFutureMonth(2026, Month.JANUARY, today))
    }

    @Test
    fun `isFutureMonth mois futur meme annee retourne true`() {
        val today = LocalDate.of(2026, 7, 15)
        assertTrue(isFutureMonth(2026, Month.DECEMBER, today))
    }

    @Test
    fun `isFutureMonth annee passee retourne toujours false`() {
        val today = LocalDate.of(2026, 7, 15)
        assertFalse(isFutureMonth(2025, Month.DECEMBER, today))
    }

    @Test
    fun `isFutureMonth annee future retourne false car displayedYear different`() {
        val today = LocalDate.of(2026, 7, 15)
        // L'année affichée est 2027 > today.year → la condition displayedYear == today.year est fausse
        assertFalse(isFutureMonth(2027, Month.AUGUST, today))
    }

    // ── trendBarHeightRatio ──────────────────────────────────────────────────

    @Test
    fun `trendBarHeightRatio maxVal zero retourne 0`() {
        assertEquals(0f, trendBarHeightRatio(1000L, 0L))
    }

    @Test
    fun `trendBarHeightRatio total zero retourne 0`() {
        assertEquals(0f, trendBarHeightRatio(0L, 10_000L))
    }

    @Test
    fun `trendBarHeightRatio total egal maxVal retourne 1`() {
        assertEquals(1f, trendBarHeightRatio(10_000L, 10_000L), 0.001f)
    }

    @Test
    fun `trendBarHeightRatio total superieur maxVal plafonne a 1`() {
        assertEquals(1f, trendBarHeightRatio(15_000L, 10_000L), 0.001f)
    }

    @Test
    fun `trendBarHeightRatio moitie retourne 0 5`() {
        assertEquals(0.5f, trendBarHeightRatio(5_000L, 10_000L), 0.001f)
    }
}
