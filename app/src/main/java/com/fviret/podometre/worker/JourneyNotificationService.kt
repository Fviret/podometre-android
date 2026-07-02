package com.fviret.podometre.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.fviret.podometre.R
import com.fviret.podometre.domain.model.Journey
import com.fviret.podometre.domain.model.Milestone
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service d'envoi de notifications locales pour la progression des trajets.
 * Gère deux types de notifications : jalon débloqué et trajet terminé.
 * Équivalent iOS : JourneyNotificationService.swift
 */
@Singleton
class JourneyNotificationService @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * Envoie une notification "Nouvelle étape débloquée !" avec le nom et l'extrait de description du jalon.
     * L'ID de notification est dérivé de l'UUID du jalon pour garantir l'unicité.
     */
    fun notifyMilestoneUnlocked(milestone: Milestone) {
        val excerpt = milestone.description.take(80).let {
            if (milestone.description.length > 80) "$it…" else it
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_JOURNEY)
            .setSmallIcon(R.drawable.ic_notification_journey)
            .setContentTitle("Nouvelle étape débloquée !")
            .setContentText("${milestone.label} — $excerpt")
            .setStyle(NotificationCompat.BigTextStyle().bigText("${milestone.label}\n\n$excerpt"))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notificationManager.notify(milestone.id.hashCode(), notification)
    }

    /**
     * Envoie une notification "Trajet terminé ! 🏁" avec le nom du trajet et sa distance totale.
     * L'ID est dérivé de l'UUID du trajet.
     */
    fun notifyJourneyCompleted(journey: Journey) {
        val distanceStr = formatKm(journey.totalKm)
        val notification = NotificationCompat.Builder(context, CHANNEL_JOURNEY)
            .setSmallIcon(R.drawable.ic_notification_journey)
            .setContentTitle("Trajet terminé ! 🏁")
            .setContentText("${journey.emoji} ${journey.name} — $distanceStr")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notificationManager.notify(journey.id.hashCode(), notification)
    }

    private fun formatKm(km: Double): String =
        if (km == km.toLong().toDouble()) "${km.toLong()} km" else "${"%.1f".format(km)} km"

    companion object {
        const val CHANNEL_JOURNEY = "journey_progress"

        /**
         * Crée le canal de notification "Progression des trajets".
         * À appeler une seule fois au démarrage de l'application.
         */
        fun createNotificationChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_JOURNEY,
                "Progression des trajets",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Jalons débloqués et trajets terminés"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
