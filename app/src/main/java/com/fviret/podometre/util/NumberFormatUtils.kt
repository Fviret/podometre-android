package com.fviret.podometre.util

import java.text.NumberFormat
import java.util.Locale

private val frenchNumberFormat: NumberFormat by lazy {
    NumberFormat.getNumberInstance(Locale.FRENCH)
}

/**
 * Formate un nombre de pas avec séparateur de milliers selon la locale française.
 * Exemple : 10000L → "10 000"
 */
fun Long.formatSteps(): String = frenchNumberFormat.format(this)

/**
 * Formate un nombre de pas (Int) avec séparateur de milliers selon la locale française.
 * Exemple : 10000 → "10 000"
 */
fun Int.formatSteps(): String = toLong().formatSteps()
