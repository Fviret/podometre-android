package com.fviret.podometre.ui.activity

import android.graphics.Paint
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fviret.podometre.R
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil

/**
 * Graphe en courbes comparant la semaine ISO courante et la semaine précédente.
 * Titre + légende en haut, dessin Canvas Compose pur, sans bibliothèque externe.
 * [currentWeek] et [previousWeek] : 7 valeurs (lundi → dimanche).
 * [dayLabels] : 7 étiquettes correspondant aux jours Lu … Di.
 * [todayIndex] : indice du jour courant dans la semaine ISO (0=lundi … 6=dimanche).
 * Équivalent iOS : WeeklyBarChartView.swift
 */
@Composable
fun WeeklyChartView(
    currentWeek: List<Long>,
    previousWeek: List<Long>,
    dayLabels: List<String>,
    todayIndex: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
) {
    val a11yLabel = buildA11yLabel(currentWeek, previousWeek, dayLabels)
    val prevColor = Color(0xFFAAAAAA)
    /** Vrai quand aucune donnée n'est disponible pour les deux semaines. */
    val allZero = currentWeek.all { it == 0L } && previousWeek.all { it == 0L }

    val tapModifier = if (onTap != null) Modifier.clickable(onClick = onTap) else Modifier
    Column(modifier = modifier.fillMaxWidth().then(tapModifier)) {
        // ── En-tête : titre + légende ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "7 derniers jours",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.weight(1f))
            // Légende sem. précédente
            Canvas(modifier = Modifier.size(8.dp)) {
                drawCircle(color = prevColor, radius = size.minDimension / 2f)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "sem. précédente",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(12.dp))
            // Légende moyenne
            Canvas(modifier = Modifier.size(width = 16.dp, height = 8.dp)) {
                drawLine(
                    color = accentColor.copy(alpha = 0.5f),
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f),
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "moyenne",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Canvas du graphe ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center,
        ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clearAndSetSemantics { contentDescription = a11yLabel },
        ) {
            val padLeft = 44.dp.toPx()
            val padRight = 8.dp.toPx()
            val padTop = 8.dp.toPx()
            val padBottom = 28.dp.toPx()

            val chartW = size.width - padLeft - padRight
            val chartH = size.height - padTop - padBottom

            // Échelle Y
            val allValues = (currentWeek + previousWeek).filter { it > 0 }
            val rawMax = allValues.maxOrNull()?.toDouble() ?: 10_000.0
            val maxY = ceilToMultiple(rawMax, 5_000.0).coerceAtLeast(5_000.0)
            val avgCurrent = if (currentWeek.any { it > 0 })
                currentWeek.filter { it > 0 }.average() else 0.0

            val axisColor = Color(0xFFDDDDDD)
            val avgColor = accentColor.copy(alpha = 0.5f)

            fun xFor(i: Int): Float = padLeft + i * (chartW / 6f)
            fun yFor(v: Long): Float = padTop + (1f - v / maxY.toFloat()) * chartH

            // ── Empty state : ligne pointillée centrale ───────────────────────
            if (allZero) {
                val midY = padTop + chartH * 0.4f
                drawLine(
                    color = Color(0xFF999999).copy(alpha = 0.3f),
                    start = Offset(padLeft, midY),
                    end = Offset(size.width - padRight, midY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f),
                )
            }

            // ── Graduations axe Y ─────────────────────────────────────────────
            val yLabelPaint = Paint().apply {
                color = Color(0xFF999999).toArgb()
                textSize = 9.sp.toPx()
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }
            for (i in 0..3) {
                val v = maxY * i / 3.0
                val y = padTop + (1f - (v / maxY).toFloat()) * chartH
                drawLine(
                    color = axisColor,
                    start = Offset(padLeft, y),
                    end = Offset(size.width - padRight, y),
                    strokeWidth = 0.8.dp.toPx(),
                )
                val label = if (v >= 1_000) "${(v / 1_000).toInt()}k" else v.toInt().toString()
                drawContext.canvas.nativeCanvas.drawText(
                    label, padLeft - 5.dp.toPx(), y + 3.5.dp.toPx(), yLabelPaint
                )
            }

            // ── Ligne de moyenne ──────────────────────────────────────────────
            if (avgCurrent > 0) {
                val avgY = yFor(avgCurrent.toLong())
                drawLine(
                    color = avgColor,
                    start = Offset(padLeft, avgY),
                    end = Offset(size.width - padRight, avgY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
                )
                val avgPaint = Paint().apply {
                    color = avgColor.toArgb()
                    textSize = 9.sp.toPx()
                    textAlign = Paint.Align.LEFT
                    isAntiAlias = true
                }
                val avgLabel = "moy. ${(avgCurrent / 1_000).toInt()}k"
                drawContext.canvas.nativeCanvas.drawText(
                    avgLabel, padLeft + 4.dp.toPx(), avgY - 4.dp.toPx(), avgPaint
                )
            }

            // ── Courbe semaine précédente ─────────────────────────────────────
            val prevPath = Path()
            previousWeek.forEachIndexed { idx, v ->
                if (idx == 0) prevPath.moveTo(xFor(idx), yFor(v))
                else prevPath.lineTo(xFor(idx), yFor(v))
            }
            drawPath(
                path = prevPath,
                color = prevColor,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f),
                ),
            )

            // ── Courbe semaine courante ───────────────────────────────────────
            val currPath = Path()
            var started = false
            currentWeek.forEachIndexed { idx, v ->
                if (v <= 0L) return@forEachIndexed
                if (!started) { currPath.moveTo(xFor(idx), yFor(v)); started = true }
                else currPath.lineTo(xFor(idx), yFor(v))
            }
            drawPath(
                path = currPath,
                color = accentColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            )

            // ── Labels jours en bas ───────────────────────────────────────────
            val dayNormal = Paint().apply {
                color = Color(0xFF999999).toArgb()
                textSize = 10.sp.toPx()
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val dayToday = Paint().apply {
                color = accentColor.toArgb()
                textSize = 10.sp.toPx()
                textAlign = Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }
            dayLabels.forEachIndexed { idx, label ->
                drawContext.canvas.nativeCanvas.drawText(
                    label, xFor(idx), size.height - 4.dp.toPx(),
                    if (idx == todayIndex) dayToday else dayNormal
                )
            }

            // ── Cercles — dessinés EN DERNIER pour rester au premier plan ─────
            // Semaine précédente : anneau gris avec fond blanc
            previousWeek.forEachIndexed { idx, v ->
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(xFor(idx), yFor(v)))
                drawCircle(
                    color = prevColor, radius = 5.dp.toPx(), center = Offset(xFor(idx), yFor(v)),
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
            // Semaine courante : disque accent avec point blanc au centre
            currentWeek.forEachIndexed { idx, v ->
                if (v <= 0L) return@forEachIndexed
                drawCircle(color = Color.White, radius = 7.dp.toPx(), center = Offset(xFor(idx), yFor(v)))
                drawCircle(color = accentColor, radius = 6.dp.toPx(), center = Offset(xFor(idx), yFor(v)))
                drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = Offset(xFor(idx), yFor(v)))
            }
        }

        // ── Label empty state superposé au Canvas ─────────────────────────────
        if (allZero) {
            Text(
                text = stringResource(R.string.weekly_chart_no_data),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
        } // fin Box

        // ── KAN-134 : Pastille de tendance ────────────────────────────────────
        val currentAvg = if (currentWeek.any { it > 0 })
            currentWeek.filter { it > 0 }.average() else 0.0
        val prevAvg = if (previousWeek.any { it > 0 })
            previousWeek.filter { it > 0 }.average() else 0.0

        if (currentAvg > 0 && prevAvg > 0) {
            val diff = currentAvg - prevAvg
            val (trendColor, trendArrow, trendLabel) = when {
                diff > 500  -> Triple(accentColor, "↑", "en hausse")
                diff < -500 -> Triple(Color(0xFFF23F4C), "↓", "en baisse")
                else        -> Triple(MaterialTheme.colorScheme.onSurfaceVariant, "—", "stable")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(trendColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = trendArrow,
                        style = MaterialTheme.typography.labelMedium,
                        color = trendColor,
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Moyenne $trendLabel vs sem. précédente",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun ceilToMultiple(value: Double, multiple: Double): Double =
    ceil(value / multiple) * multiple

private fun buildA11yLabel(current: List<Long>, previous: List<Long>, labels: List<String>): String {
    val currStr = current.mapIndexed { i, v ->
        if (v > 0) "${labels.getOrElse(i) { "" }} ${"%,d".format(v).replace(',', ' ')}" else null
    }.filterNotNull().joinToString(", ")
    val prevStr = previous.mapIndexed { i, v ->
        if (v > 0) "${labels.getOrElse(i) { "" }} ${"%,d".format(v).replace(',', ' ')}" else null
    }.filterNotNull().joinToString(", ")
    return "Graphe 7 derniers jours. Cette semaine : $currStr. Semaine précédente : $prevStr."
}
