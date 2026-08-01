package com.fviret.podometre.domain.model

import android.content.Context
import com.fviret.podometre.R
import java.util.UUID

/** Constante de conversion : 1 pas = 0,8 m = 0,0008 km */
const val KM_PER_STEP = 0.0008

/** Convertit un nombre de pas en distance parcourue, en kilomètres. */
fun stepsToKm(steps: Long): Double = steps * KM_PER_STEP

/**
 * Formate une distance en kilomètres pour l'affichage : entier si la valeur est ronde
 * (ex. "5 km"), une décimale sinon (ex. "2.5 km").
 */
fun formatKm(km: Double): String =
    if (km == km.toLong().toDouble()) "${km.toLong()} km" else "${"%.1f".format(km)} km"

/**
 * Représente un trajet virtuel avec ses jalons kilométriques.
 * Équivalent iOS : struct Journey dans JourneyModels.swift
 */
data class Journey(
    val id: UUID,
    val name: String,
    val subtitle: String,
    val totalKm: Double,
    val category: JourneyCategory,
    val emoji: String,
    val milestones: List<Milestone>
)

/**
 * Un jalon débloqué quand l'utilisateur atteint [km] depuis le début du trajet.
 * Équivalent iOS : struct Milestone dans JourneyModels.swift
 */
data class Milestone(
    val id: UUID,
    val km: Double,
    val label: String,
    val description: String
)

/**
 * Catégories des 19 trajets disponibles.
 * Équivalent iOS : enum JourneyCategory dans JourneyModels.swift
 *
 * [displayName] conserve la valeur française par défaut (utilisée comme fallback
 * et dans les contextes sans [Context]).
 * Préférer [displayName(Context)] dans les Composables pour obtenir la traduction locale.
 */
enum class JourneyCategory(val displayName: String) {
    WALK("Promenades"),
    TRAIL("Sentiers"),
    HISTORY("Histoire"),
    MYTH("Mythes & Épopées")
}

/**
 * Retourne le nom de la catégorie localisé via les ressources string.
 * À utiliser dans les Composables avec [LocalContext.current].
 */
fun JourneyCategory.displayName(context: Context): String = when (this) {
    JourneyCategory.WALK    -> context.getString(R.string.journey_category_walk)
    JourneyCategory.TRAIL   -> context.getString(R.string.journey_category_trail)
    JourneyCategory.HISTORY -> context.getString(R.string.journey_category_history)
    JourneyCategory.MYTH    -> context.getString(R.string.journey_category_myth)
}

/**
 * Retourne le pourcentage de progression du trajet (0.0 à 1.0).
 * Plafonné à 1.0 si la distance dépasse [Journey.totalKm].
 */
fun Journey.progressPercent(progress: JourneyProgress): Double =
    (progress.totalKm / totalKm).coerceIn(0.0, 1.0)

/**
 * Retourne le prochain jalon non encore débloqué, ou null si tous sont débloqués.
 */
fun Journey.nextMilestone(progress: JourneyProgress): Milestone? =
    milestones
        .sortedBy { it.km }
        .firstOrNull { it.id.toString() !in progress.unlockedMilestoneIds }
