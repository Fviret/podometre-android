# Journal de bord — Podomètre Android

Suivi des tickets livrés, testés et validés.
Colonnes : **Dev** = implémenté par IA | **Testé** = vérifié sur émulateur par l'humain ✅

---

## Sprint 1 — Fondations (2026-06-30)

| Ticket | US | Description | Dev | Testé |
|--------|----|-------------|-----|-------|
| KAN-12 | — | Initialiser le projet Android (structure, Gradle, Hilt, Material 3) | ✅ | ✅ |
| KAN-13 | — | Navigation par onglets (Activité / Trajets / Paramètres) | ✅ | ✅ |
| KAN-14 | — | Persistance locale — DataStore Preferences + JSON (progressMap) | ✅ | ✅ |
| KAN-15 | — | Injection de dépendances Hilt (AppModule, repositories) | ✅ | ✅ |

---

## Sprint 2 — Onboarding (2026-06-30)

| Ticket | US | Description | Dev | Testé |
|--------|----|-------------|-----|-------|
| KAN-16 | US-2.1 | Flux d'onboarding 4 slides (carrousel, non-dismissable) | ✅ | ✅ |
| KAN-17 | US-2.2 | Sélection d'objectif de pas (picker 5 000–20 000) | ✅ | ✅ |
| KAN-18 | US-2.3 | Demande de permissions Health Connect (READ_STEPS, READ_DISTANCE) | ✅ | ✅ |

---

## Sprint 3 — Écran Activité (2026-06-30 → 2026-07-01)

| Ticket | US | Description | Dev | Testé |
|--------|----|-------------|-----|-------|
| KAN-19 | US-3.1 | Anneau de progression journalier (Canvas Compose, dégradé animé) | ✅ | ✅ |
| KAN-20 | US-3.2 | Mise à jour des pas en temps réel (WorkManager hourly + foreground refresh) | ✅ | ✅ |
| KAN-21 | US-3.3 | Navigation entre les jours par chevrons gauche/droite (ghost slot pattern) | ✅ | ✅ |
| KAN-22 | US-3.4 | Bannière météo du moment via Open-Meteo (RAIN_NOW / RAIN_SOON / NO_RAIN) | ✅ | ✅ |
| KAN-23 | US-3.5 | Prévisions météo 7 jours (LazyRow, emoji WMO, tempMax/Min, précipitations) | ✅ | ✅ |
| KAN-24 | US-3.6 | Calendrier mensuel (grille L-D, 3 états de cellule, navigation mois, tap → anneau) | ✅ | ✅ |
| KAN-25 | US-3.7 | Graphe comparaison hebdomadaire (Canvas Compose, fenêtre 7 jours glissante, iso iOS) | ✅ | ✅ |

### Fixes sprint 3
| Fix | Description | Validé |
|-----|-------------|--------|
| KAN-22 | Centrage du contenu de la bannière météo | ✅ |
| KAN-25 | Cercles de données au premier plan (z-order Canvas) | ✅ |
| KAN-25 | Graphe iso iOS : fenêtre glissante, titre + légende, cercles plus grands | ✅ |
| KAN-25 | Aujourd'hui toujours à droite du graphe (rolling window) | ✅ |
| — | Anneau de progression remonté en haut de l'écran Activité | ✅ |

---

## Sprint 4 — Système de Trajets — Modèle & Sync (2026-07-01)

| Ticket | US | Description | Dev | Testé |
|--------|----|-------------|-----|-------|
| KAN-26 | US-4.1 | Modéliser les 19 trajets (JourneyData.kt, Journey, Milestone, JourneyCategory) | ✅ | ✅ |
| KAN-27 | US-4.2 | Persister et synchroniser la progression des trajets (JourneyProgressRepository, SyncJourneyWorker) | ✅ | ✅ |
| KAN-28 | US-4.3 | Catalogue des trajets par catégorie (LazyColumn, JourneyCard, prévisualisation) | ✅ | ✅ |

---

## Sprint 5 — Système de Trajets — UI & Notifications (2026-07-01 → 2026-07-02)

| Ticket | US | Description | Dev | Testé |
|--------|----|-------------|-----|-------|
| KAN-29 | US-4.4 | Feuille de prévisualisation trajet (ModalBottomSheet scrollable + bouton fixe) | ✅ | ✅ |
| KAN-30 | US-4.5 | Écran détail progression trajet (timeline, auto-scroll, sheet jalon débloqué) | ✅ | ✅ |
| KAN-31 | US-4.6 | Notifications locales jalons et complétion de trajet (JourneyNotificationService) | ✅ | ✅ |

---

## Sprint 6 — Paramètres, Badges, Streak, Notifications (2026-07-02)

| Ticket | US | Description | Dev | Testé |
|--------|----|-------------|-----|-------|
| KAN-32 | US-5.1 | Picker objectif quotidien de pas dans les Paramètres (grille 3 colonnes, 5 000–20 000) | ✅ | ✅ |
| KAN-33 | US-5.2 | Sélecteur de couleur de l'anneau (6 presets, cercles interactifs) | ✅ | ✅ |
| KAN-34 | US-5.3 | Toggle mode sombre avec propagation immédiate au thème racine | ✅ | ✅ |
| KAN-35 | US-5.4 | Toggles modules écran principal (météo, calendrier, graphe) | ✅ | ✅ |
| KAN-36 | US-5.5 | Toggles notifications avec gestion permission POST_NOTIFICATIONS | ✅ | ✅ |
| KAN-37 | US-5.6 | Calculer et afficher le streak de jours consécutifs (bannière 🔥 dans Paramètres) | ✅ | ✅ |
| KAN-38 | US-5.7 | Grille de badges de pas (6 seuils) et de trajets (19) dans les Paramètres | ✅ | ✅ |
| KAN-39 | US-5.8 | Notification "Objectif atteint ! 🎉" (max 1/jour, via SyncStepsWorker) | ✅ | ✅ |

### Incidents & aller-retours sprint 6

**KAN-31 — Notification des jalons :** premier jet avec `?attr/colorControlNormal` comme tint dans le drawable XML — AAPT a refusé (ressource d'attribut non résolue à la compilation). Corrigé en passant à `android:fillColor="#FFFFFF"` directement sur le path. Leçon : les attributs de thème ne sont pas résolubles dans les drawables vectoriels via AAPT, uniquement via le rendu Compose/View.

**KAN-29 — ModalBottomSheet :** `rememberModalBottomSheetState(skipPartialExpansion = true)` ne compile pas dans la version Material3 utilisée (paramètre inexistant). Corrigé en appelant `rememberModalBottomSheetState()` sans argument.

**KAN-37 — Streak :** `flatMapLatest` est annoté `@ExperimentalCoroutinesApi` — warning à la compilation. Résolu avec `@OptIn(ExperimentalCoroutinesApi::class)` sur la propriété. Import `asStateFlow` également oublié au premier jet pour les badges (KAN-38), corrigé immédiatement.

**KAN-38 — Ordre des tickets :** le ticket KAN-38 a été sauté lors d'une invocation `/feature` parce que la requête Jira traitait `priority ASC, created ASC` et KAN-38 avait été touché manuellement, décalant sa date de mise à jour. Détecté et corrigé par le développeur. La requête utilisera désormais `created ASC` uniquement pour respecter l'ordre du backlog.

**KAN-49 (ce ticket) :** pas de description dans Jira à la création. La description réelle ("journal narratif, aller-retours, succès et échecs") n'est apparue qu'à la transition vers "Revue en cours". Premier jet du journal trop tabulaire → enrichissement narratif a posteriori dans le même commit.

---

## À venir

| Ticket | US | Description |
|--------|----|-------------|
| KAN-40 | US-6.1 | Données trajets Promenades (5 trajets avec jalons complets) |
| KAN-41 | US-6.2 | Données trajets Sentiers (6 trajets avec jalons complets) |
| KAN-42 | US-6.3 | Données trajets Histoire (5 trajets avec jalons complets) |
| KAN-43 | US-6.4 | Données trajets Mythes & Épopées (3 trajets avec jalons complets) |
| KAN-10 | — | Accessibilité complète (TalkBack, contentDescription, roles) |
| KAN-11 | — | Tests unitaires et instrumentés |

---

## Outillage IA — Skill `/feature`

Une des décisions techniques les plus structurantes du projet n'est pas dans le code Kotlin : c'est la création du skill `/feature` pour Claude Code.

### Contexte

Dès le sprint 3, le rythme dev IA → review humain → merge commençait à montrer une friction : l'IA devait retrouver le bon ticket Jira, créer la bonne branche, respecter les conventions, ouvrir la PR au bon endroit, et transitionner le ticket. Autant d'étapes répétitives et sources d'erreurs de contexte.

### Ce qu'est le skill

Un fichier Markdown dans `.claude/commands/feature.md` qui encode la procédure complète en 11 étapes. Claude Code l'exécute quand on tape `/feature` dans le chat.

```
/feature          → prend le premier ticket "À faire" dans Jira
/feature KAN-49   → traite ce ticket précis
```

La séquence automatisée :
1. Fetch Jira (description, critères d'acceptation)
2. Exploration du codebase concerné
3. Création de branche depuis `dev`
4. Implémentation + respect des conventions `CLAUDE.md`
5. `assembleDebug` + `testDebugUnitTest`
6. Commit signé Co-Authored-By Claude
7. Push + PR GitHub vers `dev`
8. Transition Jira → "Revue en cours" + lien PR en commentaire

### Impact observé

À partir du sprint 4, chaque ticket a été livré de bout en bout en une seule invocation `/feature`, sans intervention manuelle entre la demande et la PR ouverte. Sur les sprints 4 à 6 (KAN-26 à KAN-39 + KAN-49), soit **15 tickets**, le skill a été invoqué 15 fois avec 0 oubli de PR ou de transition Jira.

### Limite identifiée

Le tri JQL initial (`ORDER BY priority ASC, created ASC`) a causé un saut de KAN-38 : le ticket avait été touché manuellement, décalant sa date de mise à jour. Détecté et corrigé par le développeur lors d'une vérification manuelle du backlog. La leçon : l'automatisation ne remplace pas la relecture humaine du backlog.

---

## Notes personnelles

<!-- Zone libre — écris ici tes observations, idées, blocages -->

