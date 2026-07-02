package com.fviret.podometre.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Teste les extensions pures de [Journey] ([progressPercent], [nextMilestone])
 * et la conversion [stepsToKm]. Pas de dépendance Android — logique 100% pure.
 */
class JourneyTest {

    private val milestoneA = Milestone(UUID.randomUUID(), km = 1.0, label = "A", description = "")
    private val milestoneB = Milestone(UUID.randomUUID(), km = 2.5, label = "B", description = "")
    private val milestoneC = Milestone(UUID.randomUUID(), km = 5.0, label = "C", description = "")

    private val journey = Journey(
        id = UUID.randomUUID(),
        name = "Test",
        subtitle = "Test subtitle",
        totalKm = 5.0,
        category = JourneyCategory.WALK,
        emoji = "🚶",
        milestones = listOf(milestoneA, milestoneB, milestoneC)
    )

    // ── progressPercent ─────────────────────────────────────────────────────

    @Test
    fun `progressPercent est 0 quand aucune distance parcourue`() {
        val progress = JourneyProgress(journeyId = journey.id.toString(), totalKm = 0.0)
        assertEquals(0.0, journey.progressPercent(progress))
    }

    @Test
    fun `progressPercent reflete le ratio distance parcourue sur distance totale`() {
        val progress = JourneyProgress(journeyId = journey.id.toString(), totalKm = 2.5)
        assertEquals(0.5, journey.progressPercent(progress))
    }

    @Test
    fun `progressPercent est plafonne a 1 0 si la distance depasse le total`() {
        val progress = JourneyProgress(journeyId = journey.id.toString(), totalKm = 999.0)
        assertEquals(1.0, journey.progressPercent(progress))
    }

    // ── nextMilestone ────────────────────────────────────────────────────────

    @Test
    fun `nextMilestone retourne le premier jalon non debloque par ordre de km`() {
        val progress = JourneyProgress(
            journeyId = journey.id.toString(),
            unlockedMilestoneIds = setOf(milestoneA.id.toString())
        )
        assertEquals(milestoneB, journey.nextMilestone(progress))
    }

    @Test
    fun `nextMilestone retourne null quand tous les jalons sont debloques`() {
        val progress = JourneyProgress(
            journeyId = journey.id.toString(),
            unlockedMilestoneIds = setOf(
                milestoneA.id.toString(),
                milestoneB.id.toString(),
                milestoneC.id.toString(),
            )
        )
        assertNull(journey.nextMilestone(progress))
    }

    @Test
    fun `nextMilestone retourne le premier jalon quand aucun n est debloque`() {
        val progress = JourneyProgress(journeyId = journey.id.toString())
        assertEquals(milestoneA, journey.nextMilestone(progress))
    }

    // ── stepsToKm ────────────────────────────────────────────────────────────

    @Test
    fun `stepsToKm convertit 1250 pas en 1 km`() {
        assertEquals(1.0, stepsToKm(1_250L), 0.0001)
    }

    @Test
    fun `stepsToKm de 0 pas donne 0 km`() {
        assertEquals(0.0, stepsToKm(0L))
    }

    @Test
    fun `stepsToKm de 10000 pas donne 8 km`() {
        assertEquals(8.0, stepsToKm(10_000L), 0.0001)
    }
}
