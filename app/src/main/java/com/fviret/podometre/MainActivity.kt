package com.fviret.podometre

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fviret.podometre.ui.PodoMetreApp
import com.fviret.podometre.ui.theme.PodoMetreTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activité principale et unique de l'application.
 * Délègue tout le contenu à [PodoMetreApp] via Jetpack Compose.
 * Observe [ThemeViewModel.isDarkMode] pour appliquer le thème immédiatement.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()

    /**
     * Lanceur de demande de permission POST_NOTIFICATIONS via le pattern ActivityResultContracts.
     * Si la permission est refusée, les notifications restent simplement inactives.
     */
    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            android.util.Log.d("MainActivity", "POST_NOTIFICATIONS refusée — notifications désactivées")
        }
    }

    /**
     * Demande la permission POST_NOTIFICATIONS au démarrage sur Android 13+ (API 33).
     * Non bloquant : si l'utilisateur refuse, les notifications sont simplement inactives.
     */
    private fun requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPostNotificationsIfNeeded()
        setContent {
            val isDarkMode by themeViewModel.isDarkMode.collectAsStateWithLifecycle()
            PodoMetreTheme(darkTheme = isDarkMode) {
                PodoMetreApp()
            }
        }
    }
}
