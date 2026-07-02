package com.fviret.podometre.ui

/**
 * Routes de navigation de l'application.
 * Centralise les identifiants de destination pour éviter les erreurs de typo.
 */
object NavRoutes {
    const val ACTIVITY = "activity"
    const val JOURNEYS = "journeys"
    const val SETTINGS = "settings"

    const val JOURNEY_DETAIL = "journey_detail/{journeyId}"

    /** Construit la route de navigation vers le détail d'un trajet. */
    fun journeyDetail(journeyId: String) = "journey_detail/$journeyId"
}
