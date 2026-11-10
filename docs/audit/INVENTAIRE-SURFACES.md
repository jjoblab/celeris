# Inventaire des surfaces fonctionnelles — chantier C12 (ADR-018)

> **Consigné le 2026-11-10** (chantier C12 de la feuille de route,
> décision ADR-018 du propriétaire). Toute surface fonctionnelle de la
> démonstration et du kit se statue ici : sa forme d'avant, son
> verdict, sa forme d'après. La règle : trois formes permises — feuille
> du bas, feuille du haut, popup ancrée — jamais d'écran plein cadre
> ni de dialogue système pour une fonctionnalité. L'hôte reste maître
> de son écran (ADR-009).

## Le kit livré par le chantier

`android-widgets` gagne les deux formes qui manquaient :

| Pièce | Rôle |
| --- | --- |
| `Feuille` | le chrome commun : poignée de glissement (aire tactile du kit, pilule muette à l'accessibilité — la maquette la porte `aria-hidden`), rangée de titre, bouton de fermeture, corps défilant, voile, encarts |
| `FeuilleBas` | la feuille du bas — surfaces denses : 660 dp au plus, 76 % de la hauteur au plus, coins hauts arrondis |
| `FeuilleHaut` | la feuille du haut — rappels et revues : 760 dp au plus, coins bas arrondis, calée sous la barre d'état |
| `PopupTokens` | les jetons des feuilles : rayons, largeurs, part de hauteur, cibles tactiles (48 dp), seuil et durée du glissement |

Les quatre fermetures tiennent sur les deux formes : bouton, voile,
glissement au-delà du seuil, touche retour (`surRetour()`). La
fermeture logique est immédiate — le rappel de fermeture et l'état ne
dépendent d'aucune horloge. La feuille peut prendre le focus : elle
porte des champs, elle vit hors de la couche — PIEGE-27 ne la concerne
pas (le piège interdit à la COUCHE de voler le focus de l'éditeur).
Onze tests de vue (Robolectric) couvrent le kit : pose, fermetures,
cibles, cantonnage, glissement, thème, muabilité de la poignée.

Maquettes de référence : [`docs/maquettes/popups.html`](../maquettes/popups.html).

## Les surfaces migrées

| # | Surface | Avant | Verdict | Après |
| --- | --- | --- | --- | --- |
| 1 | Réglages (six interrupteurs, curseur, paquet jouet H-04) | activité plein cadre (`SettingsActivity`) | non conforme — ADR-018 cite « paramètres » pour la feuille du bas | `FeuilleReglages` : feuille du bas, depuis l'accueil et l'éditeur ; l'écran disparaît du manifeste |
| 2 | Verdicts du banc de la démonstration | dialogue système (`MaterialAlertDialogBuilder`) | non conforme — proscrit | feuille du bas, lignes en chasse fixe |
| 3 | Recherche (A-20, facette barre) | barre dépliable dans le layout de l'activité | non conforme — ADR-018 cite « recherche/remplacement » pour la feuille du bas | `AtelierRecherche` : feuille du bas, le champ tient le focus ; l'état reste coordonné chez l'atelier des popups (M7.5, §7) |
| 4 | Plan du fichier | panneau latéral inline (`OutlineView` à 45 %) | non conforme — ADR-018 cite « plan » pour la feuille du bas | `FeuillePlan` : feuille du bas, l'entrée choisie y saute et la referme |
| 5 | Renommage de symbole (B-19, B-11) | dialogue système à champ | non conforme — le champ doit tenir le focus, la feuille le porte hors de la couche | feuille du bas dans `AtelierRefactor` |
| 6 | Revue du diff (D-12) | dialogue système par bloc | non conforme — proscrit | feuille du haut dans `AtelierDiff`, chaque bloc se décide sur la même feuille |
| 7 | Revue d'agent (F-04) | dialogue système par bloc | non conforme — la maquette porte la revue d'agent en feuille du haut | feuille du haut dans `AtelierAgent` : intention, lignes, légende des points de contrôle, trois décisions |
| 8 | Rappel de consentement IA (F-10) | dialogue système | non conforme — proscrit | feuille du haut dans `AtelierIA`, l'action ouvre la feuille des réglages |
| 9 | Décision de prédiction (F-02) | dialogue système | non conforme — proscrit | feuille du haut dans `AtelierIA` : description, question, application explicite |
| 10 | Aperçu de définition (D-11) | dialogue système | non conforme — « au point d'intérêt » | popup ancrée au curseur (`DocAncre.auCurseur`, la carte de documentation du kit) |
| 11 | Rendu délégué d'un repère `@Preview` (C-11) | dialogue système | non conforme — « au point d'intérêt » | popup ancrée au coin de la gouttière, la colonne des repères (`DocAncre.auCoin`) |
| 12 | Verdicts de prédiction (appliquée, refusée, aucune) | dialogues système | non conforme — des états, pas des surfaces | canal du verdict → barre d'état de l'hôte (chrome), comme le verdict de refactorisation |

## Les surfaces statuées conformes

| Surface | Forme | Verdict |
| --- | --- | --- |
| Accueil de la démonstration (`MainActivity`, cartes de fichiers) | activité hôte | conforme — le conteneur Android l'exige (ADR-018 : « La démonstration garde son activité hôte ») ; les cartes choisissent le fichier, c'est la navigation de l'hôte (ADR-009) ; le catalogue de scénarios proprement dit est une donnée (`ScenarioCatalog`), lue par le compteur d'accueil |
| Atelier d'édition (`EditorActivity`) | activité hôte de l'éditeur | conforme — le conteneur embarque la vue (`EditorView`), c'est le mandat d'embarquable |
| Barres d'application et menus | chrome de l'hôte | conforme — l'hôte possède sa navigation ; le voile d'une feuille couvre le menu, aucune surface ne s'ouvre sous une feuille |
| Les dix popups de la couche (M7.5, §7) | popups ancrées | conforme — complétion, documentation, signature, intentions, survol, info-bulle, fil d'Ariane, recherche (état), barre de sélection, menu de débordement |
| Bande d'erreurs (D-08) | bord droit de la scène | conforme — ornement de l'éditeur au même titre que la gouttière, il vit avec les diagnostics et saute au cran le plus proche |
| Gouttière, barre d'état, surimpression (M7.5, §9) | chrome de l'éditeur | conforme — passifs ou ornementaux, aucune n'est une surface fonctionnelle ouvrable |

## Le compte des preuves

- Aucun `AlertDialog`, `DialogFragment`, `BottomSheetDialog` ni
  `PopupWindow` ne vit dans les sources (la Javadoc de `PopupLayer`
  l'interdit en l'écrivant) ; aucune navigation plein cadre ne reste
  hors des deux activités hôtes.
- `./gradlew check` vert, SDK Android présent : les 11 tests de vue
  des feuilles, la lin Android (bloquante, zéro ligne de base),
  Checkstyle (plafonds 600/50), PMD et la ligne de base de langue
  comprises.
- `:demo:assembleDebug` livre l'APK de la démonstration migrée.
