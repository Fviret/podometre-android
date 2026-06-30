package com.fviret.podometre.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Couche d'accès aux préférences persistantes via DataStore Preferences.
 * Toutes les clés sont définies ici et nulle part ailleurs.
 * Équivalent iOS : UserDefaults + @AppStorage.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    // ── Clés DataStore ──────────────────────────────────────────────────────

    private object Keys {
        val DAILY_STEP_GOAL = intPreferencesKey("dailyStepGoal")
        val RING_COLOR_ID = stringPreferencesKey("ringColorId")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notificationsEnabled")
        val JOURNEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("journeyNotificationsEnabled")
        val GOAL_NOTIFIED_DATE = longPreferencesKey("goalNotifiedDate")
        val IS_DARK_MODE = booleanPreferencesKey("isDarkMode")
        val COMPLETED_JOURNEY_IDS = stringSetPreferencesKey("completedJourneyIds")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("hasCompletedOnboarding")
        val SHOW_WEATHER_FORECAST = booleanPreferencesKey("showWeatherForecast")
        val SHOW_MONTH_CALENDAR = booleanPreferencesKey("showMonthCalendar")
        val SHOW_WEEKLY_CHART = booleanPreferencesKey("showWeeklyChart")
    }

    // ── Lecture ─────────────────────────────────────────────────────────────

    /**
     * Flow émettant un [UserPreferences] à chaque modification d'une clé.
     * À collecter dans les ViewModels via [stateIn] ou [collectAsStateWithLifecycle].
     */
    val userPreferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            dailyStepGoal = prefs[Keys.DAILY_STEP_GOAL] ?: 10_000,
            ringColorId = prefs[Keys.RING_COLOR_ID] ?: "green",
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            journeyNotificationsEnabled = prefs[Keys.JOURNEY_NOTIFICATIONS_ENABLED] ?: true,
            goalNotifiedDate = prefs[Keys.GOAL_NOTIFIED_DATE] ?: 0L,
            isDarkMode = prefs[Keys.IS_DARK_MODE] ?: false,
            completedJourneyIds = prefs[Keys.COMPLETED_JOURNEY_IDS] ?: emptySet(),
            hasCompletedOnboarding = prefs[Keys.HAS_COMPLETED_ONBOARDING] ?: false,
            showWeatherForecast = prefs[Keys.SHOW_WEATHER_FORECAST] ?: true,
            showMonthCalendar = prefs[Keys.SHOW_MONTH_CALENDAR] ?: true,
            showWeeklyChart = prefs[Keys.SHOW_WEEKLY_CHART] ?: true,
        )
    }

    // ── Écriture ─────────────────────────────────────────────────────────────

    /** Met à jour l'objectif de pas quotidien (5 000–20 000 par pas de 500). */
    suspend fun setDailyStepGoal(goal: Int) {
        dataStore.edit { it[Keys.DAILY_STEP_GOAL] = goal }
    }

    /** Met à jour l'identifiant de couleur de l'anneau (green, blue, orange…). */
    suspend fun setRingColorId(colorId: String) {
        dataStore.edit { it[Keys.RING_COLOR_ID] = colorId }
    }

    /** Active ou désactive les notifications de goal. */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    /** Active ou désactive les notifications de jalons de trajet. */
    suspend fun setJourneyNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.JOURNEY_NOTIFICATIONS_ENABLED] = enabled }
    }

    /** Enregistre le timestamp (ms) de la dernière notification d'objectif atteint. */
    suspend fun setGoalNotifiedDate(timestampMs: Long) {
        dataStore.edit { it[Keys.GOAL_NOTIFIED_DATE] = timestampMs }
    }

    /** Bascule entre mode sombre et clair. */
    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { it[Keys.IS_DARK_MODE] = enabled }
    }

    /** Ajoute un trajet à la liste des trajets complétés. */
    suspend fun addCompletedJourney(journeyId: String) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.COMPLETED_JOURNEY_IDS] ?: emptySet()
            prefs[Keys.COMPLETED_JOURNEY_IDS] = current + journeyId
        }
    }

    /** Marque l'onboarding comme terminé. */
    suspend fun setOnboardingCompleted() {
        dataStore.edit { it[Keys.HAS_COMPLETED_ONBOARDING] = true }
    }

    /** Afficher ou masquer la bannière météo sur l'écran Activité. */
    suspend fun setShowWeatherForecast(show: Boolean) {
        dataStore.edit { it[Keys.SHOW_WEATHER_FORECAST] = show }
    }

    /** Afficher ou masquer le calendrier mensuel sur l'écran Activité. */
    suspend fun setShowMonthCalendar(show: Boolean) {
        dataStore.edit { it[Keys.SHOW_MONTH_CALENDAR] = show }
    }

    /** Afficher ou masquer le graphe comparatif hebdomadaire sur l'écran Activité. */
    suspend fun setShowWeeklyChart(show: Boolean) {
        dataStore.edit { it[Keys.SHOW_WEEKLY_CHART] = show }
    }
}
