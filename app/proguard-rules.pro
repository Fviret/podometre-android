# Règles ProGuard/R8 pour Podomètre Android
# Appliquées uniquement en build release (isMinifyEnabled = true)

# Garder les informations de debug pour les stack traces symbolisées
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Hilt — garder les classes générées
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Health Connect — garder les modèles de données
-keep class androidx.health.connect.** { *; }

# WorkManager — garder les Workers
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class **$$serializer {
    kotlinx.serialization.descriptors.SerialDescriptor descriptor;
}
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# DataStore
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# Enums — garder les valeurs pour la sérialisation
-keepclassmembers enum * { public static **[] values(); public static ** valueOf(java.lang.String); }

# Modèles du domaine — garder les noms pour la sérialisation JSON
-keep class com.fviret.podometre.domain.model.** { *; }
