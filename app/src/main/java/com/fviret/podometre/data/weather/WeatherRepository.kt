package com.fviret.podometre.data.weather

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
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
    @SerialName("temperature_2m") val temperature: List<Double> = emptyList(),
    @SerialName("precipitation_probability") val precipProbability: List<Int> = emptyList(),
)

@Serializable
private data class DailyWeather(
    val time: List<String>,
    @SerialName("weather_code") val weatherCode: List<Int>,
    @SerialName("temperature_2m_max") val tempMax: List<Double>,
    @SerialName("temperature_2m_min") val tempMin: List<Double>,
    @SerialName("precipitation_sum") val precipitationSum: List<Double>,
)

// ── Cache disque ─────────────────────────────────────────────────────────────

/** Entrée persistée sur disque après chaque appel réseau météo réussi. */
@Serializable
private data class WeatherCacheEntry(
    val timestampMs: Long,
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
    val weatherStateName: String,
    val forecasts: List<CachedDailyForecast>,
)

@Serializable
private data class CachedDailyForecast(
    val dateIso: String,
    val weatherCode: Int,
    val tempMaxCelsius: Double,
    val tempMinCelsius: Double,
    val precipitationMm: Double,
    val hourlyForecasts: List<CachedHourlyForecast>,
)

@Serializable
private data class CachedHourlyForecast(
    val hour: Int,
    val tempCelsius: Double,
    val weatherCode: Int,
    val precipProbability: Int,
)

// ── Repository ───────────────────────────────────────────────────────────────

/**
 * Accès aux données météo via l'API Open-Meteo (gratuite, sans clé).
 * Un seul appel HTTP fournit : état actuel, heure suivante et prévisions 7 jours.
 * Cache mémoire 30 min + cache disque pour affichage immédiat au démarrage.
 * Pas d'appel réseau si la position n'a pas bougé de plus de 3 km et que le cache < 30 min.
 * Équivalent iOS : WeatherService.swift / WeatherCache
 */
@Singleton
class WeatherRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private var cachedState: WeatherState? = null
    private var cachedForecasts: List<DailyForecast> = emptyList()
    private var cachedData: WeatherData? = null
    private var cachedCityName: String = ""
    private var lastFetchMs: Long = 0L
    private var lastFetchLat: Double = Double.NaN
    private var lastFetchLon: Double = Double.NaN

    init {
        loadDiskCache()
    }

    /** Retourne le snapshot en mémoire sans déclencher d'appel réseau. Null si aucun cache. */
    fun getCachedSnapshot(): Triple<WeatherState, List<DailyForecast>, String>? {
        val state = cachedState ?: return null
        return Triple(state, cachedForecasts, cachedCityName)
    }

    /** Enregistre le nom de ville résolu (géocodage inverse) pour le persister dans le cache disque. */
    fun updateCachedCityName(cityName: String) {
        if (cachedCityName == cityName) return
        cachedCityName = cityName
        // Mise à jour partielle du fichier si on a déjà des données
        if (cachedState != null && !lastFetchLat.isNaN()) {
            saveDiskCache(lastFetchLat, lastFetchLon)
        }
    }

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
        val cacheAge = now - lastFetchMs
        val positionUnchanged = !lastFetchLat.isNaN() &&
            haversineKm(lastFetchLat, lastFetchLon, latitude, longitude) < POSITION_CHANGE_THRESHOLD_KM
        if (cachedState != null && cacheAge < CACHE_DURATION_MS && positionUnchanged) return
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

            // Indexer les créneaux horaires par date
            val hourlyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
            val hourlyByDate = mutableMapOf<LocalDate, MutableList<HourlyForecast>>()
            hourly.time.forEachIndexed { i, timeStr ->
                val dt = runCatching { LocalDateTime.parse(timeStr, hourlyFormatter) }.getOrNull()
                    ?: return@forEachIndexed
                val date = dt.toLocalDate()
                val entry = HourlyForecast(
                    hour = dt.hour,
                    tempCelsius = hourly.temperature.getOrElse(i) { 0.0 },
                    weatherCode = hourly.weatherCode.getOrElse(i) { 0 },
                    precipProbability = hourly.precipProbability.getOrElse(i) { 0 },
                )
                hourlyByDate.getOrPut(date) { mutableListOf() }.add(entry)
            }

            cachedForecasts = daily.time.mapIndexedNotNull { i, dateStr ->
                val date = runCatching { LocalDate.parse(dateStr) }
                    .onFailure { Log.w(TAG, "Date de prévision invalide : $dateStr", it) }
                    .getOrNull() ?: return@mapIndexedNotNull null
                DailyForecast(
                    date = date,
                    weatherCode = daily.weatherCode.getOrElse(i) { 0 },
                    tempMaxCelsius = daily.tempMax.getOrElse(i) { 0.0 },
                    tempMinCelsius = daily.tempMin.getOrElse(i) { 0.0 },
                    precipitationMm = daily.precipitationSum.getOrElse(i) { 0.0 },
                    hourlyForecasts = hourlyByDate[date] ?: emptyList(),
                )
            }

            lastFetchMs = System.currentTimeMillis()
            lastFetchLat = latitude
            lastFetchLon = longitude
            saveDiskCache(latitude, longitude)
        }.onFailure { Log.w(TAG, "Échec de la récupération météo Open-Meteo, cache non mis à jour", it) }
    }

    /**
     * Retrouve le code WMO de la prochaine heure dans les prévisions horaires.
     */
    private fun nextHourCode(currentTime: String, hourly: HourlyWeather): Int? {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        val current = runCatching { LocalDateTime.parse(currentTime, formatter) }
            .onFailure { Log.w(TAG, "Heure courante invalide : $currentTime", it) }
            .getOrNull()
            ?: return null
        val nextHour = current.plusHours(1).format(formatter)
        val idx = hourly.time.indexOf(nextHour)
        return if (idx >= 0 && idx < hourly.weatherCode.size) hourly.weatherCode[idx] else null
    }

    private fun buildUrl(lat: Double, lon: Double): String =
        "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$lat&longitude=$lon" +
            "&current=weather_code,precipitation" +
            "&hourly=weather_code,temperature_2m,precipitation_probability" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum" +
            "&forecast_days=7" +
            "&timezone=auto"

    /** Charge le cache disque en mémoire au démarrage. Erreurs ignorées silencieusement. */
    private fun loadDiskCache() {
        runCatching {
            val file = diskCacheFile()
            if (!file.exists()) return
            val entry = json.decodeFromString<WeatherCacheEntry>(file.readText())
            cachedState = runCatching { WeatherState.valueOf(entry.weatherStateName) }.getOrNull()
                ?: return
            cachedCityName = entry.cityName
            cachedForecasts = entry.forecasts.mapNotNull { cached ->
                val date = runCatching { LocalDate.parse(cached.dateIso) }.getOrNull()
                    ?: return@mapNotNull null
                DailyForecast(
                    date = date,
                    weatherCode = cached.weatherCode,
                    tempMaxCelsius = cached.tempMaxCelsius,
                    tempMinCelsius = cached.tempMinCelsius,
                    precipitationMm = cached.precipitationMm,
                    hourlyForecasts = cached.hourlyForecasts.map { h ->
                        HourlyForecast(h.hour, h.tempCelsius, h.weatherCode, h.precipProbability)
                    },
                )
            }
            lastFetchMs = entry.timestampMs
            lastFetchLat = entry.latitude
            lastFetchLon = entry.longitude
        }.onFailure { Log.w(TAG, "Impossible de charger le cache météo disque", it) }
    }

    /** Persiste le cache météo sur disque après un appel réseau réussi. */
    private fun saveDiskCache(latitude: Double, longitude: Double) {
        runCatching {
            val entry = WeatherCacheEntry(
                timestampMs = lastFetchMs,
                latitude = latitude,
                longitude = longitude,
                cityName = cachedCityName,
                weatherStateName = cachedState?.name ?: return,
                forecasts = cachedForecasts.map { f ->
                    CachedDailyForecast(
                        dateIso = f.date.toString(),
                        weatherCode = f.weatherCode,
                        tempMaxCelsius = f.tempMaxCelsius,
                        tempMinCelsius = f.tempMinCelsius,
                        precipitationMm = f.precipitationMm,
                        hourlyForecasts = f.hourlyForecasts.map { h ->
                            CachedHourlyForecast(h.hour, h.tempCelsius, h.weatherCode, h.precipProbability)
                        },
                    )
                },
            )
            diskCacheFile().writeText(json.encodeToString(WeatherCacheEntry.serializer(), entry))
        }.onFailure { Log.w(TAG, "Impossible de sauvegarder le cache météo disque", it) }
    }

    private fun diskCacheFile(): File = File(context.filesDir, CACHE_FILE_NAME)

    /** Distance approchée en km entre deux coordonnées (formule de Haversine). */
    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val sinDLat = sin(dLat / 2)
        val sinDLon = sin(dLon / 2)
        val a = sinDLat * sinDLat +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sinDLon * sinDLon
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    companion object {
        private const val TAG = "WeatherRepository"
        private const val CACHE_DURATION_MS = 30 * 60 * 1_000L
        private const val POSITION_CHANGE_THRESHOLD_KM = 3.0
        private const val CACHE_FILE_NAME = "weather_cache.json"
    }
}
