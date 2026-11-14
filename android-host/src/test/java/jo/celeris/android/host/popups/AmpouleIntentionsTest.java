package jo.celeris.android.host.popups;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.android.host.spi.CheminDoc;
import jo.celeris.android.host.spi.DiagnosticHote;
import jo.celeris.android.host.spi.GraviteDiagnostic;

/**
 * L'ampoule d'intentions de la gouttière (l'audit Android Studio du
 * 2026-11-14) : la décision pure — l'ampoule ne vit qu'à la ligne du
 * CARET qui porte un diagnostic (le comportement de l'IDE de
 * référence : au caret en défaut, l'ampoule remplace le marqueur) ;
 * sans diagnostic sur la ligne, sans caret lu, jamais elle ne se pose.
 */
final class AmpouleIntentionsTest {

    private static DiagnosticHote diagnostic(int ligne) {
        return new DiagnosticHote(CheminDoc.de("/Compte.java"), ligne, 1,
                GraviteDiagnostic.AVERTISSEMENT, "La variable n'est jamais lue");
    }

    @Test
    void lampouleVitAuCaretQuiPorteUnDiagnostic() {
        List<DiagnosticHote> diagnostics = List.of(diagnostic(13),
                diagnostic(19));
        assertEquals(13,
                AmpouleIntentions.ligneAmpoule(13, diagnostics),
                "le caret sur la ligne du diagnostic porte l'ampoule");
        assertEquals(-1, AmpouleIntentions.ligneAmpoule(14, diagnostics),
                "la ligne sans diagnostic reste nue");
        assertEquals(-1, AmpouleIntentions.ligneAmpoule(0, diagnostics),
                "sans caret lu, aucune ampoule");
        assertEquals(-1,
                AmpouleIntentions.ligneAmpoule(13, List.of()),
                "sans diagnostics, aucune ampoule — jamais devinée");
    }
}
