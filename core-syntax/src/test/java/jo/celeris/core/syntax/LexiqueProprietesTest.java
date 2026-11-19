package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import jo.celeris.core.text.Document;

/**
 * Propriétés jqwik du moteur de lexicographie incrémentale (ADR-004,
 * ADR-010) : après une édition quelconque — insertion, suppression,
 * remplacement, avec commentaires, chaînes, crochets et sauts de ligne —
 * le résultat incrémental est égal, panier par panier et état par état, à
 * une relecture complète du document édité. L'oracle est la relecture
 * complète elle-même ; la randomisation est déterministe, rejouable à
 * l'identique à partir de la graine.
 */
class LexiqueProprietesTest {

    private static final String ALPHABET =
            "ab /* */ \" { } ( ) [ ] \n xy";

    /** Toute édition mène au même lexique qu'une relecture complète. */
    @Property(tries = 250)
    void leIncrementalEgaleLeComplet(@ForAll long graine) {
        Random rng = new Random(graine);
        String text = texteAleatoire(rng, 120);
        LanguageLexer lexeur = MiniLangage.langage().lexer();
        IncrementalLexer moteur = new IncrementalLexer();
        LexResult precedent = moteur.lexFull(Document.of(text), lexeur);

        int nombreEditions = rng.nextInt(20) + 1;
        for (int i = 0; i < nombreEditions; i++) {
            Document avant = Document.of(text);
            int start = rng.nextInt(text.length() + 1);
            int end = Math.min(start + rng.nextInt(9), text.length());
            String insertion = texteAleatoire(rng, 12);
            Document apres = avant.replace(start, end, insertion);
            EditSite site = EditSite.of(avant, apres, start, end, insertion);

            LexResult diff = moteur.lexIncremental(apres, lexeur, precedent.lex(), site);
            LexResult temoin = moteur.lexFull(apres, lexeur);
            verifierEgalite(diff.lex(), temoin.lex(), graine);

            precedent = diff;
            text = apres.text();
        }
    }

    /** Des éditions successives gardent l'incrémental égal au complet. */
    @Property(tries = 120)
    void chainesDEditionsRestentCoherentes(@ForAll long graine) {
        Random rng = new Random(graine);
        StringBuilder text = new StringBuilder(texteAleatoire(rng, 200));
        LanguageLexer lexeur = MiniLangage.langage().lexer();
        IncrementalLexer moteur = new IncrementalLexer();
        LexResult courant = moteur.lexFull(Document.of(text.toString()), lexeur);

        for (int i = 0; i < 15; i++) {
            Document avant = Document.of(text.toString());
            int start = rng.nextInt(avant.length() + 1);
            int end = Math.min(start + rng.nextInt(20), avant.length());
            String insertion = texteAleatoire(rng, 15);
            Document apres = avant.replace(start, end, insertion);
            EditSite site = EditSite.of(avant, apres, start, end, insertion);
            courant = moteur.lexIncremental(apres, lexeur, courant.lex(), site);
            text = new StringBuilder(apres.text());
            verifierEgalite(courant.lex(),
                    moteur.lexFull(apres, lexeur).lex(), graine);
        }
    }

    private static void verifierEgalite(LexDocument obtenu, LexDocument temoin, long graine) {
        assertEquals(temoin.lineCount(), obtenu.lineCount(),
                "nombre de lignes (graine " + graine + ")");
        for (int line = 0; line < temoin.lineCount(); line++) {
            assertArrayEquals(temoin.line(line).tokens(), obtenu.line(line).tokens(),
                    "paniers de la ligne " + line + " (graine " + graine + ")");
            assertEquals(temoin.line(line).exitState(), obtenu.line(line).exitState(),
                    "états de la ligne " + line + " (graine " + graine + ")");
            assertEquals(temoin.bracketDepthAtLineStart(line),
                    obtenu.bracketDepthAtLineStart(line),
                    "profondeurs de la ligne " + line + " (graine " + graine + ")");
        }
        assertEquals(temoin.stateBefore(0), obtenu.stateBefore(0),
                "état initial (graine " + graine + ")");
    }

    private static String texteAleatoire(Random rng, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(rng.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
