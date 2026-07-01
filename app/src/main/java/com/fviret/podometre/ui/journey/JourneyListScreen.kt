package com.fviret.podometre.ui.journey

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fviret.podometre.domain.JourneyData
import com.fviret.podometre.domain.model.Journey
import com.fviret.podometre.domain.model.JourneyCategory
import com.fviret.podometre.domain.model.JourneyProgress
import com.fviret.podometre.domain.model.progressPercent

/**
 * Écran principal du catalogue des trajets.
 * Affiche les 19 trajets organisés en sections par catégorie.
 * Équivalent iOS : JourneyPickerView.swift
 */
@Composable
fun JourneyListScreen(
    viewModel: JourneyListViewModel = hiltViewModel()
) {
    val progressMap by viewModel.progressMap.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()

    var selectedJourney by remember { mutableStateOf<Journey?>(null) }
    var showAbandonDialog by rememberSaveable { mutableStateOf(false) }

    val journeysByCategory = remember(JourneyData.all) {
        JourneyCategory.entries.map { cat ->
            cat to JourneyData.all.filter { it.category == cat }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            Text(
                text = "Mes Trajets",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }

        journeysByCategory.forEach { (category, journeys) ->
            item(key = category.name) {
                Text(
                    text = category.displayName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(journeys, key = { it.id.toString() }) { journey ->
                val progress = progressMap[journey.id.toString()]
                val isCompleted = journey.id.toString() in preferences.completedJourneyIds
                JourneyCard(
                    journey = journey,
                    progress = progress,
                    isCompleted = isCompleted,
                    onPreview = { selectedJourney = journey },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            item(key = "${category.name}_spacer") {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    selectedJourney?.let { journey ->
        val activeJourneyId = preferences.activeJourneyId
        val isThisJourneyActive = activeJourneyId == journey.id.toString()
        val hasActiveOther = activeJourneyId != null && !isThisJourneyActive

        JourneyPreviewSheet(
            journey = journey,
            progress = progressMap[journey.id.toString()],
            isCompleted = journey.id.toString() in preferences.completedJourneyIds,
            isActive = isThisJourneyActive,
            onDismiss = { selectedJourney = null },
            onStart = {
                if (hasActiveOther) {
                    showAbandonDialog = true
                } else {
                    viewModel.startJourney(journey.id.toString())
                    selectedJourney = null
                }
            }
        )

        if (showAbandonDialog && hasActiveOther) {
            AbandonJourneyDialog(
                onConfirm = {
                    viewModel.switchJourney(
                        abandonId = activeJourneyId!!,
                        newJourneyId = journey.id.toString()
                    )
                    showAbandonDialog = false
                    selectedJourney = null
                },
                onDismiss = { showAbandonDialog = false }
            )
        }
    }
}

/**
 * Carte représentant un trajet dans le catalogue.
 * Affiche l'emoji, nom, sous-titre, distance, nombre d'étapes,
 * et une barre de progression si le trajet est en cours.
 */
@Composable
private fun JourneyCard(
    journey: Journey,
    progress: JourneyProgress?,
    isCompleted: Boolean,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressPercent = if (progress != null) journey.progressPercent(progress).toFloat() else 0f
    val isInProgress = progress != null && !isCompleted

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = journey.emoji, style = MaterialTheme.typography.titleLarge)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = journey.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isCompleted) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Terminé ✓",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = journey.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetaBadge(text = formatKm(journey.totalKm))
                        MetaBadge(text = "${journey.milestones.size} étapes")
                    }
                }
            }

            if (isInProgress) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(progressPercent * 100).toInt()}% — ${formatKm(progress!!.totalKm)} parcourus",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onPreview,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isInProgress) "Voir mes étapes" else "Voir le trajet"
                )
            }
        }
    }
}

/** Badge de métadonnée compact (distance, nombre d'étapes…). */
@Composable
private fun MetaBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Feuille de prévisualisation d'un trajet (BottomSheet).
 * Affiche le header, les badges de métadonnées et le bouton de démarrage.
 * Sera enrichi dans KAN-29 avec la timeline complète des jalons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JourneyPreviewSheet(
    journey: Journey,
    progress: JourneyProgress?,
    isCompleted: Boolean,
    isActive: Boolean,
    onDismiss: () -> Unit,
    onStart: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = journey.emoji, style = MaterialTheme.typography.displaySmall)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = journey.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = journey.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Badges métadonnées
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetaBadge(text = formatKm(journey.totalKm))
                MetaBadge(text = "${journey.milestones.size} étapes")
                MetaBadge(text = journey.category.displayName)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Jalons (liste simplifiée — timeline complète dans KAN-29)
            journey.milestones.forEachIndexed { index, milestone ->
                val isUnlocked = progress?.unlockedMilestoneIds?.contains(milestone.id.toString()) == true
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (isUnlocked) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isUnlocked) "✓" else "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUnlocked) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = milestone.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${formatKm(milestone.km)} depuis le départ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bouton d'action
            Button(
                onClick = onStart,
                enabled = !isActive && !isCompleted,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = when {
                        isCompleted -> "Trajet terminé ✓"
                        isActive -> "Trajet en cours"
                        else -> "Commencer le trajet"
                    }
                )
            }
        }
    }
}

/**
 * Dialog de confirmation pour abandonner le trajet actif et en démarrer un nouveau.
 */
@Composable
private fun AbandonJourneyDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Changer de trajet ?") },
        text = { Text("Tu as un trajet en cours. Veux-tu l'abandonner et commencer celui-ci ? Ta progression actuelle sera perdue.") },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Abandonner et démarrer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

/** Formate une distance en km avec 1 décimale si non entière, sinon sans. */
private fun formatKm(km: Double): String {
    return if (km == km.toLong().toDouble()) "${km.toLong()} km" else "${"%.1f".format(km)} km"
}
