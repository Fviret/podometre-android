package com.fviret.podometre

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.fviret.podometre.worker.SyncJourneyWorker
import com.fviret.podometre.worker.SyncStepsWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application principale — point d'entrée Hilt.
 * Initialise WorkManager avec le HiltWorkerFactory et planifie les workers périodiques.
 */
@HiltAndroidApp
class PodoMetreApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        val workManager = WorkManager.getInstance(this)
        SyncStepsWorker.schedule(workManager)
        SyncJourneyWorker.schedule(workManager)
    }
}
