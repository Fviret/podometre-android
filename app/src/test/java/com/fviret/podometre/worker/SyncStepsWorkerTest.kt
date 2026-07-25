package com.fviret.podometre.worker

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// ─── isStepsValueValid ───────────────────────────────────────────────────────

/**
 * Teste [shouldNotifyGoalReached], la logique de décision extraite de [SyncStepsWorker]
 * pour la notification "Objectif atteint !" (max 1 fois par jour, respecte le toggle
 * utilisateur). Fonction pure — pas de WorkManager/Hilt à mocker.
 */
class SyncStepsWorkerTest {

    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.of(2026, 7, 2)

    // ─── isStepsValueValid ───────────────────────────────────────────────────

    @Test
    fun `quand readSteps retourne 0 le cache ne doit pas etre mis a jour et le worker doit retenter`() {
        // isStepsValueValid(0L) == false → SyncStepsWorker retourne Result.retry()
        assertFalse(isStepsValueValid(0L))
    }

    @Test
    fun `quand readSteps retourne une valeur positive la mise en cache est autorisee`() {
        assertTrue(isStepsValueValid(1L))
        assertTrue(isStepsValueValid(8_500L))
    }

    // ─── shouldNotifyGoalReached ─────────────────────────────────────────────

    @Test
    fun `notifie quand l'objectif est atteint pour la premiere fois aujourd'hui`() {
        val result = shouldNotifyGoalReached(
            steps = 10_500L,
            dailyStepGoal = 10_000,
            notificationsEnabled = true,
            goalNotifiedDateMs = 0L,
            today = today,
            zone = zone,
        )
        assertTrue(result)
    }

    @Test
    fun `ne notifie pas si les notifications sont desactivees`() {
        val result = shouldNotifyGoalReached(
            steps = 15_000L,
            dailyStepGoal = 10_000,
            notificationsEnabled = false,
            goalNotifiedDateMs = 0L,
            today = today,
            zone = zone,
        )
        assertFalse(result)
    }

    @Test
    fun `ne notifie pas si l'objectif n'est pas encore atteint`() {
        val result = shouldNotifyGoalReached(
            steps = 8_000L,
            dailyStepGoal = 10_000,
            notificationsEnabled = true,
            goalNotifiedDateMs = 0L,
            today = today,
            zone = zone,
        )
        assertFalse(result)
    }

    @Test
    fun `ne notifie pas deux fois le meme jour`() {
        val notifiedTodayMs = today.atStartOfDay(zone).toInstant().toEpochMilli() + 3_600_000L

        val result = shouldNotifyGoalReached(
            steps = 12_000L,
            dailyStepGoal = 10_000,
            notificationsEnabled = true,
            goalNotifiedDateMs = notifiedTodayMs,
            today = today,
            zone = zone,
        )
        assertFalse(result)
    }

    @Test
    fun `notifie a nouveau un nouveau jour meme si deja notifie la veille`() {
        val notifiedYesterdayMs = Instant.ofEpochMilli(
            today.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        ).toEpochMilli()

        val result = shouldNotifyGoalReached(
            steps = 12_000L,
            dailyStepGoal = 10_000,
            notificationsEnabled = true,
            goalNotifiedDateMs = notifiedYesterdayMs,
            today = today,
            zone = zone,
        )
        assertTrue(result)
    }

    @Test
    fun `notifie exactement au seuil de l'objectif`() {
        val result = shouldNotifyGoalReached(
            steps = 10_000L,
            dailyStepGoal = 10_000,
            notificationsEnabled = true,
            goalNotifiedDateMs = 0L,
            today = today,
            zone = zone,
        )
        assertTrue(result)
    }
}
