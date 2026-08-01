package com.fviret.podometre.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fviret.podometre.R
import com.fviret.podometre.data.aphorism.AphorismRepository
import com.fviret.podometre.fakes.TestFactories
import com.fviret.podometre.util.formatSteps
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
 *
 * Les strings sont résolues via context.getString(R.string.xxx) pour rester
 * indépendants de la locale de l'émulateur CI.
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: SettingsViewModel
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
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

    /** Le titre de l'écran est affiché en haut. */
    @Test
    fun titrePrincipale_estAffiche() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(context.getString(R.string.settings_title)).assertIsDisplayed()
    }

    /**
     * L'objectif de pas par défaut (10 000) est affiché dans la ligne StepGoalRow.
     * SectionHeader affiche le titre de la section Activité en majuscules.
     */
    @Test
    fun sectionActivite_etObjectifDefaut_sontAffiches() {
        composeTestRule.waitForIdle()
        // SectionHeader appelle title.uppercase() sur la valeur de la string resource
        composeTestRule.onNodeWithText(context.getString(R.string.settings_section_activity).uppercase()).assertIsDisplayed()
        composeTestRule.onNodeWithText(10_000.formatSteps()).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.settings_step_unit)).assertIsDisplayed()
    }

    /**
     * La section "Apparence" est visible et le nom de la couleur par défaut est affiché
     * dans RingColorRow (couleur "green" → R.string.ring_color_green).
     */
    @Test
    fun sectionApparence_etCouleurDefaut_sontAffichees() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(context.getString(R.string.settings_section_appearance).uppercase()).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.ring_color_green)).assertIsDisplayed()
    }

    /**
     * Au moins un toggle de module (Switch) est visible.
     * Le label du toggle météo est affiché dans ModuleToggleCard.
     */
    @Test
    fun toggleMeteo_estVisible() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(context.getString(R.string.settings_toggle_weather)).assertIsDisplayed()
    }
}
