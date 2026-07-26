# /sprint — Orchestrateur de sprint autonome

Traite **tous** les tickets Jira "À faire" un par un, dans l'ordre de priorité.
Pour chaque ticket : implémentation → attente CI → review → merge → "Terminé".
Quand le backlog est vide : génère le rapport de fin de sprint.

---

## Processus

### 1. Charger la file de travail

Via MCP Atlassian (`cloudId: floviret.atlassian.net`) :

```jql
project = KAN AND status = "To Do" ORDER BY priority ASC, created ASC
```

Si 0 ticket → afficher "Backlog vide — rien à traiter." et s'arrêter.

Sinon, afficher le plan :
```
🎯 Sprint en cours — N tickets à traiter
  1. KAN-XX — Titre
  2. KAN-XX — Titre
  ...
```

Initialiser le tableau de bord interne (tenu à jour après chaque ticket) :

| # | Ticket | Titre | PR | CI | Review | Merge | Statut |
|---|---|---|---|---|---|---|---|

---

### 2. Boucle principale — pour chaque ticket

Répéter les étapes A → D jusqu'à ce qu'il n'y ait plus de ticket dans la file.

---

#### Étape A — Implémentation (subagent foreground)

Charger le skill `sprint-implement` et utiliser ses instructions comme prompt pour un subagent.

Le subagent reçoit en contexte :
- L'ID du ticket courant
- Le chemin du repo : `/Users/floviret/Documents/podometreandroid`
- Les conventions CLAUDE.md (déjà incluses dans le skill)

Lancer le subagent en **foreground** (`run_in_background: false`).

**Parser la dernière ligne** du retour du subagent :
- `STATUS=OK PR_NUMBER=<N> BRANCH=<branche>` → continuer avec PR#N
- `STATUS=BUILD_FAIL:*` / `STATUS=TEST_FAIL:*` / `STATUS=IMPL_FAIL:*` → marquer le ticket ❌ IMPL, passer au suivant

---

#### Étape B — Attente CI GitHub Actions

```bash
PR_NUMBER=<N>  # extrait de l'étape A
echo "⏳ Attente CI pour PR #$PR_NUMBER..."

for i in $(seq 1 40); do
  CHECKS=$(gh pr checks $PR_NUMBER --json name,status,conclusion 2>/dev/null)
  PENDING=$(echo "$CHECKS" | python3 -c "
import sys, json
data = json.load(sys.stdin)
print(sum(1 for c in data if c.get('status') in ('IN_PROGRESS', 'QUEUED', 'PENDING', 'WAITING')))
" 2>/dev/null || echo "0")
  FAILED=$(echo "$CHECKS" | python3 -c "
import sys, json
data = json.load(sys.stdin)
print(sum(1 for c in data if c.get('conclusion') in ('failure', 'cancelled', 'timed_out')))
" 2>/dev/null || echo "0")

  if [ "$FAILED" -gt 0 ]; then
    echo "❌ CI échoué (check $i/40)"
    CI_STATUS="FAIL"
    break
  fi
  if [ "$PENDING" -eq 0 ]; then
    echo "✅ CI passé (check $i/40)"
    CI_STATUS="OK"
    break
  fi
  echo "  Check $i/40 — $PENDING en attente, retry dans 30s..."
  sleep 30
done

# Timeout
if [ -z "$CI_STATUS" ]; then
  CI_STATUS="TIMEOUT"
fi
echo "CI_RESULT=$CI_STATUS"
```

- `CI_STATUS=OK` → continuer
- `CI_STATUS=FAIL` ou `CI_STATUS=TIMEOUT` → marquer le ticket ❌ CI, **ne pas merger**, passer au suivant

---

#### Étape C — Review de code (subagent foreground)

Charger le skill `sprint-review` et utiliser ses instructions comme prompt pour un subagent.

Le subagent reçoit en contexte :
- `PR_NUMBER` (extrait de l'étape A)
- `TICKET_ID` (ticket courant)
- Le chemin du repo

Lancer le subagent en **foreground** (`run_in_background: false`).

**Parser la dernière ligne** du retour :
- `MERGE=OK TICKET=KAN-XX` → ticket livré ✅
- `MERGE=FAIL TICKET=KAN-XX REASON=*` → marquer le ticket ❌ REVIEW, noter la raison, passer au suivant

---

#### Étape D — Mettre à jour le tableau de bord

Mettre à jour la ligne du ticket dans le tableau de bord avec les statuts CI, Review, Merge.
Afficher le tableau mis à jour après chaque ticket.

---

### 3. Fin de boucle — vérification backlog

Après avoir traité tous les tickets de la file initiale, interroger Jira à nouveau :

```jql
project = KAN AND status = "To Do"
```

- Si tickets restants → les lister (nouveaux tickets ou tickets skippés)
- Si backlog vide → générer le rapport de fin de sprint

---

### 4. Rapport de fin de sprint

```
╔══════════════════════════════════════════════════════════╗
║        RAPPORT DE SPRINT — KAN — {DATE}                  ║
╚══════════════════════════════════════════════════════════╝

## ✅ Tickets livrés ({N_OK}/{N_TOTAL})

| Ticket | Titre | PR | CI | Review |
|--------|-------|----|----|----|
| KAN-XX | ...   | #N | ✅ | ✅ |

## ❌ Tickets en échec ({N_FAIL})

| Ticket | Titre | Cause | Action recommandée |
|--------|-------|-------|--------------------|
| KAN-XX | ...   | CI_FAIL / REVIEW_FAIL / IMPL_FAIL | ... |

## Métriques
- Tickets traités    : {N_TOTAL}
- Livrés dans dev    : {N_OK}
- En échec           : {N_FAIL}
- Taux de succès     : {PCT}%

## Prochaines étapes
{Si N_FAIL > 0}
- [ ] Investiguer et corriger les {N_FAIL} tickets en échec
- [ ] Relancer /sprint quand les correctifs sont appliqués

{Si N_FAIL == 0}
- [ ] Vérifier les PRs mergées dans dev
- [ ] Merger dev → main : git checkout main && git merge --no-ff dev -m "Merge sprint {DATE}"
- [ ] Pousser : git push origin main

══════════════════════════════════════════════════════════
```

---

## Règles importantes

- **Ne jamais merger directement dans main** — toujours dans dev
- **Ne pas merger si CI échoué** — même si la review est OK
- **Ne pas bloquer le sprint sur un ticket en échec** — logger et continuer
- **Un PR = un ticket** — pas de regroupement
- **Tous les commits signés** `Co-Authored-By: Claude <noreply@anthropic.com>`
