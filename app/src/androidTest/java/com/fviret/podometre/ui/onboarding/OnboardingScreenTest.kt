package com.fviret.podometre.ui.onboarding

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fviret.podometre.R
import com.fviret.podometre.fakes.TestFactories
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test d'intégration : flux d'onboarding complet — 4 slides → objectif sélectionné → [onComplete].
 * Le [OnboardingViewModel] réel est utilisé, adossé à un [com.fviret.podometre.fakes.FakeDataStore]
 * en mémoire (pas d'écriture disque) et à un [com.fviret.podometre.fakes.FakeHealthConnectClient].
 * La slide 3 (permissions) est franchie en simulant directement le retour du lanceur système
 * plutôt qu'en pilotant le vrai dialogue Health Connect — dont la disponibilité dépend de
 * l'appareil de test et sortirait du scope de ce test (logique app, pas dialogue OS).
 */
@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: OnboardingViewModel
    private var completedCount = 0

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        viewModel = OnboardingViewModel(
            userPreferencesRepository = TestFactories.userPreferencesRepository(),
            healthConnectRepository = TestFactories.healthConnectRepository(context),
        )
        completedCount = 0
    }

    private fun setContent() {
        composeTestRule.setContent {
            OnboardingScreen(onComplete = { completedCount++ }, viewModel = viewModel)
        }
    }

    @Test
    fun navigationComplete4Slides_objectifSelectionne_appLancee() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        setContent()

        // Slide 1 → 2 : le titre de la slide 1 est visible, "Suivant" avance.
        composeTestRule.onNodeWithText(context.getString(R.string.onboarding_next)).performClick()
        assertEquals(1, viewModel.uiState.value.currentPage)

        // Slide 2 → 3 (permissions)
        composeTestRule.onNodeWithText(context.getString(R.string.onboarding_next)).performClick()
        assertEquals(OnboardingViewModel.PERMISSIONS_PAGE_INDEX, viewModel.uiState.value.currentPage)

        // Slide 3 (permissions) : simule le retour du lanceur système Health Connect,
        // sans dépendre de la disponibilité réelle de Health Connect sur l'appareil de test.
        viewModel.onHealthPermissionsResult(emptySet())
        // waitUntil : la page 3 (slide 4) doit être rendue avant de cliquer sur l'objectif.
        val goalText = context.getString(R.string.onboarding_slide4_goal_label, 15_000)
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText(goalText).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(3, viewModel.uiState.value.currentPage)

        // Slide 4 : sélection d'un objectif différent du défaut (8 000)
        composeTestRule.onNodeWithText(goalText).performClick()
        assertEquals(15_000, viewModel.uiState.value.selectedGoal)

        // Lancement de l'app
        composeTestRule.onNodeWithText(context.getString(R.string.onboarding_start)).performClick()
        composeTestRule.waitForIdle()

        assertTrue(viewModel.uiState.value.isCompleted)
        assertEquals(1, completedCount)
    }

    @Test
    fun completeOnboarding_persisteObjectifEtFlagOnboarding() {
        val userPreferencesRepository = TestFactories.userPreferencesRepository()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val localViewModel = OnboardingViewModel(
            userPreferencesRepository = userPreferencesRepository,
            healthConnectRepository = TestFactories.healthConnectRepository(context),
        )

        composeTestRule.setContent {
            OnboardingScreen(onComplete = {}, viewModel = localViewModel)
        }

        localViewModel.setGoal(20_000)
        localViewModel.completeOnboarding()
        composeTestRule.waitForIdle()

        val prefs = runBlocking { userPreferencesRepository.userPreferences.first() }
        assertEquals(20_000, prefs.dailyStepGoal)
        assertTrue(prefs.hasCompletedOnboarding)
    }
}
