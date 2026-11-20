# docs/ — où vit quoi

> Orientation du dossier documentaire (feuille de route des
> améliorations, D2). La règle simple : **les décisions vivent dans
> les ADR, l'avancement dans les feuilles de route, les preuves dans
> les dossiers thématiques, les journaux clos à l'historique.**

| Endroit | Rôle | Exemples |
| --- | --- | --- |
| [`adr/`](adr/) | Les décisions d'architecture, numérotées, avec leur contexte et leurs conséquences | `0022-couche-integration.md`, `0023-nommage-api-anglais.md` |
| Feuilles de route (`feuille-de-route-*.md`) | Les chantiers EN COURS : statuts, journal, prochaines étapes — un chantier terminé range sa feuille à l'historique | [`feuille-de-route-ameliorations.md`](feuille-de-route-ameliorations.md), [`feuille-de-route-coloration.md`](feuille-de-route-coloration.md) |
| [`audit/`](audit/) | Les preuves de revue : pièges hérités, registre de portage, états des lieux | `PIEGES-HERITES.md`, `REGISTRE-PORTAGE.md` |
| [`parite/`](parite/) | La matrice de parité avec le dépôt de départ (lue par `check-parity-matrix`) | `MATRICE-PARITE.md` |
| [`qualite/`](qualite/) | Les bornes de la chaîne qualité : plafonds, cliquets, seuils | — |
| [`rapports/`](rapports/) | Les rapports ponctuels d'analyse (un sujet, une date) | `B4-arrete-hote.md` |
| [`recette/`](recette/) | Les scénarios de recette et leurs captures | — |
| [`maquettes/`](maquettes/), [`simulateur/`](simulateur/), [`simulateur-web/`](simulateur-web/) | Les supports de conception : maquettes, simulateurs | — |
| [`historique/`](historique/) | Les journaux clos — chantiers TERMINÉS (M0 à M9, popups) : on les relit, on ne les tient plus à jour | `RESUME-M*.md`, `POPUP_*.md` |
| Racine (`GLOSSAIRE.md`, `REFERENCES.md`, `renommages-code-anglais.md`) | Les références stables qui traversent les chantiers | — |

## Où chercher quoi

- **Pourquoi le code est ainsi** → l'ADR du numéro cité dans la Javadoc ou le commentaire (le registre `adr/README.md` indexe).
- **Où en est un chantier** → SA feuille de route ; si elle n'est plus à la racine, le chantier est clos (historique) — l'état fait vit au `CHANGELOG.md` du dépôt.
- **Un piège à éviter en touchant le code** → `audit/PIEGES-HERITES.md` (chaque piège y garde son test de garde au corps).
- **Ce qui reste à faire** → les feuilles de route ouvertes, et les « décisions attendues » de chacune.

## Les feuilles de route à la racine

| Feuille | Objet | État |
| --- | --- | --- |
| [`feuille-de-route.md`](feuille-de-route.md) | L'historique des chantiers C1-C14 et jalons M0-M9 (1.0.0) — journal de clôtures | Clos (1.0.0, 2026-11-10) — garde son rôle d'historique de référence |
| [`feuille-de-route-popups.md`](feuille-de-route-popups.md) | La mission popups (audit, restructuration, unification) | Close (phase 9, 2026-11-13) — suite livrée au fil des sessions |
| [`feuille-de-route-restante.md`](historique/feuille-de-route-restante.md) | L'inventaire des étapes restantes d'avant la 2.0.0 | Clos (2.0.0, 2026-11-19) — rangée à l'historique par D2 |
| [`feuille-de-route-coloration.md`](feuille-de-route-coloration.md) | Les corrections de la coloration (A1-A5) | A1-A5 closes ; A6 (moteur externe) attend la décision du propriétaire |
| [`feuille-de-route-ameliorations.md`](feuille-de-route-ameliorations.md) | Les améliorations en cours : réactivité (C), documentation (D), packages (P) | VIVANTE — C1, C2, D1, P1 faits ; D2, P2-P6 en cours |
