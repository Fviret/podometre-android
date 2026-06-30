package com.fviret.podometre.data.preferences

/**
 * Snapshot immutable de toutes les préférences utilisateur.
 * Émis par [UserPreferencesRepository] sous forme de Flow.
 * Équivalent iOS : ensemble des @AppStorage / UserDefaults de l'app.
 */
data class UserPreferences(
    val dailyStepGoal: Int = 10_000,
    val ringColorId: String = "green",
    val notificationsEnabled: Boolean = true,
    val journeyNotificationsEnabled: Boolean = true,
    val goalNotifiedDate: Long = 0L,
    val isDarkMode: Boolean = false,
    val completedJourneyIds: Set<String> = emptySet(),
    val hasCompletedOnboarding: Boolean = false,
    val showWeatherForecast: Boolean = true,
    val showMonthCalendar: Boolean = true,
    val showWeeklyChart: Boolean = true,
    /** Nombre de pas mis en cache par le SyncStepsWorker (fond d'écran). */
    val cachedStepsToday: Long = 0L,
    /** Date ISO (yyyy-MM-dd) de validité du cache de pas. */
    val cachedStepsTodayDate: String = "",
)
