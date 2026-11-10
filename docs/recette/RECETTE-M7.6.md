# Recette M7.6 — la plateforme de langages pas à pas

> Recette manuelle pour le propriétaire : chaque étape se joue dans la
> démonstration (`:demo:assembleDebug`, installer l'APK) et dit son
> résultat attendu. Ce qui ne se joue pas encore est nommé avec sa
> raison. Durée : quinze minutes.

## 1. Ouvrir les langages du registre (H-01, H-02, H-03)

1. Depuis l'accueil, ouvrir un fichier d'exemple de chaque famille :
   `exemple.java` (analyseur écrit), `exemple.py` (grammaire),
   `exemple.xml` (serveur déclaré), `exemple.md`.
2. **Attendu** : la barre d'état dit le nom affichable du langage
   (pas l'extension), la pastille du fichier porte le badge du paquet
   (deux lettres, sa couleur), la coloration apparaît à l'ouverture.
3. Ouvrir `build.gradle.kts` : **attendu** — langage `kts` (le parent
   `kotlin` lui prête ses analyseurs, la famille se lit).
4. Ouvrir un fichier sans extension reconnue : **attendu** — pas de
   langage, encre de base.

## 2. La configuration pilotée par les données (H-02)

1. Dans `exemple.py`, taper `#` en début de ligne puis du texte :
   **attendu** — commentaire coloré (préfixe du paquet, pas de code).
2. Sélectionner deux lignes, action « commenter » : **attendu** — le
   préfixe se pose sur chacune, l'action inverse le retire.
3. Dans `exemple.json`, tenter « commenter » : **attendu** — l'action
   est absente (le paquet ne déclare pas de commentaire de ligne).
4. Dans `exemple.py`, taper une ouvrante `(` : **attendu** — la
   fermante se pose (paire du paquet).

## 3. Le plan par données et les plis (H-08)

1. Ouvrir `exemple.py`, regarder le panneau du plan : **attendu** —
   les fonctions et classes s'y listent, sans une ligne de Java dédié
   au langage (les règles `outline.json` du paquet).
2. Replier un bloc `def` au chevron de gouttière : **attendu** — le
   pli par indentation se ferme, le document se rouvre sans perte.
3. Ouvrir `exemple.yaml` : **attendu** — plis par indentation aussi.

## 4. Les thèmes par portées et l'import `tokenColors` (H-07)

1. Menu ⋮ → « Thème » : **attendu** — le libellé dit « Clair ».
   Basculer quatre fois : Clair → Sombre → Contraste élevé →
   **Atelier (importé)**.
2. Sur un fichier Python ouvert, en thème Atelier : **attendu** — les
   commentaires en italique crème, les mots-clés pourpre foncé, les
   chaînes vert profond (les couleurs viennent du fichier
   `themes/atelier.json` lu par l'importeur, la palette en dérive).
3. En thème Sombre, ouvrir le même fichier : **attendu** — mêmes
   catégories, encres sombres (la résolution passe par la PILE de
   portées du jeton, la palette par sorte ne sert plus que de repli).
4. **Note** : le thème importé se charge au premier basculement (hors
   fil interface ensuite) — un léger dégelui de moins d'une seconde
   n'est pas une panne.

## 5. Le serveur tiers déclaré en données (H-13, H-11, H-12)

1. Ouvrir `exemple.xml` (DTD valide incluse) : **attendu** — la
   complétion au curseur propose les éléments de la DTD documentés,
   le survol d'une déclaration rend sa documentation.
2. Corriger une balise (par exemple fermer `</exemples>` au lieu de
   `</exemple>`) : **attendu** — la bande d'erreurs porte un cran, la
   gouttière l'encre d'erreur (diagnostics POUSSÉS par le serveur
   réel, monté par sa déclaration `servers.json` — aucun nom de
   serveur dans le code).
3. Menu des intentions sur un attribut : **attendu** — symboles,
   plis et formatage du serveur passent par la porte de session.
4. Ouvrir `exemple.java` : **attendu** — le REPLI répond : le serveur
   de référence (signature, survol), la complétion par les mots du
   code — la vue ne dit pas qui a répondu.

## 6. Le paquet jouet de l'utilisateur (H-04) — NON JOUABLE

Le chargeur de dossiers et d'archives vit au noyau avec sa sécurité
(zip-slip, liens symboliques, tailles, profondeur — `PackLoaderTest`),
mais la démonstration n'offre pas encore d'écran pour copier un paquet
au stockage et le charger à chaud : l'étape ne se joue pas. Vérification
de substitution : `./gradlew :core-language:test --tests PackLoaderTest`.

## 7. Le repli natif (H-09) — ÉCARTÉ

Le niveau 2 (tree-sitter) est écarté à M7.6 (ADR-016 : les budgets
mesurés ne tiennent pas). Rien à jouer ; la case reste déclarée dans
les paquets, révisable par ADR.

## 8. Les langages incorporés (H-06) — NON JOUABLE

Le mécanisme (inclusions externes par registre, injections) est livré
et testé, mais aucun paquet intégré ne déclare d'incorporation : pas
de HTML avec script ni de Markdown avec blocs de code colorés à
l'ouverture. Workitem futur.

## 9. Vérifications transverses

1. `./gradlew check` : **attendu** — vert (politique comprise).
2. `python3 scripts/check-language-registration` : **attendu** —
   zéro littéral d'identifiant hors des paquets et de
   l'enregistrement.
3. `python3 scripts/check-language-baseline` : **attendu** — 344
   identifiants figés, le rempart tient (jamais au-dessus).
