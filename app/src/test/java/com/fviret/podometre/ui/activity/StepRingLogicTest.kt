package com.fviret.podometre.ui.activity

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Teste la logique pure de l'écran Activité liée à l'affichage du halo et de la flamme streak.
 * Ces invariants ont été introduits par KAN-90 et KAN-91 :
 * — halo visible uniquement si isToday ET steps >= goal
 * — flamme visible uniquement si isToday ET steps >= goal ET streak > 0
 */
class StepRingLogicTest {

    // ── goalReached = isToday && steps >= goal ───────────────────────────────

    @Test
    fun `halo actif quand isToday et steps atteignent l objectif`() {
        val goalReached = goalReached(isToday = true, steps = 10_000L, goal = 10_000)
        assertTrue(goalReached)
    }

    @Test
    fun `halo actif quand isToday et steps depassent l objectif`() {
        val goalReached = goalReached(isToday = true, steps = 15_000L, goal = 10_000)
        assertTrue(goalReached)
    }

    @Test
    fun `halo inactif quand isToday mais steps sous l objectif`() {
        val goalReached = goalReached(isToday = true, steps = 9_999L, goal = 10_000)
        assertFalse(goalReached)
    }

    @Test
    fun `halo inactif quand objectif atteint mais jour passe (isToday false)`() {
        // KAN-90 : navigation vers un jour passé — pas de halo même si l'objectif était atteint
        val goalReached = goalReached(isToday = false, steps = 12_000L, goal = 10_000)
        assertFalse(goalReached)
    }

    @Test
    fun `halo inactif quand isToday false et steps sous l objectif`() {
        val goalReached = goalReached(isToday = false, steps = 5_000L, goal = 10_000)
        assertFalse(goalReached)
    }

    // ── showStreak = isToday && steps >= goal && streak > 0 ─────────────────

    @Test
    fun `flamme visible quand toutes les conditions sont reunies`() {
        val show = showStreak(isToday = true, steps = 10_000L, goal = 10_000, streak = 3)
        assertTrue(show)
    }

    @Test
    fun `flamme invisible si streak est zero (meme si objectif atteint aujourd hui)`() {
        val show = showStreak(isToday = true, steps = 12_000L, goal = 10_000, streak = 0)
        assertFalse(show)
    }

    @Test
    fun `flamme invisible si jour passe meme si objectif atteint et streak positif`() {
        // KAN-91 / KAN-92 : navigation vers jour passé — flamme masquée
        val show = showStreak(isToday = false, steps = 12_000L, goal = 10_000, streak = 7)
        assertFalse(show)
    }

    @Test
    fun `flamme invisible si steps sous l objectif aujourd hui`() {
        val show = showStreak(isToday = true, steps = 9_000L, goal = 10_000, streak = 5)
        assertFalse(show)
    }

    // Fonctions miroir de la logique de StepRing.kt (extraites pour être testables sans Compose)
    private fun goalReached(isToday: Boolean, steps: Long, goal: Int): Boolean =
        isToday && steps >= goal

    private fun showStreak(isToday: Boolean, steps: Long, goal: Int, streak: Int): Boolean =
        isToday && steps >= goal && streak > 0
}
