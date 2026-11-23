# Décisions d'architecture (ADR)

Chaque décision consigne un choix structurant au format imposé par le
mandat : **Contexte, Décision, Options, Compromis, Conséquences, Actions**.

## Statuts

| Statut | Signification |
| --- | --- |
| Proposition | rédigée à M0, en attente de validation explicite du propriétaire |
| Acceptée | validée par le propriétaire ; ne change plus que par un ADR de remplacement |
| Remplacée | annulée par un ADR ultérieur ; conservée pour l'historique |

Les ADR 001 à 013 (M0 à M7.5), rédigées en proposition à M0 ou en
cours de jalon, sont **acceptées** par le propriétaire (validation M0
le 2026-11-04, ADR-013 au jalon M7.5) ; les réponses aux questions
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
| [ADR-013](013-banc-de-test-de-vue.md) | Banc de test de vue (Robolectric) | Acceptée |
| [ADR-014](014-moteur-expressions-rationnelles.md) | Moteur d'expressions rationnelles des grammaires : joni | Proposition (spike M7.6) |
| [ADR-015](015-client-lsp-sur-lsp4j.md) | Client LSP sur LSP4J derrière une frontière | Acceptée (spike M7.6, validée le 2026-11-09) |
| [ADR-016](016-treesitter-niveau-2.md) | Analyse structurelle niveau 2 : tree-sitter écarté à M7.6 | Proposition (spike M7.6) |
| [ADR-017](017-plateforme-de-langages.md) | Plateforme de langages : paquets, modèle et coutures | Acceptée (jalon M7.6) |
| [ADR-018](018-interface-tout-en-popups.md) | Interface fonctionnelle : tout en popups | Acceptée (décision du propriétaire, 2026-11-10) |
| [ADR-019](019-plafond-releve-des-bancs.md) | Plafond relevé des bancs volumineux (600/800) | Acceptée (décision du propriétaire, 2026-11-11) |
| [ADR-020](020-niveau-2-natif-borne.md) | Niveau 2 natif borné (révision de l'ADR-016) | Acceptée (signal du propriétaire, 2026-11-13) |
| [ADR-0021](0021-modules-android-toujours-construits.md) | Les modules Android toujours construits (révision du critère M1) | Acceptée (décision du propriétaire, 2026-11-17) |
| [ADR-0022](0022-couche-integration.md) | La couche d'intégration : façade d'abord, consolidation ensuite (chantier B0) | Acceptée (mandat du propriétaire, 2026-11-17) |
| [ADR-0023](0023-nommage-api-publique.md) | Nommage de l'API publique — renommage anglais 2.0.0 | Acceptée (décision du propriétaire, appliquée le 2026-11-18) |
| [ADR-0024](0024-deux-voies-syntaxe-separees.md) | Deux voies de coloration séparées et choisissables (F.4) ; périmètre publié resserré aux modules de l'éditeur (F.7) | Acceptée (décision du propriétaire, 2026-11-18) |
| [ADR-0025](0025-treesitter-par-maven.md) | La voie tree-sitter consomme les artefacts Maven de la liaison Android (révision de l'ADR-0024 décision 1) | Acceptée (décision du propriétaire, 2026-11-18) |
| [ADR-0026](0026-fins-de-ligne-crlf.md) | Fins de ligne CRLF : le `\r` assumé dans la ligne rendue (feuille de route A3) | Acceptée (2026-11-19) |
| [ADR-0027](0027-coloration-semantique-lsp.md) | Coloration sémantique LSP : câbler en surcouche, phase dédiée (feuille de route A4) | Proposition (2026-11-19, décision propriétaire attendue) |
| [ADR-0028](0028-registre-des-descripteurs-dabord.md) | Registre des descripteurs d’abord, grammaires à la demande (chantier A de la coloration) | Acceptée (2026-11-22, prompt coloration — mesure avant/après au banc) |
| [ADR-0029](0029-cache-echecs-matchers-grammaires.md) | Cache d’échecs et matchers réutilisés dans le lexeur de grammaires (chantier B) | Acceptée (2026-11-22, prompt coloration — banc intercalé) |
| [ADR-0030](0030-ancres-fin-virtuelle-inclusions.md) | Ancres de document, fin de ligne virtuelle, inclusions externes et registre de la fabrique (chantier C) | Acceptée (2026-11-22, prompt coloration) |

| [ADR-0031](0031-requetes-surbrillance.md) | La couleur de la voie native vient des requêtes highlights.scm (chantier D) | Acceptée (2026-11-22, prompt coloration) |
| [ADR-0032](0032-fenetre-lexique-borne.md) | La fenêtre d’ouverture borne la lexicographie (retard résiduel 1-2 s) | Acceptée (2026-11-23, prompt du retour terrain) |
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
