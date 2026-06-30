package com.fviret.podometre.ui.activity

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Placeholder — sera remplacé par l'écran Activité complet (KAN-19 à KAN-25). */
@Composable
fun ActivityScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Écran Activité\n(à venir — KAN-19 à KAN-25)",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
