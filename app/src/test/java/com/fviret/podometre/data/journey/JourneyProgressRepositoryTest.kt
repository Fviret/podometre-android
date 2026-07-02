package com.fviret.podometre.data.journey

import com.fviret.podometre.domain.model.Journey
import com.fviret.podometre.domain.model.JourneyCategory
import com.fviret.podometre.domain.model.Milestone
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.UUID

/**
 * Teste [JourneyProgressRepository] : détection des jalons débloqués, complétion de trajet,
 * et idempotence de [JourneyProgressRepository.syncJourney] (recalcul depuis startDate,
 * jamais d'incrémentation — cf. CLAUDE.md).
 * Le `Context` est mocké pour rediriger `filesDir` vers un répertoire temporaire réel :
 * la persistance JSON elle-même (I/O fichier) fait partie du comportement testé.
 */
class JourneyProgressRepositoryTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var repository: JourneyProgressRepository

    private val milestone1 = Milestone(UUID.randomUUID(), km = 1.0, label = "Jalon 1", description = "")
    private val milestone2 = Milestone(UUID.randomUUID(), km = 2.5, label = "Jalon 2", description = "")
    private val milestone3 = Milestone(UUID.randomUUID(), km = 5.0, label = "Jalon 3", description = "")

    private val journey = Journey(
        id = UUID.randomUUID(),
        name = "Trajet Test",
        subtitle = "Sous-titre",
        totalKm = 5.0,
        category = JourneyCategory.WALK,
        emoji = "🚶",
        milestones = listOf(milestone1, milestone2, milestone3)
    )

    @BeforeEach
    fun setup() {
        val context = mockk<android.content.Context>()
        every { context.filesDir } returns tempDir
        repository = JourneyProgressRepository(context)
    }

    @Test
    fun `startJourney cree une progression initiale a 0 km`() = runTest {
        repository.startJourney(journey.id.toString())
        val progress = repository.getProgress(journey.id.toString())
        assertNotNull(progress)
        assertEquals(0.0, progress!!.totalKm)
        assertTrue(progress.unlockedMilestoneIds.isEmpty())
    }

    @Test
    fun `startJourney ne reinitialise pas un trajet deja demarre`() = runTest {
        repository.startJourney(journey.id.toString())
        repository.syncJourney(journey, newKm = 1.5)
        repository.startJourney(journey.id.toString())
        assertEquals(1.5, repository.getProgress(journey.id.toString())!!.totalKm)
    }

    @Test
    fun `syncJourney sans progression existante ne fait rien`() = runTest {
        val result = repository.syncJourney(journey, newKm = 3.0)
        assertTrue(result.newlyUnlockedMilestones.isEmpty())
        assertFalse(result.isNewlyCompleted)
        assertNull(repository.getProgress(journey.id.toString()))
    }

    @Test
    fun `syncJourney debloque les jalons dont le km est atteint`() = runTest {
        repository.startJourney(journey.id.toString())

        val result = repository.syncJourney(journey, newKm = 2.5)

        assertEquals(listOf(milestone1, milestone2), result.newlyUnlockedMilestones)
        assertFalse(result.isNewlyCompleted)
        val progress = repository.getProgress(journey.id.toString())!!
        assertEquals(2.5, progress.totalKm)
        assertEquals(setOf(milestone1.id.toString(), milestone2.id.toString()), progress.unlockedMilestoneIds)
    }

    @Test
    fun `syncJourney ne redebloque pas un jalon deja debloque`() = runTest {
        repository.startJourney(journey.id.toString())
        repository.syncJourney(journey, newKm = 1.0)

        val result = repository.syncJourney(journey, newKm = 2.5)

        assertEquals(listOf(milestone2), result.newlyUnlockedMilestones)
    }

    @Test
    fun `syncJourney marque le trajet complete quand le km total est atteint`() = runTest {
        repository.startJourney(journey.id.toString())

        val result = repository.syncJourney(journey, newKm = 5.0)

        assertTrue(result.isNewlyCompleted)
        assertEquals(listOf(milestone1, milestone2, milestone3), result.newlyUnlockedMilestones)
    }

    @Test
    fun `syncJourney n emet la completion qu une seule fois`() = runTest {
        repository.startJourney(journey.id.toString())
        repository.syncJourney(journey, newKm = 5.0)

        // Un second sync à distance égale ou supérieure ne doit pas re-signaler la complétion.
        val result = repository.syncJourney(journey, newKm = 5.0)

        assertFalse(result.isNewlyCompleted)
    }

    @Test
    fun `syncJourney est idempotent — un km inferieur ou egal est ignore`() = runTest {
        repository.startJourney(journey.id.toString())
        repository.syncJourney(journey, newKm = 3.0)

        val result = repository.syncJourney(journey, newKm = 2.0)

        assertTrue(result.newlyUnlockedMilestones.isEmpty())
        assertFalse(result.isNewlyCompleted)
        assertEquals(3.0, repository.getProgress(journey.id.toString())!!.totalKm)
    }

    @Test
    fun `syncJourney recalcule toujours depuis la valeur fournie, jamais par incrementation`() = runTest {
        repository.startJourney(journey.id.toString())
        repository.syncJourney(journey, newKm = 1.0)
        repository.syncJourney(journey, newKm = 4.0)

        val progress = repository.getProgress(journey.id.toString())!!
        // 4.0 remplace 1.0 (pas de somme 1.0 + 4.0)
        assertEquals(4.0, progress.totalKm)
    }

    @Test
    fun `la progression est persistee en JSON et rechargeable via load`() = runTest {
        repository.startJourney(journey.id.toString())
        repository.syncJourney(journey, newKm = 2.5)

        val reloaded = JourneyProgressRepository(mockk<android.content.Context>().also {
            every { it.filesDir } returns tempDir
        })
        reloaded.load()

        val progress = reloaded.getProgress(journey.id.toString())
        assertNotNull(progress)
        assertEquals(2.5, progress!!.totalKm)
    }

    @Test
    fun `deleteProgress supprime la progression d un trajet`() = runTest {
        repository.startJourney(journey.id.toString())
        repository.deleteProgress(journey.id.toString())
        assertNull(repository.getProgress(journey.id.toString()))
    }
}
