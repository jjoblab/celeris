package jo.celeris.core.syntax.grammars;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import jo.celeris.core.syntax.EditSite;
import jo.celeris.core.syntax.IncrementalLexer;
import jo.celeris.core.syntax.LanguageLexer;
import jo.celeris.core.syntax.LanguageSyntax;
import jo.celeris.core.syntax.LexDocument;
import jo.celeris.core.syntax.LexResult;
import jo.celeris.core.text.Document;

/**
 * Propriété jqwik de la voie de grammaires (ADR-010) : après une édition
 * quelconque du corpus python — insertion, suppression, remplacement,
 * y compris dans les chaînes triples et les commentaires — le lexique
 * incrémental égale la relecture complète, panier par panier et état
 * par état. Graine déterministe, rejouable à l'identique.
 */
class GrammaireProprietesTest {

    private static final String ALPHABET =
            "ab \" \"\"\" ''' # ( ) [ ] { } : . 0 1 \n def class x = ";

    /** Toute édition du corpus à grammaire mène au même lexique qu'une relecture. */
    @Property(tries = 120)
    void leIncrementalEgaleLeComplet(@ForAll long graine) {
        Random rng = new Random(graine);
        String texte = corpusPython();
        LanguageLexer lexeur = syntaxeDePaquet("python").lexer();
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

    private static String corpusPython() {
        return """
                class Exemple:
                    def methode(self, valeur=1):
                        doc = \"\"\"
                        une chaine triple
                        qui traverse les lignes
                        \"\"\"
                        return valeur + 1  # borne

                x = Exemple()
                """;
    }

    /** La source syntaxique d'un paquet intégré, par la vraie fabrique. */
    static LanguageSyntax syntaxeDePaquet(String id) {
        var flux = GrammaireProprietesTest.class.getResourceAsStream("/langages/" + id + "/grammar.json");
        if (flux == null) {
            throw new IllegalArgumentException("grammaire du paquet " + id);
        }
        try (flux) {
            return PackGrammars.INSTANCE.compile(id, new String(flux.readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
