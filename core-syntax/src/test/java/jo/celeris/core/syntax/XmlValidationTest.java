package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jo.celeris.core.text.Document;

import org.junit.jupiter.api.Test;

/**
 * Tests de la validation XML (C-03) : les imbrications invalides
 * rapportées avec leur plage exacte — la fermeture désassortie porte
 * sa propre plage, l'élément jamais fermé porte celle de son
 * ouverture. Les commentaires, sections CDATA, prologues et
 * auto-fermetures ne produisent jamais de diagnostic.
 */
final class XmlValidationTest {

    /** Le document bien imbriqué ne produit rien, profondeurs comprises. */
    @Test
    void documentBienImbriqueNeProduitRien() {
        String texte = "<?xml version=\"1.0\"?>\n"
                + "<catalogue>\n"
                + "  <!-- un </catalogue> dans un commentaire : ignoré -->\n"
                + "  <produit>\n"
                + "    <prix devise=\"EUR\">10</prix>\n"
                + "    <stock />\n"
                + "    <note><![CDATA[ du </produit> brut ]]></note>\n"
                + "  </produit>\n"
                + "</catalogue>";
        List<SyntaxIssue> problemes = XmlValidation.valider(Document.of(texte));
        assertTrue(problemes.isEmpty(), "le document valide ne produit rien : " + problemes);
    }

    /** La fermeture désassortie porte sa plage exacte et les deux noms. */
    @Test
    void fermetureDesassortieRapporteSaPlageExacte() {
        String texte = "<produit>\n  <prix>10</prix>\n</catalogue>";
        List<SyntaxIssue> problemes = XmlValidation.valider(Document.of(texte));
        assertEquals(1, problemes.size(), "une seule fermeture désassortie");
        SyntaxIssue probleme = problemes.get(0);
        assertEquals(texte.indexOf("</catalogue>"), probleme.startOffset(),
                "la plage commence à la fermeture fautive");
        assertEquals("</catalogue>".length(), probleme.length(),
                "la plage couvre la fermeture entière");
        assertTrue(probleme.message().contains("produit"), "le message nomme l'attendu : "
                + probleme.message());
        assertTrue(probleme.message().contains("catalogue"), "le message nomme le trouvé : "
                + probleme.message());
    }

    /** L'élément jamais fermé porte la plage de sa balise d'ouverture. */
    @Test
    void elementJamaisFermeRapporteSonOuverture() {
        String texte = "<liste>\n  <item>un\n</liste>";
        List<SyntaxIssue> problemes = XmlValidation.valider(Document.of(texte));
        assertEquals(1, problemes.size(), "l'élément jamais fermé, seul fautif");
        SyntaxIssue probleme = problemes.get(0);
        assertEquals(texte.indexOf("<item>"), probleme.startOffset(),
                "la plage commence à l'ouverture non fermée");
        assertEquals("<item>".length(), probleme.length());
        assertTrue(probleme.message().contains("item"), "le message nomme l'élément : "
                + probleme.message());
    }

    /** La fermeture sans aucune ouverture est signalée sur elle-même. */
    @Test
    void fermetureSansOuvertureEstSignalee() {
        String texte = "</seul>";
        List<SyntaxIssue> problemes = XmlValidation.valider(Document.of(texte));
        assertEquals(1, problemes.size());
        SyntaxIssue probleme = problemes.get(0);
        assertEquals(0, probleme.startOffset());
        assertTrue(probleme.message().contains("seul"), "le message nomme la fermeture : "
                + probleme.message());
    }

    /** La fermeture désassortie au milieu remonte jusqu'à l'assortie. */
    @Test
    void fermetureInterieureRefermeLesNiveauxTraverses() {
        String texte = "<a><b><c></c></a>";
        List<SyntaxIssue> problemes = XmlValidation.valider(Document.of(texte));
        assertEquals(1, problemes.size(), "la récupération referme b sans le signaler deux fois");
        assertEquals(texte.indexOf("</a>"), problemes.get(0).startOffset());
    }

    /** L'auto-fermeture ne compte pas comme ouverture en attente. */
    @Test
    void autoFermetureNeDemandePasDeFermeture() {
        List<SyntaxIssue> problemes = XmlValidation.valider(
                Document.of("<racine>\n  <image source=\"x\" />\n</racine>"));
        assertTrue(problemes.isEmpty(), "l'auto-fermeture ferme : " + problemes);
    }

    /** Les imbrications invalides multiples se rapportent toutes, dans l'ordre. */
    @Test
    void plusieursProblemesRapportesDansLOrdre() {
        String texte = "<a></x>\n<b></y>";
        List<SyntaxIssue> problemes = XmlValidation.valider(Document.of(texte));
        assertEquals(2, problemes.size());
        assertTrue(problemes.get(0).startOffset() < problemes.get(1).startOffset(),
                "les problèmes suivent l'ordre du source");
    }

    /** Le texte vide et le texte seul ne produisent rien. */
    @Test
    void texteSeulNeProduitRien() {
        assertTrue(XmlValidation.valider(Document.of("")).isEmpty());
        assertTrue(XmlValidation.valider(Document.of("du texte libre\nsur deux lignes"))
                .isEmpty());
    }
}
