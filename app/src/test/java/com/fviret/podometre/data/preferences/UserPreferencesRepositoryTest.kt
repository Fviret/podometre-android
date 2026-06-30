package com.fviret.podometre.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class UserPreferencesRepositoryTest {

    @TempDir
    lateinit var tempDir: File

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var repository: UserPreferencesRepository

    @BeforeEach
    fun setup() {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { File(tempDir, "test_prefs.preferences_pb") }
        )
        repository = UserPreferencesRepository(dataStore)
    }

    @AfterEach
    fun tearDown() {
        // Le fichier temp est nettoyé par @TempDir
    }

    @Test
    fun `les valeurs par defaut sont correctes`() = testScope.runTest {
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
    fun `setDailyStepGoal persiste la valeur`() = testScope.runTest {
        repository.setDailyStepGoal(8_000)
        assertEquals(8_000, repository.userPreferences.first().dailyStepGoal)
    }

    @Test
    fun `setRingColorId persiste la valeur`() = testScope.runTest {
        repository.setRingColorId("blue")
        assertEquals("blue", repository.userPreferences.first().ringColorId)
    }

    @Test
    fun `setDarkMode persiste la valeur`() = testScope.runTest {
        repository.setDarkMode(true)
        assertTrue(repository.userPreferences.first().isDarkMode)
    }

    @Test
    fun `addCompletedJourney ajoute sans ecraser les precedents`() = testScope.runTest {
        repository.addCompletedJourney("journey-1")
        repository.addCompletedJourney("journey-2")
        val ids = repository.userPreferences.first().completedJourneyIds
        assertTrue("journey-1" in ids)
        assertTrue("journey-2" in ids)
        assertEquals(2, ids.size)
    }

    @Test
    fun `setOnboardingCompleted passe le flag a true`() = testScope.runTest {
        assertFalse(repository.userPreferences.first().hasCompletedOnboarding)
        repository.setOnboardingCompleted()
        assertTrue(repository.userPreferences.first().hasCompletedOnboarding)
    }
}
