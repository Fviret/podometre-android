package com.fviret.podometre.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Placeholder — sera remplacé par l'écran Paramètres complet (KAN-32 à KAN-39). */
@Composable
fun SettingsScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Paramètres\n(à venir — KAN-32 à KAN-39)",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
