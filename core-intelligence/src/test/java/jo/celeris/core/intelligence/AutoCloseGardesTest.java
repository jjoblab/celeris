package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.TextSession;

import org.junit.jupiter.api.Test;

/**
 * Les gardes de l'auto-close (C3, l'analyse croisée) : le dépôt de
 * départ n'auto-ferme pas toujours — au sein d'une chaîne ou d'un
 * commentaire (le délimiteur y est du texte), et sur un document
 * dont la balance de crochets dépasse un seuil (le suivi de paires
 * coûte trop cher). Celeris auto-fermait TOUJOURS.
 */
class AutoCloseGardesTest {

    private final TypingRules rules = TypingRules.generic();
    /** La citation double, en constante : le rempart de langue strip
     *  mal le litteral de char guillemet. */
    private static final char CITATION = (char) 34;

    private String apresFrappe(String texte, int caret, char frappe) {
        Document doc = Document.of(texte);
        java.util.Optional<EditPlan> plan = SmartTyping.insert(doc,
                MultiSelection.caretsAt(caret), String.valueOf(frappe), rules);
        assertTrue(plan.isPresent(),
                "le plan existe toujours (insertion simple au pire)");
        TextSession session = TextSession.of(texte, () -> 0L);
        session.apply(plan.get().transaction(), plan.get().selection());
        return session.text();
    }

    /** La paire vit au milieu d'un mot comme après — l'insertion
     *  simple ne s'invite pas hors littéral (le garde identifiant du
     *  dépôt de départ ne survit pas au milieu de mot : la paire y
     *  reste utile). */
    @Test
    void autoCloseApresUnIdentifiant() {
        assertEquals("foo()", apresFrappe("foo", 3, '('),
                "la frappe « ( » après « foo » ouvre sa paire");
        assertEquals("a()b", apresFrappe("ab", 1, '('),
                "la frappe « ( » au milieu d'un mot ouvre sa paire");
    }

    /** Au sein d'une chaîne : PAS de paire — le délimiteur y est du
     *  texte, pas de la structure. */
    @Test
    void pasDAutoCloseDansUneChaine() {
        // le caret vit entre a et b, DANS la chaîne (guillemet ouvert en 11)
        assertEquals("String s = \"a(b\";",
                apresFrappe("String s = \"ab\";", 13, '('),
                "la frappe dans la chaine passe en insertion simple");
    }

    /** Au sein d'un commentaire de ligne : PAS de paire. */
    @Test
    void pasDAutoCloseDansUnCommentaire() {
        assertEquals("// note ((importante)",
                apresFrappe("// note (importante)", 8, '('),
                "la frappe dans le commentaire passe en insertion simple");
    }

    /** La citation dans une chaîne : PAS de dédoublement — le
     *  guillemet intérieur s'échappe au clavier, l'auto-close n'en
     *  décide pas. */
    @Test
    void pasDeDedoublementDansUneChaine() {
        // le caret vit entre a et la fermante, DANS la chaîne
        // (guillemet ouvert en 11) : la citation s'insère simplement
        assertEquals("String s = \"a\"\";",
                apresFrappe("String s = \"a\";", 13, CITATION),
                "la citation dans la chaine passe en insertion simple");
    }

    /** La balance du document : un texte de plus de 50 000 crochets
     *  non appariés ne suit plus ses paires — l'auto-close rend la
     *  main, la frappe reste simple. */
    @Test
    void balanceMonstreRendLaMain() {
        StringBuilder monstre = new StringBuilder();
        for (int i = 0; i < 60_000; i++) {
            monstre.append('(');
        }
        // au-delà du seuil : insertion simple, pas de paire
        String apres = apresFrappe(monstre.toString(), 60_000, '(');
        assertEquals(60_001, apres.length());
        assertTrue(apres.indexOf(')') < 0,
                "aucune fermante n'a été ajoutée");
    }

    /** Le fermant sous le caret se saute toujours — la garde ne
     *  bloque pas le saut. */
    @Test
    void leFermantSeSauteToujours() {
        TextSession session = TextSession.of("()", () -> 0L);
        Document doc = session.document();
        java.util.Optional<EditPlan> plan = SmartTyping.insert(doc,
                MultiSelection.caretsAt(1), ")", rules);
        assertTrue(plan.isPresent());
        session.apply(plan.get().transaction(), plan.get().selection());
        assertEquals("()", session.text());
        assertEquals(2, session.selection().carets().get(0));
    }
}
