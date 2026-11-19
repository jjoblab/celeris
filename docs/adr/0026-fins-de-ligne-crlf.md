# ADR-0026 — Fins de ligne CRLF : le `\r` assumé dans la ligne rendue

- **Statut** : Accepté (2026-11-19 — feuille de route A3 de la
  fiabilisation de la coloration ; l'audit interne du 2026-11-19 a
  posé la question, le dépôt tranche ici)
- **Date** : 2026-11-19

## Contexte

L'index de lignes du modèle de texte ne compte que `\n` (`Document.computeLineStarts`
et le splice incrémental). Dans un fichier à fins CRLF, le `\r` reste
donc dans la ligne rendue : `lineText(0)` de « `int a;\r\n` » finit par
`\r` (longueur 7). Le lexeur de la famille C le tient pour un blanc —
aucun jeton ne le couvre, la coloration est correcte — mais toute
couche qui compte des colonnes (IME, sélection, bornes visuelles) voit
un caractère invisible de plus en fin de ligne.

L'audit a chiffré les deux voies : (a) retirer le `\r` de la ligne
rendue — cohérent avec la convention du lexeur (« la ligne ne contient
jamais le saut de ligne final »), mais toucher `lineText` change le
contrat du modèle de texte, les offsets de colonnes de TOUTES les
couches, et le re-calcul des sélections sauvegardées ; (b) l'assumer
et le documenter — aucun code ne bouge, la convention existante
 (« `lineEnd` rend l'offset du caractère de saut de ligne ») devient
explicite : le séparateur visuel d'un CRLF commence au `\r`.

## Décision

**La voie (b) est retenue : le `\r` reste dans la ligne rendue.**

- `Document.lineText` rend la ligne SANS le `\n` final, `\r` compris
  s'il précède ce `\n` — le séparateur d'une fin CRLF commence au `\r`.
- `lineEnd` rend l'offset du `\n` : le `\r` est le dernier caractère
  du CONTENU de la ligne, pas un séparateur pour l'index.
- Le lexeur (voies maison et grammaires) tient le `\r` pour un blanc :
  aucun jeton ne le couvre, la couleur est exacte.
- Les couches à colonnes (IME, sélection, carets) comptent le `\r`
  comme un caractère de largeur rendue par la police — le même
  traitement que tout blanc de fin de ligne.

## Options

| Option | Effet | Retenue |
| --- | --- | --- |
| (a) Retirer le `\r` de `lineText` | Contrat du modèle changé : offsets, colonnes, plages sauvegardées, lexeurs, LSP (UTF-16 des serveurs) tous recalés ; risque élevé pour un gain cosmétique | Non |
| (b) Assumer et documenter | Aucun code ne bouge ; la convention s'écrit une fois pour toutes ; le comportement rejoint celui des éditeurs de référence (le curseur ne se place pas entre `\r` et `\n` — le guard existe déjà côté IME) | **Oui** |

## Compromis

- Une colonne « fantôme » en fin de ligne CRLF pour les couches qui
  comptent : le `\r` a une largeur de rendu (le plus souvent nulle ou
  quasi — glyphe de contrôle), la sélection qui l'inclut est visible
  comme un espace insécable de plus. C'est le comportement d'origine
  du dépôt, aucun retour terrain ne l'a signalé.
- La voie (a) reste ouverte par un ADR de remplacement si un hôte
  exige des colonnes strictes sans le `\r` — elle se paierait alors
  d'une refonte coordonnée du modèle et des couches.

## Conséquences

- Le contrat de `lineText` / `lineEnd` se documente dans la Javadoc du
  modèle de texte (le seul changement de code de cet ADR).
- Les bancs de lexicographie n'ont rien à changer : le `\r` est un
  blanc, l'audit l'a prouvé au banc (aucune divergence de jetons sur
  un document CRLF).
- Les futurs consommateurs d'offsets LSP restent sur l'UTF-16 du
  document TEL QU'EL — les serveurs comptent pareil.

## Actions

- [x] Javadoc du modèle de texte : la convention du `\r` écrite.
- [x] Banc de cohérence : un document CRLF lexicographié sans aucune
  divergence de jetons (l'audit du 2026-11-19 l'a exécuté hors dépôt ;
  le banc le fige au dépôt).
- [ ] (ouvert) ADR de remplacement si un hôte exige la voie (a).
