package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.TimeUnit;

import jo.celeris.core.lsp.PositionsLsp;
import jo.celeris.core.lsp.types.LieuLsp;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Les usages du symbole servis par le fil du protocole (le signal du
 * propriétaire du 2026-11-13 : « lancé 90 % du chantier popups » — la
 * première surface réveillée) : le serveur de référence rend les lieux
 * de chaque occurrence du mot entier, dans l'ordre du document — la
 * carte des usages ({@code ReferencesView}) les pose en occurrences
 * cliquables, le satellite de l'orchestrateur transpose le début de
 * plage en offset de saut (la colonne compte les unités UTF-16 du
 * protocole, la ligne part de zéro), le curseur porte au point
 * d'usage — la transpose vit au banc de l'hôte
 * ({@code UsagesTransposeTest}). Pur, sans
 * appareil — le transport mémoire livre sans fil ; les régimes de la
 * carte vivent au banc du kit ({@code PopupControllerTest},
 * {@code ReferencesViewTest}).
 */
class UsagesReferencesTest {

    private static final String URI = "file:///exemple.java";

    private static final String DOCUMENT = """
            int total = 1;
            int copie = total;
            """;

    private ServeurLangage serveur;

    @BeforeEach
    void demarrer() {
        serveur = ServeurLangage.demarrer();
        serveur.ouvrir(URI, "java", DOCUMENT);
    }

    @AfterEach
    void arreter() {
        serveur.close();
    }

    /** Les lieux du symbole : chaque occurrence du MOT ENTIER, dans
     *  l'ordre du document — la déclaration d'abord, la copie ensuite ;
     *  la sous-chaîne ne compte jamais. */
    @Test
    void lesLieuxDuSymboleSeServentParLeFil() throws Exception {
        TextSession session = TextSession.of(DOCUMENT, () -> 0L);
        int caret = DOCUMENT.indexOf("total") + 1;
        List<LieuLsp> lieux = serveur.references(URI, session.document(),
                caret).get(5, TimeUnit.SECONDS);
        assertEquals(lieux.size(), 2,
                "deux usages : la déclaration et la copie");
        assertEquals(URI, lieux.get(0).uri());
        assertEquals(lieux.get(0).plage().debut().ligne(), 0,
                "le premier usage vit à la déclaration");
        assertEquals(lieux.get(1).plage().debut().ligne(), 1,
                "le second usage vit à la copie");
    }

    /** La transpose de l'atelier : le début de plage devient l'offset
     *  du saut — le curseur porte au point d'usage (la colonne du
     *  protocole se lit dans SA ligne). */
    @Test
    void laTransposeDuLieuPorteLeCurseurAuPointDUsage() throws Exception {
        TextSession session = TextSession.of(DOCUMENT, () -> 0L);
        List<LieuLsp> lieux = serveur.references(URI, session.document(),
                DOCUMENT.indexOf("total") + 1).get(5, TimeUnit.SECONDS);
        int cible = PositionsLsp.offset(session.document(),
                lieux.get(1).plage().debut());
        assertEquals(DOCUMENT.lastIndexOf("total"), cible,
                "le second usage vit à la copie");
        session.setSelection(MultiSelection.of(SelRange.caretAt(cible)));
        assertEquals(session.selection().ranges().get(0).caret(), cible,
                "le saut porte le curseur au point d'usage");
    }
}
