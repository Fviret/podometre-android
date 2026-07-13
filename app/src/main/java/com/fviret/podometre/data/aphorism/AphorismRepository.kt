package com.fviret.podometre.data.aphorism

import android.content.Context
import android.util.Log
import com.fviret.podometre.data.preferences.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Modèle d'un aphorisme issu du recueil CC0.
 * [category] est optionnel pour la compatibilité avec les anciens formats JSON
 * qui ne contiennent pas ce champ.
 */
@Serializable
data class Aphorism(
    val id: Int,
    val text: String,
    val author: String,
    val category: String = "",
)

/**
 * Charge les aphorismes depuis [assets/aphorisms_humor_400.json], expose l'aphorisme
 * du jour et gère la garde "afficher une seule fois par jour" via DataStore.
 *
 * Utilise [kotlinx.serialization] avec [ignoreUnknownKeys] = true pour ignorer
 * silencieusement les champs legacy (tone, year, source) éventuellement présents.
 *
 * @param context Contexte applicatif pour accéder aux assets.
 * @param preferencesRepository Accès DataStore pour la garde 1×/jour et l'activation.
 */
@Singleton
class AphorismRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: UserPreferencesRepository,
) {

    private val json = Json { ignoreUnknownKeys = true }

    private val aphorisms: List<Aphorism> by lazy { loadFromAssets() }

    /** Retourne l'aphorisme du jour (index = (quantième - 1) % taille du recueil). */
    fun todayAphorism(): Aphorism {
        val list = aphorisms.ifEmpty { return fallback }
        val index = (LocalDate.now().dayOfYear - 1) % list.size
        return list[index]
    }

    /**
     * Retourne true si la popup "Pensée du jour" doit être affichée.
     * Conditions : feature activée, recueil non vide, et popup non encore montrée aujourd'hui.
     */
    suspend fun shouldShowPopup(): Boolean {
        if (aphorisms.isEmpty()) return false
        val prefs = preferencesRepository.userPreferences.first()
        if (!prefs.aphorismEnabled) return false
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return prefs.lastAphorismDate != today
    }

    /**
     * Marque la popup comme affichée aujourd'hui dans DataStore.
     * Empêche un second affichage lors des ouvertures suivantes de la journée.
     */
    suspend fun markDisplayed() {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        preferencesRepository.setLastAphorismDate(today)
    }

    private fun loadFromAssets(): List<Aphorism> = runCatching {
        val jsonText = context.assets.open("aphorisms_humor_400.json").bufferedReader().readText()
        json.decodeFromString<List<Aphorism>>(jsonText)
    }.getOrElse {
        Log.w("AphorismRepository", "Échec du chargement des aphorismes", it)
        emptyList()
    }

    private val fallback = Aphorism(
        id = 0,
        text = "Chaque matin, nous renaissons. Ce que nous faisons aujourd'hui compte le plus.",
        author = "Bouddha",
        category = "philosophie",
    )
}
