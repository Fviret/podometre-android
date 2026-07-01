# Journal de bord — Podomètre Android

Suivi des tickets livrés, testés et validés.
Colonnes : **Dev** = implémenté par IA | **Testé** = vérifié sur émulateur par l'humain ✅ / ⬜ en attente

---

## Sprint 1 — Fondations (2026-06-30)

| Ticket | US | Description | Dev | Testé |
|--------|----|-------------|-----|-------|
| KAN-12 | — | Initialiser le projet Android (structure, Gradle, Hilt, Material 3) | ✅ | ⬜ |
| KAN-13 | — | Navigation par onglets (Activité / Trajets / Paramètres) | ✅ | ⬜ |
| KAN-14 | — | Persistance locale — DataStore Preferences + JSON (progressMap) | ✅ | ⬜ |
| KAN-15 | — | Injection de dépendances Hilt (AppModule, repositories) | ✅ | ⬜ |

---

## Sprint 2 — Onboarding (2026-06-30)

| Ticket | US | Description | Dev | Testé |
|--------|----|-------------|-----|-------|
| KAN-16 | US-2.1 | Flux d'onboarding 4 slides (carrousel, non-dismissable) | ✅ | ⬜ |
| KAN-17 | US-2.2 | Sélection d'objectif de pas (picker 5 000–20 000) | ✅ | ⬜ |
| KAN-18 | US-2.3 | Demande de permissions Health Connect (READ_STEPS, READ_DISTANCE) | ✅ | ⬜ |

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
| Fix | Description | Validé par humain |
|-----|-------------|-------------------|
| KAN-22 | Centrage du contenu de la bannière météo | ✅ |
| KAN-25 | Cercles de données au premier plan (z-order Canvas) | ✅ |
| KAN-25 | Graphe iso iOS : fenêtre glissante, titre + légende, cercles plus grands | ✅ |
| KAN-25 | Aujourd'hui toujours à droite du graphe (rolling window) | ✅ |
| —     | Anneau de progression remonté en haut de l'écran Activité | ✅ |

---

## À venir

| Ticket | US | Description |
|--------|----|-------------|
| KAN-26+ | Sprint 4 | Système de Trajets (catalogue, progression, jalons, badges) |
| KAN-?  | Sprint 6 | Paramètres (objectif, couleur anneau, mode sombre, streak) |

---

## Notes personnelles

<!-- Zone libre — écris ici tes observations, idées, blocages -->

