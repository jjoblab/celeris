# Vérification sur appareil — M7.5

> **État** : NON EXÉCUTÉE dans la fenêtre M7.5 — l'environnement de
> développement ne dispose d'aucun émulateur (pas de virtualisation
> matérielle) ni d'appareil branché. Aucun test d'instrumentation
> (`androidTest`) n'existe encore dans le dépôt : le second étage du
> banc (ADR-013, décision 4) est spécifié ici, prêt à se poser à la
> reprise — jamais présumé vert.

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

## Le plan du second étage, à la reprise

1. **Dépendances** (catalogue, ADR-013 déjà accepté pour l'étage JVM ;
   l'étage appareil demande son propre ajout documenté) :
   `androidx.test:runner`, `androidx.test.ext:junit`,
   `androidx.test.uiautomator:uiautomator`, dans le source set
   `androidTest` de `demo`.
2. **Appareil géré Gradle** (image ATD, API 34) dans `demo` :
   ```kotlin
   android {
       testOptions {
           managedDevices {
               devices {
                   pixel2api34(com.android.build.api.dsl.ManagedVirtualDevice) {
                       device = "Pixel 2"
                       apiLevel = 34
                       systemImageSource = "aosp-atd"
                   }
               }
           }
       }
   }
   ```
3. **Les tests d'instrumentation**, un par défaut du jalon :
   - `PinchZoomOnDeviceTest` — `UiAutomator` pince réellement
     (`UiObject2.pinch`), la taille de police passe par paliers ;
   - `GutterStabilityOnDeviceTest` — capture d'écran avant/après la
     première analyse, l'abscisse du premier caractère ne bouge pas
     d'un pixel ;
   - `FlickerOnDeviceTest` — trois captures pendant une frappe à
     analyse ralentie (crochet de débogage), les mots non touchés
     gardent leurs pixels ;
   - `CompletionPopupOnDeviceTest` — premier popup réel, le focus
     reste à l'éditeur.
4. **La commande exacte** (l'hôte sans émulateur la laisse à la CI —
   les runners Ubuntu ont la virtualisation matérielle) :
   ```sh
   ./gradlew :demo:pixel2api34DebugAndroidTest
   ```
5. **Le travail CI** `device-tests` : nocturne (`schedule`) et sur
   chaque fusion (`pull_request`, types `closed`), rapports en
   artefact. Il ne sera déclaré qu'avec les tests du point 3 — un
   travail vide ne prouverait rien.

## La vérification manuelle du propriétaire, aujourd'hui

Sans émulateur, la recette manuelle `docs/recette/RECETTE-M7.5.md`
couvre les quatre défauts sur un appareil branché : cinq minutes, le
APK debug de la CI en artefact (`demo-debug-apk`) suffit.
