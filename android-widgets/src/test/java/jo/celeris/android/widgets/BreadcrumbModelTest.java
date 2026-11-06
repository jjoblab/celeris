package jo.celeris.android.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.JavaSyntax;
import jo.celeris.core.syntax.OutlineEntry;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.text.Document;

/**
 * Le fil d'Ariane (D-09, M5) : le chemin du symbole courant sort du
 * même arbre que la coloration — les entrées du plan qui englobent le
 * curseur, de la plus externe à la plus interne. La troncature par la
 * largeur garde les niveaux profonds et élague la tête.
 */
class BreadcrumbModelTest {

    /** Le chemin du symbole courant : les entrées englobantes, dehors vers dedans. */
    @Test
    void cheminDuSymboleCourant() {
        String texte = "class A {\n"
                + "    int champ;\n"
                + "    void methode() {\n"
                + "        int interne;\n"
                + "    }\n"
                + "}\n";
        Document doc = Document.of(texte);
        Analysis analyse = SyntaxService.of(JavaSyntax.INSTANCE).analyseFull(doc);
        // le curseur sur « interne » (offset 49)
        List<OutlineEntry> chemin = BreadcrumbModel.chemin(doc, analyse,
                texte.indexOf("interne"));
        assertEquals(2, chemin.size());
        assertEquals("A", chemin.get(0).name());
        assertEquals("methode", chemin.get(1).name());
    }

    /** Hors de tout symbole, le chemin est vide. */
    @Test
    void horsSymboleCheminVide() {
        String texte = "package demo;\n\nclass A {\n}\n";
        Document doc = Document.of(texte);
        Analysis analyse = SyntaxService.of(JavaSyntax.INSTANCE).analyseFull(doc);
        // le curseur sur « package »
        List<OutlineEntry> chemin = BreadcrumbModel.chemin(doc, analyse, 2);
        assertEquals(0, chemin.size());
    }

    /** La troncature garde les niveaux profonds, élague la tête (D-09). */
    @Test
    void troncatureParLaLargeur() {
        // un mesureur simple : la largeur d'un niveau est sa longueur
        ToIntFunction<String> mesureur = String::length;
        List<String> tronque = BreadcrumbModel.tronque(
                List.of("tresLongChemin", "milieu", "fin"), 12, mesureur);
        // « … › fin » (7) tient, « … › milieu › fin » (16) dépasse :
        // la tête élaguée est signalée par l'ellipse, qui compte dans la largeur
        assertEquals(List.of("…", "fin"), tronque);
    }

    /** Sans place pour la tête élaguée, les niveaux les plus profonds restent. */
    @Test
    void lesPlusProfondsRestent() {
        ToIntFunction<String> mesureur = String::length;
        List<String> tronque = BreadcrumbModel.tronque(
                List.of("aaaa", "bbbb", "cccc"), 10, mesureur);
        // « … › cccc » (7) tient, « … › bbbb › cccc » (15) dépasse
        assertEquals(List.of("…", "cccc"), tronque);
    }

    /** Quand tout tient, rien ne tombe. */
    @Test
    void quandToutTientRienNeTombe() {
        ToIntFunction<String> mesureur = String::length;
        List<String> tronque = BreadcrumbModel.tronque(
                List.of("a", "b", "c"), 100, mesureur);
        assertEquals(List.of("a", "b", "c"), tronque);
    }
}
