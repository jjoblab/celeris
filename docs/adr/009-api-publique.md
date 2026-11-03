# ADR-009 — API publique et compatibilité binaire

- **Statut** : Acceptée (validée par le propriétaire le 2026-11-04)
- **Date** : 2026-11-04

## Contexte

Le dépôt de départ vit le problème à l'échelle : `EditorView` expose 494
membres publics et les scellés `@RestrictTo` (310 annotations) créent une
API « publique pour le compilateur, interne par convention » — invérifiable
et donc réellement publique malgré elle (constat n° 3 de l'audit). Le mandat
impose une API publique minimale et stable, vérifiée en CI dès la 0.1.

## Décision proposée

1. **Interne par défaut** : classes de visibilité paquet ; tout package dont
   le nom contient `internal` est interne ; l'API publique est un ensemble
   **fermé et listé** (par module : `core-text`, `core-syntax`,
   `core-language`, `core-intelligence`, `core-lsp`, `core-ai`,
   `android-view`, `android-widgets`, `android-host`) documenté dans un
   fichier de manifeste d'API.
2. **Nullité par annotations JSpecify** (domaine public, spécification
   ouverte) vérifiée par NullAway et Error Prone en CI ; les annotations
   font partie de l'API et donc du contrat de compatibilité.
3. **Validateur de compatibilité binaire** en CI dès la 0.1.0 : comparaison
   de l'API publique de la version publiée contre la tête de `main` ;
   toute rupture hors version majeure casse la CI.
4. **SemVer strict** : 0.x autorise les ruptures à chaque version mineure
   (pré-1.0), 1.0 les interdit hors majeure ; dépréciations avec
   `forRemoval` et échéance (deux versions mineures minimum), consignées au
   CHANGELOG et dans la Javadoc (`@since` porte la version d'entrée).
5. **Types forts en surface** (ADR-003) : les positions de l'API publique
   sont `DocOffset`/`LineIndex`, jamais `int` nus ; les rappels
   (listeners) reçoivent des instantanés, jamais des objets mutables.
6. Le manifeste d'API et la Javadoc française complète sont **générés** en
   CI (aucun chiffre ni liste saisi à la main) ; une classe publique sans
   Javadoc française conforme casse la construction (politique de langue).

## Options

| Option | Verdict | Raison |
| --- | --- | --- |
| A. Interne par défaut + manifeste d'API généré | **retenue** | petit périmètre défendable ; compatibilité vérifiable mécaniquement |
| B. Tout public + contrats dans la doc | écartée | la situation mesurée du dépôt de départ : 494 membres publics pour une seule classe |
| C. Modules Java scellés (`module-info`) comme seule frontière | écartée | incompatible avec la consommation Android historique ; garde-fou complémentaire au moment voulu, pas fondation |

## Compromis

- Le manifeste d'API et le validateur ont un coût d'outillage à M1 ;
  en échange, chaque release est un contrat vérifié, pas une promesse.
- « Interne par défaut » rallonge les chemins d'appel (passer par l'API
  plutôt que lire un champ) : cohérent avec la composition imposée par
  l'ADR-002.

## Conséquences

- La démo ne compile qu'avec l'API publique : c'est la preuve continue de
  la suffisance de l'API (règle d'or du mandat, vérifiable en CI par un
  test de dépendances de la démo).
- Un consommateur peut suivre les versions par diff d'API publié à chaque
  version.
- L'API publique est petite dès la 0.1 : mieux vaut agrandir (mineure)
  que déprécier.

## Actions

- [x] M1 : choisir et intégrer le validateur de compatibilité binaire (tâches
      apiManifest/apiCheck sur sortie javap normalisée) ; manifeste d'API
      généré pour chaque module pur.
- [x] M1 : JSpecify + NullAway + Error Prone actifs, `-Werror`.
- [ ] M2 : geler l'API de `core-text` en 0.1.0 pour amorcer le cliquet.
