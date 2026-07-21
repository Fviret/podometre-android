package com.fviret.podometre.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Teste la clé DataStore [PreferenceKeys.SHOW_TODAY_METRICS] dans [UserPreferencesRepository].
 * Vérifie la valeur par défaut (true) et la lecture d'une valeur explicitement définie à false.
 */
class UserPreferencesShowMetricsTest {

    private val prefsFlow = MutableStateFlow<Preferences>(mutablePreferencesOf())
    private lateinit var repository: UserPreferencesRepository

    @BeforeEach
    fun setup() {
        val dataStore = mockk<DataStore<Preferences>>()
        every { dataStore.data } returns prefsFlow
        repository = UserPreferencesRepository(dataStore)
    }

    @Test
    fun `showTodayMetrics est true par defaut quand DataStore est vide`() = runTest {
        val prefs = repository.userPreferences.first()
        assertTrue(prefs.showTodayMetrics)
    }

    @Test
    fun `showTodayMetrics est false quand DataStore contient false`() = runTest {
        prefsFlow.value = mutablePreferencesOf(
            booleanPreferencesKey("showTodayMetrics") to false
        )
        val prefs = repository.userPreferences.first()
        assertFalse(prefs.showTodayMetrics)
    }

    @Test
    fun `showTodayMetrics est true quand DataStore contient true explicitement`() = runTest {
        prefsFlow.value = mutablePreferencesOf(
            booleanPreferencesKey("showTodayMetrics") to true
        )
        val prefs = repository.userPreferences.first()
        assertTrue(prefs.showTodayMetrics)
    }
}
