package jo.celeris.core.lsp;

import jo.celeris.core.lsp.types.ElementCompletion;
import jo.celeris.core.lsp.types.ListeCompletion;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * La liste de complétion du domaine (B-04) : les éléments du serveur
 * dans l'ordre du serveur, la coupure honorée. Le modèle ne réordonne
 * jamais une liste tronquée et ne la complète jamais localement. La
 * lecture du fil — la coupure portée par l'objet liste du protocole —
 * vit à la frontière protocolaire (CorrespondanceLspTest, ADR-015) ;
 * les règles du domaine s'éprouvent ici, sur la fabrique typée.
 *
 * <p>PIEGE-17 (hérité du dépôt de départ) : une liste de complétion
 * tronquée par le serveur doit être honorée telle quelle — jamais
 * réordonnée ni complétée localement au-delà de la coupure. Le panneau
 * de départ fusionnait et triait ce que le serveur avait daigné envoyer,
 * gommant la priorité du serveur et inventant des candidats.</p>
 */
class ListeCompletionTest {

    /** La liste tronquée : isIncomplete vrai, ordre non alphabétique assumé. */
    private static ListeCompletion listeTronquee() {
        return ListeCompletion.de(true, List.of(
                new ElementCompletion("zebra", 3, null, null, null, null, null, false),
                new ElementCompletion("apple", 1, null, null, null, null, null, false),
                new ElementCompletion("mango", 2, null, null, null, null, null, false)));
    }

    /**
     * PIEGE-17 — la liste tronquée est honorée telle quelle : l'ordre du
     * serveur survit au filtrage et à toute demande de tri.
     */
    @Test
    void truncatedCompletionList_isNeverReordered() {
        ListeCompletion liste = listeTronquee();
        assertTrue(liste.estIncomplete());
        // l'ordre du serveur survit — zebra avant apple, par choix du serveur
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

    /** La liste complète : l'ordre reste, le tri est permis. */
    @Test
    void laListeCompleteAdmetLeTri() {
        ListeCompletion liste = ListeCompletion.de(false, List.of(
                new ElementCompletion("b", 1, null, null, null, null, null, false),
                new ElementCompletion("a", 1, null, null, null, null, null, false)));
        assertTrue(!liste.estIncomplete());
        assertEquals(List.of("b", "a"), libelles(liste));
        assertEquals(List.of("a", "b"), libelles(liste.trier()));
    }

    /** Les champs de l'élément : libellé, genre, détail, documentation, textes de tri et filtre. */
    @Test
    void lesChampsDeLElement() {
        ListeCompletion liste = ListeCompletion.de(false, List.of(
                new ElementCompletion("suggestion", 2, "(String)", "la doc",
                        "001", "sugg", "suggestion()", true)));
        ElementCompletion element = liste.elements().get(0);
        assertEquals("suggestion", element.libelle());
        assertEquals(2, element.genre());
        assertEquals("(String)", element.detail());
        assertEquals("la doc", element.documentation());
        assertEquals("001", element.texteTri());
        assertEquals("sugg", element.texteFiltre());
        assertEquals("suggestion()", element.texteInsertion());
        assertTrue(element.obsolete());
        // les champs absents sont nuls ou neutres, jamais devinés
        ElementCompletion nud = new ElementCompletion(
                "x", 0, null, null, null, null, null, false);
        assertEquals(0, nud.genre());
        assertTrue(!nud.obsolete());
    }

    /** Le tri : la clé de tri du serveur prime sur le libellé. */
    @Test
    void leTriSuitLaCleDuServeur() {
        ListeCompletion liste = ListeCompletion.de(false, List.of(
                new ElementCompletion("alpha", 1, null, null, "990", null, null, false),
                new ElementCompletion("zoulou", 1, null, null, "001", null, null, false)));
        assertEquals(List.of("zoulou", "alpha"), libelles(liste.trier()));
    }

    /** Le filtrage : insensible à la casse, sur le texte de filtre quand le serveur le donne. */
    @Test
    void leFiltrage() {
        ListeCompletion liste = ListeCompletion.de(false, List.of(
                new ElementCompletion("Alpha", 0, null, null, null, null, null, false),
                new ElementCompletion("beta", 0, null, null, null, null, null, false),
                new ElementCompletion("Gamma", 0, null, null, null, "carotte", null, false)));
        assertEquals(List.of("Alpha"), libelles(liste.filtrer("ALP")));
        assertEquals(List.of("beta"), libelles(liste.filtrer("be")));
        // le texte de filtre prime sur le libellé : « carotte » attrape Gamma
        assertEquals(List.of("Gamma"), libelles(liste.filtrer("car")));
        assertTrue(liste.filtrer("zz").elements().isEmpty());
    }

    /** La liste vide : aucun élément, les règles tiennent sur le vide. */
    @Test
    void laListeVide() {
        ListeCompletion vide = ListeCompletion.de(false, List.of());
        assertTrue(vide.elements().isEmpty());
        assertTrue(vide.filtrer("a").elements().isEmpty());
        assertTrue(vide.trier().elements().isEmpty());
    }

    /** Aide : les libellés dans l'ordre. */
    private static List<String> libelles(ListeCompletion liste) {
        return liste.elements().stream().map(ElementCompletion::libelle).toList();
    }
}
