package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;

/**
 * La complétion ne se tait jamais (la revue du propriétaire du
 * 2026-11-19 : « j'ai tapé des mots, aucune suggestion, le popups ne
 * s'affiche pas ») : l'analyse à jour de la révision courante
 * n'arrive JAMAIS à la frappe (chaque caractère change la révision,
 * la publication la suit de loin) — la complétion des mots réservés
 * du langage vit SANS analyse, les mots du document s'y joignent
 * quand une publication (même périmée) existe.
 */
class CompletionMotsSansAnalyseTest {

    /** Aucune analyse jamais publiée : les mots réservés du langage
     *  se proposent seuls — jamais le silence. */
    @Test
    void lesMotsReservesViventSansAnalyse() {
        Document document = Document.of("cla\n");
        List<String> candidats = CompletionMots.completer(document,
                null, 3, List.of("class", "void", "public"));
        assertTrue(candidats.contains("class"),
                "class se propose sans analyse");
        assertFalse(candidats.contains("void"),
                "void ne passe pas le préfixe cla");
        assertFalse(candidats.contains("cla"),
                "le mot tapé ne se propose pas à lui-même");
    }

    /** Le préfixe filtre : frapper « cla » ne propose que class —
     *  la complétion de base reste utile sans analyse. */
    @Test
    void lePrefixeFiltreLesMotsReserves() {
        Document document = Document.of("cla\n");
        List<String> candidats = CompletionMots.completer(document,
                null, 3, List.of("class", "void", "public", "clamp"));
        assertTrue(candidats.contains("class"),
                "class passe le préfixe cla");
        assertTrue(candidats.contains("clamp"),
                "clamp passe le préfixe cla");
        assertFalse(candidats.contains("void"),
                "void ne passe pas le préfixe cla");
    }
}
