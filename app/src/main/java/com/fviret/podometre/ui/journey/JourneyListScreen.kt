package com.fviret.podometre.ui.journey

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Placeholder — sera remplacé par le catalogue des 19 trajets (KAN-28 à KAN-31). */
@Composable
fun JourneyListScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Catalogue Trajets\n(à venir — KAN-28 à KAN-31)",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
