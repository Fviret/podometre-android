# Podomètre Android

Portage Android de l'application iOS Podomètre — suivi de pas quotidiens avec système de trajets virtuels.

---

## Fonctionnalités

- **Anneau de progression** — visualisation circulaire des pas du jour par rapport à l'objectif
- **Météo** — bannière et prévisions 7 jours via Open-Meteo (sans clé API)
- **Calendrier mensuel** — historique des jours actifs
- **Graphe hebdomadaire** — comparaison des 2 dernières semaines
- **Trajets virtuels** — 19 trajets réels (Promenades, Sentiers, Histoire, Mythes & Épopées) progressant avec la distance parcourue
- **Jalons & badges** — notifications locales à chaque jalon débloqué, badges de complétion
- **Streak** — série de jours consécutifs actifs (jusqu'à 365 jours)
- **Paramètres** — objectif de pas (5 000–20 000), couleur de l'anneau, mode sombre, notifications

---

## Stack technique

| Couche | Technologie |
|---|---|
| Langage | Kotlin 2.0+ |
| UI | Jetpack Compose + Material 3 |
| Données santé | Health Connect API (`androidx.health.connect`) |
| Persistance | DataStore Preferences + JSON fichier local |
| DI | Hilt |
| Navigation | Navigation Compose |
| Réseau | OkHttp (Open-Meteo) |
| Background | WorkManager |
| Localisation | FusedLocationProviderClient |
| Tests | JUnit 5 + MockK + Coroutines Test |

**Minimum SDK :** Android 8.0 (API 26) — Health Connect requiert Android 9+ (API 28)  
**Target SDK :** API 35

---

## Architecture

Pattern **MVVM** avec Hilt pour l'injection de dépendances.

```
app/src/main/java/com/fviret/podometre/
├── ui/
│   ├── activity/        ← Écran Activité (anneau, météo, calendrier, graphe)
│   ├── journey/         ← Catalogue trajets, preview, détail
│   ├── settings/        ← Paramètres, badges, streak
│   ├── onboarding/      ← Flux d'onboarding 4 slides
│   └── theme/           ← MaterialTheme, couleurs, typographie
├── data/
│   ├── health/          ← HealthConnectRepository
│   ├── journey/         ← JourneyProgressRepository (JSON local)
│   ├── weather/         ← WeatherRepository (Open-Meteo)
│   └── preferences/     ← UserPreferencesRepository (DataStore)
├── domain/
│   ├── model/           ← Journey, Milestone, JourneyProgress, Badge
│   └── JourneyData.kt   ← Les 19 trajets définis comme constantes
├── di/                  ← Modules Hilt (AppModule, HealthConnectModule)
└── worker/              ← WorkManager workers (sync steps, journey progress)
```

### Principes clés

- `ViewModel` + `StateFlow` / `SharedFlow` — pas de `LiveData`
- `@HiltViewModel` sur tous les ViewModels
- `collectAsStateWithLifecycle()` dans les Composables
- Health Connect : lecture toujours depuis la source, jamais de stockage local
- Requêtes HK idempotentes : recalcul depuis `startDate`, jamais d'incrémentation

---

## Prérequis

- **Android Studio** Ladybug ou supérieur
- **JDK 17** (inclus dans Android Studio)
- **Android SDK** API 35
- Un émulateur ou appareil physique Android 9+ pour Health Connect

---

## Lancer le projet

```bash
# Cloner
git clone https://github.com/Fviret/podometre-android.git
cd podometre-android

# Builder
./gradlew assembleDebug

# Installer sur un émulateur/appareil connecté
./gradlew installDebug
```

> Sur Mac, le JDK d'Android Studio peut être utilisé si aucun JDK système n'est installé :
> ```bash
> export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
> ```

---

## Tests

```bash
# Tests unitaires (JVM)
./gradlew testDebugUnitTest

# Tests instrumentés (émulateur requis)
./gradlew connectedDebugAndroidTest

# Lint
./gradlew lintDebug
```

---

## CI/CD

GitHub Actions — `.github/workflows/ci.yml`

Déclenché sur chaque push vers `main` / `dev` et chaque PR vers `dev` :

1. **Lint** — `./gradlew lint`
2. **Tests unitaires** — `./gradlew testDebugUnitTest`
3. **Build** — `./gradlew assembleDebug`
4. **Upload APK** — artifact `debug-apk`

---

## Workflow Git

```
main          ← stable, protégé
└── dev       ← branche d'intégration
    └── feature/<ticket>   ← une branche par ticket Jira
    └── fix/<ticket>
```

Les PRs sont ouvertes vers `dev`. `main` n'est mis à jour que depuis `dev` avec `--no-ff`.

---

## Trajets disponibles (19)

| Catégorie | Exemples |
|---|---|
| 🚶 Promenades | Boucle du Lac d'Annecy (35 km), Tour du Mont-Blanc (170 km) |
| 🏔 Sentiers | GR20 Corse (180 km), Chemin de Stevenson (275 km) |
| 🏛 Histoire | Route de Napoléon (325 km), Chemin des Dames (40 km) |
| ⚔️ Mythes & Épopées | Via Francigena (1 900 km), Chemin de Compostelle (800 km) |

La progression est calculée depuis `DistanceRecord` Health Connect à partir de la date de démarrage du trajet — toujours recalculée, jamais incrémentée.

---

## Roadmap

| Sprint | Épic | Statut |
|---|---|---|
| 1 | KAN-4 Fondations & KAN-5 Onboarding | 🔄 En cours |
| 2 | KAN-6 Écran Activité (US 3.1–3.3) | ⏳ À venir |
| 3 | KAN-6 Écran Activité (US 3.4–3.7) | ⏳ À venir |
| 4 | KAN-7 Trajets (US 4.1–4.3) | ⏳ À venir |
| 5 | KAN-7 Trajets (US 4.4–4.6) | ⏳ À venir |
| 6 | KAN-8 Paramètres + Badges + Streak | ⏳ À venir |
| 7 | KAN-9 Catalogue 19 trajets | ⏳ À venir |
| 8 | KAN-10 Accessibilité + KAN-11 Tests | ⏳ À venir |

Suivi des tickets : [floviret.atlassian.net/jira/software/projects/KAN](https://floviret.atlassian.net/jira/software/projects/KAN)

---

## Licence

Projet personnel — tous droits réservés.
