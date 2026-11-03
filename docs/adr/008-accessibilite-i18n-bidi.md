# ADR-008 — Accessibilité, internationalisation et texte bidirectionnel

- **Statut** : Proposition (M0)
- **Date** : 2026-11-04

## Contexte

L'audit du dépôt de départ mesure l'accessibilité absente de la vue
principale : `AccessibilityNodeInfo` n'apparaît dans aucun fichier Java,
`contentDescription` seulement dans la barre de recherche et le fil
d'Ariane ; aucun répertoire `values-fr/` (internationalisation réduite à un
seul `strings.xml`) ; aucune prise en charge du bidirectionnel ni des
groupes de graphèmes — alors que le cœur gère déjà l'Unicode. Le mandat
classe l'excellence en accessibilité comme avantage structurel (G-05) et
l'impose « dès la première version ».

## Décision proposée

1. **Arbre d'accessibilité virtuel** dans `android-view` :
   - les lignes visibles et les plages d'intérêt (diagnostic, pli,
     occurrence) sont des nœuds virtuels exposés par `AccessibilityNodeProvider` ;
   - navigation par caractère, mot, ligne et rangée ; actions de sélection
     et d'édition exposées ;
   - le curseur, la sélection et les popups annoncent leurs changements
     (événements d'accessibilité) ;
   - chaque composant interactif (gouttière, poignées, popups, barres) porte
     un nom, un rôle et des actions — testé par vérifications automatiques
     en CI (auditeur d'accessibilité) dès M3.
2. **Ressources `en` (défaut) et `fr` à 100 %** : toute chaîne affichée vit
   en ressources ; le contrôle de parité `values/` ↔ `values-fr/` casse la
   CI (script de politique de langue). La langue de la documentation reste
   le français ; la langue du code reste l'anglais (ADR-001).
3. **Texte bidirectionnel dans le cœur** (UAX #9) : le modèle expose la
   directionnalité de base par paragraphe ; le rendu et le déplacement du
   curseur suivent l'ordre visuel ; les tests embarquent du texte mixte
   latin/arabe/hébreu avec chiffres et ponctuation neutre.
4. **Groupes de graphèmes (UAX #29)** : les opérations « par caractère » de
   l'utilisateur (suppression, déplacement, sélection au double-toucher)
   opèrent sur les groupes, pas sur les unités UTF-16 ; corpus de tests :
   emoji (y compris composés par ZWJ), CJK, combinants, voyelles hindies.
5. **Contraste élevé** : thème dédié respectant les rapports de contraste
   recommandés ; le rendu s'appuie sur les couleurs résolues du thème, donc
   un seul chemin de dessin (pas de dérivés de thème dans les couches).
6. **Polices de repli** : chaîne de polices déclarée par thème ; mesure de
   police injectée (pas de lecture directe) pour que les tests restent
   déterministes.

## Options

| Option | Verdict | Raison |
| --- | --- | --- |
| A. Nœuds virtuels par l'existant (proposition) | **retenue** | échelle à des fichiers de 100 000 lignes sans créer de vue par ligne ; actions riches |
| B. Une vue native par ligne exposée au service d'accessibilité | écartée | coût mémoire prohibitif ; incohérent avec le rendu Canvas |
| C. Accessibilité différée à après 1.0 | écartée | le mandat classe G-05 en P0 ; coût démultiillé si différée |

## Compromis

- L'arbre virtuel demande une discipline de mise à jour (nœuds invalidés
  par plage de rangées, comme le rendu) : complexité modérée, partagée avec
  l'invalidation du rendu.
- La parité `en`/`fr` à 100 % ralentit chaque ajout de chaîne : assumé,
  contrôlé en CI.

## Conséquences

- La démo expose un scénario « lecture par lecteur d'écran » par catégorie
  de matrice, avec test instrumenté de fumée.
- Le corpus de tests Unicode est embarqué dans la démo (jeux d'essai du
  mandat) et partagé avec les tests JVM.
- La conformité sera auditée avant 1.0 (jalon M9) avec un auditeur
  automatique sur chaque scénario.

## Actions

- [ ] M3 : fournisseur de nœuds + événements ; auditeur automatique en CI.
- [ ] M3 : thèmes clair, sombre, contraste élevé ; `en` + `fr` complets.
- [ ] M2 : UAX #29 et UAX #9 dans `core-text` avec corpus embarqué.
