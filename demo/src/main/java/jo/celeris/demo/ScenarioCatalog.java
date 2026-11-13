package jo.celeris.demo;

import java.util.ArrayList;
import java.util.List;

/**
 * Catalogue des scénarios de démonstration : chaque jalon y ajoute les
 * scénarios de la matrice de parité qu'il couvre, avec leur test de
 * fumée correspondant.
 *
 * <p>Jalon M2 (moteur de texte, JVM pure) : A-13 (annulation groupée),
 * A-18 (retour à la ligne souple), A-20 (recherche par transaction),
 * A-24 (encodages, fins de ligne, BOM) et A-25 (très gros fichiers).</p>
 *
 * <p>Jalon M3 (vue minimale) : A-01 (pilule multi-curseur tactile), la
 * facette vue de A-18, D-01 (gouttière), D-13 (thèmes à contraste),
 * D-15 (zoom), D-16 (poignées, pilule, loupe), D-17 (clavier), G-01
 * (ouverture progressive), G-02 (tactile, hors ligne) et G-05
 * (accessibilité) — D-14 (ligatures) rejoint son jalon.</p>
 *
 * <p>Jalon M5 (édition assistée) : A-02 (sélection en colonne),
 * A-03 (sélection syntaxique), A-05 à A-09 (frappe intelligente :
 * paires, indentation, retour arrière, entourer, collage), A-10 à
 * A-12 (lignes : déplacer, commenter, trier), A-16 (lignes
 * collantes), A-19 (caractères invisibles), A-22 (gabarits
 * dynamiques), la facette barre de A-20, B-14 (historique de
 * navigation), B-15/D-06 (signets et gouttière), D-08 (bande
 * d'erreurs), D-09 (fil d'Ariane), D-10 (popups unifiées), D-18
 * (stylet, souris, pavé) et G-04 (plugins isolés).</p>
 *
 * <p>Jalon M6 (sémantique) : B-02 (jetons sémantiques tirés à la
 * demande), B-03 (complétion de base), B-04 (complétion typée, la
 * coupure honorée), B-05 (import automatique), B-06 (squelettes de
 * méthode — le bloqué historique débloqué), B-07 (aide aux paramètres,
 * le paramètre courant souligné), B-08 (indices en ligne), B-09
 * (documentation au survol, à la demande), B-10 (navigation à la
 * déclaration), B-11 (usages raffinés au serveur), B-13 (aller à
 * symbole), B-17 (correctifs par transaction), B-19 (renommage
 * multi-fichiers), B-24 (génération de code), B-25 (formatage
 * idempotent), B-26 (éditeurconfig) et B-27 (optimisation des
 * imports) ; le corpus Android et les traces de contrat portent les
 * critères du jalon.</p>
 *
 * <p>Jalon M4 (syntaxe) : A-15 (plis calculés depuis l'arbre, le pli
 * manuel survit à l'édition voisine), B-01 (coloration lexicale et
 * syntaxique du même arbre), B-16 (plan du fichier consommateur du
 * même arbre, mise à jour incrémentale), C-01 (XML : coloration, plis,
 * crochets), C-07 (Gradle KTS et Groovy), C-14 (langages P0 : Java,
 * Kotlin, XML), C-15 (le reste des P1 par la voie de grammaires) et
 * C-16 (la longue traîne par grammaire, sans analyseur dédié). SQL,
 * C et C++ suivent leur jalon.</p>
 *
 * <p>Jalon M7 (Android et SPI) : E-01 à E-06 — les contrats
 * d'intégration de l'hôte et leurs implémentations de référence
 * testées sans appareil — puis la peinture de gouttière
 * D-03/D-04/D-05 et le bloc XML C-02/C-03/C-04, le manifeste C-06 et
 * les catalogues C-08. L'adaptateur de stockage suit.</p>
 *
 * <p>Jalon M7.6 (plateforme de langages) : H-12 — la couverture LSP
 * nouvelle par la frontière protocolaire (ADR-015).</p>
 */
public final class ScenarioCatalog {

    private ScenarioCatalog() {
    }

    /** Scénarios disponibles, dans l'ordre de la matrice de parité. */
    public static List<Scenario> scenarios() {
        List<Scenario> tous = new ArrayList<>(scenariosEdition());
        tous.addAll(scenariosLangages());
        tous.addAll(scenariosAffichage());
        tous.addAll(integrationHote());
        tous.addAll(iaNative());
        tous.addAll(scenariosGlobal());
        return List.copyOf(tous);
    }

    /** La catégorie E — les contrats d'intégration de l'hôte (M7). */
    private static List<Scenario> integrationHote() {
        return ScenariosIntegration.tous();
    }

    /** La catégorie F — l'IA native (M8). */
    private static List<Scenario> iaNative() {
        return ScenariosIA.tous();
    }

    /** A-01 à A-03 — curseurs, colonne, syntaxe. */
    private static List<Scenario> editionCurseurs() {
        return List.of(

                new Scenario(
                        "A-01",
                        "Appui long du dépôt de départ",
                        "L'appui long sélectionne le mot sous le doigt, sans "
                                + "AUCUN rectangle plein qui le couvre (le "
                                + "redressement du 2026-11-13 : la pilule "
                                + "« ajouter un curseur » se retire, la barre "
                                + "de sélection du dépôt de départ l'offre) ; "
                                + "la frappe multi-curseurs vit au cœur depuis "
                                + "M2."),
                new Scenario(
                        "A-02",
                        "Sélection en colonne",
                        "Le glissement rectangulaire devient une plage par rangée, bornes "
                                + "visuelles exactes — la tranche du retour à la ligne souple "
                                + "comprise, la ligne courte s'arrête à sa fin."),
                new Scenario(
                        "A-03",
                        "Sélection syntaxique",
                        "Étendre monte la chaîne des étendues englobantes — mot, "
                                + "instruction, bloc, type, document — réduire la redescend : le "
                                + "même arbre que la coloration sert la sélection."));
    }

    /** A-05 à A-08 — paires, indentation, retour arrière, entourer. */
    private static List<Scenario> editionFrappe() {
        return List.of(
                new Scenario(
                        "A-05",
                        "Fermeture automatique des paires",
                        "L'ouvrant insère sa paire, curseur au milieu ; la citation se "
                                + "dédouble ; l'ouvrant sur une sélection l'entoure ; le fermant "
                                + "déjà présent est sauté — pas de double fermeture."),
                new Scenario(
                        "A-06",
                        "Indentation automatique",
                        "L'Entrée porte le retrait lu dans l'arbre au point de coupe ; "
                                + "le fermant restant descend seul sous le curseur ; le style — "
                                + "espaces ou tabulations — appartient à l'hôte. Sur Java, deux "
                                + "gestes dédiés s'ajoutent : le curseur dans une chaîne la ferme, "
                                + "pose la concaténation et la rouvre à la ligne ; l'Entrée qui "
                                + "suit un label de cas gagne un cran — le corps du cas vit sous "
                                + "son étiquette."),
                new Scenario(
                        "A-07",
                        "Retour arrière intelligent",
                        "La paire vide disparaît entière, la citation vide aussi, "
                                + "l'indentation excédentaire se retire d'un cran ; le reste "
                                + "retombe sur les clusters de graphèmes du cœur."),
                new Scenario(
                        "A-08",
                        "Entourer la sélection",
                        "Accolades, guillemets ou bloc multiligne : le contenu gagne un "
                                + "cran entre les délimiteurs, le curseur se pose après le "
                                + "fermant, chaque plage de la sélection est entourée."));
    }

    /** A-09 à A-12 — collage, lignes, commentaires, tri. */
    private static List<Scenario> editionActionsDeLignes() {
        return List.of(
                new Scenario(
                        "A-09",
                        "Collage avec réindentation",
                        "La première ligne du collage ne bouge pas ; les suivantes "
                                + "s'alignent sur la profondeur d'accueil lue dans l'arbre, la "
                                + "structure relative du collage conservée."),
                new Scenario(
                        "A-10",
                        "Déplacer, dupliquer, joindre, supprimer des lignes",
                        "Le bloc de lignes touchées est l'unité du geste — monter, "
                                + "descendre, dupliquer (les curseurs descendent dans la copie), "
                                + "joindre, supprimer ; la multi-sélection opère chaque bloc."),
                new Scenario(
                        "A-11",
                        "Commenter et décommenter",
                        "Le préfixe suit le langage — Java au double barre, XML au bloc, "
                                + "Python au dièse ; JSON n'a pas de commentaire : l'action est "
                                + "masquée. Le basculement est tout ou rien, la ligne vide ne "
                                + "compte pas."),
                new Scenario(
                        "A-12",
                        "Tri de lignes naturel",
                        "Les tranches numériques se comparent en nombres — « item2 » "
                                + "avant « item10 » — le reste en chaînes, la casse au choix de "
                                + "l'hôte."));
    }

    /** A-13 à A-18 — annulation, plis, collantes, rangées. */
    private static List<Scenario> editionHistoriqueEtRendu() {
        return List.of(
                new Scenario(
                        "A-13",
                        "Annulation groupée, sélection restaurée",
                        "Cent frappes successives s'annulent en une seule opération "
                                + "et la sélection d'avant est restituée exactement "
                                + "(PIEGE-07 couvert)."),
                new Scenario(
                        "A-15",
                        "Plis par niveaux, depuis l'arbre",
                        "Les régions pliables viennent de l'arbre syntaxique — "
                                + "déclarations, commentaires multi-lignes ; le pli "
                                + "manuel survit à l'édition voisine : il se décale "
                                + "quand le document bouge ailleurs."),
                new Scenario(
                        "A-16",
                        "Lignes collantes",
                        "Les en-têtes des structures qui englobent la première ligne "
                                + "visible restent collés en haut du rendu — le budget d'image "
                                + "est tenu, les plus profondes l'emportent."),
                new Scenario(
                        "A-18",
                        "Retour à la ligne souple",
                        "Un mot plus large que la largeur disponible est coupé par "
                                + "caractères (PIEGE-08) ; la projection signe plis "
                                + "et coupures, chaque rangée porte sa tranche."));
    }

    /** A-19 à A-25 — invisibles, recherche, gabarits, encodages. */
    private static List<Scenario> editionRechercheEtGabarits() {
        return List.of(
                new Scenario(
                        "A-19",
                        "Caractères invisibles",
                        "L'espace finale, la tabulation et l'espace fine se "
                                + "matérialisent toujours ; l'espace interne en mode « tout », la "
                                + "fin de ligne sur demande."),
                new Scenario(
                        "A-20",
                        "Recherche et remplacement",
                        "Littéral, regex, casse, mot entier, sélection, multiligne, "
                                + "préservation de casse et remplacement global : "
                                + "tout passe par la voie de transaction ; la barre "
                                + "porte les options, la navigation circulaire des "
                                + "occurrences et l'ouverture depuis le curseur."),
                new Scenario(
                        "A-22",
                        "Gabarits dynamiques",
                        "Des trous tabulables dans l'ordre du rendu, des variables "
                                + "liées — l'édition d'une occurrence se propage à ses sœurs au "
                                + "même décalage relatif."),
                new Scenario(
                        "A-24",
                        "Encodages, fins de ligne, BOM",
                        "UTF-8, UTF-16 et Latin-1 font des allers-retours ; CRLF, "
                                + "LF et CR sont détectés, le BOM est préservé — "
                                + "sans heuristique réseau."),
                new Scenario(
                        "A-25",
                        "Très gros fichiers",
                        "Cent mille lignes s'ouvrent côté modèle dans les budgets "
                                + "de l'ADR-006 : première image et défilement "
                                + "restent fluides."));
    }

    /** La catégorie A — édition et saisie. */
    private static List<Scenario> scenariosEdition() {
        List<Scenario> tous = new ArrayList<>(editionCurseurs());
        tous.addAll(editionFrappe());
        tous.addAll(editionActionsDeLignes());
        tous.addAll(editionHistoriqueEtRendu());
        tous.addAll(editionRechercheEtGabarits());
        return tous;
    }

    /** Les catégories B et C — coloration et langages du même arbre. */
    private static List<Scenario> scenariosLangages() {
        List<Scenario> tous = new ArrayList<>(langagesAnalyseurs());
        tous.addAll(langagesGrammaires());
        return tous;
    }

    /** B-01 — la coloration lexicale du même arbre. */
    private static List<Scenario> structureDuFichier() {
        return List.of(

                new Scenario(
                        "B-01",
                        "Coloration du même arbre",
                        "Mots réservés, chaînes, nombres, commentaires : chaque "
                                + "plage se peint à l'encre de sa sorte, servie par "
                                + "les mêmes paniers que les plis — le rendu ne "
                                + "relit jamais le texte."));
    }

    /** B-02 à B-08 — la sémantique du jalon M6 : jetons, complétion, gabarits, indices. */
    private static List<Scenario> semantiqueInteractive() {
        return List.of(
                new Scenario(
                        "B-02",
                        "Coloration sémantique",
                        "Les jetons sémantiques tirés à la demande colorent "
                                + "paramètres, champs et statiques — le flux vit "
                                + "seul, sans aucun diagnostic (PIEGE-18)."),
                new Scenario(
                        "B-03",
                        "Complétion de base",
                        "Le préfixe tape, les mots du fichier répondent — hors "
                                + "chaînes et commentaires, sans serveur ; le mot en "
                                + "cours n'est jamais candidat de lui-même."),
                new Scenario(
                        "B-04",
                        "Complétion typée du serveur",
                        "La liste du serveur garde son ordre et sa coupure — "
                                + "jamais réordonnée ni complétée localement "
                                + "(PIEGE-17) ; l'interactif devance le fond."),
                new Scenario(
                        "B-05",
                        "Import automatique",
                        "Le candidat s'insère en tête du bloc des imports par une "
                                + "édition de position unique ; l'import déjà présent "
                                + "ne se re-insère jamais. Adossé au classpath de "
                                + "l'hôte, le nom simple sous le curseur propose ses "
                                + "noms qualifiés, dans l'ordre de l'index — jamais "
                                + "celui qui est déjà importé."),
                new Scenario(
                        "B-06",
                        "Squelettes de méthode",
                        "La signature du serveur devient un squelette aux trous "
                                + "tabulables, le corps en dernier — l'annulation "
                                + "intégrale, le bloqué historique débloqué."),
                new Scenario(
                        "B-07",
                        "Aide aux paramètres",
                        "La signature au-dessus du curseur, le paramètre courant "
                                + "souligné — la plage cherchée entre parenthèses, "
                                + "l'index borné à l'arité."),
                new Scenario(
                        "B-08",
                        "Indices en ligne",
                        "Types inférés et noms de paramètres posés dans la marge "
                                + "— cellule d'encre pleine, même ligne de base "
                                + "(PIEGE-11/12) ; les parties cliquables mènent "
                                + "à leur cible."));
    }

    /** B-09 à B-13 — survol, navigation et usages du jalon M6. */
    private static List<Scenario> semantiqueNavigation() {
        return List.of(
                new Scenario(
                        "B-09",
                        "Documentation au survol",
                        "La documentation se résout à la demande — blocs typés "
                                + "distincts, le titre jamais fondu dans le corps "
                                + "(PIEGE-16)."),
                new Scenario(
                        "B-10",
                        "Navigation à la déclaration",
                        "L'index répond vite hors ligne, le serveur répond juste "
                                + "en ligne — jamais de chaînage ; le retour au "
                                + "point de départ vit à l'historique."),
                new Scenario(
                        "B-11",
                        "Usages dans le projet",
                        "Occurrences entières à casse exacte par l'index — jamais "
                                + "dans les chaînes ni les commentaires — le "
                                + "raffinement sémantique au serveur."),
                new Scenario(
                        "B-13",
                        "Aller à symbole",
                        "La recherche floue ordonne identique, préfixe, début de "
                                + "mot, sous-chaîne, sous-suite — la requête "
                                + "muette montre tout."));
    }

    /** B-14 à B-16 — historique, signets et plan (M4/M5). */
    private static List<Scenario> navigationEtPlan() {
        return List.of(
                new Scenario(
                        "B-14",
                        "Historique de navigation",
                        "La pile bornée des positions visitées : retour et avance, la "
                                + "visite qui coupe l'avance, la fermeture de fichier qui vide "
                                + "tout."),
                new Scenario(
                        "B-15",
                        "Signets avec gouttière",
                        "Ajout et retrait par ligne, saut au suivant et au précédent en "
                                + "boucle ; la persistance par session suit les lignes à travers "
                                + "les éditions, par la projection des offsets."),
                new Scenario(
                        "B-16",
                        "Plan du fichier (structure)",
                        "Le plan sort du même arbre que la coloration : classes, "
                                + "méthodes et champs avec leurs bornes ; l'édition le "
                                + "met à jour par incrément — le préfixe intact se "
                                + "conserve, le consommateur reçoit le compte de ce "
                                + "qui change."));
    }

    /** B-17 à B-27 — refactorisation, génération, formatage du jalon M6. */
    private static List<Scenario> refactorisationEtGeneration() {
        return List.of(
                new Scenario(
                        "B-17",
                        "Correctifs rapides",
                        "Les titres à montrer, les éditions à appliquer par "
                                + "transaction diffable et annulable — le correctif "
                                + "sans édition reste un correctif."),
                new Scenario(
                        "B-18",
                        "Aperçu d'un correctif",
                        "Le correctif proposé sur la sélection se prévise "
                                + "AVANT l'acceptation : les lignes en moins et "
                                + "en plus se lisent préfixées, le contexte "
                                + "encadrant — l'acceptation applique par "
                                + "transaction, annulable d'un geste ; le refus "
                                + "ne touche à rien."),
                new Scenario(
                        "B-19",
                        "Renommage multi-fichiers",
                        "Le plan par occurrence, fichier par fichier, sur l'index "
                                + "ou au serveur — les conflits signalés, jamais "
                                + "tranchés (ambiguïté, collision)."),
                new Scenario(
                        "B-24",
                        "Génération de code",
                        "Constructeur, accesseurs aux conventions — le booléen "
                                + "en is —, equals/hashCode/toString : posés avant "
                                + "l'accolade fermante de la classe."),
                new Scenario(
                        "B-25",
                        "Formatage du code",
                        "L'indentation se règle sur les niveaux de l'arbre — "
                                + "idempotent par construction, les lignes vides "
                                + "restent vides."),
                new Scenario(
                        "B-26",
                        "Éditeurconfig respecté",
                        "Les sections par extension, la plus spécifique gagne "
                                + "— retrait, fins de ligne et encodage pilotés "
                                + "par le fichier."),
                new Scenario(
                        "B-27",
                        "Optimisation des imports",
                        "Chaque import rapporté utilisé ou non — signalé, jamais "
                                + "supprimé en silence : l'analyseur éclaire, "
                                + "l'hôte décide."));
    }

    /** C — les analyseurs dédiés des langages P0. */
    /** C — les analyseurs dédiés des langages P0 et le bloc Android (M7-t4). */
    private static List<Scenario> langagesP0() {
        List<Scenario> tous = new ArrayList<>();
        tous.add(new Scenario(
                "C-01",
                "XML : coloration, plis, crochets",
                "Balise ouvrante appariée à sa fermeture, CDATA en crochet "
                        + "apparié, éléments repliables ; la balise non "
                        + "fermée devient un nœud d'erreur, jamais un "
                        + "échec."));
        tous.addAll(ScenariosAndroid.langagePreGradle());
        tous.add(new Scenario(
                "C-07",
                "Gradle KTS et Groovy",
                "Les deux dialectes de script de construction analysés : "
                        + "chaque appel de premier niveau ouvre son chunk — "
                        + "plugins, task, dependencies — plis par bloc, "
                        + "cha\u00eenes triples en crochets appariés ; la "
                        + "ré-analyse ne relit que le bloc touché."));
        tous.addAll(ScenariosAndroid.cataloguesVersions());
        tous.add(new Scenario(
                "C-10",
                "Compose : documentation des modificateurs au survol",
                "Le modificateur d'agencement survolé sert sa table — "
                        + "padding, background, size… et le récepteur "
                        + "Modifier lui-même : l'appui long du doigt "
                        + "sélectionne le nom, la documentation s'ancre "
                        + "au curseur, le nom connu seul décide (C-10)."));
        tous.add(new Scenario(
                "C-14",
                "Langages P0 : Java, Kotlin, XML",
                "Trois grammaires réelles sur le même socle : jetons soudés "
                        + "sans trou à peindre, états transportés ligne à "
                        + "ligne, ré-analyse bornée à la reconvergence."));
        return List.copyOf(tous);
    }

    /** La structure, la sémantique et les langages à analyseurs dédiés. */
    private static List<Scenario> langagesAnalyseurs() {
        List<Scenario> tous = new ArrayList<>(structureDuFichier());
        tous.addAll(semantiqueInteractive());
        tous.addAll(semantiqueNavigation());
        tous.addAll(navigationEtPlan());
        tous.addAll(refactorisationEtGeneration());
        tous.addAll(langagesP0());
        return tous;
    }

    /** Les langages à grammaires — la longue traîne sans analyseur dédié. */
    private static List<Scenario> langagesGrammaires() {
        return List.of(
                new Scenario(
                        "C-15",
                        "Langages P1 par grammaires",
                        "TOML, YAML, properties et Markdown se colorent et se "
                                + "replient par grammaire déclarative — motifs, "
                                + "blocs début/fin, crochets appariés — la même "
                                + "palette et le même socle incrémental que les "
                                + "analyseurs dédiés."),
                new Scenario(
                        "C-16",
                        "Longue traîne : Python, Shell",
                        "Les langages P2 sans analyseur dédié : la grammaire les "
                                + "colore, les chaînes triples et les corps multi-lignes "
                                + "se replient ; l'édition ne relit que la ligne "
                                + "touchée (PIEGE-21)."));
    }

    /** D-01, D-03 à D-06, D-08 — gouttière, intégrations et bande d'erreurs. */
    private static List<Scenario> affichageGouttiere() {
        List<Scenario> tous = new ArrayList<>();
        tous.add(new Scenario(
                "D-01",
                "Gouttière à numéros absolus",
                "Les numéros s'alignent à droite dans leur champ, "
                        + "étiquettes mémo\u00efsées — jamais une cha\u00eene allouée "
                        + "par rangée et par image ; les continuations "
                        + "d'une ligne coupée restent muettes."));
        tous.addAll(ScenariosAndroid.gouttiere());
        tous.add(new Scenario(
                "D-06",
                "Gouttière des signets",
                "La gouttière porte la marque des lignes signet — le même modèle "
                        + "que les sauts, peint à l'emplacement des numéros."));
        tous.add(new Scenario(
                "D-08",
                "Bande d'erreurs annotée",
                "Les diagnostics du même arbre alignent leurs marques le long du "
                        + "défilement — densité bornée à un cran, le clic saute au "
                        + "diagnostic. La reprise M7.5 la porte au bord droit de la "
                        + "couche porte-popups, l'appui vise le cran le plus proche."));
        return List.copyOf(tous);
    }

    /** D-09, D-10, D-13 — fil d'Ariane, popups, thèmes. */
    private static List<Scenario> affichageBandeEtThemes() {
        return List.of(
                new Scenario(
                        "D-09",
                        "Fil d'Ariane",
                        "Le chemin du symbole courant sort du plan du même arbre, de la "
                                + "plus externe à la plus interne ; la troncature par la largeur "
                                + "élague la tête en ellipse. La reprise M7.5 l'étale en bandeau "
                                + "haut de l'éditeur, il suit le curseur et survit au défilement."),
                new Scenario(
                        "D-10",
                        "Popups unifiées",
                        "Complétion, documentation et signature coordonnées autour du "
                                + "curseur — jamais superposées, jamais hors écran ; le contenu "
                                + "s'annonce à l'explorateur. La reprise M7.5 pose les dix popups "
                                + "sur la couche, aucune ne prend jamais le focus (PIEGE-27)."),
                new Scenario(
                        "D-13",
                        "Thèmes à contraste vérifié",
                        "Clair, sombre et contraste élevé : encre, gouttière et "
                                + "curseur vérifiés automatiquement aux seuils WCAG "
                                + "à chaque test."));
    }

    /** D-15 à D-18 — zoom, poignées, clavier, stylet. */
    private static List<Scenario> affichagePeripheriques() {
        return List.of(
                new Scenario(
                        "D-15",
                        "Zoom au pincement",
                        "La taille monte par paliers bornés, jamais une valeur "
                                + "libre ; la colonne et la rangée sous le doigt y "
                                + "restent."),
                new Scenario(
                        "D-16",
                        "Poignées et loupe",
                        "Les poignées suivent les tranches de rangée — des "
                                + "cercles pleins SOUS la rangée d'ancre, le "
                                + "glissement ne déplace qu'une extrémité ; la "
                                + "loupe du dragage d'appui long flotte "
                                + "au-dessus du doigt, Y snappé à la rangée, X "
                                + "amorti, bornée à la fenêtre."),
                new Scenario(
                        "D-17",
                        "Clavier matériel",
                        "Table de raccourcis complète et reconfigurable ; "
                                + "mouvements par clusters de graphèmes, mots "
                                + "entiers et rangées ; suppressions avant et "
                                + "arrière par clusters entiers."),
                new Scenario(
                        "D-18",
                        "Stylet, souris, pavé tactile",
                        "La molette avec Ctrl zoome par crans bornés, sans Ctrl elle "
                                + "défile ; l'info-bulle du survol paraît après le temps de "
                                + "station, le déplacement la retire."));
    }

    /** La catégorie D — affichage, gouttière, thèmes et clavier. */
    private static List<Scenario> scenariosAffichage() {
        List<Scenario> tous = new ArrayList<>(affichageGouttiere());
        tous.addAll(affichageBandeEtThemes());
        tous.addAll(affichagePeripheriques());
        return tous;
    }

    /** La catégorie G — exigences globales, la catégorie H — langages. */
    private static List<Scenario> scenariosGlobal() {
        return ScenariosGlobales.tous();
    }
}
