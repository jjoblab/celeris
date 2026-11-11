package jo.celeris.core.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;
import jo.celeris.core.text.Transaction;

/**
 * La complétion postfixe (A-23) : l'expression devant le point se
 * consomme dans son gabarit — le critère de la matrice :
 * {@code expr.if} devient {@code if (expr) {}} sans matérialiser le
 * préfixe (le piège hérité du lot L0 — l'expression ne se duplique
 * jamais).
 */
class PostfixCompletionTest {

    /** A-23 — le critère de la matrice : une édition, une seule occurrence de l'expression. */
    @Test
    void leCritereDeLaMatrice() {
        String source = """
                class Achat {
                    void facture(int total) {
                        total.if
                    }
                }
                """;
        Document doc = Document.of(source);
        int caret = source.indexOf("total.if") + "total.if".length();
        PostfixCompletion.PostfixEdit geste = PostfixCompletion.expand(doc, caret);
        assertNotNull(geste, "le geste s'étend");
        TextSession session = TextSession.of(source, System::nanoTime);
        session.apply(Transaction.of(java.util.List.of(geste.edit())));
        String resultat = session.text();
        assertEquals("""
                class Achat {
                    void facture(int total) {
                        if (total) {}
                    }
                }
                """, resultat, "le gabarit remplace l'expression, le point et le motif");
        assertTrue(occurrences(resultat, "total") == 2,
                "l'expression ne se matérialise jamais en préfixe — paramètre et gabarit seuls");
        session.undo();
        assertEquals(source, session.text(), "le geste s'annule");
        assertEquals(source.indexOf("total.if") + "if (total) {".length(),
                geste.caret(), "le curseur vit entre les accolades");
    }

    /** A-23 — chaque motif consomme l'expression dans son gabarit. */
    @Test
    void lesMotifs() {
        assertEquals("while (pret) {}", etendu("pret.while"));
        assertEquals("for (var element : liste) {}", etendu("liste.for"));
        assertEquals("!actif", etendu("actif.not"));
        assertEquals("if (valeur == null) {}", etendu("valeur.null"));
        assertEquals("if (valeur != null) {}", etendu("valeur.notnull"));
        assertEquals("return total;", etendu("total.return"));
    }

    /** A-23 — l'expression composée : les appels appariés se remontent entiers. */
    @Test
    void lexpressionComposee() {
        assertEquals("if (listes.get(0)) {}", etendu("listes.get(0).if"));
        assertEquals("if (porteur.pret()) {}", etendu("porteur.pret().if"));
        assertEquals("!cache.cle(3).valide", etendu("cache.cle(3).valide.not"));
    }

    /** A-23 — le curseur : entre les accolades pour les blocs, à la fin pour les fins d'instruction. */
    @Test
    void leCurseur() {
        Document doc = Document.of("total.if");
        PostfixCompletion.PostfixEdit geste = PostfixCompletion.expand(doc, "total.if".length());
        assertNotNull(geste);
        assertEquals("if (total) {".length(), geste.caret(), "entre les accolades");

        doc = Document.of("total.return");
        geste = PostfixCompletion.expand(doc, "total.return".length());
        assertNotNull(geste);
        assertEquals("return total;".length(), geste.caret(), "à la fin");
    }

    /** A-23 — les refus : motif inconnu, mot sans point, expression vide, littérale ou mal terminée. */
    @Test
    void lesRefus() {
        assertNull(etend("total.boum"), "motif inconnu");
        assertNull(etend("totalif"), "mot sans point");
        assertNull(etend("total.i"), "mot incomplet");
        assertNull(etend(".if"), "point sans expression");
        assertNull(etend("123.if"), "expression littérale");
        assertNull(etend("a..if"), "expression mal terminée");
        assertNull(etend("total."), "sans motif");
    }

    /** A-23 — le curseur hors bornes refuse d'exception ; les motifs se listent. */
    @Test
    void lesBornesEtLesMotifs() {
        Document doc = Document.of("total.if");
        assertThrows(IllegalArgumentException.class,
                () -> PostfixCompletion.expand(doc, -1));
        assertThrows(IllegalArgumentException.class,
                () -> PostfixCompletion.expand(doc, doc.length() + 1));
        assertEquals(java.util.List.of("for", "if", "not", "notnull", "null", "return", "while"),
                PostfixCompletion.motifs());
    }

    /** Le geste étendu sur une source d'une ligne, rendu en texte. */
    private String etendu(String source) {
        Document doc = Document.of(source);
        PostfixCompletion.PostfixEdit geste =
                PostfixCompletion.expand(doc, source.length());
        assertNotNull(geste, "le geste doit s'étendre : " + source);
        return geste.edit().inserted();
    }

    /** Les occurrences d'un mot dans un texte. */
    private int occurrences(String texte, String mot) {
        int total = 0;
        int position = texte.indexOf(mot);
        while (position >= 0) {
            total++;
            position = texte.indexOf(mot, position + mot.length());
        }
        return total;
    }

    /** Le geste étendu, ou null sur refus. */
    private PostfixCompletion.@org.jspecify.annotations.Nullable PostfixEdit etend(
            String source) {
        return PostfixCompletion.expand(Document.of(source), source.length());
    }
}
