package com.fviret.podometre.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fviret.podometre.data.weather.DailyForecast
import com.fviret.podometre.data.weather.emojiForWeatherCode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Bandeau de prévisions météo sur 7 jours.
 * La cellule du jour courant est mise en évidence avec un fond [MaterialTheme.colorScheme.surfaceVariant].
 * Le nom de la ville est affiché en bas si non null.
 * Invisible si [forecasts] est vide.
 * Équivalent iOS : WeeklyForecastBannerView.swift
 */
@Composable
fun WeeklyForecastBanner(
    forecasts: List<DailyForecast>,
    cityName: String?,
    modifier: Modifier = Modifier,
) {
    if (forecasts.isEmpty()) return

    val today = LocalDate.now()

    Column(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(forecasts) { index, forecast ->
                DayCell(
                    forecast = forecast,
                    dayLabel = dayLabel(forecast.date, today),
                    isToday = index == 0,
                    accessibilityLabel = accessibilityLabel(forecast, today),
                )
            }
        }

        cityName?.let { city ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = city,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Cellule d'un jour de prévision. */
@Composable
private fun DayCell(
    forecast: DailyForecast,
    dayLabel: String,
    isToday: Boolean,
    accessibilityLabel: String,
) {
    val bgColor = if (isToday) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
    val labelColor = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(52.dp)
            .background(color = bgColor, shape = RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .clearAndSetSemantics { contentDescription = accessibilityLabel }
    ) {
        Text(
            text = dayLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = labelColor,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = emojiForWeatherCode(forecast.weatherCode),
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${forecast.tempMaxCelsius.roundToInt()}°",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "${forecast.tempMinCelsius.roundToInt()}°",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (forecast.precipitationMm > DailyForecast.PRECIP_THRESHOLD_MM) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${forecast.precipitationMm.roundToInt()}mm",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Label court du jour : "Auj." pour aujourd'hui, "Dem." pour demain,
 * puis abréviation du jour de la semaine en français ("Lun.", "Mar.", etc.).
 */
private fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Auj."
    today.plusDays(1) -> "Dem."
    else -> date.format(DateTimeFormatter.ofPattern("EEE", Locale.FRENCH))
        .replaceFirstChar { it.uppercaseChar() }
}

/**
 * Construit le contentDescription TalkBack pour une cellule de prévision.
 * Ex. : "Mardi 1 juillet, pluie, 18° max, 12° min, 4mm de pluie"
 */
private fun accessibilityLabel(forecast: DailyForecast, today: LocalDate): String {
    val dayName = when (forecast.date) {
        today -> "Aujourd'hui"
        today.plusDays(1) -> "Demain"
        else -> forecast.date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH))
    }
    val emoji = emojiForWeatherCode(forecast.weatherCode)
    val max = forecast.tempMaxCelsius.roundToInt()
    val min = forecast.tempMinCelsius.roundToInt()
    val precipPart = if (forecast.precipitationMm > DailyForecast.PRECIP_THRESHOLD_MM)
        ", ${forecast.precipitationMm.roundToInt()}mm de pluie"
    else ""
    return "$dayName, $emoji, ${max}° max, ${min}° min$precipPart"
}
