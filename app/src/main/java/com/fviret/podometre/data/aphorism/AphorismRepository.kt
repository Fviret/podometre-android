package com.fviret.podometre.data.aphorism

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
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
 * Charge les aphorismes depuis [assets/aphorisms_humor_400.json] et sélectionne
 * l'aphorisme du jour de façon déterministe via le quantième de l'année.
 * La sélection est stable sur toute la journée : changer de session en cours
 * de journée ne change pas l'aphorisme affiché.
 *
 * Utilise [kotlinx.serialization] avec [ignoreUnknownKeys] = true pour ignorer
 * silencieusement les champs legacy (tone, year, source) éventuellement présents.
 */
@Singleton
class AphorismRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val json = Json { ignoreUnknownKeys = true }

    private val aphorisms: List<Aphorism> by lazy { loadFromAssets() }

    /** Retourne l'aphorisme du jour (index = quantième % taille du recueil). */
    fun todayAphorism(): Aphorism {
        val list = aphorisms.ifEmpty { return fallback }
        val index = (LocalDate.now().dayOfYear - 1) % list.size
        return list[index]
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
