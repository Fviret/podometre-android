package com.fviret.podometre.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.fviret.podometre.R
import com.fviret.podometre.data.weather.WeatherState

/**
 * Bannière discrète indiquant l'état des précipitations.
 * Fond bleu léger si pluie en cours ou imminente ; fond transparent si pas de pluie.
 * Invisible si [state] est null (permission localisation absente ou données indisponibles).
 * Équivalent iOS : WeatherBannerView.swift
 */
@Composable
fun WeatherBanner(
    state: WeatherState?,
    modifier: Modifier = Modifier,
) {
    if (state == null) return

    val bgColor = when (state) {
        WeatherState.RAIN_NOW, WeatherState.RAIN_SOON -> MaterialTheme.colorScheme.primaryContainer
        WeatherState.NO_RAIN -> Color.Transparent
    }

    val label = when (state) {
        WeatherState.RAIN_NOW -> stringResource(R.string.weather_rain_now)
        WeatherState.RAIN_SOON -> stringResource(R.string.weather_rain_soon)
        WeatherState.NO_RAIN -> stringResource(R.string.weather_no_rain)
    }

    val iconTint = when (state) {
        WeatherState.RAIN_NOW, WeatherState.RAIN_SOON -> MaterialTheme.colorScheme.primary
        WeatherState.NO_RAIN -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = bgColor, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clearAndSetSemantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.WaterDrop,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = when (state) {
                WeatherState.RAIN_NOW, WeatherState.RAIN_SOON -> MaterialTheme.colorScheme.onPrimaryContainer
                WeatherState.NO_RAIN -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
