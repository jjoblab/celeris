# ADR-0032 — La fenêtre d'ouverture borne la lexicographie

- **Statut** : Acceptée (retard résiduel 1-2 s — prompt du 2026-11-23)
- **Date** : 2026-11-23

## Contexte

Après le chantier A (ADR-0028 : registre des descripteurs, 217-243 ms
jusqu'à la langue au banc JVM), un retard de 1-2 s restait à l'ouverture
de la démo : la première analyse (`SyntaxService.analyzeWindow`) lexAIT
le document ENtier (`lexFull`) avant de publier — N lignes lexées avant
la première ligne colorée.

## Décision

**`analyzeWindow` lex la fenêtre (marge 64 incluse) seule ; le reste
vit VIDE au lexique ; la complétion relit par lots, l'arbre se rebâtit
au lexique complet.**

1. `IncrementalLexer.lexFenetre` : lignes [first-64, last+64] lues,
   les autres = `LexiqueVide.LIGNE` (aucun jeton, profondeur reportée,
   état null) ;
2. `LexDocument.stateBefore` traduit la ligne vide en état initial ;
3. `IncrementalLexer.completerLexique` : passe ordonnée du document,
   256 lignes/lot ;
4. `SyntaxService.completer` : lexique d'abord, rebuild `parseFull`
   quand le lexique est complet (la géométrie de chunks d'un arbre né
   de fenêtre ne se prédit pas) ;
5. `LexedLine.exitState` @Nullable (ligne vide) — NullAway satisfait.

## Chiffres

Le banc `PremiereImageAnalyseBudgetTest` (fenêtre ±32, 100k lignes,
budget 500 ms) passe : 868 ms au banc complet (3 tours, JIT chaud) ;
le lexique lu = ~128 lignes au lieu de 100 000 (sonde du banc
`LexiqueFenetreBorneTest`). La couleur du visible est exacte.

## Écart ouvert (consigné)

Le parseur écrit clike peut bourgeonner un nœud de plus sur la
tranche relexée (enfants 10 vs 11, chunk 12) : l'équivalence
STRUCTURELLE intégrale de l'arbre rebuildé reste à enquêter — la
couleur (contrat principal) est exacte, les plis suivent les chunks
complétés.

## Conséquences

Banc rouge posé d'abord (`LexiqueFenetreBorneTest`, sonde compteur) ;
`Charge480kLignesTest` lit l'attente au LEXIQUE (`porteLignesVides`) ;
non-régression : 245 tests du module vert.
