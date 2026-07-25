package com.fviret.podometre.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fviret.podometre.data.aphorism.AphorismRepository
import com.fviret.podometre.fakes.TestFactories
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests d'intégration de [SettingsScreen].
 * Crée un [SettingsViewModel] manuellement avec des repositories fake (mode émulateur),
 * sans Hilt — même pattern que [com.fviret.podometre.ui.activity.ActivityScreenTest].
 *
 * Ces tests vérifient les éléments statiques (toujours visibles, indépendants des données
 * Health Connect) : titre, sections, toggles, couleur de l'anneau par défaut.
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        viewModel = SettingsViewModel(
            userPreferencesRepository = TestFactories.userPreferencesRepository(),
            healthConnectRepository = TestFactories.healthConnectRepository(context),
            journeyProgressRepository = TestFactories.journeyProgressRepository(context),
            aphorismRepository = AphorismRepository(
                context = context,
                preferencesRepository = TestFactories.userPreferencesRepository(),
            ),
        )
        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel)
        }
    }

    /** Le titre "Paramètres" est affiché en haut de l'écran. */
    @Test
    fun titrePrincipale_estAffiche() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Paramètres").assertIsDisplayed()
    }

    /**
     * L'objectif de pas par défaut (10 000) est affiché dans la ligne StepGoalRow.
     * SectionHeader affiche "ACTIVITÉ" (uppercase du titre "Activité").
     */
    @Test
    fun sectionActivite_etObjectifDefaut_sontAffiches() {
        composeTestRule.waitForIdle()
        // SectionHeader appelle title.uppercase() → "ACTIVITÉ"
        composeTestRule.onNodeWithText("ACTIVITÉ").assertIsDisplayed()
        // StepGoalRow affiche "${currentGoal.formatSteps()} pas" → "10 000 pas"
        composeTestRule.onNodeWithText("10 000 pas").assertIsDisplayed()
    }

    /**
     * La section "Apparence" est visible et le nom de la couleur par défaut
     * ("Forêt" pour l'id "green") est affiché dans RingColorRow.
     */
    @Test
    fun sectionApparence_etCouleurDefaut_sontAffichees() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("APPARENCE").assertIsDisplayed()
        // Couleur par défaut = "green" → nom affiché = "Forêt"
        composeTestRule.onNodeWithText("Forêt").assertIsDisplayed()
    }

    /**
     * Au moins un toggle de module (Switch) est visible.
     * Le label "Météo & prévisions" est affiché dans ModuleToggleCard.
     */
    @Test
    fun toggleMeteo_estVisible() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Météo & prévisions").assertIsDisplayed()
    }
}
