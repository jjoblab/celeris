package jo.celeris.android.host.popups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jo.celeris.android.view.IndiceInlay;
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
        List<IndiceInlay> indices = MoteurInlayHints.indices(Document.of(
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
        assertEquals(": int", indices.get(0).rendu());
        assertEquals(": double", indices.get(1).rendu());
        assertEquals(": float", indices.get(2).rendu());
        assertEquals(": long", indices.get(3).rendu());
        assertEquals(": boolean", indices.get(4).rendu());
        assertEquals(": String", indices.get(5).rendu());
        assertEquals(": char", indices.get(6).rendu());
        assertEquals(": int[]", indices.get(7).rendu());
        // l'appel et le null ne se devinent JAMAIS — huit indices seuls
        assertEquals(8, indices.size());
    }

    /** Le type se lit à la fin de la ligne UTILE — l'offset porte la
     *  colonne, l'indice s'ancre au texte. */
    @Test
    void leTypeSeLitEnFinDeLigneUtile() {
        List<IndiceInlay> indices = MoteurInlayHints.indices(Document.of(
                "var solde = 100;"));
        assertEquals(1, indices.size());
        assertEquals(1, indices.get(0).ligne());
        assertEquals("var solde = 100;".length(), indices.get(0).offset());
    }

    /** Le nom du paramètre muet se lit devant SON argument littéral —
     *  la méthode du fichier sert ses paramètres. */
    @Test
    void leNomDuParametreSeLitDevantSonArgument() {
        List<IndiceInlay> indices = MoteurInlayHints.indices(Document.of(
                "private void verser(int montant, boolean vif) {\n"
                + "}\n"
                + "void appeler() {\n"
                + "    verser(100, true);\n"
                + "}\n"));
        IndiceInlay montant = chercher(indices, "montant:");
        IndiceInlay vif = chercher(indices, "vif:");
        assertNotNull(montant, "le premier paramètre se nomme");
        assertNotNull(vif, "le second paramètre se nomme");
        assertEquals(4, montant.ligne());
        assertTrue(montant.offset() > 0);
        assertTrue(montant.offset() < vif.offset());
    }

    /** L'argument NOMMÉ d'avance et l'expression ne portent rien. */
    @Test
    void largumentNommeNePorteRien() {
        List<IndiceInlay> indices = MoteurInlayHints.indices(Document.of(
                "private void verser(int montant) {\n"
                + "}\n"
                + "void appeler() {\n"
                + "    verser(inconnu);\n"
                + "}\n"));
        assertNull(chercher(indices, "montant:"));
    }

    /** La méthode INCONNUE du fichier ne sert rien — jamais de nom
     *  décalé. */
    @Test
    void laMethodeInconnueNeSertRien() {
        List<IndiceInlay> indices = MoteurInlayHints.indices(Document.of(
                "void appeler() {\n"
                + "    inconnue(100);\n"
                + "}\n"));
        assertTrue(indices.isEmpty());
    }

    /** Le premier indice du rendu donné, null s'il n'existe pas. */
    private static IndiceInlay chercher(List<IndiceInlay> indices,
            String rendu) {
        for (IndiceInlay indice : indices) {
            if (indice.rendu().equals(rendu)) {
                return indice;
            }
        }
        return null;
    }
}
