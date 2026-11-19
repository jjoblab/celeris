package jo.celeris.core.syntax.grammars;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.language.packs.LanguagePack;
import jo.celeris.core.language.packs.PackLoader;
import jo.celeris.core.language.packs.PackSource;
import jo.celeris.core.language.packs.Packs;
import jo.celeris.core.syntax.scope.ScopeStacks;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;


import jo.celeris.core.language.LanguagePlatform;
import jo.celeris.core.language.LanguageRegistry;




import jo.celeris.core.syntax.LexState;
import jo.celeris.core.syntax.LexedLine;

import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;

/**
 * Le banc de conformité du paquet intégré Smali (chantier C5, la
 * longue traîne) : la grammaire écrite ici reprend l'oracle du dépôt
 * de départ — le tokéniseur {@code SmaliTokenizer} (gel f310a39) :
 * les directives connues en mot-clé (l'inconnue en annotation), les
 * registres p/v en constante, les descripteurs {@code L…;} en type,
 * les opcodes à tiret et slash en mot-clé, les nombres hexadécimaux
 * et signés en nombre, les chaînes fermées en chaîne, le commentaire
 * au chevron. L'orpheline tombe dans les règles suivantes (la garde
 * historique du départ se conserve : la chaîne NON fermée ne se
 * colore pas comme chaîne).
 */
final class PaquetSmaliIntegreTest {

    /** La grammaire livrée du paquet intégré, en texte. */
    private static String grammaireLivree() {
        return read("langages/smali/grammar.json");
    }

    private static String read(String chemin) {
        try (InputStream flux = PaquetSmaliIntegreTest.class.getClassLoader()
                .getResourceAsStream(chemin)) {
            if (flux == null) {
                throw new IllegalStateException("ressource absente : " + chemin);
            }
            return new String(flux.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("ressource illisible : " + chemin, e);
        }
    }

    /** Le paquet smali chargé par la vraie fabrique. */
    private static LanguagePack paquetSmali() throws IOException {
        PackLoader chargeur = PackLoader.with(PackGrammars.INSTANCE);
        for (PackSource.PackFiles fichiers : Packs.classpath("langages").packs()) {
            LanguagePack pack = chargeur.load(fichiers);
            if ("smali".equals(pack.manifest().id())) {
                return pack;
            }
        }
        throw new IllegalStateException("le paquet intégré smali manque");
    }

    /**
     * La grammaire écrite ici compile SANS écart — la traîne maison
     * ne demande rien au moteur qu'il ne serve.
     */
    @Test
    void laGrammaireCompileSansEcart() {
        Grammar grammaire = Grammars.read(grammaireLivree());
        assertEquals(1.0, grammaire.report().acceptation(),
                "smali : acceptation " + grammaire.report().acceptation()
                        + " (" + grammaire.report().refuses() + " refus sur "
                        + grammaire.report().patterns() + " motifs)");
        assertTrue(grammaire.report().patterns() > 0, "la grammaire a des motifs");
    }

    /**
     * L'oracle du départ rejoué : chaque famille du départ vit à SA
     * sorte — la directive connue en KEYWORD, l'inconnue en
     * ANNOTATION, le registre en constante, le descripteur et le
     * type majuscule en TYPE, l'opcode en KEYWORD, l'hexadécimal en
     * NUMBER, la chaîne fermée en STRING, le commentaire en COMMENT.
     */
    @Test
    void lOracleDuDepartSeRejoue() throws IOException {
        LanguagePack pack = paquetSmali();
        assertNotNull(pack.grammar(), "la grammaire du paquet smali compile");
        GrammarLexer lexeur = (GrammarLexer) pack.grammar().lexer();
        sorte(lexeur, ".class public Lbanc/Echantillon;", TokenKind.KEYWORD,
                "la directive .class");
        sorte(lexeur, ".registers 5", TokenKind.KEYWORD, "la directive .registers");
        sorte(lexeur, ".end method", TokenKind.KEYWORD, "la directive .end method");
        sorte(lexeur, ".quidonc chose", TokenKind.ANNOTATION,
                "la directive inconnue");
        sorte(lexeur, "iput-object p1, p0, Lbanc/Echantillon;->nom:Ljava/lang/String;",
                TokenKind.CONSTANT, "le registre p0");
        sorte(lexeur, "iput-object p1, p0, Lbanc/Echantillon;->nom:Ljava/lang/String;",
                TokenKind.TYPE, "le descripteur Ljava/lang/String;");
        sorte(lexeur, "invoke-direct {p0}, Ljava/lang/Object;-><init>()V",
                TokenKind.KEYWORD, "l'opcode invoke-direct");
        sorte(lexeur, "invoke-interface/range {v0 .. v5}, Lbanc/Corpus;->ligne(I)Ljava/lang/String;",
                TokenKind.KEYWORD, "l'opcode invoke-interface/range");
        sorte(lexeur, "const/4 v0, 0x1f", TokenKind.NUMBER,
                "l'hexadécimal 0x1f");
        sorte(lexeur, "const/4 v1, -0x1", TokenKind.NUMBER, "l'entier signé");
        sorte(lexeur, "const-string v4, \"index hors bornes\"",
                TokenKind.STRING, "la chaîne fermée");
        sorte(lexeur, "# le commentaire du banc", TokenKind.COMMENT,
                "le commentaire au croisillon");
        Token[] orpheline = lexeur.lex("const-string v4, \"orpheline",
                lexeur.initialState()).tokens();
        for (Token token : orpheline) {
            assertTrue(token.kind() != TokenKind.STRING,
                    "la chaîne orpheline ne se colore pas comme chaîne (la"
                            + " garde historique du départ) : " + token.kind());
        }
    }

    /**
     * La ligne porte au moins un jeton de la sorte attendue —
     * l'oracle SmaliTokenizer classait les familles, le paquet les
     * rend par la portée du commerce.
     */
    private static void sorte(GrammarLexer lexeur, String line,
            TokenKind attendu, String commentaire) {
        LexedLine lexee = lexeur.lex(line, lexeur.initialState());
        Token[] tokens = lexee.tokens();
        assertTrue(tokens.length > 0, "'" + line + "' produit des jetons");
        boolean trouve = false;
        for (Token token : tokens) {
            assertTrue(token.scope() != ScopeStacks.NONE,
                    "chaque jeton porte sa pile (H-07)");
            if (token.kind() == attendu) {
                trouve = true;
            }
        }
        assertTrue(trouve, line + " : " + commentaire + " vit en " + attendu);
    }

    /**
     * L'échantillon se rejoue avec déterminisme : des jetons à piles
     * de portées, aucun incident de budget.
     */
    @Test
    void lEchantillonSeRejoueAvecDeterminisme() throws IOException {
        LanguagePack pack = paquetSmali();
        assertNotNull(pack.grammar(), "la grammaire du paquet smali compile");
        GrammarLexer lexeur = (GrammarLexer) pack.grammar().lexer();
        String[] lines = read("grammaires-commerce/echantillons/smali.txt")
                .split("\n", -1);
        LexState etat = lexeur.initialState();
        int tokens = 0;
        for (String line : lines) {
            LexedLine premiere = lexeur.lex(line, etat);
            LexedLine seconde = lexeur.lex(line, etat);
            assertArrayEquals(premiere.tokens(), seconde.tokens(),
                    "le lexeur du paquet est déterministe");
            for (Token token : premiere.tokens()) {
                assertTrue(token.scope() != ScopeStacks.NONE,
                        "chaque jeton porte sa pile (H-07)");
            }
            tokens += premiere.tokens().length;
            etat = premiere.exitState();
        }
        assertTrue(tokens > 0, "l'échantillon produit des jetons");
        assertEquals(0, lexeur.incidents(),
                "aucun incident de budget sur l'échantillon");
    }

    /** Le registre assemblé détecte le Smali par extension et par nom. */
    @Test
    void leRegistreDetecteLeSmali() {
        LanguageRegistry registre = LanguagePlatform.with(PackGrammars.INSTANCE)
                .builtinPacks().build();
        LanguageSupport support = registre.byId("smali");
        assertNotNull(support, "smali est au registre");
        assertNotNull(support.syntax(), "smali a sa source syntaxique");
        assertEquals("smali", support.syntax().id());
    }
}
