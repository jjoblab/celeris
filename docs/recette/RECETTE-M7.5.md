# Recette manuelle M7.5 — ce que le propriétaire rejoue dans la démo

> Cinq minutes sur un appareil (ou l'émulateur de son choix), avec
> l'APK debug produit par la CI (artefact `demo-debug-apk`). Chaque
> étape dit ce qu'on fait et ce qu'on doit VOIR — un écart se signale
> avec le numéro de l'étape.
>
> Ce que cette recette ne couvre pas encore : la preuve APPAREIL des
> budgets de rendu (aucun test d'instrumentation n'existe — le plan
> vit dans `docs/qualite/VERIFICATION-APPAREIL.md`), la surimpression
> de diagnostic et le réglage de lenteur d'analyse.

## 1. La gouttière ne bouge plus

1. Ouvrir la démonstration, puis un fichier Java de taille moyenne
   (par exemple l'exemple `exemple.java` d'une carte).
   **Vu** : le champ de numéros occupe au moins trois chiffres de
   large, le texte démarre à sa droite.
2. Se placer à la fin de la première ligne, appuyer sur Entrée dix
   fois de suite, en regardant le bord gauche du texte.
   **Vu** : le texte ne se décale JAMAIS d'un cran — la dixième ligne
   n'élargit rien (plancher de trois chiffres).
3. Ouvrir un fichier de plus de 1 000 lignes s'il s'en trouve un ;
   sinon rester : en couper un bloc entier par pli (chevron) puis le
   déplier.
   **Vu** : le repli et le dépliage ne décalent pas le texte.

## 2. Le pincement zoome pour de vrai

4. Pincer l'écran à deux doigts, franchement (écarter les doigts).
   **Vu** : la police monte par PALIERS visibles (14 → 16 → 18 → 20 →
   24 → 28), jamais en continu, jamais de vibration entre deux
   tailles.
5. Pincer l'écran au milieu d'une longue ligne, autour d'un mot
   précis, et regarder CE mot pendant le palier.
   **Vu** : le mot reste sous les doigts — la colonne affichée ne
   saute pas d'un cran à l'autre (gouttière recalculée comprise).
6. Resserrer pour redescendre ; la police redescend par paliers
   jusqu'à 10 si on insiste. Relâcher.
   **Vu** : rien ne casse, le défilement reste borné au contenu.

## 3. La coloration ne clignote plus

7. Ouvrir un fichier Java coloré, poser le curseur AU MILIEU d'un mot
   coloré de la ligne (par exemple dans un identifiant), et taper
   lentement cinq caractères, un par seconde, en regardant LES AUTRES
   mots de la ligne.
   **Vu** : les autres mots ne clignotent jamais — le mot-clé garde sa
   couleur, le nombre garde la sienne, du premier au dernier
   caractère.
8. Taper vite une rafale de vingt caractères.
   **Vu** : la ligne ne passe jamais en couleur unique (encre de
   base) ; la couleur du mot tapé peut mûrir à la publication, jamais
   disparaître.
9. Avec le clavier logiciel : composer un mot (composition IME,
   caractères surlignés par le clavier) sur la même ligne.
   **Vu** : les mots voisins restent colorés pendant toute la
   composition.

## 4. Le raccourci et la persistance

10. Clavier matériel branché (ou le clavier de l'émulateur) :
    Ctrl + plus, Ctrl + moins, Ctrl + 0.
    **Vu** : un palier exact par frappe, retour à la taille par défaut
    sur Ctrl + 0.
11. Molette avec Ctrl (souris) sur un écran large.
    **Vu** : un palier par cran de molette, le point sous le curseur
    reste en place.

## 5. Les dix popups se jouent (la reprise)

12. Menu de l'atelier, « Compléter le mot » : la liste s'ouvre au
    curseur, les mots du CODE répondent au préfixe — ceux des chaînes
    jamais. Taper une lettre de plus : la liste FILTRE sans se
    fermer ; la documentation du premier candidat connu suit sa
    liste.
13. Menu, « Intentions au curseur » : le menu s'ouvre au curseur —
    l'appui exécute lint, aperçu, diff, agent ou IA.
14. Sélectionner un mot : la BARRE de sélection naît au-dessus —
    couper, copier, coller, tout sélectionner s'exécutent ; la frappe
    la fait céder, le défilement la laisse vivre.
15. Placer le curseur sur une ligne marquée par le lint (après
    « Analyser (lint) ») : l'INFO-BULLE s'ouvre au curseur, la BANDE
    d'erreurs vit au bord droit — l'appui sur un cran saute au
    diagnostic le plus proche.
16. Le FIL D'ARIANE se lit en haut de la couche : le chemin du
    symbole englobant suit le curseur, entrée par entrée.
17. Menu, « Survoler le symbole » sur un appel connu : la
    documentation arrive PAR LE FIL du serveur de langage en
    processus — titre et sections distincts.
18. Placer le curseur DANS un appel connu (par exemple dans
    « ajouter(1, 2) », après la virgule) : l'AIDE AUX PARAMÈTRES
    s'ouvre, le paramètre COURANT souligné par le tronçon exact.
19. Menu, « Rechercher » : le champ tient le focus, la couche
    l'interdit aux popups (PIEGE-27) ; l'état se coordonne — fermer
    referme l'état, pas le champ.
20. Défiler pendant que la complétion et le fil d'Ariane sont
    ouverts : les popups se REPOSITIONNENT sans se fermer.

## 6. La frappe intelligente vit dans la démo (le branchement)

21. Dans un fichier Java, taper `(` au clavier (logiciel ou
    matériel) : la PAIRE se pose, le curseur vit AU MILIEU.
    **Vu** : `()` — le fermant tapé ensuite SE SAUTE (pas de double).
22. Sélectionner un mot puis taper `(` : le mot se retrouve ENTOURÉ,
    la sélection porte toujours le contenu.
23. Clavier matériel, Entrée juste après l'accolade ouvrante d'une
    classe : les DEUX SAUTS indentés — le fermant descend seul sous
    le curseur.
24. Retour arrière juste entre une paire vide `()` : les DEUX
    caractères partent ensemble.
25. Coller un bloc multiligne au milieu d'une classe : les lignes
    suivantes s'alignent sur l'indentation d'accueil.
26. Menu : « Insérer le gabarit de méthode » (le premier trou vit
    sélectionné), « Formater le document » (l'indentation se
    redresse, tout s'annule d'un geste), « Renommer le symbole » (le
    nouveau nom se demande, les usages ENTIERS suivent, les conflits
    se disent), « Ajouter l'import List » (la ligne se pose sous le
    paquet, le doublon se refuse).

## 7. La démonstration vérifie ses scénarios

27. Écran d'accueil : le troisième compteur « Vérifiés » lit
    `12/12`. L'appui liste les verdicts ligne à ligne — chaque
    scénario vérifié rejoue ses gestes par le fil réel de la
    démonstration, un écart s'y verrait.

## 8. Ce qui reste ouvert (le propriétaire le verra)

- Aucune preuve APPAREIL : tous les statuts « Démontré » de la
  matrice se prouvent au banc (JVM) et dans la démo — le premier
  appareil dira le reste (voir VERIFICATION-APPAREIL).
- La surimpression de diagnostic (taille de police, largeur de
  gouttière, compteur d'images décolorées) n'est pas livrée : le
  compteur vit au banc (`FlickerStabilityTest`).
- Le réglage de lenteur d'analyse n'est pas livré : la retenue de la
  publication par l'exécuteur manuel du banc joue ce rôle dans les
  preuves.
