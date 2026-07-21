package com.fviret.podometre.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fviret.podometre.R
import java.util.Locale

/**
 * Formate un nombre de minutes en texte lisible.
 * Exemples : 45 → "45 min", 74 → "1 h 14 min", 120 → "2 h".
 */
internal fun formatActiveTime(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}

/**
 * Rangée de 3 tuiles affichant les métriques du jour sélectionné :
 * Distance (km), Temps actif (min/h), Calories (kcal).
 * Insérée entre l'anneau et la bannière météo dans [ActivityScreen].
 *
 * @param ringColor Couleur de l'anneau — teinte les icônes de chaque tuile.
 * @param distanceKm Distance parcourue en kilomètres.
 * @param activeMinutes Temps actif estimé en minutes.
 * @param caloriesKcal Calories actives brûlées en kcal.
 * @param modifier Modificateur Compose standard.
 */
@Composable
fun TodayMetricsView(
    ringColor: Color,
    distanceKm: Double,
    activeMinutes: Int,
    caloriesKcal: Int,
    modifier: Modifier = Modifier,
) {
    val distanceFormatted = String.format(Locale.getDefault(), "%.1f", distanceKm)
    val activeTimeFormatted = formatActiveTime(activeMinutes)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MetricTile(
            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
            iconTint = ringColor,
            value = distanceFormatted,
            unit = stringResource(R.string.metrics_distance_unit),
            label = stringResource(R.string.metrics_distance_label),
            accessibilityDescription = stringResource(R.string.metrics_distance_accessibility, distanceFormatted),
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            icon = Icons.Default.Timer,
            iconTint = ringColor,
            value = activeTimeFormatted,
            unit = "",
            label = stringResource(R.string.metrics_active_time_label),
            accessibilityDescription = stringResource(R.string.metrics_active_time_accessibility, activeTimeFormatted),
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            icon = Icons.Default.LocalFireDepartment,
            iconTint = ringColor,
            value = caloriesKcal.toString(),
            unit = stringResource(R.string.metrics_calories_unit),
            label = stringResource(R.string.metrics_calories_label),
            accessibilityDescription = stringResource(R.string.metrics_calories_accessibility, caloriesKcal),
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Tuile individuelle d'une métrique : icône, valeur en évidence, libellé.
 * Fond arrondi [surfaceVariant], cornerRadius 12dp.
 *
 * @param icon Icône Material représentant la métrique.
 * @param iconTint Couleur de l'icône (correspond à la couleur de l'anneau).
 * @param value Valeur principale à afficher (ex. "3.2", "45 min", "180").
 * @param unit Unité affichée à côté de la valeur (ex. "km", "kcal"), ou vide si [value] contient déjà l'unité.
 * @param label Libellé descriptif sous la valeur.
 * @param accessibilityDescription Description vocalissée pour TalkBack.
 * @param modifier Modificateur Compose standard.
 */
@Composable
private fun MetricTile(
    icon: ImageVector,
    iconTint: Color,
    value: String,
    unit: String,
    label: String,
    accessibilityDescription: String,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = accessibilityDescription
        },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
            if (unit.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = " $unit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
