package jo.celeris.lsp.protocole;

import jo.celeris.core.lsp.types.CodeActionLsp;
import jo.celeris.core.lsp.types.DocumentEditLsp;
import jo.celeris.core.lsp.types.WorkspaceEditLsp;
import jo.celeris.core.lsp.types.TextEditLsp;
import jo.celeris.core.lsp.types.InlayHintLsp;
import jo.celeris.core.lsp.types.LocationLsp;
import jo.celeris.core.lsp.types.FormatOptionsLsp;
import jo.celeris.core.lsp.types.RangeLsp;
import jo.celeris.core.lsp.types.FoldLsp;
import jo.celeris.core.lsp.types.LspPosition;
import jo.celeris.core.lsp.types.RenamePrepareLsp;
import jo.celeris.core.lsp.types.SemanticTokensLsp;
import jo.celeris.core.lsp.types.HighlightLsp;
import jo.celeris.core.lsp.types.SymbolLsp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
















/**
 * La couverture du jalon (H-12) exercée au MONTEUR : le serveur de
 * documents du domaine, ses réponses riches comme ses replis vides,
 * traversent l'adaptateur du module ET la correspondance inverse — le
 * domaine part du terrain, devient protocole sur le fil, redevient
 * domaine à la session. Les treize méthodes y passent une à une : la
 * preuve du aller-retour, pas seulement de l'aller.
 */
class ServeurDocumentsCouvertureTest {

    private static final String URI = "file:///banc/couverture.txt";

    private EmbeddedServer montage;

    private LspSession session;

    @BeforeEach
    void demarrer() {
        montage = EmbeddedServer.start(new TerrainServant(), "celeris-banc");
        session = montage.session();
        session.initialize();
        session.open(URI, "texte", "terrain de couverture");
    }

    @AfterEach
    void arreter() {
        montage.close();
    }

    private static RangeLsp range(int ld, int cd, int lf, int cf) {
        return new RangeLsp(new LspPosition(ld, cd), new LspPosition(lf, cf));
    }

    /** Le plan : la hiérarchie du terrain revient au domaine, enfants compris. */
    @Test
    void lesSymbolesFontLallerRetour() throws Exception {
        List<SymbolLsp> symbols = session.symbols(URI)
                .get(5, TimeUnit.SECONDS);
        assertEquals(1, symbols.size());
        assertEquals("racine", symbols.get(0).nom());
        assertEquals(5, symbols.get(0).kind());
        assertEquals(1, symbols.get(0).enfants().size());
        assertEquals("enfant", symbols.get(0).enfants().get(0).nom());
        assertEquals(range(1, 0, 1, 7),
                symbols.get(0).enfants().get(0).plageSelection());
    }

    /** Le pli : la fin EXCLUSIVE du domaine survit au protocole inclusif. */
    @Test
    void lesPlisFontLallerRetour() throws Exception {
        List<FoldLsp> folds = session.folds(URI).get(5, TimeUnit.SECONDS);
        assertEquals(List.of(new FoldLsp(0, 3)), folds);
    }

    /** Le formatage : les éditions du document et celles de la plage. */
    @Test
    void leFormatageFontLallerRetour() throws Exception {
        List<TextEditLsp> edits = session.formatage(URI,
                FormatOptionsLsp.DEFAUT).get(5, TimeUnit.SECONDS);
        assertEquals(List.of(new TextEditLsp(range(0, 0, 0, 2),
                "aligne")), edits);
        List<TextEditLsp> locales = session.rangeFormatting(URI,
                range(0, 0, 1, 0), FormatOptionsLsp.DEFAUT)
                .get(5, TimeUnit.SECONDS);
        assertEquals(List.of(new TextEditLsp(range(0, 0, 1, 0), "local")),
                locales);
    }

    /** Les occurrences : la plage surlignée et son genre de lecture. */
    @Test
    void lesSurlignagesFontLallerRetour() throws Exception {
        List<HighlightLsp> highlights = session.highlights(URI,
                new LspPosition(0, 5)).get(5, TimeUnit.SECONDS);
        assertEquals(List.of(new HighlightLsp(range(0, 4, 0, 9), 2)),
                highlights);
    }

    /** Le renommage : la préparation puis l'édition d'espace, la version suit. */
    @Test
    void leRennomageFaitLallerRetour() throws Exception {
        RenamePrepareLsp preparation = session.preparerRennomage(URI,
                new LspPosition(0, 5)).get(5, TimeUnit.SECONDS);
        assertEquals(range(0, 4, 0, 9), preparation.range());
        assertEquals("ancien", preparation.leadingText());
        WorkspaceEditLsp espace = session.rename(URI,
                new LspPosition(0, 5), "renouvele").get(5, TimeUnit.SECONDS);
        assertEquals(1, espace.documents().size());
        DocumentEditLsp document = espace.documents().get(0);
        assertEquals(URI, document.uri());
        assertEquals(7, document.version());
        assertEquals(List.of(new TextEditLsp(range(0, 4, 0, 9),
                "renouvele")), document.edits());
    }

    /** Le menu d'intentions : le titre du correctif et son édition portent. */
    @Test
    void lesCorrectifsFontLallerRetour() throws Exception {
        List<CodeActionLsp> correctifs = session.correctifs(URI,
                range(0, 4, 0, 9)).get(5, TimeUnit.SECONDS);
        assertEquals(1, correctifs.size());
        assertEquals("Renommer en majuscules", correctifs.get(0).titre());
        assertEquals(1, correctifs.get(0).edit().documents().size());
    }

    /**
     * Les indices en ligne : les parties typées, leur cible de
     * navigation, le genre et les remplissages — la géométrie de saisie
     * ne bouge pas, la marge se peint.
     */
    @Test
    void lesIndicesFontLallerRetour() throws Exception {
        List<InlayHintLsp> inlayHints = session.inlayHints(URI, range(0, 0, 1, 0))
                .get(5, TimeUnit.SECONDS);
        assertEquals(1, inlayHints.size());
        InlayHintLsp indice = inlayHints.get(0);
        assertEquals(new LspPosition(4, 2), indice.position());
        assertEquals(2, indice.parties().size());
        assertEquals("Double", indice.parties().get(0).value());
        assertEquals(new LocationLsp(URI, range(9, 0, 9, 3)),
                indice.parties().get(0).target());
        assertEquals(1, indice.kind());
        assertTrue(indice.remplissageGauche());
        assertTrue(indice.remplissageDroite());
    }

    /**
     * Les jetons sémantiques : la charge encodée du protocole redevient
     * les jetons absolus du domaine — le document entier comme la plage.
     */
    @Test
    void lesTokensFontLallerRetour() throws Exception {
        SemanticTokensLsp tokens = session.tokens(URI)
                .get(5, TimeUnit.SECONDS);
        assertEquals("resultat-1", tokens.identifiantResultat());
        assertEquals(1, tokens.tokens().size());
        assertEquals(4, tokens.tokens().get(0).column());
        assertEquals(5, tokens.tokens().get(0).length());
        SemanticTokensLsp locaux = session.tokensRange(URI,
                range(0, 0, 1, 0)).get(5, TimeUnit.SECONDS);
        assertNull(locaux.identifiantResultat());
        assertEquals(1, locaux.tokens().size());
        assertEquals(3, locaux.tokens().get(0).length());
    }

    /** Les références : les lieux du terrain, l'uri et la plage. */
    @Test
    void lesReferencesFontLallerRetour() throws Exception {
        List<LocationLsp> lieux = session.references(URI,
                new LspPosition(0, 5)).get(5, TimeUnit.SECONDS);
        assertEquals(List.of(new LocationLsp(URI, range(3, 1, 3, 6))), lieux);
    }

    /** Les plages de sélection : la chaîne des englobantes, intime puis large. */
    @Test
    void lesPlagesSelectionFontLallerRetour() throws Exception {
        List<RangeLsp> ranges = session.selectionRanges(URI,
                new LspPosition(0, 5)).get(5, TimeUnit.SECONDS);
        assertEquals(List.of(range(0, 4, 0, 9), range(0, 0, 1, 20)), ranges);
    }

    /** Le terrain muet : les replis du SPI deviennent les vides du domaine. */
    @Test
    void leTerrainMuetRendLesVides() throws Exception {
        try (EmbeddedServer muet = EmbeddedServer.start(
                new EmbeddedServer.Docs() {
                }, "celeris-banc")) {
            LspSession sessionMute = muet.session();
            sessionMute.initialize();
            sessionMute.open(URI, "texte", "rien");
            assertEquals(List.of(), sessionMute.symbols(URI)
                    .get(5, TimeUnit.SECONDS));
            assertEquals(List.of(), sessionMute.folds(URI)
                    .get(5, TimeUnit.SECONDS));
            assertEquals(List.of(), sessionMute.formatage(URI,
                    FormatOptionsLsp.DEFAUT).get(5, TimeUnit.SECONDS));
            assertEquals(List.of(), sessionMute.rangeFormatting(URI,
                    range(0, 0, 1, 0), FormatOptionsLsp.DEFAUT)
                    .get(5, TimeUnit.SECONDS));
            assertEquals(List.of(), sessionMute.highlights(URI,
                    new LspPosition(0, 1)).get(5, TimeUnit.SECONDS));
            assertNull(sessionMute.preparerRennomage(URI,
                    new LspPosition(0, 1)).get(5, TimeUnit.SECONDS));
            assertEquals(WorkspaceEditLsp.EMPTY, sessionMute.rename(URI,
                    new LspPosition(0, 1), "autre").get(5, TimeUnit.SECONDS));
            assertEquals(List.of(), sessionMute.correctifs(URI,
                    range(0, 0, 0, 1)).get(5, TimeUnit.SECONDS));
            assertEquals(List.of(), sessionMute.inlayHints(URI,
                    range(0, 0, 1, 0)).get(5, TimeUnit.SECONDS));
            assertEquals(List.of(), sessionMute.tokens(URI)
                    .get(5, TimeUnit.SECONDS).tokens());
            assertEquals(List.of(), sessionMute.tokensRange(URI,
                    range(0, 0, 1, 0)).get(5, TimeUnit.SECONDS).tokens());
            assertEquals(List.of(), sessionMute.references(URI,
                    new LspPosition(0, 1)).get(5, TimeUnit.SECONDS));
            assertEquals(List.of(), sessionMute.selectionRanges(URI,
                    new LspPosition(0, 1)).get(5, TimeUnit.SECONDS));
        }
    }

    /**
     * Le terrain du banc : chaque méthode du SPI y répond RICHE — le
     * aller-retour se prête sur des valeurs, pas des vides.
     */
    private static final class TerrainServant
            implements EmbeddedServer.Docs {

        @Override
        public List<SymbolLsp> symbols(String uri) {
            return List.of(new SymbolLsp("racine", 5, range(0, 0, 3, 0),
                    range(0, 0, 0, 6), List.of(new SymbolLsp("enfant", 12,
                            range(1, 0, 2, 0), range(1, 0, 1, 7),
                            List.of()))));
        }

        @Override
        public List<FoldLsp> folds(String uri) {
            return List.of(new FoldLsp(0, 3));
        }

        @Override
        public List<TextEditLsp> formatage(String uri,
                FormatOptionsLsp options) {
            assertEquals(4, options.tabWidth());
            return List.of(new TextEditLsp(range(0, 0, 0, 2), "aligne"));
        }

        @Override
        public List<TextEditLsp> rangeFormatting(String uri,
                RangeLsp range, FormatOptionsLsp options) {
            return List.of(new TextEditLsp(range, "local"));
        }

        @Override
        public List<HighlightLsp> highlights(String uri,
                LspPosition position) {
            return List.of(new HighlightLsp(range(0, 4, 0, 9), 2));
        }

        @Override
        public RenamePrepareLsp preparerRennomage(String uri,
                LspPosition position) {
            return new RenamePrepareLsp(range(0, 4, 0, 9), "ancien");
        }

        @Override
        public WorkspaceEditLsp rename(String uri, LspPosition position,
                String nouveauNom) {
            assertEquals("renouvele", nouveauNom);
            return new WorkspaceEditLsp(List.of(new DocumentEditLsp(uri, 7,
                    List.of(new TextEditLsp(range(0, 4, 0, 9),
                            nouveauNom)))), List.of());
        }

        @Override
        public List<CodeActionLsp> correctifs(String uri, RangeLsp range) {
            return List.of(new CodeActionLsp("Renommer en majuscules",
                    new WorkspaceEditLsp(List.of(new DocumentEditLsp(uri,
                            -1, List.of(new TextEditLsp(range,
                                    "MAJUSCULES")))), List.of())));
        }

        @Override
        public List<InlayHintLsp> inlayHints(String uri, RangeLsp range) {
            return List.of(new InlayHintLsp(new LspPosition(4, 2), List.of(
                    new InlayHintLsp.Part("Double",
                            new LocationLsp(uri, range(9, 0, 9, 3))),
                    new InlayHintLsp.Part("total", null)), 1, true, true));
        }

        @Override
        public SemanticTokensLsp tokens(String uri) {
            return SemanticTokensLsp.of("resultat-1", List.of(0, 4, 5, 1, 0));
        }

        @Override
        public SemanticTokensLsp tokensRange(String uri, RangeLsp range) {
            return SemanticTokensLsp.of(null, List.of(0, 2, 3, 2, 0));
        }

        @Override
        public List<LocationLsp> references(String uri, LspPosition position) {
            return List.of(new LocationLsp(uri, range(3, 1, 3, 6)));
        }

        @Override
        public List<RangeLsp> selectionRanges(String uri,
                LspPosition position) {
            return List.of(range(0, 4, 0, 9), range(0, 0, 1, 20));
        }
    }
}
