package com.fviret.podometre.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Anti-régression et tests structurels pour [PreferenceKeys].
 *
 * Ces tests garantissent que :
 * 1. Aucune clé n'a été renommée par erreur (migration silencieuse).
 * 2. Toutes les clés sont uniques dans le DataStore.
 * 3. Le round-trip typé (Int / Bool / String) + suppression fonctionne
 *    via [UserPreferencesRepository].
 *
 * [FakeDataStore] est une implémentation en mémoire identique à celle de l'androidTest,
 * reproduite ici pour éviter la dépendance entre les deux source sets.
 */
class PreferenceKeysTest {

    private lateinit var repository: UserPreferencesRepository

    /** Implémentation en mémoire de DataStore pour les tests unitaires. */
    private class FakeDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow<Preferences>(mutablePreferencesOf())
        private val mutex = Mutex()
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            mutex.withLock {
                val updated = transform(state.value)
                state.value = updated
                updated
            }
    }

    @BeforeEach
    fun setup() {
        repository = UserPreferencesRepository(FakeDataStore())
    }

    // ────────────────────────────────────────────────────────────────────────
    // 1. Anti-régression — noms de clés historiques
    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun `rawValues reproduisent exactement les cles historiques DataStore`() {
        assertEquals("dailyStepGoal", PreferenceKeys.DAILY_STEP_GOAL.name)
        assertEquals("ringColorId", PreferenceKeys.RING_COLOR_ID.name)
        assertEquals("notificationsEnabled", PreferenceKeys.NOTIFICATIONS_ENABLED.name)
        assertEquals("journeyNotificationsEnabled", PreferenceKeys.JOURNEY_NOTIFICATIONS_ENABLED.name)
        assertEquals("goalNotifiedDate", PreferenceKeys.GOAL_NOTIFIED_DATE.name)
        assertEquals("isDarkMode", PreferenceKeys.IS_DARK_MODE.name)
        assertEquals("completedJourneyIds", PreferenceKeys.COMPLETED_JOURNEY_IDS.name)
        assertEquals("hasCompletedOnboarding", PreferenceKeys.HAS_COMPLETED_ONBOARDING.name)
        assertEquals("showWeatherForecast", PreferenceKeys.SHOW_WEATHER_FORECAST.name)
        assertEquals("showMonthCalendar", PreferenceKeys.SHOW_MONTH_CALENDAR.name)
        assertEquals("showWeeklyChart", PreferenceKeys.SHOW_WEEKLY_CHART.name)
        assertEquals("cachedStepsToday", PreferenceKeys.CACHED_STEPS_TODAY.name)
        assertEquals("cachedStepsTodayDate", PreferenceKeys.CACHED_STEPS_TODAY_DATE.name)
        assertEquals("activeJourneyId", PreferenceKeys.ACTIVE_JOURNEY_ID.name)
        assertEquals("aphorismEnabled", PreferenceKeys.APHORISM_ENABLED.name)
        assertEquals("lastAphorismDate", PreferenceKeys.LAST_APHORISM_DATE.name)
    }

    // ────────────────────────────────────────────────────────────────────────
    // 2. Unicité des noms de clés
    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun `toutes les cles ont des noms uniques`() {
        val names = PreferenceKeys.all.map { it.name }
        assertEquals(
            names.size,
            names.toSet().size,
            "Doublons détectés : ${names.groupBy { it }.filter { it.value.size > 1 }.keys}",
        )
    }

    @Test
    fun `la liste all contient toutes les cles`() {
        // Vérification du nombre de clés déclarées — à mettre à jour si une clé est ajoutée
        assertEquals(18, PreferenceKeys.all.size)
    }

    // ────────────────────────────────────────────────────────────────────────
    // 3. Round-trip typé via UserPreferencesRepository
    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun `round-trip Int - dailyStepGoal`() = runTest {
        repository.setDailyStepGoal(7_500)
        assertEquals(7_500, repository.userPreferences.first().dailyStepGoal)
    }

    @Test
    fun `round-trip String - ringColorId`() = runTest {
        repository.setRingColorId("purple")
        assertEquals("purple", repository.userPreferences.first().ringColorId)
    }

    @Test
    fun `round-trip Boolean - isDarkMode`() = runTest {
        repository.setDarkMode(true)
        assertEquals(true, repository.userPreferences.first().isDarkMode)
    }

    @Test
    fun `round-trip Long - goalNotifiedDate`() = runTest {
        repository.setGoalNotifiedDate(1_720_000_000_000L)
        assertEquals(1_720_000_000_000L, repository.userPreferences.first().goalNotifiedDate)
    }

    @Test
    fun `round-trip String nullable - activeJourneyId set puis supprime`() = runTest {
        val id = "abc-123"
        repository.setActiveJourneyId(id)
        assertEquals(id, repository.userPreferences.first().activeJourneyId)

        repository.setActiveJourneyId(null)
        assertNull(repository.userPreferences.first().activeJourneyId)
    }

    @Test
    fun `round-trip Boolean - aphorismEnabled`() = runTest {
        repository.setAphorismEnabled(false)
        assertEquals(false, repository.userPreferences.first().aphorismEnabled)
        repository.setAphorismEnabled(true)
        assertEquals(true, repository.userPreferences.first().aphorismEnabled)
    }

    @Test
    fun `round-trip String - lastAphorismDate`() = runTest {
        repository.setLastAphorismDate("2026-07-13")
        assertEquals("2026-07-13", repository.userPreferences.first().lastAphorismDate)

        repository.setLastAphorismDate("")
        assertEquals("", repository.userPreferences.first().lastAphorismDate)
    }
}
