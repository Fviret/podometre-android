package com.fviret.podometre.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Couche d'accès aux préférences persistantes via DataStore Preferences.
 * Toutes les clés sont définies dans [PreferenceKeys] (source unique de vérité).
 * Équivalent iOS : UserDefaults + @AppStorage.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    // ── Lecture ─────────────────────────────────────────────────────────────

    /**
     * Flow émettant un [UserPreferences] à chaque modification d'une clé.
     * À collecter dans les ViewModels via [stateIn] ou [collectAsStateWithLifecycle].
     */
    val userPreferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            dailyStepGoal = prefs[PreferenceKeys.DAILY_STEP_GOAL] ?: 10_000,
            ringColorId = prefs[PreferenceKeys.RING_COLOR_ID] ?: "green",
            notificationsEnabled = prefs[PreferenceKeys.NOTIFICATIONS_ENABLED] ?: true,
            journeyNotificationsEnabled = prefs[PreferenceKeys.JOURNEY_NOTIFICATIONS_ENABLED] ?: true,
            goalNotifiedDate = prefs[PreferenceKeys.GOAL_NOTIFIED_DATE] ?: 0L,
            isDarkMode = prefs[PreferenceKeys.IS_DARK_MODE] ?: false,
            completedJourneyIds = prefs[PreferenceKeys.COMPLETED_JOURNEY_IDS] ?: emptySet(),
            hasCompletedOnboarding = prefs[PreferenceKeys.HAS_COMPLETED_ONBOARDING] ?: false,
            showWeatherForecast = prefs[PreferenceKeys.SHOW_WEATHER_FORECAST] ?: true,
            showMonthCalendar = prefs[PreferenceKeys.SHOW_MONTH_CALENDAR] ?: true,
            showWeeklyChart = prefs[PreferenceKeys.SHOW_WEEKLY_CHART] ?: true,
            cachedStepsToday = prefs[PreferenceKeys.CACHED_STEPS_TODAY] ?: 0L,
            cachedStepsTodayDate = prefs[PreferenceKeys.CACHED_STEPS_TODAY_DATE] ?: "",
            activeJourneyId = prefs[PreferenceKeys.ACTIVE_JOURNEY_ID],
            aphorismEnabled = prefs[PreferenceKeys.APHORISM_ENABLED] ?: true,
            lastAphorismDate = prefs[PreferenceKeys.LAST_APHORISM_DATE] ?: "",
            showTodayMetrics = prefs[PreferenceKeys.SHOW_TODAY_METRICS] ?: true,
            lastWeeklyRecapDate = prefs[PreferenceKeys.LAST_WEEKLY_RECAP_DATE] ?: "",
        )
    }

    // ── Écriture ─────────────────────────────────────────────────────────────

    /** Met à jour l'objectif de pas quotidien (5 000–20 000 par pas de 500). */
    suspend fun setDailyStepGoal(goal: Int) {
        dataStore.edit { it[PreferenceKeys.DAILY_STEP_GOAL] = goal }
    }

    /** Met à jour l'identifiant de couleur de l'anneau (green, blue, orange…). */
    suspend fun setRingColorId(colorId: String) {
        dataStore.edit { it[PreferenceKeys.RING_COLOR_ID] = colorId }
    }

    /** Active ou désactive les notifications de goal. */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferenceKeys.NOTIFICATIONS_ENABLED] = enabled }
    }

    /** Active ou désactive les notifications de jalons de trajet. */
    suspend fun setJourneyNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferenceKeys.JOURNEY_NOTIFICATIONS_ENABLED] = enabled }
    }

    /** Enregistre le timestamp (ms) de la dernière notification d'objectif atteint. */
    suspend fun setGoalNotifiedDate(timestampMs: Long) {
        dataStore.edit { it[PreferenceKeys.GOAL_NOTIFIED_DATE] = timestampMs }
    }

    /** Bascule entre mode sombre et clair. */
    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { it[PreferenceKeys.IS_DARK_MODE] = enabled }
    }

    /** Ajoute un trajet à la liste des trajets complétés. */
    suspend fun addCompletedJourney(journeyId: String) {
        dataStore.edit { prefs ->
            val current = prefs[PreferenceKeys.COMPLETED_JOURNEY_IDS] ?: emptySet()
            prefs[PreferenceKeys.COMPLETED_JOURNEY_IDS] = current + journeyId
        }
    }

    /** Marque l'onboarding comme terminé. */
    suspend fun setOnboardingCompleted() {
        dataStore.edit { it[PreferenceKeys.HAS_COMPLETED_ONBOARDING] = true }
    }

    /** Afficher ou masquer la bannière météo sur l'écran Activité. */
    suspend fun setShowWeatherForecast(show: Boolean) {
        dataStore.edit { it[PreferenceKeys.SHOW_WEATHER_FORECAST] = show }
    }

    /** Afficher ou masquer le calendrier mensuel sur l'écran Activité. */
    suspend fun setShowMonthCalendar(show: Boolean) {
        dataStore.edit { it[PreferenceKeys.SHOW_MONTH_CALENDAR] = show }
    }

    /** Afficher ou masquer le graphe comparatif hebdomadaire sur l'écran Activité. */
    suspend fun setShowWeeklyChart(show: Boolean) {
        dataStore.edit { it[PreferenceKeys.SHOW_WEEKLY_CHART] = show }
    }

    /**
     * Écrit le nombre de pas mis en cache par le SyncStepsWorker.
     * [date] est la date ISO yyyy-MM-dd correspondant au comptage, pour invalider le cache le lendemain.
     */
    suspend fun updateCachedSteps(steps: Long, date: String) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.CACHED_STEPS_TODAY] = steps
            prefs[PreferenceKeys.CACHED_STEPS_TODAY_DATE] = date
        }
    }

    /** Afficher ou masquer la rangée de métriques sous l'anneau. */
    suspend fun setShowTodayMetrics(show: Boolean) {
        dataStore.edit { it[PreferenceKeys.SHOW_TODAY_METRICS] = show }
    }

    /** Active ou désactive la popup "Pensée du jour". */
    suspend fun setAphorismEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferenceKeys.APHORISM_ENABLED] = enabled }
    }

    /** Enregistre la date ISO du dernier affichage de la popup "Pensée du jour". */
    suspend fun setLastAphorismDate(date: String) {
        dataStore.edit { it[PreferenceKeys.LAST_APHORISM_DATE] = date }
    }

    /** Enregistre la date ISO du lundi pour lequel le récapitulatif hebdo a été affiché. */
    suspend fun setLastWeeklyRecapDate(date: String) {
        dataStore.edit { it[PreferenceKeys.LAST_WEEKLY_RECAP_DATE] = date }
    }

    /**
     * Définit l'UUID du trajet actuellement actif.
     * Passer null pour indiquer qu'aucun trajet n'est en cours.
     */
    suspend fun setActiveJourneyId(journeyId: String?) {
        dataStore.edit { prefs ->
            if (journeyId != null) {
                prefs[PreferenceKeys.ACTIVE_JOURNEY_ID] = journeyId
            } else {
                prefs.remove(PreferenceKeys.ACTIVE_JOURNEY_ID)
            }
        }
    }
}
