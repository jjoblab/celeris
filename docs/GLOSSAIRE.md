# Glossaire — terminologie de référence

> Termes retenus pour toute la documentation, les commentaires et les
> interfaces rédigées en français. Un terme absent de ce glossaire ne doit
> pas être francisé de manière improvisée : on l'ajoute ici d'abord.
> Les identifiants de code restent anglais ; la correspondance se fait dans
> la Javadoc.

## Termes de l'éditeur

| Terme source | Terme retenu | Note |
| --- | --- | --- |
| caret | curseur | le point d'insertion (par opposition à la sélection) |
| cursor (text) | curseur | homonymie acceptée ; « pointeur » désigne la souris |
| multi-cursor | multi-curseur | plusieurs curseurs simultanés, sélection multiple |
| gutter | gouttière | bande des numéros de ligne, marques et plis |
| fold | pli | repli d'une plage de lignes ; « déplier » l'inverse |
| sticky lines | lignes collantes | lignes d'en-tête maintenues visibles au défilement |
| soft wrap | retour à la ligne souple | sans couper le fichier en lignes logiques |
| visual row | rangée visuelle | ligne telle qu'affichée (plis + retour souple appliqués) |
| document line | ligne du document | ligne logique du fichier ; jamais confondue avec la rangée |
| breadcrumb | fil d'Ariane | chemin du symbole courant dans la hiérarchie |
| ghost text | texte fantôme | complétion en ligne proposée, non encore acceptée |
| inline hint (inlay) | indice en ligne | type ou nom de paramètre inséré dans le texte |
| popup | popup | conservé : aucun équivalent français usuel satisfaisant |
| completion | complétion | suggestion de fin de mot ou d'instruction |
| snippet | gabarit | expansion de texte à trous ; les trous sont des variables |
| live template | gabarit dynamique | gabarit déclenché par un raccourci |
| keymap | table de raccourcis | correspondance touches → commandes |
| theme | thème | jeu de couleurs et de polices de l'éditeur |
| ligature | ligature | glyphe fusionné (par ex. « ≠ » rendu « != ») |
| viewport | fenêtre d'affichage | plage de rangées actuellement visibles |
| magnifier | loupe | loupe de précision au doigt |
| handle (selection) | poignée | poignée de sélection tactile |
| pill | pilule | pastille d'actions flottante du mode tactile |

## Termes du modèle de texte

| Terme source | Terme retenu | Note |
| --- | --- | --- |
| rope | rope | structure d'arbre équilibré ; l'article est admis en français technique |
| snapshot | instantané | état immuable du document, lisible depuis tout fil |
| transaction | transaction | ensemble immuable et inversible de changements |
| offset | décalage | position en unités UTF-16 ; type fort `DocOffset` |
| line index | index de lignes | table des débuts de lignes ; type fort `LineIndex` |
| undo / redo | annulation / rétablissement | jamais « undo » seul dans la prose |
| coalescing | coalescence | fusion des frappes successives en une seule opération d'annulation |
| grapheme cluster | groupe de graphèmes | unité perçue par l'utilisateur (UAX #29) |
| BiDi | bidirectionnel | algorithme UAX #9 |
| surrogate pair | paire de substitution | encodage UTF-16 des caractères hors BMP |
| BOM | marque d'ordre des octets | conservé : sigle universel |

## Termes d'analyse et d'intégration

| Terme source | Terme retenu | Note |
| --- | --- | --- |
| syntax tree | arbre syntaxique | concret, sans perte, tolérant aux erreurs |
| tokenizer | tokéniseur | analyse lexicale seule ; remplacé par l'arbre quand il existe |
| parse | analyser | l'analyse syntaxique produit l'arbre |
| incremental | incrémental | re-traitement limité à ce qui a changé |
| LSP | LSP | Language Server Protocol : norme citée comme norme |
| SPI | SPI | interface de service : point de branchement de l'hôte |
| provider | fournisseur | contrat de langage implémenté par module |
| diagnostic | diagnostic | erreur, avertissement ou information rattaché à une plage |
| quick fix | correctif rapide | action de correction proposée sur un diagnostic |
| intention | intention | action de refactorisation proposée sans diagnostic |
| inspection | inspection | analyse statique productrice de diagnostics |
| refactoring | refactorisation | jamais « refactoring » dans la prose française |
| symbol index | index de symboles | table persistante des symboles du projet |
| VCS | VCS | système de gestion de versions ; nom de norme |
| blame | blame | annotation par auteur de ligne ; conservé faute d'usage établi |
| build | build | conservé : « construction » prête à confusion |
| fuzz | fuzz | génération aléatoire d'entrées ; l'oracle est la référence naïve |
| oracle | oracle | implémentation naïve de référence pour les tests de propriété |
| jank | image manquée | image dont la durée dépasse le budget ; « saut » pour l'effet visible |
| frame budget | budget d'image | 16 ms à 60 Hz, 8 ms à 120 Hz |
| cliquet | cliquet | seuil de qualité qui ne peut que se resserrer |
| SBOM | SBOM | nomenclature des composants logiciels |
| télémétrie | télémétrie | toujours en option explicite (opt-in) |
