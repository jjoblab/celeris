# Vérification sur appareil — M7.5

> **État** : le second étage est POSÉ (C11, livré le 2026-11-12) — les
> quatre bancs d'instrumentation vivent dans `demo/src/androidTest`,
> l'appareil géré Gradle les exécute dans la CI (nocturne, à chaque
> fusion, au déclenchement manuel — [`appareil.yml`](../../.github/workflows/appareil.yml)),
> rapports en artefact. L'hôte de développement reste sans
> virtualisation matérielle : la vérification exécutoire vit à la CI,
> jamais présumée verte avant son premier passage.

## Ce qui est déjà prouvé sans appareil

Les quatre défauts du jalon sont couverts par des tests de VUE sur
JVM (Robolectric, rendu natif) — de vrais `MotionEvent` à deux
pointeurs, la vraie `InputConnection`, un vrai `Canvas` :

- pincement : `PinchZoomGestureTest` (14 → 28 sp par paliers, colonne
  sous le point focal stable, re-mesure à révision constante) ;
- gouttière : `GutterStabilityTest` (dixième ligne, marques, repli,
  retour à la ligne, deux images consécutives) ;
- coloration : `FlickerStabilityTest` (200 frappes sans aucune image
  décolorée, composition IME, mesure conservée à la publication) ;
- popups : `PopupLayerFocusTest` (PIEGE-27 — le focus ne quitte jamais
  l'éditeur).

## Le second étage, posé

1. **Dépendances** (catalogue, l'ajout documenté de l'ADR-013 pour
   l'étage appareil) : `androidx.test:runner`, `androidx.test.ext:junit`,
   `androidx.test.uiautomator:uiautomator`, dans le source set
   `androidTest` de `demo` — jamais dans les artefacts publiés.
2. **Appareil géré Gradle** (image ATD, API 34) dans `demo` :
   `pixel2api34` — l'image se télécharge pour la tâche d'instrumentation
   seule.
3. **Les bancs d'instrumentation**, un par défaut du jalon, dans
   `demo/src/androidTest/java/jo/celeris/demo` :
   - `PinchZoomOnDeviceTest` — UiAutomator pince réellement à deux
     pointeurs, la police logique passe aux paliers 28 puis 14 sp ;
   - `GutterStabilityOnDeviceTest` — captures avant/après la
     publication d'une analyse, l'abscisse de la première encre ne
     bouge pas d'un pixel, le décalage minimal reste sous le pixel ;
   - `FlickerOnDeviceTest` — trois captures pendant une frappe à
     analyse retenue (crochet de débogage) et une après publication,
     les mots non touchés gardent leurs pixels ;
   - `CompletionPopupOnDeviceTest` — le premier popup réel de
     complétion (le serveur de langage Java répond), l'éditeur garde
     le focus, le blanc referme la liste.

   La frappe des bancs vit par la voie exacte de l'IME (la connexion
   d'entrée de l'éditeur), le curseur porte en fin de document : les
   bandes mesurées (les premières lignes) restent jamais touchées.
4. **La commande exacte** (l'hôte sans virtualisation matérielle la
   laisse à la CI — les runners Ubuntu portent `/dev/kvm`) :
   ```sh
   ./gradlew :demo:pixel2api34DebugAndroidTest
   ```
5. **Le travail CI** `appareil.yml` : nocturne (`schedule`), à chaque
   fusion (`pull_request`, types `closed`, fusion vérifiée), au
   déclenchement manuel (`workflow_dispatch`) — rapports en artefact
   `device-tests-reports`, la porte `/dev/kvm` échoue à défaut
   d'accélération matérielle.

## Ce que la CI exécute, ce que l'hôte vérifie

La compilation du banc d'appareil se vérifie partout (le plafond
Checkstyle couvre `src/androidTest` comme les bancs JVM) ; son
EXÉCUTION demande l'appareil : la CI nocturne la porte, le rapport
part en artefact, la colonne « Vérifié appareil » de la matrice se
remplit par ces passages.

## La vérification manuelle du propriétaire, aujourd'hui

La recette manuelle [`RECETTE-M7.5.md`](../recette/RECETTE-M7.5.md)
couvre les quatre défauts sur un appareil branché : cinq minutes,
l'APK debug de la CI en artefact (`demo-debug-apk`) suffit.
