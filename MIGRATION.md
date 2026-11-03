# Migration — du dépôt de départ vers celeris

> Guide de migration pour les applications hôtes qui consomment le dépôt de
> départ. Les sections « correspondance des API » et « décisions de portage »
> sont d'ores et déjà réelles (issues de l'audit) ; la section « pas à pas »
> sera complétée à M9, quand l'API de celeris sera gelée en 1.0.

## 1. Correspondance des modules

| Dépôt de départ | celeris | Nature du changement |
| --- | --- | --- |
| `cel-core` (moteur, Android) | `core-text`, `core-syntax`, `core-language`, `core-intelligence` | éclatement en modules JVM purs ; le moteur ne dépend plus du SDK |
| `cel-ui` (vues) | `android-view` + `android-widgets` | refonte complète de la vue (plafond 600 lignes) ; kit de popups porté |
| `cel-lsp` (client LSP) | `core-lsp` | client réécrit sans dépendance tierce (ADR-005) |
| `cel-lsp-api` (SPI langage) | `core-language` | contrats portés, enrichis |
| `cel-textmate` (grammaires) | `core-syntax-grammars` | conservé ; les archives binaires versionnées deviennent des dépendances coordonnées |
| `demo` (démo) | `demo` | réécrite sur l'API publique uniquement |

## 2. Changements majeurs à prévoir

1. **Sélection** : le type mono-intervalle du dépôt de départ devient une
   liste de plages (multi-curseur) ; les appants de code qui construisaient
   une sélection à deux `int` migrent vers la fabrique de sélection multiple.
2. **Positions** : les `int` bruts deviennent des types forts
   (`DocOffset`, `LineIndex`) ; la confusion ligne/rangée/pixel devient une
   erreur de compilation au lieu d'un bug.
3. **Mutation** : les mutations directes du document (hors transaction)
   disparaissent ; tout passe par la `Transaction`, annulation comprise.
4. **Tokéniseurs** : les fournisseurs de langage qui implémentaient un
   tokéniseur manuel migrent vers l'arbre syntaxique ou la voie de
   grammaires.
5. **Artefacts** : coordonnées Maven `jo.celeris:*` en lieu et place de
   `jo.codeeditor:*` ; niveaux d'API minimum relevés de 24 à 26.

## 3. Ce qui ne change pas

- Le modèle mental : une vue Canvas unique, un document immuable par
  instantané, une voie de mutation unique.
- Les contrats de langage (`Language` et fournisseurs), portés et enrichis.
- Le comportement des popups unifiées (kit porté tel quel, tests inclus).
- La licence Apache-2.0 et l'absence de dépendance au runtime Kotlin.

## 4. Pas à pas de migration (à compléter à M9)

- [ ] Table de correspondance API publique (générée par le validateur de
      compatibilité, ADR-009) : dépôt de départ → celeris 1.0.
- [ ] Procédure de reprise des thèmes et gabarits personnalisés.
- [ ] Procédure de reprise des extensions de langage.
- [ ] Checklist de vérification des performances après migration.
