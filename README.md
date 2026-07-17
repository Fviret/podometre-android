# Podomètre Android

Portage Android de l'application iOS Podomètre — suivi de pas quotidiens avec système de trajets virtuels.

---

## Fonctionnalités

- **Anneau de progression** — visualisation circulaire des pas du jour par rapport à l'objectif (Canvas Compose, dégradé animé)
- **Météo** — bannière et prévisions 7 jours via Open-Meteo (sans clé API)
- **Calendrier mensuel** — historique des jours actifs avec navigation par mois
- **Graphe hebdomadaire** — comparaison des 2 dernières semaines en fenêtre glissante
- **Trajets virtuels** — 19 trajets réels (Promenades, Sentiers, Histoire, Mythes & Épopées) progressant avec la distance parcourue via Health Connect
- **Catalogue trajets** — cartes par catégorie, feuille de prévisualisation, écran de détail avec timeline auto-scroll
- **Jalons & notifications** — notifications locales à chaque jalon débloqué et à la complétion d'un trajet
- **Streak** — série de jours consécutifs où l'objectif a été atteint (jusqu'à 365 jours)
- **Badges** — grille 3 colonnes : 6 badges de seuils de pas + 19 badges de trajets complétés
- **Notification objectif** — alerte locale « Objectif atteint ! 🎉 » (max 1 par jour)
- **Paramètres** — objectif de pas (5 000–20 000), couleur de l'anneau (6 presets), mode sombre, modules de l'écran principal, notifications
- **Accessibilité** — compatibilité TalkBack (nœuds sémantiques fusionnés, éléments décoratifs masqués) et support du Dynamic Type (tailles de police système, `TextStyle` Material 3)

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
| Tests | JUnit 5 + MockK + Coroutines Test (unitaires) · Compose Testing + Espresso (intégration UI) |

**Minimum SDK :** Android 8.0 (API 26) — Health Connect requiert Android 9+ (API 28)  
**Target SDK :** API 35

---

## Architecture

Pattern **MVVM** avec Hilt pour l'injection de dépendances.

```
app/src/main/java/com/fviret/podometre/
├── ui/
│   ├── activity/        ← Écran Activité (anneau, météo, calendrier, graphe)
│   ├── journey/         ← Catalogue trajets, preview, détail progression
│   ├── settings/        ← Paramètres, streak, badges
│   ├── onboarding/      ← Flux d'onboarding 4 slides
│   └── theme/           ← MaterialTheme, couleurs, typographie
├── data/
│   ├── health/          ← HealthConnectRepository
│   ├── journey/         ← JourneyProgressRepository (JSON local)
│   ├── weather/         ← WeatherRepository (Open-Meteo)
│   └── preferences/     ← UserPreferencesRepository (DataStore)
├── domain/
│   ├── model/           ← Journey, Milestone, JourneyProgress
│   └── JourneyData.kt   ← Les 19 trajets définis comme constantes
├── di/                  ← Modules Hilt (AppModule, HealthConnectModule)
└── worker/              ← SyncStepsWorker, SyncJourneyWorker, notifications
```

### Principes clés

- `ViewModel` + `StateFlow` / `SharedFlow` — pas de `LiveData`
- `@HiltViewModel` sur tous les ViewModels
- `collectAsStateWithLifecycle()` dans les Composables
- Health Connect : lecture toujours depuis la source, jamais de stockage local
- Requêtes HK idempotentes : recalcul depuis `startDate`, jamais d'incrémentation
- Graphes : Canvas Compose uniquement, aucune bibliothèque tierce

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

Déclenché sur chaque push vers `main` / `dev` et chaque PR vers `dev`, en deux jobs :

**`build`**
1. **Lint** — `./gradlew lint`
2. **Tests unitaires** — `./gradlew testDebugUnitTest`
3. **Build** — `./gradlew assembleDebug`
4. **Upload APK** — artifact `debug-apk`

**`instrumented-tests`** (dépend de `build`)
1. Émulateur headless (`reactivecircus/android-emulator-runner`, API 33, KVM activé sur `ubuntu-latest`)
2. **Tests d'intégration UI** — `./gradlew connectedDebugAndroidTest`
3. **Upload du rapport de test** — artifact `instrumented-test-report`

---

## Workflow Git

```
main          ← stable, protégé
└── dev       ← branche d'intégration
    └── feature/<ticket>   ← une branche par ticket Jira
    └── fix/<ticket>
```

Les PRs sont ouvertes vers `dev`. `main` n'est mis à jour que depuis `dev` avec `--no-ff`.

### Skill `/feature` — automatisation bout-en-bout

Ce projet utilise un **skill Claude Code personnalisé** défini dans [`.claude/commands/feature.md`](.claude/commands/feature.md).

#### Usage

```
/feature            → sélectionne automatiquement le ticket "À faire" le plus ancien
/feature KAN-59     → traite ce ticket précis
```

#### Ce que fait le skill en une seule commande

```
Ticket Jira "À faire"
        ↓
   Fetch + lecture des specs (description, critères d'acceptation)
        ↓
   Exploration du codebase concerné (Grep, Glob)
        ↓
   git checkout -b feature/<ticket>-<slug>  (depuis dev)
        ↓
   Implémentation Kotlin/Compose — conventions CLAUDE.md
        ↓
   ./gradlew assembleDebug  +  testDebugUnitTest
        ↓
   git commit  Co-Authored-By: Claude
        ↓
   git push  +  gh pr create --base dev
        ↓
   Jira → "Revue en cours"  +  commentaire lien PR
        ↓
   ✅ Rapport final  (PR prête, humain review & merge)
```

#### Résultats observés

Sur les sprints 4 à 9 (plus de 30 tickets), le skill a été invoqué sans aucune intervention manuelle entre la demande et la PR ouverte. L'humain garde la main sur la review et le merge — l'IA ne merge jamais elle-même.

| Métrique | Valeur |
|---|---|
| Tickets livrés via `/feature` | 30+ |
| Oublis de PR ou transition Jira | 0 |
| Interventions manuelles entre `/feature` et PR | 0 |

#### Réutiliser ce skill dans ton projet

Le fichier source est disponible ici : [`.claude/skills/feature.md`](.claude/skills/feature.md)

Pour l'adapter à ton projet :
1. Copier `.claude/skills/feature.md` dans `.claude/commands/feature.md`
2. Remplacer `KAN` par ton projet Jira
3. Adapter les conventions de l'étape 4 à ton stack
4. Connecter le MCP Atlassian dans Claude Code (`/mcp`)

> Ce skill illustre l'approche *build in public* du projet : l'IA code, l'humain valide.

---

## Pensée du jour

L'application affiche chaque matin une citation motivante issue d'un recueil de 400 aphorismes CC0.

### Comportement
- **Popup matinale** : s'affiche à la première ouverture de la journée **et** à chaque retour en premier plan (garde 1×/jour dans DataStore).
- **Sélection déterministe** : l'aphorisme du jour est toujours `recueil[(quantième - 1) % 400]` — stable sur 24h, différent chaque jour, sans réseau.
- **Carte dans les Paramètres** : l'aphorisme du jour est visible en permanence, appui = copie dans le presse-papiers.
- **Toggle** : désactiver la feature masque la popup. La réactiver réinitialise la garde et ré-affiche la popup au prochain retour sur l'écran principal.

### Sources
Recueil embarqué (`assets/aphorisms_humor_400.json`) — 400 citations CC0, chargé hors ligne, aucune dépendance réseau.

---

## Trajets disponibles (19)

| Catégorie | Trajets |
|---|---|
| 🚶 Promenades | Tour des Tuileries (2,5 km), Berges de la Seine (5 km), Boucle de Central Park (10 km), Semi-marathon de Paris (21 km), Marathon de Paris (42 km) |
| 🏔 Sentiers | GR20 Complet (180 km), Camino Francés tronçon final (111 km), Camino Francés complet (780 km), Via de la Plata (1 000 km), Tour du Mont Blanc (170 km), Via Francigena tronçon final (420 km) |
| 🏛 Histoire | Route Royale Perse (2 700 km), Alexandre — Campagne Perse (5 000 km), Alexandre — Épopée Complète (35 000 km), Route de la Soie (6 400 km), Marco Polo (12 000 km) |
| ⚔️ Mythes & Épopées | L'Odyssée — Trajet Réel (900 km), L'Odyssée — Voyage Complet (8 000 km), L'Iliade — Siège de Troie (400 km) |

La progression est calculée depuis `DistanceRecord` Health Connect à partir de la date de démarrage du trajet — toujours recalculée, jamais incrémentée.

---

## Roadmap

| Sprint | Épic | Tickets | Statut |
|---|---|---|---|
| 1 | Fondations | KAN-12 à KAN-15 | ✅ Terminé |
| 2 | Onboarding | KAN-16 à KAN-18 | ✅ Terminé |
| 3 | Écran Activité | KAN-19 à KAN-25 | ✅ Terminé |
| 4 | Système de Trajets (modèle + sync) | KAN-26 à KAN-28 | ✅ Terminé |
| 5 | Système de Trajets (UI + notifs) | KAN-29 à KAN-31 | ✅ Terminé |
| 6 | Paramètres + Badges + Streak + Notifs | KAN-32 à KAN-39 | ✅ Terminé |
| 7 | Catalogue 19 trajets (données complètes) | KAN-40 à KAN-43 | ✅ Terminé |
| 8 | Accessibilité (TalkBack + Dynamic Type) | KAN-44, KAN-45 | ✅ Terminé |
| 9 | Tests unitaires et d'intégration UI + audit qualité | KAN-46, KAN-47, KAN-51 à KAN-58 | ✅ Terminé |
| — | Bugfixes & maintenance | KAN-59 (couleur calendrier) | ✅ Terminé |
| 10 | Pensée du jour (400 citations CC0, popup 1×/jour, réarmement) | KAN-60 à KAN-68 | 🔄 In Review |

Suivi des tickets : [floviret.atlassian.net/jira/software/projects/KAN](https://floviret.atlassian.net/jira/software/projects/KAN)

---

## Licence

Projet personnel — tous droits réservés.
