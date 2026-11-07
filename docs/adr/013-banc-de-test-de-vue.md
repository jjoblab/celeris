# ADR-013 — Banc de test de vue : Robolectric sur JVM, appareils gérés sur CI

- **Statut** : Acceptée (jalon de redressement M7.5, 2026-11-07)
- **Date** : 2026-11-07

## Contexte

ADR-010 annonçait les tests de vue « en deux temps » — captures de
référence puis tests instrumentés — sans nommer l'outillage du premier
étage. Six jalons plus tard, le constat de M7.5 est sec : les quatre
défauts de la démonstration (pincement muet, gouttière instable,
scintillement de coloration, popups absents) vivent tous dans la couture
entre l'API Android et les modèles purs — exactement la couche qu'aucun
test n'exerce. Les modules Android n'ont que des tests de calculs purs ;
l'`InputConnection`, les `MotionEvent`, le `Canvas`, les densités et le
cycle de l'image n'ont jamais été rejoués automatiquement.

## Décision

1. **Robolectric 4.15.1** (Maven Central, vérifiée le 2026-11-07) entre
   comme unique dépendance de test de vue des modules Android
   (`android-view`, puis `android-widgets` et `demo`) : elle porte le
   cadre d'exécution, les jarres `android-all` instrumentées se résolvent
   par elle — aucune version supplémentaire au catalogue, la
   reproductibilité du build (ADR-011) tient.
2. **JUnit Jupiter** reste la plateforme (ADR-010) : Robolectric n'a
   pas d'extension Jupiter officielle à ce jour — ses tests se pilotent
   par le lanceur JUnit 4 (`@RunWith(RobolectricTestRunner.class)`) et
   roulent sur la plateforme Jupiter commune au dépôt par le moteur
   **vintage** (même train de version que Jupiter, 6.1.3) : mêmes
   tâches Gradle, mêmes rapports.
3. **`EditorTestRig`** (source de test de `android-view`) est le harnais
   partagé : une `EditorView` réelle mesurée et posée, un exécuteur
   d'analyse manuel (la publication ne part que sur ordre), un canvas
   enregistreur (chaque `drawText` avec sa couleur), un générateur de
   gestes multi-pointeurs et la vraie `InputConnection`. Les correctifs
   de M7.5 s'y servent tous.
4. **Appareils gérés Gradle** (image ATD, API 34) portent le deuxième
   étage dans `demo` : `androidTest` avec UiAutomator. La CI ouvre un
   travail `device-tests` (KVM des runners Ubuntu) ; il est nocturne et
   sur chaque fusion, hors du chemin critique. Là où l'environnement de
   développement n'a pas de virtualisation matérielle, les tests
   d'appareil restent compilants et documentés
   (`docs/qualite/VERIFICATION-APPAREIL.md`) — jamais présumés verts.
5. **Plancher de couverture** pour `android-view` et `android-widgets` :
   mesuré au jour de l'installation du banc (consigné dans
   `scripts/coverage-floor.txt`), vérifié par `scripts/check-coverage`
   en CI ; il ne descend jamais, il monte à chaque jalon.

## Options

| Option | Verdict | Raison |
| --- | --- | --- |
| Stubs maison des classes Android | Écartée | Réécrit le cadre à chaque API, ne prouve rien du vrai framework |
| Robolectric sur JVM | Retenue | Vrai framework instrumenté sur JVM, rapide, en CI à chaque commit |
| Instrumentation seule (appareil/émulateur) | Écartée comme unique étage | Lente, chère, aveugle au raisonnement géométrique interne |
| Capture d'écran en JVM (rendu natif) | Partielle | Robolectric ne pixelise pas ; le rendu natif rejoint l'étage appareil quand un pixel doit être comparé |

## Conséquences

- La boucle « test rouge → correctif → test vert » de M7.5 s'applique
  enfin à la vue elle-même, pas seulement aux calculs purs.
- Limite assumée (héritée d'ADR-010) : Robolectric ne reproduit pas
  fidèlement les claviers logiciels — l'`InputConnection` se teste
  directement, l'IME réel se vérifie à l'étage appareil.
- Le téléchargement des jarres `android-all` s'ajoute au premier tour de
  CI ; les tours suivants les servent du cache Gradle.
