package jo.celeris.android.host.popups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jo.celeris.android.view.InlayHintMark;
import jo.celeris.core.text.Document;

import org.junit.jupiter.api.Test;

/**
 * Le moteur des indices en ligne (inlay hints — l'audit Android Studio
 * du 2026-11-14, complété le même jour : « assure-toi qu'il soit
 * complet comme Android Studio ») : le TYPE des « var » littéraux
 * (entier, décimal, suffixes, booléen, chaîne, caractère, TABLEAU) se
 * lit en fin de ligne utile, le NOM d'un paramètre muet se lit devant
 * son argument littéral aux appels des méthodes du fichier — jamais
 * deviné.
 */
class MoteurInlayHintsTest {

    /** Le « var » littéral porte SON type — chaque famille compte. */
    @Test
    void leVarLitteralPorteSonType() {
        List<InlayHintMark> inlayHints = InlayHintsEngine.inlayHints(Document.of(
                "var entier = 1;\n"
                + "var decimal = 2.5;\n"
                + "var flottant = 3f;\n"
                + "var longu = 4L;\n"
                + "var vrai = true;\n"
                + "var mot = \"bonjour\";\n"
                + "var lettre = 'a';\n"
                + "var table = {1, 2, 3};\n"
                + "var inconnu = calcul();\n"
                + "var rien = null;\n"));
        assertEquals(": int", inlayHints.get(0).rendu());
        assertEquals(": double", inlayHints.get(1).rendu());
        assertEquals(": float", inlayHints.get(2).rendu());
        assertEquals(": long", inlayHints.get(3).rendu());
        assertEquals(": boolean", inlayHints.get(4).rendu());
        assertEquals(": String", inlayHints.get(5).rendu());
        assertEquals(": char", inlayHints.get(6).rendu());
        assertEquals(": int[]", inlayHints.get(7).rendu());
        // l'appel et le null ne se devinent JAMAIS — huit indices seuls
        assertEquals(8, inlayHints.size());
    }

    /** Le type se lit à la fin de la ligne UTILE — l'offset porte la
     *  colonne, l'indice s'ancre au texte. */
    @Test
    void leTypeSeLitEnFinDeLigneUtile() {
        List<InlayHintMark> inlayHints = InlayHintsEngine.inlayHints(Document.of(
                "var solde = 100;"));
        assertEquals(1, inlayHints.size());
        assertEquals(1, inlayHints.get(0).line());
        assertEquals("var solde = 100;".length(), inlayHints.get(0).offset());
    }

    /** Le nom du paramètre muet se lit devant SON argument littéral —
     *  la méthode du fichier sert ses paramètres. */
    @Test
    void leNomDuParametreSeLitDevantSonArgument() {
        List<InlayHintMark> inlayHints = InlayHintsEngine.inlayHints(Document.of(
                "private void verser(int montant, boolean vif) {\n"
                + "}\n"
                + "void appeler() {\n"
                + "    verser(100, true);\n"
                + "}\n"));
        InlayHintMark montant = search(inlayHints, "montant:");
        InlayHintMark vif = search(inlayHints, "vif:");
        assertNotNull(montant, "le premier paramètre se nomme");
        assertNotNull(vif, "le second paramètre se nomme");
        assertEquals(4, montant.line());
        assertTrue(montant.offset() > 0);
        assertTrue(montant.offset() < vif.offset());
    }

    /** L'argument NOMMÉ d'avance et l'expression ne portent rien. */
    @Test
    void largumentNommeNePorteRien() {
        List<InlayHintMark> inlayHints = InlayHintsEngine.inlayHints(Document.of(
                "private void verser(int montant) {\n"
                + "}\n"
                + "void appeler() {\n"
                + "    verser(inconnu);\n"
                + "}\n"));
        assertNull(search(inlayHints, "montant:"));
    }

    /** La méthode INCONNUE du fichier ne sert rien — jamais de nom
     *  décalé. */
    @Test
    void laMethodeInconnueNeSertRien() {
        List<InlayHintMark> inlayHints = InlayHintsEngine.inlayHints(Document.of(
                "void appeler() {\n"
                + "    inconnue(100);\n"
                + "}\n"));
        assertTrue(inlayHints.isEmpty());
    }

    /** Le premier indice du rendu donné, null s'il n'existe pas. */
    private static InlayHintMark search(List<InlayHintMark> inlayHints,
            String rendu) {
        for (InlayHintMark indice : inlayHints) {
            if (indice.rendu().equals(rendu)) {
                return indice;
            }
        }
        return null;
    }
}
