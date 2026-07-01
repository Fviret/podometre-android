package com.fviret.podometre.data.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

// ── Modèle public ────────────────────────────────────────────────────────────

/**
 * Données météo d'une heure précise (prévision ou actuelle).
 * Équivalent iOS : WeatherData dans WeatherService.swift
 */
data class WeatherData(
    val temperatureCelsius: Double,
    val weatherCode: Int,
    val timestampMs: Long = System.currentTimeMillis()
)

// ── DTOs Open-Meteo ──────────────────────────────────────────────────────────

@Serializable
private data class OpenMeteoResponse(
    val current: CurrentWeather,
    val hourly: HourlyWeather,
    val daily: DailyWeather,
)

@Serializable
private data class CurrentWeather(
    val time: String,
    @SerialName("weather_code") val weatherCode: Int,
    val precipitation: Double,
)

@Serializable
private data class HourlyWeather(
    val time: List<String>,
    @SerialName("weather_code") val weatherCode: List<Int>,
)

@Serializable
private data class DailyWeather(
    val time: List<String>,
    @SerialName("weather_code") val weatherCode: List<Int>,
    @SerialName("temperature_2m_max") val tempMax: List<Double>,
    @SerialName("temperature_2m_min") val tempMin: List<Double>,
    @SerialName("precipitation_sum") val precipitationSum: List<Double>,
)

// ── Repository ───────────────────────────────────────────────────────────────

/**
 * Accès aux données météo via l'API Open-Meteo (gratuite, sans clé).
 * Un seul appel HTTP fournit : état actuel, heure suivante et prévisions 7 jours.
 * Cache 30 min pour éviter les appels redondants.
 * Équivalent iOS : WeatherService.swift
 */
@Singleton
class WeatherRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private var cachedState: WeatherState? = null
    private var cachedForecasts: List<DailyForecast> = emptyList()
    private var cachedData: WeatherData? = null
    private var lastFetchMs: Long = 0L

    /**
     * Retourne le [WeatherState] pour les coordonnées données.
     * Utilise le cache si la dernière récupération date de moins de 30 minutes.
     */
    suspend fun getWeatherState(latitude: Double, longitude: Double): WeatherState? {
        ensureFresh(latitude, longitude)
        return cachedState
    }

    /**
     * Retourne les prévisions journalières sur 7 jours.
     * Utilise le cache partagé avec [getWeatherState].
     */
    suspend fun getDailyForecasts(latitude: Double, longitude: Double): List<DailyForecast> {
        ensureFresh(latitude, longitude)
        return cachedForecasts
    }

    /**
     * Retourne les données météo brutes (code WMO) pour les coordonnées données.
     */
    suspend fun getWeather(latitude: Double, longitude: Double): WeatherData? {
        ensureFresh(latitude, longitude)
        return cachedData
    }

    private suspend fun ensureFresh(latitude: Double, longitude: Double) {
        val now = System.currentTimeMillis()
        if (cachedState != null && now - lastFetchMs < CACHE_DURATION_MS) return
        fetch(latitude, longitude)
    }

    /**
     * Appel HTTP Open-Meteo combiné (current + hourly + daily), met à jour le cache.
     */
    private suspend fun fetch(latitude: Double, longitude: Double) {
        val url = buildUrl(latitude, longitude)
        runCatching {
            val request = Request.Builder().url(url).get().build()
            val body = withContext(Dispatchers.IO) {
                okHttpClient.newCall(request).execute().use { it.body?.string() }
            } ?: return@runCatching

            val response = json.decodeFromString<OpenMeteoResponse>(body)
            val current = response.current
            val hourly = response.hourly
            val daily = response.daily

            cachedState = when {
                isRainCode(current.weatherCode) || current.precipitation > 0 -> WeatherState.RAIN_NOW
                nextHourCode(current.time, hourly)?.let { isRainCode(it) } == true -> WeatherState.RAIN_SOON
                else -> WeatherState.NO_RAIN
            }

            cachedData = WeatherData(temperatureCelsius = 0.0, weatherCode = current.weatherCode)

            cachedForecasts = daily.time.mapIndexedNotNull { i, dateStr ->
                val date = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: return@mapIndexedNotNull null
                DailyForecast(
                    date = date,
                    weatherCode = daily.weatherCode.getOrElse(i) { 0 },
                    tempMaxCelsius = daily.tempMax.getOrElse(i) { 0.0 },
                    tempMinCelsius = daily.tempMin.getOrElse(i) { 0.0 },
                    precipitationMm = daily.precipitationSum.getOrElse(i) { 0.0 },
                )
            }

            lastFetchMs = System.currentTimeMillis()
        }
    }

    /**
     * Retrouve le code WMO de la prochaine heure dans les prévisions horaires.
     */
    private fun nextHourCode(currentTime: String, hourly: HourlyWeather): Int? {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        val current = runCatching { LocalDateTime.parse(currentTime, formatter) }.getOrNull()
            ?: return null
        val nextHour = current.plusHours(1).format(formatter)
        val idx = hourly.time.indexOf(nextHour)
        return if (idx >= 0 && idx < hourly.weatherCode.size) hourly.weatherCode[idx] else null
    }

    private fun buildUrl(lat: Double, lon: Double): String =
        "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$lat&longitude=$lon" +
            "&current=weather_code,precipitation" +
            "&hourly=weather_code" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum" +
            "&forecast_days=7" +
            "&timezone=auto"

    companion object {
        private const val CACHE_DURATION_MS = 30 * 60 * 1_000L
    }
}
