package jo.celeris.core.syntax.structure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.text.Document;

/**
 * D-04 (l'analyse croisée) : les plis REGIONS NOMMÉES et IMPORTS —
 * le dépôt de départ repliait les imports Java et les régions
 * marquées ({@code // region} / {@code // endregion},
 * {@code // <editor-fold>} / {@code // </editor-fold>}) ; le portage
 * ne reconnaissait que les nœuds structurels (bloc, type, fonction,
 * balise) et les commentaires.
 */
public class RegionsNommeesTest {

    /** La langue du banc : java (le lexeur y marque les commentaires). */
    private static final JavaSyntax SYNTAXE = JavaSyntax.INSTANCE;

    /** Le document analysé : plis dérivés de l'arbre + du lexique. */
    private static Document document(String texte) {
        return Document.of(texte);
    }

    private static boolean pliable(String texte, int ligne) {
        Document doc = document(texte);
        Analysis analyse = jo.celeris.core.syntax.SyntaxService.of(SYNTAXE)
                .analyseFull(doc);
        return TreeFolds.regions(doc, analyse).stream()
                .anyMatch(r -> r.startLine() == ligne);
    }

    /** La région nommée {@code // region} se plie : l'en-tête porte
     *  le pli, la dernière ligne est celle du {@code // endregion}. */
    @Test
    public void laRegionNommeeSePlie() {
        String texte = """
                class A {
                }
                // region Constantes
                int un = 1;
                int deux = 2;
                // endregion
                """;
        assertTrue(pliable(texte, 2),
                "la ligne // region porte un pli");
    }

    /** Le repli d'editor-fold (NetBeans/Studio) se plie aussi : la
     *  marque ouvrante porte le pli, la fermante ferme la plage. */
    @Test
    public void leditorFoldSePlie() {
        String texte = """
                class A {
                }
                // <editor-fold desc="Champs">
                int un = 1;
                int deux = 2;
                // </editor-fold>
                """;
        assertTrue(pliable(texte, 2),
                "la ligne editor-fold porte un pli");
    }

    /** Le bloc d'imports Java se plie : la première ligne d'import
     *  porte le pli, la dernière le referme (le dépôt de départ
     *  repliait « Imports » d'un geste). */
    @Test
    public void leBlocDimportsSePlie() {
        String texte = """
                package jo;

                import java.util.List;
                import java.util.Map;
                import java.util.Set;

                class A {
                }
                """;
        assertTrue(pliable(texte, 2),
                "la première ligne d'import porte un pli");
    }

    /** Sans région ni imports, la ligne orpheline ne porte rien. */
    @Test
    public void lesLignesOrphelinesNePortentRien() {
        String texte = """
                class A {
                    int x = 1;
                }
                """;
        assertFalse(pliable(texte, 1),
                "la ligne du champ ne porte pas de pli région");
    }

    /** Une région FERMÉE d'abord (endif sans region) ne plie rien. */
    @Test
    public void laRegionFermeeSansOuvertureNePlieRien() {
        String texte = """
                class A {
                }
                // endregion orphelin
                int un = 1;
                """;
        assertFalse(pliable(texte, 2),
                "l'endregion orphelin ne porte pas de pli");
    }
}
