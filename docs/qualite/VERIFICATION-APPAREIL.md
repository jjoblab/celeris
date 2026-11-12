# Vérification sur appareil — M7.5

> **État** : le second étage est POSÉ ET EXÉCUTÉ VERT (C11, livré et
> passé le 2026-11-12 — premier passage vert au déclenchement manuel,
> le nocturne le rejoue) — les quatre bancs d'instrumentation vivent
> dans `demo/src/androidTest`, l'appareil géré Gradle les exécute dans
> la CI ([`appareil.yml`](../../.github/workflows/appareil.yml)),
> rapports en artefact. L'hôte de développement reste sans
> virtualisation matérielle : l'exécution vit à la CI, jamais présumée
> verte avant son passage.

## Ce qui est déjà prouvé sans appareil

Les quatre défauts du jalon sont couverts par des tests de VUE sur
JVM (Robolectric, rendu natif) — de vrais `MotionEvent` à deux
pointeurs, la vraie `InputConnection`, un vrai `Canvas` :

- pincement : `PinchZoomGestureTest` (14 → 28 sp par paliers, colonne
  sous le point focal stable, re-mesure à révision constante) ;
- gouttière : `GutterStabilityTest` (dixième ligne, marques, repli,
  retour à la ligne, deux images consécutives) ;
- coloration : `FlickerStabilityTest` (200 frappes sans aucune image
  décolorée, composition IME, mesure conservée à la publication) ;
- popups : `PopupLayerFocusTest` (PIEGE-27 — le focus ne quitte jamais
  l'éditeur).

## Le second étage, exécuté vert

1. **Dépendances** (catalogue) : `androidx.test:runner`,
   `androidx.test.ext:junit`, `androidx.test.uiautomator:uiautomator`
   (2.3.0), dans le source set `androidTest` de `demo` — jamais dans
   les artefacts publiés.
2. **Appareil géré Gradle** : `pixel2api34` (image ATD, API 34,
   `localDevices` sous AGP 9).
3. **Les bancs d'instrumentation**, un par défaut du jalon, dans
   `demo/src/androidTest/java/jo/celeris/demo` — les voies réelles
   quatorze passages d'émulateur ont éprouvées :
   - `PinchZoomOnDeviceTest` — les mêmes `MotionEvent` que le banc
     JVM (doigts verticaux, cadence de 16 ms, distribution à la vue),
     envergures CALIBRÉES au-dessus de l'envergure minimale de mise à
     l'échelle du détecteur (environ 200 dp : l'engagement ne vient
     qu'au-delà) — 14 puis 28 puis 14 sp, les paliers de PIEGE-24 ;
   - `GutterStabilityOnDeviceTest` — la vue SE DESSINE en bitmap (le
     vrai Skia de l'appareil, la vraie mise en page — le compositeur
     de l'image ATD demeure muet, son GPU v37 rejette les options
     d'AGP : les défauts du jalon vivent dans le rendu de l'éditeur,
     pas dans la composition de la vitre) ; l'abscisse de la première
     encre et le décalage minimal restent sous le pixel à la
     publication d'une analyse, PIEGE-25 ;
   - `FlickerOnDeviceTest` — quatre rendus de la vue pendant une
     frappe à analyse retenue (le crochet de débogage) et après
     publication : les mots non touchés gardent leurs pixels,
     PIEGE-26 ;
   - `CompletionPopupOnDeviceTest` — le premier popup réel de
     complétion (le serveur de langage Java répond par la voie
     asynchrone), le focus ne quitte jamais l'éditeur, le blanc
     referme la liste, PIEGE-27.
4. **La commande exacte** (l'hôte sans virtualisation matérielle la
   laisse à la CI — les runners Ubuntu portent `/dev/kvm`) :
   ```sh
   ./gradlew :demo:pixel2api34DebugAndroidTest
   ```
5. **Le travail CI** `appareil.yml` : nocturne (`schedule`), à chaque
   fusion (`pull_request`, types `closed`, fusion vérifiée), au
   déclenchement manuel (`workflow_dispatch`) — rapports en artefact
   `device-tests-reports`, le journal des échecs imprime les XML de
   résultats (messages d'assertion, aperçu ASCII de la capture,
   mesures du détecteur, état du système). La règle udev `99-kvm4all`
   ouvre `/dev/kvm` au runner ; la config de l'AVD porte
   `swiftshader_indirect` (le rendu logiciel).

## Les leçons de l'image ATD, consignées pour la reprise

- l'arbre d'accessibilité de l'image ne porte pas la vitre : les
  recherches d'UiAutomator reviennent vides — les gestes vivent par la
  distribution directe à la vue, la géométrie venant des bornes
  mesurées ;
- l'écran s'éveille muet : `wakeUp` et `svc power stayon` précèdent
  les mesures, sinon la politique d'entrée jette les touchers ;
- le détecteur d'échelle n'engage le pincement qu'au-delà de
  l'envergure minimale de mise à l'échelle (environ 200 dp) : les
  envergures du banc JVM (120 à 520 px) frôlent le seuil sans
  l'atteindre — les envergures du banc d'appareil se calibrent
  au-dessus (300 à 1300, 1100 à 480) ;
- la voie de commandes n'interprète pas les tubes : les sorties de
  dumpsys se filtrent en Java.

## La vérification manuelle du propriétaire, aujourd'hui

La recette manuelle [`RECETTE-M7.5.md`](../recette/RECETTE-M7.5.md)
couvre les quatre défauts sur un appareil branché : cinq minutes,
l'APK debug de la CI en artefact (`demo-debug-apk`) suffit — la vitre
de référence reste la main du propriétaire.
