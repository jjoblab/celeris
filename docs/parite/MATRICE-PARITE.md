# Matrice de parité — celeris contre Android Studio

> **Référentiel** : les capacités d'Android Studio servent
> d'étalon. Son état a été vérifié le 2026-11-04 sur sa documentation
> officielle : la ligne stable actuelle est **2026.2.2** (le mandat annonçait
> la ligne 2025.3 — déjà deux lignes de retard, voir « Sources ») ; le mode
> agent, les compétences d'agent et les serveurs d'outils y sont confirmés.
>
> **Colonnes** : la priorité suit le mandat (P0 indispensable, P1 important,
> P2 différenciant) ; « dépôt de départ » résume l'état audité
> (`docs/audit/ETAT-DES-LIEUX.md`) ; le jalon renvoie à
> `docs/feuille-de-route.md` ; le critère d'acceptation est **testable** et
> le scénario de démonstration correspondant doit citer l'identifiant de la
> ligne (règle d'or du mandat).
>
> **Statut celeris** : à M0, toutes les lignes sont « planifié » ; le statut
> passera à « couvert » quand le scénario de démo et son test de fumée
> existeront — ce fichier est mis à jour à chaque jalon.

> Couverture actuelle (tenue à jour à chaque jalon) :
>
> - **M2 — moteur de texte (2026-11-04, clos)** : facettes cœur de A-13 (annulation groupée, sélection restituée), A-18 (coupe par caractère du modèle pur), A-20 (moteur et passe de remplacement par la voie de transaction), A-24 (encodages, fins de ligne, BOM) et A-25 (côté modèle : index de lignes, budgets de benchmark) ; scénarios au catalogue `ScenarioCatalog`, test de fumée `ScenarioCatalogTest`. Les facettes interface (A-01 tactile, A-18 rangées, A-20 barre de recherche, A-25 rendu progressif) suivent leur jalon respectif (M3, M5).
> - **M3 — vue minimale (en cours)** : tranches 1 à 4 livrées — socle de
>   rendu JVM pur (PIEGE-01 à 06, 14, 15), vue Canvas réelle (thèmes
>   D-13, gouttière D-01, défilement borné), retour à la ligne par
>   offsets et gros fichiers (A-18, A-25/G-01 — première image JVM
>   154 ms / 500 ms), tactile complet et clavier matériel (A-01,
>   D-15, D-17 — suppression avant par clusters au cœur), IME par
>   niveau d'API 26 à 36 avec composition par clusters (tranche 5 —
>   surface complète implantée, fenêtres plutôt que texte entier),
>   accessibilité, BiDi au rendu, scénarios et ressources en/fr
>   (tranche 6). Jalon livré côté développement (2026-11-05) :
>   six tranches, portes qualité vertes sur tous les modules,
>   Checkstyle/PMD désormais bloquants sous AGP ; la vérification
>   appareil (IME réelle, gestes, accessibilité, budgets de rendu)
>   reste au propriétaire — D-02, D-07 et D-14 rejoignent leur
>   jalon (M4/M5).

> - **M7.6 — plateforme de langages (en cours)** : étapes 1 à 8 livrées — le
>   modèle (descripteur, configuration, support, capacités, détection),
>   les seize langages en paquets de données (six analyseurs écrits
>   attachés par préférence, dix grammaires réellement TextMate
>   compilées par la couture `PackGrammars`), la détection à
>   précédence complète, la suppression des aiguillages par
>   identifiant, le contrôle d'architecture `check-language-registration`,
>   l’analyse structurelle niveau 1 (H-08 : plis par indentation de la
>   configuration, plan par les règles `outline.json` des paquets —
>   Python, YAML et SQL sans une ligne de Java dédiée) et le niveau 3
>   factorisé (H-10 : Java, Kotlin, KTS et Groovy en spécialisations
>   d’au plus 150 lignes du socle C-like, instantanés identiques) ;
>   la frontière protocolaire du LSP est née et prononcée (H-11 :
>   étape 7 — le module `lsp-protocole` porte le protocole par la
>   bibliothèque, la parité est prononcée sur les sept tests du banc
>   SANS MODIFICATION, le code maison est supprimé, le corpus de
>   traces lit par le Gson de la bibliothèque, les règles R8 sont
>   prouvées sur la démo réduite — ADR-015) ; la couverture nouvelle est
>   raccordée (H-12 : étape 8 — treize méthodes derrière la frontière,
>   servies par le serveur réel, le banc et la porte de session de la
>   démo les rejouent) ; les serveurs se déclarent en données (H-13 :
>   `servers.json` des paquets et de l'hôte, les seize paquets intégrés
>   n'en déclarent aucun — l'éditeur ne connaît pas les serveurs de ses
>   hôtes, la démo déclare le sien et la disponibilité décide du montage
>   ou du repli).
> - **M4 — syntaxe (en cours)** : socle d'analyse incrémentale livré
>   (tranche 1 : jetons à offsets relatifs, états par ligne, cascade de
>   commentaires bornée — PIEGE-19 — arbre tolérant ancré par chunks,
>   crochets, plis, plan, diagnostics, coordinateur session) ;
>   les cinq analyseurs P0 sont livrés avec le registre des langages
>   (tranches 2 à 4 : Java, XML, JSON, Kotlin et Gradle KTS —
>   lexeurs à états transportés, parseurs tolérants, plan, plis,
>   diagnostics et indentation du même arbre, extensions composées,
>   CDATA, gabarits et chaînes brutes, commentaires nichés) ; budget de
>   ré-analyse à seuil tenu (tranche 4bis : les cinq langages sous
>   10 ms sur 50 000 lignes, PIEGE-13) ; les consommateurs du rendu
>   sont branchés (tranche 5 : coloration par plages du même arbre
>   — B-01 — palettes vérifiées WCAG sur les trois thèmes, plis
>   calculés depuis l'arbre et survie du pli manuel à l'éditon
>   voisine — A-15 — chevrons de la gouttière et bascule au clic
>   — D-07 — marques d'erreur par ligne — D-02) ; l'analyse vit
>   désormais hors fil d'appel — fenêtre visible d'abord, complétion
>   par lots, annulation moins chère que terminer (tranche 6, G-01
>   facette analyse) ; le sixième analyseur couvre le dialecte Gradle
>   Groovy — C-07 bouclé avec ses deux dialectes (tranche 7 : appels
>   de premier niveau en chunks, chaînes triples appariées, gabarits
>   ${…} où les guillemets internes ne clôturent pas) et le budget de
>   ré-analyse retombe local sur machine lente (JSON en chunks par
>   valeur de premier niveau, assemblage du lexique en copie de masse
>   — PIEGE-13) ; la voie de grammaires et le plan consommateur
>   (B-16) est livré
>   (tranche 8 : le pont pousse les entrées de l'analyse habillée, le
>   modèle conserve le préfixe intact) et la voie de grammaires couvre la
>   longue traîne (tranche 9 : module core-syntax-grammars, grammaires
>   déclaratives — TOML, YAML, properties, Markdown en P1, Python et
>   Shell en P2 — coloration et plis sans analyseur dédié, pile d'états
>   à égalité de valeur, PIEGE-21).

## A. Édition et saisie

| Id | Capacité | Prio | Dépôt de départ | Module | Jalon | Critère d'acceptation | Statut (M7.5) | Test de vue | Scénario de démo | Test d'appareil |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| A-01 | Multi-curseur (tactile : pilule « ajouter un curseur » ; clavier) | P0 | absent (au départ ; celeris : pilule tactile livrée M3-t4 — l'appui long sélectionne le mot et OFFRE la pilule, l'appui dessus ajoute le curseur, les existants tiennent — redressement M7.5 ; la frappe multi-curseurs vit au cœur depuis M2) | `core-text` + `android-view` | M2/M3 | scénario « 3 curseurs, frappe simultanée » ; test JVM : transaction appliquée aux 3 plages, annulation restaure les 3 sélections | Câblé | ChromePainterTest | oui | — |
| A-02 | Sélection en colonne | P1 | absent (celeris : livré M5-t3 — plage par rangée aux bornes exactes, tranche du retour à la ligne souple comprise) | `android-view` | M5 | scénario colonne sur 10 lignes ; test : bornes visuelles exactes par rangée | Câblé | ColumnSelectionTest | oui | — |
| A-03 | Sélection syntaxique (étendre/réduire par nœud) | P1 | partiel (mot courant) (celeris : livré M5-t3 — chaîne mot → instruction → bloc → type, extension et réduction) | `core-syntax` | M5 | test : extension successives mot → expression → instruction → bloc sur corpus Java/Kotlin/XML | Noyau | — | oui | — |
| A-04 | Déplacement et sélection par mots, y compris casse chameau | P1 | non vérifié | `core-text` | M2 | test : « monCompteurDeLignes » se coupe en 4 mots ; BiDi et CJK inclus | Non démarré | — | non | — |
| A-05 | Fermeture automatique des paires et des citations | P0 | présent (celeris : moteur de plans livré M5-t1 — paire insérée curseur au milieu, sélection entourée, fermant déjà présent sauté) | `core-intelligence` | M5 | scénario frappe ; test : paire fermante insérée, sélection entourée, pas de double fermeture | Démontré | SmartTypingViewTest | oui | — |
| A-06 | Indentation automatique (Entrée, collage, gabarit) | P0 | présent (celeris : livré M5-t1 — retrait lu dans l'arbre au point de coupe, fermant restant descendu seul, style espaces/tabulations) | `core-intelligence` | M5 | test : retrait attendu calculé depuis l'arbre (crochets + options de style) sur corpus | Démontré | SmartTypingViewTest | oui | — |
| A-07 | Retour arrière intelligent | P1 | non vérifié (celeris : livré M5-t1 — paire et citations vides entières, cran d'indentation, repli documenté sur les clusters du cœur) | `core-intelligence` | M5 | test : supprime la paire entière, l'indentation excédentaire, le groupe de graphèmes | Démontré | SmartTypingViewTest | oui | — |
| A-08 | Entourer la sélection | P1 | non vérifié (celeris : livré M5-t1 — paire en ligne et bloc multiligne, positions du curseur après le geste) | `core-intelligence` | M5 | scénario : entourer par accolades/guillemets/bloc ; test : positions du curseur après | Démontré | SmartTypingViewTest | oui | — |
| A-09 | Collage avec réindentation | P1 | non vérifié (celeris : livré M5-t1 — première ligne inchangée, suivantes alignées à la profondeur d'accueil, structure relative conservée) | `core-intelligence` | M5 | test : collage multiligne réindenté selon le contexte d'accueil | Démontré | SmartTypingViewTest | oui | — |
| A-10 | Déplacer, dupliquer, joindre, supprimer des lignes | P1 | non vérifié (celeris : livré M5-t2 — bloc de lignes touchées, dupliquer place les curseurs dans la copie, multi-sélection par blocs) | `core-intelligence` | M5 | scénario clavier + pilule ; test : multi-sélection incluse | Noyau | — | oui | — |
| A-11 | Commenter/décommenter (ligne et bloc) | P0 | présent (celeris : livré M5-t2 — préfixe par langage, tout ou rien, XML par bloc, JSON masqué) | `core-intelligence` | M5 | test : respect du langage (syntaxe Java, XML `<!-- -->`, JSON sans commentaire → action masquée) | Noyau | — | oui | — |
| A-12 | Tri de lignes (naturel, sensible à la casse) | P2 | absent (celeris : livré M5-t2 — tri naturel « item2 » < « item10 », casse au choix) | `core-intelligence` | M5 | test : tri naturel « item2 » < « item10 » | Noyau | — | oui | — |
| A-13 | Annulation/rétablissement groupés avec restauration de sélection | P0 | présent (coalescence) | `core-text` | M2 | test : 100 frappes coalescées en 1 opération, sélection restaurée, PIEGE-07 couvert | Câblé | RetourSoupleCrashTest | oui | — |
| A-14 | Historique local par fichier | P2 | absent | `android-host` | M7 | scénario : restaurer une version antérieure ; test d'isolement par fichier | Non démarré | — | non | — |
| A-15 | Pli manuel, par niveau, régions | P0 | présent (au départ ; celeris : régions calculées depuis l'arbre et pli manuel survivant à l'éditon voisine livrées M4-t5 — `FoldTracker`, remappage monotone, rejeu d'annulation compris) | `core-syntax` | M4 | test : plis calculés depuis l'arbre ; pli manuel survive à l'édition voisine | Câblé | GutterMarksTest | oui | — |
| A-16 | Lignes collantes | P1 | absent (celeris : livré M5-t6 — en-têtes englobants du même arbre, budget d'image borné aux plus profondes) | `android-view` | M5 | test : en-tête visible au défilement, budget d'image tenu | Câblé | StickyLinesTest | oui | — |
| A-17 | Guides d'indentation | P1 | présent | `android-view` | M3 | test de capture : guides alignés aux niveaux, plis inclus | Non démarré | — | non | — |
| A-18 | Retour à la ligne souple (mots, coupure forcée) | P0 | présent | `core-text` + `android-view` | M2/M3 | test : mot plus large que la largeur coupé par caractère (PIEGE-08) ; rangées ≠ lignes (PIEGE-04) | Câblé | GutterPainterTest | oui | — |
| A-19 | Caractères invisibles (espaces, tabulations, contrôles) | P1 | non vérifié (celeris : livré M5-t6 — finale, tabulation et fine toujours, interne en mode « tout », fin de ligne sur demande) | `android-view` | M5 | test de capture : espaces fines/finales, tabulations, fins de ligne | Câblé | InvisibleCharsTest | oui | — |
| A-20 | Recherche/remplacement (regex, casse, mot entier, sélection, multiligne, préservation de casse) | P0 | présent (celeris : moteur livré M2 ; barre — état, options, navigation circulaire — livrée M5-t4) | `core-text` + `android-widgets` | M2/M5 | test : les 8 variantes + remplacement multi-curseur ; scénario de démo dédié | Câblé | SearchBarModelTest | oui | — |
| A-21 | Recherche dans le projet (adossée à l'index) | P1 | absent | `core-intelligence` | M6 | test : recherche hors ligne dans un projet embarqué, latence consignée | Non démarré | — | non | — |
| A-22 | Gabarits dynamiques (à trous, variables liées) | P1 | présent, gabarits riches (celeris : livré M5-t2 — trous tabulables, variables liées, miroirs d'édition au décalage relatif) | `core-intelligence` | M5 | scénario : gabarit à 3 trous tabulables ; test : ordre de tabulation et édition simultanée des variables liées | Démontré | AtelierRefactorTest | oui | — |
| A-23 | Complétion postfixe | P2 | présent (bancs démo) | `core-intelligence` | M6 | test : `expr.if` → `if (expr) {}` sans matérialiser le préfixe (PIEGE hérité du lot L0) | Non démarré | — | non | — |
| A-24 | Encodage, fins de ligne, BOM | P0 | partiel | `core-text` | M2 | test : UTF-8/UTF-16/Latin-1, CRLF/LF/CR, BOM préservé, détection sans heuristique réseau | Noyau | — | oui | — |
| A-25 | Très gros fichiers : ouverture progressive | P0 | partiel (première image JVM : 154 ms pour 100 000 lignes ; appareil réel à vérifier) | `core-text` + `android-view` | M2/M3 | benchmark : 100 000 lignes → première image ≤ 500 ms ; défilement fluide immédiat | Câblé | GrosFichiersTest | oui | — |
| A-26 | Fichiers binaires : détection, aperçu sûr, refus d'édition destructrice | P1 | absent | `android-host` | M7 | test : binaire détecté, aperçu hexadécimal lecture seule | Non démarré | — | non | — |

## B. Intelligence du langage

| Id | Capacité | Prio | Dépôt de départ | Module | Jalon | Critère d'acceptation | Statut (M7.5) | Test de vue | Scénario de démo | Test d'appareil |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| B-01 | Coloration lexicale et syntaxique | P0 | présent (19 tokéniseurs) (au départ ; celeris : coloration par plages du même arbre livrée M4-t5 — `SyntaxRowSource` et `TextPainter`, palettes par sorte vérifiées WCAG sur les trois thèmes) | `core-syntax` | M4 | test : même arbre pour Java, Kotlin, XML, JSON, KTS ; budgets de ré-analyse tenus | Câblé | SyntaxPaletteTest | oui | — |
| B-02 | Coloration sémantique (paramètres, champs, statiques, obsolète barré, inutilisé estompé) | P1 | présent (tir sémantique) | `core-lsp` | M6 | test avec serveur factice : jetons sémantiques indépendants des diagnostics (PIEGE-18) | Noyau | — | oui | — |
| B-03 | Complétion de base (préfixe, mots du fichier) | P0 | présent (celeris : la reprise M7.5 branche la complétion au curseur — CompletionListView sur la couche porte-popups, le filtrage par préfixe sans proposer le mot sous le curseur, la frappe filtre sans refermer la liste ; le redressement M7.5 la propose à la FRAPPE dès le premier caractère d'identifiant, le blanc referme) | `core-intelligence` | M6 | test hors ligne : complétion au mot près sans serveur | Démontré | PopupViewsTest | oui | — |
| B-04 | Complétion typée, chaînée, d'instruction | P0 | présent | `core-lsp` | M6 | test avec serveur factice : tri et filtrage respectés, liste tronquée honorée (PIEGE-17) | Noyau | — | oui | — |
| B-05 | Import automatique | P0 | présent | `core-intelligence` | M6 | test : candidat d'import inséré en tête avec édition de position unique | Noyau | — | oui | — |
| B-06 | Implémenter/surcharger une méthode | P1 | absent | `core-lsp` | M6 | test : squelette généré depuis la signature du serveur, curseurs aux trous | Noyau | — | oui | — |
| B-07 | Aide aux paramètres (signature) | P0 | présent (celeris : la reprise M7.5 livre la vue — SignatureHelpView sur la couche, le paramètre courant souligné par le tronçon exact du modèle, placé au côté opposé de la complétion) | `core-lsp` + `android-widgets` | M6 | test : signature au-dessus du curseur, paramètre courant souligné (placement coordonné) | Démontré | PopupViewsTest | oui | — |
| B-08 | Indices en ligne (types, noms de paramètres, lambdas) | P1 | présent (inlay hints réparés) | `core-lsp` + `android-view` | M6 | tests PIEGE-11 et PIEGE-12 (cellule d'encre pleine, même ligne de base) | Noyau | — | oui | — |
| B-09 | Documentation rapide (Javadoc/KDoc au survol) | P0 | présent (celeris : la reprise M7.5 livre la vue — CompletionDocView sur la couche, titre et sections typés DISTINCTS, le survol s'y sert aussi) | `core-lsp` + `android-widgets` | M6 | test : blocs typés distincts (PIEGE-16), résolution à la demande | Démontré | PopupViewsTest | oui | — |
| B-10 | Navigation : déclaration, type, implémentations, super | P0 | partiel (LSP) | `core-lsp` | M6 | test : navigation hors ligne par l'index, en ligne par le serveur, retour au point de départ | Noyau | — | oui | — |
| B-11 | Usages dans le projet | P1 | absent | `core-intelligence` | M6 | test : usages indexés hors ligne + raffinement serveur | Noyau | — | oui | — |
| B-12 | Hiérarchies de types et d'appels | P2 | absent | `core-intelligence` | M7+ | scénario arbre de hiérarchie ; test de profondeur sur corpus | Non démarré | — | non | — |
| B-13 | Aller à classe, fichier, symbole | P1 | partiel | `core-intelligence` | M6 | test : recherche floue de symboles sur l'index, hors ligne | Noyau | — | oui | — |
| B-14 | Historique de navigation avant/arrière | P1 | absent (celeris : livré M5-t4 — pile bornée, la visite coupe l'avance, la fermeture de fichier vide) | `android-view` | M5 | test : pile bornée, comportement après fermeture de fichier | Câblé | NavigationHistoryTest | oui | — |
| B-15 | Signets (avec gouttière) | P1 | absent (celeris : livré M5-t4 — bascule, sauts circulaires, persistance par projection des offsets) | `android-view` | M5 | test : ajout/retrait, saut, persistance par session | Câblé | BookmarkModelTest | oui | — |
| B-16 | Plan du fichier (structure) | P0 | absent (au départ ; celeris : livré M4-t8 — le pont session ↔ vue pousse le plan de l'analyse habillée, le modèle déduit la profondeur de l'imbrication et conserve le préfixe intact d'un plan à l'autre, le peintre trace glyphe de catégorie et retrait par niveau) | `core-syntax` + `android-widgets` + `android-view` | M4 | test : plan généré du même arbre que la coloration, mise à jour incrémentale | Câblé | EditorSyntaxPlanTest | oui | — |
| B-17 | Inspections, intentions, correctifs rapides | P1 | partiel (quick fixes) | `core-lsp` | M6 | test : correctif applicable = transaction diffable, annulable | Noyau | — | oui | — |
| B-18 | Aperçu d'un correctif (diff avant application) | P2 | absent | `android-widgets` | M7 | scénario : aperçu en ligne avant acceptation | Non démarré | — | non | — |
| B-19 | Refactorisation : renommer (fichier et projet) | P0 | absent | `core-intelligence` | M6 | test : renommage multi-fichiers par éditions d'espace de travail, conflits signalés | Démontré | AtelierRefactorTest | oui | — |
| B-20 | Extraire (méthode, variable, constante, champ, paramètre) | P1 | absent | `core-intelligence` | M7 | tests par variante sur corpus Java | Non démarré | — | non | — |
| B-21 | Aligner en ligne (inline) | P1 | absent | `core-intelligence` | M7 | test : usages remplacés, déclaration supprimée si privée | Non démarré | — | non | — |
| B-22 | Changer la signature | P2 | absent | `core-intelligence` | M7+ | scénario : réordonnancement sécurisé | Non démarré | — | non | — |
| B-23 | Déplacement et suppression sûre | P2 | absent | `core-intelligence` | M7+ | scénario : usages vérifiés avant suppression | Non démarré | — | non | — |
| B-24 | Génération de code (constructeurs, accesseurs, `equals`, `hashCode`, `toString`) | P1 | présent (démo) | `core-intelligence` | M6 | tests par générateur sur corpus ; conformité aux conventions | Noyau | — | oui | — |
| B-25 | Formatage du code | P0 | partiel | `core-intelligence` | M6 | test : formatage idempotent sur corpus multi-langages | Démontré | AtelierRefactorTest | oui | — |
| B-26 | `.editorconfig` respecté | P1 | absent | `core-intelligence` | M6 | test : retrait, fins de ligne, encodage pilotés par le fichier | Noyau | — | oui | — |
| B-27 | Optimisation des imports à l'enregistrement | P1 | présent | `core-intelligence` | M6 | test : imports non résolus signalés, pas supprimés en silence | Démontré | AtelierRefactorTest | oui | — |

## C. Spécifique Android

| Id | Capacité | Prio | Dépôt de départ | Module | Jalon | Critère d'acceptation | Statut (M7.5) | Test de vue | Scénario de démo | Test d'appareil |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| C-01 | XML : coloration, plis, crochets | P0 | présent (au départ ; celeris : arbre, plis et coloration XML livrés M4-t3 et M4-t5) | `core-syntax` | M4 | test : arbre XML tolérant (balise non fermée), plis par élément | Noyau | — | oui | — |
| C-02 | XML : complétion guidée par le schéma des ressources | P1 | livré (celeris : M7-t4 — schéma embarqué, le contexte du curseur décide : balise, attribut, valeur ; unités dp/sp, préfixes `@string/`) | `core-intelligence` | M7 | test : attributs et unités (dp, sp, `@string/`) suggérés depuis le schéma embarqué | Noyau | — | oui | — |
| C-03 | XML : validation et diagnostics | P1 | livré (celeris : M7-t4 — la fermeture étrangère porte sa plage, les traversés d'une fermeture d'ancêtre se signalent à leur ouverture, les ouverts restants à la fin) | `core-syntax` | M7 | test : diagnostic d'imbrication invalide avec plage exacte | Noyau | — | oui | — |
| C-04 | XML : navigation des références (`@string/`, `@layout/`…) | P1 | livré (celeris : M7-t4 — définition par balise de valeurs ou fichier propre, usages du projet à l'offset exact, référence prolongée exclue) | `core-intelligence` | M7 | test : aller à la ressource définie, aller à l'usage | Noyau | — | oui | — |
| C-05 | Pastilles de couleur et miniatures en gouttière | P2 | livré (celeris : M7-t8 — les quatre formes Android résolues, la forme courte étend ses composantes ; le carré se peint dans la respiration droite de la gouttière, test de capture à la valeur résolue) | `android-widgets` | M7 | test de capture : pastille rendue depuis la valeur résolue | Câblé | GutterPainterTest | oui | — |
| C-06 | Manifeste : complétion et diagnostics | P1 | livré (celeris : M7-t4 — attributs des balises du manifeste et permissions usuelles, par préfixe) | `core-intelligence` | M7 | test : attributs du manifeste connus, permissions suggérées | Noyau | — | oui | — |
| C-07 | Gradle KTS et Groovy : coloration, plis, structure | P0 | livré (KTS tranche 4, Groovy tranche 7 — chunks par appel de premier niveau, chaînes triples appariées) | `core-syntax` | M4 | test : les deux dialectes analysés, plis par bloc | Noyau | — | oui | — |
| C-08 | Catalogues de versions : complétion liée | P2 | livré (celeris : M7-t4 — sections lues, accès suggérés : tirets en points, bundles dans leur espace de noms) | `core-intelligence` | M7 | test : `libs.versions.toml` ↔ alias suggérés dans les builds | Noyau | — | oui | — |
| C-09 | Règles R8/ProGuard : coloration et diagnostics de base | P2 | livré (celeris : M7-t7 — grammaire des règles, jokers et blocs de membres repliés ; ValidationRegles signale règles inconnues, accolades orphelines et membres sans point-virgule, sans faux positif sur le corpus sain) | `core-syntax` | M7 | test : grammaire de règles, garde-fou d'erreur de syntaxe | Noyau | — | non | — |
| C-10 | Compose : documentation des modificateurs au survol | P2 | absent | `core-lsp` | M7+ | scénario : survol d'un modificateur → documentation | Non démarré | — | non | — |
| C-11 | Compose : repères `@Preview` en gouttière (rendu délégué à l'hôte) | P2 | livré (celeris : M7-t8 — chaque annotation ancre son losange dans la coulisse, le nom du composable se lit sur place ou dessous ; le clic délègue le rendu à l'hôte, jamais embarqué) | `android-host` | M7 | test : repère ancré à la ligne, délégation du rendu à l'hôte | Câblé | GutterPainterTest | oui | — |
| C-12 | Lint Android : diagnostics affichés, correctifs applicables | P1 | livré (celeris : M7-t6 — la sortie texte de lint se convertit aux lignes exactes, les diagnostics importés se fondent aux erreurs de la syntaxe dans la gouttière ; les correctifs mécaniques — ellipse, cadratin — s'appliquent en transactions annulables) | `android-host` | M7 | test : diagnostics de build importés, décalage exact (tests hérités de décalage) | Câblé | GutterMarksTest | oui | — |
| C-13 | Extraction d'une chaîne en ressource | P2 | absent | `core-intelligence` | M7+ | test : ressource créée + référence insérée + transaction annulable | Non démarré | — | non | — |
| C-14 | Langages P0 : Java, Kotlin, XML | P0 | Java partiel (démo) (au départ ; celeris : les trois analyseurs livrés M4-t2 à t4, coloration consommatrice M4-t5) | `core-syntax` | M4 | critère identique à B-01 par langage | Noyau | — | oui | — |
| C-15 | Langages P1 : Gradle, JSON, TOML, YAML, properties, Markdown, SQL, C, C++ | P1 | livré (Gradle KTS et Groovy tranches 4 et 7, JSON tranche 3 ; TOML, YAML, properties et Markdown voie de grammaires tranche 9 ; SQL, C et C++ voie de grammaires M7-t7, budgets de ré-analyse tenus) | `core-syntax` + `core-syntax-grammars` | M4/M7 | test : coloration + plis par langue (analyseurs dédiés ou grammaires) | Noyau | — | oui | — |
| C-16 | Langages P2 : longue traîne | P2 | partiel (tokéniseurs au départ ; celeris : la voie de grammaires livrée M4-t9 — Python et Shell colorés et repliés par grammaire déclarative sans analyseur dédié, pile d'états à égalité de valeur PIEGE-21, budget de ré-analyse tenu) | `core-syntax-grammars` | M4/M7 | test : coloration par grammaire sans analyseur dédié | Noyau | — | oui | — |

## D. Surface d'interface

| Id | Capacité | Prio | Dépôt de départ | Module | Jalon | Critère d'acceptation | Statut (M7.5) | Test de vue | Scénario de démo | Test d'appareil |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| D-01 | Gouttière : numéros absolus et relatifs | P0 | présent | `android-widgets` | M3 | test de capture : bascule relative, alignement par rangée | Câblé | GutterPainterTest | oui | — |
| D-02 | Gouttière : diagnostics | P0 | présent (au départ ; celeris : marque d'erreur par ligne dans la coulisse, à l'encre d'erreur du thème, livrée M4-t5 — le socle ne rapporte qu'un niveau d'erreur, la priorité multi-niveaux attend la sémantique M6) | `android-widgets` | M3 | test : icône au niveau de ligne, priorité erreur > avertissement | Câblé | GutterPainterTest | non | — |
| D-03 | Gouttière : marques de modification VCS | P1 | livré (celeris : peinture M7-t5 — barre au bord gauche de la coulisse, modifiée et ajoutée distinguées ; diff vivant à la frappe dans la démonstration) | `android-host` | M7 | test SPI : marque par plage de lignes modifiée | Câblé | MarquesIntegrationTest | oui | — |
| D-04 | Gouttière : points d'arrêt (SPI débogueur) | P1 | livré (celeris : peinture M7-t5 — carré plein posé, carré creux désactivé ; le clic du champ des numéros pose et retire dans la démonstration) | `android-host` | M7 | test SPI : pose, suppression, état désactivé | Câblé | MarquesIntegrationTest | oui | — |
| D-05 | Gouttière : repères d'exécution | P1 | livré (celeris : peinture M7-t5 — flèche de ligne courante à l'encre du curseur, chevrons de pile ; pause simulée au premier arrêt actif) | `android-host` | M7 | test SPI : ligne courante, plages dans la pile | Câblé | MarquesIntegrationTest | oui | — |
| D-06 | Gouttière : signets | P1 | absent (couvert par B-15) | `android-widgets` | M5 | couvert par B-15 | Noyau | — | oui | — |
| D-07 | Gouttière : plis (flèches, survol) | P0 | présent (au départ ; celeris : chevrons replié/déplié et bascule au clic dans la coulisse livrés M4-t5 — le redressement M7.5 agrandit le chevron : cellule carrée cohérente, bras au tiers du côté, trait épais, fond de gouttière voilé de translucide) | `android-widgets` | M3 | test : pli/dépli par clic, aperçu au maintien | Câblé | GutterPainterTest | non | — |
| D-08 | Bande d'erreurs annotée (parallèle au défilement) | P1 | absent (celeris : livré M5-t5 — densité bornée à un cran de bande, clic → saut au diagnostic ; la reprise M7.5 livre la vue — ErrorBandView au bord droit de la couche, l'appui saute au diagnostic du cran le plus proche) | `android-widgets` | M5 | test : densité bornée, clic → saut au diagnostic | Câblé | PopupSurfacesTest | oui | — |
| D-09 | Fil d'Ariane | P1 | présent (celeris : livré M5-t5 — chemin du symbole depuis le plan du même arbre, troncature en ellipse ; la reprise M7.5 livre la vue — BreadcrumbView en bandeau haut de la couche, il suit le curseur et survit au défilement) | `android-widgets` | M5 | test : chemin du symbole courant depuis l'arbre, tronqué par la largeur | Câblé | PopupSurfacesTest | oui | — |
| D-10 | Popups unifiées (complétion, documentation, signature, actions) | P0 | présent (kit unifié) (celeris : placement coordonné livré M5-t5 — complétion, documentation, signature sans superposition ni sortie d'écran ; la reprise M7.5 pose les DIX popups sur la couche — PopupAnchor borne chaque ancre à la couche, le contrôleur porte les régimes d'exclusion, PIEGE-27 prouvé à chaque vue) | `android-widgets` | M5 | tests de placement coordonné (héritage direct des acquis) ; accessibilité annoncée | Câblé | PopupAnchorTest | oui | — |
| D-11 | Aperçu rapide d'une définition | P2 | livré (celeris : M7-t9 — le plan du fichier sert d'index, l'entrée la plus intime contenant le curseur se survole, l'extrait reste borné ; le symbole au curseur se lit en caractères d'identifiant) | `android-widgets` | M7 | scénario : définition flottante sans quitter le fichier | Câblé | ApercuDefinitionTest | oui | — |
| D-12 | Vue de différences côte à côte et en ligne (VCS et agent) | P1 | livré (celeris : M7-t9 — DiffBlocs découpe par la plus longue sous-suite commune, lectures en ligne et côte à côte ; RevueDiff accepte et refuse par bloc avec réalignement et points de contrôle — la matière de la revue d'agent F-04) | `android-widgets` | M7/M8 | test : diff par blocs, accepter/refuser par bloc, réalignement après rejet | Câblé | DiffBlocsTest | oui | — |
| D-13 | Thèmes clair, sombre, contraste élevé | P0 | partiel | `android-view` | M3 | test : 3 thèmes complets, contraste vérifié automatiquement | Câblé | SyntaxPaletteTest | oui | — |
| D-14 | Polices à ligatures (licence libre) | P2 | présent | `android-view` | M3 | test de capture : ligatures activables, police libre attribuée dans `NOTICE` | Non démarré | — | oui | — |
| D-15 | Zoom au pincement | P0 | présent (celeris M3-t4 : paliers bornés, point sous le doigt stable — test JVM vert ; appareil réel à vérifier) | `android-view` | M3 | test gestuel : paliers de taille, curseur stable sous le doigt | Câblé | KeyboardZoomTest | oui | — |
| D-16 | Tactile : poignées, loupe, pilule, barre de symboles | P0 | présent | `android-view` | M3 | tests gestuels instrumentés ; PIEGE-03 (ancre figée au DOWN) | Noyau | — | oui | — |
| D-17 | Clavier matériel : table de raccourcis alignée sur Android Studio, reconfigurable | P0/P1 | partiel (celeris M3-t4 : table complète, remappage, encodage, mouvements et suppression avant par clusters — tests JVM verts ; instrumenté à venir) | `android-view` | M3/M5 | test : table par défaut complète, remappage persistant, conflits détectés | Câblé | CaretsMotionTest | oui | — |
| D-18 | Stylet, souris, pavé tactile (survol, cliquer-tirer, molette) | P1 | partiel (celeris : livré M5-t6 — molette Ctrl = zoom borné, info-bulle après station, déplacement qui la retire) | `android-view` | M5 | test : survol → info-bulle, sélection à la souris, molette avec Ctrl = zoom | Câblé | PointerGesturesTest | oui | — |
| D-19 | Pliables, grands écrans, mode bureau | P1 | livré (celeris : M7-t9 — ClasseLargeur suit les seuils des classes de taille, la gouttière resserre ou respire selon la fenêtre, le pliable déplié change de classe d'un geste) | `android-view` | M7 | scénario pliable ; test : rangées/gouttière s'adaptent à la largeur | Câblé | ClasseLargeurTest | oui | — |
| D-20 | Glisser-déposer (texte, fichiers) | P2 | absent | `android-view` | M7+ | scénario : dépôt de fichier → ouverture | Non démarré | — | non | — |

## E. Contrats d'intégration (SPI, avec implémentation de référence)

| Id | Capacité | Prio | Dépôt de départ | Module | Jalon | Critère d'acceptation | Statut (M7.5) | Test de vue | Scénario de démo | Test d'appareil |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| E-01 | Système de fichiers virtuel (accès stockage cloisonné, arbres de documents) | P0 | livré (celeris : contrat SPI et implémentation de référence M7-t2 ; l'adaptateur de production M7-t6 — AdaptateurStockageDocuments sur la porte ArbreDocuments, ArbreDocumentsSaf seul à toucher au framework, parents implicites à l'écriture, même contrat que la référence, testé sur arbre factice) | `android-host` | M7 | test : ouverture/écriture par le framework d'accès au stockage, chemins validés contre la traversée | Noyau | — | oui | — |
| E-02 | VCS : diff de gouttière, blame, historique | P1 | couvert (celeris : contrat SPI et faux VCS de référence livrés M7-t2 — marques par ligne, plages supprimées, blame, historique ; la peinture de gouttière D-03 livrée M7-t5) | `android-host` | M7 | test SPI avec faux VCS : marques, annotations par ligne | Noyau | — | oui | — |
| E-03 | Débogueur : points d'arrêt, ligne courante, valeurs en ligne, évaluation au survol | P1 | couvert (celeris : contrat SPI et faux débogueur livrés M7-t2 — arrêts désactivables, ligne courante, pile, survol borné ; la peinture D-04/D-05 livrée M7-t5) | `android-host` | M7 | test SPI avec faux débogueur : pose d'arrêt, ligne courante, survol borné | Noyau | — | oui | — |
| E-04 | Exécution et tests : repères de gouttière | P1 | couvert (celeris : contrat SPI et fausse exécution livrés M7-t2 — lancements signalés, états visibles ; la peinture D-05 livrée M7-t5) | `android-host` | M7 | test SPI : lancement signalé, état visible | Noyau | — | oui | — |
| E-05 | Problèmes de build convertis en diagnostics | P1 | livré (celeris : analyseur de sortie M7-t2, formes complète et courte ; le branchement à l'éditeur livré M7-t6 — les diagnostics de lint importés se peignent dans la gouttière, fondus aux erreurs de la syntaxe) | `android-host` | M7 | test : sortie de build parsée → diagnostics avec plages exactes | Noyau | — | oui | — |
| E-06 | Traces d'appel liées à `fichier:ligne` | P1 | absent (celeris : analyseur de piles livré M7-t2 — cadres convertis, ligne inconnue à zéro ; le lien cliquable suit) | `android-host` | M7 | test : pile parsée, saut à la ligne exacte | Noyau | — | oui | — |

## F. IA native

| Id | Capacité | Prio | Dépôt de départ | Module | Jalon | Critère d'acceptation | Statut (M7.5) | Test de vue | Scénario de démo | Test d'appareil |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| F-01 | Texte fantôme multiligne (accepter par mot/ligne/bloc) | P0 | livré (celeris : noyau M8-t1 — SessionTexteFantome, suggestion datée par révision, jamais ré-ancrée, acceptation par mot/ligne/bloc, refus à la frappe suivante ; rendu M8-t5 — FantomePainter au chemin des indices en ligne, une cellule d'encre pleine par caractère sur la ligne de base du code (PIEGE-11 couvert), encre fanée, acceptation au clavier (Tab, remappable) et au geste (appui sur le fantôme), PiloteFantome pur ré-ancre le reste au décalage, l'atelier IA et le menu de la démonstration suivent) | `core-ai` + `android-view` | M8 | test : affichage ≤ budget, refus à la frappe suivante, acceptation par granularité (`TexteFantomeTest`, `FantomePainterTest`, `PiloteFantomeTest`) | Câblé | LatenceFantomeVueTest | oui | — |
| F-02 | Prédiction de la prochaine édition | P1 | livré (celeris : noyau M8-t1 — ModelePrediction prédit une opération, AppliquerPrediction l'applique en transaction annulable, jamais silencieuse, refus de révision ; M8-t5 — l'atelier IA de la démonstration demande la prédiction au faux modèle local et l'applique explicitement, le refus se lit) | `core-ai` | M8 | test : prédiction = transaction annulable, jamais d'application silencieuse (`PredictionTest`) | Noyau | — | oui | — |
| F-03 | Actions IA dans le menu d'intentions | P1 | livré (celeris : M8-t5 — les actions IA (suggestion, prédiction) vivent au menu de l'atelier de la démonstration, au même titre que les intentions classiques — lint, aperçu de définition, revue du diff, revue d'agent — même barre, même gestuelle) | `core-ai` | M8 | test : action exposée au même titre qu'une intention classique (menu de l'atelier, `AtelierIA`) | Noyau | — | oui | — |
| F-04 | Revue des modifications d'agent : diff en ligne, accepter/refuser par bloc, points de contrôle | P0 | livré (celeris : M8-t2 — PropositionAgent étiquetée et datée de son instantané de référence, plages ordonnées et disjointes ; RevueAgent découpe en blocs d'éditions contiguës, le refus réaligne les suivants — les plages de référence ne bougent jamais —, les points de contrôle reviennent en arrière, l'application se prend en une transaction annulable et refuse explicitement le document qui a bougé ; la lecture en ligne rend les « - » et les « + » par bloc, la démonstration révise l'agent factice bloc par bloc) | `core-ai` + `android-widgets` | M8 | test : 3 blocs, refus du milieu, re-alignement, retour au point de contrôle (`RevueAgentTest`) | Noyau | — | oui | — |
| F-05 | API d'outils éditeur pour agents (lire, symboles, diagnostics, transaction avec conflit) | P0 | livré (celeris : M8-t3 — OutilsEditeur : lire des plages et l'instantané, révision servie comprise ; symboles et diagnostics des sources injectées par l'hôte ; appliquer une transaction avec détection de conflit — l'instantané de référence doit correspondre, refus explicite au message lisible, jamais de fusion silencieuse — la voie de transaction ordinaire, une étape d'annulation ; les éditions multiples vivent en coordonnées de référence, ordonnées et disjointes) | `core-ai` | M8 | test : transaction refusée si instantané de référence périmé, refus explicite (`OutilsEditeurTest`) | Noyau | — | oui | — |
| F-06 | Surface compatible avec les serveurs d'outils (protocole MCP) | P1 | livré (celeris : M8-t3 — ConnecteurMcp : le catalogue des cinq outils (noms celeris.*, descriptions, arguments), chaque appel traverse la même méthode qu'en direct, les réponses se rendent en JSON avec échappement — le transport reste à l'hôte, le cœur ne connaît aucun fil réseau, ADR-012) | `core-ai` | M8 | test : mêmes outils exposés par le connecteur MCP et en direct (`ConnecteurMcpTest`) | Noyau | — | oui | — |
| F-07 | Respect du fichier d'instructions d'agent et des exclusions de contexte | P1 | livré (celeris : M8-t4 — ExclusionsContexte lit le fichier du dépôt : chemins exacts, dossiers et jokers, une règle sans barre vaut pour tout suffixe de composante ; GardeContexte se glisse entre la session et le modèle — le chemin exclu ne produit aucun départ, le journal du faux le prouve ; InstructionsAgent sert le fichier d'instructions par la voie des outils, l'outil celeris.instructions l'expose, jamais inventé) | `core-ai` | M8 | test : chemin exclu jamais transmis (assertion sur le faux modèle) (`ExclusionsContexteTest`) | Noyau | — | oui | — |
| F-08 | Modèles locaux ou distants, branchés par l'hôte | P1 | livré (celeris : M8-t1 — FauxModeleLocal synchrone, FauxModeleDistant par exécuteur injecté, même contrat testé, journal des requêtes ; M8-t6 — l'atelier IA de la démonstration branche le faux local derrière le garde du contexte, consentement et exclusions compris) | `core-ai` | M8 | test : faux modèle local + faux distant, même contrat (`FauxModelesTest`) | Noyau | — | oui | — |
| F-09 | Annulation et budgets de latence | P0 | livré (celeris : noyau M8-t1 — jeton annulé à la frappe suivante, mille demandes au modèle aveugle mesurées au minimum de trois tours ; M8-t5/t6 — le budget d'affichage se consigne avec le rendu : mille tracés du fantôme au minimum de trois tours, chaque tracé sous le budget d'une image à soixante hertz) | `core-ai` | M8 | test : jeton annulé à la 2ᵉ frappe, aucune latence bloquante mesurée (`LatenceFantomeTest`, `LatenceFantomeVueTest`) | Noyau | — | oui | — |
| F-10 | Télémétrie en option uniquement ; aucun trafic sans consentement | P0 | livré (celeris : M8-t6 — le garde du contexte consulte le consentement à chaque départ, relu jamais figé ; l’interrupteur des réglages de la démonstration reste coupé par défaut, sans lui aucune requête ne part — pas même vers le modèle local ; le manifeste ne déclare aucune prise réseau, la neutralité est structurelle — aucun paquet celeris n’embarque de télémétrie, l’option reste à l’hôte, documentée au README) | tous | M8 | test CI : aucune requête réseau en mode par défaut (démo neutre) (`DemoNeutreTest`) | Noyau | — | oui | — |

## G. Avantages structurels visés

| Id | Capacité | Prio | Dépôt de départ | Module | Jalon | Critère d'acceptation | Statut (M7.5) | Test de vue | Scénario de démo | Test d'appareil |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| G-01 | Ouverture instantanée et progressive de très gros fichiers | P0 | partiel (texte : 154 ms / 500 ms, 480 000 lignes ouvrables ; analyse : hors fil, fenêtre d'abord — instantané 224 ms sur 100 000 lignes, 1441 ms / budget 2500 ms à 480 000 lignes (banc Charge480kLignesTest, tas dédié 1 Go, chantier C10), incrément au milieu 36 ms / budget 100 ms, annulation moins chère que terminer, tranche 6 ; mode gros fichiers tri-état AUTO/ON/OFF à la manière du dépôt de départ, banc LargeFileModeTest — la complétion coupe au-delà des seuils 2,5 M caractères / 50 000 lignes, l'édition reste entière) | `core-text` + `android-view` | M2/M3 | benchmark CI : 100 000 lignes → première image ≤ 500 ms, 480 000 lignes ouvrables | Câblé | GrosFichiersTest | oui | — |
| G-02 | Tactile d'abord, hors ligne d'abord, sobre en mémoire et batterie | P0 | partiel | tous | M3+ | scénarios démo hors ligne ; budgets mémoire et images tenus ; aucun réveil réseau parasite | Noyau | — | oui | — |
| G-03 | Exécutable sans Android (`editor-cli` : analyser, diagnostiquer, formater, refactorer) | P1 | absent | `cli` | M7 | exécution en CI sur corpus, sortie déterministe | Non démarré | — | non | — |
| G-04 | Plugins isolés (peintres, gouttière, commandes) : l'erreur d'un plugin ne tue pas l'éditeur | P1 | absent (celeris : livré M5-t6 — l'extension qui lève est journalisée, les suivantes courent, image suivante intacte) | `android-view` | M5 | test : plugin lançant une exception → ligne de journal, image suivante intacte | Câblé | PluginHostTest | oui | — |
| G-05 | Accessibilité exemplaire dès la première version | P0 | absent (au départ ; celeris M3-t6 : arbre virtuel de rangées visibles, granularités, annonces — tests JVM verts ; auditeur et lecteur d'écran sur appareil à vérifier) | `android-view` | M3 | auditeur automatique vert sur chaque scénario ; lecture par lignes et plages | Câblé | MovementGranularityTest | oui | — |

## H. Langages et protocole (M7.6)

| Id | Capacité | Prio | Dépôt de départ | Module | Jalon | Critère d'acceptation | Statut (M7.5) | Test de vue | Scénario de démo | Test d'appareil |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| H-01 | Détection des langages (réglage, nom exact, motif, extension composée, première ligne, contenu) | P0 | présent | `core-language` | M7.6 | tableau de 30+ cas couvrant chaque rang de la précédence ; propriété : jamais deux noms exacts revendiqués | Câblé | DetectionPaquetsIntegresTest | non | — |
| H-02 | Configuration de langage pilotée par les données (commentaires, fermeture automatique, entourage, plis, indentation) | P0 | présent (codée en dur par classe) | `core-language` | M7.6 | UN test paramétré sur tous les langages intégrés ; plus aucun aiguillage par identifiant | Câblé | PaquetsIntegresParametresTest | non | — |
| H-03 | Paquets de langage intégrés (seize langages en données) | P0 | absent | `core-language` | M7.6 | les seize paquets se chargent et se valident entiers ; l'index se génère à la construction | Câblé | PackGrammarsTest | non | — |
| H-04 | Ajouter un langage sans recompiler (paquet de l'utilisateur, dossier ou archive) | P0 | livré (celeris : M9 — les réglages de la démonstration copient le paquet jouet Logo au stockage ou le retirent, le registre se RECHARGE à chaud par le dossier des paquets, la carte tortue.lgo entre et sort de l'accueil à chaud ; le paquet entier vit en données seules — pack.json, language.json, grammar.json, outline.json, snippets.json — aucun mot de Logo dans le code) | `core-language` | M7.6 | le paquet jouet copié au stockage se charge à chaud, se colore, se plie et se commente ; sécurité : zip-slip, liens symboliques, tailles, profondeur, schéma — chacun refusé avec message précis | Démontré | PaquetJouetTest | oui | — |
| H-05 | Compatibilité TextMate réelle (captures objets, inclusions externes, injections L/R) | P0 | livré (celeris : H-05 — le moteur charge le format réel depuis l'étape 5 ; les injections L/R s'appliquent désormais par sélecteur de portées contre la pile courante (L : devant l'hôte, R : derrière), la forme catastrophique et l'inclusion inconnue écartent leur règle — comptées, jamais le paquet — et le banc du commerce charge les ONZE grammaires des extensions VS Code SANS MODIFICATION, seuils d'acceptation par grammaire : json 100 %, python 98,8 %, html 98,7 %, cpp 71,5 %) | `core-syntax-grammars` | M7.6 | au moins dix grammaires du commerce chargées sans modification | Câblé | GrammairesCommerceTest | non | — |
| H-06 | Langages incorporés (script dans HTML, code dans Markdown) | P1 | présent | `core-syntax-grammars` | M7.6 | la grammaire incorporée s'empile sur l'hôte, les deux se colorent | Noyau | — | non | — |
| H-07 | Thèmes par portées (sélecteurs TextMate, import tokenColors de VS Code) | P1 | livré (celeris : H-07 — la pile de portées vit dans le jeton de la voie des grammaires, internée dans un arbre de préfixes ; `ScopeTheme` résout par sélecteurs TextMate (descendance, exclusion, spécificité), fusionne les règles qui matchent et garde le résultat en cache par pile — le tracé n'alloue rien par image ; les thèmes clair, sombre et contraste élevé sont écrits ici, l'importeur `TokenColorsTheme` lit le format VS Code et la démo y bâtit son quatrième thème « Atelier », sa palette dérivée du fichier seul ; `Portees.de` disparaît, la sorte de jeton devient catégorie dérivée) | `core-syntax` | M7.6 | la couleur se résout depuis la pile de portées ; le tracé n'alloue rien par image | Câblé | TextPainterScopesTest | oui | — |
| H-08 | Analyse structurelle niveau 1 (plis et plan génériques par données) | P0 | présent | `core-syntax` | M7.6 | Python, YAML et SQL ont plis et plan sans une ligne de Java | Câblé | DataStructureTest | non | — |
| H-09 | Analyse structurelle niveau 2 (tree-sitter natif) | P2 | absent | — | M7.6 | ADR chiffré : si les budgets ne tiennent pas, l'ADR dit pourquoi (écarté — ADR-016) | Non démarré | — | non | — |
| H-10 | Analyse structurelle niveau 3 (analyseurs écrits factorisés, attachés par paquets) | P0 | présent (non factorisés) | `core-syntax` | M7.6 | Java, Kotlin, KTS et Groovy en spécialisations d'au plus 150 lignes ; instantanés identiques | Câblé | ClikeSnapshotsTest | non | — |
| H-11 | Client LSP sur LSP4J derrière la frontière `lsp-protocole` | P0 | livré (celeris : la parité est prononcée à l'étape 7 de M7.6 — les sept tests de ServeurRetireTest passent SANS MODIFICATION par la frontière lsp-protocole contre le serveur tiers retiré, l'archive découpée n'embarque plus la liaison ni son rouage JSON, copie unique ; le code maison est supprimé le même jour — le corpus de traces passe par le Gson de la bibliothèque, la couverture du domaine se reconstruit par construction directe ; les règles R8 sont prouvées sur la démo réduite — assembleRelease vert, 6,3 Mo contre 10,3, le dex vérifié porte la bibliothèque et sa copie unique de Gson, cinq scénarios rejoués sur le binaire réduit — ADR-015) | `core-lsp` | M7.6 | les tests de contrat passent par la nouvelle voie ; une seule copie de lsp4j et Gson dans l'APK | Noyau | — | non | — |
| H-12 | Couverture LSP nouvelle (symboles, formatage, plis, correctifs, indices en ligne) | P1 | absent | `core-lsp` | M7.6 | au moins douze méthodes raccordées à une interface | Câblé | ServeurRetireCouvertureTest | oui | — |
| H-13 | Serveurs déclarés dans les paquets (`servers.json`) | P1 | livré (celeris : H-13 — `LanguageServerDeclaration` du `core-language`, donnée pure validée au chargement, portée par les paquets et par l'hôte qui assemble ; les seize paquets intégrés n'en déclarent AUCUN, l'éditeur ne connaît pas les serveurs de ses hôtes — la démo déclare le sien en ressource `serveurs/servers.json`, `DeclaredServers` vérifie la disponibilité et monte par la désignation de la frontière, le repli sur le serveur de référence sinon, la session générique `DeclaredServerSession` ne demande que ce que la négociation annonce) | `core-language` | M7.6 | un langage dont le serveur est disponible reçoit ses fournisseurs LSP automatiquement | Câblé | DeclaredServersTest | oui | — |

## État vérifié de l'étalon (2026-11-04)

Vérifié sur la documentation officielle d'Android Studio et de la
plateforme (voir « Sources ») :

- la ligne stable est **2026.2.2** (le mandat citait la ligne 2025.3 d'avril
  2026 : l'étalon a avancé de deux lignes depuis — la matrice cible donc la
  ligne stable au jour de l'exécution, conformément au mandat) ;
- le mode agent, la gestion de permissions d'agent et les compétences
  d'agent sont confirmés sur les canaux récents ;
- le plugin Gradle Android stable est 9.5.0, Gradle 9.8.2, le niveau de SDK
  de compilation 36, et `targetSdk` 36 est exigé par le magasin depuis
  août 2026 ;
- la portée cumulée du niveau 26 d'API est ≈ 99 % des appareils actifs.

## Sources consultées (2026-11-04)

1. Notes de version d'Android Studio — documentation officielle de la
   plateforme : `https://developer.android.com/studio/releases` (tableau de
   compatibilité des versions, lignes 2025.3 → 2026.2).
2. Notes de version du plugin Gradle Android : `https://developer.android.com/build/releases` (9.5.0, octobre 2026).
3. Versions du système de build : `https://gradle.org/releases` (9.8.2, octobre 2026).
4. Tableau de distribution des niveaux d'API : `https://developer.android.com/about/dashboards`
   et agrégateur `https://apilevels.com` (portée cumulée du niveau 26 ≈ 99 %,
   données d'octobre 2026).
5. Guide « Write your app » de la plateforme : `https://developer.android.com/studio/write`
   (capacités d'édition de l'étalon).

> Les URL ci-dessus pointent vers la documentation officielle de la
> plateforme Android ; l'identification nominative de l'étalon est réservée
> au prompt du mandat et aux échanges avec le propriétaire, jamais au dépôt.
