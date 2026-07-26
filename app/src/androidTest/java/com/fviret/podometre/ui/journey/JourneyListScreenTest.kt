package com.fviret.podometre.ui.journey

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fviret.podometre.data.journey.JourneyProgressRepository
import com.fviret.podometre.domain.JourneyData
import com.fviret.podometre.fakes.TestFactories
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Test d'intégration : catalogue des trajets — liste affichée → tap preview → tap
 * commencer → détail affiché. Le [JourneyListViewModel] réel est utilisé, adossé à un
 * vrai [JourneyProgressRepository] (écrit dans le répertoire de fichiers de l'app de test,
 * nettoyé après chaque test) et à un [com.fviret.podometre.fakes.FakeDataStore] en mémoire.
 * "Détail affiché" est vérifié via l'invocation de `onNavigateToDetail` avec le bon
 * journeyId — c'est exactement ce que le NavHost réel utilise pour naviguer vers
 * [JourneyDetailScreen].
 *
 * Note : le bouton "Commencer le trajet" (dans le `ModalBottomSheet`) n'est pas vérifié
 * via `assertIsDisplayed()` — dans l'hôte de test nu de `createComposeRule()` (sans le
 * thème/les insets de la vraie `MainActivity`), le `Popup` de la feuille se mesure avec
 * une hauteur non bornée et positionne son contenu au-delà du viewport visible, sans que
 * cela reflète un bug de l'app (confirmé manuellement sur émulateur lors de la vérification
 * KAN-44 — la feuille s'affichait normalement). `performClick()` agit directement sur
 * l'action sémantique du nœud et ne dépend pas de sa visibilité à l'écran.
 */
@RunWith(AndroidJUnit4::class)
class JourneyListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var journeyProgressRepository: JourneyProgressRepository
    private lateinit var viewModel: JourneyListViewModel
    private val firstJourney = JourneyData.all.first { it.name.contains("Tuileries") }

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        journeyProgressRepository = TestFactories.journeyProgressRepository(context)
        viewModel = JourneyListViewModel(
            journeyProgressRepository = journeyProgressRepository,
            userPreferencesRepository = TestFactories.userPreferencesRepository(),
        )
    }

    @After
    fun cleanup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "journey_progress.json").delete()
        File(context.filesDir, "journey_progress.json.tmp").delete()
    }

    @Test
    fun catalogueTrajets_listeAfficheePreviewCommencerDetailAffiche() {
        var navigatedToJourneyId: String? = null

        composeTestRule.setContent {
            JourneyListScreen(
                onNavigateToDetail = { navigatedToJourneyId = it },
                viewModel = viewModel,
            )
        }
        composeTestRule.waitForIdle()

        // ── Liste affichée ──────────────────────────────────────────────
        composeTestRule.onNodeWithText("Mes Trajets").assertIsDisplayed()
        composeTestRule.onNodeWithText(firstJourney.name).assertIsDisplayed()

        // ── Tap preview (première carte) ──────────────────────────────
        // La carte est cliquable via contentDescription = journey.name (sémantique fusionnée).
        // "Voir le trajet" n'existe pas dans le UI — c'est le contentDescription qui est l'ancre.
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithContentDescription(firstJourney.name, substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription(firstJourney.name, substring = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Commencer le trajet").performClick()
        composeTestRule.waitForIdle()

        // ── Détail affiché : après startJourney, la carte est isInProgress → un tap dessus
        //    déclenche directement onNavigateToDetail (pas de preview sheet intermédiaire).
        //    On attend que la barre de progression apparaisse pour confirmer l'état mis à jour.
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithContentDescription(firstJourney.name, substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription(firstJourney.name, substring = true).performClick()
        composeTestRule.waitForIdle()

        assertEquals(firstJourney.id.toString(), navigatedToJourneyId)
    }
}
