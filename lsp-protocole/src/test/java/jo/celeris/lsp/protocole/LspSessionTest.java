package jo.celeris.lsp.protocole;

import jo.celeris.core.lsp.types.CodeActionLsp;
import jo.celeris.core.lsp.types.DiagnosticLsp;
import jo.celeris.core.lsp.types.WorkspaceEditLsp;
import jo.celeris.core.lsp.types.TextEditLsp;
import jo.celeris.core.lsp.types.InlayHintLsp;
import jo.celeris.core.lsp.types.LocationLsp;
import jo.celeris.core.lsp.types.CompletionListLsp;
import jo.celeris.core.lsp.types.FormatOptionsLsp;
import jo.celeris.core.lsp.types.RangeLsp;
import jo.celeris.core.lsp.types.FoldLsp;
import jo.celeris.core.lsp.types.LspPosition;
import jo.celeris.core.lsp.types.RenamePrepareLsp;
import jo.celeris.core.lsp.types.SemanticTokensLsp;
import jo.celeris.core.lsp.types.SignatureLsp;
import jo.celeris.core.lsp.types.HighlightLsp;
import jo.celeris.core.lsp.types.HoverLsp;
import jo.celeris.core.lsp.types.SymbolLsp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;














import jo.celeris.core.lsp.RequestCancelledException;





/**
 * La session de la frontière exercée de bout en bout contre un serveur
 * factice EN PROCESSUS (ADR-015) : le protocole circule pour de vrai —
 * cadrage, sérialisation, fils de lecture — sans qu'aucun type de la
 * bibliothèque ne paraisse dans les réponses, qui sont du domaine.
 */
public final class LspSessionTest {

    private static final String URI = "file:///banc/exemple.xml";

    /** Une session démarrée avec son serveur factice et son état observable. */
    private static final class Banc implements AutoCloseable {
        private final LspSession session;
        private final ServeurFactice.State etat;

        private Banc(LspSession session, ServeurFactice.State etat) {
            this.session = session;
            this.etat = etat;
        }

        private LspSession session() {
            return session;
        }

        private ServeurFactice.State etat() {
            return etat;
        }

        @Override
        public void close() {
            session.close();
        }
    }

    private static Banc demarrer() throws Exception {
        // tube à file, le transport du dépôt de départ et des bancs (ADR-015) :
        // les tubes du JDK couplent la lecture à la vivacité du fil
        // écrivain — les fils de piscine du serveur sont éphémères
        MemoryPipes.Direction toServer = MemoryPipes.create();
        MemoryPipes.Direction fromServer = MemoryPipes.create();
        ServeurFactice.State etat = ServeurFactice.demarrer(
                toServer.input(), fromServer.output());
        LspSession session = LspSession.on(
                fromServer.input(), toServer.output(), "celeris-banc");
        session.initialize();
        return new Banc(session, etat);
    }

    private static <T> List<T> attendreNonVide(Supplier<List<T>> sonde)
            throws Exception {
        long borne = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        List<T> vu = sonde.get();
        while (vu.isEmpty() && System.nanoTime() < borne) {
            Thread.sleep(10);
            vu = sonde.get();
        }
        return vu;
    }

    private static void attendreCondition(java.util.function.BooleanSupplier condition)
            throws Exception {
        long borne = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < borne) {
            Thread.sleep(10);
        }
    }

    @Test
    void laPoigneeDeMainNegocieLesCapacites() throws Exception {
        try (Banc banc = demarrer()) {
            assertTrue(banc.session().capabilities().supportsCompletion());
            assertTrue(banc.session().capabilities().supportsHover());
            assertTrue(banc.session().capabilities().supportsSignature());
            assertTrue(banc.session().capabilities().supportsDefinition());
            assertEquals(2, banc.session().capabilities().syncKind());
            assertEquals(List.of("type", "fonction"),
                    banc.session().capabilities().tokenTypes());
            assertEquals(List.of("declaration"),
                    banc.session().capabilities().tokenModifiers());
        }
    }

    @Test
    void leDocumentValidePublieSesDiagnostics() throws Exception {
        try (Banc banc = demarrer()) {
            List<DiagnosticLsp> entendues = new ArrayList<>();
            banc.session().addDiagnosticsListener((uri, diagnostics) ->
                    entendues.addAll(diagnostics));
            banc.session().open(URI, "xml", "<ex/>");
            List<DiagnosticLsp> publies = attendreNonVide(
                    () -> banc.session().diagnosticsOf(URI));
            assertEquals(1, publies.size());
            assertEquals(4, publies.get(0).severity());
            assertEquals("document ouvert", publies.get(0).message());
            assertEquals("factice", publies.get(0).source());
            assertEquals(1, banc.session().versionOf(URI));
            List<DiagnosticLsp> entendu = attendreNonVide(() -> entendues);
            assertEquals(1, entendu.size());
        }
    }

    @Test
    void laFermetureOublieLesDiagnostics() throws Exception {
        try (Banc banc = demarrer()) {
            banc.session().open(URI, "xml", "<ex/>");
            attendreNonVide(() -> banc.session().diagnosticsOf(URI));
            banc.session().close(URI);
            assertTrue(banc.session().diagnosticsOf(URI).isEmpty(),
                    "la fermeture purge la carte des diagnostics : "
                            + "une session longue n'accumule rien");
        }
    }

    @Test
    void lEditionSuitLeDocumentEtRevalide() throws Exception {
        try (Banc banc = demarrer()) {
            banc.session().open(URI, "xml", "<ex>");
            banc.session().change(URI, 2, "<ex>");
            List<DiagnosticLsp> publies = attendreNonVide(() ->
                    banc.session().diagnosticsOf(URI).stream()
                            .filter(d -> d.severity() == DiagnosticLsp.ERREUR)
                            .toList());
            assertEquals(1, publies.size());
            assertEquals("l'élément **n'est pas déclaré**",
                    publies.get(0).message());
            assertEquals("42", publies.get(0).code());
            assertEquals(2, banc.session().versionOf(URI));
            assertEquals("<ex>", banc.etat().dernierTexte.get());
        }
    }

    @Test
    void laCompletionSertLesElementsDocumentes() throws Exception {
        try (Banc banc = demarrer()) {
            banc.session().open(URI, "xml", "<ex");
            CompletionListLsp liste = banc.session().completion(
                    URI, new LspPosition(0, 3)).get(5, TimeUnit.SECONDS);
            assertTrue(liste.isIncomplete());
            assertEquals(1, liste.elements().size());
            assertEquals("ex:exemplaire", liste.elements().get(0).label());
            assertEquals(5, liste.elements().get(0).kind());
            assertEquals("l'élément exemplaire",
                    liste.elements().get(0).detail());
            assertEquals("La documentation de l'élément exemplaire.",
                    liste.elements().get(0).documentation());
        }
    }

    @Test
    void leSurvolSertLaDocumentationBalisee() throws Exception {
        try (Banc banc = demarrer()) {
            banc.session().open(URI, "xml", "<ex");
            HoverLsp hover = banc.session().hover(
                    URI, new LspPosition(0, 1)).get(5, TimeUnit.SECONDS);
            assertEquals("La documentation survolée.", hover.text());
            assertEquals("markdown", hover.kind());
            RangeLsp plageSurvol = hover.range();
            assertNotNull(plageSurvol);
            assertEquals(new LspPosition(0, 0), plageSurvol.start());
        }
    }

    @Test
    void laSignatureSertLesParametresADecalages() throws Exception {
        try (Banc banc = demarrer()) {
            banc.session().open(URI, "xml", "<ex");
            SignatureLsp help = banc.session().signature(
                    URI, new LspPosition(0, 1)).get(5, TimeUnit.SECONDS);
            assertEquals(1, help.entrees().size());
            assertEquals("f(a, b)", help.entrees().get(0).label());
            assertEquals(2, help.entrees().get(0).parameters().size());
            assertEquals("a", help.entrees().get(0).parameters().get(0).label());
            assertEquals("b", help.entrees().get(0).parameters().get(1).label());
            assertEquals(0, help.signatureActiveIndex());
            assertEquals(1, help.parametreCourant());
        }
    }

    @Test
    void laDefinitionSertLesLieux() throws Exception {
        try (Banc banc = demarrer()) {
            banc.session().open(URI, "xml", "<ex");
            List<LocationLsp> lieux = banc.session().definition(
                    URI, new LspPosition(0, 1)).get(5, TimeUnit.SECONDS);
            assertEquals(1, lieux.size());
            assertEquals(URI, lieux.get(0).uri());
            RangeLsp plageLieu = lieux.get(0).range();
            assertNotNull(plageLieu);
            assertEquals(new LspPosition(0, 0), plageLieu.start());
        }
    }

    @Test
    void laFermetureDemandeBreakpointJamaisSortie() throws Exception {
        Banc banc = demarrer();
        banc.session().close();
        banc.session().close();
        assertTrue(banc.etat().arretDemande.get());
        assertFalse(banc.etat().sortieDemandee.get());
        assertThrows(IllegalStateException.class,
                () -> banc.session().open(URI, "xml", "<ex/>"));
    }

    @Test
    void laSessionRefuseSansInitialisation() throws Exception {
        MemoryPipes.Direction toServer = MemoryPipes.create();
        MemoryPipes.Direction fromServer = MemoryPipes.create();
        ServeurFactice.demarrer(toServer.input(), fromServer.output());
        try (LspSession session = LspSession.on(
                fromServer.input(), toServer.output(), "celeris-banc")) {
            assertThrows(IllegalStateException.class,
                    () -> session.open(URI, "xml", "<ex/>"));
        }
    }

    /**
     * PIEGE-18 — les frappes rapides, la preuve au fil : deux
     * complétions de suite, la seconde remplace la première en vol —
     * le futur remplacé échoue REMPLACÉE, l'annulation TRAVERSE le
     * fil ({@code $/cancelRequest} : le futur tenu côté serveur est
     * annulé par le cadre de la bibliothèque), le dernier mot gagne.
     */
    @Test
    void lesFrappesRapidesRemplacentEnVolAuFil() throws Exception {
        try (Banc banc = demarrer()) {
            banc.etat().tenirCompletion.set(true);
            banc.session().open(URI, "xml", "<ex");
            CompletableFuture<CompletionListLsp> vieille = banc.session()
                    .completion(URI, new LspPosition(0, 2));
            CompletableFuture<CompletionListLsp> fraiche = banc.session()
                    .completion(URI, new LspPosition(0, 3));
            attendreCondition(() -> banc.etat().volsCompletion.size() >= 2);
            assertEquals(2, banc.etat().volsCompletion.size(),
                    "les deux demandes ont volé");
            ExecutionException remplacee = assertThrows(ExecutionException.class,
                    () -> vieille.get(5, TimeUnit.SECONDS));
            assertTrue(remplacee.getCause() instanceof RequestCancelledException annulee
                    && annulee.reason() == RequestCancelledException.Reason.REMPLACEE,
                    "le futur remplacé échoue REMPLACÉE : " + remplacee.getCause());
            attendreCondition(() -> banc.etat().volsCompletion.get(0).isCancelled());
            assertTrue(banc.etat().volsCompletion.get(0).isCancelled(),
                    "l'annulation traverse le fil : le vol tenu côté serveur"
                            + " est annulé par $/cancelRequest");
            // le dernier mot gagne : le banc achève le second vol, la fraîche est servie
            banc.etat().volsCompletion.get(1).complete(Either.forRight(
                    new CompletionList(false, List.of())));
            CompletionListLsp servie = fraiche.get(5, TimeUnit.SECONDS);
            assertFalse(servie.isIncomplete(),
                    "la demande fraîche est servie, jamais empilée");
        }
    }

    // ── La couverture du jalon (H-12) : chaque méthode, la forme du
    //    protocole lue en domaine — le serveur factice les sert toutes. ──

    /** Les références : les lieux plats du protocole. */
    @Test
    void lesReferencesServentLesLieux() throws Exception {
        try (Banc banc = demarrer()) {
            List<LocationLsp> lieux = banc.session().references(URI,
                    new LspPosition(0, 4)).get(5, TimeUnit.SECONDS);
            assertEquals(1, lieux.size());
            assertEquals(URI, lieux.get(0).uri());
            assertNotNull(lieux.get(0).range());
            assertEquals(new LspPosition(3, 1), lieux.get(0).range().start());
        }
    }

    /** Les symboles : la hiérarchie du serveur suit, la profondeur entière. */
    @Test
    void lesSymbolesServentLaHierarchie() throws Exception {
        try (Banc banc = demarrer()) {
            List<SymbolLsp> symbols = banc.session().symbols(URI)
                    .get(5, TimeUnit.SECONDS);
            assertEquals(1, symbols.size());
            SymbolLsp racine = symbols.get(0);
            assertEquals("exemplaire", racine.nom());
            assertEquals(8, racine.kind());
            assertEquals(1, racine.enfants().size());
            assertEquals("imbrique", racine.enfants().get(0).nom());
        }
    }

    /** Les surlignages : la plage et son genre, lus du protocole. */
    @Test
    void lesSurlignagesServentLesOccurrences() throws Exception {
        try (Banc banc = demarrer()) {
            List<HighlightLsp> highlights = banc.session().highlights(URI,
                    new LspPosition(0, 5)).get(5, TimeUnit.SECONDS);
            assertEquals(1, highlights.size());
            assertEquals(new RangeLsp(new LspPosition(0, 4),
                    new LspPosition(0, 9)), highlights.get(0).range());
            assertEquals(2, highlights.get(0).kind());
        }
    }

    /** La préparation de renommage : la plage et son amorce. */
    @Test
    void laPreparationDeRennomageSertPlageEtAmorce() throws Exception {
        try (Banc banc = demarrer()) {
            RenamePrepareLsp preparation =
                    banc.session().preparerRennomage(URI,
                            new LspPosition(0, 5)).get(5, TimeUnit.SECONDS);
            assertNotNull(preparation);
            assertEquals(new RangeLsp(new LspPosition(0, 4),
                    new LspPosition(0, 9)), preparation.range());
            assertEquals("ancien", preparation.leadingText());
        }
    }

    /** Le renommage : l'édition d'espace porte le nouveau nom. */
    @Test
    void leRennomageSertLEditionDEspace() throws Exception {
        try (Banc banc = demarrer()) {
            WorkspaceEditLsp espace = banc.session().rename(URI,
                    new LspPosition(0, 5), "neuf").get(5, TimeUnit.SECONDS);
            assertEquals(1, espace.documents().size());
            assertEquals("neuf", espace.documents().get(0).edits().get(0)
                    .newText());
        }
    }

    /** Les correctifs : le titre et son édition — la carte héritée se plie. */
    @Test
    void lesCorrectifsServentTitreEtEdition() throws Exception {
        try (Banc banc = demarrer()) {
            List<CodeActionLsp> correctifs = banc.session().correctifs(URI,
                    new RangeLsp(new LspPosition(0, 0), new LspPosition(0, 4)))
                    .get(5, TimeUnit.SECONDS);
            assertEquals(1, correctifs.size());
            assertEquals("Entourer d'accolades", correctifs.get(0).titre());
            assertEquals(1, correctifs.get(0).edit().documents().size());
        }
    }

    /** Le formatage : les éditions de texte, l'ordre du serveur. */
    @Test
    void leFormatageSertSesEditions() throws Exception {
        try (Banc banc = demarrer()) {
            List<TextEditLsp> edits = banc.session().formatage(URI,
                    FormatOptionsLsp.DEFAUT).get(5, TimeUnit.SECONDS);
            assertEquals(1, edits.size());
            assertEquals("  ", edits.get(0).newText());
            List<TextEditLsp> range = banc.session().rangeFormatting(URI,
                    new RangeLsp(new LspPosition(0, 0), new LspPosition(1, 0)),
                    FormatOptionsLsp.DEFAUT).get(5, TimeUnit.SECONDS);
            assertEquals("formate", range.get(0).newText());
        }
    }

    /** Les plis : la fin inclusive du protocole devient EXCLUSIVE au domaine. */
    @Test
    void lesPlisDeviennentAFinExclusive() throws Exception {
        try (Banc banc = demarrer()) {
            List<FoldLsp> folds = banc.session().folds(URI).get(5, TimeUnit.SECONDS);
            assertEquals(1, folds.size());
            assertEquals(new FoldLsp(1, 5), folds.get(0),
                    "la ligne 4 inclusive du protocole vaut 5 exclusive");
        }
    }

    /** Les plages de sélection : la chaîne des englobantes, de l'intime à la large. */
    @Test
    void lesPlagesDeSelectionDescendentLesPaliers() throws Exception {
        try (Banc banc = demarrer()) {
            List<RangeLsp> ranges = banc.session().selectionRanges(URI,
                    new LspPosition(0, 5)).get(5, TimeUnit.SECONDS);
            assertEquals(2, ranges.size());
            assertEquals(new RangeLsp(new LspPosition(0, 4),
                    new LspPosition(0, 9)), ranges.get(0));
            assertEquals(new RangeLsp(new LspPosition(0, 0),
                    new LspPosition(0, 20)), ranges.get(1));
        }
    }

    /** Les jetons : la charge encodée traverse, le décodage reste au domaine. */
    @Test
    void lesTokensServentLaChargeEncodee() throws Exception {
        try (Banc banc = demarrer()) {
            SemanticTokensLsp tokens = banc.session().tokens(URI)
                    .get(5, TimeUnit.SECONDS);
            assertEquals("factice-1", tokens.identifiantResultat());
            assertEquals(1, tokens.tokens().size());
            assertEquals(2, tokens.tokens().get(0).line());
            SemanticTokensLsp range = banc.session().tokensRange(URI,
                    new RangeLsp(new LspPosition(0, 0), new LspPosition(1, 0)))
                    .get(5, TimeUnit.SECONDS);
            assertEquals(1, range.tokens().size());
        }
    }

    /** Les indices : les parties typées et leur cible de navigation. */
    @Test
    void lesIndicesServentPartiesEtCible() throws Exception {
        try (Banc banc = demarrer()) {
            List<InlayHintLsp> inlayHints = banc.session().inlayHints(URI,
                    new RangeLsp(new LspPosition(0, 0), new LspPosition(9, 0)))
                    .get(5, TimeUnit.SECONDS);
            assertEquals(1, inlayHints.size());
            InlayHintLsp indice = inlayHints.get(0);
            assertEquals(1, indice.kind());
            assertTrue(indice.remplissageGauche());
            assertEquals(2, indice.parties().size());
            assertEquals("Double", indice.parties().get(0).value());
            assertNotNull(indice.parties().get(0).target());
            assertEquals("total", indice.parties().get(1).value());
        }
    }
}
