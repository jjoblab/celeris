# Feuille de route restante — les étapes après le 2026-11-12

> Inventaire honnête de tout ce qui reste, état vérifié au 2026-11-12
> (décision du propriétaire du 2026-11-11 : ce fichier dit les étapes
> restantes). La [`feuille-de-route.md`](feuille-de-route.md) garde
> l'historique complet des chantiers C1 à C14 et des jalons M0 à M9,
> avec le journal des clôtures ; le dépôt publie ses mises à jour au
> [`CHANGELOG.md`](../CHANGELOG.md).
>
> Le volume de tests n'y figure pas comme étape : la parité de volume
> avec l'audit du dépôt de départ ne se poursuit plus (décision du
> propriétaire du 2026-11-11) — les bancs continuent de naître au fil
> des chantiers, sans quota ; les cliquets font foi (couverture JaCoCo
> ≥ 90 % de lignes sur les modules purs, mutation ≥ 80 % sur
> `core-text`, plafonds Checkstyle/PMD, manifestes d'API vérifiés).

## Vue d'ensemble

| # | Étape | Statut | Ce qui bloque |
| --- | --- | --- | --- |
| 1 | Publication distante des douze artefacts (C1) | Clos le 2026-11-12 | douze artefacts publiés (les onze + `serveur-lsp-java`) — projet vierge vérifié, sommes conformes |
| 2 | Clôture de la 1.1.0 et tag `v1.1.0` | Clos le 2026-11-12 | — |
| 3 | Langages suivants : Go, Rust… (C5) | Go et Rust livrés le 2026-11-12 | la suite de la traîne dort sur le signal |
| 4 | Second étage appareil de vérification (C11) | Livré le 2026-11-12 | la CI exécute au nocturne — la recette manuelle du propriétaire reste |
| 5 | Archivage du dépôt de départ (C14) | Bloqué | décision du propriétaire ; poussée hors de ce bac à sable |
| 6 | Entretien continu (dependabot, montées de version) | Permanent | rien — veille hebdomadaire |

## 1 — Publication distante des onze artefacts (C1)

**Périmètre** : pousser les onze artefacts `jo.celeris:*` (huit modules
purs, trois bibliothèques Android en AAR depuis le 2026-11-11) vers le
registre de paquets de l'hébergeur (`https://maven.pkg.github.com/jjoblab/celeris`,
cible confirmée le 2026-11-04 — ADR-011) ; poser les tags (`v1.0.0` sur
le commit de la 1.0.0, `v1.1.0` à la clôture de l'étape 2) ; vérifier
la somme d'un artefact consommé depuis un projet vierge.

**Sortie** : la publication distante a réussi ; le tag existe ; un
projet vierge consomme `jo.celeris:core-text` (et l'AAR
`jo.celeris:android-view`) avec la somme vérifiée qui correspond.

**Blocage** : les identifiants du registre (`REGISTRY_USER`,
`REGISTRY_TOKEN`) viennent de l'environnement, jamais du dépôt — aucun
secret de publication ne sort du bac à sable.

**Source** : audit § 9, recommandation 1 ; § 6, gap 1.

## 2 — Clôture de la 1.1.0 et tag `v1.1.0`

**Périmètre** : l'entrée `[1.1.0]` du changelog est « en cours » — la
clôture la date, monte la version du dépôt (racine du build, bannière
de la cli et test cliquet suivent), pose le tag `v1.1.0` et publie
(avec l'étape 1). Livré depuis la 1.0.0 : les chantiers C2, C3, C4,
C6, C7, C8, C9, C10, C12 et C13 ; la publication des modules Android
en AAR ; un README par module et le racine honnête ; ADR-019.

**Sortie** : entrée datée, version montée, tag posé, publication
distante vérifiée.

**Blocage** : aucun côté dépôt — la date relève du propriétaire.

**Source** : [`CHANGELOG.md`](../CHANGELOG.md), entrée `[1.1.0]`.

## 3 — Langages suivants : Go, Rust… (C5)

**Périmètre** : la suite de la longue traîne par la voie des paquets de
données (ADR-017) — Go, Rust, puis le reste des vingt-neuf du dépôt de
départ ; grammaires du commerce reprises sans modification, épinglées
au NOTICE et au registre de portage, banc de conformité par langue.

**Sortie** : par langue — paquet épinglé, licence consignée, échantillon
rejoué avec déterminisme, ligne de matrice, banc de conformité étendu.

**Avancement** : Go et Rust livrés le 2026-11-12 (signal du propriétaire — « continue avec les étapes suivantes » — paquets `langages/go` et `langages/rust`, grammaires du commerce épinglées, bancs `PaquetGoIntegreTest` et `PaquetRustIntegreTest`, lignes de matrice et registre consignées). Vingt-quatre langages intégrés désormais — la suite de la traîne (vingt-sept langages du dépôt de départ) dort sur le signal.

**Blocage** : décision du propriétaire du 2026-11-10 — « on ajoutera
d'autres langues après » : le chantier dort jusqu'à son signal.

**Source** : audit § 9, recommandation 5 ; § 6, gap 4 ; vingt-quatre
langages intégrés à ce jour.

## 4 — Second étage appareil de vérification (C11)

**Périmètre** : exécuter le second étage de
[`qualite/VERIFICATION-APPAREIL.md`](qualite/VERIFICATION-APPAREIL.md) —
pincement, gouttière, flicker, popup sur vitre. C'est le seul étage
qui manque aux deux dépôts : la colonne « Vérifié appareil » de la
matrice est à zéro sur 125 lignes.

**Sortie** : rapport d'exécution consigné au document de vérification.

**Avancement** : livré le 2026-11-12 — les quatre bancs d'instrumentation
vivent dans `demo/src/androidTest` (`PinchZoomOnDeviceTest`,
`GutterStabilityOnDeviceTest`, `FlickerOnDeviceTest`,
`CompletionPopupOnDeviceTest`), l'appareil géré `pixel2api34` (image
ATD) les exécute, le travail CI `appareil.yml` court au nocturne, à
chaque fusion et au déclenchement manuel — rapports en artefact
`device-tests-reports`. Le plafond Checkstyle couvre le banc d'appareil
comme les bancs JVM ; la compilation se vérifie partout, l'exécution
demande la virtualisation matérielle — les runners Ubuntu la portent.
La colonne « Vérifié appareil » de la matrice se remplit par ces
passages nocturnes.

**Ce qui reste au propriétaire** : la recette manuelle de cinq minutes
sur l'appareil de référence
([`recette/RECETTE-M7.5.md`](../recette/RECETTE-M7.5.md), APK debug de
la CI en artefact) — le banc JVM ne remplace pas la vitre, le banc
d'appareil de la CI ne remplace pas la main du propriétaire.

**Source** : audit § 9, recommandation 9.

## 5 — Archivage du dépôt de départ (C14)

**Périmètre** : archiver le dépôt gelé (commit `f310a39`, gel déjà
acté par le registre de portage) avec bannière de migration vers
celeris — pour qu'aucun hôte ne sème sur une impasse.

**Sortie** : bannière visible sur le dépôt de départ ; le registre de
portage le consigne.

**Blocage** : décision du propriétaire ; la poussée se fait dans le
dépôt de départ, hors de ce bac à sable.

**Source** : audit § 9, recommandation 10 ; § 7.2.

## 6 — Entretien continu

**Périmètre** : dependabot veille chaque semaine (catalogue de versions
et actions GitHub — chantier C2) ; chaque montée de version traverse
les portes qualité, les tests de contrat l'accompagnent (AGP, Gradle,
dépendances — ADR-011 : la « gelée de versions » du dépôt de départ est
l'anti-pattern à bannir). Les révisions de sécurité se signalent par
courriel ([`SECURITY.md`](../SECURITY.md)), jamais d'issue publique.

**Sortie** : aucune — veille permanente, le changelog consigne chaque
montée.

**Source** : ADR-011 ; dependabot depuis le 2026-11-10.

## Ce qui ne reste pas (clos, pour mémoire)

Dix chantiers post-1.0 sont clos : C2 (hygiène), C3 (cohérence
documentaire), C4 (`USAGE.md`), C6 (raffinements de complétion), C7
(signature et gabarits riches), C8 (frappe fine), C9 (chrome de
l'éditeur), C10 (charge et limites), C12 (tout en popups) et C13
(deuxième vague — clôturé le 2026-11-11 : le volume de tests ne se
poursuit plus, décision du propriétaire consignée au
[`CHANGELOG.md`](../CHANGELOG.md)). La publication des modules Android
comme les purs (onze artefacts) et le README par module sont livrés le
2026-11-11. Les jalons M0 à M9 sont bouclés depuis la 1.0.0 du
2026-11-10.
