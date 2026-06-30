package com.fviret.podometre.data.weather

/**
 * État météo simplifié pour la bannière de l'écran Activité.
 * Calculé depuis les codes WMO de l'API Open-Meteo.
 */
enum class WeatherState {
    /** Précipitations en cours (code WMO ≥ 51). */
    RAIN_NOW,
    /** Précipitations prévues dans la prochaine heure (code WMO ≥ 51 à h+1). */
    RAIN_SOON,
    /** Pas de précipitations actuelles ni prévues dans l'heure. */
    NO_RAIN,
}

/**
 * Retourne true si un code WMO Open-Meteo indique des précipitations.
 * Codes concernés : 51–55 (bruine), 61–67 (pluie), 71–77 (neige), 80–82 (averses), 85–86, 95–99 (orage).
 */
fun isRainCode(code: Int): Boolean = code in 51..99
