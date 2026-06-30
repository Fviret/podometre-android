package com.fviret.podometre.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Teste le mapping DataStore Preferences → UserPreferences dans [UserPreferencesRepository].
 * On mocke uniquement le flow data — les méthodes d'écriture (edit) sont des wrappers directs
 * sur DataStore et n'ont pas de logique propre à tester en unitaire.
 */
class UserPreferencesRepositoryTest {

    private val prefsFlow = MutableStateFlow<Preferences>(mutablePreferencesOf())
    private lateinit var repository: UserPreferencesRepository

    @BeforeEach
    fun setup() {
        val dataStore = mockk<DataStore<Preferences>>()
        every { dataStore.data } returns prefsFlow
        repository = UserPreferencesRepository(dataStore)
    }

    @Test
    fun `les valeurs par defaut sont correctes quand DataStore est vide`() = runTest {
        val prefs = repository.userPreferences.first()
        assertEquals(10_000, prefs.dailyStepGoal)
        assertEquals("green", prefs.ringColorId)
        assertTrue(prefs.notificationsEnabled)
        assertTrue(prefs.journeyNotificationsEnabled)
        assertEquals(0L, prefs.goalNotifiedDate)
        assertFalse(prefs.isDarkMode)
        assertTrue(prefs.completedJourneyIds.isEmpty())
        assertFalse(prefs.hasCompletedOnboarding)
        assertTrue(prefs.showWeatherForecast)
        assertTrue(prefs.showMonthCalendar)
        assertTrue(prefs.showWeeklyChart)
    }

    @Test
    fun `dailyStepGoal est correctement mappé depuis DataStore`() = runTest {
        prefsFlow.value = mutablePreferencesOf(intPreferencesKey("dailyStepGoal") to 7_500)
        assertEquals(7_500, repository.userPreferences.first().dailyStepGoal)
    }

    @Test
    fun `ringColorId est correctement mappé depuis DataStore`() = runTest {
        prefsFlow.value = mutablePreferencesOf(stringPreferencesKey("ringColorId") to "purple")
        assertEquals("purple", repository.userPreferences.first().ringColorId)
    }

    @Test
    fun `isDarkMode est correctement mappé depuis DataStore`() = runTest {
        prefsFlow.value = mutablePreferencesOf(booleanPreferencesKey("isDarkMode") to true)
        assertTrue(repository.userPreferences.first().isDarkMode)
    }

    @Test
    fun `completedJourneyIds est correctement mappé depuis DataStore`() = runTest {
        prefsFlow.value = mutablePreferencesOf(
            stringSetPreferencesKey("completedJourneyIds") to setOf("id-1", "id-2")
        )
        val ids = repository.userPreferences.first().completedJourneyIds
        assertEquals(setOf("id-1", "id-2"), ids)
    }

    @Test
    fun `hasCompletedOnboarding est correctement mappé depuis DataStore`() = runTest {
        prefsFlow.value = mutablePreferencesOf(booleanPreferencesKey("hasCompletedOnboarding") to true)
        assertTrue(repository.userPreferences.first().hasCompletedOnboarding)
    }
}
