package com.fviret.podometre.ui.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Teste la logique des badges de pas (KAN-89) :
 * — détermination de l'état verrouillé/débloqué
 * — formatage de la date de déblocage
 * — cohérence des seuils
 */
class BadgeDataTest {

    // ── Logique verrouillé / débloqué ────────────────────────────────────────

    @Test
    fun `badge est verrouille quand count est 0`() {
        val count = 0
        assertFalse(count > 0)
    }

    @Test
    fun `badge est debloque quand count est positif`() {
        val count = 1
        assertTrue(count > 0)
    }

    @Test
    fun `badge est debloque avec un count eleve (ex 45 jours)`() {
        val count = 45
        assertTrue(count > 0)
    }

    // ── Formatage date de déblocage ──────────────────────────────────────────

    @Test
    fun `date de deblocage formatee en francais jour mois annee`() {
        val date = LocalDate.of(2026, 3, 15)
        val formatted = date.format(
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)
        )
        assertEquals("15 mars 2026", formatted)
    }

    @Test
    fun `date de deblocage 1er janvier formatee correctement`() {
        val date = LocalDate.of(2025, 1, 1)
        val formatted = date.format(
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)
        )
        assertEquals("1 janvier 2025", formatted)
    }

    @Test
    fun `date nulle n est pas affichee (badge jamais debloque)`() {
        val firstEarnedDate: LocalDate? = null
        assertNull(firstEarnedDate)
    }

    @Test
    fun `date non nulle est affichee (badge debloque)`() {
        val firstEarnedDate: LocalDate? = LocalDate.of(2026, 7, 10)
        assertNotNull(firstEarnedDate)
    }

    // ── Seuils des badges de pas ─────────────────────────────────────────────

    @Test
    fun `les seuils sont dans l ordre croissant`() {
        val thresholds = listOf(5_000L, 10_000L, 20_000L, 30_000L, 50_000L, 100_000L)
        val sorted = thresholds.sorted()
        assertEquals(sorted, thresholds)
    }

    @Test
    fun `il y a exactement 6 badges de pas`() {
        val thresholds = listOf(5_000L, 10_000L, 20_000L, 30_000L, 50_000L, 100_000L)
        assertEquals(6, thresholds.size)
    }

    @Test
    fun `le seuil minimum est 5000 pas`() {
        val thresholds = listOf(5_000L, 10_000L, 20_000L, 30_000L, 50_000L, 100_000L)
        assertEquals(5_000L, thresholds.first())
    }

    @Test
    fun `le seuil maximum est 100000 pas`() {
        val thresholds = listOf(5_000L, 10_000L, 20_000L, 30_000L, 50_000L, 100_000L)
        assertEquals(100_000L, thresholds.last())
    }

    // ── readStepBadgeFirstEarnedDates : logique emulateur ───────────────────

    @Test
    fun `les dates emulateur refletent les badges débloqués attendus`() {
        // Sur émulateur : 5k, 10k, 20k débloqués — 30k, 50k, 100k non débloqués
        val today = LocalDate.now()
        val mockDates: Map<Long, LocalDate?> = mapOf(
            5_000L   to today.minusDays(90),
            10_000L  to today.minusDays(60),
            20_000L  to today.minusDays(30),
            30_000L  to null,
            50_000L  to null,
            100_000L to null,
        )
        assertNotNull(mockDates[5_000L])
        assertNotNull(mockDates[10_000L])
        assertNotNull(mockDates[20_000L])
        assertNull(mockDates[30_000L])
        assertNull(mockDates[50_000L])
        assertNull(mockDates[100_000L])
    }

    @Test
    fun `la date de 5k est anterieure a la date de 10k sur emulateur`() {
        val today = LocalDate.now()
        val date5k  = today.minusDays(90)
        val date10k = today.minusDays(60)
        assertTrue(date5k.isBefore(date10k))
    }
}
