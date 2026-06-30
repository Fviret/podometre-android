package com.fviret.podometre.ui.activity

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fviret.podometre.R
import kotlin.math.roundToInt

private const val RING_STROKE_WIDTH_DP = 20
private const val RING_ANIMATION_DURATION_MS = 600

/**
 * Anneau circulaire de progression des pas, avec compteur centré et objectif en dessous.
 * Le remplissage est animé (600ms, easeInOut) et plafonné à 100% même si [steps] dépasse [goal].
 * Équivalent iOS : StepRingView.swift (partie anneau).
 */
@Composable
fun StepRing(
    steps: Long,
    goal: Int,
    ringColor: Color,
    modifier: Modifier = Modifier,
) {
    val rawProgress = if (goal > 0) steps.toFloat() / goal.toFloat() else 0f
    val progress = rawProgress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = RING_ANIMATION_DURATION_MS, easing = FastOutSlowInEasing),
        label = "step_ring_progress"
    )
    val percent = (progress * 100).roundToInt()
    val accessibilityLabel = stringResource(
        R.string.activity_ring_accessibility_label,
        steps,
        goal,
        percent
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .clearAndSetSemantics { contentDescription = accessibilityLabel },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().size(240.dp)) {
                val strokeWidthPx = RING_STROKE_WIDTH_DP.dp.toPx()
                val arcSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
                val topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)

                // Piste de fond
                drawArc(
                    color = ringColor.copy(alpha = 0.15f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )

                // Arc de progression — dégradé couleur à 70% d'opacité vers couleur pleine
                val sweepAngle = 360f * animatedProgress
                if (sweepAngle > 0f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(ringColor.copy(alpha = 0.7f), ringColor),
                            center = center
                        ),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "%,d".format(steps).replace(',', ' '),
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.activity_ring_steps_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = stringResource(R.string.activity_ring_goal_label, goal),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
