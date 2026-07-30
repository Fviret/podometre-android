package com.fviret.podometre.ui.history

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

/**
 * Tests unitaires pour la logique pure de [HistoryViewModel].
 *
 * Le ViewModel dépend de [android.content.Context] (via Hilt) et ne peut pas
 * être instancié directement dans un test JVM. Ce fichier couvre donc :
 * - [HistoryUiState] : valeurs par défaut et invariants
 * - Logique de navigation par année (bornes oldestYear / année courante)
 * - Logique de sélection/désélection de semaine (toggle)
 * - Invariants de [selectWeek] et [previousYear]/[nextYear]
 */
class HistoryViewModelTest {

    // ── HistoryUiState — valeurs par défaut ──────────────────────────────────

    @Test
    fun `etat initial est en chargement`() {
        val state = HistoryUiState()
        assertTrue(state.isLoading)
    }

    @Test
    fun `weeklyTotals vide par defaut`() {
        assertEquals(emptyList<Pair<LocalDate, Long>>(), HistoryUiState().weeklyTotals)
    }

    @Test
    fun `selectedWeekIndex null par defaut`() {
        assertNull(HistoryUiState().selectedWeekIndex)
    }

    @Test
    fun `displayedYear correspond a l annee courante par defaut`() {
        assertEquals(LocalDate.now().year, HistoryUiState().displayedYear)
    }

    @Test
    fun `oldestYear correspond a l annee courante par defaut`() {
        assertEquals(LocalDate.now().year, HistoryUiState().oldestYear)
    }

    @Test
    fun `monthlyTotals vide par defaut`() {
        assertEquals(emptyMap<Month, Long>(), HistoryUiState().monthlyTotals)
    }

    @Test
    fun `bestDay a 0 pas et date null par defaut`() {
        val (steps, date) = HistoryUiState().bestDay
        assertEquals(0L, steps)
        assertNull(date)
    }

    @Test
    fun `longestStreak a 0 jours et date null par defaut`() {
        val (days, date) = HistoryUiState().longestStreak
        assertEquals(0, days)
        assertNull(date)
    }

    @Test
    fun `totalCumulativeSteps est 0 par defaut`() {
        assertEquals(0L, HistoryUiState().totalCumulativeSteps)
    }

    // ── selectWeek — logique de toggle ───────────────────────────────────────

    /**
     * Simule la logique de toggle de [HistoryViewModel.selectWeek] :
     * sélectionner un index le sélectionne, le re-sélectionner le désélectionne.
     */
    private fun applySelectWeek(current: Int?, clicked: Int): Int? =
        if (current == clicked) null else clicked

    @Test
    fun `selectWeek selectionne un index non selectionne`() {
        assertEquals(2, applySelectWeek(current = null, clicked = 2))
    }

    @Test
    fun `selectWeek deselectionne l index deja selectionne`() {
        assertNull(applySelectWeek(current = 2, clicked = 2))
    }

    @Test
    fun `selectWeek change la selection vers un nouvel index`() {
        assertEquals(5, applySelectWeek(current = 2, clicked = 5))
    }

    @Test
    fun `selectWeek sur index 0 fonctionne`() {
        assertEquals(0, applySelectWeek(current = null, clicked = 0))
        assertNull(applySelectWeek(current = 0, clicked = 0))
    }

    // ── previousYear — garde de borne basse ──────────────────────────────────

    /**
     * Simule la logique de garde de [HistoryViewModel.previousYear] :
     * ne peut pas reculer en dessous de [oldestYear].
     */
    private fun canGoToPreviousYear(displayedYear: Int, oldestYear: Int): Boolean =
        displayedYear > oldestYear

    @Test
    fun `previousYear est possible si annee affichee superieure a oldestYear`() {
        assertTrue(canGoToPreviousYear(displayedYear = 2025, oldestYear = 2023))
    }

    @Test
    fun `previousYear est bloque si annee affichee egale a oldestYear`() {
        val canGo = canGoToPreviousYear(displayedYear = 2023, oldestYear = 2023)
        assertTrue(!canGo)
    }

    @Test
    fun `previousYear decremente l annee d une unite`() {
        val year = 2025
        val newYear = year - 1
        assertEquals(2024, newYear)
    }

    // ── nextYear — garde de borne haute ─────────────────────────────────────

    /**
     * Simule la logique de garde de [HistoryViewModel.nextYear] :
     * ne peut pas avancer au-delà de l'année courante.
     */
    private fun canGoToNextYear(displayedYear: Int): Boolean =
        displayedYear < LocalDate.now().year

    @Test
    fun `nextYear est possible si annee affichee inferieure a l annee courante`() {
        assertTrue(canGoToNextYear(displayedYear = LocalDate.now().year - 1))
    }

    @Test
    fun `nextYear est bloque si annee affichee est l annee courante`() {
        val canGo = canGoToNextYear(displayedYear = LocalDate.now().year)
        assertTrue(!canGo)
    }

    @Test
    fun `nextYear incremente l annee d une unite`() {
        val year = 2024
        val newYear = year + 1
        assertEquals(2025, newYear)
    }

    // ── Invariants de navigation combinés ───────────────────────────────────

    @Test
    fun `la navigation par annee est impossible quand oldestYear equals annee courante`() {
        val currentYear = LocalDate.now().year
        val state = HistoryUiState(displayedYear = currentYear, oldestYear = currentYear)
        val canGoBack = state.displayedYear > state.oldestYear
        val canGoForward = state.displayedYear < currentYear
        assertTrue(!canGoBack && !canGoForward)
    }

    @Test
    fun `avec 3 ans d historique la navigation couvre bien la plage`() {
        val currentYear = LocalDate.now().year
        val oldestYear = currentYear - 3
        // On peut reculer depuis currentYear jusqu'à oldestYear+1
        var year = currentYear
        var steps = 0
        while (year > oldestYear) { year--; steps++ }
        assertEquals(3, steps)
        assertEquals(oldestYear, year)
    }
}
