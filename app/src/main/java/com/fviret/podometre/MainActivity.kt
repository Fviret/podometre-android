package com.fviret.podometre

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fviret.podometre.ui.PodoMetreApp
import com.fviret.podometre.ui.theme.PodoMetreTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activité principale et unique de l'application.
 * Délègue tout le contenu à [PodoMetreApp] via Jetpack Compose.
 * Observe [MainViewModel.isDarkMode] pour appliquer le thème immédiatement.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by mainViewModel.isDarkMode.collectAsStateWithLifecycle()
            PodoMetreTheme(darkTheme = isDarkMode) {
                PodoMetreApp()
            }
        }
    }
}
