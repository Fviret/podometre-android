package com.fviret.podometre.di

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import com.fviret.podometre.data.health.EmulatorHealthConnectRepository
import com.fviret.podometre.data.health.HealthConnectRepository
import com.fviret.podometre.data.health.HealthConnectRepositoryImpl
import com.fviret.podometre.util.isEmulator
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Module Hilt fournissant le [HealthConnectClient] et le [HealthConnectRepository].
 * Séparé de [AppModule] pour faciliter le remplacement par un fake en tests.
 */
@Module
@InstallIn(SingletonComponent::class)
object HealthConnectModule {

    /** Fournit le client Health Connect. Requiert Android 9+ (API 28). */
    @Provides
    @Singleton
    fun provideHealthConnectClient(@ApplicationContext context: Context): HealthConnectClient =
        HealthConnectClient.getOrCreate(context)

    /**
     * Point de décision UNIQUE (KAN-160) entre l'implémentation Health Connect réelle et
     * l'implémentation mock utilisée sur émulateur. Résolu une seule fois, à la construction,
     * plutôt que dupliqué dans chaque méthode du repository.
     */
    @Provides
    @Singleton
    fun provideHealthConnectRepository(
        client: Lazy<HealthConnectClient>,
        @ApplicationContext context: Context
    ): HealthConnectRepository {
        val real = HealthConnectRepositoryImpl(client, context)
        return if (isEmulator()) EmulatorHealthConnectRepository(real) else real
    }
}
