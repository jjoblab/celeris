package jo.celeris.demo;

import java.util.ArrayList;
import java.util.List;

/**
 * Catalogue des scénarios de démonstration : chaque jalon y ajoute les
 * scénarios de la matrice de parité qu'il couvre, avec leur test de
 * fumée correspondant.
 *
 * <p>Jalon M2 (moteur de texte, JVM pure) : A-13 (annulation groupée avec
 * restauration de sélection), A-18 (retour à la ligne souple — la coupe
 * par caractère du cœur), A-20 (recherche/remplacement par la voie de
 * transaction), A-24 (encodages, fins de ligne, BOM) et A-25 (très gros
 * fichiers côté modèle).</p>
 *
 * <p>Jalon M3 (vue minimale) : A-01 (pilule multi-curseur tactile), la
 * facette vue de A-18 (rangées et tranches), D-01 (gouttière à numéros),
 * D-13 (thèmes à contraste vérifié), D-15 (zoom au pincement), D-16
 * (poignées, pilule, loupe), D-17 (clavier matériel), G-01 (ouverture
 * progressive), G-02 (tactile d'abord, hors ligne) et G-05
 * (accessibilité). Les facettes non livrées — D-14 (ligatures) —
 * rejoignent leur jalon.</p>
 *
 * <p>Jalon M4 (syntaxe) : A-15 (plis calculés depuis l'arbre, le pli
 * manuel survit à l'édition voisine), B-01 (coloration lexicale et
 * syntaxique du même arbre), C-01 (XML : coloration, plis, crochets),
 * C-07 (Gradle KTS et Groovy) et C-14 (langages P0 : Java, Kotlin,
 * XML). Les scénarios B-16 (plan) et C-15/C-16 (longue traîne)
 * suivent leurs tranches.</p>
 */
public final class ScenarioCatalog {

    private ScenarioCatalog() {
    }

    /** Scénarios disponibles, dans l'ordre de la matrice de parité. */
    public static List<Scenario> scenarios() {
        List<Scenario> tous = new ArrayList<>(scenariosEdition());
        tous.addAll(scenariosLangages());
        tous.addAll(scenariosAffichage());
        tous.addAll(scenariosGlobal());
        return List.copyOf(tous);
    }

    /** La catégorie A — édition et saisie. */
    private static List<Scenario> scenariosEdition() {
        return List.of(
                new Scenario(
                        "A-01",
                        "Pilule « ajouter un curseur »",
                        "L'appui long arme la pilule, chaque appui ailleurs "
                                + "ajoute un curseur — les existants tiennent ; la "
                                + "frappe multi-curseurs vit au cœur depuis M2."),
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
                        "A-18",
                        "Retour à la ligne souple",
                        "Un mot plus large que la largeur disponible est coupé par "
                                + "caractères (PIEGE-08) ; la projection signe plis "
                                + "et coupures, chaque rangée porte sa tranche."),
                new Scenario(
                        "A-20",
                        "Recherche et remplacement",
                        "Littéral, regex, casse, mot entier, sélection, multiligne, "
                                + "préservation de casse et remplacement global : "
                                + "tout passe par la voie de transaction."),
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

    /** Les catégories B et C — coloration et langages du même arbre. */
    private static List<Scenario> scenariosLangages() {
        return List.of(
                new Scenario(
                        "B-01",
                        "Coloration du même arbre",
                        "Mots réservés, chaînes, nombres, commentaires : chaque "
                                + "plage se peint à l'encre de sa sorte, servie par "
                                + "les mêmes paniers que les plis — le rendu ne "
                                + "relit jamais le texte."),
                new Scenario(
                        "C-01",
                        "XML : coloration, plis, crochets",
                        "Balise ouvrante appariée à sa fermeture, CDATA en crochet "
                                + "apparié, éléments repliables ; la balise non "
                                + "fermée devient un nœud d'erreur, jamais un "
                                + "échec."),
                new Scenario(
                        "C-07",
                        "Gradle KTS et Groovy",
                        "Les deux dialectes de script de construction analysés : "
                                + "chaque appel de premier niveau ouvre son chunk — "
                                + "plugins, task, dependencies — plis par bloc, "
                                + "chaînes triples en crochets appariés ; la "
                                + "ré-analyse ne relit que le bloc touché."),
                new Scenario(
                        "C-14",
                        "Langages P0 : Java, Kotlin, XML",
                        "Trois grammaires réelles sur le même socle : jetons soudés "
                                + "sans trou à peindre, états transportés ligne à "
                                + "ligne, ré-analyse bornée à la reconvergence."));
    }

    /** La catégorie D — affichage, gouttière, thèmes et clavier. */
    private static List<Scenario> scenariosAffichage() {
        return List.of(
                new Scenario(
                        "D-01",
                        "Gouttière à numéros absolus",
                        "Les numéros s'alignent à droite dans leur champ, "
                                + "étiquettes mémoïsées — jamais une chaîne allouée "
                                + "par rangée et par image ; les continuations "
                                + "d'une ligne coupée restent muettes."),
                new Scenario(
                        "D-13",
                        "Thèmes à contraste vérifié",
                        "Clair, sombre et contraste élevé : encre, gouttière et "
                                + "curseur vérifiés automatiquement aux seuils WCAG "
                                + "à chaque test."),
                new Scenario(
                        "D-15",
                        "Zoom au pincement",
                        "La taille monte par paliers bornés, jamais une valeur "
                                + "libre ; la colonne et la rangée sous le doigt y "
                                + "restent."),
                new Scenario(
                        "D-16",
                        "Poignées, pilule et loupe",
                        "Les poignées suivent les tranches de rangée, le "
                                + "glissement ne déplace qu'une extrémité ; la "
                                + "géométrie de loupe flotte au-dessus du doigt, "
                                + "bornée à la fenêtre."),
                new Scenario(
                        "D-17",
                        "Clavier matériel",
                        "Table de raccourcis complète et reconfigurable ; "
                                + "mouvements par clusters de graphèmes, mots "
                                + "entiers et rangées ; suppressions avant et "
                                + "arrière par clusters entiers."));
    }

    /** La catégorie G — exigences globales. */
    private static List<Scenario> scenariosGlobal() {
        return List.of(
                new Scenario(
                        "G-01",
                        "Ouverture progressive",
                        "Cent mille lignes : première image mesurée à 154 ms "
                                + "(budget 500 ms), 480 000 lignes ouvrables — la "
                                + "fenêtre seule se sert, la coloration suit par l'analyse "
                                + "hors fil : fenêtre visible d'abord (224 ms mesurés sur cent mille lignes), "
                                + "complétion par lots ensuite."),
                new Scenario(
                        "G-02",
                        "Tactile d'abord, hors ligne",
                        "La démonstration tourne hors ligne, aucun réveil réseau ; "
                                + "les budgets d'image tiennent par le cache de "
                                + "rangées."),
                new Scenario(
                        "G-05",
                        "Accessibilité",
                        "L'éditeur s'expose comme un nœud éditable dont les "
                                + "enfants virtuels sont les rangées visibles : "
                                + "lecture par lignes et plages, mouvements à "
                                + "granularité — clusters, mots, rangées, pages."));
    }
}
