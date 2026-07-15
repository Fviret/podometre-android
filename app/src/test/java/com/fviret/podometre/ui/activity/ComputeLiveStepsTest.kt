package com.fviret.podometre.ui.activity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Tests unitaires pour [computeLiveSteps].
 *
 * Vérifie que la combinaison référence HC + delta capteur est correcte dans tous les cas limites :
 * capteur non initialisé, zéro pas depuis reprise, pas négatifs (capteur rebooté), etc.
 */
class ComputeLiveStepsTest {

    @Test
    fun `retourne hcBaseline quand sensorStart vaut -1 (non initialise)`() {
        assertEquals(5_000L, computeLiveSteps(hcBaseline = 5_000L, sensorStart = -1L, sensorCurrent = 150_000L))
    }

    @Test
    fun `retourne hcBaseline quand aucun pas depuis la reprise (delta zero)`() {
        assertEquals(5_000L, computeLiveSteps(hcBaseline = 5_000L, sensorStart = 150_000L, sensorCurrent = 150_000L))
    }

    @Test
    fun `additionne le delta capteur a la reference HC`() {
        assertEquals(5_050L, computeLiveSteps(hcBaseline = 5_000L, sensorStart = 150_000L, sensorCurrent = 150_050L))
    }

    @Test
    fun `delta negatif (reboot capteur) sature a zero - ne recule pas`() {
        assertEquals(5_000L, computeLiveSteps(hcBaseline = 5_000L, sensorStart = 150_000L, sensorCurrent = 100L))
    }

    @Test
    fun `hcBaseline zero - delta seul compte`() {
        assertEquals(30L, computeLiveSteps(hcBaseline = 0L, sensorStart = 200_000L, sensorCurrent = 200_030L))
    }

    @Test
    fun `gros comptage - pas de debordement Long`() {
        assertEquals(
            10_000L + 50_000L,
            computeLiveSteps(hcBaseline = 10_000L, sensorStart = 1_000_000L, sensorCurrent = 1_050_000L)
        )
    }
}
