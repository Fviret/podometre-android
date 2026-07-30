package com.fviret.podometre.domain

import com.fviret.podometre.domain.model.JourneyCategory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Teste l'intégrité structurelle du catalogue [JourneyData] :
 * unicité des IDs, cohérence des jalons, contraintes par catégorie.
 * Ces tests protègent contre les régressions lors des densifications de trajets (KAN-87, KAN-88, KAN-93).
 */
class JourneyDataIntegrityTest {

    private val allJourneys = JourneyData.all

    // ── Unicité des IDs ──────────────────────────────────────────────────────

    @Test
    fun `tous les journey IDs sont uniques`() {
        val ids = allJourneys.map { it.id.toString() }
        assertEquals(ids.size, ids.toSet().size, "IDs de trajets en doublon : ${ids.groupBy { it }.filter { it.value.size > 1 }.keys}")
    }

    @Test
    fun `tous les milestone IDs sont uniques dans le catalogue complet`() {
        val allMilestoneIds = allJourneys.flatMap { journey ->
            journey.milestones.map { it.id.toString() }
        }
        val duplicates = allMilestoneIds.groupBy { it }.filter { it.value.size > 1 }.keys
        assertTrue(duplicates.isEmpty(), "UUIDs de jalons en doublon : $duplicates")
    }

    // ── Nombre de trajets ────────────────────────────────────────────────────

    @Test
    fun `le catalogue contient exactement 28 trajets`() {
        assertEquals(28, allJourneys.size)
    }

    // ── Cohérence des jalons ─────────────────────────────────────────────────

    @Test
    fun `chaque trajet a au moins 1 jalon`() {
        val empty = allJourneys.filter { it.milestones.isEmpty() }.map { it.name }
        assertTrue(empty.isEmpty(), "Trajets sans jalon : $empty")
    }

    @Test
    fun `les km des jalons sont strictement croissants dans chaque trajet`() {
        val violations = allJourneys.filter { journey ->
            val sorted = journey.milestones.sortedBy { it.km }
            sorted.zipWithNext().any { (a, b) -> b.km <= a.km }
        }.map { it.name }
        assertTrue(violations.isEmpty(), "Jalons non croissants dans : $violations")
    }

    @Test
    fun `le dernier jalon de chaque trajet atteint exactement la distance totale`() {
        val violations = allJourneys.filter { journey ->
            val lastKm = journey.milestones.maxOf { it.km }
            lastKm != journey.totalKm
        }.map { "${it.name} (totalKm=${it.totalKm}, lastMilestone=${it.milestones.maxOf { m -> m.km }})" }
        assertTrue(violations.isEmpty(), "Distance finale incorrecte : $violations")
    }

    // ── Contraintes par catégorie (KAN-93 : Mythes ≥ 8 jalons) ─────────────

    @Test
    fun `chaque trajet Mythes et Epopees a au moins 8 jalons`() {
        val violations = allJourneys
            .filter { it.category == JourneyCategory.MYTH }
            .filter { it.milestones.size < 8 }
            .map { "${it.name} (${it.milestones.size} jalons)" }
        assertTrue(violations.isEmpty(), "Trajets MYTH avec moins de 8 jalons : $violations")
    }

    @Test
    fun `chaque trajet Histoire a au moins 8 jalons`() {
        val violations = allJourneys
            .filter { it.category == JourneyCategory.HISTORY }
            .filter { it.milestones.size < 8 }
            .map { "${it.name} (${it.milestones.size} jalons)" }
        assertTrue(violations.isEmpty(), "Trajets HISTORY avec moins de 8 jalons : $violations")
    }

    // ── Contraintes de contenu ───────────────────────────────────────────────

    @Test
    fun `chaque trajet a un emoji non vide`() {
        val violations = allJourneys.filter { it.emoji.isBlank() }.map { it.name }
        assertTrue(violations.isEmpty(), "Trajets sans emoji : $violations")
    }

    @Test
    fun `chaque jalon a un label et une description non vides`() {
        val violations = allJourneys.flatMap { journey ->
            journey.milestones
                .filter { it.label.isBlank() || it.description.isBlank() }
                .map { "${journey.name} → jalon km=${it.km}" }
        }
        assertTrue(violations.isEmpty(), "Jalons avec contenu vide : $violations")
    }

    @Test
    fun `findById retourne le trajet correspondant`() {
        val journey = allJourneys.first()
        val found = JourneyData.findById(journey.id.toString())
        assertEquals(journey.id, found?.id)
    }

    @Test
    fun `findById retourne null pour un UUID inexistant`() {
        val found = JourneyData.findById("00000000-0000-0000-0000-000000000000")
        assertEquals(null, found)
    }
}
