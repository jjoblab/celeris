package jo.celeris.demo;

import java.util.List;

/**
 * Catalogue des scénarios de démonstration : chaque jalon y ajoute les
 * scénarios de la matrice de parité qu'il couvre, avec leur test de
 * fumée correspondant.
 *
 * <p>Jalon M2 (moteur de texte, JVM pure) : A-13 (annulation groupée avec
 * restauration de sélection), A-18 (retour à la ligne souple — la coupe
 * par caractère du cœur, la géométrie des rangées venant avec la vue),
 * A-20 (recherche/remplacement par la voie de transaction), A-24
 * (encodages, fins de ligne, BOM) et A-25 (très gros fichiers côté
 * modèle). Les facettes interface de ces scénarios s'ajouteront à leur
 * jalon respectif (M3, M5).</p>
 */
public final class ScenarioCatalog {

    private ScenarioCatalog() {
    }

    /** Scénarios disponibles, dans l'ordre de la matrice de parité. */
    public static List<Scenario> scenarios() {
        return List.of(
                new Scenario(
                        "A-13",
                        "Annulation groupée, sélection restaurée",
                        "Cent frappes successives s'annulent en une seule opération "
                                + "et la sélection d'avant est restituée exactement "
                                + "(PIEGE-07 couvert)."),
                new Scenario(
                        "A-18",
                        "Retour à la ligne souple",
                        "Un mot plus large que la largeur disponible est coupé par "
                                + "caractères (PIEGE-08) ; les rangées restent "
                                + "distinctes des lignes."),
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
}
