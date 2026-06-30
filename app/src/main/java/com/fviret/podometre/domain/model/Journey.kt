package com.fviret.podometre.domain.model

import java.util.UUID

/** Constante de conversion : 1 pas = 0,8 m = 0,0008 km */
const val KM_PER_STEP = 0.0008

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
 */
enum class JourneyCategory(val displayName: String) {
    WALK("Promenades"),
    TRAIL("Sentiers"),
    HISTORY("Histoire"),
    MYTH("Mythes & Épopées")
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
        .firstOrNull { it.id !in progress.unlockedMilestoneIds }
