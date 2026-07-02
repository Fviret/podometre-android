package com.fviret.podometre.ui.journey

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fviret.podometre.domain.model.Journey
import com.fviret.podometre.domain.model.JourneyProgress
import com.fviret.podometre.domain.model.Milestone
import com.fviret.podometre.domain.model.nextMilestone
import com.fviret.podometre.domain.model.progressPercent

/**
 * Écran de détail d'un trajet en cours.
 * Affiche la progression globale, la prochaine étape et la timeline des jalons.
 * Équivalent iOS : JourneyDetailView.swift
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyDetailScreen(
    onBack: () -> Unit,
    viewModel: JourneyDetailViewModel = hiltViewModel(),
) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val journey = viewModel.journey

    // Jalon nouvellement débloqué → afficher une BottomSheet
    var unlockedSheetMilestoneId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        viewModel.newlyUnlockedMilestoneId.collect { milestoneId ->
            unlockedSheetMilestoneId = milestoneId
        }
    }

    if (journey == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Trajet introuvable.")
        }
        return
    }

    val isCompleted = journey.id.toString() in preferences.completedJourneyIds
    val sortedMilestones = remember(journey) { journey.milestones.sortedBy { it.km } }
    val lastUnlockedIndex = sortedMilestones.indexOfLast {
        progress?.unlockedMilestoneIds?.contains(it.id.toString()) == true
    }
    val listState = rememberLazyListState()

    // Scroll automatique vers le dernier jalon débloqué au chargement
    LaunchedEffect(lastUnlockedIndex) {
        if (lastUnlockedIndex >= 0) {
            listState.animateScrollToItem(
                index = (lastUnlockedIndex + HEADER_ITEMS).coerceAtMost(
                    HEADER_ITEMS + sortedMilestones.lastIndex
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = journey.emoji, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = journey.name, style = MaterialTheme.typography.titleMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            // ── Header progression ────────────────────────────────────────────
            item {
                ProgressHeader(
                    journey = journey,
                    progress = progress,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Prochaine étape / Trajet achevé ──────────────────────────────
            item {
                if (isCompleted || (progress != null && journey.progressPercent(progress!!) >= 1.0)) {
                    CompletedCard()
                } else {
                    progress?.let { p ->
                        journey.nextMilestone(p)?.let { next ->
                            NextMilestoneCard(
                                milestone = next,
                                distanceRemainingKm = next.km - p.totalKm,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── Section titre timeline ────────────────────────────────────────
            item {
                Text(
                    text = "ÉTAPES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Timeline des jalons ───────────────────────────────────────────
            itemsIndexed(sortedMilestones, key = { _, m -> m.id.toString() }) { index, milestone ->
                val isUnlocked = progress?.unlockedMilestoneIds
                    ?.contains(milestone.id.toString()) == true
                val isLast = index == sortedMilestones.lastIndex

                DetailMilestoneRow(
                    number = index + 1,
                    milestone = milestone,
                    isUnlocked = isUnlocked,
                    showConnector = !isLast,
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // ── BottomSheet jalon nouvellement débloqué ───────────────────────────
    unlockedSheetMilestoneId?.let { milestoneId ->
        val milestone = journey.milestones.firstOrNull { it.id.toString() == milestoneId }
        if (milestone != null) {
            MilestoneUnlockedSheet(
                milestone = milestone,
                onDismiss = { unlockedSheetMilestoneId = null },
            )
        }
    }
}

/** Nombre d'items LazyColumn avant la liste de jalons (pour le scroll automatique). */
private const val HEADER_ITEMS = 3

// ── Composables internes ──────────────────────────────────────────────────────

/**
 * Header avec barre de progression linéaire, km parcourus / km total et pourcentage.
 */
@Composable
private fun ProgressHeader(
    journey: Journey,
    progress: JourneyProgress?,
) {
    val percent = if (progress != null) journey.progressPercent(progress).toFloat() else 0f
    val totalKmStr = formatKm(journey.totalKm)
    val doneKmStr = formatKm(progress?.totalKm ?: 0.0)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$doneKmStr / $totalKmStr",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${(percent * 100).toInt()} %",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { percent },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
        )
    }
}

/** Card affichée quand le trajet est complété à 100%. */
@Composable
private fun CompletedCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🏁 Trajet achevé !",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "Félicitations, tu as parcouru tout ce trajet !",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

/**
 * Card affichant le prochain jalon à atteindre et la distance restante.
 */
@Composable
private fun NextMilestoneCard(
    milestone: Milestone,
    distanceRemainingKm: Double,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Prochaine étape",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = milestone.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = "encore ${formatKm(distanceRemainingKm.coerceAtLeast(0.0))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

/**
 * Ligne de timeline pour un jalon dans l'écran de détail.
 * Cercle plein = débloqué, cercle vide = verrouillé.
 * Accessibilité : chaque jalon décrit son état via [semantics].
 */
@Composable
private fun DetailMilestoneRow(
    number: Int,
    milestone: Milestone,
    isUnlocked: Boolean,
    showConnector: Boolean,
) {
    val circleColor = if (isUnlocked) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val connectorColor = MaterialTheme.colorScheme.outlineVariant

    val accessibilityLabel = if (isUnlocked)
        "Étape $number débloquée : ${milestone.label}, ${formatKm(milestone.km)} depuis le départ"
    else
        "Étape $number verrouillée : ${milestone.label}, ${formatKm(milestone.km)} depuis le départ"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = accessibilityLabel },
    ) {
        // Colonne gauche : cercle + connecteur
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(circleColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isUnlocked) "✓" else "$number",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (showConnector) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(72.dp)
                        .background(connectorColor)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Colonne droite : textes
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = milestone.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (isUnlocked) milestone.description
                else "${formatKm(milestone.km)} depuis le départ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(if (showConnector) 40.dp else 8.dp))
        }
    }
}

/**
 * BottomSheet modale affichée quand un jalon est nouvellement débloqué.
 * Affiche le titre et la description du jalon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MilestoneUnlockedSheet(
    milestone: Milestone,
    onDismiss: () -> Unit,
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
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "🎉", style = MaterialTheme.typography.displayMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Nouvelle étape débloquée !",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = milestone.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = milestone.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatKm(km: Double): String =
    if (km == km.toLong().toDouble()) "${km.toLong()} km" else "${"%.1f".format(km)} km"
