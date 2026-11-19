package jo.celeris.lsp.protocole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import jo.celeris.core.lsp.types.DiagnosticLsp;
import jo.celeris.core.lsp.types.CompletionItemLsp;
import jo.celeris.core.lsp.types.CompletionListLsp;
import jo.celeris.core.lsp.types.RangeLsp;
import jo.celeris.core.lsp.types.LspPosition;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Le retour terrain du 2026-11-19 exercé au MONTEUR : le serveur de
 * documents qui PUBLIE ses diagnostics et SERT sa complétion — les
 * deux additions au chemin standard (le mini serveur Java de
 * l'exemple s'y branche). Les diagnostics partent du terrain,
 * traversent le protocole (publishDiagnostics du proxy client
 * injecté par la convention setClient) et reviennent à l'auditeur de
 * la session ; la complétion fait le même aller-retour, éléments et
 * documentation compris.
 */
@Timeout(20)
class PublicationDiagnosticsCompletionTest {

    private static final String URI = "file:///banc/publication.txt";

    private EmbeddedServer montage;

    private LspSession session;

    /** Les diagnostics entendus par l'auditeur de la session. */
    private final List<DiagnosticLsp> entendus = new CopyOnWriteArrayList<>();

    @BeforeEach
    void demarrer() {
        montage = EmbeddedServer.start(new TerrainPublicant(),
                "celeris-banc");
        session = montage.session();
        session.initialize();
        session.addDiagnosticsListener((uri, diagnostics) ->
                entendus.addAll(diagnostics));
    }

    @AfterEach
    void arreter() {
        montage.close();
    }

    /**
     * Les diagnostics du terrain se PUBLIENT par le protocole :
     * l'ouverture comme le changement déclenchent la publication,
     * l'auditeur l'entend — plages, gravité, message, code et source
     * compris.
     */
    @Test
    void louverturePublieLesDiagnostics() throws Exception {
        session.open(URI, "java", "class A {");
        attendreDiagnostics(1);
        DiagnosticLsp diagnostic = entendus.get(0);
        assertEquals(new RangeLsp(new LspPosition(0, 0),
                new LspPosition(0, 1)), diagnostic.range());
        assertEquals(DiagnosticLsp.ERREUR, diagnostic.severity());
        assertEquals("accolade jamais fermee", diagnostic.message());
        assertEquals("banc", diagnostic.source());
        // le changement re-publie : la nouvelle version du texte
        session.change(URI, 2, "class A {}");
        attendreDiagnostics(2);
        assertTrue(entendus.size() >= 2, "le changement publie aussi");
    }

    /** La complétion du terrain traverse le protocole : éléments,
     *  genres, détail, documentation, texte d'insertion, coupure. */
    @Test
    void laCompletionTraverseLeProtocole() throws Exception {
        session.open(URI, "java", "class A {}");
        CompletionListLsp liste = session.completion(URI,
                new LspPosition(0, 8)).get(5, TimeUnit.SECONDS);
        assertEquals(1, liste.elements().size());
        CompletionItemLsp element = liste.elements().get(0);
        assertEquals("length()", element.label());
        assertEquals(2, element.kind());
        assertEquals("int", element.detail());
        assertEquals("La longueur.", element.documentation());
        assertEquals("length", element.insertText());
        assertTrue(liste.isIncomplete());
    }

    /** Le terrain sans service rend ses replis : pas de publication,
     *  pas de complétion — la liste vide, jamais d'exception. */
    @Test
    void leTerrainMuetNePublieRien() throws Exception {
        EmbeddedServer muet = EmbeddedServer.start(
                new EmbeddedServer.Docs() {
                }, "celeris-banc-muet");
        try {
            LspSession sessionMuet = muet.session();
            sessionMuet.initialize();
            sessionMuet.open(URI, "texte", "rien");
            CompletionListLsp liste = sessionMuet.completion(URI,
                    new LspPosition(0, 0)).get(5, TimeUnit.SECONDS);
            assertTrue(liste.elements().isEmpty(),
                    "le repli rend la liste vide");
        } finally {
            muet.close();
        }
    }

    /** Attendre que le nombre de diagnostics entendus atteigne le seuil. */
    private void attendreDiagnostics(int seuil) throws InterruptedException {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (entendus.size() < seuil
                && System.nanoTime() < end) {
            Thread.sleep(20L);
        }
        assertTrue(entendus.size() >= seuil,
                "les diagnostics se publient ("
                        + entendus.size() + " entendus)");
    }

    /** Le terrain qui publie et sert : les deux additions du retour. */
    private static final class TerrainPublicant
            implements EmbeddedServer.Docs {

        @Override
        public void open(String uri, String language, String text) {
            // le texte suivi ne sert pas au terrain : la publication
            // est constante, le banc ne lit que son passage
        }

        @Override
        public void change(String uri, int version, String text) {
            // idem : le changement re-publie la même constante
        }

        @Override
        public List<DiagnosticLsp> diagnosticsOf(String uri) {
            return List.of(new DiagnosticLsp(
                    new RangeLsp(new LspPosition(0, 0),
                            new LspPosition(0, 1)),
                    DiagnosticLsp.ERREUR,
                    "accolade jamais fermee", "0", "banc"));
        }

        @Override
        public CompletionListLsp completion(String uri,
                LspPosition position) {
            return CompletionListLsp.of(true, List.of(
                    new CompletionItemLsp("length()", 2, "int",
                            "La longueur.", null, null, "length", false)));
        }
    }
}
