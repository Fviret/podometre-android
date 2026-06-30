package com.fviret.podometre.data.journey

import android.content.Context
import com.fviret.podometre.domain.model.JourneyProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistance de la progression des trajets dans un fichier JSON local.
 * Utilise un fichier dédié car la map peut être volumineuse pour DataStore.
 *
 * Clé de la map : journeyId (String UUID)
 * Valeur : [JourneyProgress]
 *
 * Équivalent iOS : JourneyProgressService (fichier JSON dans Documents/)
 */
@Singleton
class JourneyProgressRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val file: File get() = File(context.filesDir, FILE_NAME)
    private val mutex = Mutex()

    private val _progressMap = MutableStateFlow<Map<String, JourneyProgress>>(emptyMap())

    /** Map journeyId → progression, mise à jour à chaque modification. */
    val progressMap: StateFlow<Map<String, JourneyProgress>> = _progressMap.asStateFlow()

    // ── Initialisation ───────────────────────────────────────────────────────

    /**
     * Charge la map depuis le fichier JSON au démarrage.
     * À appeler depuis le scope de l'Application ou du ViewModel parent.
     */
    suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                if (file.exists()) {
                    val map: Map<String, JourneyProgress> = json.decodeFromString(file.readText())
                    _progressMap.value = map
                }
            }
        }
    }

    // ── Lecture ─────────────────────────────────────────────────────────────

    /** Retourne la progression d'un trajet, ou null s'il n'a jamais été démarré. */
    fun getProgress(journeyId: String): JourneyProgress? = _progressMap.value[journeyId]

    // ── Écriture ─────────────────────────────────────────────────────────────

    /**
     * Met à jour (ou crée) la progression d'un trajet et persiste la map.
     * Thread-safe grâce au [Mutex].
     */
    suspend fun saveProgress(progress: JourneyProgress) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val updated = _progressMap.value + (progress.journeyId to progress)
            persist(updated)
            _progressMap.value = updated
        }
    }

    /**
     * Supprime la progression d'un trajet (réinitialisation).
     */
    suspend fun deleteProgress(journeyId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val updated = _progressMap.value - journeyId
            persist(updated)
            _progressMap.value = updated
        }
    }

    // ── Persistance ─────────────────────────────────────────────────────────

    /**
     * Écrit la map complète dans [FILE_NAME] de manière atomique (fichier temp + rename).
     */
    @Throws(IOException::class)
    private fun persist(map: Map<String, JourneyProgress>) {
        val tmp = File(context.filesDir, "$FILE_NAME.tmp")
        tmp.writeText(json.encodeToString(map))
        tmp.renameTo(file)
    }

    companion object {
        private const val FILE_NAME = "journey_progress.json"
    }
}
