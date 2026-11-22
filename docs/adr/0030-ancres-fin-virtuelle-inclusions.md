# ADR-0030 — Ancres de document, fin de ligne virtuelle, inclusions externes et registre de la fabrique

- **Statut** : Acceptée (chantier C de la coloration — prompt du
  2026-11-22, constats C1-C4 et C7 de l'audit ; bancs rouges posés
  avant chaque correctif)
- **Date** : 2026-11-22

## Contexte

L'audit coloration avait chiffré quatre écarts de fidélité entre le
lexeur de grammaires et le modèle TextMate réel (tm4e/Sora) :

- **C1** : `Expressions.compiler` compilait avec `Option.NONE` — les
  grammaires TextMate lisent les captures `\N` et `$0` des
  correspondances ; tm4e compile avec `Option.CAPTURE_GROUP`,
  `Syntax.DEFAULT`, `WarnCallback.NONE`.
- **C2** : six grammaires livrées portent `\A` (csharp, css,
  javascript, lua, swift, typescript) — tm4e le traite par
  `buildAnchorCache` avec allowA sur la ligne 0 : notre lexeur
  ligne-indépendant lisait `\A` « début de la ligne » : la grammaire
  csharp matchait son en-tête sur TOUTES les lignes.
- **C3** : vingt grammaires livrées portent des motifs à `\n` (lua
  39, swift 18, ruby 19…) — tm4e ajoute `'\n'` à la ligne avant la
  recherche (`Grammar.tokenizeLine`) ; nos lignes ne le portaient
  pas : ces motifs échouaient à la fin de chaque ligne.
- **C4** : `PackGrammars.compile` lisait `Grammars.read(json)` SANS
  registre — les inclusions externes (`source.css` depuis html :
  `<style>`, `<script>`) se sautaient, comptées au rapport.

## Décision

**C1 — les groupes capturants vivent.** `Expressions.compiler`
compile avec `Option.CAPTURE_GROUP`, `Syntax.DEFAULT`,
`WarnCallback.NONE` : les portées de capture des grammaires réelles
sont servies, la région des correspondances rend ses groupes.

**C2 — l'ancre de document ne vit que sur la première ligne.** Le
lecteur tisse un jeu d'ancres par grammaire (`Grammar.ancres()`,
`IdentityHashMap<Regex, Regex>`) : tout motif à `\A` s'y double de son
expression IMPOSSIBLE (`\A` remplacé par le non-caractère `\uFFFF`).
Le lexeur marque la PREMIÈRE ligne de sa vie (le service bâtit un
lexeur neuf par analyse, la passe part de la ligne 0 — le début du
texte coïncide avec le début de document) : hors cette ligne, toute
expression du jeu se traduit par son double impossible. `Grammar`
passe de record à classe finale immuable pour porter le champ (un
record n'accepte pas de champ d'instance ; l'égalité par identité
des Regex impose `IdentityHashMap`).

**C3 — la fin de ligne virtuelle.** `Utf8Line.preparer` pose un `\n`
APRÈS la ligne dans le buffer d'octets (la table de correspondance y
mène au BOUT du texte) : les motifs à `\n` matchent en fin de ligne ;
`versChar` borne le retour — une correspondance qui inclut le saut se
rogne à la ligne, exactement comme tm4e rogne à `lineLength`.

**C4 — le registre partagé de la fabrique.**
`PackGrammars.avecRegistre()` : un `ConcurrentHashMap` par portée
racine ; chaque grammaire compilée s'y pose (`putIfAbsent`), les
inclusions externes des grammaires suivantes s'y résolvent par la
couture existante (`GrammarReader.externe`). `INSTANCE` (sans
registre) reste pour les usages isolés — l'inclusion inconnue se
saute, comptée au rapport, jamais de refus de paquet.

**C7 — le banc de reproductibilité des piles.**
`ScopeStacksOrdreTest` : 2 000 piles poussées dans deux ordres
d'ouverture — mêmes portées servies (la table statique bornée à
50 000 nœuds reste un garde-fou documenté, pas un cache d'ordre).

## Chiffres

- Bancs : `AncresDocumentTest` 2/2 (rouge avant — la directive
  colorait aussi la ligne suivante), `FinDeLigneVirtuelleTest` 2/2,
  `InclusionsExternesTest` 2/2 (le bloc `<style>` porte les portées
  css de l'invitée), `ScopeStacksOrdreTest` 1/1.
- Non-régression : `:core-syntax-grammars:check` vert (163 tests),
  `:core-language:test` + `:core-syntax:check` verts (407 tests).

## Conséquences

- Le manifeste `api/core-syntax-grammars.api` change : `Grammar`
  cesse d'être un record (plus de `equals`/`hashCode`/`toString`
  automatiques — la grammaire se compare par identité, comme les
  Regex qu'elle porte ; aucun consommateur réel ne comparait des
  grammaires par valeur).
- `PackGrammars.INSTANCE` conserve son comportement historique ;
  le montage de l'hôte (sample) passe à `avecRegistre()` pour la
  couleur des inclusions html (à faire au chantier suivant).
- Les grammaires sans `\A` ne paient rien (jeu vide, la traduction
  ne consulte qu'une carte vide).
