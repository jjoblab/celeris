package jo.celeris.android.widgets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.SyntaxIssue;
import jo.celeris.core.text.Document;

/**
 * La bande d'erreurs annotée (D-08, M5) : les diagnostics du même arbre
 * deviennent des marques le long du défilement — la densité est bornée
 * (au plus une marque par cran de bande, la première l'emporte), le
 * clic saute au diagnostic du cran le plus proche.
 */
class ErrorBandModelTest {

    private static Document document(int lignes) {
        StringBuilder texte = new StringBuilder();
        for (int i = 0; i < lignes; i++) {
            texte.append("aaaa\n");
        }
        return Document.of(texte.toString());
    }

    /** La densité est bornée : deux diagnostics d'un même cran ne font qu'une marque. */
    @Test
    void densiteBornee() {
        // cent lignes « aaaa » : la ligne i commence à l'offset 5i
        Document doc = document(100);
        List<SyntaxIssue> diagnostics = List.of(
                new SyntaxIssue(0, 1, "ligne 0"),
                new SyntaxIssue(6, 1, "ligne 1 — même cran"),
                new SyntaxIssue(101, 1, "ligne 20"));
        ErrorBandModel bande = ErrorBandModel.of(diagnostics, doc, 10);
        // lignes 0 et 1 partagent le cran 0 ; la ligne 20 vit au cran 1
        // (le saut final crée une cent-unième ligne, vide)
        assertEquals(2, bande.marques().size());
        assertEquals(0, bande.marques().get(0).ligne());
        assertEquals(0, bande.marques().get(0).cran());
        assertEquals(20, bande.marques().get(1).ligne());
        assertEquals(1, bande.marques().get(1).cran());
    }

    /** Le clic sur un cran saute au diagnostic qu'il porte. */
    @Test
    void clicSauteAuDiagnostic() {
        Document doc = document(100);
        List<SyntaxIssue> diagnostics = List.of(
                new SyntaxIssue(62, 1, "ligne 12"),
                new SyntaxIssue(425, 1, "ligne 85"));
        ErrorBandModel bande = ErrorBandModel.of(diagnostics, doc, 10);
        // le cran 1 porte la ligne 12 ; le cran 9 se rabat au cran 8 (ligne 85)
        assertEquals(62, bande.clic(1));
        assertEquals(425, bande.clic(9));
    }

    /** Le clic loin d'une marque choisit le cran porté le plus proche. */
    @Test
    void clicChoisitLePlusProche() {
        Document doc = document(100);
        List<SyntaxIssue> diagnostics = List.of(new SyntaxIssue(0, 1, "seul"));
        ErrorBandModel bande = ErrorBandModel.of(diagnostics, doc, 10);
        // la seule marque vit au cran 0 : tout clic s'y rabat
        assertEquals(0, bande.clic(7));
    }

    /** Une bande sans diagnostic ne rend rien. */
    @Test
    void sansDiagnosticRien() {
        ErrorBandModel bande = ErrorBandModel.of(List.of(), document(100), 10);
        assertTrue(bande.marques().isEmpty());
        assertEquals(-1, bande.clic(3));
    }

    /** Un diagnostic périmé au-delà du document est ignoré. */
    @Test
    void diagnosticPerimeIgnore() {
        Document doc = document(100);
        List<SyntaxIssue> diagnostics = List.of(
                new SyntaxIssue(10_000, 1, "au-delà"));
        ErrorBandModel bande = ErrorBandModel.of(diagnostics, doc, 10);
        assertTrue(bande.marques().isEmpty());
    }
}
