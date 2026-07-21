package com.fviret.podometre.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fviret.podometre.data.weather.DailyForecast
import com.fviret.podometre.data.weather.HourlyForecast
import com.fviret.podometre.data.weather.emojiForWeatherCode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Bottom sheet Material 3 affichant le détail météo horaire d'un jour sélectionné.
 * Affiche les créneaux de 06h à 23h par tranches de 3 heures.
 * Fermeture par swipe natif ou en rappelant [onDismiss].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDetailBottomSheet(
    forecast: DailyForecast,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            // Titre : "Mardi 22 juillet"
            Text(
                text = formatDayTitle(forecast.date),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            val slots = hourlySlots(forecast.hourlyForecasts)

            if (slots.isEmpty()) {
                Text(
                    text = "Détail horaire non disponible",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    items(slots) { slot ->
                        HourlySlotRow(slot = slot)
                    }
                }
            }
        }
    }
}

/**
 * Ligne compacte pour un créneau horaire.
 * Format : heure | emoji météo | température | proba pluie (si > 20 %)
 */
@Composable
private fun HourlySlotRow(slot: HourlyForecast) {
    val precipText = if (slot.precipProbability > 20) "💧 ${slot.precipProbability}%" else ""
    val a11y = buildString {
        append("${slot.hour}h, ")
        append(emojiForWeatherCode(slot.weatherCode))
        append(", ${slot.tempCelsius.roundToInt()}°")
        if (slot.precipProbability > 20) append(", ${slot.precipProbability}% de précipitation")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .semantics { contentDescription = a11y },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Heure
        Text(
            text = "%02dh".format(slot.hour),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        // Emoji météo
        Text(
            text = emojiForWeatherCode(slot.weatherCode),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.width(12.dp))
        // Température
        Text(
            text = "${slot.tempCelsius.roundToInt()}°",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        // Proba pluie
        if (precipText.isNotEmpty()) {
            Text(
                text = precipText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * Filtre les créneaux horaires pour n'afficher que 06h–23h, par tranches de 3 heures.
 * Si la liste est vide, retourne une liste vide (le bottom sheet affichera un message).
 */
private fun hourlySlots(hourly: List<HourlyForecast>): List<HourlyForecast> {
    if (hourly.isEmpty()) return emptyList()
    return hourly.filter { it.hour in 6..23 && it.hour % 3 == 0 }
}

/**
 * Formate la date pour le titre du bottom sheet.
 * Ex. : "Mardi 22 juillet" ou "Aujourd'hui" / "Demain".
 */
private fun formatDayTitle(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Aujourd'hui"
        today.plusDays(1) -> "Demain"
        else -> date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH))
            .replaceFirstChar { it.uppercaseChar() }
    }
}
