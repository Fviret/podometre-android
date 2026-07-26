---
description: Implémenter un ticket Jira de bout en bout — branche, code, tests, PR, ticket en review
allowed-tools: ["Bash", "Read", "Write", "Edit", "Glob", "Grep", "mcp__atlassian__*"]
---

# /feature — Boucle complète feature Jira → PR

## Paramètre

`$ARGUMENTS` peut être :
- Un ID de ticket (ex. `KAN-12`) → traite ce ticket précis
- Vide → sélectionne automatiquement le ticket "To Do" de plus haute priorité dans le projet KAN

---

## Processus

### 1. Récupérer le ticket Jira

Si `$ARGUMENTS` est fourni, fetch le ticket `$ARGUMENTS`.  
Sinon, cherche dans Jira :
```
project = KAN AND status = "To Do" ORDER BY priority ASC, created ASC LIMIT 1
```

Extrais et affiche :
- **ID** du ticket (ex. `KAN-12`)
- **Titre** (summary)
- **Description** et critères d'acceptation
- **Epic parent** si présent

### 2. Analyser le codebase

Lis le `CLAUDE.md` à la racine pour les conventions.  
Explore les fichiers concernés par le ticket (utilise Glob et Grep pour localiser les composants à modifier ou créer).  
Construis une plan d'implémentation concis avant de commencer à coder.

### 3. Créer la branche

```bash
git checkout dev && git pull origin dev
git checkout -b feature/{TICKET_ID}-{slug-du-titre}
```

Le slug est le titre en minuscules, espaces remplacés par des tirets, 30 caractères max.

### 4. Implémenter la feature

Respecte scrupuleusement les conventions du `CLAUDE.md` :
- Kotlin 2.0+ / Jetpack Compose / Material 3
- MVVM + StateFlow, jamais LiveData
- `@HiltViewModel` pour tous les ViewModels
- `collectAsStateWithLifecycle()` dans les Composables
- Pas de force-cast (`!!`) — utiliser `?.let`, `?: return`
- Pas de bibliothèques tierces pour les graphes (Canvas Compose uniquement)
- Health Connect : ne jamais stocker localement, toujours lire depuis la source
- Ajouter `if (isEmulator())` avec mock data réaliste pour l'émulateur
- Documenter toute fonction non triviale avec `/** en français */`

Pour chaque nouveau fichier ou modification, vérifie la cohérence avec le reste du module.

### 5. Vérifier la compilation

```bash
./gradlew assembleDebug --no-daemon 2>&1 | tail -30
```

Si la compilation échoue, corrige les erreurs avant de continuer.

### 6. Lancer les tests

```bash
./gradlew testDebugUnitTest --no-daemon 2>&1 | tail -30
```

Si des tests échouent, corrige-les ou documente pourquoi ils ne sont pas applicables.

### 7. Committer

```bash
git add <fichiers modifiés>
git commit -m "feat({TICKET_ID}): {résumé en français, max 72 chars}" \
  --trailer "Co-Authored-By: Claude <noreply@anthropic.com>"
```

### 8. Pusher la branche

```bash
git push origin feature/{TICKET_ID}-{slug}
```

### 9. Créer la PR GitHub

```bash
gh pr create \
  --base dev \
  --title "{TICKET_ID}: {titre complet du ticket}" \
  --body "## Ticket Jira
[{TICKET_ID}](https://floviret.atlassian.net/browse/{TICKET_ID})

## Changements
{description des changements en bullet points}

## Critères d'acceptation
{liste des critères issus du ticket}

## Tests
{ce qui a été testé, résultats}

## Notes
{points d'attention pour la review}"
```

### 10. Passer le ticket en "In Review" sur Jira

Via le MCP Atlassian :
- Transition le ticket vers le statut **"In Review"**
- Ajoute un commentaire sur le ticket avec le lien de la PR :
  > PR ouverte : {lien GitHub de la PR}

### 11. Rapport final

Affiche un résumé :
```
✅ Feature {TICKET_ID} implémentée

Branche : feature/{TICKET_ID}-{slug}
PR      : {lien GitHub}
Jira    : https://floviret.atlassian.net/browse/{TICKET_ID} → In Review

À faire : reviewer et merger la PR dans dev
```

**⛔ Ne pas merger la PR — s'arrêter là.**
