package com.fviret.podometre.fakes

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Implémentation en mémoire de [DataStore] pour les tests d'intégration UI.
 * Évite d'écrire sur le disque de l'appareil de test — état réinitialisé à chaque instance.
 */
class FakeDataStore<T>(initial: T) : DataStore<T> {

    private val state = MutableStateFlow(initial)
    private val mutex = Mutex()

    override val data: Flow<T> = state

    override suspend fun updateData(transform: suspend (t: T) -> T): T = mutex.withLock {
        val updated = transform(state.value)
        state.value = updated
        updated
    }
}
