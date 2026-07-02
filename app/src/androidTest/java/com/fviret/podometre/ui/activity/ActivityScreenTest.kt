package com.fviret.podometre.ui.activity

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fviret.podometre.R
import com.fviret.podometre.fakes.TestFactories
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

/**
 * Test d'intégration : écran Activité avec données mock injectées (mode émulateur —
 * [com.fviret.podometre.util.isEmulator] est vrai sur l'appareil de test, ce qui active
 * directement le chemin de données mock déjà intégré à [ActivityViewModel], sans avoir
 * besoin de fakes supplémentaires pour Health Connect / météo).
 * Vérifie l'affichage de l'anneau et la navigation vers le jour précédent.
 *
 * L'anneau ([StepRing]) fusionne son sous-arbre sémantique en un seul nœud
 * (`clearAndSetSemantics`, cf. KAN-44) : le compteur de pas et le libellé "pas" ne sont
 * donc pas des nœuds texte individuellement interrogeables — on vérifie la description
 * d'accessibilité fusionnée, qui est aussi ce que TalkBack annoncerait.
 */
@RunWith(AndroidJUnit4::class)
class ActivityScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: ActivityViewModel

    /** Reproduit le format de `activity_ring_accessibility_label` (contentDescription fusionnée de l'anneau). */
    private fun ringContentDescription(steps: Long, goal: Int): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val percent = (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f).let { (it * 100).roundToInt() }
        return context.getString(R.string.activity_ring_accessibility_label, steps, goal, percent)
    }

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        viewModel = ActivityViewModel(
            healthConnectRepository = TestFactories.healthConnectRepository(context),
            weatherRepository = TestFactories.weatherRepository(),
            userPreferencesRepository = TestFactories.userPreferencesRepository(),
            context = context,
        )
        composeTestRule.setContent {
            ActivityScreen(viewModel = viewModel)
        }
    }

    @Test
    fun anneauAffiche_avecDonneesMockEmulateur() {
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Aujourd'hui").assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(ringContentDescription(steps = 7_430L, goal = 10_000))
            .assertIsDisplayed()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeTestRule
            .onNodeWithText(context.getString(R.string.activity_ring_goal_label, 10_000))
            .assertIsDisplayed()
    }

    @Test
    fun navigationJourPrecedent_changeLeLabelEtLesPas() {
        composeTestRule.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.activity_chevron_prev_desc))
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(-1, viewModel.uiState.value.selectedDayOffset)
        composeTestRule.onNodeWithText("Hier").assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(ringContentDescription(steps = 6_200L, goal = 10_000))
            .assertIsDisplayed()
    }

    @Test
    fun navigationJourPrecedent_puisJourSuivant_revientAAujourdHui() {
        composeTestRule.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.activity_chevron_prev_desc))
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.activity_chevron_next_desc))
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(0, viewModel.uiState.value.selectedDayOffset)
        composeTestRule.onNodeWithText("Aujourd'hui").assertIsDisplayed()
    }
}
