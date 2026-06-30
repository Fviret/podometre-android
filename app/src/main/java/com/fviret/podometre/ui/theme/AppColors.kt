package com.fviret.podometre.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Couleurs de l'anneau de progression — 6 presets identiques à l'iOS.
 * L'ID est persisté dans DataStore sous la clé `ringColorId`.
 */
object AppColors {

    /** Map ID → Color pour les 6 presets de couleur de l'anneau */
    val ringColorOptions: Map<String, Color> = mapOf(
        "green"  to Color(0xFF33C759),
        "blue"   to Color(0xFF3399F2),
        "orange" to Color(0xFFFF9F1A),
        "red"    to Color(0xFFF23F4C),
        "purple" to Color(0xFFA64CF2),
        "teal"   to Color(0xFF26CCBF)
    )

    /** ID de la couleur par défaut */
    const val defaultRingColorId = "green"

    /** Retourne la Color correspondant à l'ID, ou la couleur verte par défaut */
    fun colorForId(id: String): Color =
        ringColorOptions[id] ?: ringColorOptions[defaultRingColorId]!!
}
