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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fviret.podometre.R
import com.fviret.podometre.data.weather.DailyForecast
import com.fviret.podometre.data.weather.HourlyForecast
import com.fviret.podometre.data.weather.emojiForWeatherCode
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Bottom sheet Material 3 affichant le détail météo d'un jour sélectionné.
 * Affiche une section héros (icône, condition, max/min, précipitations)
 * puis les créneaux groupés par tranches de 3 heures.
 * Les tranches passées sont filtrées quand le jour sélectionné est aujourd'hui.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDetailBottomSheet(
    forecast: DailyForecast,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val isToday = forecast.date == LocalDate.now()
    val todayLabel = stringResource(R.string.weather_detail_today)
    val tomorrowLabel = stringResource(R.string.weather_detail_tomorrow)

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
            // Titre : "Aujourd'hui", "Demain", ou "Mardi 22 juillet"
            Text(
                text = formatDayTitle(forecast.date, todayLabel, tomorrowLabel),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Section héros
            WeatherHeroSection(forecast = forecast)

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            val slots = hourlyBands(forecast.hourlyForecasts, filterPast = isToday)

            if (slots.isEmpty()) {
                Text(
                    text = stringResource(R.string.weather_detail_no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    items(slots) { band ->
                        HourlyBandRow(band = band)
                    }
                }
            }
        }
    }
}

/**
 * Section héros affichant : grande icône météo, libellé de condition, max/min,
 * et total précipitations si > [DailyForecast.PRECIP_THRESHOLD_MM].
 */
@Composable
private fun WeatherHeroSection(forecast: DailyForecast) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Grande icône météo (48sp)
        Text(
            text = emojiForWeatherCode(forecast.weatherCode),
            fontSize = 48.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Libellé condition météo
        Text(
            text = labelForWeatherCode(forecast.weatherCode),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Max / Min
        Text(
            text = "${forecast.tempMaxCelsius.roundToInt()}° / ${forecast.tempMinCelsius.roundToInt()}°",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        // Précipitations si significatives
        if (forecast.precipitationMm > DailyForecast.PRECIP_THRESHOLD_MM) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.weather_detail_precip_mm,
                    "%.1f".format(forecast.precipitationMm),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Modèle d'une tranche horaire groupée (3 heures).
 * Ex. tranche 06h–08h : température moyenne + weatherCode représentatif.
 */
internal data class HourlyBand(
    /** Heure de début de la tranche (ex. 6 pour 06h–08h). */
    val startHour: Int,
    /** Heure de fin de la tranche (ex. 8 pour 06h–08h). */
    val endHour: Int,
    /** Température moyenne des créneaux de la tranche. */
    val avgTempCelsius: Double,
    /** Code WMO le plus représentatif (le plus fréquent, ou le premier). */
    val weatherCode: Int,
    /** Probabilité de précipitation maximale dans la tranche. */
    val maxPrecipProbability: Int,
)

/**
 * Ligne compacte pour une tranche horaire groupée.
 * Format : plage horaire | emoji météo | température moyenne | proba pluie (si > 20 %)
 */
@Composable
private fun HourlyBandRow(band: HourlyBand) {
    val precipText = if (band.maxPrecipProbability > 20) "💧 ${band.maxPrecipProbability}%" else ""
    val rangeLabel = "%02dh–%02dh".format(band.startHour, band.endHour)
    val a11y = buildString {
        append("$rangeLabel, ")
        append(emojiForWeatherCode(band.weatherCode))
        append(", ${band.avgTempCelsius.roundToInt()}°")
        if (band.maxPrecipProbability > 20) append(", ${band.maxPrecipProbability}% de précipitation")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .semantics { contentDescription = a11y },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Plage horaire
        Text(
            text = rangeLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        // Emoji météo
        Text(
            text = emojiForWeatherCode(band.weatherCode),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.width(12.dp))
        // Température moyenne
        Text(
            text = "${band.avgTempCelsius.roundToInt()}°",
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
 * Tranches horaires fixes : 00h–02h, 03h–05h, … 21h–23h (8 tranches de 3 heures).
 * Chaque tranche regroupe les créneaux horaires dont l'heure tombe dans [start, start+2].
 * Si [filterPast] est vrai (aujourd'hui), les tranches dont la fin est déjà passée sont omises.
 */
private val HOURLY_BANDS = listOf(0, 3, 6, 9, 12, 15, 18, 21)

internal fun hourlyBands(
    hourly: List<HourlyForecast>,
    filterPast: Boolean,
): List<HourlyBand> {
    if (hourly.isEmpty()) return emptyList()
    val nowHour = if (filterPast) LocalTime.now().hour else -1

    return HOURLY_BANDS.mapNotNull { start ->
        val end = start + 2
        // Filtrer les tranches passées pour aujourd'hui : fin de tranche <= heure actuelle
        if (filterPast && end < nowHour) return@mapNotNull null

        val slots = hourly.filter { it.hour in start..end }
        if (slots.isEmpty()) return@mapNotNull null

        val avgTemp = slots.map { it.tempCelsius }.average()
        // Code le plus représentatif : le plus fréquent (ou le premier en cas d'égalité)
        val representativeCode = slots
            .groupingBy { it.weatherCode }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: slots.first().weatherCode
        val maxPrecip = slots.maxOf { it.precipProbability }

        HourlyBand(
            startHour = start,
            endHour = end,
            avgTempCelsius = avgTemp,
            weatherCode = representativeCode,
            maxPrecipProbability = maxPrecip,
        )
    }
}

/**
 * Retourne le libellé français d'une condition météo à partir d'un code WMO Open-Meteo.
 * Couvre les codes principaux définis par la spec Open-Meteo.
 */
internal fun labelForWeatherCode(code: Int): String = when {
    code == 0 -> "Ensoleillé"
    code == 1 -> "Principalement ensoleillé"
    code == 2 -> "Partiellement nuageux"
    code == 3 -> "Couvert"
    code in 45..48 -> "Brouillard"
    code in 51..55 -> "Bruine"
    code in 61..65 -> "Pluie"
    code in 66..67 -> "Pluie verglaçante"
    code in 71..75 -> "Neige"
    code == 77 -> "Grésil"
    code in 80..82 -> "Averses"
    code in 85..86 -> "Averses de neige"
    code == 95 -> "Orages"
    code in 96..99 -> "Orages avec grêle"
    else -> "Conditions variables"
}

/**
 * Formate la date pour le titre du bottom sheet.
 * Ex. : "Mardi 22 juillet" ou la valeur de [todayLabel] / [tomorrowLabel].
 * Les libellés localisés sont résolus par le Composable appelant via [stringResource].
 */
private fun formatDayTitle(date: LocalDate, todayLabel: String, tomorrowLabel: String): String {
    val today = LocalDate.now()
    return when (date) {
        today -> todayLabel
        today.plusDays(1) -> tomorrowLabel
        else -> date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault()))
            .replaceFirstChar { it.uppercaseChar() }
    }
}
