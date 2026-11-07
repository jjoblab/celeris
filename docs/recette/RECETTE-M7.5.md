# Recette manuelle M7.5 — ce que le propriétaire rejoue dans la démo

> Cinq minutes sur un appareil (ou l'émulateur de son choix), avec
> l'APK debug produit par la CI (artefact `demo-debug-apk`). Chaque
> étape dit ce qu'on fait et ce qu'on doit VOIR — un écart se signale
> avec le numéro de l'étape.
>
> Ce que cette recette ne couvre pas encore : les popups (aucun n'est
> branché à la démo — la couche existe, les vues suivent), la
> complétion, la barre de sélection et le réglage de lenteur
> d'analyse : non livrés, consignés au résumé M7.5.

## 1. La gouttière ne bouge plus

1. Ouvrir la démonstration, puis un fichier Java de taille moyenne
   (par exemple l'exemple `exemple.java` d'une carte).
   **Vu** : le champ de numéros occvre au moins trois chiffres de
   large, le texte démarre à sa droite.
2. Se placer à la fin de la première ligne, appuyer sur Entrée dix
   fois de suite, en regardant le bord gauche du texte.
   **Vu** : le texte ne se décale JAMAIS d'un cran — la dixième ligne
   n'élargit rien (plancher de trois chiffres).
3. Ouvrir un fichier de plus de 1 000 lignes s'il s'en trouve un ;
   sinon rester : en couper un bloc entier par pli (chevron) puis le
   déplier.
   **Vu** : le repli et le dépliage ne décalent pas le texte.

## 2. Le pincement zoome pour de vrai

4. Pincer l'écran à deux doigts, franchement (écarter les doigts).
   **Vu** : la police monte par PALIERS visibles (14 → 16 → 18 → 20 →
   24 → 28), jamais en continu, jamais de vibration entre deux
   tailles.
5. Pincer l'écran au milieu d'une longue ligne, autour d'un mot
   précis, et regarder CE mot pendant le palier.
   **Vu** : le mot reste sous les doigts — la colonne affichée ne
   saute pas d'un cran à l'autre (gouttière recalculée comprise).
6. Resserrer pour redescendre ; la police redescend par paliers
   jusqu'à 10 si on insiste. Relâcher.
   **Vu** : rien ne casse, le défilement reste borné au contenu.

## 3. La coloration ne clignote plus

7. Ouvrir un fichier Java coloré, poser le curseur AU MILIEU d'un mot
   coloré de la ligne (par exemple dans un identifiant), et taper
   lentement cinq caractères, un par seconde, en regardant LES AUTRES
   mots de la ligne.
   **Vu** : les autres mots ne clignotent jamais — le mot-clé garde sa
   couleur, le nombre garde la sienne, du premier au dernier
   caractère.
8. Taper vite une rafale de vingt caractères.
   **Vu** : la ligne ne passe jamais en couleur unique (encre de
   base) ; la couleur du mot tapé peut mûrir à la publication, jamais
   disparaître.
9. Avec le clavier logiciel : composer un mot (composition IME,
   caractères surlignés par le clavier) sur la même ligne.
   **Vu** : les mots voisins restent colorés pendant toute la
   composition.

## 4. Le raccourci et la persistance

10. Clavier matériel branché (ou le clavier de l'émulateur) :
    Ctrl + plus, Ctrl + moins, Ctrl + 0.
    **Vu** : un palier exact par frappe, retour à la taille par défaut
    sur Ctrl + 0.
11. Molette avec Ctrl (souris) sur un écran large.
    **Vu** : un palier par cran de molette, le point sous le curseur
    reste en place.

## 5. Ce qui reste ouvert (le propriétaire le verra)

- Aucun popup (complétition, documentation, aide aux paramètres,
  barre de sélection) ne s'ouvre : la couche qui les porte est posée
  (`PopupLayer`, `PopupController`), les vues elles-mêmes ne sont pas
  livrées — c'est le premier chantier de la reprise.
- La surimpression de diagnostic (taille de police, largeur de
  gouttière, compteur d'images décolorées) n'est pas livrée : le
  compteur vit pour l'instant dans le banc (`FlickerStabilityTest`).
