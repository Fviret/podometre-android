package com.fviret.podometre.di

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Module Hilt fournissant le [HealthConnectClient].
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
}
