# ADR-016 — Analyse structurelle niveau 2 : tree-sitter écarté à M7.6

- **Statut** : Proposition (spike exécuté le 2026-11-08 — les chiffres sont mesurés, la validation reste au propriétaire)
- **Date** : 2026-11-08
- **Révise** : l'option B de l'ADR-004 (révision du 2026-11-08), restée ouverte par le prompt M7.6

## Contexte

Le prompt M7.6 rouvre l'option B de l'ADR-004 : un moteur d'analyse
incrémental par grammaires, écrit en C (tree-sitter, licence MIT),
intégré par une liaison JNI fine écrite ici — jamais celle de
l'organisation interdite (règle 7). Trois niveaux d'analyse sont
prévus : grammaire générique (défaut), tree-sitter (option natif),
analyseurs écrits factorisés (langages P0). Le spike
(`spikes/treesitter/` — liaison JNI minimale `liaison.c`, harnais Java,
benchmark C pur `bench.c` et sonde `sonde.c`, sources épinglées par
`recuperer-sources`, jamais versionnées) mesure : temps d'analyse
complète, re-parse incrémental après insertion d'un caractère (le
budget de l'ADR-004 est **10 ms** après édition à l'échelle de 50 000
lignes, hors fil UI), mémoire des arbres, taille ajoutée.

Runtime testé : tree-sitter **v0.27.2** (publié le 2026-11-07, MIT,
vérifié). Grammaires : json v0.24.8, python v0.25.0, javascript
v0.25.0 (MIT, ABI 14–15, acceptées par le runtime). Environnement :
bac à sable partagé, gcc 14.2, x86_64 — **pas un appareil** ; les
mesures M0 de référence sur le même environnement (lexeur maison :
8 ms pour 50 000 lignes complètes, 1,6 µs incrémental) donnent
l'étalon relatif.

## Résultats mesurés (corpus VALIDES, 50 000 lignes ou équivalent)

| Mesure | json (2,5 Mo, 16 k lignes) | python (1,0 Mo, 45 k lignes) | javascript (1,0 Mo, 42 k lignes) |
| --- | --- | --- | --- |
| Analyse complète (min de 3 tours) | 263 ms | 132–134 ms | 144–156 ms |
| Incrémental après 1 caractère, **position fixe sans erreur** (plancher, p50 / p95) | 15,5 / 16,7 ms | 9,6 / 9,9 ms | 24,6 / 37,4 ms |
| Incrémental, **position aléatoire** (frappe réelle, p50 / p95) | 16,5 / 18,9 ms | 33,7 / 60,9 ms | 44,5 / 49,5 ms |
| Mémoire résidente de l'arbre | 82 Mo (30× le texte) | 32 Mo (31×) | 26 Mo (26×) |

La sonde à petite échelle (190 Ko, 2 400 lignes) isole le
comportement : `ts_tree_edit` est gratuit (0,001 ms), le re-parse avec
ancien arbre tombe à **0,5 ms** (contre 18 ms sans — l'incrémental
fonctionne), mais le plancher croît **linéairement avec la taille du
document** (≈ 6–10 ms par Mo) : le budget de 10 ms est déjà atteint
vers 1 Mo, loin des 50 000 lignes de l'étalon. Les positions
aléatoires (une frappe réelle au milieu d'un document imparfait)
ajoutent la récupération d'erreur — jusqu'à 44,5 ms de p50 sur
javascript. La mémoire des arbres (26–31× le texte) dépasse le budget
de l'ADR-006 (3× modèle et index) dès quelques centaines de Ko.

Tailles (hôte x86_64, `-O2`, élagué) : runtime **223 Ko** ; grammaires
**+8 Ko** (json) à **+464 Ko** (python) chacune — huit grammaires
tiendraient dans ~3 Mo par ABI. La taille n'est pas le bloqueur ; le
temps et la mémoire le sont. La liaison JNI du spike fonctionne telle
quelle (aucune dépendance, aucun symbole exotique) et les mesures
vérifient le principe du jalon : jamais de JNI dans le chemin de
rendu, tout est matérialisé en Java.

Le prompt exigeait au moins huit grammaires testées si le niveau était
retenu : trois suffisent à constater le dépassement — le bloqueur est
l'échelle, pas la grammaire ; la raison est consignée ici.

## Décision

**Le niveau 2 (tree-sitter) n'est pas livré à M7.6 : les budgets de
l'ADR-004/ADR-006 ne tiennent pas à l'échelle du mandat.** Ce n'est
pas un échec — c'est la réponse chiffrée que le spike devait donner.

1. `LanguageSupport` garde son champ `syntax: {preferred, fallback}` :
   le niveau 2 reste une case vide déclarée dans les paquets, ravivable
   par ADR sans toucher aux consommateurs (l'arbre commun ne sait pas
   qui a répondu).
2. Les niveaux 1 (grammaire générique : plis par crochets et par
   indentation, appariement, plan par `outline.json`) et 3
   (analyseurs écrits factorisés, Java/Kotlin/Groovy/KTS ≤ 150 lignes
   de spécialisation chacun) portent le jalon.
3. Conditions de révision (toutes nécessaires) : (a) mesure sur
   l'appareil de référence — le bac à sable est partagé, un appareil
   rapide pourrait diviser par deux, pas par dix ; (b) bornage par
   taille de fichier (≤ 200 Ko, où le spike mesure 0,5 ms incrémental
   — un « tree-sitter pour fichiers modestes » se défendrait) ; (c)
   budget mémoire révisé ou arbres élagués ; (d) grammaires auditées
   (règle 7, provenance vérifiée).

## Options

| Option | Verdict | Raison mesurée |
| --- | --- | --- |
| Niveau 2 tree-sitter pour tout langage | **écartée à M7.6** | incrémental 9,6–44,5 ms de p50 à l'échelle du mandat (budget 10 ms) ; arbres 26–31× le texte (budget 3×) |
| Niveau 2 borné aux fichiers modestes (≤ 200 Ko) | différée | 0,5 ms incrémental à 190 Ko — tient ; mais la règle « un langage, une voie » vaut mieux qu'un seuil caché : à rouvrir par ADR avec mesures appareil |
| Niveaux 1 + 3 seuls | **retenue** | le lexeur maison mesure 8 ms complet / 1,6 µs incrémental à la même échelle (spike M0, même environnement) ; la plateforme de langages et le moteur TextMate réel donnent la longue traîne |

## Compromis

- La précision syntaxique des grammaires tree-sitter (requêtes
  `highlights.scm`, plis exacts) manque à la longue traîne : le moteur
  TextMate réel (ADR-014) et le plan déclaratif couvrent coloration,
  plis et plan — la structure TOLERANTE reste du niveau 1.
- L'architecture à trois niveaux reste (champ déclaré dans le paquet) :
  le coût de la case vide est nul, l'option reste lisible.

## Conséquences

- Aucun module natif n'entre dans le dépôt : pas de NDK, pas de CMake,
  pas de `.so` par ABI — la chaîne de build reste pure.
- Le registre des tailles du spike (runtime 223 Ko, grammaires 8–464
  Ko) sert de référence si l'option se rouvre.
- Les budgets de l'ADR-004 (re-analyse ≤ 10 ms) restent le seuil de CI
  des niveaux livrés.

## Actions

- [x] M7.6 niveaux 1 et 3 : moteur générique (plis, appariement,
      indentation, plan par données) et factorisation C-like — le
      jalon continue sans le natif. Livré à l'étape 6 : les plis par
      indentation (`IndentationFolds`, réglage `offSide` de la
      configuration, lignes vides et de commentaire seul jamais
      clôturantes) et le plan par règles (`outline.json` des paquets,
      `RuleOutline` — Python, YAML et SQL se structurent sans une
      ligne de Java dédiée) ; les analyseurs écrits de Java, Kotlin,
      KTS et Groovy deviennent des spécialisations d'au plus 150
      lignes du socle `CLikeLexer`/`CLikeParser`, les instantanés des
      corpus prouvant l'identité (`ClikeSnapshotsTest`).
- [ ] Si le propriétaire veut rouvrir : mesures sur l'appareil de
      référence d'abord (plan VERIFICATION-APPAREIL), puis ADR de
      révision avec le bornage par taille.
