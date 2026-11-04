package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;

/**
 * Le lexeur Kotlin (ADR-004) : mots réservés, gabarits de chaînes
 * (guillemets internes aux accolades), chaînes brutes multi-lignes par
 * état transporté, commentaires de bloc NICHÉS par état à profondeur,
 * documentation distinguée du bloc, annotations, opérateurs propres
 * (?:, ::, ->, ..), convention de casse. Une chaîne non fermée est un
 * jeton ERROR, jamais une exception.
 */
class KotlinLexeurTest {

    private final LanguageLexer lexeur = KotlinSyntax.INSTANCE.lexer();

    private Token[] lex(String ligne) {
        return lexeur.lex(ligne, KotlinLexeur.Etat.NORMAL).tokens();
    }

    /** Les mots réservés du langage se distinguent des identificateurs. */
    @Test
    void motsReserves() {
        Token[] jetons = lex("fun val var class object when companion data");
        for (Token jeton : jetons) {
            assertEquals(TokenKind.KEYWORD, jeton.kind());
        }
        assertEquals(TokenKind.IDENTIFIER, lex("compteur")[0].kind());
    }

    /** Le gabarit garde ses guillemets internes dans la chaîne. */
    @Test
    void gabaritsDeChaine() {
        Token[] jetons = lex("valeur = \"somme = ${somme}\" + \"$nom\"");
        assertEquals(TokenKind.STRING, jetons[2].kind());
        assertEquals(18, jetons[2].length(), "le gabarit entier reste un jeton");
        assertEquals(TokenKind.OPERATOR, jetons[3].kind());
        Token[] interne = lex("x = \"cle ${map[\"k\"]}\"");
        assertEquals(3, interne.length,
                "les guillemets internes aux accolades ne clôturent pas");
        assertEquals(TokenKind.STRING, interne[2].kind());
    }

    /** La chaîne brute ouvre un crochet apparié et traverse les lignes. */
    @Test
    void chaineBruteMultiLignes() {
        LexedLine ouverture = lexeur.lex("val b = \"\"\"", KotlinLexeur.Etat.NORMAL);
        assertEquals(TokenKind.OPEN_BRACKET, ouverture.tokens()[3].kind());
        assertSame(KotlinLexeur.Etat.CHAINE_BRUTE, ouverture.exitState());
        LexedLine milieu = lexeur.lex("brute $gabarit", ouverture.exitState());
        assertEquals(1, milieu.tokens().length);
        assertEquals(TokenKind.STRING, milieu.tokens()[0].kind());
        LexedLine fermeture = lexeur.lex("fin\"\"\";", ouverture.exitState());
        assertEquals(TokenKind.CLOSE_BRACKET, fermeture.tokens()[1].kind());
        assertSame(KotlinLexeur.Etat.NORMAL, fermeture.exitState());
    }

    /** Les commentaires de bloc se nichent — l'état porte la profondeur. */
    @Test
    void commentairesNiches() {
        Token[] memeLigne = lex("a /* un /* deux */ reste */ b");
        assertEquals(TokenKind.IDENTIFIER, memeLigne[0].kind());
        assertEquals(TokenKind.COMMENT, memeLigne[1].kind());
        assertEquals(TokenKind.IDENTIFIER, memeLigne[2].kind());

        LexedLine ouverture = lexeur.lex("/* niveau un", KotlinLexeur.Etat.NORMAL);
        assertEquals(new KotlinLexeur.DansBloc(1), ouverture.exitState());
        LexedLine niche = lexeur.lex("/* niche", ouverture.exitState());
        assertEquals(new KotlinLexeur.DansBloc(2), niche.exitState());
        LexedLine fermeture = lexeur.lex("*/ puis */ suite", niche.exitState());
        assertEquals(TokenKind.COMMENT, fermeture.tokens()[0].kind());
        assertSame(KotlinLexeur.Etat.NORMAL, fermeture.exitState());
        assertEquals(TokenKind.IDENTIFIER, fermeture.tokens()[2].kind());
    }

    /** La documentation (double astérisque) se distingue du bloc simple. */
    @Test
    void documentationContreBloc() {
        assertEquals(TokenKind.DOC, lex("/** doc */ fun")[0].kind());
        assertEquals(TokenKind.COMMENT, lex("/* bloc */ x")[0].kind());
        assertEquals(TokenKind.COMMENT, lex("// ligne")[0].kind());
    }

    /** Annotations, opérateurs propres et ponctuation. */
    @Test
    void annotationsEtOperateurs() {
        assertEquals(TokenKind.ANNOTATION, lex("@JvmStatic")[0].kind());
        assertEquals(TokenKind.OPERATOR, lex("a ?: b")[1].kind());
        assertEquals(TokenKind.OPERATOR, lex("A::b")[1].kind());
        assertEquals(TokenKind.OPERATOR, lex("x -> y")[1].kind());
        assertEquals(TokenKind.OPERATOR, lex("1..9")[1].kind());
        assertEquals(TokenKind.PUNCTUATION, lex("a.b, c")[1].kind());
        assertEquals(TokenKind.PUNCTUATION, lex("a.b, c")[3].kind());
    }

    /** La convention de casse distingue types et constantes. */
    @Test
    void conventionDeCasse() {
        assertEquals(TokenKind.TYPE, lex("Catalogue")[0].kind());
        assertEquals(TokenKind.CONSTANT, lex("LIMITE_MAX")[0].kind());
        assertEquals(TokenKind.IDENTIFIER, lex("somme")[0].kind());
        Token[] qualifie = lex("java.util.Map");
        assertEquals(TokenKind.IDENTIFIER, qualifie[0].kind());
        assertEquals(TokenKind.TYPE, qualifie[2].kind(),
                "le dernier segment porte le type");
    }

    /** Une chaîne non fermée donne un jeton ERROR, pas d'exception. */
    @Test
    void chaineNonFermee() {
        assertEquals(TokenKind.ERROR, lex("\"sans fin")[0].kind());
    }

    /** Le lexique complet du corpus tient sans incident. */
    @Test
    void lexiqueComplet() {
        String source = CorpusChargeur.exempleKotlin();
        IncrementalLexer moteur = new IncrementalLexer();
        LexResult resultat = moteur.lexFull(Document.of(source), lexeur);
        assertEquals(Document.of(source).lineCount(), resultat.lex().lineCount());
        assertSame(KotlinLexeur.Etat.NORMAL,
                resultat.lex().line(resultat.lex().lineCount() - 1).exitState());
    }
}
