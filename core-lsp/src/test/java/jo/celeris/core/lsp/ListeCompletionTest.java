package jo.celeris.core.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * La liste de complétion du protocole (B-04) : les éléments du serveur
 * dans l'ordre du serveur, la coupure honorée. Le modèle ne réordonne
 * jamais une liste tronquée et ne la complète jamais localement.
 *
 * <p>PIEGE-17 (hérité du dépôt de départ) : une liste de complétion
 * tronquée par le serveur doit être honorée telle quelle — jamais
 * réordonnée ni complétée localement au-delà de la coupure. Le panneau
 * de départ fusionnait et triait ce que le serveur avait daigné envoyer,
 * gommant la priorité du serveur et inventant des candidats.</p>
 */
class ListeCompletionTest {

    /** La liste factice tronquée : isIncomplete vrai, ordre non alphabétique assumé. */
    private static String listeTronquee() {
        return "{\"isIncomplete\": true, \"items\": ["
                + "{\"label\": \"zebra\", \"kind\": 3},"
                + "{\"label\": \"apple\", \"kind\": 1},"
                + "{\"label\": \"mango\", \"kind\": 2}]}";
    }

    /**
     * PIEGE-17 — la liste tronquée est honorée telle quelle : l'ordre du
     * serveur survit à l'analyse, au filtrage et à toute demande de tri.
     */
    @Test
    void truncatedCompletionList_isNeverReordered() {
        ListeCompletion liste = ListeCompletion.de(JsonLecteur.lire(listeTronquee()));
        assertTrue(liste.estIncomplete());
        // l'ordre du serveur survit à l'analyse — zebra avant apple, par choix du serveur
        assertEquals(List.of("zebra", "apple", "mango"), libelles(liste));
        // le filtrage garde l'ordre relatif du serveur, il ne trie pas
        assertEquals(List.of("apple"), libelles(liste.filtrer("a")));
        assertEquals(List.of("zebra", "apple", "mango"), libelles(liste.filtrer("")));
        // le tri local est REFUSÉ sur une liste tronquée : la même liste revient
        assertEquals(List.of("zebra", "apple", "mango"), libelles(liste.trier()));
        // jamais de complétion locale au-delà de la coupure : le filtre ne
        // fait qu'ôter, la taille ne croît jamais
        assertTrue(liste.filtrer("a").elements().size() <= liste.elements().size());
    }

    /** La liste complète (tableau nu ou isIncomplete faux) : l'ordre reste, le tri est permis. */
    @Test
    void laListeCompleteAdmetLeTri() {
        ListeCompletion nue = ListeCompletion.de(JsonLecteur.lire(
                "[{\"label\": \"b\", \"kind\": 1}, {\"label\": \"a\", \"kind\": 1}]"));
        assertTrue(!nue.estIncomplete());
        assertEquals(List.of("b", "a"), libelles(nue));
        assertEquals(List.of("a", "b"), libelles(nue.trier()));
        ListeCompletion objet = ListeCompletion.de(JsonLecteur.lire(
                "{\"isIncomplete\": false, \"items\": ["
                + "{\"label\": \"b\"}, {\"label\": \"a\"}]}"));
        assertTrue(!objet.estIncomplete());
        assertEquals(List.of("a", "b"), libelles(objet.trier()));
    }

    /** Les champs de l'élément : libellé, genre, détail, documentation, textes de tri et filtre. */
    @Test
    void lesChampsDeLElement() {
        ListeCompletion liste = ListeCompletion.de(JsonLecteur.lire(
                "{\"items\": [{\"label\": \"suggestion\", \"kind\": 2, "
                + "\"detail\": \"(String)\", \"documentation\": \"la doc\", "
                + "\"sortText\": \"001\", \"filterText\": \"sugg\", "
                + "\"insertText\": \"suggestion()\", \"deprecated\": true}]}"));
        ElementCompletion element = liste.elements().get(0);
        assertEquals("suggestion", element.libelle());
        assertEquals(2, element.genre());
        assertEquals("(String)", element.detail());
        assertEquals("la doc", element.documentation());
        assertEquals("001", element.texteTri());
        assertEquals("sugg", element.texteFiltre());
        assertEquals("suggestion()", element.texteInsertion());
        assertTrue(element.obsolete());
        ElementCompletion nud = ListeCompletion.de(JsonLecteur.lire(
                "[{\"label\": \"x\"}]")).elements().get(0);
        assertEquals(0, nud.genre());
        assertNull(nud.detail());
        assertNull(nud.documentation());
        assertNull(nud.texteTri());
        assertNull(nud.texteFiltre());
        assertTrue(!nud.obsolete());
    }

    /** Le filtrage : insensible à la casse, sur le texte de filtre quand le serveur le donne. */
    @Test
    void leFiltrage() {
        ListeCompletion liste = ListeCompletion.de(JsonLecteur.lire(
                "{\"items\": [{\"label\": \"Alpha\"}, {\"label\": \"beta\"},"
                + " {\"label\": \"Gamma\", \"filterText\": \"carotte\"}]}"));
        assertEquals(List.of("Alpha"), libelles(liste.filtrer("ALP")));
        assertEquals(List.of("beta"), libelles(liste.filtrer("be")));
        // le texte de filtre prime sur le libellé : « carotte » attrape Gamma
        assertEquals(List.of("Gamma"), libelles(liste.filtrer("car")));
        assertTrue(liste.filtrer("zz").elements().isEmpty());
    }

    /**
     * La documentation dans ses trois formes du protocole (le
     * redressement du terrain réel, M8.6 : le serveur tiers retiré sert
     * du CONTENU BALISÉ) : chaîne nue, objet {@code {kind, value}} —
     * la valeur s'en tire —, tableau de contenus dont les valeurs se
     * joignent.
     */
    @Test
    void laDocumentationDansSesTroisFormes() {
        assertEquals("la doc", ElementCompletion.documentationDe(
                JsonLecteur.lire("\"la doc\"")));
        assertEquals("Un exemple nommé.", ElementCompletion.documentationDe(
                JsonLecteur.lire("{\"kind\": \"plaintext\", "
                        + "\"value\": \"Un exemple nommé.\"}")));
        assertEquals("première\nseconde",
                ElementCompletion.documentationDe(JsonLecteur.lire(
                        "[{\"value\": \"première\"}, "
                                + "{\"language\": \"xml\", "
                                + "\"value\": \"seconde\"}]")));
        assertNull(ElementCompletion.documentationDe(
                JsonLecteur.lire("[]")));
        assertNull(ElementCompletion.documentationDe(null));
    }

    /** La liste vide : tableau vide, objet sans items, items null — trois formes, un vide. */
    @Test
    void laListeVide() {
        assertTrue(ListeCompletion.de(JsonLecteur.lire("[]")).elements().isEmpty());
        assertTrue(ListeCompletion.de(JsonLecteur.lire("{}")).elements().isEmpty());
        assertTrue(ListeCompletion.de(JsonLecteur.lire(
                "{\"isIncomplete\": false, \"items\": null}")).elements().isEmpty());
    }

    /** Aide : les libellés dans l'ordre. */
    private static List<String> libelles(ListeCompletion liste) {
        return liste.elements().stream().map(ElementCompletion::libelle).toList();
    }
}
