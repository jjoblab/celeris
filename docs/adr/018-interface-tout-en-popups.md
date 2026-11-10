# ADR-018 — Interface fonctionnelle : tout en popups

- **Statut** : Acceptée (décision du propriétaire du 2026-11-10)
- **Date** : 2026-11-10

## Contexte

celeris est une bibliothèque embarquable : l'hôte fournit l'écran, la
bibliothèque fournit des surfaces d'édition. Depuis M5, le module
`android-widgets` livre popups et barres unifiées — thémées,
accessibles selon l'ADR-008, testées par les tests de vue de la CI.
Mais aucune règle n'interdisait jusqu'ici à une fonctionnalité d'ouvrir
un écran plein cadre ou un dialogue système, et la démonstration porte
des surfaces plein cadre (écran de réglages, catalogue de scénarios).

Le propriétaire arrête le principe le 2026-11-10 : pour les
fonctionnalités des chantiers en cours, à venir et existantes, il ne
veut que des popups — feuilles surgissantes basses et hautes, popups
ancrées.

## Décision

**Toute surface fonctionnelle est une popup ; jamais un écran plein
cadre, jamais un dialogue système.** Trois formes permises, toutes
dans `android-widgets` :

1. la **feuille du bas** (bottom sheet) — surfaces denses :
   paramètres, plan, recherche/remplacement, palette de commandes ;
2. la **feuille du haut** (top sheet) — rappels et barres d'outils
   contextuelles ;
3. la **popup ancrée** — au point d'intérêt : complétion, diagnostics,
   aide aux paramètres.

Sont proscrits pour une fonctionnalité : activité ou fragment plein
cadre, dialogues système (`AlertDialog` et semblables), tiroirs de
navigation fonctionnels. L'hôte reste maître de sa navigation
(ADR-009) ; la bibliothèque rend des surfaces, elle n'impose pas
d'écran. La démonstration garde son activité hôte — le conteneur
Android l'exige — mais ses surfaces fonctionnelles migrent en feuilles.

## Options

| Option | Verdict | Raison |
| --- | --- | --- |
| Écrans plein cadre par fonctionnalité | écartée | vole l'écran de l'hôte, contraire au mandat d'embarquable |
| Dialogues système | écartée | thèmes, accessibilité et BiDi hors de contrôle (ADR-008) |
| Popups et feuilles seules | **retenue** | prouvé depuis M5 (kit unifié de `android-widgets`), thèmes et accessibilité maîtrisés, cohérent avec l'API publique |

## Compromis

- Moins d'espace par surface : les contenus denses (réglages riches,
  navigateur) s'agencent en volets à l'intérieur de la feuille, ou en
  feuilles enchaînées — jamais en écran.
- La feuille du haut n'a pas encore d'implémentation : elle naîtra à
  son premier besoin, dans le kit unifié, avec ses tests de vue.
- Les surfaces plein cadre existantes de la démonstration doivent
  migrer : un chantier dédié de la feuille de route les inventorie et
  les ramène à la conformité.

## Conséquences

- `android-widgets` reste l'unique foyer des surfaces fonctionnelles.
- Toute ligne d'interface nouvelle de la matrice de parité cite le
  présent ADR dans sa définition de « terminé ».
- La conformité se vérifie en revue et par les tests de vue ;
  l'ADR-008 (étiquetage, tailles tactiles) s'applique aux trois formes.

## Actions

- [x] Consigner la décision (le présent ADR, le 2026-11-10).
- [ ] Inventorier les surfaces fonctionnelles existantes (la
      démonstration en tête) et migrer les non-conformes — chantier
      dédié de la feuille de route.
- [ ] Livrer la feuille du haut à son premier besoin, avec ses tests
      de vue.
