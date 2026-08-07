package com.fviret.podometre.data.health

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Tests unitaires pour [computeDailyDelta], la logique pure de calcul du delta de pas
 * entre deux relevés du capteur `TYPE_STEP_COUNTER` (KAN-156).
 */
class SensorStepHistoryRepositoryTest {

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
}
