package com.fviret.podometre.data.weather

import com.fviret.podometre.ui.activity.labelForWeatherCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Teste le mapping des codes météo WMO vers emoji et libellé français.
 * Couvre les codes principaux définis dans [emojiForWeatherCode] et [labelForWeatherCode].
 */
class WeatherCodeTest {

    // ──────────────────────────────────────────────────
    // emojiForWeatherCode
    // ──────────────────────────────────────────────────

    @Test
    fun `emoji code 0 ensoleille`() {
        assertEquals("☀️", emojiForWeatherCode(0))
    }

    @Test
    fun `emoji code 1 principalement ensoleille`() {
        assertEquals("🌤️", emojiForWeatherCode(1))
    }

    @Test
    fun `emoji code 2 partiellement nuageux`() {
        assertEquals("🌤️", emojiForWeatherCode(2))
    }

    @Test
    fun `emoji code 3 couvert`() {
        assertEquals("☁️", emojiForWeatherCode(3))
    }

    @Test
    fun `emoji code 45 brouillard`() {
        assertEquals("🌫️", emojiForWeatherCode(45))
    }

    @Test
    fun `emoji code 51 bruine`() {
        assertEquals("🌦️", emojiForWeatherCode(51))
    }

    @Test
    fun `emoji code 61 pluie`() {
        assertEquals("🌧️", emojiForWeatherCode(61))
    }

    @Test
    fun `emoji code 71 neige`() {
        assertEquals("❄️", emojiForWeatherCode(71))
    }

    @Test
    fun `emoji code 80 averses`() {
        assertEquals("🌧️", emojiForWeatherCode(80))
    }

    @Test
    fun `emoji code 95 orages`() {
        assertEquals("⛈️", emojiForWeatherCode(95))
    }

    @Test
    fun `emoji code inconnu retourne thermometre`() {
        assertEquals("🌡️", emojiForWeatherCode(999))
    }

    // ──────────────────────────────────────────────────
    // labelForWeatherCode
    // ──────────────────────────────────────────────────

    @Test
    fun `label code 0 ensoleille`() {
        assertEquals("Ensoleillé", labelForWeatherCode(0))
    }

    @Test
    fun `label code 1 principalement ensoleille`() {
        assertEquals("Principalement ensoleillé", labelForWeatherCode(1))
    }

    @Test
    fun `label code 2 partiellement nuageux`() {
        assertEquals("Partiellement nuageux", labelForWeatherCode(2))
    }

    @Test
    fun `label code 3 couvert`() {
        assertEquals("Couvert", labelForWeatherCode(3))
    }

    @Test
    fun `label code 45 brouillard`() {
        assertEquals("Brouillard", labelForWeatherCode(45))
    }

    @Test
    fun `label code 51 bruine`() {
        assertEquals("Bruine", labelForWeatherCode(51))
    }

    @Test
    fun `label code 61 pluie`() {
        assertEquals("Pluie", labelForWeatherCode(61))
    }

    @Test
    fun `label code 71 neige`() {
        assertEquals("Neige", labelForWeatherCode(71))
    }

    @Test
    fun `label code 80 averses`() {
        assertEquals("Averses", labelForWeatherCode(80))
    }

    @Test
    fun `label code 95 orages`() {
        assertEquals("Orages", labelForWeatherCode(95))
    }

    @Test
    fun `label code inconnu retourne conditions variables`() {
        assertEquals("Conditions variables", labelForWeatherCode(999))
    }
}
