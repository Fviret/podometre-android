package com.fviret.podometre.data.preferences

/**
 * Sections optionnelles de l'écran Activité, dans l'ordre par défaut.
 * L'utilisateur peut les réorganiser et les activer/désactiver individuellement.
 */
enum class HomeSection(val id: String) {
    METRICS("metrics"),
    WEATHER("weather"),
    CALENDAR("calendar"),
    CHART("chart");

    companion object {
        private val byId = entries.associateBy { it.id }

        /** Ordre par défaut si aucune préférence n'est persistée. */
        val defaultOrder: List<HomeSection> = listOf(METRICS, WEATHER, CALENDAR, CHART)

        /**
         * Désérialise une chaîne CSV persistée (ex. "weather,metrics,chart,calendar").
         * Renvoie [defaultOrder] si la chaîne est vide, invalide ou incomplète.
         */
        fun fromCsv(csv: String): List<HomeSection> {
            if (csv.isBlank()) return defaultOrder
            val parsed = csv.split(",").mapNotNull { byId[it.trim()] }
            // Ajoute les sections absentes (compatibilité future) en fin de liste
            val missing = defaultOrder.filter { it !in parsed }
            return parsed + missing
        }

        /** Sérialise une liste de sections en chaîne CSV. */
        fun toCsv(order: List<HomeSection>): String = order.joinToString(",") { it.id }
    }
}
