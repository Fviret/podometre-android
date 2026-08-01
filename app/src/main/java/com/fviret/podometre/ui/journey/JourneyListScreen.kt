package com.fviret.podometre.ui.journey

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fviret.podometre.domain.JourneyData
import com.fviret.podometre.domain.model.Journey
import com.fviret.podometre.domain.model.JourneyCategory
import com.fviret.podometre.domain.model.JourneyProgress
import com.fviret.podometre.domain.model.formatKm
import com.fviret.podometre.domain.model.progressPercent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.fviret.podometre.R
import com.fviret.podometre.domain.model.displayName
import com.fviret.podometre.ui.theme.rememberReduceMotion
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Écran principal du catalogue des trajets.
 * Affiche la card du trajet en cours en tête (si trajet actif),
 * puis les 19 trajets organisés en sections par catégorie.
 * Équivalent iOS : JourneyPickerView.swift
 */
@Composable
fun JourneyListScreen(
    onNavigateToDetail: (String) -> Unit = {},
    viewModel: JourneyListViewModel = hiltViewModel()
) {
    val progressMap by viewModel.progressMap.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val activeJourneyCard by viewModel.activeJourneyCard.collectAsStateWithLifecycle()

    var selectedJourney by remember { mutableStateOf<Journey?>(null) }
    var showAbandonDialog by rememberSaveable { mutableStateOf(false) }

    // ID du trajet en cours — à exclure du catalogue
    val activeJourneyId = activeJourneyCard?.journey?.id?.toString()

    val journeysByCategory = remember(JourneyData.all) {
        JourneyCategory.entries.map { cat ->
            cat to JourneyData.all.filter { it.category == cat }
        }
    }

    val reduceMotion = rememberReduceMotion()
    val context = LocalContext.current

    // Catégories dépliées par défaut : celle contenant le trajet en cours
    val defaultExpanded = remember(activeJourneyId) {
        journeysByCategory
            .filter { (_, journeys) -> journeys.any { it.id.toString() == activeJourneyId } }
            .map { it.first }
            .toSet()
    }
    var expandedCategories by rememberSaveable { mutableStateOf(defaultExpanded) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.journey_screen_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }

        // Card du trajet en cours épinglée en tête
        activeJourneyCard?.let { cardData ->
            item(key = "active_journey_card") {
                ActiveJourneyCard(
                    cardData = cardData,
                    onClick = { onNavigateToDetail(cardData.journey.id.toString()) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // KAN-128 : card d'incitation quand aucun trajet n'est en cours
        if (activeJourneyCard == null) {
            item(key = "empty_state_card") {
                EmptyJourneyCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // KAN-129 : catégories en accordéon
        journeysByCategory.forEach { (category, journeys) ->
            val filteredJourneys = journeys.filter { it.id.toString() != activeJourneyId }
            if (filteredJourneys.isEmpty()) return@forEach

            val completed = filteredJourneys.filter { it.id.toString() in preferences.completedJourneyIds }
            val notCompleted = filteredJourneys.filter { it.id.toString() !in preferences.completedJourneyIds }
            val remainingCount = notCompleted.size
            val isExpanded = category in expandedCategories

            item(key = "${category.name}_header") {
                CategoryHeader(
                    displayName = category.displayName(context),
                    remainingCount = remainingCount,
                    isExpanded = isExpanded,
                    reduceMotion = reduceMotion,
                    onClick = {
                        expandedCategories = if (isExpanded)
                            expandedCategories - category
                        else
                            expandedCategories + category
                    }
                )
            }

            if (isExpanded) {
                // KAN-130 : grille des badges terminés en tête
                if (completed.isNotEmpty()) {
                    item(key = "${category.name}_completed_label") {
                        Text(
                            text = "TERMINÉS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 2.dp)
                        )
                    }
                    item(key = "${category.name}_badges") {
                        CompletedBadgesGrid(
                            journeys = completed,
                            progressMap = progressMap,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                items(notCompleted, key = { it.id.toString() }) { journey ->
                    val progress = progressMap[journey.id.toString()]
                    val isInProgress = progress != null
                    JourneyCard(
                        journey = journey,
                        progress = progress,
                        isCompleted = false,
                        onPreview = { selectedJourney = journey },
                        onDetail = if (isInProgress) {
                            { onNavigateToDetail(journey.id.toString()) }
                        } else null,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            item(key = "${category.name}_spacer") {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    selectedJourney?.let { journey ->
        val isThisJourneyActive = preferences.activeJourneyId == journey.id.toString()
        val hasActiveOther = preferences.activeJourneyId != null && !isThisJourneyActive

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
                    preferences.activeJourneyId?.let { abandonId ->
                        viewModel.switchJourney(
                            abandonId = abandonId,
                            newJourneyId = journey.id.toString()
                        )
                    }
                    showAbandonDialog = false
                    selectedJourney = null
                },
                onDismiss = { showAbandonDialog = false }
            )
        }
    }
}

/**
 * Card d'incitation affichée quand aucun trajet n'est en cours (KAN-128).
 * Même gabarit que [ActiveJourneyCard] pour une mise en page stable entre les états.
 * Purement informatif — aucune action au tap.
 */
@Composable
private fun EmptyJourneyCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🗺️", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Aucun trajet en cours",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Choisissez votre prochain trajet ci-dessous.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * En-tête de catégorie cliquable pour l'accordéon (KAN-129).
 * Affiche le nom, le nombre de trajets restants, et un chevron qui pivote.
 */
@Composable
private fun CategoryHeader(
    displayName: String,
    remainingCount: Int,
    isExpanded: Boolean,
    reduceMotion: Boolean,
    onClick: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        label = "chevron_rotation",
        animationSpec = if (reduceMotion) snap() else spring()
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = displayName.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (remainingCount > 0) {
            Text(
                text = "$remainingCount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = if (isExpanded)
                stringResource(R.string.journey_category_collapse, displayName)
            else
                stringResource(R.string.journey_category_expand, displayName),
            modifier = Modifier
                .size(12.dp)
                .graphicsLayer { rotationZ = rotation },
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Grille fluide des trajets terminés dans une catégorie (KAN-130).
 * Chaque badge ouvre une popup de complétion au tap.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun CompletedBadgesGrid(
    journeys: List<Journey>,
    progressMap: Map<String, JourneyProgress>,
    modifier: Modifier = Modifier
) {
    var popupJourney by remember { mutableStateOf<Journey?>(null) }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        journeys.forEach { journey ->
            CompletedBadgeChip(
                journey = journey,
                onClick = { popupJourney = journey }
            )
        }
    }

    popupJourney?.let { journey ->
        CompletedJourneyPopup(
            journey = journey,
            progress = progressMap[journey.id.toString()],
            onDismiss = { popupJourney = null }
        )
    }
}

/** Chip compacte représentant un trajet terminé : emoji + nom. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun CompletedBadgeChip(
    journey: Journey,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.semantics {
            contentDescription = "${journey.name}, terminé. Appuyer pour les détails."
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = journey.emoji,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { invisibleToUser() }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = journey.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Popup de complétion d'un trajet terminé.
 * Affiche : emoji, nom, sous-titre, date de complétion, distance totale, nombre d'étapes.
 */
@Composable
private fun CompletedJourneyPopup(
    journey: Journey,
    progress: JourneyProgress?,
    onDismiss: () -> Unit,
) {
    val completionDate = progress?.completionDate
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(journey.emoji, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(12.dp))
                Text(journey.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    journey.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                if (completionDate != null) {
                    Text(
                        text = "Terminé le ${completionDate.format(formatter)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Distance : ${formatKm(journey.totalKm)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${journey.milestones.size} étapes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}

/**
 * Card épinglée du trajet en cours, avec trois niveaux de lecture :
 * 1. En-tête : emoji, nom, pourcentage global.
 * 2. Tracé du segment entre le dernier et le prochain jalon avec marqueur « tu es ici ».
 * 3. Distance jusqu'à la prochaine étape + date d'arrivée estimée.
 *
 * La card entière est un seul élément cliquable avec un résumé vocal complet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveJourneyCard(
    cardData: ActiveJourneyCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val journey = cardData.journey
    val globalPct = (cardData.globalPercent * 100).toInt()
    val etaText = cardData.etaDate?.let { date ->
        "le ${date.format(DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH))}"
    }

    val activeDesc = stringResource(R.string.journey_active_card_a11y, journey.name, globalPct)
    val milestoneDesc = stringResource(R.string.journey_active_card_milestone, cardData.nextMilestoneLabel, formatKm(cardData.kmToNextMilestone))
    val etaDesc = etaText?.let { stringResource(R.string.journey_active_card_eta, it) }
    val a11yDescription = buildString {
        append(activeDesc)
        append(" ")
        append(milestoneDesc)
        etaDesc?.let { append(" "); append(it) }
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_journey_card")
            .semantics { contentDescription = a11yDescription },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {

            // ── Niveau 1 : En-tête ───────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = journey.emoji,
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = journey.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.journey_active_label, globalPct),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Niveau 2 : Tracé du segment avec marqueur « tu es ici » ─────
            SegmentTrack(
                leftLabel = cardData.lastMilestoneLabel,
                rightLabel = cardData.nextMilestoneLabel,
                progress = cardData.segmentProgress
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ── Niveau 3 : Distance + ETA ────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.journey_next_milestone_distance, cardData.kmToNextMilestone),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                etaText?.let {
                    Text(
                        text = stringResource(R.string.journey_estimated_arrival, it),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

/**
 * Tracé horizontal du segment courant entre deux jalons.
 * Un point mobile indique la position « tu es ici » selon [progress] (0.0 à 1.0).
 * Libellés gauche (dernier jalon) et droite (prochain jalon) en dessous.
 */
@Composable
private fun SegmentTrack(
    leftLabel: String,
    rightLabel: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
    val progressColor = MaterialTheme.colorScheme.primary
    val markerColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
        ) {
            val trackY = size.height / 2f
            val trackHeight = 4.dp.toPx()
            val markerRadius = 7.dp.toPx()
            val padding = markerRadius

            // Piste totale
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(padding, trackY - trackHeight / 2),
                size = androidx.compose.ui.geometry.Size(size.width - padding * 2, trackHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2)
            )

            // Portion parcourue
            val progressWidth = (size.width - padding * 2) * progress.coerceIn(0f, 1f)
            if (progressWidth > 0f) {
                drawRoundRect(
                    color = progressColor,
                    topLeft = Offset(padding, trackY - trackHeight / 2),
                    size = androidx.compose.ui.geometry.Size(progressWidth, trackHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2)
                )
            }

            // Marqueur « tu es ici »
            val markerX = padding + (size.width - padding * 2) * progress.coerceIn(0f, 1f)
            drawCircle(
                color = markerColor,
                radius = markerRadius,
                center = Offset(markerX, trackY)
            )
            drawCircle(
                color = androidx.compose.ui.graphics.Color.White,
                radius = markerRadius * 0.45f,
                center = Offset(markerX, trackY)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = leftLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = rightLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }
}

/**
 * Carte représentant un trajet dans le catalogue.
 * La card entière est le seul élément tappable (un chevron › indique l'affordance visuelle).
 * Les cartes à l'état « Terminé » sont non-interactives.
 * Affiche l'emoji, nom, sous-titre, distance, nombre d'étapes,
 * et une barre de progression si le trajet est en cours.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun JourneyCard(
    journey: Journey,
    progress: JourneyProgress?,
    isCompleted: Boolean,
    onPreview: () -> Unit,
    onDetail: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val progressPercent = if (progress != null) journey.progressPercent(progress).toFloat() else 0f
    val isInProgress = progress != null && !isCompleted

    // Description accessibilité complète : nom, sous-titre, distance, étapes, progression éventuelle.
    val a11yLabel = buildString {
        append("${journey.name}, ${journey.subtitle}. ")
        append("${formatKm(journey.totalKm)}, ${journey.milestones.size} étapes. ")
        if (isInProgress && progress != null) {
            append("Progression : ${(progressPercent * 100).toInt()} %, ${formatKm(progress.totalKm)} parcourus. ")
        }
    }

    // Libellé de l'action vocale selon l'état du trajet.
    val a11yActionLabel = if (isInProgress) "Voir mes étapes" else "Voir le trajet"

    // Contenu partagé entre la version cliquable et non-cliquable.
    @Composable
    fun CardContent() {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = journey.emoji,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { invisibleToUser() },
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = journey.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
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
                        // Pastille purement visuelle — invisible pour TalkBack (annoncé via contentDescription de la card).
                        if (!isCompleted) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier.semantics { invisibleToUser() }
                            ) {
                                Text(
                                    text = a11yActionLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (!isCompleted) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isInProgress) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .semantics { invisibleToUser() },
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(progressPercent * 100).toInt()}% — ${formatKm(progress?.totalKm ?: 0.0)} parcourus",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { invisibleToUser() }
                )
            }
        }
    }

    if (isCompleted) {
        // Carte non-interactive : les trajets terminés ne réagissent pas au tap.
        Card(
            modifier = modifier
                .fillMaxWidth()
                .semantics { contentDescription = a11yLabel },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            CardContent()
        }
    } else {
        // Carte interactive : toute la surface déclenche l'action.
        val onCardClick = if (isInProgress && onDetail != null) onDetail else onPreview
        Card(
            onClick = onCardClick,
            modifier = modifier
                .fillMaxWidth()
                .semantics {
                    // La description inclut le résumé complet + l'action vocale pour TalkBack.
                    contentDescription = "$a11yLabel$a11yActionLabel."
                },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            CardContent()
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
 * Feuille de prévisualisation complète d'un trajet (BottomSheet).
 * Header + badges métadonnées + timeline verticale des jalons (scrollable) + bouton fixe en bas.
 * Équivalent iOS : JourneyPreviewSheet.swift
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
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
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        // ── Contenu entièrement scrollable — pas de zone vide fixe ──────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 24.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = journey.emoji,
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.semantics { invisibleToUser() },
                )
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
                MetaBadge(text = journey.category.displayName(LocalContext.current))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ÉTAPES DU TRAJET",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Timeline verticale des jalons
            val sortedMilestones = journey.milestones.sortedBy { it.km }
            sortedMilestones.forEachIndexed { index, milestone ->
                val isUnlocked = progress?.unlockedMilestoneIds
                    ?.contains(milestone.id.toString()) == true

                MilestoneRow(
                    number = index + 1,
                    label = milestone.label,
                    distanceKm = milestone.km,
                    description = milestone.description,
                    isUnlocked = isUnlocked,
                    showConnector = true,
                )
            }

            // ── Terminus de la timeline ──────────────────────────────────────
            TimelineTerminus(label = stringResource(R.string.journey_timeline_end))

            Spacer(modifier = Modifier.height(16.dp))

            // ── Bouton CTA ───────────────────────────────────────────────────
            Button(
                onClick = onStart,
                enabled = !isActive && !isCompleted,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = when {
                        isCompleted -> stringResource(R.string.journey_completed_label)
                        isActive -> stringResource(R.string.journey_continue_button)
                        else -> stringResource(R.string.journey_start_button)
                    }
                )
            }
        }
    }
}

/**
 * Ligne de timeline pour un jalon :
 * cercle numéroté (plein = débloqué, vide = verrouillé),
 * connecteur vertical vers le jalon suivant si [showConnector],
 * nom + distance depuis le départ + description complète.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MilestoneRow(
    number: Int,
    label: String,
    distanceKm: Double,
    description: String,
    isUnlocked: Boolean,
    showConnector: Boolean,
) {
    val circleColor = if (isUnlocked) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val connectorColor = MaterialTheme.colorScheme.outlineVariant

    Row(modifier = Modifier.fillMaxWidth()) {
        // Colonne gauche : cercle + connecteur
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(circleColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isUnlocked) "✓" else "$number",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { invisibleToUser() },
                )
            }
            if (showConnector) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(64.dp)
                        .background(connectorColor)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Colonne droite : textes
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (showConnector) 0.dp else 0.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${formatKm(distanceKm)} depuis le départ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(if (showConnector) 32.dp else 8.dp))
        }
    }
}

/**
 * Terminus visuel de la timeline : connecteur + cercle avec icône drapeau + label "Arrivée".
 * Indique la fin du trajet après le dernier jalon.
 */
@Composable
private fun TimelineTerminus(label: String) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val connectorColor = MaterialTheme.colorScheme.outlineVariant

    Row(modifier = Modifier.fillMaxWidth()) {
        // Colonne gauche : connecteur entrant + cercle terminus
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(32.dp)
                    .background(connectorColor)
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(primaryColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Colonne droite : label aligné verticalement avec le cercle
        Box(
            modifier = Modifier
                .height(32.dp + 32.dp) // hauteur connecteur + cercle
                .padding(top = 32.dp), // aligner avec le centre du cercle
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = primaryColor
            )
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
        title = { Text(stringResource(R.string.journey_change_dialog_title)) },
        text = { Text("Tu as un trajet en cours. Veux-tu l'abandonner et commencer celui-ci ? Ta progression actuelle sera perdue.") },
        confirmButton = {
            Button(onClick = onConfirm) { Text(stringResource(R.string.journey_abandon_button)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

/** Formate une distance en km avec 1 décimale si non entière, sinon sans. */
