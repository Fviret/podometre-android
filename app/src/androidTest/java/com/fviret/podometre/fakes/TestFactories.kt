package com.fviret.podometre.fakes

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.fviret.podometre.data.health.HealthConnectRepository
import com.fviret.podometre.data.health.HealthConnectRepositoryImpl
import com.fviret.podometre.data.health.SensorStepHistoryRepository
import com.fviret.podometre.data.journey.JourneyProgressRepository
import com.fviret.podometre.data.preferences.UserPreferencesRepository
import com.fviret.podometre.data.weather.WeatherRepository
import okhttp3.OkHttpClient

/**
 * Fabriques de repositories réels branchés sur des fakes, pour les tests d'intégration UI.
 * Les écrans testés tournent sur l'émulateur : [com.fviret.podometre.util.isEmulator] est vrai,
 * donc les ViewModels court-circuitent la plupart des appels vers des données mock internes
 * avant de solliciter ces repositories — les fakes ne servent qu'à satisfaire la construction.
 */
object TestFactories {

    /** [UserPreferencesRepository] réel adossé à un [FakeDataStore] en mémoire, vide par défaut. */
    fun userPreferencesRepository(
        initial: Preferences = mutablePreferencesOf()
    ): UserPreferencesRepository = UserPreferencesRepository(FakeDataStore(initial))

    /** [HealthConnectRepository] réel adossé à un [FakeHealthConnectClient] (non sollicité en mode émulateur). */
    fun healthConnectRepository(context: Context): HealthConnectRepository =
        HealthConnectRepositoryImpl(dagger.Lazy { FakeHealthConnectClient() }, context)

    /** [WeatherRepository] réel — jamais appelé en mode émulateur, un vrai OkHttpClient suffit. */
    fun weatherRepository(context: Context): WeatherRepository = WeatherRepository(context, OkHttpClient())

    /**
     * [SensorStepHistoryRepository] réel adossé au [Context] d'instrumentation — jamais sollicité
     * en mode émulateur (capteur non fiable, court-circuité par [com.fviret.podometre.util.isEmulator]).
     * Écrit dans le répertoire de fichiers réel de l'app de test — le fichier
     * `sensor_step_history.json` doit être nettoyé entre les tests si besoin (voir `@After`).
     */
    fun sensorStepHistoryRepository(context: Context): SensorStepHistoryRepository =
        SensorStepHistoryRepository(context)

    /**
     * [JourneyProgressRepository] réel adossé au [Context] d'instrumentation.
     * Écrit dans le répertoire de fichiers réel de l'app de test — le fichier
     * `journey_progress.json` doit être nettoyé entre les tests (voir `@After`).
     */
    fun journeyProgressRepository(context: Context): JourneyProgressRepository =
        JourneyProgressRepository(context)
}
