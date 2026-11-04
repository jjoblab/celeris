package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;

/**
 * Le moteur de lexicographie incrémentale (ADR-004) : les paniers de jetons
 * vivent par ligne avec des offsets relatifs au début de ligne, les états se
 * transportent d'une ligne à la suivante, et une édition ne relit que les
 * lignes dont le contenu ou l'état peut changer — la suite du document se
 * réutilise à l'identité près.
 */
class LexiqueIncrementalTest {

    private static final String TEXTE = "alpha beta\n"
            + "gamma /* debut\n"
            + "milieu du commentaire\n"
            + "fin */ delta\n"
            + "epsilon\n"
            + "zeta\n";

    private static Document document(String texte) {
        return Document.of(texte);
    }

    /** La lexicographie complète pose un panier et un état par ligne. */
    @Test
    void lexicographieCompletePaniersEtEtats() {
        IncrementalLexer moteur = new IncrementalLexer();
        LexResult resultat = moteur.lexFull(document(TEXTE), lexeur());
        LexDocument lex = resultat.lex();
        assertEquals(6, lex.lineCount());
        assertEquals(0, resultat.firstLine());
        assertEquals(5, resultat.lastLine());
        assertSame(MiniLangage.Etat.NORMAL, lex.stateBefore(0));
        assertSame(MiniLangage.Etat.NORMAL, lex.stateBefore(1));
        assertSame(MiniLangage.Etat.COMMENTAIRE, lex.stateBefore(2));
        assertSame(MiniLangage.Etat.COMMENTAIRE, lex.stateBefore(3));
        assertSame(MiniLangage.Etat.NORMAL, lex.stateBefore(4));

        Token[] ligne1 = lex.line(1).tokens();
        assertEquals(2, ligne1.length);
        assertEquals(TokenKind.IDENTIFIER, ligne1[0].kind());
        assertEquals(0, ligne1[0].start());
        assertEquals(5, ligne1[0].end());
        assertEquals(TokenKind.COMMENT, ligne1[1].kind());
        assertEquals(6, ligne1[1].start());
        assertEquals(14, ligne1[1].end());
        assertSame(MiniLangage.Etat.COMMENTAIRE, lex.line(1).exitState());

        Token[] ligne3 = lex.line(3).tokens();
        assertEquals(2, ligne3.length);
        assertEquals(TokenKind.COMMENT, ligne3[0].kind());
        assertEquals(0, ligne3[0].start());
        assertEquals(5, ligne3[0].end());
        assertEquals(TokenKind.IDENTIFIER, ligne3[1].kind());
    }

    /** Une édition sur une seule ligne ne relit qu'elle, la suite est réutilisée. */
    @Test
    void editionUneLigneUneSeuleRelecture() {
        IncrementalLexer moteur = new IncrementalLexer();
        LexResult complet = moteur.lexFull(document(TEXTE), lexeur());
        Document edite = document(TEXTE).replace(28, 33, "omega");
        LexResult diff = moteur.lexIncremental(edite, lexeur(), complet.lex(),
                EditSite.of(document(TEXTE), edite, 28, 33, "omega"));
        assertEquals(4, diff.firstLine());
        assertEquals(4, diff.lastLine());
        for (int ligne = 0; ligne < 6; ligne++) {
            if (ligne == 4) {
                continue;
            }
            assertSame(complet.lex().line(ligne), diff.lex().line(ligne),
                    "la ligne " + ligne + " doit être réutilisée à l'identité");
        }
        assertSame(MiniLangage.Etat.NORMAL, diff.lex().line(4).exitState());
        Token[] remplacee = diff.lex().line(4).tokens();
        assertEquals(TokenKind.IDENTIFIER, remplacee[0].kind());
        assertEquals("omega".length(), remplacee[0].end() - remplacee[0].start());
    }

    /** Une insertion multi-lignes décale les paniers par le delta de lignes. */
    @Test
    void insertionMultilignesAligneLaSuite() {
        IncrementalLexer moteur = new IncrementalLexer();
        LexResult complet = moteur.lexFull(document(TEXTE), lexeur());
        int coupe = TEXTE.indexOf("epsilon");
        Document edite = document(TEXTE).replace(coupe, coupe, "nouveau\nencore\n");
        LexResult diff = moteur.lexIncremental(edite, lexeur(), complet.lex(),
                EditSite.of(document(TEXTE), edite, coupe, coupe, "nouveau\nencore\n"));
        assertEquals(2, EditSite.of(document(TEXTE), edite, coupe, coupe, "nouveau\nencore\n").lineDelta());
        assertEquals(8, diff.lex().lineCount());
        assertSame(complet.lex().line(5), diff.lex().line(7),
                "zeta garde son panier après les deux lignes insérées");
        assertSame(complet.lex().line(3), diff.lex().line(3));
        assertEquals(MiniLangage.Etat.NORMAL, diff.lex().line(4).exitState());
    }

    /** Une suppression de lignes réaligne la suite vers le bas. */
    @Test
    void suppressionDeLignesRealigneLaSuite() {
        IncrementalLexer moteur = new IncrementalLexer();
        LexResult complet = moteur.lexFull(document(TEXTE), lexeur());
        int debut = TEXTE.indexOf("gamma");
        int fin = TEXTE.indexOf("epsilon");
        Document edite = document(TEXTE).replace(debut, fin, "");
        LexResult diff = moteur.lexIncremental(edite, lexeur(), complet.lex(),
                EditSite.of(document(TEXTE), edite, debut, fin, ""));
        assertEquals(3, diff.lex().lineCount());
        assertSame(complet.lex().line(4), diff.lex().line(1));
        assertSame(complet.lex().line(5), diff.lex().line(2));
        assertSame(MiniLangage.Etat.NORMAL, diff.lex().line(0).exitState());
    }

    /** La profondeur de crochets au début de chaque ligne suit les éditions. */
    @Test
    void profondeurCrochetsSuivieEtDecalee() {
        IncrementalLexer moteur = new IncrementalLexer();
        String source = "a { b\n" + "c } d\n" + "e\n";
        LexResult complet = moteur.lexFull(document(source), lexeur());
        assertEquals(0, complet.lex().bracketDepthAtLineStart(0));
        assertEquals(1, complet.lex().bracketDepthAtLineStart(1));
        assertEquals(0, complet.lex().bracketDepthAtLineStart(2));

        int coupe = source.indexOf('e');
        Document edite = document(source).replace(coupe, coupe, "{ ");
        LexResult diff = moteur.lexIncremental(edite, lexeur(), complet.lex(),
                EditSite.of(document(source), edite, coupe, coupe, "{ "));
        assertEquals(1, diff.lex().bracketDepthAtLineStart(0));
        assertEquals(2, diff.lex().bracketDepthAtLineStart(1));
        assertEquals(1, diff.lex().bracketDepthAtLineStart(2));

        Document edite2 = document(source).replace(coupe, coupe, "z ");
        LexResult diff2 = moteur.lexIncremental(document(source), lexeur(),
                diff.lex(), EditSite.of(edite, document(source), coupe, coupe, "z "));
        assertEquals(0, diff2.lex().bracketDepthAtLineStart(2));
    }

    /** Le résultat incrémental est identique à une relecture complète. */
    @Test
    void incrementEgalRelectureComplete() {
        IncrementalLexer moteur = new IncrementalLexer();
        LexResult complet = moteur.lexFull(document(TEXTE), lexeur());
        int coupe = TEXTE.indexOf("milieu");
        Document edite = document(TEXTE).replace(coupe, coupe + 6, "X");
        LexResult diff = moteur.lexIncremental(edite, lexeur(), complet.lex(),
                EditSite.of(document(TEXTE), edite, coupe, coupe + 6, "X"));
        LexResult temoin = moteur.lexFull(edite, lexeur());
        assertEquals(temoin.lex().lineCount(), diff.lex().lineCount());
        for (int ligne = 0; ligne < temoin.lex().lineCount(); ligne++) {
            assertArrayEquals(temoin.lex().line(ligne).tokens(),
                    diff.lex().line(ligne).tokens(), "panier de la ligne " + ligne);
            assertEquals(temoin.lex().line(ligne).exitState(),
                    diff.lex().line(ligne).exitState(), "état de la ligne " + ligne);
            assertEquals(temoin.lex().bracketDepthAtLineStart(ligne),
                    diff.lex().bracketDepthAtLineStart(ligne), "profondeur ligne " + ligne);
        }
        assertTrue(diff.lastLine() < 6, "la cascade doit s'arrêter avant la fin");
    }

    /** Un document vide se lit sans incident. */
    @Test
    void documentVide() {
        IncrementalLexer moteur = new IncrementalLexer();
        LexResult resultat = moteur.lexFull(document(""), lexeur());
        assertEquals(1, resultat.lex().lineCount());
        assertEquals(0, resultat.lex().line(0).tokens().length);
        assertSame(MiniLangage.Etat.NORMAL, resultat.lex().line(0).exitState());
    }

    private static LanguageLexer lexeur() {
        return MiniLangage.langage().lexer();
    }
}
