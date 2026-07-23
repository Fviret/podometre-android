package com.fviret.podometre.ui.activity

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Teste [streakColor] : paliers de couleur selon le nombre de jours de série.
 * Couvre les frontières de chaque palier (1, 6, 7, 29, 30, 365).
 */
class StreakColorTest {

    private val orange = Color(0xFFFF9F1A)
    private val rouge  = Color(0xFFF23F4C)
    private val violet = Color(0xFFA64CF2)

    @Test
    fun `1 jour de suite retourne orange`() {
        assertEquals(orange, streakColor(1))
    }

    @Test
    fun `6 jours de suite retourne orange (dernier palier orange)`() {
        assertEquals(orange, streakColor(6))
    }

    @Test
    fun `7 jours de suite retourne rouge (premier palier rouge)`() {
        assertEquals(rouge, streakColor(7))
    }

    @Test
    fun `29 jours de suite retourne rouge (dernier palier rouge)`() {
        assertEquals(rouge, streakColor(29))
    }

    @Test
    fun `30 jours de suite retourne violet (premier palier violet)`() {
        assertEquals(violet, streakColor(30))
    }

    @Test
    fun `365 jours de suite retourne violet`() {
        assertEquals(violet, streakColor(365))
    }

    @Test
    fun `0 jour retourne orange (cas defensif, valeur hors plage normale)`() {
        // streakColor n'est appelé que si streak > 0, mais on vérifie le comportement défensif
        assertEquals(orange, streakColor(0))
    }
}
