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
        String texte = texteAleatoire(rng, 120);
        LanguageLexer lexeur = MiniLangage.langage().lexer();
        IncrementalLexer moteur = new IncrementalLexer();
        LexResult precedent = moteur.lexFull(Document.of(texte), lexeur);

        int nombreEditions = rng.nextInt(20) + 1;
        for (int i = 0; i < nombreEditions; i++) {
            Document avant = Document.of(texte);
            int debut = rng.nextInt(texte.length() + 1);
            int fin = Math.min(debut + rng.nextInt(9), texte.length());
            String insertion = texteAleatoire(rng, 12);
            Document apres = avant.replace(debut, fin, insertion);
            EditSite site = EditSite.of(avant, apres, debut, fin, insertion);

            LexResult diff = moteur.lexIncremental(apres, lexeur, precedent.lex(), site);
            LexResult temoin = moteur.lexFull(apres, lexeur);
            verifierEgalite(diff.lex(), temoin.lex(), graine);

            precedent = diff;
            texte = apres.text();
        }
    }

    /** Des éditions successives gardent l'incrémental égal au complet. */
    @Property(tries = 120)
    void chainesDEditionsRestentCoherentes(@ForAll long graine) {
        Random rng = new Random(graine);
        StringBuilder texte = new StringBuilder(texteAleatoire(rng, 200));
        LanguageLexer lexeur = MiniLangage.langage().lexer();
        IncrementalLexer moteur = new IncrementalLexer();
        LexResult courant = moteur.lexFull(Document.of(texte.toString()), lexeur);

        for (int i = 0; i < 15; i++) {
            Document avant = Document.of(texte.toString());
            int debut = rng.nextInt(avant.length() + 1);
            int fin = Math.min(debut + rng.nextInt(20), avant.length());
            String insertion = texteAleatoire(rng, 15);
            Document apres = avant.replace(debut, fin, insertion);
            EditSite site = EditSite.of(avant, apres, debut, fin, insertion);
            courant = moteur.lexIncremental(apres, lexeur, courant.lex(), site);
            texte = new StringBuilder(apres.text());
            verifierEgalite(courant.lex(),
                    moteur.lexFull(apres, lexeur).lex(), graine);
        }
    }

    private static void verifierEgalite(LexDocument obtenu, LexDocument temoin, long graine) {
        assertEquals(temoin.lineCount(), obtenu.lineCount(),
                "nombre de lignes (graine " + graine + ")");
        for (int ligne = 0; ligne < temoin.lineCount(); ligne++) {
            assertArrayEquals(temoin.line(ligne).tokens(), obtenu.line(ligne).tokens(),
                    "paniers de la ligne " + ligne + " (graine " + graine + ")");
            assertEquals(temoin.line(ligne).exitState(), obtenu.line(ligne).exitState(),
                    "états de la ligne " + ligne + " (graine " + graine + ")");
            assertEquals(temoin.bracketDepthAtLineStart(ligne),
                    obtenu.bracketDepthAtLineStart(ligne),
                    "profondeurs de la ligne " + ligne + " (graine " + graine + ")");
        }
        assertEquals(temoin.stateBefore(0), obtenu.stateBefore(0),
                "état initial (graine " + graine + ")");
    }

    private static String texteAleatoire(Random rng, int longueur) {
        StringBuilder sb = new StringBuilder(longueur);
        for (int i = 0; i < longueur; i++) {
            sb.append(ALPHABET.charAt(rng.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
