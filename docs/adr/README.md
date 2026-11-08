# Décisions d'architecture (ADR)

Chaque décision consigne un choix structurant au format imposé par le
mandat : **Contexte, Décision, Options, Compromis, Conséquences, Actions**.

## Statuts

| Statut | Signification |
| --- | --- |
| Proposition | rédigée à M0, en attente de validation explicite du propriétaire |
| Acceptée | validée par le propriétaire ; ne change plus que par un ADR de remplacement |
| Remplacée | annulée par un ADR ultérieur ; conservée pour l'historique |

Les douze ADR, rédigées en proposition à M0, sont **acceptées** par le
propriétaire le 2026-11-04 (validation M0) ; les réponses aux questions
groupées sont consignées dans le résumé M0.

## Index

| ADR | Sujet | Statut |
| --- | --- | --- |
| [ADR-001](001-langage-et-outillage.md) | Langage et chaîne d'outils | Acceptée |
| [ADR-002](002-topologie-des-modules.md) | Topologie des modules et règles de dépendance | Acceptée |
| [ADR-003](003-modele-de-texte.md) | Modèle de texte et transactions | Acceptée |
| [ADR-004](004-analyse-syntaxique.md) | Analyse syntaxique incrémentale | Acceptée |
| [ADR-005](005-semantique-index-lsp.md) | Sémantique, index et client LSP | Acceptée |
| [ADR-006](006-rendu-et-budgets.md) | Rendu et budgets de performance | Acceptée |
| [ADR-007](007-modele-de-threading.md) | Modèle d'exécution et concurrence | Acceptée |
| [ADR-008](008-accessibilite-i18n-bidi.md) | Accessibilité, internationalisation et BiDi | Acceptée |
| [ADR-009](009-api-publique.md) | API publique et compatibilité binaire | Acceptée |
| [ADR-010](010-strategie-de-test.md) | Stratégie de test | Acceptée |
| [ADR-011](011-build-et-publication.md) | Build, publication et sécurité des dépendances | Acceptée |
| [ADR-012](012-contrats-ia-agents.md) | Contrats IA et outils d'agent | Acceptée |

## Règles

1. Un ADR est daté, signé du contexte qui l'a motivé et chiffré quand la
   décision se prête à la mesure.
2. Un principe imposé par le mandat (section « Principes d'architecture »)
   ne peut être contourné que par un ADR chiffré validé par le propriétaire.
3. Depuis M7.6, les projets et bibliothèques tiers se nomment en toutes
   lettres, avec version ou lien ; les dépendances reprises portent leur
   licence dans `NOTICE` et le registre de portage. Deux projets restent
   interdits (règle 7 du jalon) : leurs noms ne paraissent ni ici ni
   nulle part dans le dépôt.
