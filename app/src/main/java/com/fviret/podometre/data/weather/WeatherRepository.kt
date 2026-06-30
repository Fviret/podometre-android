package com.fviret.podometre.data.weather

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Données météo d'une heure précise (prévision ou actuelle).
 * Équivalent iOS : WeatherData dans WeatherService.swift
 */
data class WeatherData(
    val temperatureCelsius: Double,
    val weatherCode: Int,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Accès aux données météo via l'API Open-Meteo (gratuite, sans clé).
 * Mise en cache 30 min pour éviter les appels redondants.
 * Équivalent iOS : WeatherService.swift
 *
 * Implémentation complète dans KAN-22 (écran Activité — météo).
 */
@Singleton
class WeatherRepository @Inject constructor() {

    private var cachedData: WeatherData? = null
    private var lastFetchMs: Long = 0L

    /**
     * Retourne les données météo pour les coordonnées données.
     * Utilise le cache si la dernière récupération date de moins de [CACHE_DURATION_MS].
     *
     * @param latitude Latitude de l'utilisateur
     * @param longitude Longitude de l'utilisateur
     */
    suspend fun getWeather(latitude: Double, longitude: Double): WeatherData? {
        val now = System.currentTimeMillis()
        if (cachedData != null && now - lastFetchMs < CACHE_DURATION_MS) {
            return cachedData
        }
        // TODO KAN-22 : appel HTTP Open-Meteo
        return null
    }

    companion object {
        private const val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes
    }
}
