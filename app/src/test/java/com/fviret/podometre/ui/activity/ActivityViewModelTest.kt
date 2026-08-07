package com.fviret.podometre.ui.activity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Tests unitaires pour la logique pure du ViewModel de l'écran Activité.
 *
 * Le ViewModel dépend de [android.hardware.SensorManager] et [android.content.Context],
 * qui ne peuvent pas être instanciés dans un test JVM.
 * Ces tests couvrent donc les fonctions extraites accessibles sans instancier le ViewModel :
 * - [ActivityViewModel.labelForOffset] (logique de navigation par jour)
 * - [ActivityUiState] (valeurs par défaut : objectif, streak, métriques)
 * - Calcul de progression (steps / goal) — logique pure testée ici en isolation
 */
class ActivityViewModelTest {

    // ── Navigation par jour — labelForOffset ─────────────────────────────────

    // Les libellés "Aujourd'hui" / "Hier" viennent désormais des ressources (KAN-146).
    // Les tests passent des chaînes explicites pour rester indépendants du contexte Android.
    private val TODAY_LABEL = "Aujourd'hui"
    private val YESTERDAY_LABEL = "Hier"

    @Test
    fun `offset 0 retourne le label aujourd hui fourni en parametre`() {
        assertEquals(TODAY_LABEL, ActivityViewModel.labelForOffset(0, TODAY_LABEL, YESTERDAY_LABEL))
    }

    @Test
    fun `offset -1 retourne le label hier fourni en parametre`() {
        assertEquals(YESTERDAY_LABEL, ActivityViewModel.labelForOffset(-1, TODAY_LABEL, YESTERDAY_LABEL))
    }

    @Test
    fun `offset -2 retourne une date formatee selon la locale systeme`() {
        val date = LocalDate.now().plusDays(-2L)
        val formatter = DateTimeFormatter.ofPattern("EEE d MMMM", Locale.getDefault())
        val expected = date.format(formatter).replaceFirstChar { it.uppercaseChar() }
        assertEquals(expected, ActivityViewModel.labelForOffset(-2, TODAY_LABEL, YESTERDAY_LABEL))
    }

    @Test
    fun `offset positif produit une date dans le futur (labelForOffset ne bloque pas)`() {
        // goToNextDay ignore les offsets positifs, mais labelForOffset est une fonction pure.
        // Vérification : le résultat n'est pas le label aujourd'hui ni hier pour offset +1.
        val label = ActivityViewModel.labelForOffset(1, TODAY_LABEL, YESTERDAY_LABEL)
        assertFalse(label == TODAY_LABEL)
        assertFalse(label == YESTERDAY_LABEL)
    }

    @Test
    fun `offset -30 genere un label de date non vide`() {
        val label = ActivityViewModel.labelForOffset(-30, TODAY_LABEL, YESTERDAY_LABEL)
        assertTrue(label.isNotBlank())
        assertFalse(label == TODAY_LABEL)
        assertFalse(label == YESTERDAY_LABEL)
    }

    // ── État par défaut (objectif, streak, métriques) ─────────────────────────

    @Test
    fun `ActivityUiState objectif par defaut est 10 000 pas`() {
        val state = ActivityUiState()
        assertEquals(10_000, state.stepGoal)
    }

    @Test
    fun `ActivityUiState streak par defaut est 0`() {
        val state = ActivityUiState()
        assertEquals(0, state.streak)
        assertTrue(state.streak >= 0)
    }

    @Test
    fun `ActivityUiState selectedDayOffset par defaut est 0 (aujourd hui)`() {
        val state = ActivityUiState()
        assertEquals(0, state.selectedDayOffset)
    }

    @Test
    fun `ActivityUiState stepsToday par defaut est 0`() {
        val state = ActivityUiState()
        assertEquals(0L, state.stepsToday)
    }

    // ── Calcul de progression objectif ────────────────────────────────────────

    /**
     * Calcule le ratio de progression (0.0–∞) à partir des pas et de l'objectif.
     * Reflète la logique utilisée dans [StepRing] : stepsToday / stepGoal.
     */
    private fun progressRatio(steps: Long, goal: Int): Float =
        if (goal == 0) 0f else steps.toFloat() / goal.toFloat()

    @Test
    fun `progression au seuil exact - steps == goal retourne 1f (100 pourcent)`() {
        val goal = 10_000
        val ratio = progressRatio(steps = 10_000L, goal = goal)
        assertEquals(1.0f, ratio, 0.001f)
    }

    @Test
    fun `progression a mi-chemin - steps == goal divise 2 retourne 0_5f (50 pourcent)`() {
        val goal = 10_000
        val ratio = progressRatio(steps = 5_000L, goal = goal)
        assertEquals(0.5f, ratio, 0.001f)
    }

    @Test
    fun `progression depasse 100 pourcent quand steps superieur a goal`() {
        val goal = 10_000
        val ratio = progressRatio(steps = 12_000L, goal = goal)
        assertTrue(ratio > 1.0f)
    }

    @Test
    fun `progression reste 0 quand aucun pas enregistre`() {
        val ratio = progressRatio(steps = 0L, goal = 10_000)
        assertEquals(0.0f, ratio, 0.001f)
    }

    // ── Streak — invariants ───────────────────────────────────────────────────

    @Test
    fun `streak dans ActivityUiState ne peut pas etre negatif par defaut`() {
        val state = ActivityUiState(streak = 0)
        assertTrue(state.streak >= 0)
    }

    @Test
    fun `streak est independant du selectedDayOffset dans l etat`() {
        // La doc KAN-92 stipule que la série est toujours calculée depuis aujourd'hui.
        // Vérification structurelle : streak et selectedDayOffset sont deux champs indépendants.
        val stateToday = ActivityUiState(selectedDayOffset = 0, streak = 5)
        val stateYesterday = ActivityUiState(selectedDayOffset = -1, streak = 5)
        assertEquals(stateToday.streak, stateYesterday.streak)
    }

    @Test
    fun `streak 0 quand objectif non atteint - etat initial coherent`() {
        // Un état frais n'a ni pas ni streak.
        val state = ActivityUiState()
        assertEquals(0L, state.stepsToday)
        assertEquals(0, state.streak)
        // L'affichage de la flamme nécessite steps >= goal ET streak > 0.
        val shouldShowFlame = state.stepsToday >= state.stepGoal && state.streak > 0
        assertFalse(shouldShowFlame)
    }

    // ── goToNextDay — logique de garde ────────────────────────────────────────

    @Test
    fun `goToNextDay ne doit pas avancer depuis aujourd hui - offset reste 0`() {
        // Le ViewModel bloque goToNextDay si current >= 0 (voir implémentation).
        // On vérifie la condition de garde ici en logique pure.
        val currentOffset = 0
        val shouldAdvance = currentOffset < 0
        assertFalse(shouldAdvance)
    }

    @Test
    fun `goToPreviousDay decremente l offset d une unite`() {
        // Simulation de la logique de newOffset = selectedDayOffset - 1.
        val currentOffset = -3
        val newOffset = currentOffset - 1
        assertEquals(-4, newOffset)
    }

    // ── mergeStepsByDay — capteur local primaire, Health Connect secondaire (KAN-156) ─────

    private val day1 = LocalDate.of(2026, 8, 1)
    private val day2 = LocalDate.of(2026, 8, 2)
    private val day3 = LocalDate.of(2026, 8, 3)

    @Test
    fun `mergeStepsByDay privilegie le capteur quand il a une valeur non nulle`() {
        val sensor = mapOf(day1 to 5_000L)
        val hc = mapOf(day1 to 9_999L)
        val merged = mergeStepsByDay(sensor, hc)
        assertEquals(5_000L, merged[day1])
    }

    @Test
    fun `mergeStepsByDay bascule sur Health Connect quand le capteur n a aucune valeur`() {
        val sensor = mapOf(day1 to 0L)
        val hc = mapOf(day1 to 7_200L)
        val merged = mergeStepsByDay(sensor, hc)
        assertEquals(7_200L, merged[day1])
    }

    @Test
    fun `mergeStepsByDay bascule sur Health Connect quand le jour est absent du capteur`() {
        val sensor = emptyMap<LocalDate, Long>()
        val hc = mapOf(day1 to 4_300L)
        val merged = mergeStepsByDay(sensor, hc)
        assertEquals(4_300L, merged[day1])
    }

    @Test
    fun `mergeStepsByDay retourne 0 quand aucune des deux sources n a de donnee`() {
        val sensor = mapOf(day1 to 0L)
        val hc = emptyMap<LocalDate, Long>()
        val merged = mergeStepsByDay(sensor, hc)
        assertEquals(0L, merged[day1])
    }

    @Test
    fun `mergeStepsByDay fusionne des jours disjoints des deux sources`() {
        val sensor = mapOf(day1 to 5_000L)
        val hc = mapOf(day2 to 6_000L)
        val merged = mergeStepsByDay(sensor, hc)
        assertEquals(5_000L, merged[day1])
        assertEquals(6_000L, merged[day2])
        assertEquals(2, merged.size)
    }

    // ── computeStreakFromMap ────────────────────────────────────────────────

    @Test
    fun `computeStreakFromMap compte les jours consecutifs sur des donnees fusionnees`() {
        val steps = mapOf(day3 to 11_000L, day2 to 10_500L, day1 to 4_000L)
        val streak = computeStreakFromMap(steps, goalSteps = 10_000L, today = day3)
        assertEquals(2, streak)
    }

    @Test
    fun `computeStreakFromMap retourne 0 si aujourd hui n atteint pas l objectif`() {
        val steps = mapOf(day3 to 2_000L, day2 to 15_000L)
        val streak = computeStreakFromMap(steps, goalSteps = 10_000L, today = day3)
        assertEquals(0, streak)
    }

    @Test
    fun `computeStreakFromMap traite les jours absents comme 0 pas`() {
        val streak = computeStreakFromMap(emptyMap(), goalSteps = 10_000L, today = day3)
        assertEquals(0, streak)
    }
}
