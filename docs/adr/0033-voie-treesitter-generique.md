# ADR-0033 — La voie tree-sitter générique par dépendances

Date : 2026-11-23
Statut : acceptée

## Contexte

Le retour terrain du 2026-11-23 : « l'option tree-sitter n'est pas
générique comme l'éditeur de référence — l'utilisateur doit pouvoir
choisir sa voie (textmate OU treesitter) et ses langages par
DÉPENDANCE, comme il le fait là-bas ». L'audit du module a confirmé
trois défauts :

1. `GrammaireNatif` était une ENUM fermée (json, python) : l'hôte ne
   pouvait PAS ajouter sa grammaire ;
2. les requêtes `highlights.scm` n'étaient JAMAIS lues en production
   (`installerRequete` n'était appelé que par les bancs), et la table
   de captures était GLOBALE statique (contamination entre langages) ;
3. le module embarquait deux grammaires en dur — l'hôte ne choisit
   rien, tout le monde paie json/python.

## Décision

Le modèle de l'éditeur de référence (« la grammaire suit la
dépendance ») s'applique :

1. **Le module ne porte que le châssis** : le cœur
   `android-tree-sitter` seul (implementation) — AUCUNE grammaire
   embarquée ; l'hôte ajoute les artefacts
   `com.itsaky.androidide.treesitter:tree-sitter-<langue>` qu'il
   veut servir. Chaque langue ne pèse QUE si on l'embarque.
2. **L'API publique d'enregistrement** :
   `TreeSitterGrammars.enregistrer(id, TSLanguageSupplier, requete)`
   — le fournisseur rend la classe de langue de la liaison
   (`TSLanguageJson::getInstance`…), la requête `highlights.scm`
   l'accompagne ; la source se pose au registre de la plateforme,
   la préférence « tree-sitter » du paquet se met à servir. Le
   chargement reste paresseux (première vue).
3. **La fabrique ne registre plus rien** : `TreeSitterPathFactory`
   fabrique le moteur, c'est tout — l'inscription automatique
   (json/python sans demande) est retirée, le choix vit chez l'hôte.
4. **Les requêtes vivent PAR GRAMMAIRE** : la table des noms →
   teintes se pose à la première passe du langage (lue par
   `LecteurRequetes`, sans liaison) — la contamination
   inter-langages est morte.
5. **La préférence globale** : `TreeSitterLanguages.preferTreeSitter
   (boolean)` déroge aux préférences déclarées des paquets (le
   sélecteur « toujours tree-sitter » de l'hôte devient RÉEL).
6. **La cohérence déclarative** : plus aucun paquet intégré ne
   déclare `preferred: tree-sitter` (le dépôt ne publie aucune
   grammaire — la préférence déclarée décrivait un mensonge : le
   repli servait en silence). json, python, javascript corrigés.

## Conséquences

- L'orthogonalité des deux voies se PROUVE : la voie native sans
  grammaires TextMate, les grammaires TextMate sans la voie native
  (le registre vide ne coûte rien, le repli déclaré sert).
- L'exemple embarque les SEPT langues publiées par la liaison
  (json, python, java, kotlin, xml, c, cpp) avec leurs requêtes
  officielles (MIT, attributions au NOTICE) ; la requête kotlin est
  écrite sur les nœuds de SA grammaire.
- `SyntaxeNatif.json()/python()` (fabriques fermées) et l'enum
  `GrammaireNatif` : retraits cassants consignés à MIGRATION.md
  (fenêtre zéro-consommateur, la 2.2.21).
- Le lecteur de requêtes gagne les LISTES ALTERNATIVES
  (`[ (a) (b) "c" ] @capture`) — la forme dominante des requêtes
  réelles — et le filet d'avancement (aucune boucle sur une
  requête mal parenthésée, la leçon du xml de référence).
