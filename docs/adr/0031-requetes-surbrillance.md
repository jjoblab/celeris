# ADR-0031 — La couleur de la voie native vient des requêtes highlights.scm

- **Statut** : Acceptée (chantier D de la coloration — prompt du
  2026-11-22, point 1 : « requêtes .scm, suppression de la table
  KINDS codée en dur »)
- **Date** : 2026-11-22

## Contexte

L'audit coloration (§ constat) avait mesuré la voie native
(`core-syntax-treesitter`) : la couleur venait d'une table de mots
codée en dur (`MaterialisationNatif.KINDS` : 52 entrées `if`, `def`,
`class`…) et d'heuristiques sur le nom du nœud — aucune requête
`.scm`, aucune distinction fonction/classe/paramètre, la teinte des
langages non couverts par la table retombait en identificateur.

## Décision

**Trois pièces, la liaison native gardant son rôle d'exécution :**

1. **`LecteurRequetes`** (module, classe close) : un fichier
   `highlights.scm` se lit SANS la liaison — la grammaire du format
   est régulière (motifs parenthésés, captures `@nom`, littéraux
   `"if"`, commentaires `;`). La capture s'attache au NŒUD qu'elle
   suit : dans `(racine champ: (terminal) @capture)`, le nœud capturé
   est `terminal` — le lecteur tient la pile des noms, fige le nom
   du nœud à SA fermeture, et rend les entrées (nom du nœud,
   capture) dans l'ordre du texte.
2. **`CapturesSurbrillance`** : la carte capture → `TokenKind` —
   `@keyword` → KEYWORD, `@function.method` → IDENTIFIER (le socle
   n'a pas de teinte « fonction » : la distinction vit à la vue par
   la portée), `@constant.builtin` → KEYWORD, les composées par leur
   famille (`keyword.return` → KEYWORD), l'inconnue → IDENTIFIER
   (la requête évolue sans casser la teinte).
3. **`MaterialisationNatif.installerRequete(entrees)`** : chaque
   entrée lue y pose sa teinte dans la carte des noms de nœuds
   (`ConcurrentHashMap`, `putIfAbsent`) — `kindOfName` la consulte
   AVANT les heuristiques de repli (la requête absente ne casse
   rien : le repli heuristique reste). **La table `KINDS` de 52 mots
   est retirée** : la couleur vient de la requête du langage.

**Ce que cette version ne fait PAS encore** (les points 2-6 du
chantier D, hors du périmètre de cette passe) : l'exécution de la
requête par `TSQuery`/`TSQueryCursor` borné à la plage visible (la
liaison AndroidIDE 4.3.3 la publie — l'aplatisseur actuel reste le
chemin), les prédicats `#match?`/`#eq?`, `locals.scm`, les
injections, et les grammaires supplémentaires. La lecture et la
teinte par requête sont la fondation : l'exécution native s'y
branchera en remplaçant l'aplatisseur, la couture
(`installerRequete`) restera.

## Bancs

- `RequetesSurbrillanceTest` (JVM, sans liaison) : 12 entrées lues
  d'un extrait réaliste (motifs imbriqués, atomiques, littéraux,
  captures composées), la carte traduit les captures (familles,
  inconnues).
- `MaterialisationNatifTest` : les bancs existants installent LEUR
  requête avant la lecture (le nouveau contrat) — la teinte de
  `def`/`true` vient de la requête, plus de table codée en dur.
- Module vert : 21 tests, 0 échec (7 sautés : les bancs natifs sans
  Android, contrat de la liaison absente sur JVM).

## Conséquences

- La teinte d'un nœud dépend de la requête installée : un langage
  sans requête garde les heuristiques de repli (identique à avant).
- `installerRequete` est de visibilité paquet : le montage natif
  l'appellera au chargement de la grammaire (la suite du chantier).
