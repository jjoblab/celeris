package jo.celeris.android.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.OutlineEntry;

import org.junit.jupiter.api.Test;

/**
 * L'aperçu rapide d'une définition (D-11) : le plan du fichier sert
 * d'index, la déclaration du symbole visé se lit dans l'extrait —
 * sans quitter le fichier, et l'extrait reste lisible.
 */
class ApercuDefinitionTest {

    private static List<OutlineEntry> plan() {
        return List.of(
                new OutlineEntry(NodeCategory.TYPE, "Conversions", 10, 400, 0, 9),
                new OutlineEntry(NodeCategory.FUNCTION, "dpVersPx", 40, 130, 2, 4),
                new OutlineEntry(NodeCategory.FUNCTION, "pxVersDp", 200, 320, 6, 8));
    }

    private static List<String> lignes() {
        return List.of(
                "object Conversions {",
                "",
                "    fun dpVersPx(dp: Float): Int {",
                "        return (dp * 2.75f).toInt()",
                "    }",
                "",
                "    fun pxVersDp(px: Int): Float {",
                "        return px / 2.75f",
                "    }",
                "}");
    }

    /** Le symbole visé rend sa déclaration : nom, catégorie, extrait. */
    @Test
    void leSymboleViseRendSaDeclaration() {
        ApercuDefinition apercu = ApercuDefinition.de(plan(), lignes(),
                "pxVersDp", 0);
        assertTrue(apercu != null);
        assertEquals("pxVersDp", apercu.nom());
        assertEquals("FUNCTION", apercu.categorie());
        assertEquals(6, apercu.ligne());
        assertEquals("    fun pxVersDp(px: Int): Float {",
                apercu.extrait().get(0));
    }

    /** Sans symbole, l'entrée qui contient le curseur se survole. */
    @Test
    void leCurseurDansUneDeclarationSeSurvole() {
        ApercuDefinition apercu = ApercuDefinition.de(plan(), lignes(), "",
                220);
        assertTrue(apercu != null);
        assertEquals("pxVersDp", apercu.nom());
    }

    /** Le symbole inconnu ne rend rien : jamais d'aperçu inventé. */
    @Test
    void leSymboleInconnuNeRendRien() {
        assertNull(ApercuDefinition.de(plan(), lignes(), "inconnu", 0));
    }

    /** L'extrait reste borné : assez pour lire, trop peu pour noyer. */
    @Test
    void lextraitResteBorne() {
        OutlineEntry longue = new OutlineEntry(NodeCategory.TYPE, "Longue",
                0, 1000, 0, 40);
        List<String> nombreuses = lignes();
        ApercuDefinition apercu = ApercuDefinition.de(List.of(longue),
                nombreuses, "Longue", 0);
        assertTrue(apercu != null);
        assertTrue(apercu.extrait().size() <= 12,
                "l'extrait ne dépasse jamais douze lignes");
    }
}
