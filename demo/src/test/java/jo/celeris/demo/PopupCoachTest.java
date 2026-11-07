package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.OutlineEntry;
import jo.celeris.core.text.TextSession;

import org.junit.jupiter.api.Test;

/**
 * Les décisions de contenu des popups (M7.5, §7, reprise) : la
 * complétion filtre par le préfixe tapé sans se proposer le mot sous
 * le curseur, la signature compte les paramètres à plat dans l'appel
 * ouvert le plus proche, la documentation sert la table du serveur
 * factice, le fil d'Ariane descend le plan. Pur, sans appareil.
 */
class PopupCoachTest {

    private static final String DOCUMENT = """
            int ajouter(int a, int b) {
                return a + b;
            }
            int mesurer(String unite, float valeur, boolean arrondir) {
                return ajouter(1, 2);
            }
            int total = ajouter(1, 2);
            """;

    /** La complétion filtre par le préfixe, jamais le mot sous le curseur. */
    @Test
    void completionFiltersByPrefix() {
        TextSession session = TextSession.of(DOCUMENT, () -> 0L);
        // « aj » avant le curseur : les deux fonctions et rien d'autre.
        int curseur = DOCUMENT.indexOf("ajouter(") + 2;
        List<String> candidats = PopupCoach.completionFor(
                session.document(), curseur);
        assertTrue(candidats.contains("ajouter"),
                "ajouter est candidat de son propre préfixe");
        assertTrue(candidats.size() <= PopupCoach.MAX_CANDIDATS,
                "la liste reste capée");
        // Sans préfixe tapé : rien à proposer.
        int finDeLigne = DOCUMENT.indexOf('\n');
        assertTrue(PopupCoach.completionFor(session.document(),
                finDeLigne).isEmpty(),
                "sans préfixe, la complétion ne propose rien");
    }

    /** La signature compte les paramètres de l'appel ouvert. */
    @Test
    void signatureCountsParametersFlat() {
        TextSession session = TextSession.of(DOCUMENT, () -> 0L);
        // Dans l'appel « ajouter(1, 2) » après la première virgule :
        // le paramètre COURANT est le deuxième.
        int apresVirgule = DOCUMENT.lastIndexOf("ajouter(1,") + 11;
        var signature = PopupCoach.signatureFor(session.document(),
                apresVirgule);
        assertNotNull(signature, "l'appel ouvert rend sa signature");
        assertEquals(1, signature.parametreCourant(),
                "après la première virgule : le deuxième paramètre");
        // Hors de tout appel : rien.
        assertNull(PopupCoach.signatureFor(session.document(), 3),
                "hors appel, pas de signature");
    }

    /** La documentation sert la table — connue ou inconnue. */
    @Test
    void docServesTheFakeTable() {
        assertNotNull(PopupCoach.docFor("ajouter"),
                "ajouter est documenté");
        assertNull(PopupCoach.docFor("inconnu"),
                "le serveur factice n'invente rien");
        assertEquals("ajouter", PopupCoach.docFor("ajouter").titre(),
                "le titre est le symbole, jamais fondu (PIEGE-16)");
    }

    /** Le fil d'Ariane englobe le curseur, externe vers interne. */
    @Test
    void pathGoesOuterToInner() {
        OutlineEntry externe = new OutlineEntry(NodeCategory.FUNCTION,
                "mesurer", 0, 100, 0, 4);
        OutlineEntry interne = new OutlineEntry(NodeCategory.FIELD,
                "total", 50, 60, 2, 2);
        List<String> chemin = PopupCoach.pathFor(
                List.of(externe, interne), 55);
        assertEquals(List.of("mesurer", "total"), chemin,
                "la plus externe d'abord, la plus interne en dernier");
        assertTrue(PopupCoach.pathFor(List.of(externe, interne), 150)
                .isEmpty(), "hors de tout symbole, le fil se vide");
    }
}
