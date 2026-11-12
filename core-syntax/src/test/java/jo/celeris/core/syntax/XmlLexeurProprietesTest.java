package jo.celeris.core.syntax;

import jo.celeris.core.syntax.xml.XmlSyntax;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import jo.celeris.core.text.Document;

/**
 * Propriété jqwik du lexeur XML (ADR-010) : après une édition quelconque du
 * corpus — insertion, suppression, remplacement, y compris dans les
 * commentaires, sections CDATA, attributs et balises à cheval sur les
 * lignes — le lexique incrémental égale la relecture complète, panier par
 * panier et état par état. Graine déterministe, rejouable à l'identique.
 */
class XmlLexeurProprietesTest {

    private static final String ALPHABET =
            "ab < > / \" = ! - ? & é \n <tag </tag ]] CDATA ";

    /** Toute édition du corpus XML mène au même lexique qu'une relecture. */
    @Property(tries = 120)
    void leIncrementalEgaleLeComplet(@ForAll long graine) {
        Random rng = new Random(graine);
        String texte = CorpusChargeur.exempleXml();
        LanguageLexer lexeur = XmlSyntax.INSTANCE.lexer();
        IncrementalLexer moteur = new IncrementalLexer();
        LexResult courant = moteur.lexFull(Document.of(texte), lexeur);

        for (int i = 0; i < 12; i++) {
            Document avant = Document.of(texte);
            int debut = rng.nextInt(avant.length() + 1);
            int fin = Math.min(debut + rng.nextInt(30), avant.length());
            String insertion = texteAleatoire(rng, 20);
            Document apres = avant.replace(debut, fin, insertion);
            EditSite site = EditSite.of(avant, apres, debut, fin, insertion);
            courant = moteur.lexIncremental(apres, lexeur, courant.lex(), site);
            LexResult temoin = moteur.lexFull(apres, lexeur);
            verifierEgalite(courant.lex(), temoin.lex(), graine);
            texte = apres.text();
        }
    }

    private static void verifierEgalite(LexDocument obtenu, LexDocument temoin, long graine) {
        assertEquals(temoin.lineCount(), obtenu.lineCount(), "lignes (graine " + graine + ")");
        for (int ligne = 0; ligne < temoin.lineCount(); ligne++) {
            assertArrayEquals(temoin.line(ligne).tokens(), obtenu.line(ligne).tokens(),
                    "paniers ligne " + ligne + " (graine " + graine + ")");
            assertEquals(temoin.line(ligne).exitState(), obtenu.line(ligne).exitState(),
                    "états ligne " + ligne + " (graine " + graine + ")");
        }
    }

    private static String texteAleatoire(Random rng, int longueur) {
        StringBuilder sb = new StringBuilder(longueur);
        for (int i = 0; i < longueur; i++) {
            sb.append(ALPHABET.charAt(rng.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
