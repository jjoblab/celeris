package jo.celeris.lsp.protocole;

import jo.celeris.core.lsp.types.CorrectifLsp;
import jo.celeris.core.lsp.types.EditionDocumentLsp;
import jo.celeris.core.lsp.types.EditionEspaceLsp;
import jo.celeris.core.lsp.types.EditionTexteLsp;
import jo.celeris.core.lsp.types.IndiceLsp;
import jo.celeris.core.lsp.types.LieuLsp;
import jo.celeris.core.lsp.types.OptionsFormatageLsp;
import jo.celeris.core.lsp.types.PlageLsp;
import jo.celeris.core.lsp.types.PliLsp;
import jo.celeris.core.lsp.types.PositionLsp;
import jo.celeris.core.lsp.types.PreparationRennomageLsp;
import jo.celeris.core.lsp.types.SemanticTokensLsp;
import jo.celeris.core.lsp.types.SurlignageLsp;
import jo.celeris.core.lsp.types.SymboleLsp;
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

    private ServeurEmbarque montage;

    private LspSession session;

    @BeforeEach
    void demarrer() {
        montage = ServeurEmbarque.monter(new TerrainServant(), "celeris-banc");
        session = montage.session();
        session.initialiser();
        session.ouvrir(URI, "texte", "terrain de couverture");
    }

    @AfterEach
    void arreter() {
        montage.close();
    }

    private static PlageLsp plage(int ld, int cd, int lf, int cf) {
        return new PlageLsp(new PositionLsp(ld, cd), new PositionLsp(lf, cf));
    }

    /** Le plan : la hiérarchie du terrain revient au domaine, enfants compris. */
    @Test
    void lesSymbolesFontLallerRetour() throws Exception {
        List<SymboleLsp> symboles = session.symboles(URI)
                .get(5, TimeUnit.SECONDS);
        assertEquals(1, symboles.size());
        assertEquals("racine", symboles.get(0).nom());
        assertEquals(5, symboles.get(0).genre());
        assertEquals(1, symboles.get(0).enfants().size());
        assertEquals("enfant", symboles.get(0).enfants().get(0).nom());
        assertEquals(plage(1, 0, 1, 7),
                symboles.get(0).enfants().get(0).plageSelection());
    }

    /** Le pli : la fin EXCLUSIVE du domaine survit au protocole inclusif. */
    @Test
    void lesPlisFontLallerRetour() throws Exception {
        List<PliLsp> plis = session.plis(URI).get(5, TimeUnit.SECONDS);
        assertEquals(List.of(new PliLsp(0, 3)), plis);
    }

    /** Le formatage : les éditions du document et celles de la plage. */
    @Test
    void leFormatageFontLallerRetour() throws Exception {
        List<EditionTexteLsp> editions = session.formatage(URI,
                OptionsFormatageLsp.DEFAUT).get(5, TimeUnit.SECONDS);
        assertEquals(List.of(new EditionTexteLsp(plage(0, 0, 0, 2),
                "aligne")), editions);
        List<EditionTexteLsp> locales = session.formatagePlage(URI,
                plage(0, 0, 1, 0), OptionsFormatageLsp.DEFAUT)
                .get(5, TimeUnit.SECONDS);
        assertEquals(List.of(new EditionTexteLsp(plage(0, 0, 1, 0), "local")),
                locales);
    }

    /** Les occurrences : la plage surlignée et son genre de lecture. */
    @Test
    void lesSurlignagesFontLallerRetour() throws Exception {
        List<SurlignageLsp> surlignages = session.surlignages(URI,
                new PositionLsp(0, 5)).get(5, TimeUnit.SECONDS);
        assertEquals(List.of(new SurlignageLsp(plage(0, 4, 0, 9), 2)),
                surlignages);
    }

    /** Le renommage : la préparation puis l'édition d'espace, la version suit. */
    @Test
    void leRennomageFaitLallerRetour() throws Exception {
        PreparationRennomageLsp preparation = session.preparerRennomage(URI,
                new PositionLsp(0, 5)).get(5, TimeUnit.SECONDS);
        assertEquals(plage(0, 4, 0, 9), preparation.plage());
        assertEquals("ancien", preparation.texteAmorce());
        EditionEspaceLsp espace = session.rennomage(URI,
                new PositionLsp(0, 5), "renouvele").get(5, TimeUnit.SECONDS);
        assertEquals(1, espace.documents().size());
        EditionDocumentLsp document = espace.documents().get(0);
        assertEquals(URI, document.uri());
        assertEquals(7, document.version());
        assertEquals(List.of(new EditionTexteLsp(plage(0, 4, 0, 9),
                "renouvele")), document.editions());
    }

    /** Le menu d'intentions : le titre du correctif et son édition portent. */
    @Test
    void lesCorrectifsFontLallerRetour() throws Exception {
        List<CorrectifLsp> correctifs = session.correctifs(URI,
                plage(0, 4, 0, 9)).get(5, TimeUnit.SECONDS);
        assertEquals(1, correctifs.size());
        assertEquals("Renommer en majuscules", correctifs.get(0).titre());
        assertEquals(1, correctifs.get(0).edition().documents().size());
    }

    /**
     * Les indices en ligne : les parties typées, leur cible de
     * navigation, le genre et les remplissages — la géométrie de saisie
     * ne bouge pas, la marge se peint.
     */
    @Test
    void lesIndicesFontLallerRetour() throws Exception {
        List<IndiceLsp> indices = session.indices(URI, plage(0, 0, 1, 0))
                .get(5, TimeUnit.SECONDS);
        assertEquals(1, indices.size());
        IndiceLsp indice = indices.get(0);
        assertEquals(new PositionLsp(4, 2), indice.position());
        assertEquals(2, indice.parties().size());
        assertEquals("Double", indice.parties().get(0).valeur());
        assertEquals(new LieuLsp(URI, plage(9, 0, 9, 3)),
                indice.parties().get(0).cible());
        assertEquals(1, indice.genre());
        assertTrue(indice.remplissageGauche());
        assertTrue(indice.remplissageDroite());
    }

    /**
     * Les jetons sémantiques : la charge encodée du protocole redevient
     * les jetons absolus du domaine — le document entier comme la plage.
     */
    @Test
    void lesJetonsFontLallerRetour() throws Exception {
        SemanticTokensLsp jetons = session.jetons(URI)
                .get(5, TimeUnit.SECONDS);
        assertEquals("resultat-1", jetons.identifiantResultat());
        assertEquals(1, jetons.jetons().size());
        assertEquals(4, jetons.jetons().get(0).colonne());
        assertEquals(5, jetons.jetons().get(0).longueur());
        SemanticTokensLsp locaux = session.jetonsPlage(URI,
                plage(0, 0, 1, 0)).get(5, TimeUnit.SECONDS);
        assertNull(locaux.identifiantResultat());
        assertEquals(1, locaux.jetons().size());
        assertEquals(3, locaux.jetons().get(0).longueur());
    }

    /** Les références : les lieux du terrain, l'uri et la plage. */
    @Test
    void lesReferencesFontLallerRetour() throws Exception {
        List<LieuLsp> lieux = session.references(URI,
                new PositionLsp(0, 5)).get(5, TimeUnit.SECONDS);
        assertEquals(List.of(new LieuLsp(URI, plage(3, 1, 3, 6))), lieux);
    }

    /** Les plages de sélection : la chaîne des englobantes, intime puis large. */
    @Test
    void lesPlagesSelectionFontLallerRetour() throws Exception {
        List<PlageLsp> plages = session.plagesSelection(URI,
                new PositionLsp(0, 5)).get(5, TimeUnit.SECONDS);
        assertEquals(List.of(plage(0, 4, 0, 9), plage(0, 0, 1, 20)), plages);
    }

    /** Le terrain muet : les replis du SPI deviennent les vides du domaine. */
    @Test
    void leTerrainMuetRendLesVides() throws Exception {
        try (ServeurEmbarque muet = ServeurEmbarque.monter(
                new ServeurEmbarque.Documents() {
                }, "celeris-banc")) {
            LspSession sessionMute = muet.session();
            sessionMute.initialiser();
            sessionMute.ouvrir(URI, "texte", "rien");
            assertEquals(List.of(), sessionMute.symboles(URI)
                    .get(5, TimeUnit.SECONDS));
            assertEquals(List.of(), sessionMute.plis(URI)
                    .get(5, TimeUnit.SECONDS));
            assertEquals(List.of(), sessionMute.formatage(URI,
                    OptionsFormatageLsp.DEFAUT).get(5, TimeUnit.SECONDS));
            assertEquals(List.of(), sessionMute.formatagePlage(URI,
                    plage(0, 0, 1, 0), OptionsFormatageLsp.DEFAUT)
                    .get(5, TimeUnit.SECONDS));
            assertEquals(List.of(), sessionMute.surlignages(URI,
                    new PositionLsp(0, 1)).get(5, TimeUnit.SECONDS));
            assertNull(sessionMute.preparerRennomage(URI,
                    new PositionLsp(0, 1)).get(5, TimeUnit.SECONDS));
            assertEquals(EditionEspaceLsp.VIDE, sessionMute.rennomage(URI,
                    new PositionLsp(0, 1), "autre").get(5, TimeUnit.SECONDS));
            assertEquals(List.of(), sessionMute.correctifs(URI,
                    plage(0, 0, 0, 1)).get(5, TimeUnit.SECONDS));
            assertEquals(List.of(), sessionMute.indices(URI,
                    plage(0, 0, 1, 0)).get(5, TimeUnit.SECONDS));
            assertEquals(List.of(), sessionMute.jetons(URI)
                    .get(5, TimeUnit.SECONDS).jetons());
            assertEquals(List.of(), sessionMute.jetonsPlage(URI,
                    plage(0, 0, 1, 0)).get(5, TimeUnit.SECONDS).jetons());
            assertEquals(List.of(), sessionMute.references(URI,
                    new PositionLsp(0, 1)).get(5, TimeUnit.SECONDS));
            assertEquals(List.of(), sessionMute.plagesSelection(URI,
                    new PositionLsp(0, 1)).get(5, TimeUnit.SECONDS));
        }
    }

    /**
     * Le terrain du banc : chaque méthode du SPI y répond RICHE — le
     * aller-retour se prête sur des valeurs, pas des vides.
     */
    private static final class TerrainServant
            implements ServeurEmbarque.Documents {

        @Override
        public List<SymboleLsp> symboles(String uri) {
            return List.of(new SymboleLsp("racine", 5, plage(0, 0, 3, 0),
                    plage(0, 0, 0, 6), List.of(new SymboleLsp("enfant", 12,
                            plage(1, 0, 2, 0), plage(1, 0, 1, 7),
                            List.of()))));
        }

        @Override
        public List<PliLsp> plis(String uri) {
            return List.of(new PliLsp(0, 3));
        }

        @Override
        public List<EditionTexteLsp> formatage(String uri,
                OptionsFormatageLsp options) {
            assertEquals(4, options.largeurTab());
            return List.of(new EditionTexteLsp(plage(0, 0, 0, 2), "aligne"));
        }

        @Override
        public List<EditionTexteLsp> formatagePlage(String uri,
                PlageLsp plage, OptionsFormatageLsp options) {
            return List.of(new EditionTexteLsp(plage, "local"));
        }

        @Override
        public List<SurlignageLsp> surlignages(String uri,
                PositionLsp position) {
            return List.of(new SurlignageLsp(plage(0, 4, 0, 9), 2));
        }

        @Override
        public PreparationRennomageLsp preparerRennomage(String uri,
                PositionLsp position) {
            return new PreparationRennomageLsp(plage(0, 4, 0, 9), "ancien");
        }

        @Override
        public EditionEspaceLsp rennomage(String uri, PositionLsp position,
                String nouveauNom) {
            assertEquals("renouvele", nouveauNom);
            return new EditionEspaceLsp(List.of(new EditionDocumentLsp(uri, 7,
                    List.of(new EditionTexteLsp(plage(0, 4, 0, 9),
                            nouveauNom)))), List.of());
        }

        @Override
        public List<CorrectifLsp> correctifs(String uri, PlageLsp plage) {
            return List.of(new CorrectifLsp("Renommer en majuscules",
                    new EditionEspaceLsp(List.of(new EditionDocumentLsp(uri,
                            -1, List.of(new EditionTexteLsp(plage,
                                    "MAJUSCULES")))), List.of())));
        }

        @Override
        public List<IndiceLsp> indices(String uri, PlageLsp plage) {
            return List.of(new IndiceLsp(new PositionLsp(4, 2), List.of(
                    new IndiceLsp.Partie("Double",
                            new LieuLsp(uri, plage(9, 0, 9, 3))),
                    new IndiceLsp.Partie("total", null)), 1, true, true));
        }

        @Override
        public SemanticTokensLsp jetons(String uri) {
            return SemanticTokensLsp.de("resultat-1", List.of(0, 4, 5, 1, 0));
        }

        @Override
        public SemanticTokensLsp jetonsPlage(String uri, PlageLsp plage) {
            return SemanticTokensLsp.de(null, List.of(0, 2, 3, 2, 0));
        }

        @Override
        public List<LieuLsp> references(String uri, PositionLsp position) {
            return List.of(new LieuLsp(uri, plage(3, 1, 3, 6)));
        }

        @Override
        public List<PlageLsp> plagesSelection(String uri,
                PositionLsp position) {
            return List.of(plage(0, 4, 0, 9), plage(0, 0, 1, 20));
        }
    }
}
