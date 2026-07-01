package com.fviret.podometre.data.weather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
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

// ── Repository ───────────────────────────────────────────────────────────────

/**
 * Accès aux données météo via l'API Open-Meteo (gratuite, sans clé).
 * Mise en cache 30 min pour éviter les appels redondants.
 * Équivalent iOS : WeatherService.swift
 */
@Singleton
class WeatherRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private var cachedState: WeatherState? = null
    private var lastFetchMs: Long = 0L
    private var cachedData: WeatherData? = null

    /**
     * Retourne le [WeatherState] pour les coordonnées données.
     * Utilise le cache si la dernière récupération date de moins de 30 minutes.
     */
    suspend fun getWeatherState(latitude: Double, longitude: Double): WeatherState? {
        val now = System.currentTimeMillis()
        if (cachedState != null && now - lastFetchMs < CACHE_DURATION_MS) {
            return cachedState
        }
        return fetchWeatherState(latitude, longitude)
    }

    /**
     * Retourne les données météo brutes (température, code WMO) pour les coordonnées données.
     * Utilisé pour afficher la température dans la bannière.
     */
    suspend fun getWeather(latitude: Double, longitude: Double): WeatherData? {
        val now = System.currentTimeMillis()
        if (cachedData != null && now - lastFetchMs < CACHE_DURATION_MS) {
            return cachedData
        }
        fetchWeatherState(latitude, longitude)
        return cachedData
    }

    /**
     * Appel HTTP Open-Meteo, parse la réponse et met à jour le cache.
     * Détermine [WeatherState] depuis le code WMO courant et celui de la prochaine heure.
     */
    private suspend fun fetchWeatherState(latitude: Double, longitude: Double): WeatherState? {
        val url = buildUrl(latitude, longitude)
        return runCatching {
            val request = Request.Builder().url(url).get().build()
            val body = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                okHttpClient.newCall(request).execute().use { it.body?.string() }
            } ?: return@runCatching null

            val response = json.decodeFromString<OpenMeteoResponse>(body)
            val current = response.current
            val hourly = response.hourly

            val state = when {
                isRainCode(current.weatherCode) || current.precipitation > 0 -> WeatherState.RAIN_NOW
                nextHourCode(current.time, hourly)?.let { isRainCode(it) } == true -> WeatherState.RAIN_SOON
                else -> WeatherState.NO_RAIN
            }

            cachedState = state
            cachedData = WeatherData(
                temperatureCelsius = 0.0, // non utilisé dans la bannière
                weatherCode = current.weatherCode,
            )
            lastFetchMs = System.currentTimeMillis()
            state
        }.getOrNull()
    }

    /**
     * Retrouve le code WMO de la prochaine heure dans les prévisions horaires.
     * [currentTime] est au format ISO "yyyy-MM-dd'T'HH:mm".
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
            "&forecast_days=1" +
            "&timezone=auto"

    companion object {
        private const val CACHE_DURATION_MS = 30 * 60 * 1_000L
    }
}
