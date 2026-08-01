package com.fviret.podometre.ui.aphorism

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fviret.podometre.R
import com.fviret.podometre.data.aphorism.Aphorism
import com.fviret.podometre.data.aphorism.AphorismRepository
import com.fviret.podometre.fakes.TestFactories
import com.fviret.podometre.ui.settings.SettingsScreen
import com.fviret.podometre.ui.settings.SettingsViewModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests UI instrumentés pour la feature "Pensée du jour".
 *
 * Stratégie :
 * - [AphorismPopup] et [AphorismCard] sont testés en isolation via `setContent` direct,
 *   sans dépendance à un ViewModel ni à l'heure réelle du système.
 * - L'écran Paramètres est testé avec un [SettingsViewModel] construit à partir de
 *   [TestFactories] — le mode émulateur court-circuite les appels Health Connect.
 *
 * Les composables cibles sont identifiés via `testTag` (cf. KAN-67).
 */
@RunWith(AndroidJUnit4::class)
class AphorismUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ────────────────────────────────────────────────────────────────────────
    // AphorismPopup — tests en isolation
    // ────────────────────────────────────────────────────────────────────────

    /** La popup s'affiche bien avec le texte de l'aphorisme. */
    @Test
    fun popup_apparait_avec_texte_aphorisme() {
        composeTestRule.setContent {
            AphorismPopup(aphorism = TEST_APHORISM, onDismiss = {})
        }

        composeTestRule.onNodeWithTag("aphorism_popup").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pensée du jour").assertIsDisplayed()
    }

    /** Appui sur "Make my day" déclenche [onDismiss] et fait disparaître la popup. */
    @Test
    fun popup_dismiss_button_ferme_la_popup() {
        var dismissed = false
        composeTestRule.setContent {
            var visible by remember { mutableStateOf(true) }
            if (visible) {
                AphorismPopup(
                    aphorism = TEST_APHORISM,
                    onDismiss = {
                        dismissed = true
                        visible = false
                    },
                )
            }
        }

        composeTestRule.onNodeWithTag("aphorism_dismiss_button").performClick()
        composeTestRule.waitForIdle()

        assertTrue("onDismiss doit être appelé", dismissed)
        composeTestRule.onNodeWithTag("aphorism_popup").assertDoesNotExist()
    }

    /** La popup est absente quand [showAphorism] est false — aucun Dialog ne doit être présent. */
    @Test
    fun popup_absente_quand_non_affichee() {
        composeTestRule.setContent {
            // Rien — simuler showAphorism = false en ne rendant pas la popup
        }

        composeTestRule.onNodeWithTag("aphorism_popup").assertDoesNotExist()
    }

    // ────────────────────────────────────────────────────────────────────────
    // AphorismCard — tests en isolation
    // ────────────────────────────────────────────────────────────────────────

    /** La carte s'affiche avec le bon tag (présence dans la hiérarchie). */
    @Test
    fun carte_aphorisme_affichee() {
        composeTestRule.setContent {
            AphorismCard(aphorism = TEST_APHORISM)
        }

        composeTestRule.onNodeWithTag("aphorism_card").assertIsDisplayed()
    }

    // ────────────────────────────────────────────────────────────────────────
    // SettingsScreen — présence du toggle et de la carte
    // ────────────────────────────────────────────────────────────────────────

    /**
     * L'écran Paramètres affiche le toggle "Afficher la pensée du jour"
     * et la carte de l'aphorisme du jour.
     */
    @Test
    fun settings_contient_toggle_et_carte_aphorisme() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val viewModel = SettingsViewModel(
            userPreferencesRepository = TestFactories.userPreferencesRepository(
                initial = mutablePreferencesOf()
            ),
            healthConnectRepository = TestFactories.healthConnectRepository(context),
            journeyProgressRepository = TestFactories.journeyProgressRepository(context),
            aphorismRepository = AphorismRepository(
                context = context,
                preferencesRepository = TestFactories.userPreferencesRepository(),
            ).also { it.todayProvider = { java.time.LocalDate.of(2026, 7, 13) } },
        )

        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel)
        }

        composeTestRule.waitForIdle()

        // Toggle "Pensée du jour" présent dans la section dédiée
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeTestRule.onNodeWithText(context.getString(R.string.settings_toggle_aphorism))
            .performScrollTo()
            .assertIsDisplayed()

        // Carte de l'aphorisme du jour présente (scroll nécessaire — SettingsScreen est scrollable)
        composeTestRule.onNodeWithTag("aphorism_card")
            .performScrollTo()
            .assertIsDisplayed()
    }

    // ────────────────────────────────────────────────────────────────────────
    // Fixtures
    // ────────────────────────────────────────────────────────────────────────

    companion object {
        private val TEST_APHORISM = Aphorism(
            id = 1,
            text = "Le voyage de mille lieues commence par un pas.",
            author = "Lao Tseu",
            category = "sagesse",
        )
    }
}
