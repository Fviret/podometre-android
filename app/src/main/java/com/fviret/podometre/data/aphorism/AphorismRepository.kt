package com.fviret.podometre.data.aphorism

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Modèle d'un aphorisme issu du recueil CC0. */
data class Aphorism(val id: Int, val text: String, val author: String)

/**
 * Charge les 400 aphorismes depuis [assets/aphorisms.json] et sélectionne
 * l'aphorisme du jour de façon déterministe via le quantième de l'année.
 * La sélection est stable sur toute la journée : changer de session en cours
 * de journée ne change pas l'aphorisme affiché.
 */
@Singleton
class AphorismRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val aphorisms: List<Aphorism> by lazy { loadFromAssets() }

    /** Retourne l'aphorisme du jour (index = quantième % taille du recueil). */
    fun todayAphorism(): Aphorism {
        val list = aphorisms.ifEmpty { return fallback }
        val index = (LocalDate.now().dayOfYear - 1) % list.size
        return list[index]
    }

    private fun loadFromAssets(): List<Aphorism> = runCatching {
        val json = context.assets.open("aphorisms.json").bufferedReader().readText()
        val array = JSONArray(json)
        List(array.length()) { i ->
            val obj = array.getJSONObject(i)
            Aphorism(
                id = obj.getInt("id"),
                text = obj.getString("text"),
                author = obj.getString("author"),
            )
        }
    }.getOrElse {
        android.util.Log.w("AphorismRepository", "Échec du chargement des aphorismes", it)
        emptyList()
    }

    private val fallback = Aphorism(
        id = 0,
        text = "Chaque matin, nous renaissons. Ce que nous faisons aujourd'hui compte le plus.",
        author = "Bouddha",
    )
}
