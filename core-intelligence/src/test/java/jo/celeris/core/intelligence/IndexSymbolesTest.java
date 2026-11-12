package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.SyntaxService;
import jo.celeris.core.syntax.xml.XmlSyntax;
import jo.celeris.core.text.Document;

/**
 * L'index de symboles hors ligne (ADR-005, B-10/B-11/B-13) : alimenté
 * par le même arbre que la coloration, remplacé fichier par fichier à
 * chaque ré-analyse — la navigation répond sans serveur, à budget
 * mémoire tenu, persistant par forme sérialisée compacte.
 */
class IndexSymbolesTest {

    private final SyntaxService java = SyntaxService.of(JavaSyntax.INSTANCE);
    private final SyntaxService xml = SyntaxService.of(XmlSyntax.INSTANCE);

    /** Un fichier Java de démonstration : une classe, un champ, deux méthodes. */
    private static String sourceClient() {
        return """
                package demo;

                class Client {
                    private String nom;

                    String nomComplet() { return nom; }
                    void ajouter(Client autre) { }
                }
                """;
    }

    /** Un second fichier : il utilise le premier. */
    private static String sourceUtilisateur() {
        return """
                package demo;

                class Utilisateur {
                    Client client;

                    void saluer() {
                        Client voisin = null;
                    }
                }
                """;
    }

    private void indexJava(IndexSymboles index, String uri, String source) {
        Document doc = Document.of(source);
        Analysis analyse = java.analyseFull(doc);
        index.remplacer(uri, doc, analyse);
    }

    /** Les symboles sortent du plan du même arbre : catégories, bornes, lignes. */
    @Test
    void lesSymbolesDuPlan() {
        IndexSymboles index = IndexSymboles.vide();
        indexJava(index, "file:///Client.java", sourceClient());
        List<Symbole> symboles = index.symboles();
        assertEquals(4, symboles.size());
        Symbole classe = symboles.get(0);
        assertEquals("Client", classe.nom());
        assertEquals(NodeCategory.TYPE, classe.categorie());
        assertEquals("file:///Client.java", classe.uri());
        assertEquals(2, classe.ligne());
        assertTrue(classe.debut() < classe.fin());
        assertEquals("nom", symboles.get(1).nom());
        assertEquals(NodeCategory.FIELD, symboles.get(1).categorie());
        assertEquals("nomComplet", symboles.get(2).nom());
        assertEquals(NodeCategory.FUNCTION, symboles.get(2).categorie());
        assertEquals("ajouter", symboles.get(3).nom());
    }

    /** Le remplacement par fichier : la ré-analyse remplace, ne cumule pas. */
    @Test
    void leRemplacementParFichier() {
        IndexSymboles index = IndexSymboles.vide();
        indexJava(index, "file:///Client.java", sourceClient());
        assertEquals(4, index.symboles().size());
        // le fichier change : une méthode disparaît, une autre naît
        String nouvelle = """
                package demo;

                class Client {
                    String nomComplet() { return ""; }
                }
                """;
        indexJava(index, "file:///Client.java", nouvelle);
        assertEquals(2, index.symboles().size());
        assertEquals("Client", index.symboles().get(0).nom());
        assertEquals("nomComplet", index.symboles().get(1).nom());
    }

    /** Le retrait : le fichier fermé quitte l'index, les autres restent. */
    @Test
    void leRetrait() {
        IndexSymboles index = IndexSymboles.vide();
        indexJava(index, "file:///Client.java", sourceClient());
        indexJava(index, "file:///Utilisateur.java", sourceUtilisateur());
        assertEquals(7, index.symboles().size());
        index.retirer("file:///Client.java");
        assertEquals(3, index.symboles().size());
        assertTrue(index.symboles().stream()
                .allMatch(symbole -> symbole.uri().equals("file:///Utilisateur.java")));
    }

    /**
     * Les usages (B-11) : occurrences ENTIÈRES du nom à travers les fichiers
     * indexés — déclaration et usages, casse exacte, jamais dans les chaînes
     * ni les commentaires (les jetons du lexique décident).
     */
    @Test
    void lesUsagesEntreFichiers() {
        IndexSymboles index = IndexSymboles.vide();
        indexJava(index, "file:///Client.java", sourceClient());
        indexJava(index, "file:///Utilisateur.java",
                sourceUtilisateur().replace("Client voisin = null;",
                        "Client voisin = null; // Client en commentaire"));
        List<IndexSymboles.Usage> usages = index.usages("Client");
        // Client.java : la déclaration (le C de class Client) + le paramètre (Client autre) = 2
        // Utilisateur.java : le champ + la locale = 2 — le commentaire ne compte pas
        assertEquals(4, usages.size());
        assertTrue(usages.stream().anyMatch(usage ->
                usage.uri().equals("file:///Client.java") && usage.offset() == 21));
        assertEquals(2, usages.stream()
                .filter(usage -> usage.uri().equals("file:///Utilisateur.java")).count());
        // la casse est exacte : « client » minuscule ne trouve que le champ minuscule
        assertEquals(1, index.usages("client").size());
        // un nom inconnu ne trouve rien
        assertTrue(index.usages("Inconnu").isEmpty());
    }

    /** Le XML aussi : les balises nommées de l'arbre XML sont des symboles. */
    @Test
    void leXmlAussi() {
        IndexSymboles index = IndexSymboles.vide();
        String manifeste = """
                <manifest>
                  <application label="App"/>
                  <activity name="Main"/>
                </manifest>
                """;
        Document doc = Document.of(manifeste);
        index.remplacer("file:///AndroidManifest.xml", doc, xml.analyseFull(doc));
        List<String> noms = index.symboles().stream().map(Symbole::nom).toList();
        assertEquals(List.of("manifest", "application", "activity"), noms);
        // le nom de balise compte à l'ouverture et à la fermeture
        assertEquals(2, index.usages("manifest").size());
        assertEquals(1, index.usages("application").size());
    }

    /** La recherche floue (B-13) : l'aller à symbole dans l'ordre des scores. */
    @Test
    void laRechercheFloue() {
        IndexSymboles index = IndexSymboles.vide();
        indexJava(index, "file:///Client.java", sourceClient());
        indexJava(index, "file:///Utilisateur.java", sourceUtilisateur());
        List<String> trouves = index.rechercher("cl").stream()
                .map(Symbole::nom).distinct().toList();
        // « cl » ouvre Client et client en préfixe, nomComplet en sous-suite ;
        // les autres noms ne répondent pas
        assertTrue(trouves.contains("Client"));
        assertEquals("Client", trouves.get(0), "le meilleur score d'abord");
        // la requête exacte : les identiques d'abord, la casse à l'ordre près
        List<String> exacte = index.rechercher("client").stream()
                .map(Symbole::nom).toList();
        assertEquals(List.of("Client", "client"), exacte);
        // requête muette : tout l'index
        assertEquals(index.symboles().size(), index.rechercher("").size());
    }

    /** Le budget mémoire (ADR-005) : les occurrences sont bornées, la saturation est signée. */
    @Test
    void leBudgetDEntrees() {
        IndexSymboles index = IndexSymboles.vide(8);
        indexJava(index, "file:///Client.java", sourceClient());
        // le fichier compte plus d'occurrences que le budget : il sature, l'index tient
        assertTrue(index.sature("file:///Client.java"));
        assertTrue(index.nombreOccurrences() <= 8);
        // ce qui est entré reste juste
        assertTrue(!index.usages("Client").isEmpty());
        // un second fichier entre dans ce qui reste — ou sature à son tour, sans dépasser
        indexJava(index, "file:///Utilisateur.java", sourceUtilisateur());
        assertTrue(index.nombreOccurrences() <= 8);
        assertTrue(index.sature("file:///Utilisateur.java"));
    }

    /** La persistance : la forme sérialisée compacte refait l'index identique. */
    @Test
    void laPersistance() {
        IndexSymboles index = IndexSymboles.vide();
        indexJava(index, "file:///Client.java", sourceClient());
        indexJava(index, "file:///Utilisateur.java", sourceUtilisateur());
        String forme = index.serialiser();
        IndexSymboles relu = IndexSymboles.deserialiser(forme);
        assertEquals(index.symboles(), relu.symboles());
        assertEquals(index.usages("Client"), relu.usages("Client"));
        assertEquals(index.nombreOccurrences(), relu.nombreOccurrences());
        assertTrue(!relu.sature("file:///Client.java"));
        // la forme est compacte : une entrée par ligne, pas de JSON
        assertTrue(forme.lines().count() < 25);
        assertTrue(forme.startsWith("# celeris index symboles 1"));
    }

    /** La désérialisation d'une forme inconnue : refusée clairement. */
    @Test
    void laFormeInconnueEstRefusee() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> IndexSymboles.deserialiser("# n'importe quoi"));
    }
}
