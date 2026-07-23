package com.fviret.podometre.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Retourne `true` si l'utilisateur a activé "Réduire les animations" au niveau système
 * (ANIMATOR_DURATION_SCALE == 0).
 *
 * À utiliser dans la couche UI (Composable uniquement) pour conditionner les specs
 * d'animation : remplacer [tween] ou [spring] par [snap] quand la valeur est `true`.
 *
 * Exemple :
 * ```kotlin
 * val reduceMotion = rememberReduceMotion()
 * val spec = if (reduceMotion) snap<Float>() else tween(600)
 * val value by animateFloatAsState(target, animationSpec = spec)
 * ```
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
}
