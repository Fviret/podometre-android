package com.fviret.podometre.ui.activity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import com.fviret.podometre.ui.theme.rememberReduceMotion
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fviret.podometre.R
import com.fviret.podometre.util.formatSteps
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Alpha de l'ombre interne de la piste (effet creusé). */
private const val TRACK_INNER_SHADOW_ALPHA = 0.12f

/** Durée d'apparition du badge de célébration (ms). */
private const val CELEBRATION_APPEAR_MS = 300

/** Durée de maintien du badge visible avant dissolution (ms). */
private const val CELEBRATION_HOLD_MS = 1200

/** Durée de dissolution du badge (ms). */
private const val CELEBRATION_DISSOLVE_MS = 400

/** Nombre de particules dans le burst de célébration. */
private const val CELEBRATION_PARTICLE_COUNT = 12

/** Distance maximale des particules depuis le centre de l'anneau (dp). */
private const val CELEBRATION_PARTICLE_MAX_DIST_DP = 80

/**
 * Overlay ponctuel de célébration affiché une seule fois au franchissement de l'objectif.
 * Affiche un badge "Objectif atteint 🎉" avec un burst de 12 particules colorées.
 * En mode "Réduire les animations", seul le badge est affiché (pas de particules ni de scale).
 * L'overlay est purement décoratif pour TalkBack — exclu de l'arbre A11y.
 *
 * @param show     Vrai une seule fois quand l'objectif vient d'être franchi.
 * @param onDone   Appelé à la fin de l'animation pour réinitialiser l'état parent.
 */
@Composable
private fun CelebrationOverlay(
    show: Boolean,
    ringColor: Color,
    reduceMotion: Boolean,
    onDone: () -> Unit,
) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.6f) }
    val particleProgress = remember { Animatable(0f) }

    LaunchedEffect(show) {
        if (!show) return@LaunchedEffect
        // Apparition parallèle : fondu + scale spring + particules
        launch { alpha.animateTo(1f, tween(if (reduceMotion) 200 else CELEBRATION_APPEAR_MS)) }
        if (!reduceMotion) {
            launch {
                scale.animateTo(
                    1f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                )
            }
            launch { particleProgress.animateTo(1f, tween(400, easing = FastOutSlowInEasing)) }
        } else {
            scale.snapTo(1f)
        }
        delay(if (reduceMotion) 800L else CELEBRATION_HOLD_MS.toLong())
        // Dissolution
        launch { scale.animateTo(0.8f, tween(if (reduceMotion) 200 else CELEBRATION_DISSOLVE_MS)) }
        alpha.animateTo(0f, tween(if (reduceMotion) 200 else CELEBRATION_DISSOLVE_MS))
        // Remise à zéro pour un éventuel déclenchement futur
        scale.snapTo(0.6f)
        particleProgress.snapTo(0f)
        onDone()
    }

    if (alpha.value > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clearAndSetSemantics { /* décoratif — exclu de l'arbre A11y */ },
            contentAlignment = Alignment.Center
        ) {
            // Burst de particules (désactivé si "Réduire les animations")
            if (!reduceMotion && particleProgress.value > 0f) {
                val pProgress = particleProgress.value
                val pAlpha = alpha.value * (1f - pProgress * 0.5f)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val particleRadiusPx = 5.dp.toPx()
                    val maxDistPx = CELEBRATION_PARTICLE_MAX_DIST_DP.dp.toPx()
                    for (i in 0 until CELEBRATION_PARTICLE_COUNT) {
                        val angle = Math.toRadians((i * 360.0 / CELEBRATION_PARTICLE_COUNT) - 90.0)
                        val distance = maxDistPx * pProgress
                        val x = center.x + (cos(angle) * distance).toFloat()
                        val y = center.y + (sin(angle) * distance).toFloat()
                        // Varier légèrement la taille pour plus de naturel
                        val radius = if (i % 3 == 0) particleRadiusPx * 1.4f else particleRadiusPx
                        drawCircle(
                            color = ringColor.copy(alpha = pAlpha),
                            radius = radius,
                            center = Offset(x, y),
                        )
                    }
                }
            }
            // Badge "Objectif atteint 🎉"
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        this.alpha = alpha.value
                    }
                    .background(ringColor.copy(alpha = 0.92f), shape = RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.activity_goal_reached_badge),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                )
            }
        }
    }
}

/**
 * Retourne la couleur de la flamme et du texte streak selon le palier de jours.
 * - 1–6 jours  : orange
 * - 7–29 jours : rouge
 * - 30+ jours  : violet
 */
internal fun streakColor(streakDays: Int): Color = when {
    streakDays >= 30 -> Color(0xFFA64CF2) // violet
    streakDays >= 7  -> Color(0xFFF23F4C) // rouge
    else             -> Color(0xFFFF9F1A) // orange
}

private const val RING_STROKE_WIDTH_DP = 20
private const val RING_ANIMATION_DURATION_MS = 600

/** Alpha maximal du halo de célébration (objectif atteint). */
private const val HALO_ALPHA = 0.30f

/** Scale initial du halo avant le pulse d'apparition (0.9 → 1.05). */
private const val HALO_SCALE_START = 0.9f

/** Scale final du halo après le pulse d'apparition. */
private const val HALO_SCALE_END = 1.05f

/**
 * Durée de l'animation du compteur de pas.
 * Compose honore automatiquement le réglage système "Réduire les animations"
 * (ANIMATOR_DURATION_SCALE = 0 → rendu instantané sans code supplémentaire).
 */
private const val COUNTER_ANIMATION_DURATION_MS = 600

/**
 * Anneau circulaire de progression des pas, avec compteur centré, objectif en dessous,
 * et — quand l'objectif du jour est atteint — la série 🔥 en fade-in.
 *
 * @param streak Nombre de jours consécutifs avec l'objectif atteint (0 = pas de série).
 * @param isToday Vrai uniquement si le jour affiché est aujourd'hui (offset == 0).
 *               Par défaut à false pour éviter un affichage fantôme si le paramètre est omis.
 * @param showCelebration Vrai une seule fois au moment du franchissement de l'objectif.
 *                        Déclenche l'overlay ponctuel (badge + particules). Doit être remis à
 *                        false par le parent via [onCelebrationEnd] dès la fin de l'animation.
 * @param onCelebrationEnd Appelé quand l'animation de célébration est terminée.
 */
@Composable
fun StepRing(
    steps: Long,
    goal: Int,
    ringColor: Color,
    modifier: Modifier = Modifier,
    streak: Int = 0,
    isToday: Boolean = false,
    showCelebration: Boolean = false,
    onCelebrationEnd: () -> Unit = {},
) {
    val reduceMotion = rememberReduceMotion()
    val rawProgress = if (goal > 0) steps.toFloat() / goal.toFloat() else 0f
    val progress = rawProgress.coerceIn(0f, 1f)
    /**
     * Compteur animé : monte progressivement vers la nouvelle valeur (ease-out, 0,6 s).
     * L'affichage des intermédiaires (ex. 5 423,7 → 5 423) crée l'effet "rouleau".
     * Si "Réduire les animations" est actif, [snap] est utilisé pour un affichage instantané.
     */
    val animatedSteps by animateFloatAsState(
        targetValue = steps.toFloat(),
        animationSpec = if (reduceMotion) snap()
        else tween(durationMillis = COUNTER_ANIMATION_DURATION_MS, easing = LinearOutSlowInEasing),
        label = "step_counter"
    )
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = if (reduceMotion) snap()
        else tween(durationMillis = RING_ANIMATION_DURATION_MS, easing = FastOutSlowInEasing),
        label = "step_ring_progress"
    )

    // Couleurs de la piste calculées ici (pas dans Canvas) pour accéder à MaterialTheme.
    val trackColorTop = MaterialTheme.colorScheme.surfaceVariant
    val trackColorBottom = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val percent = (rawProgress * 100).roundToInt()

    // ── Halo de célébration ──────────────────────────────────────────────────
    // Visible uniquement aujourd'hui, quand les pas atteignent l'objectif.
    val goalReached = isToday && steps >= goal

    /**
     * Déclenche l'animation spring (scale 0.9 → 1.05) au premier affichage
     * lorsque l'objectif vient d'être atteint.
     * Compose honore ANIMATOR_DURATION_SCALE = 0 : le spring se termine
     * instantanément si "Réduire les animations" est actif.
     */
    var haloTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(goalReached) {
        haloTriggered = goalReached
    }
    val haloScale by animateFloatAsState(
        targetValue = if (haloTriggered) HALO_SCALE_END else HALO_SCALE_START,
        animationSpec = if (reduceMotion) snap()
        else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "halo_scale"
    )
    // ────────────────────────────────────────────────────────────────────────

    val showStreak = isToday && steps >= goal && streak > 0
    val streakA11y = if (showStreak) {
        if (streak <= 1) stringResource(R.string.activity_ring_streak_accessibility, streak)
        else stringResource(R.string.activity_ring_streak_accessibility_plural, streak)
    } else ""
    val accessibilityLabel = buildString {
        append(stringResource(R.string.activity_ring_accessibility_label, steps.formatSteps(), goal.formatSteps(), percent))
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
            // ── Halo de célébration (élément purement décoratif) ─────────────
            // Dessiné DERRIÈRE l'anneau via un dégradé radial : couleur opaque
            // au centre → transparent au bord. Exclure de l'accessibilité.
            if (goalReached) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .size(240.dp)
                        .graphicsLayer {
                            scaleX = haloScale
                            scaleY = haloScale
                        }
                        .clearAndSetSemantics { /* décoratif — exclu de l'arbre A11y */ }
                ) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ringColor.copy(alpha = HALO_ALPHA),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension / 2f
                        )
                    )
                }
            }
            // ────────────────────────────────────────────────────────────────

            // ── Overlay de célébration (badge + particules, one-shot) ────────
            CelebrationOverlay(
                show = showCelebration,
                ringColor = ringColor,
                reduceMotion = reduceMotion,
                onDone = onCelebrationEnd,
            )
            // ────────────────────────────────────────────────────────────────

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
                    text = animatedSteps.toLong().formatSteps(),
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
                    enter = if (reduceMotion) fadeIn(snap())
                    else fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.85f),
                ) {
                    val color = streakColor(streak)
                    // Animation de pulsation douce de la flamme (scale 0.9 → 1.1, ~1.2s).
                    // Si "Réduire les animations" est actif, la flamme reste à sa taille naturelle.
                    val infiniteTransition = rememberInfiniteTransition(label = "streak_pulse")
                    val flameScaleAnimated by infiniteTransition.animateFloat(
                        initialValue = 0.9f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "streak_scale"
                    )
                    val flameScale = if (reduceMotion) 1f else flameScaleAnimated
                    val streakDaysLabel = if (streak <= 1)
                        stringResource(R.string.activity_ring_streak_label_days, streak)
                    else
                        stringResource(R.string.activity_ring_streak_label_days_plural, streak)
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    ) {
                        Text(
                            text = "🔥",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.graphicsLayer {
                                scaleX = flameScale
                                scaleY = flameScale
                            },
                        )
                        Text(
                            text = " $streakDaysLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                // Pourcentage d'objectif — masqué si anneau vide, exclu de l'A11y car
                // déjà présent dans le contentDescription du Box parent.
                if (percent > 0) {
                    Text(
                        text = stringResource(R.string.activity_ring_percent_label, percent),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }
                /**
                 * Message motivant affiché uniquement quand aucun pas n'a encore été compté
                 * pour le jour sélectionné. Disparaît dès qu'un premier pas est enregistré.
                 * Exclu de l'arbre d'accessibilité car le contentDescription du Box parent
                 * communique déjà l'état "0 pas".
                 */
                if (steps == 0L) {
                    Text(
                        text = stringResource(R.string.activity_ring_empty_state),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clearAndSetSemantics { },
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.activity_ring_goal_label, goal.formatSteps()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
