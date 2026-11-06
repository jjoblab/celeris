package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.JavaSyntax;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;

/**
 * Le renommage multi-fichiers (B-19) : le plan se construit sur l'index
 * hors ligne — une édition par occurrence, fichier par fichier — et
 * s'applique par la voie de transaction, annulable. Les conflits sont
 * SIGNALÉS : deux déclarations du même nom, un nouveau nom déjà pris.
 */
class RenommageTest {

    private final SyntaxService java = SyntaxService.of(JavaSyntax.INSTANCE);

    private static String sourceClient() {
        return """
                package demo;

                class Client {
                    String nomComplet() { return ""; }
                }
                """;
    }

    private static String sourceUtilisateur() {
        return """
                package demo;

                class Utilisateur {
                    Client client;

                    Client voisin() { return null; }
                }
                """;
    }

    private IndexSymboles indexDeuxFichiers() {
        IndexSymboles index = IndexSymboles.vide();
        for (String[] uriSource : new String[][] {
                {"file:///Client.java", sourceClient()},
                {"file:///Utilisateur.java", sourceUtilisateur()}}) {
            Document doc = Document.of(uriSource[1]);
            index.remplacer(uriSource[0], doc, java.analyseFull(doc));
        }
        return index;
    }

    /** Le plan : une édition par occurrence, fichier par fichier, offsets exacts. */
    @Test
    void lePlanMultiFichiers() {
        IndexSymboles index = indexDeuxFichiers();
        Renommage.PlanRenommage plan = Renommage.plan(index, "Client", "Acheteur");
        assertTrue(plan.conflits().isEmpty());
        Map<String, List<jo.celeris.core.text.TextEdit>> editions = plan.editions();
        assertEquals(2, editions.size());
        // Client.java : la déclaration seule
        List<jo.celeris.core.text.TextEdit> dansClient = editions.get("file:///Client.java");
        assertNotNull(dansClient);
        assertEquals(1, dansClient.size());
        // Utilisateur.java : le champ ET le type de retour — deux occurrences
        List<jo.celeris.core.text.TextEdit> dansUtilisateur = editions.get("file:///Utilisateur.java");
        assertNotNull(dansUtilisateur);
        assertEquals(2, dansUtilisateur.size());
        // chaque édition remplace exactement l'ancien nom par le nouveau
        Document utilisateur = Document.of(sourceUtilisateur());
        for (jo.celeris.core.text.TextEdit edition : dansUtilisateur) {
            assertEquals("Client", edition.removed());
            assertEquals("Acheteur", edition.inserted());
            assertEquals("Client", utilisateur.subText(edition.start(), edition.start()
                    + edition.removed().length()));
        }
    }

    /** L'application : les deux fichiers renommés, l'annulation restitue tout. */
    @Test
    void lapplicationAnnulable() {
        IndexSymboles index = indexDeuxFichiers();
        Renommage.PlanRenommage plan = Renommage.plan(index, "Client", "Acheteur");
        TextSession sessionClient = TextSession.of(sourceClient(), System::nanoTime);
        TextSession sessionUtilisateur = TextSession.of(sourceUtilisateur(), System::nanoTime);
        List<jo.celeris.core.text.TextEdit> editionsClient =
                editionsDe(plan, "file:///Client.java");
        List<jo.celeris.core.text.TextEdit> editionsUtilisateur =
                editionsDe(plan, "file:///Utilisateur.java");
        assertNotNull(editionsClient);
        assertNotNull(editionsUtilisateur);
        sessionClient.apply(jo.celeris.core.text.Transaction.of(editionsClient));
        sessionUtilisateur.apply(jo.celeris.core.text.Transaction.of(editionsUtilisateur));
        assertTrue(sessionClient.text().contains("class Acheteur {"));
        assertTrue(!sessionClient.text().contains("Client"));
        assertTrue(sessionUtilisateur.text().contains("Acheteur client;"));
        assertTrue(sessionUtilisateur.text().contains("Acheteur voisin()"));
        assertTrue(!sessionUtilisateur.text().contains("Client"));
        // annulable : fichier par fichier, l'original revient
        sessionClient.undo();
        sessionUtilisateur.undo();
        assertEquals(sourceClient(), sessionClient.text());
        assertEquals(sourceUtilisateur(), sessionUtilisateur.text());
    }

    /** Aide : les éditions du plan pour une uri — le test sait qu'elles y sont. */
    private static List<jo.celeris.core.text.TextEdit> editionsDe(
            Renommage.PlanRenommage plan, String uri) {
        List<jo.celeris.core.text.TextEdit> editions = plan.editions().get(uri);
        if (editions == null) {
            throw new AssertionError("aucune edition pour " + uri);
        }
        return editions;
    }

    /** Le conflit d'ambiguïté : deux déclarations portent le nom — signalé, le plan tient. */
    @Test
    void leConflitDAmbiguite() {
        IndexSymboles index = indexDeuxFichiers();
        // un second Client déclaré dans Utilisateur.java : le nom est ambigu
        Document deuxieme = Document.of("class Client { int doublon; }\n");
        index.remplacer("file:///Doublon.java", deuxieme, java.analyseFull(deuxieme));
        Renommage.PlanRenommage plan = Renommage.plan(index, "Client", "Acheteur");
        assertEquals(1, plan.conflits().size());
        assertEquals(Renommage.RaisonConflit.AMBIGUITE, plan.conflits().get(0).raison());
        assertTrue(plan.conflits().get(0).nom().equals("Client"));
        // le plan existe toujours : l'hôte décide après lecture du conflit
        assertTrue(!plan.editions().isEmpty());
    }

    /** Le conflit de collision : le nouveau nom est déjà déclaré — signalé. */
    @Test
    void leConflitDeCollision() {
        IndexSymboles index = indexDeuxFichiers();
        // Acheteur existe déjà quelque part
        Document acheteur = Document.of("class Acheteur { int deja; }\n");
        index.remplacer("file:///Acheteur.java", acheteur, java.analyseFull(acheteur));
        Renommage.PlanRenommage plan = Renommage.plan(index, "Client", "Acheteur");
        assertEquals(1, plan.conflits().size());
        assertEquals(Renommage.RaisonConflit.COLLISION, plan.conflits().get(0).raison());
    }

    /** Le nom inconnu : plan vide, aucun conflit — rien à renommer. */
    @Test
    void leNomInconnu() {
        IndexSymboles index = indexDeuxFichiers();
        Renommage.PlanRenommage plan = Renommage.plan(index, "Inconnu", "Autre");
        assertTrue(plan.editions().isEmpty());
        assertTrue(plan.conflits().isEmpty());
    }
}
