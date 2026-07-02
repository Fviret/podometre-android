package com.fviret.podometre.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.fviret.podometre.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service d'envoi de la notification "Objectif atteint !" quotidienne.
 * Envoyée au maximum une fois par jour, respecte le toggle utilisateur.
 * Équivalent iOS : sendGoalReachedNotification() dans StepCountViewModel.swift
 */
@Singleton
class GoalNotificationService @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * Envoie la notification "Objectif atteint ! 🎉" avec le nombre de pas accomplis.
     */
    fun notifyGoalReached(stepCount: Long) {
        val stepsFormatted = "%,d".format(stepCount).replace(',', ' ')
        val notification = NotificationCompat.Builder(context, CHANNEL_GOAL)
            .setSmallIcon(R.drawable.ic_notification_journey)
            .setContentTitle("Objectif atteint ! 🎉")
            .setContentText("Tu as atteint $stepsFormatted pas aujourd'hui.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_GOAL = "daily_goal"
        private const val NOTIFICATION_ID = 1001

        /**
         * Crée le canal de notification "Objectif quotidien".
         * À appeler une seule fois au démarrage de l'application.
         */
        fun createNotificationChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_GOAL,
                "Objectif quotidien",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Félicitations quand l'objectif de pas est atteint"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
