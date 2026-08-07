# Podomètre Android — CLAUDE.md

## Contexte projet

Portage Android de l'application iOS Podomètre.
App de suivi de pas quotidiens — Kotlin / Jetpack Compose.
Projet personnel à but de portfolio et storytelling LinkedIn ("build in public").
Développement incrémental solo, sans dépendances tierces inutiles.

---

## Stack

- **Langage** : Kotlin 2.0+
- **UI** : Jetpack Compose + Material 3
- **Données santé** : Health Connect API (`androidx.health.connect`) — `StepsRecord`, `DistanceRecord`, `ExerciseSessionRecord`
- **Capteur live** : `SensorManager` + `TYPE_STEP_COUNTER` (pas en temps réel, aujourd'hui uniquement)
- **Notifications** : NotificationManager + WorkManager
- **Localisation** : FusedLocationProviderClient (Google Play Services)
- **Météo** : Open-Meteo API (REST, gratuit, sans clé)
- **Persistance** : DataStore Preferences + JSON fichier local (progressMap)
- **DI** : Hilt
- **Navigation** : Navigation Compose
- **Cible minimum** : Android 8.0 (API 26) — Health Connect requiert Android 9+ (API 28)
- **Cible SDK** : API 35

---

## Architecture

**MVVM** — pattern standard Android moderne.

- `ViewModel` + `StateFlow` / `SharedFlow` (pas de `LiveData`)
- `@HiltViewModel` pour tous les ViewModels
- Repositories injectés via Hilt
- `collectAsStateWithLifecycle()` dans les Composables (pas `collectAsState()`)

### Modules de code

```
app/src/main/java/com/fviret/podometre/
├── ui/
│   ├── activity/          ← Écran Activité (anneau, météo, calendrier, graphe, métriques, détail météo)
│   ├── aphorism/          ← AphorismCard, AphorismPopup
│   ├── journey/           ← Catalogue trajets, preview, détail
│   ├── settings/          ← Paramètres, badges, streak
│   ├── onboarding/        ← Flux d'onboarding 4 slides
│   └── theme/             ← MaterialTheme, couleurs, typographie
├── data/
│   ├── health/            ← HealthConnectRepository
│   ├── aphorism/          ← AphorismRepository (400 citations CC0, assets JSON)
│   ├── journey/           ← JourneyProgressRepository (JSON local)
│   ├── weather/           ← WeatherRepository (Open-Meteo)
│   └── preferences/       ← UserPreferencesRepository (DataStore)
├── domain/
│   ├── model/             ← Journey, Milestone, JourneyProgress, Badge
│   └── JourneyData.kt     ← Les 19 trajets définis comme constantes
├── di/                    ← Modules Hilt
└── worker/                ← WorkManager workers (sync steps, journey progress)
```

### Services partagés

| Service | Rôle | Équivalent iOS |
|---|---|---|
| `HealthConnectRepository` | Pas, distance, streak, idempotent | `StepCountViewModel` / `JourneyProgressService` |
| `JourneyProgressRepository` | Progression trajets, JSON, jalons | `JourneyProgressService` |
| `WeatherRepository` | Open-Meteo, cache 30min | `WeatherService` |
| `UserPreferencesRepository` | DataStore Preferences | `UserDefaults` / `@AppStorage` |
| `AphorismRepository` | 400 citations CC0, sélection déterministe par jour | — |

---

## Fonctionnalités implémentées

### Onboarding
- Carrousel 4 slides, non-dismissable
- Permission Health Connect (READ_STEPS, READ_DISTANCE) sur slide 3
- Sélection objectif sur slide 4
- Persisté via `hasCompletedOnboarding` dans DataStore

### Écran Activité
- Anneau circulaire Canvas Compose (épaisseur 20dp, dégradé + effet creusé sur la piste)
- Compteur animé (ease-out, 600ms, chiffres tabulaires `tnum`)
- Pourcentage d'objectif sous le compteur (non plafonné — peut dépasser 100 %)
- Halo de célébration (dégradé radial + spring animation) quand objectif atteint
- Série 🔥 sous le compteur quand objectif atteint aujourd'hui (fade-in + scale)
- Haptic feedback au franchissement de l'objectif
- Capteur live `TYPE_STEP_COUNTER` (baseline HK + delta capteur, today only)
- Navigation par chevrons (ghost slot pattern pour maintenir le centrage)
- Bannière météo + prévisions 7 jours (Open-Meteo) avec en-tête de section
- Tap sur un jour → bottom sheet détail météo (`WeatherDetailBottomSheet`) : héros, tranches 3h (00h–02h … 21h–23h), filtre tranches passées pour aujourd'hui
- Calendrier mensuel (grille L-D, lundi en premier)
- Graphe comparaison semaines (Canvas Compose, sans bibliothèque externe)
- Rangée métriques du jour (`TodayMetricsView`) : Distance, Temps actif (ExerciseSessionRecord + fallback), Calories — masquable via toggle Paramètres
- Popup pensée du jour (1×/jour, réarmable)

### Système de Trajets
- 19 trajets, 4 catégories : Promenades, Sentiers, Histoire, Mythes & Épopées
- Progression via `DistanceRecord` depuis `startDate` (requête idempotente)
- Jalons débloqués → notification locale
- Completion → badge + notification

### Paramètres / Badges / Streak
- Objectif : picker 5 000–20 000 pas (pas de 500)
- Couleur anneau : 6 presets (green, blue, orange, red, purple, teal)
- Mode sombre via DataStore
- Streak : calculé Health Connect, 365 jours max
- Badges de pas : 5k, 10k, 20k, 30k, 50k, 100k
- Badges de trajets : 19 badges (un par trajet complété)
- Toggle affichage rangée métriques du jour (`showTodayMetrics`)

### Pensée du jour
- Popup matinale à la première ouverture (garde 1×/jour dans DataStore, réarmable)
- 400 citations CC0 embarquées (`assets/aphorisms_humor_400.json`)
- Sélection déterministe : `recueil[(quantième - 1) % 400]`
- Carte dans les Paramètres (appui = copie presse-papiers)
- Toggle dans les Paramètres (réinitialise la garde à la réactivation)

---

## DataStore — clés en production

| Clé | Type | Valeur par défaut |
|---|---|---|
| `dailyStepGoal` | Int | 10 000 |
| `ringColorId` | String | "green" |
| `notificationsEnabled` | Boolean | true |
| `journeyNotificationsEnabled` | Boolean | true |
| `goalNotifiedDate` | Long (timestamp ms) | 0 |
| `isDarkMode` | Boolean | false |
| `completedJourneyIds` | Set\<String\> | vide |
| `hasCompletedOnboarding` | Boolean | false |
| `showWeatherForecast` | Boolean | true |
| `showMonthCalendar` | Boolean | true |
| `showWeeklyChart` | Boolean | true |
| `cachedStepsToday` | Long | 0 |
| `cachedStepsTodayDate` | String (date ISO yyyy-MM-dd) | "" (vide) |
| `activeJourneyId` | String? (UUID) | null |
| `showTodayMetrics` | Boolean | true |
| `aphorismEnabled` | Boolean | true |
| `lastAphorismDate` | String (date ISO yyyy-MM-dd) | "" (vide) |
| `sectionOrder` | String (CSV d'IDs) | "metrics,weather,calendar,chart" |

Ne pas créer de nouvelles clés sans les ajouter ici.

La `journeyProgressMap` est persistée en JSON dans un fichier dédié (`journey_progress.json`) car trop volumineuse pour DataStore.

L'historique de pas capteur (`SensorStepHistoryRepository`, KAN-156) est persisté en JSON dans un fichier dédié (`sensor_step_history.json`) : `{ stepsByDay: Map<date ISO, Long>, lastCaptureDate, lastRawCounterValue }`. Il s'agit de données capteur brutes (pas Health Connect) servant de fallback local quand Health Connect ne retourne aucune donnée (OEM comme Honor/Huawei/Xiaomi qui n'écrivent pas dans Health Connect) — alimenté quotidiennement par `SyncSensorStepHistoryWorker`.

---

## Conventions

- **Nommage** : anglais pour le code, commentaires en français si nécessaire
- **Pas de force-cast** (`as!` ou `!!`) — utiliser `?.let`, `?: return`, `checkNotNull`
- **Pas de dépendances inutiles** — pas de Room (HK suffit), pas de Retrofit si OkHttp suffit
- **Émulateur** : toujours ajouter `if (isEmulator())` avec des données mock réalistes
- **Health Connect** : ne jamais stocker les données HK localement — toujours lire depuis la source
- **Requêtes HK idempotentes** : calculer depuis `startDate`, ne jamais incrémenter

### Documentation des fonctions

Toute fonction ou propriété non triviale doit être documentée avec un commentaire `/**` en français.

```kotlin
/**
 * Retourne le pourcentage de progression du trajet (0.0 à 1.0).
 * Plafonné à 1.0 si la distance totale dépasse [Journey.totalKm].
 */
fun Journey.progressPercent(progress: JourneyProgress): Double =
    (progress.totalKm / totalKm).coerceIn(0.0, 1.0)
```

### Pattern ghost slot (chevrons)

```kotlin
// Maintenir l'espace d'un élément conditionnel pour stabiliser le layout
if (showNextButton) {
    IconButton(onClick = { ... }) { Icon(...) }
} else {
    Spacer(modifier = Modifier.size(48.dp))
}
```

### Requête HK idempotente

```kotlin
// Recalculer depuis startDate, ne jamais incrémenter
val km = healthConnectRepository.readDistance(from = progress.startDate)
if (km <= progress.totalKm) return
progress = progress.copy(totalKm = km)
```

### Pattern capteur live (SensorManager + HK baseline)

Le capteur `TYPE_STEP_COUNTER` fournit un compteur croissant depuis le reboot — pas les pas du jour.
La baseline HK est capturée au chargement, le delta capteur s'y ajoute en temps réel.

```kotlin
// Fonction pure testable (top-level internal)
internal fun computeLiveSteps(hcBaseline: Long, sensorStart: Long, sensorCurrent: Long): Long {
    if (sensorStart < 0L) return hcBaseline          // capteur non démarré
    val delta = (sensorCurrent - sensorStart).coerceAtLeast(0L)  // protect contre reboot
    return hcBaseline + delta
}

// Dans onSensorChanged — max() uniquement pour offset == 0
val live = computeLiveSteps(hcStepsRef, sensorStart, raw)
_uiState.value = _uiState.value.copy(stepsToday = maxOf(_uiState.value.stepsToday, live))
```

- `sensorStart = -1L` est la sentinelle "capteur non initialisé"
- `@Volatile` sur `hcStepsRef` et `sensorStart` (lecture cross-thread)
- `max()` uniquement pour `offset == 0` — pour les jours passés, affectation directe
- `ACTIVITY_RECOGNITION` requis Android 10+ (API 29) — vérifier avant d'enregistrer le listener

### Communication entre ViewModels

Préférer les Flows partagés via le Repository plutôt que `EventBus` ou `BroadcastReceiver`.

```kotlin
// Dans JourneyProgressRepository
val journeyCompleted: SharedFlow<UUID> = _journeyCompleted.asSharedFlow()

// Dans SettingsViewModel
viewModelScope.launch {
    journeyProgressRepository.journeyCompleted.collect { journeyId ->
        markJourneyCompleted(journeyId)
    }
}
```

### Pattern `GrantPermissionRule` dans les tests instrumentés

Quand un écran demande une permission système au démarrage (`ACTIVITY_RECOGNITION`, etc.),
le dialog système détruit la hiérarchie Compose dans les tests.
Pré-accorder la permission via `RuleChain` avant que `composeTestRule` s'initialise :

```kotlin
private val composeTestRule = createComposeRule()

@get:Rule
val ruleChain: RuleChain = RuleChain
    .outerRule(GrantPermissionRule.grant(Manifest.permission.ACTIVITY_RECOGNITION))
    .around(composeTestRule)
```

Dépendance requise : `androidTestImplementation("androidx.test:rules:1.5.0")`.

---

## Constantes de conversion

```kotlin
const val KM_PER_STEP = 0.0008  // 1 pas = 0,8 m = 0,0008 km
```

---

## Couleurs de l'anneau

```kotlin
object AppColors {
    val ringColorOptions = mapOf(
        "green"  to Color(0xFF33C759),
        "blue"   to Color(0xFF3399F2),
        "orange" to Color(0xFFFF9F1A),
        "red"    to Color(0xFFF23F4C),
        "purple" to Color(0xFFA64CF2),
        "teal"   to Color(0xFF26CCBF)
    )
    val defaultRingColorId = "green"
}
```

---

## Workflow Git — collaboration Humain / IA

`main` est toujours stable et déployable. Tout le travail passe par des branches.
**L'IA ne pousse jamais directement sur `main` ni sur `dev`.**

### Structure des branches

```
main          ← stable, protégé, mergé uniquement par toi
└── dev       ← branche d'intégration continue
    └── feature/<nom>   ← une branche par feature (créée par l'IA)
    └── fix/<nom>       ← une branche par bugfix (créée par l'IA)
    └── refactor/<nom>  ← une branche par refactoring (créée par l'IA)
```

### Cycle de travail

```bash
# 1. L'IA crée la branche depuis dev
git checkout dev && git pull origin dev
git checkout -b feature/<nom>

# 2. L'IA développe et committe
git add <fichiers>
git commit -m "feat: message" --trailer "Co-Authored-By: Claude <noreply@anthropic.com>"

# 3. L'IA pousse et ouvre une PR feature/<nom> → dev
git push origin feature/<nom>
gh pr create --base dev --title "..." --body "..."

# 4. Toi : tu reviews et merges dans dev

# 5. Toi : quand dev est stable, merge dev → main
git checkout main && git merge --no-ff dev -m "Merge dev"
git push origin main
```

### Règles

- L'IA crée toujours une branche depuis `dev`, jamais depuis `main`
- L'IA ouvre une PR vers `dev` — elle ne merge jamais elle-même
- Un PR = une feature ou un fix
- Merger avec `--no-ff` pour garder une trace claire
- Tous les commits de l'IA sont signés `Co-Authored-By: Claude`

---

## Permissions Android (AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />  <!-- API 29+ capteur pas -->
<!-- Health Connect -->
<uses-permission android:name="android.permission.health.READ_STEPS" />
<uses-permission android:name="android.permission.health.READ_DISTANCE" />
<uses-permission android:name="android.permission.health.READ_EXERCISE" />
```

---

## Ce qu'il ne faut pas faire

- Ne pas utiliser `LiveData` — utiliser `StateFlow` / `SharedFlow`
- Ne pas utiliser `@Observable` Compose (pas `@Composable` dans les ViewModels)
- Ne pas stocker les données Health Connect localement
- Ne pas utiliser `collectAsState()` — utiliser `collectAsStateWithLifecycle()`
- Ne pas casser la navigation par chevrons avec des limites arbitraires
- Ne pas créer de clé DataStore sans la documenter dans la table ci-dessus
- Ne pas utiliser Room — Health Connect est la source de vérité pour les données santé
- Ne pas utiliser de bibliothèque tierce pour les graphes — Canvas Compose uniquement
- Ne pas appliquer `max()` sur les pas pour les jours passés (offset ≠ 0) — affectation directe uniquement

---

## Roadmap / Tickets Jira

Projet Jira : **KAN** (Podomètre Android) — floviret.atlassian.net

| Sprint | Épic | Statut |
|---|---|---|
| Sprint 1 | KAN-4 Fondations & KAN-5 Onboarding | ✅ Terminé |
| Sprint 2 | KAN-6 Écran Activité (US 3.1–3.3) | ✅ Terminé |
| Sprint 3 | KAN-6 Écran Activité (US 3.4–3.7) | ✅ Terminé |
| Sprint 4 | KAN-7 Trajets (US 4.1–4.3) | ✅ Terminé |
| Sprint 5 | KAN-7 Trajets (US 4.4–4.6) | ✅ Terminé |
| Sprint 6 | KAN-8 Paramètres + Badges + Streak | ✅ Terminé |
| Sprint 7 | KAN-9 Catalogue 19 trajets | ✅ Terminé |
| Sprint 8 | KAN-10 Accessibilité + KAN-11 Tests | ✅ Terminé |
| Sprint 9 | KAN-60–68 Pensée du jour (aphorismes) | ✅ Terminé |
| Sprint 10 | KAN-71–78 Polish UI (haptic, live sensor, typo, anneau) | ✅ Terminé |
| Sprint 11 | KAN-79–86 Polish UI II (halo, métriques, détail météo, tests) | ✅ Terminé |
| Sprint 12 | KAN-89–96 Polish III (badges, streak animé, densif. trajets, a11y, haptic, À propos) | ✅ Terminé |

---

*Mis à jour le 21/07/2026*
