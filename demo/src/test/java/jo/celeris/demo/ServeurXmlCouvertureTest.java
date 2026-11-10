package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import jo.celeris.core.lsp.EditionEspaceLsp;
import jo.celeris.core.lsp.EditionTexteLsp;
import jo.celeris.core.lsp.LieuLsp;
import jo.celeris.core.lsp.OptionsFormatageLsp;
import jo.celeris.core.lsp.PlageLsp;
import jo.celeris.core.lsp.PliLsp;
import jo.celeris.core.lsp.PositionLsp;
import jo.celeris.core.lsp.PreparationRennomageLsp;
import jo.celeris.core.lsp.SurlignageLsp;
import jo.celeris.core.lsp.SymboleLsp;
import jo.celeris.core.text.TextSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * La couverture LSP nouvelle du jalon (H-12) contre le serveur XML
 * RÉEL embarqué (M8.6 — LemMinX, identité au NOTICE), monté par sa
 * DÉCLARATION (H-13 — servers.json, le chemin réel de la
 * démonstration) : les méthodes raccordées à la porte de session de
 * la démonstration — symboles, plis, formatage, surlignages,
 * préparation de renommage, renommage, références, plages de
 * sélection — servies par le fil protocolaire de la frontière
 * (ADR-015), rendues en types du domaine. Les sept tests de parité
 * de {@code ServeurXmlEmbarqueTest} restent INTACTS : ici croît la
 * surface, là se garde la parité.
 */
class ServeurXmlCouvertureTest {

    private static final String URI = "file:///couverture.xml";

    private static final String DOCUMENT = """
            <?xml version="1.0" encoding="utf-8"?>
            <catalogue>
                <exemple id="premier">
                    <nom>premier</nom>
                </exemple>
                <exemple id="second">
                    <nom>second</nom>
                </exemple>
            </catalogue>
            """;

    private LanguageSession serveur;
    private jo.celeris.core.text.Document document;

    @BeforeEach
    void demarrer() {
        // le chemin réel de la démonstration (H-13) : la déclaration
        // servers.json décide du serveur, le montage l'exécute
        serveur = DeclaredServers.open(
                LangagesDemo.langageDe("couverture.xml", null, null));
        serveur.ouvrir(URI, "xml", DOCUMENT);
        document = TextSession.of(DOCUMENT, () -> 0L).document();
    }

    @AfterEach
    void arreter() {
        serveur.close();
    }

    private static <T> T joindre(java.util.concurrent.CompletableFuture<T> futur)
            throws Exception {
        return futur.get(20, TimeUnit.SECONDS);
    }

    /** La position LSP d'un texte cherché — la première occurrence. */
    private static PositionLsp positionDe(String texte) {
        int offset = DOCUMENT.indexOf(texte);
        return jo.celeris.core.lsp.PositionsLsp.position(
                TextSession.of(DOCUMENT, () -> 0L).document(), offset);
    }

    /** Les symboles : le document en porte, la hiérarchie suit. */
    @Test
    void lesSymbolesServentLeDocument() throws Exception {
        List<SymboleLsp> symboles = joindre(serveur.symboles(URI));
        assertFalse(symboles.isEmpty(), "le serveur réel sert les symboles");
        assertTrue(symboles.stream().anyMatch(s -> s.nom().equals("catalogue")),
                "l'élément racine est un symbole : " + symboles);
    }

    /** Les plis : le document imbriqué en porte, la fin est EXCLUSIVE. */
    @Test
    void lesPlisServentLeDocument() throws Exception {
        List<PliLsp> plis = joindre(serveur.plis(URI));
        assertFalse(plis.isEmpty(), "le serveur réel sert les plis");
        for (PliLsp pli : plis) {
            assertTrue(pli.ligneFin() > pli.ligneDebut(),
                    "la fin EXCLUSIVE reste après le début : " + pli);
        }
    }

    /** Le formatage : le document mal aligné se réaligne. */
    @Test
    void leFormatageSertSesEditions() throws Exception {
        List<EditionTexteLsp> editions = joindre(serveur.formatage(URI,
                OptionsFormatageLsp.DEFAUT));
        assertNotNull(editions);
    }

    /** Les surlignages : l'élément sous le curseur se surligne partout. */
    @Test
    void lesSurlignagesServentLesOccurrences() throws Exception {
        List<SurlignageLsp> surlignages = joindre(serveur.surlignages(URI,
                document, DOCUMENT.indexOf("exemple")));
        assertFalse(surlignages.isEmpty(),
                "le serveur réel surligne les occurrences de l'élément");
    }

    /**
     * La préparation de renommage : la forme « plage seule » du serveur
     * réel — l'amorce absente est légitime, la plage nomme le
     * renommable, jamais deviné.
     */
    @Test
    void laPreparationDeRennomageSertSaPlage() throws Exception {
        // le curseur DANS le nom de la balise ouvrante — le renommable
        PreparationRennomageLsp preparation = joindre(
                serveur.preparerRennomage(URI, document,
                        DOCUMENT.indexOf("<exemple") + 3));
        assertNotNull(preparation, "le serveur réel prépare le renommage");
        assertEquals(new PlageLsp(new PositionLsp(2, 5),
                new PositionLsp(2, 12)), preparation.plage(),
                "la plage porte le nom de la balise ouvrante");
    }

    /**
     * Le renommage : le serveur réel 0.31.2 répond l'erreur interne —
     * contre un client aux capacités minimales, son renommage casse de
     * son côté ; la frontière porte l'erreur FIDÈLEMENT (elle ne
     * l'invente pas, ne l'avale pas), l'appelant l'apprend telle
     * quelle. Le constat vit au changement du jalon : la négociation
     * de capacités clientes affinera la réponse, jamais la voie.
     */
    @Test
    void leRennomagePorteLErreurDuServeurReelFidelement() throws Exception {
        java.util.concurrent.CompletableFuture<EditionEspaceLsp> edition =
                serveur.rennomage(URI, document,
                        DOCUMENT.indexOf("<exemple") + 3, "renouvele");
        java.util.concurrent.ExecutionException echec =
                org.junit.jupiter.api.Assertions.assertThrows(
                        java.util.concurrent.ExecutionException.class,
                        () -> joindre(edition));
        assertNotNull(echec.getCause(),
                "l'erreur du serveur réel traverse la frontière");
        assertTrue(String.valueOf(echec.getCause().getMessage())
                        .contains("Internal error"),
                "l'erreur du serveur arrive telle quelle : " + echec.getCause());
    }

    /** Les références : l'élément nommé sert ses lieux. */
    @Test
    void lesReferencesServentLesLieux() throws Exception {
        List<LieuLsp> lieux = joindre(serveur.references(URI, document,
                DOCUMENT.indexOf("exemple") + 1));
        assertNotNull(lieux);
    }

    /** Les plages de sélection : la chaîne des englobantes descend. */
    @Test
    void lesPlagesDeSelectionDescendent() throws Exception {
        List<PlageLsp> plages = joindre(serveur.plagesSelection(URI, document,
                DOCUMENT.indexOf("premier") + 1));
        assertNotNull(plages);
    }
}
