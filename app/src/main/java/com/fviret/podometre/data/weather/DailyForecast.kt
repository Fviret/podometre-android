package com.fviret.podometre.data.weather

import java.time.LocalDate

/**
 * Prévisions météo pour un jour donné.
 * Construit depuis la réponse Open-Meteo `daily`.
 * Équivalent iOS : DayForecast dans WeatherService.swift
 */
data class DailyForecast(
    val date: LocalDate,
    val weatherCode: Int,
    val tempMaxCelsius: Double,
    val tempMinCelsius: Double,
    /** Total de précipitations en mm. Affiché uniquement si > [PRECIP_THRESHOLD_MM]. */
    val precipitationMm: Double,
) {
    companion object {
        const val PRECIP_THRESHOLD_MM = 0.2
    }
}

/**
 * Retourne l'emoji correspondant à un code WMO Open-Meteo.
 * Référence : https://open-meteo.com/en/docs#weathervariables
 */
fun emojiForWeatherCode(code: Int): String = when {
    code == 0 -> "☀️"
    code in 1..2 -> "🌤️"
    code == 3 -> "☁️"
    code in 45..48 -> "🌫️"
    code in 51..55 -> "🌦️"
    code in 61..67 -> "🌧️"
    code in 71..77 -> "❄️"
    code in 80..82 -> "🌧️"
    code in 85..86 -> "❄️"
    code in 95..99 -> "⛈️"
    else -> "🌡️"
}
