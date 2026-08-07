package com.fviret.podometre.data.health

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calcule le nombre de pas écoulés entre deux relevés du capteur `TYPE_STEP_COUNTER`.
 * Le capteur est cumulatif depuis le dernier reboot : si [currentRaw] < [previousRaw],
 * un reboot a eu lieu entre les deux relevés — le delta est alors ramené à 0 plutôt que
 * de produire une valeur négative (la période du reboot sera sous-comptée, comportement
 * accepté car sans autre source fiable pour reconstituer le delta perdu).
 */
internal fun computeDailyDelta(previousRaw: Long, currentRaw: Long): Long =
    (currentRaw - previousRaw).coerceAtLeast(0L)

/**
 * État persisté : historique pas/jour finalisé + pointeur du jour en cours.
 * [dayStartRaw] est la valeur brute du capteur au premier relevé de [currentDay] — c'est
 * la référence servant à calculer les pas du jour en cours à tout moment
 * (`computeDailyDelta(dayStartRaw, lastRawCounterValue ou valeur live)`).
 */
@Serializable
internal data class SensorStepHistoryState(
    /** Map date ISO (yyyy-MM-dd) → nombre de pas ce jour-là (jours finalisés uniquement). */
    val stepsByDay: Map<String, Long> = emptyMap(),
    /** Date ISO du jour en cours de suivi, null si jamais capturé. */
    val currentDay: String? = null,
    /** Valeur brute du capteur au premier relevé de [currentDay] (référence "minuit"). */
    val dayStartRaw: Long = 0L,
    /** Valeur brute du capteur au dernier relevé (worker ou capteur live), pour l'estimation courante. */
    val lastRawCounterValue: Long = 0L,
)

/**
 * Historique local de pas basé sur le capteur `TYPE_STEP_COUNTER` — source **primaire** des pas
 * (aujourd'hui et historique), Health Connect ne servant que de source secondaire pour combler
 * les jours sans relevé capteur (première installation, permission refusée temporairement…).
 *
 * Le capteur est un accès direct au hardware, sans dépendre d'une app tierce (contrairement à
 * Health Connect, vide par défaut sur les OEM — Honor, Huawei, Xiaomi — qui n'y écrivent jamais).
 * Voir KAN-156.
 *
 * Persisté dans un fichier JSON dédié (comme [com.fviret.podometre.data.journey.JourneyProgressRepository])
 * — il ne s'agit pas de données Health Connect mais de données capteur brutes propres à l'app,
 * donc la règle CLAUDE.md "ne jamais stocker les données HK localement" ne s'applique pas ici.
 *
 * [recordSnapshot] doit être appelé régulièrement (worker périodique 15 min, voir
 * [com.fviret.podometre.worker.SyncSensorStepHistoryWorker]) ET par le capteur live en foreground
 * (voir `ActivityViewModel.stepListener`) : chaque relevé rapproche la fraîcheur de l'estimation
 * du jour courant, et détecte le changement de jour calendaire pour finaliser la veille.
 */
@Singleton
class SensorStepHistoryRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val file: File get() = File(context.filesDir, FILE_NAME)
    private val mutex = Mutex()

    private var cached: SensorStepHistoryState? = null

    /** Charge l'état depuis le fichier JSON si pas déjà en mémoire. */
    private suspend fun ensureLoaded(): SensorStepHistoryState = withContext(Dispatchers.IO) {
        mutex.withLock { loadLocked() }
    }

    /** Charge l'état sans re-verrouiller — à appeler uniquement sous [mutex]. */
    private fun loadLocked(): SensorStepHistoryState {
        cached?.let { return it }
        val loaded = runCatching {
            if (file.exists()) json.decodeFromString<SensorStepHistoryState>(file.readText())
            else SensorStepHistoryState()
        }.onFailure { Log.w(TAG, "Échec du chargement de $FILE_NAME, historique réinitialisé", it) }
            .getOrDefault(SensorStepHistoryState())
        cached = loaded
        return loaded
    }

    /**
     * Enregistre un relevé du capteur (worker périodique OU capteur live en foreground).
     * - Premier appel jamais effectué : initialise seulement le pointeur du jour courant.
     * - Même jour calendaire que [SensorStepHistoryState.currentDay] : met à jour
     *   [SensorStepHistoryState.lastRawCounterValue] uniquement (la baseline du jour ne bouge pas).
     * - Nouveau jour calendaire détecté : finalise la veille dans [SensorStepHistoryState.stepsByDay]
     *   via [computeDailyDelta] entre l'ancienne baseline et le dernier relevé connu, puis établit
     *   la nouvelle baseline à partir de [rawCounterValue].
     */
    suspend fun recordSnapshot(rawCounterValue: Long, today: LocalDate = LocalDate.now()) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val state = loadLocked()
                val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

                val newState = when (state.currentDay) {
                    null -> state.copy(
                        currentDay = todayStr,
                        dayStartRaw = rawCounterValue,
                        lastRawCounterValue = rawCounterValue,
                    )
                    todayStr -> state.copy(lastRawCounterValue = rawCounterValue)
                    else -> {
                        val finalizedDelta = computeDailyDelta(state.dayStartRaw, state.lastRawCounterValue)
                        state.copy(
                            stepsByDay = state.stepsByDay + (state.currentDay to finalizedDelta),
                            currentDay = todayStr,
                            dayStartRaw = rawCounterValue,
                            lastRawCounterValue = rawCounterValue,
                        )
                    }
                }
                persist(newState)
                cached = newState
            }
        }
    }

    /**
     * Retourne la baseline (valeur brute capteur au début du jour) pour [today], ou null si
     * aucun relevé n'a encore été fait aujourd'hui — dans ce cas l'appelant doit établir la
     * baseline lui-même via [recordSnapshot] dès qu'il obtient un premier relevé.
     * Utilisé par le capteur live pour calculer les pas du jour sans dépendre de Health Connect.
     */
    suspend fun getTodayBaseline(today: LocalDate = LocalDate.now()): Long? {
        val state = ensureLoaded()
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        return if (state.currentDay == todayStr) state.dayStartRaw else null
    }

    /**
     * Estimation des pas du jour courant à partir du dernier relevé connu (worker ou capteur live) —
     * peut être légèrement obsolète si aucun relevé récent n'a eu lieu (jusqu'à 15 min via le worker).
     * Retourne null si aucun relevé n'a encore été fait pour [today].
     */
    suspend fun readTodayStepsEstimate(today: LocalDate = LocalDate.now()): Long? {
        val state = ensureLoaded()
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        return if (state.currentDay == todayStr) computeDailyDelta(state.dayStartRaw, state.lastRawCounterValue) else null
    }

    /**
     * Retourne les pas par jour finalisés pour la plage [from]–[to] (bornes incluses).
     * Ne contient jamais le jour en cours ([SensorStepHistoryState.currentDay]) — utiliser
     * [readTodayStepsEstimate] pour aujourd'hui. Jours absents = jamais capturés.
     */
    suspend fun readStepsByDay(from: LocalDate, to: LocalDate): Map<LocalDate, Long> {
        val state = ensureLoaded()
        return state.stepsByDay
            .mapNotNull { (dateStr, steps) ->
                runCatching { LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
                    ?.let { it to steps }
            }
            .filter { (date, _) -> !date.isBefore(from) && !date.isAfter(to) }
            .toMap()
    }

    /** Retourne le total de pas finalisés sur la plage [from]–[to] (bornes incluses). */
    suspend fun readSteps(from: LocalDate, to: LocalDate): Long =
        readStepsByDay(from, to).values.sum()

    /**
     * Écrit l'état complet dans [FILE_NAME] de manière atomique (fichier temp + rename).
     */
    @Throws(IOException::class)
    private fun persist(state: SensorStepHistoryState) {
        val tmp = File(context.filesDir, "$FILE_NAME.tmp")
        tmp.writeText(json.encodeToString(state))
        tmp.renameTo(file)
    }

    companion object {
        private const val TAG = "SensorStepHistoryRepository"
        private const val FILE_NAME = "sensor_step_history.json"
    }
}
