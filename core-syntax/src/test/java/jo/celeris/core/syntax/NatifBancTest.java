package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.TextSession;

/**
 * Le banc du niveau 2 (ADR-020 — le signal du propriétaire : « tu dois
 * lancer le chantier tree sitter ») : la liaison native analyse le
 * document ENTIER — jetons exacts (les feuilles soudées, la convention
 * du socle), profondeurs de crochets, chunks du pli exact, re-parse
 * incrémental à l'édition. Le banc vit sous {@code assumeTrue} : sans
 * la bibliothèque partagée (tâche {@code natif}, hors de {@code
 * check}), il se saute — la voie retombe sur le repli des paquets,
 * aucun consommateur ne change.
 */
class NatifBancTest {

    /** La fabrique rend-elle la source ? — le saut du banc sans elle. */
    private static SyntaxeNatif source(int langue) {
        SyntaxeNatif source = switch (langue) {
            case 0 -> SyntaxeNatif.json();
            case 1 -> SyntaxeNatif.python();
            default -> SyntaxeNatif.javascript();
        };
        assumeTrue(source != null);
        return source;
    }

    /**
     * Le lexique exact de json : les feuilles soudées — la chaîne
     * « cle » redevient UN jeton du socle — les crochets appariables,
     * les nombres, les mots réservés.
     */
    @Test
    void jsonPorteSonLexiqueExact() {
        SyntaxeNatif json = source(0);
        TextSession session = TextSession.of("{\"cle\": [1, true, null]}",
                () -> 0L);
        MoteurNatif moteur = MoteurNatif.sur(session, json, null);
        assumeTrue(moteur != null);
        moteur.reanalyser();
        Analysis analyse = moteur.analysePubliee();
        assertNotNull(analyse);
        assertEquals(1, analyse.lex().lineCount());
        assertEquals(0, analyse.lex().bracketDepthAtLineStart(0));
        Token[] jetons = analyse.lex().line(0).tokens();
        assertEquals(11, jetons.length);
        assertEquals(TokenKind.OPEN_BRACKET, jetons[0].kind());
        // « cle » : les guillemets de la grammaire soudés au contenu
        assertEquals(TokenKind.STRING, jetons[1].kind());
        assertEquals(1, jetons[1].start());
        assertEquals(6, jetons[1].end());
        assertEquals(TokenKind.PUNCTUATION, jetons[2].kind());
        assertEquals(TokenKind.OPEN_BRACKET, jetons[3].kind());
        assertEquals(TokenKind.NUMBER, jetons[4].kind());
        assertEquals(TokenKind.KEYWORD, jetons[6].kind());
        assertEquals(TokenKind.KEYWORD, jetons[8].kind());
        assertEquals(TokenKind.CLOSE_BRACKET, jetons[9].kind());
        assertEquals(TokenKind.CLOSE_BRACKET, jetons[10].kind());
        assertEquals(24, jetons[10].end());
        moteur.close();
    }

    /**
     * Le pli exact de python : le module entier porte un chunk, la
     * docstring multi-lignes s'y tranche ligne par ligne — le pli de
     * la gouttière se sert du MÊME arbre que la coloration.
     */
    @Test
    void pythonPorteSonPliEtSaDocstring() {
        SyntaxeNatif python = source(1);
        TextSession session = TextSession.of(
                "def f():\n    \"\"\"doc\n    suite\n    \"\"\"\n    return 1\n",
                () -> 0L);
        MoteurNatif moteur = MoteurNatif.sur(session, python, null);
        assumeTrue(moteur != null);
        moteur.reanalyser();
        Analysis analyse = moteur.analysePubliee();
        assertNotNull(analyse);
        assertEquals(6, analyse.lex().lineCount());
        Token[] tete = analyse.lex().line(0).tokens();
        assertEquals(TokenKind.KEYWORD, tete[0].kind());
        assertEquals(TokenKind.IDENTIFIER, tete[1].kind());
        // la docstring : tranchée à SA ligne, l'encre chaîne partout —
        // le blanc de tête n'est à personne, la convention du socle
        Token[] doc = analyse.lex().line(1).tokens();
        assertEquals(1, doc.length);
        assertEquals(TokenKind.STRING, doc[0].kind());
        assertEquals(4, doc[0].start());
        assertEquals(10, doc[0].end());
        // le chunk du module : le pli exact du niveau 2
        List<Chunk> chunks = analyse.tree().chunks();
        assertEquals(1, chunks.size());
        assertEquals(0, chunks.get(0).startLine());
        assertEquals(4, chunks.get(0).endLine());
        assertTrue(analyse.estComplet());
        moteur.close();
    }

    /**
     * Le re-parse incrémental à l'édition : l'arbre précédent sert, la
     * révision suit, les jetons de la ligne touchée se matérialisent à
     * l'état neuf — les lignes épargnées gardent leurs paniers.
     */
    @Test
    void leditionReparseIncrementalement() {
        SyntaxeNatif python = source(1);
        TextSession session = TextSession.of(
                "def f():\n    return 1\n", () -> 0L);
        MoteurNatif moteur = MoteurNatif.sur(session, python, null);
        assumeTrue(moteur != null);
        moteur.reanalyser();
        assertNotNull(moteur.analysePubliee());
        assertEquals(0, moteur.analysePubliee().revision());
        assertTrue(session.replaceRange(21, 21, " + 2"));
        moteur.reanalyser();
        Analysis apres = moteur.analysePubliee();
        assertNotNull(apres);
        assertEquals(1, apres.revision());
        Token[] jetons = apres.lex().line(1).tokens();
        // « return 1 + 2 » : l'opérateur et le second nombre naissent,
        // les blancs soudés au jeton qui les suit (la convention)
        assertEquals(TokenKind.KEYWORD, jetons[0].kind());
        assertEquals(4, jetons[0].start());
        assertEquals(TokenKind.NUMBER, jetons[1].kind());
        assertEquals(TokenKind.OPERATOR, jetons[2].kind());
        assertEquals(TokenKind.NUMBER, jetons[3].kind());
        assertEquals(14, jetons[3].start());
        assertEquals(16, jetons[3].end());
        moteur.close();
    }

    /**
     * La case déclarée reste vide sans bibliothèque — la fabrique rend
     * null, le moteur aussi : le repli des paquets sert, jamais la voie
     * ne devine (le banc saute d'un geste quand la bibliothèque vit).
     */
    @Test
    void sansBibliothequeLaFabriqueRendNull() {
        assumeTrue(!LiaisonArbre.presente());
        assumeTrue(SyntaxeNatif.json() == null);
    }
}
