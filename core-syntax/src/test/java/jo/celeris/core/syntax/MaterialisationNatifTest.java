package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;

/**
 * Le banc de la matérialisation Java de l'arbre natif aplati (ADR-020) :
 * des arbres SYNTHÉTIQUES — le format exact de la liaison (six entiers
 * par nœud en préordre : le symbole, l'octet de début, l'octet de fin,
 * la ligne, la colonne d'octets, le compte d'enfants) — y nourrissent
 * {@code MaterialisationNatif.de} : les feuilles soudées redeviennent
 * les jetons du socle, les blancs au jeton qui suit, le pli se chunk
 * aux nœuds de premier niveau qui traversent les lignes, les octets de
 * la grammaire se traduisent en caractères du modèle. Le banc vit sans
 * la bibliothèque partagée — la matière est pure, l'arbre y entre
 * aplati.
 */
class MaterialisationNatifTest {

    /** Les noms de symboles du banc — les sorts s'y lisent. */
    private static final String[] SYMBOLES = {
        "{", "}", "[", "]", "(", ")", ":", ",", "+", ";", "\"", "'", "`",
        "string", "comment", "escape", "regex", "integer", "float",
        "number", "identifier", "decorator", "attribute", "def", "return",
        "true", "void", "inconnu"
    };

    private static final int OUVR_ACC = 0;
    private static final int FERM_ACC = 1;
    private static final int OUVR_CRO = 2;
    private static final int FERM_CRO = 3;
    private static final int DEUX_PTS = 6;
    private static final int VIRGULE = 7;
    private static final int PLUS = 8;
    private static final int POINT_VIRGULE = 9;
    private static final int CHAINE = 13;
    private static final int IDENT = 20;

    /** Un nœud aplati : [symbole, debut, fin, ligne, colonne, enfants]. */
    private static int[] noeud(int symbole, int debut, int fin, int enfants) {
        return new int[] {symbole, debut, fin, 0, 0, enfants};
    }

    /** L'arbre aplati d'une suite de nœuds — le préordre s'y écrit. */
    private static int[] aplatir(int[]... noeuds) {
        int[] arbre = new int[noeuds.length * 6];
        for (int i = 0; i < noeuds.length; i++) {
            System.arraycopy(noeuds[i], 0, arbre, i * 6, 6);
        }
        return arbre;
    }

    private static Analysis materielise(String texte, int[] arbre) {
        Document document = TextSession.of(texte, () -> 0L).document();
        return MaterialisationNatif.de(document, arbre, SYMBOLES, 7);
    }

    /** Les trois morceaux d'une chaîne (guillemet, contenu, guillemet)
     *  se SOUDENT en UN jeton du socle ; le blanc APPARTIENT au jeton
     *  qui le suit ; les fermantes voisines ne se soudent jamais —
     *  l'appariement le exige. */
    @Test
    void laChaineSesMorceauxSeSoudeEnUnJeton() {
        // {"cle": [1, true]}
        int[] arbre = aplatir(
                noeud(27, 0, 18, 1),
                noeud(27, 0, 18, 11),
                noeud(OUVR_ACC, 0, 1, 0),
                noeud(CHAINE, 1, 2, 0),
                noeud(CHAINE, 2, 5, 0),
                noeud(CHAINE, 5, 6, 0),
                noeud(DEUX_PTS, 6, 7, 0),
                noeud(OUVR_CRO, 8, 9, 0),
                noeud(19, 9, 10, 0),
                noeud(VIRGULE, 10, 11, 0),
                noeud(25, 12, 16, 0),
                noeud(FERM_CRO, 16, 17, 0),
                noeud(FERM_ACC, 17, 18, 0));
        Analysis analyse = materielise("{\"cle\": [1, true]}", arbre);
        assertEquals(7, analyse.revision());
        Token[] jetons = analyse.lex().line(0).tokens();
        // onze feuilles, trois soudées en une : neuf jetons
        assertEquals(9, jetons.length);
        assertEquals(TokenKind.OPEN_BRACKET, jetons[0].kind());
        assertEquals(0, jetons[0].start());
        assertEquals(1, jetons[0].end());
        // la chaîne soudée couvre guillemets compris
        assertEquals(TokenKind.STRING, jetons[1].kind());
        assertEquals(1, jetons[1].start());
        assertEquals(6, jetons[1].end());
        assertEquals(TokenKind.PUNCTUATION, jetons[2].kind());
        // le blanc du crochet : [8..9] devient [7..9]
        assertEquals(TokenKind.OPEN_BRACKET, jetons[3].kind());
        assertEquals(7, jetons[3].start());
        assertEquals(9, jetons[3].end());
        assertEquals(TokenKind.NUMBER, jetons[4].kind());
        assertEquals(9, jetons[4].start());
        // le blanc avant « true » lui appartient
        assertEquals(TokenKind.KEYWORD, jetons[6].kind());
        assertEquals(11, jetons[6].start());
        assertEquals(16, jetons[6].end());
        // les fermantes voisines restent DEUX jetons
        assertEquals(TokenKind.CLOSE_BRACKET, jetons[7].kind());
        assertEquals(16, jetons[7].start());
        assertEquals(17, jetons[7].end());
        assertEquals(TokenKind.CLOSE_BRACKET, jetons[8].kind());
        assertEquals(17, jetons[8].start());
        assertEquals(18, jetons[8].end());
        assertEquals(0, analyse.lex().bracketDepthAtLineStart(0));
    }

    /** Les jetons se rognent à leur ligne et la profondeur de crochets
     *  ouvre la ligne suivante à la profondeur qui reste ; le bloc de
     *  premier niveau qui traverse les lignes devient UN chunk du pli. */
    @Test
    void leBlocQuiTraverseLesLignesSeChunk() {
        // [a,\n b]
        int[] arbre = aplatir(
                noeud(27, 0, 7, 1),
                noeud(27, 0, 7, 5),
                noeud(OUVR_CRO, 0, 1, 0),
                noeud(IDENT, 1, 2, 0),
                noeud(VIRGULE, 2, 3, 0),
                noeud(IDENT, 5, 6, 0),
                noeud(FERM_CRO, 6, 7, 0));
        Analysis analyse = materielise("[a,\n b]", arbre);
        assertEquals(2, analyse.lex().lineCount());
        assertEquals(0, analyse.lex().bracketDepthAtLineStart(0));
        assertEquals(1, analyse.lex().bracketDepthAtLineStart(1));
        // la ligne 1 : le blanc de tête n'est à personne, b puis ]
        Token[] ligne1 = analyse.lex().line(1).tokens();
        assertEquals(2, ligne1.length);
        assertEquals(TokenKind.IDENTIFIER, ligne1[0].kind());
        assertEquals(1, ligne1[0].start());
        assertEquals(2, ligne1[0].end());
        assertEquals(TokenKind.CLOSE_BRACKET, ligne1[1].kind());
        // le bloc qui traverse : UN chunk, du début à la fin de ligne 1
        List<Chunk> chunks = analyse.tree().chunks();
        assertEquals(1, chunks.size());
        Chunk chunk = chunks.get(0);
        assertEquals(0, chunk.startLine());
        assertEquals(0, chunk.startCol());
        assertEquals(1, chunk.endLine());
        assertEquals(3, chunk.endCol());
        assertTrue(analyse.estComplet());
    }

    /** Les octets de la grammaire deviennent les caractères du modèle :
     *  é (deux octets) et l'emoji (quatre) s'y traduisent — la table
     *  dressée par le décodage, les colonnes suivent. */
    @Test
    void lesOctetsSeTraduisentEnCaracteres() {
        // é + ☕ : octets é=0..2, +=3..4, ☕=5..8 ; caractères 0, 2, 4
        int[] arbre = aplatir(
                noeud(27, 0, 8, 1),
                noeud(27, 0, 8, 3),
                noeud(IDENT, 0, 2, 0),
                noeud(PLUS, 3, 4, 0),
                noeud(IDENT, 5, 8, 0));
        Analysis analyse = materielise("é + ☕", arbre);
        Token[] jetons = analyse.lex().line(0).tokens();
        assertEquals(3, jetons.length);
        // é : l'identificateur d'UN caractère
        assertEquals(TokenKind.IDENTIFIER, jetons[0].kind());
        assertEquals(0, jetons[0].start());
        assertEquals(1, jetons[0].end());
        // le blanc au + qui le suit
        assertEquals(TokenKind.OPERATOR, jetons[1].kind());
        assertEquals(1, jetons[1].start());
        assertEquals(3, jetons[1].end());
        // l'emoji : UN caractère, blanc compris
        assertEquals(TokenKind.IDENTIFIER, jetons[2].kind());
        assertEquals(3, jetons[2].start());
        assertEquals(5, jetons[2].end());
    }

    /** La table des sorts : mots réservés, constructions par leur nom,
     *  opérateur d'un caractère, ponctuation — le repli identificateur. */
    @Test
    void laTableDesSortsSeLit() {
        String texte = "x".repeat(50);
        int[] arbre = aplatir(
                noeud(27, 0, 50, 1),
                noeud(27, 0, 50, 11),
                noeud(23, 0, 3, 0),
                noeud(26, 4, 8, 0),
                noeud(IDENT, 9, 12, 0),
                noeud(POINT_VIRGULE, 13, 14, 0),
                noeud(14, 15, 22, 0),
                noeud(15, 23, 29, 0),
                noeud(16, 30, 35, 0),
                noeud(17, 36, 39, 0),
                noeud(18, 40, 42, 0),
                noeud(21, 43, 45, 0),
                noeud(27, 46, 50, 0));
        Analysis analyse = materielise(texte, arbre);
        Token[] jetons = analyse.lex().line(0).tokens();
        assertEquals(11, jetons.length);
        assertEquals(TokenKind.KEYWORD, jetons[0].kind());
        // « void » porte la teinte opérateur — la table le dit
        assertEquals(TokenKind.OPERATOR, jetons[1].kind());
        assertEquals(TokenKind.IDENTIFIER, jetons[2].kind());
        assertEquals(TokenKind.PUNCTUATION, jetons[3].kind());
        assertEquals(TokenKind.COMMENT, jetons[4].kind());
        assertEquals(TokenKind.STRING, jetons[5].kind());
        assertEquals(TokenKind.STRING, jetons[6].kind());
        assertEquals(TokenKind.NUMBER, jetons[7].kind());
        assertEquals(TokenKind.NUMBER, jetons[8].kind());
        // le décorateur porte l'annotation, l'inconnu l'identifiant
        assertEquals(TokenKind.ANNOTATION, jetons[9].kind());
        assertEquals(TokenKind.IDENTIFIER, jetons[10].kind());
    }

    /** Les guillemets d'un seul caractère portent la teinte chaîne —
     *  l'apostrophe et l'accent grave aussi. */
    @Test
    void lesGuillemetsDUnCaracterePortentLaChaine() {
        int[] arbre = aplatir(
                noeud(27, 0, 5, 1),
                noeud(27, 0, 5, 3),
                noeud(10, 0, 1, 0),
                noeud(11, 2, 3, 0),
                noeud(12, 4, 5, 0));
        Analysis analyse = materielise("\" ' `", arbre);
        Token[] jetons = analyse.lex().line(0).tokens();
        assertEquals(3, jetons.length);
        assertEquals(TokenKind.STRING, jetons[0].kind());
        assertEquals(TokenKind.STRING, jetons[1].kind());
        assertEquals(TokenKind.STRING, jetons[2].kind());
    }

    /** La pile des enfants restants GRANDIT : soixante-dix niveaux
     *  d'imbrication — le tableau double, la profondeur tient. */
    @Test
    void laPileDesEnfantsGranditAuDelaDeSoixanteQuatre() {
        int profondeur = 70;
        int[] arbre = new int[profondeur * 6];
        for (int i = 0; i < profondeur; i++) {
            arbre[i * 6] = i == profondeur - 1 ? IDENT : 27;
            arbre[i * 6 + 1] = 0;
            arbre[i * 6 + 2] = 1;
            arbre[i * 6 + 3] = 0;
            arbre[i * 6 + 4] = 0;
            arbre[i * 6 + 5] = 1;
        }
        // le dernier nœud n'a plus d'enfant : c'est la feuille « x »
        arbre[(profondeur - 1) * 6 + 5] = 0;
        Analysis analyse = materielise("x", arbre);
        assertEquals(1, analyse.lex().line(0).tokens().length);
        assertEquals(TokenKind.IDENTIFIER,
                analyse.lex().line(0).tokens()[0].kind());
    }

    /** L'arbre vide ne donne rien : aucune feuille, aucun chunk — le
     *  lexique reste muet, les profondeurs nulles. */
    @Test
    void larbreVideNeDonneRien() {
        Analysis analyse = materielise("a\nb", new int[0]);
        assertEquals(2, analyse.lex().lineCount());
        assertEquals(0, analyse.lex().line(0).tokens().length);
        assertEquals(0, analyse.lex().line(1).tokens().length);
        assertEquals(0, analyse.tree().chunks().size());
        assertEquals(0, analyse.lex().bracketDepthAtLineStart(1));
    }

    /** Le repli du pont sans source native : null — jamais la voie ne
     *  devine (ADR-020). */
    @Test
    void leRepliDuPontSansSourceNativeRendNull() {
        TextSession session = TextSession.of("x", () -> 0L);
        assertNull(MoteurNatif.de(null, session, null));
    }
}
