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
 * de produire une valeur négative (la journée du reboot sera sous-comptée, comportement
 * accepté car sans autre source fiable pour reconstituer le delta perdu).
 */
internal fun computeDailyDelta(previousRaw: Long, currentRaw: Long): Long =
    (currentRaw - previousRaw).coerceAtLeast(0L)

/** État persisté : historique pas/jour + pointeur pour calculer le prochain delta. */
@Serializable
internal data class SensorStepHistoryState(
    /** Map date ISO (yyyy-MM-dd) → nombre de pas ce jour-là. */
    val stepsByDay: Map<String, Long> = emptyMap(),
    /** Date ISO du dernier relevé capteur, null si jamais capturé. */
    val lastCaptureDate: String? = null,
    /** Valeur cumulative brute du capteur au dernier relevé. */
    val lastRawCounterValue: Long = 0L,
)

/**
 * Historique local de pas basé sur le capteur `TYPE_STEP_COUNTER`, utilisé en fallback
 * quand Health Connect ne retourne aucune donnée exploitable (cas fréquent sur les OEM
 * qui n'écrivent pas dans Health Connect — Honor, Huawei, Xiaomi…).
 *
 * Persisté dans un fichier JSON dédié (comme [com.fviret.podometre.data.journey.JourneyProgressRepository])
 * — il ne s'agit pas de données Health Connect mais de données capteur brutes propres à l'app,
 * donc la règle CLAUDE.md "ne jamais stocker les données HK localement" ne s'applique pas ici.
 *
 * [captureDailySnapshot] doit être appelé une fois par jour (voir [com.fviret.podometre.worker.SyncSensorStepHistoryWorker])
 * avec la valeur brute courante du capteur : le delta avec le relevé précédent est attribué
 * au jour du relevé précédent (jour qui vient de s'écouler).
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
        mutex.withLock {
            cached?.let { return@withLock it }
            val loaded = runCatching {
                if (file.exists()) json.decodeFromString<SensorStepHistoryState>(file.readText())
                else SensorStepHistoryState()
            }.onFailure { Log.w(TAG, "Échec du chargement de $FILE_NAME, historique réinitialisé", it) }
                .getOrDefault(SensorStepHistoryState())
            cached = loaded
            loaded
        }
    }

    /**
     * Enregistre un relevé quotidien du capteur.
     * Si un relevé précédent existe, le delta ([computeDailyDelta]) est attribué à
     * [SensorStepHistoryState.lastCaptureDate] (le jour qui vient de s'écouler).
     * Sans relevé précédent (premier appel), initialise seulement le pointeur — aucun
     * historique n'est encore calculable.
     */
    suspend fun captureDailySnapshot(rawCounterValue: Long, today: LocalDate = LocalDate.now()) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val state = cached ?: runCatching {
                    if (file.exists()) json.decodeFromString<SensorStepHistoryState>(file.readText())
                    else SensorStepHistoryState()
                }.getOrDefault(SensorStepHistoryState())

                val previousDate = state.lastCaptureDate
                val updatedStepsByDay = if (previousDate != null) {
                    val delta = computeDailyDelta(state.lastRawCounterValue, rawCounterValue)
                    state.stepsByDay + (previousDate to delta)
                } else {
                    state.stepsByDay
                }

                val newState = state.copy(
                    stepsByDay = updatedStepsByDay,
                    lastCaptureDate = today.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    lastRawCounterValue = rawCounterValue,
                )
                persist(newState)
                cached = newState
            }
        }
    }

    /**
     * Retourne les pas par jour pour la plage [from]–[to] (bornes incluses).
     * Jours absents de l'historique = jamais capturés, absents de la map résultante.
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

    /** Retourne le total de pas capturés sur la plage [from]–[to] (bornes incluses). */
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
