package com.fviret.podometre.worker

import com.fviret.podometre.data.journey.JourneySyncResult
import com.fviret.podometre.domain.model.Milestone
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Teste [SyncJourneyWorker.shouldNotifyJourneyProgress], la logique de décision extraite
 * du worker pour l'envoi des notifications de jalons et de complétion de trajet.
 * Fonction pure — pas de WorkManager/Hilt à mocker.
 */
class SyncJourneyWorkerTest {

    private fun milestone(km: Double = 10.0) = Milestone(
        id = UUID.randomUUID(),
        km = km,
        label = "Jalon test",
        description = "Description test",
    )

    @Test
    fun `notifie quand un jalon est nouvellement debloque et les notifications sont activees`() {
        val result = SyncJourneyWorker.shouldNotifyJourneyProgress(
            journeyNotificationsEnabled = true,
            syncResult = JourneySyncResult(
                newlyUnlockedMilestones = listOf(milestone()),
                isNewlyCompleted = false,
            ),
        )
        assertTrue(result)
    }

    @Test
    fun `ne notifie pas si les notifications de trajet sont desactivees`() {
        val result = SyncJourneyWorker.shouldNotifyJourneyProgress(
            journeyNotificationsEnabled = false,
            syncResult = JourneySyncResult(
                newlyUnlockedMilestones = listOf(milestone()),
                isNewlyCompleted = false,
            ),
        )
        assertFalse(result)
    }

    @Test
    fun `ne notifie pas si le resultat est vide (aucun jalon, non complete)`() {
        val result = SyncJourneyWorker.shouldNotifyJourneyProgress(
            journeyNotificationsEnabled = true,
            syncResult = JourneySyncResult(
                newlyUnlockedMilestones = emptyList(),
                isNewlyCompleted = false,
            ),
        )
        assertFalse(result)
    }

    @Test
    fun `notifie quand le trajet est nouvellement complete meme sans jalon supplementaire`() {
        val result = SyncJourneyWorker.shouldNotifyJourneyProgress(
            journeyNotificationsEnabled = true,
            syncResult = JourneySyncResult(
                newlyUnlockedMilestones = emptyList(),
                isNewlyCompleted = true,
            ),
        )
        assertTrue(result)
    }

    @Test
    fun `ne notifie pas si trajet complete mais notifications desactivees`() {
        val result = SyncJourneyWorker.shouldNotifyJourneyProgress(
            journeyNotificationsEnabled = false,
            syncResult = JourneySyncResult(
                newlyUnlockedMilestones = emptyList(),
                isNewlyCompleted = true,
            ),
        )
        assertFalse(result)
    }

    @Test
    fun `notifie quand plusieurs jalons debloquees et trajet complete simultanement`() {
        val result = SyncJourneyWorker.shouldNotifyJourneyProgress(
            journeyNotificationsEnabled = true,
            syncResult = JourneySyncResult(
                newlyUnlockedMilestones = listOf(milestone(10.0), milestone(20.0)),
                isNewlyCompleted = true,
            ),
        )
        assertTrue(result)
    }

    @Test
    fun `Health Connect gagne meme quand sa valeur est inferieure a l'estimation capteur`() {
        val result = resolveJourneyDistanceKm(hcKm = 2.0, fallbackKm = 8.0)
        assertEquals(2.0, result, 0.0001)
    }

    @Test
    fun `Health Connect gagne quand sa valeur est superieure a l'estimation capteur`() {
        val result = resolveJourneyDistanceKm(hcKm = 8.0, fallbackKm = 2.0)
        assertEquals(8.0, result, 0.0001)
    }

    @Test
    fun `le fallback capteur ne s'applique que si Health Connect n'a rien d'exploitable`() {
        val result = resolveJourneyDistanceKm(hcKm = 0.0, fallbackKm = 3.5)
        assertEquals(3.5, result, 0.0001)
    }

    @Test
    fun `Health Connect negatif est traite comme non exploitable`() {
        val result = resolveJourneyDistanceKm(hcKm = -1.0, fallbackKm = 1.5)
        assertEquals(1.5, result, 0.0001)
    }
}
