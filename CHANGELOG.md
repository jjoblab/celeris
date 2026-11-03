# CHANGELOG

Tous les changements notables de ce projet sont consignés ici, par version,
en français. Format calqué sur « Keep a Changelog » ; versions suivant
SemVer. **Court par construction** : une version = une entrée ; les détails
vivent dans les ADR et le registre de portage, pas ici.

## [Non publié]

### Ajouté

- Validation M0 par le propriétaire (2026-11-04) : les douze ADR passent à
  l'état « Acceptée » ; les réponses aux questions groupées sont consignées
  dans le résumé M0 (question 5 — appareil de référence des budgets —
  ouverte).
- Fondations M1 : `build-logic` à plugins de convention, catalogue unique,
  squelette exécuté des dix modules et de la démonstration, cli, workflows
  CI épinglés par somme SHA avec validation du wrapper.
- Portes qualité bloquantes dès le premier commit : Checkstyle, PMD, Error
  Prone + NullAway (JSpecify), JaCoCo, ArchUnit, Jupiter/jqwik, mutation
  PIT au seuil de 80 % , scripts de politique (langue, neutralité).
- Manifeste d'API publique par module pur (`api/*.api`) vérifié dans
  `check` ; publication Maven locale complète avec SBOM et sommes SHA-256.
- `AGENT.md` (contrats d'agent) ; `NOTICE` consigne l'outillage et ses
  licences.

## [0.0.1] — 2026-11-04

### Ajouté

- Cadrage M0 : audit mesuré du dépôt de départ (`docs/audit/`), matrice de
  parité A–G (`docs/parite/`), ADR 001 à 012 en proposition, feuille de
  route chiffrée, glossaire.
- Spikes de mesure (rope re-mesurée, lexicographie incrémentale, rendu par
  proxy JVM) avec résultats consignés.
- Contrôles de politique de langue et de références externes, exécutables
  dès M0.

[0.0.1]: https://github.com/jjoblab/celeris/releases/tag/v0.0.1
