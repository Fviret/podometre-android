package com.fviret.podometre.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Données du récapitulatif hebdomadaire.
 * Couvre la semaine en cours (lundi → aujourd'hui) et la semaine précédente.
 */
data class WeeklyRecapData(
    /** Pas totaux cette semaine (lundi → aujourd'hui). */
    val currentWeekSteps: Long,
    /** Pas totaux la semaine précédente (lundi → dimanche). */
    val previousWeekSteps: Long,
    /** Distance (km) cette semaine. */
    val currentWeekDistanceKm: Double,
    /** Calories (kcal) cette semaine. */
    val currentWeekCalories: Int,
    /** Temps actif (minutes) cette semaine. */
    val currentWeekActiveMinutes: Int,
    /** Nombre de jours avec objectif atteint cette semaine. */
    val daysGoalMet: Int,
    /** Objectif quotidien de pas. */
    val dailyGoal: Int,
    /** Pas par jour cette semaine (index 0=lundi…6=dimanche, null si jour futur). */
    val stepsPerDay: List<Long?>,
    /** Lundi de la semaine en cours. */
    val weekStart: LocalDate,
    /** Titre du trajet actif, null si aucun. */
    val activeJourneyTitle: String? = null,
    /** Progression km du trajet actif, null si aucun trajet. */
    val activeJourneyProgressKm: Double? = null,
    /** Km total du trajet actif, null si aucun trajet. */
    val activeJourneyTotalKm: Double? = null,
)

/**
 * Bottom sheet de récapitulatif hebdomadaire — affiché le lundi matin (1×/semaine).
 *
 * Structure :
 * - En-tête : rangée de 7 pastilles jour (L à D), colorées selon objectif atteint
 * - 4 lignes métriques : Pas, Distance, Temps actif, Calories
 * - Ligne trajet actif (si présent) sur fond accentué — tap → onglet Trajets
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyRecapSheet(
    data: WeeklyRecapData,
    accentColor: Color,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit,
    onNavigateToJourneys: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            // Titre
            Text(
                text = "Récapitulatif de la semaine",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                text = "${data.daysGoalMet}/7 jours avec l'objectif atteint",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp),
            )

            // Rangée 7 pastilles (L M M J V S D)
            DayPillRow(
                weekStart = data.weekStart,
                stepsPerDay = data.stepsPerDay,
                dailyGoal = data.dailyGoal,
                accentColor = accentColor,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 4 lignes métriques
            MetricRow(
                label = "Pas",
                current = formatSteps(data.currentWeekSteps),
                previous = formatSteps(data.previousWeekSteps),
                trend = trendSign(data.currentWeekSteps.toDouble(), data.previousWeekSteps.toDouble()),
                showTrend = true,
                accentColor = accentColor,
            )
            Spacer(modifier = Modifier.height(12.dp))
            MetricRow(
                label = "Distance",
                current = "%.1f km".format(data.currentWeekDistanceKm),
                previous = null,
                trend = null,
                showTrend = false,
                accentColor = accentColor,
            )
            Spacer(modifier = Modifier.height(12.dp))
            MetricRow(
                label = "Temps actif",
                current = formatMinutes(data.currentWeekActiveMinutes),
                previous = null,
                trend = null,
                showTrend = false,
                accentColor = accentColor,
            )
            Spacer(modifier = Modifier.height(12.dp))
            MetricRow(
                label = "Calories",
                current = "${data.currentWeekCalories} kcal",
                previous = null,
                trend = null,
                showTrend = false,
                accentColor = accentColor,
            )

            // Ligne trajet actif (optionnelle)
            if (data.activeJourneyTitle != null && data.activeJourneyProgressKm != null && data.activeJourneyTotalKm != null) {
                Spacer(modifier = Modifier.height(20.dp))
                ActiveJourneyRow(
                    title = data.activeJourneyTitle,
                    progressKm = data.activeJourneyProgressKm,
                    totalKm = data.activeJourneyTotalKm,
                    accentColor = accentColor,
                    onClick = {
                        onDismiss()
                        onNavigateToJourneys()
                    },
                )
            }
        }
    }
}

// ── Composables internes ──────────────────────────────────────────────────────

/**
 * Rangée de 7 pastilles (lundi → dimanche) colorées selon objectif atteint.
 */
@Composable
private fun DayPillRow(
    weekStart: LocalDate,
    stepsPerDay: List<Long?>,
    dailyGoal: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val today = LocalDate.now()
        for (i in 0..6) {
            val date = weekStart.plusDays(i.toLong())
            val steps = stepsPerDay.getOrNull(i)
            val isFuture = date.isAfter(today)
            val goalMet = !isFuture && (steps ?: 0L) >= dailyGoal

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                goalMet -> accentColor
                                isFuture -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (goalMet) "✓" else "",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/**
 * Une ligne de métrique avec label, valeur courante, et flèche de tendance optionnelle.
 */
@Composable
private fun MetricRow(
    label: String,
    current: String,
    previous: String?,
    trend: String?,
    showTrend: Boolean,
    accentColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showTrend && trend != null) {
                Text(
                    text = trend,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (trend == "↑") accentColor else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
            Text(
                text = current,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * Ligne trajet actif — fond accentué, tap → ferme la sheet et bascule sur l'onglet Trajets.
 */
@Composable
private fun ActiveJourneyRow(
    title: String,
    progressKm: Double,
    totalKm: Double,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Trajet en cours",
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "%.1f / %.0f km".format(progressKm, totalKm),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Fonctions utilitaires ─────────────────────────────────────────────────────

private fun formatSteps(steps: Long): String =
    if (steps >= 1_000) "%.1fk".format(steps / 1_000.0) else steps.toString()

private fun formatMinutes(minutes: Int): String =
    if (minutes >= 60) "${minutes / 60}h${(minutes % 60).toString().padStart(2, '0')}" else "${minutes}min"

/** Retourne "↑" si current > previous, "↓" sinon (jamais neutre pour simplifier l'UI). */
private fun trendSign(current: Double, previous: Double): String =
    if (current >= previous) "↑" else "↓"
