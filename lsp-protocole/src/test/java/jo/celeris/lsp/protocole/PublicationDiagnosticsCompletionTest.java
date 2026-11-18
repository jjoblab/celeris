package jo.celeris.lsp.protocole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import jo.celeris.core.lsp.types.DiagnosticLsp;
import jo.celeris.core.lsp.types.ElementCompletion;
import jo.celeris.core.lsp.types.ListeCompletion;
import jo.celeris.core.lsp.types.PlageLsp;
import jo.celeris.core.lsp.types.PositionLsp;

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

    private ServeurEmbarque montage;

    private LspSession session;

    /** Les diagnostics entendus par l'auditeur de la session. */
    private final List<DiagnosticLsp> entendus = new CopyOnWriteArrayList<>();

    @BeforeEach
    void demarrer() {
        montage = ServeurEmbarque.monter(new TerrainPublicant(),
                "celeris-banc");
        session = montage.session();
        session.initialiser();
        session.ajouterAuditeurDiagnostics((uri, diagnostics) ->
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
        session.ouvrir(URI, "java", "class A {");
        attendreDiagnostics(1);
        DiagnosticLsp diagnostic = entendus.get(0);
        assertEquals(new PlageLsp(new PositionLsp(0, 0),
                new PositionLsp(0, 1)), diagnostic.plage());
        assertEquals(DiagnosticLsp.ERREUR, diagnostic.gravite());
        assertEquals("accolade jamais fermee", diagnostic.message());
        assertEquals("banc", diagnostic.source());
        // le changement re-publie : la nouvelle version du texte
        session.changer(URI, 2, "class A {}");
        attendreDiagnostics(2);
        assertTrue(entendus.size() >= 2, "le changement publie aussi");
    }

    /** La complétion du terrain traverse le protocole : éléments,
     *  genres, détail, documentation, texte d'insertion, coupure. */
    @Test
    void laCompletionTraverseLeProtocole() throws Exception {
        session.ouvrir(URI, "java", "class A {}");
        ListeCompletion liste = session.completion(URI,
                new PositionLsp(0, 8)).get(5, TimeUnit.SECONDS);
        assertEquals(1, liste.elements().size());
        ElementCompletion element = liste.elements().get(0);
        assertEquals("length()", element.libelle());
        assertEquals(2, element.genre());
        assertEquals("int", element.detail());
        assertEquals("La longueur.", element.documentation());
        assertEquals("length", element.texteInsertion());
        assertTrue(liste.estIncomplete());
    }

    /** Le terrain sans service rend ses replis : pas de publication,
     *  pas de complétion — la liste vide, jamais d'exception. */
    @Test
    void leTerrainMuetNePublieRien() throws Exception {
        ServeurEmbarque muet = ServeurEmbarque.monter(
                new ServeurEmbarque.Documents() {
                }, "celeris-banc-muet");
        try {
            LspSession sessionMuet = muet.session();
            sessionMuet.initialiser();
            sessionMuet.ouvrir(URI, "texte", "rien");
            ListeCompletion liste = sessionMuet.completion(URI,
                    new PositionLsp(0, 0)).get(5, TimeUnit.SECONDS);
            assertTrue(liste.elements().isEmpty(),
                    "le repli rend la liste vide");
        } finally {
            muet.close();
        }
    }

    /** Attendre que le nombre de diagnostics entendus atteigne le seuil. */
    private void attendreDiagnostics(int seuil) throws InterruptedException {
        long fin = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (entendus.size() < seuil
                && System.nanoTime() < fin) {
            Thread.sleep(20L);
        }
        assertTrue(entendus.size() >= seuil,
                "les diagnostics se publient ("
                        + entendus.size() + " entendus)");
    }

    /** Le terrain qui publie et sert : les deux additions du retour. */
    private static final class TerrainPublicant
            implements ServeurEmbarque.Documents {

        @Override
        public void ouvrir(String uri, String langue, String texte) {
            // le texte suivi ne sert pas au terrain : la publication
            // est constante, le banc ne lit que son passage
        }

        @Override
        public void changer(String uri, int version, String texte) {
            // idem : le changement re-publie la même constante
        }

        @Override
        public List<DiagnosticLsp> diagnosticsDe(String uri) {
            return List.of(new DiagnosticLsp(
                    new PlageLsp(new PositionLsp(0, 0),
                            new PositionLsp(0, 1)),
                    DiagnosticLsp.ERREUR,
                    "accolade jamais fermee", "0", "banc"));
        }

        @Override
        public ListeCompletion completion(String uri,
                PositionLsp position) {
            return ListeCompletion.de(true, List.of(
                    new ElementCompletion("length()", 2, "int",
                            "La longueur.", null, null, "length", false)));
        }
    }
}
