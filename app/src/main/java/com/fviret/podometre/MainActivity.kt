package com.fviret.podometre

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fviret.podometre.ui.PodoMetreApp
import com.fviret.podometre.ui.theme.PodoMetreTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activité principale et unique de l'application.
 * Délègue tout le contenu à [PodoMetreApp] via Jetpack Compose.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PodoMetreTheme {
                PodoMetreApp()
            }
        }
    }
}
