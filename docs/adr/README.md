# Décisions d'architecture (ADR)

Chaque décision consigne un choix structurant au format imposé par le
mandat : **Contexte, Décision, Options, Compromis, Conséquences, Actions**.

## Statuts

| Statut | Signification |
| --- | --- |
| Proposition | rédigée à M0, en attente de validation explicite du propriétaire |
| Acceptée | validée par le propriétaire ; ne change plus que par un ADR de remplacement |
| Remplacée | annulée par un ADR ultérieur ; conservée pour l'historique |

## Index

| ADR | Sujet | Statut |
| --- | --- | --- |
| [ADR-001](001-langage-et-outillage.md) | Langage et chaîne d'outils | Proposition |
| [ADR-002](002-topologie-des-modules.md) | Topologie des modules et règles de dépendance | Proposition |
| [ADR-003](003-modele-de-texte.md) | Modèle de texte et transactions | Proposition |
| [ADR-004](004-analyse-syntaxique.md) | Analyse syntaxique incrémentale | Proposition |
| [ADR-005](005-semantique-index-lsp.md) | Sémantique, index et client LSP | Proposition |
| [ADR-006](006-rendu-et-budgets.md) | Rendu et budgets de performance | Proposition |
| [ADR-007](007-modele-de-threading.md) | Modèle d'exécution et concurrence | Proposition |
| [ADR-008](008-accessibilite-i18n-bidi.md) | Accessibilité, internationalisation et BiDi | Proposition |
| [ADR-009](009-api-publique.md) | API publique et compatibilité binaire | Proposition |
| [ADR-010](010-strategie-de-test.md) | Stratégie de test | Proposition |
| [ADR-011](011-build-et-publication.md) | Build, publication et sécurité des dépendances | Proposition |
| [ADR-012](012-contrats-ia-agents.md) | Contrats IA et outils d'agent | Proposition |

## Règles

1. Un ADR est daté, signé du contexte qui l'a motivé et chiffré quand la
   décision se prête à la mesure.
2. Un principe imposé par le mandat (section « Principes d'architecture »)
   ne peut être contourné que par un ADR chiffré validé par le propriétaire.
3. Tout identifiant de projet tiers est remplacé par une référence neutre
   `[EXT-n]` ; les dépendances réellement déclarées sont nommées dans les
   fichiers de build et dans `NOTICE`, jamais dans la prose.
