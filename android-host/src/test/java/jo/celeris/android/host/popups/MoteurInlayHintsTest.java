package jo.celeris.android.host.popups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.android.view.IndiceInlay;
import jo.celeris.core.text.Document;

/**
 * Le moteur des indices en ligne (inlay hints — l'audit Android Studio
 * du 2026-11-14) : le type du « var » LITTÉRAL se lit en fin de ligne
 * utile — la convention du simulateur de référence (« var resultat =
 * solde; » porte « : int »). Pur, sans appareil : la syntaxe seule
 * décide, JAMAIS le moteur ne devine — un « var » à main-d'œuvre
 * inconnue ne porte AUCUN indice.
 */
final class MoteurInlayHintsTest {

    /** L'indice unique du document — sa ligne et son rendu. */
    private static IndiceInlay unique(Document document) {
        List<IndiceInlay> indices = MoteurInlayHints.indices(document);
        assertEquals(1, indices.size(), "un var littéral, un indice");
        return indices.get(0);
    }

    @Test
    void lesTypesLitterauxSYLisent() {
        assertEquals(new IndiceInlay(1, ": int"),
                unique(Document.of("var solde = 500;")));
        assertEquals(new IndiceInlay(1, ": long"),
                unique(Document.of("var compteur = 10L;")));
        assertEquals(new IndiceInlay(1, ": double"),
                unique(Document.of("var taux = 1.5;")));
        assertEquals(new IndiceInlay(1, ": float"),
                unique(Document.of("var poids = 70.5f;")));
        assertEquals(new IndiceInlay(1, ": float"),
                unique(Document.of("var poids = 5f;")));
        assertEquals(new IndiceInlay(1, ": boolean"),
                unique(Document.of("var actif = true;")));
        assertEquals(new IndiceInlay(1, ": String"),
                unique(Document.of("var nom = \"Celeris\";")));
        assertEquals(new IndiceInlay(1, ": char"),
                unique(Document.of("var initiale = 'c';")));
    }

    @Test
    void laLigneDUneIndentationCompte() {
        // la ligne 13 du simulateur : var au milieu du document — la
        // main-d'œuvre LITTÉRALE seule se tranche (jamais devinée)
        List<IndiceInlay> indices = MoteurInlayHints.indices(Document.of(
                "public int consulter() {\n"
                        + "        var resultat = 0;\n"
                        + "        return solde;\n"
                        + "    }"));
        assertEquals(List.of(new IndiceInlay(2, ": int")), indices);
    }

    @Test
    void laMainDoeuvreInconnueNePorteAucunIndice() {
        // jamais deviné : identifiant, appel, null, champ typé
        assertTrue(MoteurInlayHints.indices(Document.of(
                "var copie = solde;\n"
                        + "var recu = consulter();\n"
                        + "var rien = null;\n"
                        + "var hexa = 0xFF;\n"
                        + "int explicite = 5;\n"
                        + "private var interdit = 5;")).isEmpty(),
                "la syntaxe seule ne devine jamais le type");
    }

    @Test
    void lesBlancsDeQueueNeComptentPas() {
        assertEquals(new IndiceInlay(1, ": int"),
                unique(Document.of("var solde = 500;   ")));
        assertEquals(new IndiceInlay(1, ": int"),
                unique(Document.of("    var solde = 500;")));
    }

    @Test
    void plusieursVarsPlusieursIndices() {
        List<IndiceInlay> indices = MoteurInlayHints.indices(Document.of(
                "var a = 1;\nvar b = 2.0;\nvar c = b;"));
        assertEquals(2, indices.size());
        assertEquals(new IndiceInlay(1, ": int"), indices.get(0));
        assertEquals(new IndiceInlay(2, ": double"), indices.get(1));
    }
}
