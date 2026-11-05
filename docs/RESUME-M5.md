# Résumé M5 — édition assistée (en cours)

> **Sortie visée** : catégorie A P0 couverte (A-05, A-06, A-11,
> A-20) + D-08/D-09/D-10 ; scénario par ligne de matrice
> référencé. La vérification appareil du propriétaire reste sienne.

> **Déclenchement** : clôture du jalon M4 (2026-11-05, critères de
> sortie couverts — ré-analyse ≤ 10 ms, corpus, scénarios). Périmètre
> (feuille de route) : frappe intelligente (paires, indentation,
> retour arrière, entourer, collage) ; gabarits dynamiques ;
> interface de recherche/remplacement ; historique de
> navigation ; fil d'Ariane ; lignes collantes ; popups
> unifiées ; plugins isolés. Vingt-trois lignes de matrice M5 au
> total.

## Tranche 1 — la frappe intelligente par plans (2026-11-06)

### Livré

- **Le contrat du geste** (`EditPlan`) : transaction par la voie
  unique + sélection d'après explicite. Le moteur de décision ne mute
  jamais ; la session applique le plan, l'annulation restitue la
  sélection d'avant, le refaire la sélection explicite (A-13).
- **La surcharge de session**
  (`TextSession.apply(Transaction, MultiSelection)`) : le geste
  intelligent choisit ses curseurs — entre les deux caractères d'une
  paire, après l'indentation d'une Entrée — là où la projection
  automatique ne peut pas les deviner. La transaction vide déplace la
  sélection sans créer d'étape : le saut par-dessus un fermant
  n'édite pas.
- **Les règles par langage** (`TypingRules`) : paires appariées
  `(`, `[`, `{` et citations ; le JSON ne dédouble que le
  guillemet ; les identifiants inconnus retombent sur le défaut —
  la longue traîne (C-16) frappe aussi bien.
- **La frappe** (`SmartTyping.insert`, A-05) : l'ouvrant insère sa
  paire, curseur au milieu ; la citation se dédouble ;
  l'ouvrant ou la citation sur une sélection l'entoure, le contenu reste
  sélectionné ; le fermant déjà présent sous le curseur est
  sauté — pas de double fermeture. Multi-curseurs : chaque
  curseur reçoit son geste, deltas cumulés.
- **L'Entrée** (`SmartTyping.enter`, A-06) : le retrait attendu se
  lit dans l'arbre — le nombre de nœuds structurants qui englobent la
  coupe (`IndentEngine.depthAtCaret`). Le fermant qui reste sur la
  ligne descend seul sous le curseur : l'Entrée entre accolades
  produit les deux sauts. Style espaces ou tabulations
  (`IndentStyle`) ; sans arbre, la ligne courante prête son
  indentation.
- **Le retour arrière** (`SmartTyping.backspace`, A-07) : la paire
  vide disparaît entière, la citation vide aussi, l'indentation
  excédentaire se retire d'un cran. Tout autre cas rend la main :
  la session retombe sur sa suppression par clusters de graphèmes
  (ADR-003, livré M2) — le repli est global, geste partagé par tous les
  curseurs, documenté.
- **L'entourage** (`SmartTyping.surround`, A-08) : paire en ligne
  ou bloc multiligne (le contenu gagne un cran), curseur après le
  fermant, chaque plage de la sélection entourée.
- **Le collage réindenté** (`PasteReindenter`, A-09) : la première
  ligne ne bouge pas — elle complète la ligne d'accueil ; les
  suivantes s'alignent sur la profondeur d'accueil lue dans l'arbre, la
  structure relative du collage conservée.

### Vérifié localement

- `./gradlew check` vert sur l'ensemble du dépôt, modules purs ET avec
  SDK Android (politique de langue, Checkstyle, PMD, manifestes
  compris) ;
- `:core-text:test` : **424 tests, zéro échec** (la surcharge
  comprise — restauration au refaire, transaction vide, lecture seule,
  bornes) ;
- `:core-intelligence:test` : **40 tests, zéro échec** — paires,
  Entrée (corpus Java et XML vérifiés par sonde sur les bornes réelles
  des nœuds), retour arrière, entourage, collage ;
- mutation `:core-text:mutationThreshold` : **85,2 %** (seuil
  80) sur 1 807 mutations ;
- manifestes d'API régénérés (`core-text`, `core-intelligence`).

### Écarts et limites

1. **L'arbre peut être périmé d'une édition** : la profondeur se
   lit sur l'analyse publiée — une accolade à peine frappée peut
   décaler le retrait d'un cran jusqu'à la ré-analyse (≤ 10 ms par le
   moteur M4). Borné, jamais faux de plus d'un cran.
2. **Le repli du retour arrière est global** : en multi-curseurs
   mixte (une paire vide, un mot), tous les curseurs retombent sur la
   suppression par clusters — la paire se retire alors caractère par
   caractère sur ce curseur. Choix documenté, la règle locale par
   curseur n'était pas au périmètre.
3. **L'Entrée en repli (sans arbre) ne descend pas le fermant** :
   la double insertion suppose la profondeur ; sans arbre, seul
   le saut simple est produit.
4. **Les gabarits de A-06** (« Entrée, collage, gabarit ») : la
   facette gabarit d'indentation suit la tranche des gabarits
   dynamiques (A-22).
