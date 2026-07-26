package com.fviret.podometre.ui.journey

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fviret.podometre.domain.model.Journey
import com.fviret.podometre.domain.model.JourneyProgress
import com.fviret.podometre.domain.model.Milestone
import com.fviret.podometre.domain.model.formatKm

/**
 * Résultat du calcul du segment courant d'un trajet.
 *
 * @param leftLabel   Libellé du bord gauche du segment (dernier jalon franchi ou "Départ").
 * @param rightLabel  Libellé du bord droit du segment (prochain jalon ou "Arrivée").
 * @param markerFraction Position du marqueur "tu es ici" dans le segment, entre 0.0 et 1.0.
 * @param distanceToNext Distance restante jusqu'au prochain jalon, en km (null si plus de jalon).
 */
internal data class SegmentInfo(
    val leftLabel: String,
    val rightLabel: String,
    val markerFraction: Float,
    val distanceToNext: Double?,
)

/**
 * Calcule les bornes et la position du marqueur pour le segment courant.
 *
 * Règles :
 * - Le bord gauche est le dernier jalon débloqué, ou "Départ" (km = 0) si aucun.
 * - Le bord droit est le premier jalon non débloqué, ou "Arrivée" (km = totalKm) si tous débloqués.
 * - La fraction est clampée entre 0.0 et 1.0.
 *
 * @param journey   Le trajet en cours.
 * @param progress  La progression de l'utilisateur.
 * @return [SegmentInfo] décrivant le segment courant, ou null si le trajet est terminé.
 */
internal fun computeSegmentInfo(journey: Journey, progress: JourneyProgress): SegmentInfo? {
    val totalKm = journey.totalKm
    val walkedKm = progress.totalKm

    // Trajet terminé : on ne dessine pas le tracé.
    if (walkedKm >= totalKm) return null

    val sorted = journey.milestones.sortedBy { it.km }

    // Dernier jalon débloqué (bord gauche du segment).
    val lastUnlocked: Milestone? = sorted.lastOrNull {
        it.id.toString() in progress.unlockedMilestoneIds
    }

    // Premier jalon non débloqué (bord droit du segment).
    val nextMilestone: Milestone? = sorted.firstOrNull {
        it.id.toString() !in progress.unlockedMilestoneIds
    }

    val leftKm = lastUnlocked?.km ?: 0.0
    val leftLabel = lastUnlocked?.label ?: "Départ"

    val rightKm = nextMilestone?.km ?: totalKm
    val rightLabel = nextMilestone?.label ?: "Arrivée"

    val segmentLength = rightKm - leftKm
    val markerFraction = if (segmentLength <= 0.0) 0f
    else ((walkedKm - leftKm) / segmentLength).coerceIn(0.0, 1.0).toFloat()

    val distanceToNext = nextMilestone?.let { (it.km - walkedKm).coerceAtLeast(0.0) }

    return SegmentInfo(
        leftLabel = leftLabel,
        rightLabel = rightLabel,
        markerFraction = markerFraction,
        distanceToNext = distanceToNext,
    )
}

/**
 * Tracé horizontal du segment courant d'un trajet (dernier jalon franchi → prochain jalon).
 *
 * - Bord gauche : dernier jalon débloqué (ou "Départ" si aucun).
 * - Bord droit  : prochain jalon (ou "Arrivée" si tous débloqués).
 * - Marqueur "tu es ici" positionné proportionnellement à la progression dans le segment.
 * - Masqué si [isCompleted] est vrai (trajet achevé à 100 %).
 *
 * Accessibilité : l'ensemble du tracé est un seul élément sémantique avec un libellé résumé.
 *
 * @param journey     Le trajet en cours.
 * @param progress    La progression de l'utilisateur (null → tracé masqué).
 * @param isCompleted Vrai si le trajet est terminé (tracé masqué).
 * @param modifier    Modificateur Compose standard.
 * @param trackHeight Épaisseur de la piste (défaut 6 dp).
 */
@Composable
fun JourneySegmentTrack(
    journey: Journey,
    progress: JourneyProgress?,
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 6.dp,
) {
    // Pas d'affichage si terminé ou pas encore démarré.
    if (isCompleted || progress == null) return

    val segmentInfo = computeSegmentInfo(journey, progress) ?: return

    // Libellé d'accessibilité résumant le segment courant.
    val a11yLabel = buildString {
        append("Segment en cours : de ${segmentInfo.leftLabel} à ${segmentInfo.rightLabel}.")
        segmentInfo.distanceToNext?.let {
            append(" Prochaine étape dans ${formatKm(it)}.")
        }
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary
    val markerColor = MaterialTheme.colorScheme.primary
    val markerBorderColor = MaterialTheme.colorScheme.surface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = false) { contentDescription = a11yLabel }
    ) {
        // ── Piste Canvas ──────────────────────────────────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight + 20.dp) // espace vertical pour le marqueur
        ) {
            val trackY = size.height / 2f
            val trackStartX = 0f
            val trackEndX = size.width
            val strokePx = trackHeight.toPx()
            val markerX = trackStartX + (trackEndX - trackStartX) * segmentInfo.markerFraction

            // Piste de fond (gris)
            drawLine(
                color = trackColor,
                start = Offset(trackStartX, trackY),
                end = Offset(trackEndX, trackY),
                strokeWidth = strokePx,
                cap = StrokeCap.Round,
            )

            // Portion déjà parcourue (couleur principale)
            if (markerX > trackStartX) {
                drawLine(
                    color = progressColor,
                    start = Offset(trackStartX, trackY),
                    end = Offset(markerX, trackY),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round,
                )
            }

            // Marqueur "tu es ici" : cercle avec bordure
            val markerRadius = strokePx * 1.6f
            drawCircle(
                color = markerBorderColor,
                radius = markerRadius + 2.dp.toPx(),
                center = Offset(markerX, trackY),
            )
            drawCircle(
                color = markerColor,
                radius = markerRadius,
                center = Offset(markerX, trackY),
            )
        }

        // ── Libellés gauche / droite ──────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = segmentInfo.leftLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp),
                maxLines = 1,
            )
            Text(
                text = segmentInfo.rightLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
                maxLines = 1,
            )
        }

        // Distance jusqu'au prochain jalon
        segmentInfo.distanceToNext?.let { dist ->
            Text(
                text = "encore ${formatKm(dist)} jusqu'à ${segmentInfo.rightLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
