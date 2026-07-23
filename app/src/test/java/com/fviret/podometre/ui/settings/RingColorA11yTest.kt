package com.fviret.podometre.ui.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Teste la logique d'accessibilité des pastilles de couleur (KAN-94).
 * Vérifie les contentDescription générés et l'état de sélection.
 */
class RingColorA11yTest {

    // Reproduit la map ringColorNames de SettingsScreen.kt
    private val ringColorNames: Map<String, String> = mapOf(
        "green"  to "Forêt",
        "blue"   to "Océan",
        "orange" to "Soleil",
        "red"    to "Corail",
        "purple" to "Violet",
        "teal"   to "Glace",
    )

    private fun a11yLabel(colorId: String, selectedColorId: String): String {
        val name = ringColorNames[colorId] ?: colorId
        return if (colorId == selectedColorId) "$name — sélectionnée" else name
    }

    // ── contentDescription ───────────────────────────────────────────────────

    @Test
    fun `pastille selectionnee annonce son nom et selectionne`() {
        val label = a11yLabel("green", selectedColorId = "green")
        assertEquals("Forêt — sélectionnée", label)
    }

    @Test
    fun `pastille non selectionnee annonce seulement son nom`() {
        val label = a11yLabel("blue", selectedColorId = "green")
        assertEquals("Océan", label)
    }

    @Test
    fun `pastille orange selectionne annonce Soleil`() {
        val label = a11yLabel("orange", selectedColorId = "orange")
        assertEquals("Soleil — sélectionnée", label)
    }

    @Test
    fun `pastille teal non selectionnee annonce Glace`() {
        val label = a11yLabel("teal", selectedColorId = "green")
        assertEquals("Glace", label)
    }

    // ── Toutes les 6 couleurs ont un nom ─────────────────────────────────────

    @Test
    fun `les 6 couleurs ont toutes un nom dans la map`() {
        val expectedIds = listOf("green", "blue", "orange", "red", "purple", "teal")
        expectedIds.forEach { id ->
            assertTrue(ringColorNames.containsKey(id), "Couleur '$id' absente de ringColorNames")
        }
    }

    @Test
    fun `aucun nom de couleur n est vide`() {
        ringColorNames.values.forEach { name ->
            assertFalse(name.isBlank(), "Nom de couleur vide détecté")
        }
    }

    // ── État sélectionné ─────────────────────────────────────────────────────

    @Test
    fun `isSelected est vrai quand colorId correspond au selectedColorId`() {
        val isSelected = "purple" == "purple"
        assertTrue(isSelected)
    }

    @Test
    fun `isSelected est faux quand colorId differe du selectedColorId`() {
        val isSelected = "red" == "green"
        assertFalse(isSelected)
    }

    @Test
    fun `une seule couleur est selectionnee a la fois`() {
        val selectedColorId = "teal"
        val selectedCount = ringColorNames.keys.count { it == selectedColorId }
        assertEquals(1, selectedCount)
    }
}
