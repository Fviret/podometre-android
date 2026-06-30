package com.fviret.podometre.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

/**
 * Progression d'un trajet pour un utilisateur donné.
 * Persistée en JSON dans `journey_progress.json` (trop volumineuse pour DataStore).
 * Équivalent iOS : struct JourneyProgress dans JourneyModels.swift
 *
 * @property journeyId UUID du trajet suivi
 * @property totalKm Distance totale parcourue depuis [startDate] en kilomètres
 * @property unlockedMilestoneIds UUIDs des jalons débloqués
 * @property startDate Date de début du trajet (référence pour la requête HK idempotente)
 * @property lastUpdatedDate Dernière mise à jour depuis Health Connect
 */
@Serializable
data class JourneyProgress(
    val journeyId: String,           // UUID sérialisé en String
    val totalKm: Double = 0.0,
    val unlockedMilestoneIds: Set<String> = emptySet(),  // UUIDs sérialisés
    val startDateMs: Long = System.currentTimeMillis(),
    val lastUpdatedMs: Long = System.currentTimeMillis()
)
