package com.fviret.podometre

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application principale — point d'entrée Hilt.
 * Initialise aussi WorkManager avec le HiltWorkerFactory pour l'injection dans les Workers.
 */
@HiltAndroidApp
class PodoMetreApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
