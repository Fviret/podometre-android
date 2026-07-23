package com.fviret.podometre.ui.activity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

/**
 * Teste [labelForWeatherCode] : conversion des codes WMO Open-Meteo en libellés français.
 * Couvre les codes clés et les cas limites (inconnus, frontières de plages).
 */
class WeatherDetailLabelTest {

    // ── Codes principaux ─────────────────────────────────────────────────────

    @Test
    fun `code 0 retourne Ensoleille`() {
        assertEquals("Ensoleillé", labelForWeatherCode(0))
    }

    @Test
    fun `code 1 retourne Principalement ensoleille`() {
        assertEquals("Principalement ensoleillé", labelForWeatherCode(1))
    }

    @Test
    fun `code 2 retourne Partiellement nuageux`() {
        assertEquals("Partiellement nuageux", labelForWeatherCode(2))
    }

    @Test
    fun `code 3 retourne Couvert`() {
        assertEquals("Couvert", labelForWeatherCode(3))
    }

    // ── Brouillard (45–48) ───────────────────────────────────────────────────

    @Test
    fun `code 45 retourne Brouillard`() {
        assertEquals("Brouillard", labelForWeatherCode(45))
    }

    @Test
    fun `code 48 retourne Brouillard (fin de plage)`() {
        assertEquals("Brouillard", labelForWeatherCode(48))
    }

    // ── Bruine (51–55) ───────────────────────────────────────────────────────

    @Test
    fun `code 51 retourne Bruine`() {
        assertEquals("Bruine", labelForWeatherCode(51))
    }

    // ── Pluie (61–65) ────────────────────────────────────────────────────────

    @Test
    fun `code 61 retourne Pluie`() {
        assertEquals("Pluie", labelForWeatherCode(61))
    }

    @Test
    fun `code 65 retourne Pluie (fin de plage)`() {
        assertEquals("Pluie", labelForWeatherCode(65))
    }

    // ── Neige (71–75) ────────────────────────────────────────────────────────

    @Test
    fun `code 71 retourne Neige`() {
        assertEquals("Neige", labelForWeatherCode(71))
    }

    // ── Averses (80–82) ──────────────────────────────────────────────────────

    @Test
    fun `code 80 retourne Averses`() {
        assertEquals("Averses", labelForWeatherCode(80))
    }

    // ── Orages (95, 96–99) ───────────────────────────────────────────────────

    @Test
    fun `code 95 retourne Orages`() {
        assertEquals("Orages", labelForWeatherCode(95))
    }

    @Test
    fun `code 96 retourne Orages avec grele`() {
        assertEquals("Orages avec grêle", labelForWeatherCode(96))
    }

    @Test
    fun `code 99 retourne Orages avec grele (fin de plage)`() {
        assertEquals("Orages avec grêle", labelForWeatherCode(99))
    }

    // ── Codes inconnus ───────────────────────────────────────────────────────

    @Test
    fun `code inconnu retourne Conditions variables`() {
        assertEquals("Conditions variables", labelForWeatherCode(999))
    }

    @Test
    fun `code negatif retourne Conditions variables`() {
        assertEquals("Conditions variables", labelForWeatherCode(-1))
    }

    // ── Frontières de plages ─────────────────────────────────────────────────

    @Test
    fun `code 44 ne retourne pas Brouillard (hors plage)`() {
        assertNotEquals("Brouillard", labelForWeatherCode(44))
    }

    @Test
    fun `code 49 ne retourne pas Brouillard (hors plage)`() {
        assertNotEquals("Brouillard", labelForWeatherCode(49))
    }

    @Test
    fun `code 4 retourne Conditions variables (entre les codes connus)`() {
        assertEquals("Conditions variables", labelForWeatherCode(4))
    }
}
