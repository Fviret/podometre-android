package com.fviret.podometre.ui.activity

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Teste la logique de déclenchement du haptic de franchissement d'objectif (KAN-90).
 * Condition : offset == 0 ET prev >= 0 ET prev < goal ET current >= goal.
 *
 * Ces tests couvrent les scénarios de navigation entre jours qui causaient
 * des haptics fantômes avant le correctif.
 */
class HapticTriggerLogicTest {

    /** Miroir exact de la condition dans ActivityScreen.kt. */
    private fun shouldTriggerHaptic(
        selectedDayOffset: Int,
        prev: Long,
        current: Long,
        goal: Long,
    ): Boolean = selectedDayOffset == 0 && prev >= 0L && prev < goal && current >= goal

    // ── Cas normaux ──────────────────────────────────────────────────────────

    @Test
    fun `haptic declenche quand objectif franchi aujourd hui`() {
        assertTrue(shouldTriggerHaptic(selectedDayOffset = 0, prev = 9_999L, current = 10_000L, goal = 10_000L))
    }

    @Test
    fun `haptic declenche quand objectif depasse (pas au dela du seuil)`() {
        assertTrue(shouldTriggerHaptic(selectedDayOffset = 0, prev = 9_000L, current = 11_000L, goal = 10_000L))
    }

    // ── Sentinelle initiale ──────────────────────────────────────────────────

    @Test
    fun `haptic non declenche a la premiere composition (prev est -1)`() {
        // KAN-90 : prevStepsRef initialisé à -1L pour éviter le haptic au chargement
        assertFalse(shouldTriggerHaptic(selectedDayOffset = 0, prev = -1L, current = 15_000L, goal = 10_000L))
    }

    // ── Navigation jours passés ──────────────────────────────────────────────

    @Test
    fun `haptic non declenche sur un jour passe meme si objectif franchi`() {
        // KAN-90 : navigation vers hier — pas de haptic fantôme
        assertFalse(shouldTriggerHaptic(selectedDayOffset = -1, prev = 9_500L, current = 10_500L, goal = 10_000L))
    }

    @Test
    fun `haptic non declenche sur offset negatif quelconque`() {
        assertFalse(shouldTriggerHaptic(selectedDayOffset = -7, prev = 0L, current = 20_000L, goal = 10_000L))
    }

    // ── Retour sur aujourd'hui après navigation ───────────────────────────────

    @Test
    fun `haptic non declenche au retour si objectif deja atteint (prev reset a -1)`() {
        // KAN-90 : après reset du prevStepsRef, prev == -1L → pas de haptic
        assertFalse(shouldTriggerHaptic(selectedDayOffset = 0, prev = -1L, current = 12_000L, goal = 10_000L))
    }

    // ── Objectif non encore atteint ──────────────────────────────────────────

    @Test
    fun `haptic non declenche si current reste sous l objectif`() {
        assertFalse(shouldTriggerHaptic(selectedDayOffset = 0, prev = 5_000L, current = 9_999L, goal = 10_000L))
    }

    @Test
    fun `haptic non declenche si prev etait deja au dessus de l objectif`() {
        // L'objectif était déjà atteint au tick précédent
        assertFalse(shouldTriggerHaptic(selectedDayOffset = 0, prev = 10_500L, current = 11_000L, goal = 10_000L))
    }
}
