package jo.celeris.core.syntax;

import jo.celeris.core.syntax.clike.JavaSyntax;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import jo.celeris.core.text.Document;

/**
 * Propriété jqwik du lexeur Java (ADR-010) : après une édition quelconque
 * du corpus — insertion, suppression, remplacement, y compris dans les
 * commentaires, chaînes et textes en bloc — le lexique incrémental égale
 * la relecture complète, panier par panier et état par état. Graine
 * déterministe, rejouable à l'identique.
 */
class JavaLexeurProprietesTest {

    private static final String ALPHABET =
            "ab \" /* */ \"\"\" { } ( ) ; . 0 1 \n class int ";

    /** Toute édition du corpus Java mène au même lexique qu'une relecture. */
    @Property(tries = 120)
    void leIncrementalEgaleLeComplet(@ForAll long graine) {
        Random rng = new Random(graine);
        String text = CorpusChargeur.exempleJava();
        LanguageLexer lexeur = JavaSyntax.INSTANCE.lexer();
        IncrementalLexer moteur = new IncrementalLexer();
        LexResult courant = moteur.lexFull(Document.of(text), lexeur);

        for (int i = 0; i < 12; i++) {
            Document avant = Document.of(text);
            int start = rng.nextInt(avant.length() + 1);
            int end = Math.min(start + rng.nextInt(30), avant.length());
            String insertion = texteAleatoire(rng, 20);
            Document apres = avant.replace(start, end, insertion);
            EditSite site = EditSite.of(avant, apres, start, end, insertion);
            courant = moteur.lexIncremental(apres, lexeur, courant.lex(), site);
            LexResult temoin = moteur.lexFull(apres, lexeur);
            verifierEgalite(courant.lex(), temoin.lex(), graine);
            text = apres.text();
        }
    }

    private static void verifierEgalite(LexDocument obtenu, LexDocument temoin, long graine) {
        assertEquals(temoin.lineCount(), obtenu.lineCount(), "lignes (graine " + graine + ")");
        for (int line = 0; line < temoin.lineCount(); line++) {
            assertArrayEquals(temoin.line(line).tokens(), obtenu.line(line).tokens(),
                    "paniers ligne " + line + " (graine " + graine + ")");
            assertEquals(temoin.line(line).exitState(), obtenu.line(line).exitState(),
                    "états ligne " + line + " (graine " + graine + ")");
        }
    }

    private static String texteAleatoire(Random rng, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(rng.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
