package com.fviret.podometre.data.journey

import android.content.Context
import com.fviret.podometre.domain.JourneyData
import com.fviret.podometre.domain.model.Journey
import com.fviret.podometre.domain.model.JourneyProgress
import com.fviret.podometre.domain.model.Milestone
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    /** Émis quand un trajet est complété à 100% pour la première fois. Valeur : journeyId. */
    private val _journeyCompleted = MutableSharedFlow<String>()
    val journeyCompleted: SharedFlow<String> = _journeyCompleted.asSharedFlow()

    /**
     * Émis quand un jalon est nouvellement débloqué.
     * Valeur : Pair(journeyId, milestoneId).
     */
    private val _milestoneUnlocked = MutableSharedFlow<Pair<String, String>>()
    val milestoneUnlocked: SharedFlow<Pair<String, String>> = _milestoneUnlocked.asSharedFlow()

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
     * Démarre un trajet si pas encore démarré.
     * Crée une [JourneyProgress] initiale avec la date de départ à maintenant.
     */
    suspend fun startJourney(journeyId: String) {
        if (getProgress(journeyId) != null) return
        saveProgress(JourneyProgress(journeyId = journeyId))
    }

    /**
     * Met à jour la progression d'un trajet depuis une distance [newKm] lue depuis Health Connect.
     * Requête idempotente : si [newKm] ≤ ancienne distance, rien ne se passe.
     * Détecte les jalons nouvellement débloqués, émet [milestoneUnlocked] et [journeyCompleted].
     * Retourne un [JourneySyncResult] pour que l'appelant puisse envoyer des notifications.
     */
    suspend fun syncJourney(journey: Journey, newKm: Double): JourneySyncResult {
        val journeyId = journey.id.toString()
        val progress = getProgress(journeyId)
            ?: return JourneySyncResult(emptyList(), false)
        if (newKm <= progress.totalKm) return JourneySyncResult(emptyList(), false)

        val previousUnlocked = progress.unlockedMilestoneIds
        val nowUnlocked = journey.milestones
            .filter { it.km <= newKm }
            .map { it.id.toString() }
            .toSet()
        val newlyUnlockedIds = nowUnlocked - previousUnlocked

        saveProgress(
            progress.copy(
                totalKm = newKm,
                unlockedMilestoneIds = previousUnlocked + nowUnlocked,
                lastUpdatedMs = System.currentTimeMillis(),
            )
        )

        newlyUnlockedIds.forEach { milestoneId ->
            _milestoneUnlocked.emit(journeyId to milestoneId)
        }

        val wasAlreadyComplete = journey.milestones.all { it.id.toString() in previousUnlocked }
        val isNowComplete = newKm >= journey.totalKm
        val isNewlyCompleted = isNowComplete && !wasAlreadyComplete
        if (isNewlyCompleted) {
            _journeyCompleted.emit(journeyId)
        }

        val newlyUnlockedMilestones = journey.milestones
            .filter { it.id.toString() in newlyUnlockedIds }
            .sortedBy { it.km }

        return JourneySyncResult(newlyUnlockedMilestones, isNewlyCompleted)
    }

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
     * Supprime la progression d'un trajet (réinitialisation ou abandon).
     */
    suspend fun deleteProgress(journeyId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val updated = _progressMap.value - journeyId
            persist(updated)
            _progressMap.value = updated
        }
    }

    /**
     * Injecte des données mock pour l'émulateur :
     * - GR20 à 55% de progression (~99 km)
     * - Berges de la Seine terminées (5 km)
     */
    suspend fun injectEmulatorMock() {
        val gr20 = JourneyData.all.firstOrNull { it.name.contains("GR20") } ?: return
        val berges = JourneyData.all.firstOrNull { it.name.contains("Berges") } ?: return

        val gr20Km = gr20.totalKm * 0.55
        val gr20Progress = JourneyProgress(
            journeyId = gr20.id.toString(),
            totalKm = gr20Km,
            unlockedMilestoneIds = gr20.milestones
                .filter { it.km <= gr20Km }
                .map { it.id.toString() }
                .toSet(),
        )
        val bergesProgress = JourneyProgress(
            journeyId = berges.id.toString(),
            totalKm = berges.totalKm,
            unlockedMilestoneIds = berges.milestones
                .map { it.id.toString() }
                .toSet(),
        )
        saveProgress(gr20Progress)
        saveProgress(bergesProgress)
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

/**
 * Résultat d'une synchronisation de trajet.
 * Permet à l'appelant ([SyncJourneyWorker]) d'envoyer les notifications appropriées.
 */
data class JourneySyncResult(
    val newlyUnlockedMilestones: List<Milestone>,
    val isNewlyCompleted: Boolean,
)
