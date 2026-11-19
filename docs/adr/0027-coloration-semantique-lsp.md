# ADR-0027 — Coloration sémantique LSP : câbler en surcouche, ne pas retirer

- **Statut** : Proposition (2026-11-19 — feuille de route A4 de la
  fiabilisation de la coloration ; le constat d'audit posait
  « câbler ou retirer », cette proposition recommande et attend la
  validation du propriétaire)
- **Date** : 2026-11-19

## Contexte

La chaîne LSP décode déjà les jetons sémantiques du protocole
(`SemanticTokensLsp`, B-02/H-12) : le delta relatif du serveur devient
des positions absolues à la frontière protocolaire, la couverture
raccorde treize méthodes dont `textDocument/semanticTokens`. Mais le
**rendu ne les consomme pas** : la vue colore depuis l'arbre syntaxique
(voies maison et native), les jetons sémantiques s'arrêtent à la
frontière — aucune couleur de type, aucun paramètre distingué, aucune
variable ombrée.

L'audit du 2026-11-19 a posé l'alternative : câbler le rendu ou
retirer l'API morte. Retirer perd un travail protocolaire complet et
testé ; câbler coûte une couche de fusion au rendu. La question est
donc QUAND câbler, pas si.

## Décision proposée

**Câbler, en surcouche retardée — phase dédiée après la stabilisation
de la coloration syntaxique (feuille de route A1-A5), pas avant.**

1. La surcouche vit derrière le même contrat que le rattrapage :
   les jetons sémantiques forment une COUCHE au-dessus de la couleur
   syntaxique — ils raffinent (le paramètre devient « variable »), ne
   remplacent jamais (un jeton sémantique sans couleur syntaxique
   dessous ne s'affiche pas).
2. La fusion se paie à l'habillage (comme les issues de banc,
   D-05) : la couleur syntaxique reste la base, la teinte sémantique
   ne survit que sur les plages où l'éditeur a une preuve des deux.
3. La fraîcheur suit la même règle que tout le reste (PIEGE-20) :
   les jetons portent la version du document que le serveur a vu ;
   une publication sémantique périmée se rattrape par recalage ou
   s'ignore — jamais elle ne colore une révision plus récente.
4. Le retrait n'est pas une option : l'API protocolaire est complète,
   testée, et le coût de son silence est nul (rien ne la consomme
   aujourd'hui, elle n'abîme rien).

## Options

| Option | Effet | Retenue |
| --- | --- | --- |
| Retirer l'API | Perte d'un socle protocolaire testé ; à recâbler un jour depuis zéro | Non |
| Câbler maintenant | Concurrency avec les correctifs A1-A3 en cours de validation terrain ; la surcouche se construirait sur une base qui bouge | Non |
| **Câbler en surcouche, phase dédiée** | La base syntaxique se stabilise d'abord (A1-A5 + retour terrain) ; la surcouche se construit une fois, sur un contrat figé | **Proposée** |

## Compromis

- Les utilisateurs ne voient pas la couleur sémantique tout de suite :
  c'est le prix de la stabilisation d'abord. La surcouche est une
 fonctionnalité, pas une réparation — elle n'a pas l'urgence des A1-A3.
- La fusion à l'habillage ajoute un coût par rangée : mesuré au banc
  de charge avant adoption (le même que TokenCatchUp, payé une fois
  par publication sémantique, pas par image).

## Conséquences

- `SemanticTokensLsp` reste tel quel (aucun changement d'API).
- La feuille de route de la coloration gagne une étape B1
  (« surcouche sémantique ») qui ne démarre qu'après validation
  terrain de A1-A3.
- Le rendu actuel n'est pas touché : zéro risque porté par cette
  décision.

## Actions

- [ ] Validation du propriétaire (câbler en surcouche / autre chose).
- [ ] (si validée) Étape B1 posée à la feuille de route, phase dédiée.
- [ ] (si validée) Banc de fusion : jeton sémantique raffinant,
  jeton orphelin ignoré, jeton périmé recalé ou ignoré.
