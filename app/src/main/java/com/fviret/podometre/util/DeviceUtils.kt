package com.fviret.podometre.util

import android.os.Build

/**
 * Détecte si l'app tourne sur un émulateur Android.
 * Utilisé pour injecter des données mock réalistes (Health Connect indisponible/vide sur AVD).
 */
fun isEmulator(): Boolean =
    Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.startsWith("unknown") ||
        Build.MODEL.contains("google_sdk") ||
        Build.MODEL.contains("Emulator") ||
        Build.MODEL.contains("Android SDK built for x86") ||
        Build.MANUFACTURER.contains("Genymotion") ||
        Build.PRODUCT.contains("sdk_gphone") ||
        Build.PRODUCT.contains("emulator") ||
        Build.HARDWARE.contains("ranchu") ||
        Build.HARDWARE.contains("goldfish")
