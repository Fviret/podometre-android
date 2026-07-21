package com.fviret.podometre.ui.activity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Tests unitaires pour [formatActiveTime].
 * Vérifie les cas limites autour du seuil 60 minutes (< 1h, pile 1h, > 1h).
 */
class FormatActiveTimeTest {

    @Test
    fun `0 minute retourne 0 min`() {
        assertEquals("0 min", formatActiveTime(0))
    }

    @Test
    fun `1 minute retourne 1 min`() {
        assertEquals("1 min", formatActiveTime(1))
    }

    @Test
    fun `45 minutes retourne 45 min`() {
        assertEquals("45 min", formatActiveTime(45))
    }

    @Test
    fun `59 minutes retourne 59 min (juste sous le seuil)`() {
        assertEquals("59 min", formatActiveTime(59))
    }

    @Test
    fun `60 minutes retourne 1 h (pile le seuil, sans minutes)`() {
        assertEquals("1 h", formatActiveTime(60))
    }

    @Test
    fun `61 minutes retourne 1 h 1 min (juste au-dessus du seuil)`() {
        assertEquals("1 h 1 min", formatActiveTime(61))
    }

    @Test
    fun `74 minutes retourne 1 h 14 min`() {
        assertEquals("1 h 14 min", formatActiveTime(74))
    }

    @Test
    fun `120 minutes retourne 2 h (multiple exact de 60)`() {
        assertEquals("2 h", formatActiveTime(120))
    }

    @Test
    fun `121 minutes retourne 2 h 1 min`() {
        assertEquals("2 h 1 min", formatActiveTime(121))
    }
}
