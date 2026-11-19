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

    private static final String TEXTE = """
            alpha beta
            gamma /* debut
            milieu du commentaire
            fin */ delta
            epsilon
            zeta
            """;

    private static Document document(String text) {
        return Document.of(text);
    }

    /** La lexicographie complète pose un panier et un état par ligne. */
    @Test
    void lexicographieCompletePaniersEtEtats() {
        IncrementalLexer moteur = new IncrementalLexer();
        LexResult resultat = moteur.lexFull(document(TEXTE), lexeur());
        LexDocument lex = resultat.lex();
        assertEquals(7, lex.lineCount(),
                "six lignes de contenu plus la ligne vide finale");
        assertEquals(0, resultat.firstLine());
        assertEquals(6, resultat.lastLine());
        assertSame(MiniLangage.State.NORMAL, lex.stateBefore(0));
        assertSame(MiniLangage.State.NORMAL, lex.stateBefore(1));
        assertSame(MiniLangage.State.COMMENTAIRE, lex.stateBefore(2));
        assertSame(MiniLangage.State.COMMENTAIRE, lex.stateBefore(3));
        assertSame(MiniLangage.State.NORMAL, lex.stateBefore(4));

        Token[] ligne1 = lex.line(1).tokens();
        assertEquals(2, ligne1.length);
        assertEquals(TokenKind.IDENTIFIER, ligne1[0].kind());
        assertEquals(0, ligne1[0].start());
        assertEquals(5, ligne1[0].end());
        assertEquals(TokenKind.COMMENT, ligne1[1].kind());
        assertEquals(6, ligne1[1].start());
        assertEquals(14, ligne1[1].end());
        assertSame(MiniLangage.State.COMMENTAIRE, lex.line(1).exitState());

        Token[] ligne3 = lex.line(3).tokens();
        assertEquals(2, ligne3.length);
        assertEquals(TokenKind.COMMENT, ligne3[0].kind());
        assertEquals(0, ligne3[0].start());
        assertEquals(6, ligne3[0].end(),
                "le commentaire couvre « fin */ » en entier");
        assertEquals(TokenKind.IDENTIFIER, ligne3[1].kind());
    }

    /** Une édition sur une seule ligne ne relit qu'elle, la suite est réutilisée. */
    @Test
    void editionUneLigneUneSeuleRelecture() {
        IncrementalLexer moteur = new IncrementalLexer();
        LexResult complet = moteur.lexFull(document(TEXTE), lexeur());
        int target = TEXTE.indexOf("delta");
        Document edite = document(TEXTE).replace(target, target + 5, "omega");
        LexResult diff = moteur.lexIncremental(edite, lexeur(), complet.lex(),
                EditSite.of(document(TEXTE), edite, target, target + 5, "omega"));
        assertEquals(3, diff.firstLine());
        assertEquals(3, diff.lastLine());
        for (int line = 0; line < 7; line++) {
            if (line == 4) {
                continue;
            }
            if (line == 3) {
                continue;
            }
            assertSame(complet.lex().line(line), diff.lex().line(line),
                    "la ligne " + line + " doit être réutilisée à l'identité");
        }
        assertSame(MiniLangage.State.NORMAL, diff.lex().line(3).exitState());
        Token[] remplacee = diff.lex().line(3).tokens();
        assertEquals(2, remplacee.length);
        assertEquals(TokenKind.IDENTIFIER, remplacee[1].kind());
        assertEquals("omega".length(), remplacee[1].end() - remplacee[1].start());
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
        assertEquals(9, diff.lex().lineCount(),
                "huit lignes de contenu plus la vide finale");
        assertSame(complet.lex().line(5), diff.lex().line(7),
                "zeta garde son panier après les deux lignes insérées");
        assertSame(complet.lex().line(3), diff.lex().line(3));
        assertEquals(MiniLangage.State.NORMAL, diff.lex().line(4).exitState());
    }

    /** Une suppression de lignes réaligne la suite vers le bas. */
    @Test
    void suppressionDeLignesRealigneLaSuite() {
        IncrementalLexer moteur = new IncrementalLexer();
        LexResult complet = moteur.lexFull(document(TEXTE), lexeur());
        int start = TEXTE.indexOf("gamma");
        int end = TEXTE.indexOf("epsilon");
        Document edite = document(TEXTE).replace(start, end, "");
        LexResult diff = moteur.lexIncremental(edite, lexeur(), complet.lex(),
                EditSite.of(document(TEXTE), edite, start, end, ""));
        assertEquals(4, diff.lex().lineCount());
        assertSame(complet.lex().line(0), diff.lex().line(0));
        assertArrayEquals(complet.lex().line(4).tokens(), diff.lex().line(1).tokens(),
                "la ligne de reprise se relit une fois, au contenu identique");
        assertSame(complet.lex().line(5), diff.lex().line(2));
        assertSame(complet.lex().line(6), diff.lex().line(3));
        assertSame(MiniLangage.State.NORMAL, diff.lex().line(0).exitState());
    }

    /** La profondeur de crochets au début de chaque ligne suit les éditions. */
    @Test
    void profondeurCrochetsSuivieEtDecalee() {
        IncrementalLexer moteur = new IncrementalLexer();
        String source = "a { b\nc } d\ne\nf\n";
        LexResult complet = moteur.lexFull(document(source), lexeur());
        assertEquals(0, complet.lex().bracketDepthAtLineStart(0));
        assertEquals(1, complet.lex().bracketDepthAtLineStart(1));
        assertEquals(0, complet.lex().bracketDepthAtLineStart(2));
        assertEquals(0, complet.lex().bracketDepthAtLineStart(3));

        int coupe = source.indexOf('e');
        Document edite = document(source).replace(coupe, coupe, "{ ");
        LexResult diff = moteur.lexIncremental(edite, lexeur(), complet.lex(),
                EditSite.of(document(source), edite, coupe, coupe, "{ "));
        assertEquals(0, diff.lex().bracketDepthAtLineStart(0));
        assertEquals(1, diff.lex().bracketDepthAtLineStart(1));
        assertEquals(0, diff.lex().bracketDepthAtLineStart(2));
        assertEquals(1, diff.lex().bracketDepthAtLineStart(3),
                "l'ouvrant inséré décale la profondeur de la suite");

        Document edite2 = document(source).replace(coupe, coupe + 1, "f");
        LexResult diff2 = moteur.lexIncremental(edite2, lexeur(), complet.lex(),
                EditSite.of(document(source), edite2, coupe, coupe + 1, "f"));
        assertEquals(0, diff2.lex().bracketDepthAtLineStart(3),
                "une édition sans crochet ne décale rien");
        assertSame(complet.lex().line(3), diff2.lex().line(3));
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
        for (int line = 0; line < temoin.lex().lineCount(); line++) {
            assertArrayEquals(temoin.lex().line(line).tokens(),
                    diff.lex().line(line).tokens(), "panier de la ligne " + line);
            assertEquals(temoin.lex().line(line).exitState(),
                    diff.lex().line(line).exitState(), "état de la ligne " + line);
            assertEquals(temoin.lex().bracketDepthAtLineStart(line),
                    diff.lex().bracketDepthAtLineStart(line), "profondeur ligne " + line);
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
        assertSame(MiniLangage.State.NORMAL, resultat.lex().line(0).exitState());
    }

    private static LanguageLexer lexeur() {
        return MiniLangage.langage().lexer();
    }
}
