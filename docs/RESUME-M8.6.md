# Résumé M8.6 — jalon de redressement (2026-11-08)

> **Déclenchement** : le constat du propriétaire dans la démonstration —
> « la largeur du popup de complétion trop petite, il n'a pas une
> taille raisonnable », « tu dois aussi faire la même chose pour le
> toolbar de sélection avec le menu overflow même design », et « pour
> tester à fond la fonctionnalité de LSP, je veux que tu recherches un
> serveur LSP pas trop gros (quelques Mo, 3-6 Mo) que tu incluras dans
> le module démo, à lancer en in process ». Périmètre : le plancher de
> largeur de la complétion, le menu de débordement de la barre de
> sélection au design du kit, et le serveur de langage XML réel
> embarqué.
>
> **État** : les TROIS livrables sont en place, `./gradlew check` vert
> sur l'ensemble (politique comprise), l'APK de démonstration se
> construit avec le serveur embarqué, et le banc JVM exécute le
> serveur RÉEL de bout en bout.

## La complétion raisonnable

Le dépôt de départ fixait la largeur de sa liste à 280 dp ; le portage
l'en avait fait un plafond adaptatif — à libellés courts, la carte
rapetissait à l'indécence. Le plancher `LARGEUR_MIN_LISTE_DP` (240 dp)
rejoint le plafond : la carte vit entre les deux, la rangée deux
lignes — badge de genre, libellé, identité qualifiée — y respire
toujours.

## Le menu de débordement de la barre de sélection

Le motif de la plateforme, au design du kit du dépôt de départ :
couper, copier, coller tiennent les pastilles ; tout le reste vit
derrière la PUCE DE DÉBORDEMENT — carré arrondi au rayon du kit (6 dp),
teinté à vingt pour cent de l'encre du texte, trois points dessinés au
cercle (aucune dépendance de glyphe), teinte de sélection à l'appui.
L'appui prévient l'hôte, qui pose le menu : la même carte que le menu
d'intentions (`MenuDebordement`), ancrée sous la puce par l'ancre
public `ancrePuceDebordementX()`. La machine à états (`PopupController`)
porte le régime — `SELECTION_OVERFLOW` n'ouvre pas sans sa barre, ne
lui survit pas, cède à la frappe avec elle, se repositionne au
défilement.

## Le serveur de langage réel embarqué

La recherche (banc de candidats JVM purs : un serveur de scripts à
~12 Mo, un serveur d'intégration à ~69 Mo, d'autres natifs impossibles
en processus) retient le serveur XML [EXT-6], EPL-2.0, identité et
provenance au `NOTICE` : Java 11, lancé par son API sur des flux
quelconques, riche (complétion documentée, survol, définition,
références, diagnostics de validation, symboles, plis).

- **Découpe** (`scripts/decoupe-serveur-xml`) : l'ubérifié de 8,8 Mo
  perd la bibliothèque de collections hors fermeture de portée (pool de
  constantes dépouillé, 4 474 classes visitées), les doublons de
  plateforme (`org/w3c`, `org/xml`, `javax/xml` hors StAX — l'appareil
  les fournit), le point d'entrée de ligne de commande (il attend la
  mort du processus PARENT : sur appareil, le parent EST
  l'application) et `ListenableFuture` (la stub androidx la porte).
  Résultat : 7,0 Mo, reproductible au octet près.
- **Tubes mémoire** (`TubesMemoire`) : les tubes du JDK couplent la
  lecture à la VIVACITÉ du fil écrivain — le serveur répond depuis ses
  fils de piscine éphémères, leur mort tuait le fil de lecture du
  client (« Write end dead »), les réponses mouraient dans le tampon.
  Une file bloquante de trames remplace la filature : la lecture attend
  des octets ou la fermeture, jamais la mort de l'écrivain.
- **L'interface `LanguageSession`** : le serveur de référence (M7.5) et
  le serveur réel passent par la même porte — ouvrir, synchroniser,
  signature, survol, complétion, diagnostics. Les fichiers XML de la
  démonstration y gagnent la complétion documentée de leur DTD, le
  survol des commentaires de déclaration, et les diagnostics de
  validation POUSSÉS (`EtatDiagnostics` : gouttière, bande d'erreurs,
  barre d'état).
- **La fermeture sans `exit`** : l'implémentation du serveur y éteint
  la machine virtuelle — fatale au processus hôte sur appareil. Les
  transports se ferment, la piscine s'arrête, les fils démons meurent
  avec le processus.

## Le constat du terrain réel

Le banc contre le serveur réel a fait tomber une vraie panne du
lecteur : la documentation de complétion n'était lue qu'en chaîne nue —
le protocole sert aussi du CONTENU BALISÉ `{kind, value}` (et des
tableaux de contenus). `ElementCompletion` décode désormais les trois
formes. C'est cela, tester à fond : le monde réel répond différemment
des traces.

## Vérification

- `ServeurXmlEmbarqueTest` (banc JVM) : poignée de main, ouverture,
  diagnostics d'un document valide, violation de DTD publiée,
  revalidation à l'édition, complétion documentée, survol de DTD,
  signature muette, remplacement en vol de deux frappes (PIEGE-18) —
  sept tests contre le serveur DÉCOUPÉ.
- `ListeCompletionTest` : les trois formes de documentation, contrat du
  protocole.
- `PopupRegimesTest`/`PopupSurfacesTest`/`PopupViewsTest` : les régimes
  du menu de débordement, la puce, le plancher de complétion.
- `./gradlew check` vert (683 tests, 0 échec), `:demo:assembleDebug`
  vert — l'APK porte le serveur (dex de 8,1 Mo, services inclus).
