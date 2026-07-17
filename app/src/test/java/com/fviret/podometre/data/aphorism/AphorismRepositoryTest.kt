package com.fviret.podometre.data.aphorism

import android.content.Context
import android.content.res.AssetManager
import com.fviret.podometre.data.preferences.UserPreferences
import com.fviret.podometre.data.preferences.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Tests unitaires de [AphorismRepository], [parseAphorismsJson] et [selectAphorismForDay].
 *
 * Stratégie :
 * - [parseAphorismsJson] et [selectAphorismForDay] sont des fonctions pures testées directement.
 * - [AphorismRepository] est instancié avec un [Context] mocké (assets) et un
 *   [UserPreferencesRepository] mocké pour éviter toute dépendance Android.
 * - [todayProvider] est surchargé avec une date fixe pour rendre les tests déterministes.
 */
class AphorismRepositoryTest {

    // ────────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────────

    /** Construit un [AphorismRepository] avec le JSON donné comme source d'assets. */
    private fun buildRepo(
        json: String = SAMPLE_JSON,
        prefs: UserPreferences = UserPreferences(),
        date: LocalDate = FIXED_DATE,
    ): AphorismRepository {
        val assetManager = mockk<AssetManager>()
        every { assetManager.open("aphorisms_humor_400.json") } returns json.byteInputStream()
        val context = mockk<Context>()
        every { context.assets } returns assetManager

        val preferencesRepository = mockk<UserPreferencesRepository>()
        every { preferencesRepository.userPreferences } returns flowOf(prefs)
        coEvery { preferencesRepository.setLastAphorismDate(any()) } just Runs

        return AphorismRepository(context, preferencesRepository).also {
            it.todayProvider = { date }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // parseAphorismsJson — fonctions pures
    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun `parseAphorismsJson decode les champs requis`() {
        val result = parseAphorismsJson(SAMPLE_JSON)
        assertEquals(3, result.size)
        assertEquals(1, result[0].id)
        assertEquals("Premier aphorisme.", result[0].text)
        assertEquals("Auteur Un", result[0].author)
        assertEquals("philosophie", result[0].category)
    }

    @Test
    fun `parseAphorismsJson tolere les champs inconnus tone year source`() {
        val json = """[{"id":1,"text":"Texte.","author":"Auteur","tone":"warm","year":1890,"source":"Livre"}]"""
        val result = parseAphorismsJson(json)
        assertEquals(1, result.size)
        assertEquals("Texte.", result[0].text)
    }

    @Test
    fun `parseAphorismsJson retourne liste vide pour JSON vide`() {
        val result = parseAphorismsJson("[]")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseAphorismsJson retourne liste vide pour JSON invalide`() {
        val result = parseAphorismsJson("not json at all")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseAphorismsJson utilise la valeur par defaut pour category absente`() {
        val json = """[{"id":1,"text":"Texte.","author":"Auteur"}]"""
        val result = parseAphorismsJson(json)
        assertEquals("", result[0].category)
    }

    // ────────────────────────────────────────────────────────────────────────
    // selectAphorismForDay — fonctions pures
    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun `selectAphorismForDay retourne le premier element le jour 1`() {
        val list = aphorisms(5)
        assertEquals(list[0], selectAphorismForDay(list, dayOfYear = 1))
    }

    @Test
    fun `selectAphorismForDay retourne le dernier element le jour N`() {
        val list = aphorisms(5)
        assertEquals(list[4], selectAphorismForDay(list, dayOfYear = 5))
    }

    @Test
    fun `selectAphorismForDay wrap-around le jour N+1`() {
        val list = aphorisms(5)
        assertEquals(list[0], selectAphorismForDay(list, dayOfYear = 6))
    }

    @Test
    fun `selectAphorismForDay est stable pour le meme quantieme`() {
        val list = aphorisms(400)
        val first = selectAphorismForDay(list, dayOfYear = 194)
        val second = selectAphorismForDay(list, dayOfYear = 194)
        assertEquals(first, second)
    }

    @Test
    fun `selectAphorismForDay retourne null pour liste vide`() {
        assertNull(selectAphorismForDay(emptyList(), dayOfYear = 1))
    }

    // ────────────────────────────────────────────────────────────────────────
    // AphorismRepository.todayAphorism
    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun `todayAphorism retourne le fallback quand les assets sont vides`() {
        val repo = buildRepo(json = "[]")
        val aphorism = repo.todayAphorism()
        assertNotNull(aphorism)
        // Le fallback a toujours id=0
        assertEquals(0, aphorism.id)
    }

    @Test
    fun `todayAphorism retourne un aphorisme valide avec un recueil non vide`() {
        val repo = buildRepo()
        val aphorism = repo.todayAphorism()
        assertTrue(aphorism.id > 0)
    }

    // ────────────────────────────────────────────────────────────────────────
    // AphorismRepository.shouldShowPopup
    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun `shouldShowPopup retourne true par defaut avec feature activee et jamais affichee`() = runTest {
        val repo = buildRepo(prefs = UserPreferences(aphorismEnabled = true, lastAphorismDate = ""))
        assertTrue(repo.shouldShowPopup())
    }

    @Test
    fun `shouldShowPopup retourne false quand feature desactivee`() = runTest {
        val repo = buildRepo(prefs = UserPreferences(aphorismEnabled = false))
        assertFalse(repo.shouldShowPopup())
    }

    @Test
    fun `shouldShowPopup retourne false quand deja affichee aujourd hui`() = runTest {
        val repo = buildRepo(
            prefs = UserPreferences(lastAphorismDate = FIXED_DATE.toString()),
            date = FIXED_DATE,
        )
        assertFalse(repo.shouldShowPopup())
    }

    @Test
    fun `shouldShowPopup retourne true quand affichee hier`() = runTest {
        val yesterday = FIXED_DATE.minusDays(1).toString()
        val repo = buildRepo(
            prefs = UserPreferences(lastAphorismDate = yesterday),
            date = FIXED_DATE,
        )
        assertTrue(repo.shouldShowPopup())
    }

    @Test
    fun `shouldShowPopup retourne false quand recueil vide`() = runTest {
        val repo = buildRepo(json = "[]", prefs = UserPreferences(aphorismEnabled = true))
        assertFalse(repo.shouldShowPopup())
    }

    // ────────────────────────────────────────────────────────────────────────
    // AphorismRepository.markDisplayed
    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun `markDisplayed appelle setLastAphorismDate avec la date du jour`() = runTest {
        val assetManager = mockk<AssetManager>()
        every { assetManager.open("aphorisms_humor_400.json") } returns SAMPLE_JSON.byteInputStream()
        val context = mockk<Context>()
        every { context.assets } returns assetManager

        val preferencesRepository = mockk<UserPreferencesRepository>()
        every { preferencesRepository.userPreferences } returns flowOf(UserPreferences())
        coEvery { preferencesRepository.setLastAphorismDate(any()) } just Runs

        val repo = AphorismRepository(context, preferencesRepository).also {
            it.todayProvider = { FIXED_DATE }
        }
        repo.markDisplayed()

        coVerify { preferencesRepository.setLastAphorismDate(FIXED_DATE.toString()) }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Fixtures
    // ────────────────────────────────────────────────────────────────────────

    companion object {
        private val FIXED_DATE = LocalDate.of(2026, 7, 13)

        private val SAMPLE_JSON = """
            [
              {"id":1,"text":"Premier aphorisme.","author":"Auteur Un","category":"philosophie"},
              {"id":2,"text":"Deuxième aphorisme.","author":"Auteur Deux","category":"humour"},
              {"id":3,"text":"Troisième aphorisme.","author":"Auteur Trois","category":""}
            ]
        """.trimIndent()

        /** Génère une liste de [n] aphorismes de test numérotés de 1 à n. */
        private fun aphorisms(n: Int) = (1..n).map { i ->
            Aphorism(id = i, text = "Texte $i", author = "Auteur $i", category = "")
        }
    }
}
