package com.fviret.podometre.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_HEADERS = listOf("L", "M", "M", "J", "V", "S", "D")
private val MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH)

/**
 * Grille calendrier mensuel avec indicateurs de progression journalière.
 * 3 états par cellule : objectif atteint (cercle plein), progression partielle (cercle vide coloré),
 * aucun pas (cercle gris). Les jours futurs sont à 30% d'opacité.
 * Tap sur un jour passé déclenche [onDayTap].
 * Équivalent iOS : MonthCalendarView.swift
 */
@Composable
fun MonthCalendarView(
    month: YearMonth,
    stepsPerDay: Map<LocalDate, Long>,
    goal: Int,
    total: Long,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayTap: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val currentMonth = YearMonth.now()
    val canGoNext = month < currentMonth

    Column(modifier = modifier.fillMaxWidth()) {
        // En-tête : chevron gauche — nom du mois — chevron droit (ghost si mois courant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onPreviousMonth, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Mois précédent",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = month.format(MONTH_FORMATTER).replaceFirstChar { it.uppercaseChar() },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            if (canGoNext) {
                IconButton(onClick = onNextMonth, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Mois suivant",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(40.dp))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Ligne d'en-têtes des jours : L M M J V S D
        Row(modifier = Modifier.fillMaxWidth()) {
            DAY_HEADERS.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Grille des jours
        val firstDay = month.atDay(1)
        val startOffset = firstDay.dayOfWeek.value - 1 // Lundi = 0, Dimanche = 6
        val daysInMonth = month.lengthOfMonth()
        val totalCells = startOffset + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - startOffset + 1
                    if (dayNumber < 1 || dayNumber > daysInMonth) {
                        Box(modifier = Modifier.weight(1f))
                    } else {
                        val date = month.atDay(dayNumber)
                        val steps = stepsPerDay[date] ?: 0L
                        val isFuture = date.isAfter(today)
                        val isToday = date == today
                        CalendarDayCell(
                            day = dayNumber,
                            date = date,
                            steps = steps,
                            goal = goal,
                            isFuture = isFuture,
                            isToday = isToday,
                            onTap = if (!isFuture) onDayTap else null,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Total mensuel
        Text(
            text = "Total : ${"%,d".format(total).replace(',', ' ')} pas",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Cellule d'un jour du calendrier. */
@Composable
private fun CalendarDayCell(
    day: Int,
    date: LocalDate,
    steps: Long,
    goal: Int,
    isFuture: Boolean,
    isToday: Boolean,
    onTap: ((LocalDate) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val ringColor = MaterialTheme.colorScheme.primary
    val a11yLabel = buildA11yLabel(date, steps, goal, isFuture)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .alpha(if (isFuture) 0.3f else 1f)
            .then(
                if (onTap != null) Modifier.clickable { onTap(date) } else Modifier
            )
            .clearAndSetSemantics { contentDescription = a11yLabel },
    ) {
        when {
            isFuture || steps == 0L -> {
                // Cercle gris vide
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape,
                        )
                )
            }
            steps >= goal -> {
                // Cercle plein (objectif atteint)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(ringColor)
                )
            }
            else -> {
                // Cercle vide coloré (progression partielle)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .border(width = 2.dp, color = ringColor, shape = CircleShape)
                )
            }
        }

        // Numéro du jour
        val textColor = when {
            isFuture || steps == 0L -> MaterialTheme.colorScheme.onSurface
            steps >= goal -> Color.White
            else -> ringColor
        }
        Text(
            text = day.toString(),
            fontSize = 10.sp,
            color = textColor,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
        )
    }
}

private fun buildA11yLabel(date: LocalDate, steps: Long, goal: Int, isFuture: Boolean): String {
    val dayStr = date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH))
    return when {
        isFuture -> "$dayStr, jour à venir"
        steps == 0L -> "$dayStr, aucun pas"
        steps >= goal -> "$dayStr, objectif atteint, $steps pas"
        else -> "$dayStr, $steps pas sur $goal"
    }
}
