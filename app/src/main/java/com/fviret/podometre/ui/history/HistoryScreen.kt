package com.fviret.podometre.ui.history

import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fviret.podometre.ui.theme.AppColors
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.ceil

private const val EARTH_CIRCUMFERENCE_KM = 40_075.0
private val SHORT_DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)
private val MONTH_YEAR_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH)

/**
 * Écran Historique — KAN-135/136/137/138/139.
 * Affiche la tendance 10 semaines, les totaux mensuels, les records personnels
 * et le total cumulé avec jauge « tour du monde ».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val accent = AppColors.ringColorOptions["green"] ?: Color(0xFF33C759)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historique") },
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
        if (uiState.isLoading && uiState.weeklyTotals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accent)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // ── Header spacer ─────────────────────────────────────────────────
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // ── Section Tendance (KAN-135/136) ────────────────────────────────
            item {
                SectionTitle("Tendance sur 10 semaines")
                Spacer(modifier = Modifier.height(12.dp))
                TrendBarChart(
                    weeklyTotals = uiState.weeklyTotals,
                    selectedIndex = uiState.selectedWeekIndex,
                    accentColor = accent,
                    onSelectBar = { idx -> viewModel.selectWeek(idx) },
                )
                AnimatedVisibility(
                    visible = uiState.selectedWeekIndex != null,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(150)),
                ) {
                    val idx = uiState.selectedWeekIndex ?: 0
                    val (weekStart, total) = uiState.weeklyTotals.getOrNull(idx) ?: return@AnimatedVisibility
                    val weekEnd = weekStart.plusDays(6)
                    val avg = if (total > 0) total / 7 else 0
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "${weekStart.format(SHORT_DATE_FMT)} – ${weekEnd.format(SHORT_DATE_FMT)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                MetricChip("Total", formatSteps(total))
                                MetricChip("Moy./jour", formatSteps(avg))
                            }
                        }
                    }
                }
            }

            // ── Section Totaux mensuels (KAN-137) ─────────────────────────────
            item {
                SectionTitle("Totaux mensuels")
                Spacer(modifier = Modifier.height(8.dp))
                MonthlyYearNavigator(
                    displayedYear = uiState.displayedYear,
                    oldestYear = uiState.oldestYear,
                    onPrevious = { viewModel.previousYear() },
                    onNext = { viewModel.nextYear() },
                )
                Spacer(modifier = Modifier.height(12.dp))
                MonthlyBarChart(
                    monthlyTotals = uiState.monthlyTotals,
                    displayedYear = uiState.displayedYear,
                    accentColor = accent,
                )
            }

            // ── Section Records personnels (KAN-139) ──────────────────────────
            item {
                SectionTitle("Records personnels")
                Spacer(modifier = Modifier.height(12.dp))
                PersonalRecordsCard(
                    bestDay = uiState.bestDay,
                    bestWeek = uiState.bestWeek,
                    bestMonth = uiState.bestMonth,
                    accentColor = accent,
                )
            }

            // ── Carte Plus longue série ────────────────────────────────────────
            item {
                LongestStreakCard(
                    longestStreak = uiState.longestStreak,
                    accentColor = accent,
                )
            }

            // ── Section Total cumulé / Tour du monde ──────────────────────────
            item {
                SectionTitle("Total cumulé")
                Spacer(modifier = Modifier.height(12.dp))
                CumulativeTotalsCard(
                    totalSteps = uiState.totalCumulativeSteps,
                    totalKm = uiState.totalCumulativeKm,
                    accentColor = accent,
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// KAN-136 : Graphe de tendance 10 semaines (barres, interactif)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun TrendBarChart(
    weeklyTotals: List<Pair<LocalDate, Long>>,
    selectedIndex: Int?,
    accentColor: Color,
    onSelectBar: (Int) -> Unit,
) {
    if (weeklyTotals.isEmpty()) {
        Text(
            text = "Aucune donnée disponible.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val maxVal = weeklyTotals.maxOf { it.second }.coerceAtLeast(1L)

    // Barres animées (hauteur proportionnelle)
    val animatedHeights = weeklyTotals.mapIndexed { idx, (_, total) ->
        val target = total.toFloat() / maxVal.toFloat()
        animateFloatAsState(
            targetValue = target,
            animationSpec = tween(durationMillis = 400, delayMillis = idx * 30),
            label = "bar_$idx"
        )
    }

    val surfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val chartHeight = 160.dp

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(chartHeight)
            .clickable(
                interactionSource = null,
                indication = null,
            ) { /* handled via pointer below */ },
    ) {
        val nBars = weeklyTotals.size
        val barWidth = (size.width - (nBars - 1) * 6.dp.toPx()) / nBars
        val padBottom = 20.dp.toPx()
        val chartH = size.height - padBottom

        weeklyTotals.forEachIndexed { idx, (weekStart, _) ->
            val barH = animatedHeights[idx].value * chartH
            val left = idx * (barWidth + 6.dp.toPx())
            val top = chartH - barH
            val isSelected = idx == selectedIndex
            val color = if (isSelected) accentColor else accentColor.copy(alpha = 0.4f)

            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )

            // Label semaine sous la barre (ex. "S1", "S2"...)
            val paint = Paint().apply {
                this.color = surfaceVariant.toArgb()
                textSize = 8.sp.toPx()
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                "S${nBars - idx}",
                left + barWidth / 2f,
                size.height - 2.dp.toPx(),
                paint
            )
        }
    }

    // Zone de tap divisée en N zones égales — détecte la barre tapée
    Row(modifier = Modifier.fillMaxWidth()) {
        weeklyTotals.indices.forEach { idx ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clickable { onSelectBar(idx) }
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// KAN-137 : Navigation année + graphe mensuel
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun MonthlyYearNavigator(
    displayedYear: Int,
    oldestYear: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val currentYear = LocalDate.now().year
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onPrevious,
            enabled = displayedYear > oldestYear,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Année précédente"
            )
        }
        Text(
            text = displayedYear.toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        IconButton(
            onClick = onNext,
            enabled = displayedYear < currentYear,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Année suivante"
            )
        }
    }
}

@Composable
private fun MonthlyBarChart(
    monthlyTotals: Map<Month, Long>,
    displayedYear: Int,
    accentColor: Color,
) {
    val today = LocalDate.now()
    val months = Month.values()
    val maxVal = monthlyTotals.values.filter { it > 0 }.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val surfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
    ) {
        val nBars = months.size
        val barWidth = (size.width - (nBars - 1) * 4.dp.toPx()) / nBars
        val padBottom = 18.dp.toPx()
        val chartH = size.height - padBottom

        months.forEachIndexed { idx, month ->
            val isFuture = displayedYear == today.year && month.value > today.monthValue
            val total = monthlyTotals[month] ?: 0L
            val barH = if (total > 0) (total.toFloat() / maxVal.toFloat()) * chartH
            else if (isFuture) chartH * 0.08f  // bar fantôme pour les mois futurs
            else 2.dp.toPx()
            val left = idx * (barWidth + 4.dp.toPx())
            val top = chartH - barH
            val color = when {
                isFuture -> accentColor.copy(alpha = 0.12f)
                total > 0 -> accentColor.copy(alpha = 0.7f)
                else -> accentColor.copy(alpha = 0.15f)
            }

            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(3.dp.toPx()),
            )

            val paint = Paint().apply {
                this.color = surfaceVariant.toArgb()
                textSize = 7.sp.toPx()
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                month.getDisplayName(TextStyle.NARROW, Locale.FRENCH),
                left + barWidth / 2f,
                size.height - 2.dp.toPx(),
                paint
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// KAN-139 : Records personnels
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun PersonalRecordsCard(
    bestDay: Pair<Long, LocalDate?>,
    bestWeek: Pair<Long, LocalDate?>,
    bestMonth: Pair<Long, LocalDate?>,
    accentColor: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RecordRow(
                emoji = "🏆",
                label = "Meilleur jour",
                value = formatSteps(bestDay.first),
                subtitle = bestDay.second?.format(SHORT_DATE_FMT),
                accentColor = accentColor,
            )
            RecordRow(
                emoji = "📅",
                label = "Meilleure semaine",
                value = formatSteps(bestWeek.first),
                subtitle = bestWeek.second?.let { "sem. du ${it.format(SHORT_DATE_FMT)}" },
                accentColor = accentColor,
            )
            RecordRow(
                emoji = "🗓️",
                label = "Meilleur mois",
                value = formatSteps(bestMonth.first),
                subtitle = bestMonth.second?.format(MONTH_YEAR_FMT),
                accentColor = accentColor,
            )
        }
    }
}

@Composable
private fun RecordRow(
    emoji: String,
    label: String,
    value: String,
    subtitle: String?,
    accentColor: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, style = MaterialTheme.typography.titleSmall)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = accentColor,
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// KAN-139 : Plus longue série
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun LongestStreakCard(
    longestStreak: Pair<Int, LocalDate?>,
    accentColor: Color,
) {
    val (days, startDate) = longestStreak

    // Animation d'entrée : la flamme bondit depuis 0.4 jusqu'à 1.0 (spring, comme iOS)
    val scaleAnim = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.45f, stiffness = 300f),
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9F1A).copy(alpha = 0.08f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Flamme volontairement grande — récompense la plus symbolique de l'app (KAN-140)
            Text(
                text = "🔥",
                fontSize = 80.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer(
                    scaleX = scaleAnim.value,
                    scaleY = scaleAnim.value,
                ),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (days == 0) "—" else "$days",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF9F1A),
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (days <= 1) "jour consécutif" else "jours consécutifs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (startDate != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "depuis le ${startDate.format(SHORT_DATE_FMT)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
// ──────────────────────────────────────────────────────────────────────────────
// KAN-139 : Total cumulé + tour du monde
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun CumulativeTotalsCard(
    totalSteps: Long,
    totalKm: Double,
    accentColor: Color,
) {
    val worldTourPct = (totalKm / EARTH_CIRCUMFERENCE_KM * 100).coerceIn(0.0, 100.0)
    val animatedPct by animateFloatAsState(
        targetValue = worldTourPct.toFloat() / 100f,
        animationSpec = tween(800),
        label = "world_tour_pct"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column {
                    Text(
                        text = formatSteps(totalSteps),
                        style = MaterialTheme.typography.headlineSmall,
                        color = accentColor,
                    )
                    Text(
                        text = "pas au total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column {
                    Text(
                        text = "%.1f km".format(totalKm),
                        style = MaterialTheme.typography.headlineSmall,
                        color = accentColor,
                    )
                    Text(
                        text = "distance parcourue",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Barre de progression tour du monde
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🌍 Tour du monde",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "%.2f %%".format(worldTourPct),
                        style = MaterialTheme.typography.bodySmall,
                        color = accentColor,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                ) {
                    drawRoundRect(
                        color = accentColor.copy(alpha = 0.15f),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                    )
                    drawRoundRect(
                        color = accentColor,
                        size = Size(size.width * animatedPct, size.height),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "%.0f km restants pour faire le tour".format((EARTH_CIRCUMFERENCE_KM - totalKm).coerceAtLeast(0.0)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Composants partagés
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun MetricChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Formate un nombre de pas en français avec séparateur d'espace. */
private fun formatSteps(steps: Long): String =
    "%,d".format(steps).replace(',', ' ')
