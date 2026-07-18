package com.fviret.podometre.ui.activity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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

/** Alpha de l'ombre interne de la piste (effet creusé). */
private const val TRACK_INNER_SHADOW_ALPHA = 0.12f

private const val RING_STROKE_WIDTH_DP = 20
private const val RING_ANIMATION_DURATION_MS = 600

/**
 * Durée de l'animation du compteur de pas.
 * Compose honore automatiquement le réglage système "Réduire les animations"
 * (ANIMATOR_DURATION_SCALE = 0 → rendu instantané sans code supplémentaire).
 */
private const val COUNTER_ANIMATION_DURATION_MS = 600

/**
 * Anneau circulaire de progression des pas, avec compteur centré et objectif en dessous.
 * Le remplissage est animé (600ms, easeInOut) et plafonné à 100% même si [steps] dépasse [goal].
 * Équivalent iOS : StepRingView.swift (partie anneau).
 */
/**
 * Anneau circulaire de progression des pas, avec compteur centré, objectif en dessous,
 * et — quand l'objectif du jour est atteint — la série 🔥 en fade-in.
 *
 * @param streak Nombre de jours consécutifs avec l'objectif atteint (0 = pas de série).
 * @param isToday Vrai uniquement si le jour affiché est aujourd'hui (offset == 0).
 */
@Composable
fun StepRing(
    steps: Long,
    goal: Int,
    ringColor: Color,
    modifier: Modifier = Modifier,
    streak: Int = 0,
    isToday: Boolean = true,
) {
    val rawProgress = if (goal > 0) steps.toFloat() / goal.toFloat() else 0f
    val progress = rawProgress.coerceIn(0f, 1f)
    /**
     * Compteur animé : monte progressivement vers la nouvelle valeur (ease-out, 0,6 s).
     * L'affichage des intermédiaires (ex. 5 423,7 → 5 423) crée l'effet "rouleau".
     * Compose respecte ANIMATOR_DURATION_SCALE : si "Réduire les animations" est actif,
     * la durée passe à 0 et le chiffre s'affiche directement.
     */
    val animatedSteps by animateFloatAsState(
        targetValue = steps.toFloat(),
        animationSpec = tween(durationMillis = COUNTER_ANIMATION_DURATION_MS, easing = LinearOutSlowInEasing),
        label = "step_counter"
    )
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = RING_ANIMATION_DURATION_MS, easing = FastOutSlowInEasing),
        label = "step_ring_progress"
    )

    // Couleurs de la piste calculées ici (pas dans Canvas) pour accéder à MaterialTheme.
    val trackColorTop = MaterialTheme.colorScheme.surfaceVariant
    val trackColorBottom = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val percent = (progress * 100).roundToInt()
    val showStreak = isToday && steps >= goal && streak > 0
    val streakA11y = if (showStreak) {
        if (streak <= 1) stringResource(R.string.activity_ring_streak_accessibility, streak)
        else stringResource(R.string.activity_ring_streak_accessibility_plural, streak)
    } else ""
    val accessibilityLabel = buildString {
        append(stringResource(R.string.activity_ring_accessibility_label, steps, goal, percent))
        if (streakA11y.isNotEmpty()) append(". $streakA11y")
    }

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

                // Piste de fond — dégradé vertical (haut clair → bas légèrement plus foncé)
                drawArc(
                    brush = Brush.verticalGradient(
                        colors = listOf(trackColorTop, trackColorBottom),
                        startY = topLeft.y,
                        endY = topLeft.y + arcSize.height,
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
                // Ombre interne — couche sombre translucide en bas pour l'effet creusé
                drawArc(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = TRACK_INNER_SHADOW_ALPHA),
                        ),
                        startY = topLeft.y,
                        endY = topLeft.y + arcSize.height,
                    ),
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
                    text = "%,d".format(animatedSteps.toLong()).replace(',', ' '),
                    // "tnum" = chiffres tabulaires (largeur fixe) → pas de jitter pendant l'animation.
                    style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.activity_ring_steps_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AnimatedVisibility(
                    visible = showStreak,
                    enter = fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.85f),
                ) {
                    val streakLabel = if (streak <= 1)
                        stringResource(R.string.activity_ring_streak_label, streak)
                    else
                        stringResource(R.string.activity_ring_streak_label_plural, streak)
                    Text(
                        text = streakLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
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
