package com.fviret.podometre.data.preferences

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

/**
 * Source unique de vérité pour toutes les clés DataStore Preferences.
 *
 * Les noms de clés reproduisent à l'identique les chaînes historiques (aucune migration).
 * [all] permet de vérifier l'unicité et les valeurs brutes dans les tests d'anti-régression.
 *
 * Équivalent iOS : enum `PreferenceKey` (PR #22).
 */
object PreferenceKeys {

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
    val CACHED_STEPS_TODAY = longPreferencesKey("cachedStepsToday")
    val CACHED_STEPS_TODAY_DATE = stringPreferencesKey("cachedStepsTodayDate")
    val ACTIVE_JOURNEY_ID = stringPreferencesKey("activeJourneyId")
    val APHORISM_ENABLED = booleanPreferencesKey("aphorismEnabled")
    val LAST_APHORISM_DATE = stringPreferencesKey("lastAphorismDate")

    /**
     * Liste exhaustive de toutes les clés — utilisée pour les tests d'unicité
     * et d'anti-régression des noms historiques.
     */
    val all: List<Preferences.Key<*>> = listOf(
        DAILY_STEP_GOAL,
        RING_COLOR_ID,
        NOTIFICATIONS_ENABLED,
        JOURNEY_NOTIFICATIONS_ENABLED,
        GOAL_NOTIFIED_DATE,
        IS_DARK_MODE,
        COMPLETED_JOURNEY_IDS,
        HAS_COMPLETED_ONBOARDING,
        SHOW_WEATHER_FORECAST,
        SHOW_MONTH_CALENDAR,
        SHOW_WEEKLY_CHART,
        CACHED_STEPS_TODAY,
        CACHED_STEPS_TODAY_DATE,
        ACTIVE_JOURNEY_ID,
        APHORISM_ENABLED,
        LAST_APHORISM_DATE,
    )
}
