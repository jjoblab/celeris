# Résumé M8.6 — jalon de redressement (2026-11-08)

> **Déclenchement** : le constat du propriétaire dans la démonstration —
> « la largeur du popup de complétion trop petite, il n'a pas une
> taille raisonnable », « tu dois aussi faire la même chose pour le
> toolbar de sélection avec le menu overflow même design », et l'exécution des serveurs de langage en processus devait devenir sûre. Périmètre : le plancher de largeur de la complétition, le menu de débordement de la barre de sélection au design du kit, et les tubes mémoire des serveurs en processus.
>
> **État** : les DEUX livrables sont en place, `./gradlew check` vert sur l'ensemble (politique comprise), l'APK de démonstration se construit, et les serveurs de langage passent par la porte commune de session sur tubes à file.

## La complétion raisonnable

Le dépôt de départ fixait la largeur de sa liste à 280 dp ; le portage
l'en avait fait un plafond adaptatif — à libellés courts, la carte
rapetissait à l'indécence. Le plancher `LARGEUR_MIN_LISTE_DP` (240 dp)
rejoint le plafond : la carte vit entre les deux, la rangée deux
lignes — badge de genre, libellé, identité qualifiée — y respire
toujours.

## Le menu de débordement de la barre de sélection

Le motif de la plateforme, au design du kit du dépôt de départ :
couper, copier, coller tiennent les pastilles ; tout le reste vit
derrière la PUCE DE DÉBORDEMENT — carré arrondi au rayon du kit (6 dp),
teinté à vingt pour cent de l'encre du texte, trois points dessinés au
cercle (aucune dépendance de glyphe), teinte de sélection à l'appui.
L'appui prévient l'hôte, qui pose le menu : la même carte que le menu
d'intentions (`MenuDebordement`), ancrée sous la puce par l'ancre
public `ancrePuceDebordementX()`. La machine à états (`PopupController`)
porte le régime — `SELECTION_OVERFLOW` n'ouvre pas sans sa barre, ne
lui survit pas, cède à la frappe avec elle, se repositionne au
défilement.

## Les tubes mémoire des serveurs en processus

- **Tubes mémoire** (`TubesMemoire`) : les tubes du JDK couplent la lecture à la VIVACITÉ du fil écrivain — un serveur répond depuis ses fils de piscine éphémères, leur mort tuait le fil de lecture du client (« Write end dead »), les réponses mouraient dans le tampon. Une file bloquante de trames remplace la filature : la lecture attend des octets ou la fermeture, jamais la mort de l'écrivain.
- **L'interface `LanguageSession`** : le serveur de référence (M7.5) et tout serveur déclaré passent par la même porte — ouvrir, synchroniser, signature, survol, complétion, diagnostics (`EtatDiagnostics` : gouttière, bande d'erreurs, barre d'état).
- **La fermeture sans `exit`** : l'implémentation d'un serveur réel peut y éteindre la machine virtuelle — fatale au processus hôte sur appareil. Les transports se ferment, la piscine s'arrête, les fils démons meurent avec le processus.

## Le constat du banc d'intégration

Le banc d'intégration a fait tomber une vraie panne du lecteur : la documentation de complétion n'était lue qu'en chaîne nue —
le protocole sert aussi du CONTENU BALISÉ `{kind, value}` (et des
tableaux de contenus). `ElementCompletion` décode désormais les trois
formes. C'est cela, tester à fond : le monde réel répond différemment
des traces.

## Vérification

- `ListeCompletionTest` : les trois formes de documentation, contrat du
  protocole.
- `PopupRegimesTest`/`PopupSurfacesTest`/`PopupViewsTest` : les régimes
  du menu de débordement, la puce, le plancher de complétion.
- `./gradlew check` vert, `:demo:assembleDebug` vert.
