package com.fviret.podometre.ui.activity

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

private val DAY_LABELS = listOf("Lu", "Ma", "Me", "Je", "Ve", "Sa", "Di")

/**
 * Graphe en courbes comparant la semaine courante et la semaine précédente.
 * Dessiné entièrement via Canvas Compose, sans bibliothèque externe.
 * [currentWeek] et [previousWeek] contiennent 7 valeurs (lundi → dimanche).
 * Les jours futurs de la semaine courante doivent être 0.
 * [todayIndex] indice du jour courant (0=lundi … 6=dimanche).
 * Équivalent iOS : WeeklyBarChartView.swift
 */
@Composable
fun WeeklyChartView(
    currentWeek: List<Long>,
    previousWeek: List<Long>,
    todayIndex: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val a11yLabel = buildA11yLabel(currentWeek, previousWeek, todayIndex)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clearAndSetSemantics { contentDescription = a11yLabel },
    ) {
        val padLeft = 48.dp.toPx()
        val padRight = 8.dp.toPx()
        val padTop = 16.dp.toPx()
        val padBottom = 32.dp.toPx()

        val chartW = size.width - padLeft - padRight
        val chartH = size.height - padTop - padBottom

        // ── Calcul de l'échelle Y ─────────────────────────────────────────────
        val allValues = (currentWeek + previousWeek).filter { it > 0 }
        val rawMax = allValues.maxOrNull()?.toDouble() ?: 10_000.0
        val maxY = ceilToMultiple(rawMax, 5_000.0).coerceAtLeast(5_000.0)
        val avgCurrent = if (currentWeek.any { it > 0 })
            currentWeek.filter { it > 0 }.average() else 0.0

        // ── Couleurs ──────────────────────────────────────────────────────────
        val prevColor = Color(0xFFAAAAAA)
        val axisColor = Color(0xFFDDDDDD)
        val avgColor = accentColor.copy(alpha = 0.5f)

        // ── Helpers ───────────────────────────────────────────────────────────
        fun xFor(dayIdx: Int): Float = padLeft + dayIdx * (chartW / 6f)
        fun yFor(value: Long): Float = padTop + (1f - value / maxY.toFloat()) * chartH

        // ── Axe Y — 4 graduations ─────────────────────────────────────────────
        val labelPaint = Paint().apply {
            color = Color(0xFF888888).toArgb()
            textSize = 10.sp.toPx()
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
                strokeWidth = 1.dp.toPx(),
            )
            val label = if (v >= 1_000) "${(v / 1_000).toInt()}k" else v.toInt().toString()
            drawContext.canvas.nativeCanvas.drawText(label, padLeft - 6.dp.toPx(), y + 4.dp.toPx(), labelPaint)
        }

        // ── Ligne de moyenne (pointillée) ─────────────────────────────────────
        if (avgCurrent > 0) {
            val avgY = yFor(avgCurrent.toLong())
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            drawLine(
                color = avgColor,
                start = Offset(padLeft, avgY),
                end = Offset(size.width - padRight, avgY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect,
            )
            val avgLabel = "moy. ${(avgCurrent / 1_000).toInt()}k"
            val avgPaint = Paint().apply {
                color = avgColor.toArgb()
                textSize = 9.sp.toPx()
                textAlign = Paint.Align.LEFT
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                avgLabel, padLeft + 4.dp.toPx(), avgY - 4.dp.toPx(), avgPaint
            )
        }

        // ── Courbe semaine précédente (grise, pointillée, 1.5dp) ─────────────
        val prevPath = Path()
        previousWeek.forEachIndexed { idx, v ->
            val x = xFor(idx)
            val y = yFor(v)
            if (idx == 0) prevPath.moveTo(x, y) else prevPath.lineTo(x, y)
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
        // ── Courbe semaine courante (accent, pleine, 2dp) ─────────────────────
        val currPath = Path()
        var started = false
        currentWeek.forEachIndexed { idx, v ->
            if (v <= 0L) return@forEachIndexed
            val x = xFor(idx)
            val y = yFor(v)
            if (!started) { currPath.moveTo(x, y); started = true } else currPath.lineTo(x, y)
        }
        drawPath(
            path = currPath,
            color = accentColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )

        // ── Labels jours en bas ───────────────────────────────────────────────
        val dayPaintNormal = Paint().apply {
            color = Color(0xFF888888).toArgb()
            textSize = 10.sp.toPx()
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val dayPaintToday = Paint().apply {
            color = accentColor.toArgb()
            textSize = 10.sp.toPx()
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        DAY_LABELS.forEachIndexed { idx, label ->
            val paint = if (idx == todayIndex) dayPaintToday else dayPaintNormal
            drawContext.canvas.nativeCanvas.drawText(
                label, xFor(idx), size.height - 6.dp.toPx(), paint
            )
        }

        // ── Cercles des points de données — dessinés en dernier (premier plan) ─
        previousWeek.forEachIndexed { idx, v ->
            drawCircle(color = prevColor, radius = 3.dp.toPx(), center = Offset(xFor(idx), yFor(v)))
        }
        currentWeek.forEachIndexed { idx, v ->
            if (v <= 0L) return@forEachIndexed
            drawCircle(color = accentColor, radius = 4.dp.toPx(), center = Offset(xFor(idx), yFor(v)))
            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(xFor(idx), yFor(v)))
        }
    }
}

/** Arrondit [value] au multiple de [multiple] supérieur. */
private fun ceilToMultiple(value: Double, multiple: Double): Double =
    ceil(value / multiple) * multiple

/**
 * Construit le label TalkBack résumant le graphe.
 * Exemple : "Cette semaine : Lu 8 500, Ma 6 700, Me 7 430. Semaine précédente : Lu 11 200, …"
 */
private fun buildA11yLabel(
    current: List<Long>,
    previous: List<Long>,
    todayIndex: Int,
): String {
    val fullDays = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")
    fun formatWeek(values: List<Long>, limit: Int = values.size): String =
        values.take(limit + 1)
            .mapIndexed { i, v -> if (v > 0) "${fullDays[i]} ${"%,d".format(v).replace(',', ' ')}" else null }
            .filterNotNull()
            .joinToString(", ")

    val currStr = formatWeek(current, todayIndex)
    val prevStr = formatWeek(previous)
    return "Graphe hebdomadaire. Cette semaine : $currStr. Semaine précédente : $prevStr."
}
