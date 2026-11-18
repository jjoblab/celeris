package jo.celeris.core.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.syntax.SyntaxIssue;
import jo.celeris.core.text.TextSession;
import jo.celeris.core.text.TextEdit;

/**
 * Tests du connecteur MCP (F-06, ADR-012) : les mêmes outils exposés
 * par le connecteur et en direct — le catalogue se liste comme un
 * serveur d'outils, chaque appel traverse la même méthode que l'appel
 * direct, la réponse se rend en JSON.
 */
class McpConnectorTest {

    private static EditorTools outils() {
        TextSession session = TextSession.of("fun principal() {\n}", System::nanoTime);
        return new PerSessionTools(session,
                document -> List.of(new OutlineEntry(NodeCategory.FUNCTION,
                        "principal", 0, document.length(), 1, 2)),
                () -> List.of(new SyntaxIssue(0, 3, "accolade \"ouverte\"")));
    }

    /**
     * F-06 — le catalogue : les cinq outils de base plus les
     * instructions (F-07), noms celeris.*, descriptions et arguments
     * lus par le serveur.
     */
    @Test
    void leCatalogueListeLesOutils() {
        List<McpConnector.DescriptionOutil> catalogue = McpConnector.catalogue();
        assertEquals(6, catalogue.size());
        assertEquals("celeris.lire", catalogue.get(0).nom());
        assertEquals(List.of("debut", "fin"), catalogue.get(0).arguments());
        assertEquals("celeris.appliquer", catalogue.get(5).nom());
        assertTrue(catalogue.stream().allMatch(d -> !d.description().isBlank()),
                "chaque outil porte sa description");
    }

    /**
     * F-06 — le critère de la matrice : les mêmes outils exposés par le
     * connecteur et en direct. La lecture, l'instantané, les symboles
     * et les diagnostics rendent en JSON ce que l'appel direct rend en
     * types — même méthode, même contenu.
     */
    @Test
    void memesOutilsParLeConnecteurEtEnDirect() {
        EditorTools outils = outils();

        assertEquals("{\"revision\":0,\"texte\":\"fun\"}",
                McpConnector.lire(outils, 0, 3));
        assertEquals("{\"revision\":0,\"texte\":\""
                        + "fun principal() {\\n}\"}",
                McpConnector.instantane(outils));
        assertTrue(McpConnector.symboles(outils).contains(
                "\"nom\":\"principal\""));
        assertTrue(McpConnector.symboles(outils).contains(
                "\"categorie\":\"FUNCTION\""));
        assertTrue(McpConnector.diagnostics(outils).contains(
                "accolade \\\"ouverte\\\""),
                "les guillemets du message s'echappent en JSON");

        assertEquals(outils.lire(0, 3).texte(), "fun");
        assertEquals(outils.symboles().get(0).name(), "principal");
        assertEquals(outils.diagnostics().get(0).message(),
                "accolade \"ouverte\"");
    }

    /**
     * F-06 — l'application par le connecteur : le même refus de conflit
     * qu'en direct, le même application aussi — la réponse JSON porte
     * la sortie, le message et la révision courante.
     */
    @Test
    void applicationEtConflitParLeConnecteur() {
        TextSession session = TextSession.of("val a = 1", System::nanoTime);
        EditorTools outils = new PerSessionTools(session,
                document -> List.of(), List::of);

        String refus = McpConnector.appliquer(outils,
                new ToolTransaction(7, List.of(new TextEdit(0, "val", "var"))));
        assertTrue(refus.contains("\"sortie\":\"REFUSEE_CONFLIT\""),
                "le refus de conflit se rend pareil : " + refus);

        String reponse = McpConnector.appliquer(outils,
                new ToolTransaction(session.document().revision(),
                        List.of(new TextEdit(0, "val", "var"))));
        assertTrue(reponse.contains("\"sortie\":\"APPLIQUEE\""));
        assertTrue(reponse.contains("\"revision\":1"));
        assertEquals("var a = 1", session.document().text(),
                "l'application par le connecteur mute comme en direct");
    }
}
