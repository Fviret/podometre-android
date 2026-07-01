package com.fviret.podometre.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fviret.podometre.ui.theme.AppColors

/**
 * Écran Paramètres — section US-5.1 : objectif quotidien de pas.
 * Affiche la valeur courante et un picker expandable (5 000–20 000 par pas de 500).
 * Équivalent iOS : SettingsView.swift > Section "Objectif quotidien".
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        Text(
            text = "Paramètres",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp),
        )

        // ── Section : Objectif quotidien ──────────────────────────────────────
        SectionHeader(title = "Activité")

        StepGoalRow(
            currentGoal = prefs.dailyStepGoal,
            onGoalSelected = { viewModel.updateDailyStepGoal(it) },
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── Section : Apparence ───────────────────────────────────────────────
        SectionHeader(title = "Apparence")

        RingColorRow(
            selectedColorId = prefs.ringColorId,
            onColorSelected = { viewModel.updateRingColorId(it) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader(title = "Notifications")
        ComingSoonRow(label = "Notifications de goal", ticket = "KAN-34")
        ComingSoonRow(label = "Notifications de trajet", ticket = "KAN-35")

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader(title = "Progression")
        ComingSoonRow(label = "Série de jours (streak)", ticket = "KAN-36")
        ComingSoonRow(label = "Badges", ticket = "KAN-38")
    }
}

/** En-tête d'une section de paramètres. */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
    )
}

/**
 * Ligne "Objectif quotidien" avec picker expandable.
 * Tap sur la ligne développe / réduit la grille de valeurs.
 * La valeur sélectionnée est mise en surbrillance et persiste immédiatement.
 */
@Composable
private fun StepGoalRow(
    currentGoal: Int,
    onGoalSelected: (Int) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            // ── Ligne principale ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel = if (expanded) "Fermer le picker d'objectif" else "Ouvrir le picker d'objectif",
                    ) { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .semantics { role = Role.Button },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Pas par jour",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "%,d pas".format(currentGoal).replace(',', ' '),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }

            // ── Picker expandable ──────────────────────────────────────────────
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    StepGoalGrid(
                        options = STEP_GOAL_OPTIONS,
                        selectedGoal = currentGoal,
                        onSelect = { goal ->
                            onGoalSelected(goal)
                            expanded = false
                        },
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}

/**
 * Grille des valeurs d'objectif (3 colonnes).
 * La cellule correspondant à [selectedGoal] est mise en couleur primaire.
 */
@Composable
private fun StepGoalGrid(
    options: List<Int>,
    selectedGoal: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val columns = 3
    val rows = (options.size + columns - 1) / columns

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (col in 0 until columns) {
                    val index = row * columns + col
                    if (index < options.size) {
                        val goal = options[index]
                        val isSelected = goal == selectedGoal
                        GoalCell(
                            goal = goal,
                            isSelected = isSelected,
                            onSelect = { onSelect(goal) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Cellule individuelle du picker d'objectif. */
@Composable
private fun GoalCell(
    goal: Int,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = "%,d".format(goal).replace(',', ' ')
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
            )
            .clickable(onClickLabel = "Objectif $label pas") { onSelect() }
            .padding(vertical = 10.dp)
            .semantics { contentDescription = "Objectif $label pas${if (isSelected) ", sélectionné" else ""}" },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** Noms français des couleurs de l'anneau, dans le même ordre qu'[AppColors.ringColorOptions]. */
private val ringColorNames: Map<String, String> = mapOf(
    "green"  to "Forêt",
    "blue"   to "Océan",
    "orange" to "Soleil",
    "red"    to "Corail",
    "purple" to "Violet",
    "teal"   to "Glace",
)

/**
 * Section "Couleur de l'anneau" — nom de la couleur sélectionnée + grille de 6 cercles.
 * Cercle sélectionné : bordure noire épaisse (comme iOS).
 * Équivalent iOS : Section "Personnalisation des couleurs" dans SettingsView.swift.
 */
@Composable
private fun RingColorRow(
    selectedColorId: String,
    onColorSelected: (String) -> Unit,
) {
    val selectedColor = AppColors.colorForId(selectedColorId)
    val selectedName = ringColorNames[selectedColorId] ?: selectedColorId

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            // Nom de la couleur sélectionnée avec pastille (iso iOS)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(selectedColor),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedName,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            // Grille des 6 cercles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                AppColors.ringColorOptions.forEach { (id, color) ->
                    val isSelected = id == selectedColorId
                    val name = ringColorNames[id] ?: id
                    val a11yLabel = "$name${if (isSelected) ", sélectionnée" else ""}"
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (isSelected)
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                else Modifier
                            )
                            .clickable(onClickLabel = a11yLabel) { onColorSelected(id) }
                            .semantics { contentDescription = a11yLabel },
                    )
                }
            }
        }
    }
}

/** Ligne placeholder pour les sections à venir. */
@Composable
private fun ComingSoonRow(label: String, ticket: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = ticket,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
    }
}
