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
 * Le banc de conformité du paquet intégré Log (chantier C5, la longue
 * traîne) : la grammaire écrite ici reprend l'oracle du dépôt de
 * départ — le classifieur {@code LogTokenizer} (gel f310a39) : la
 * LIGNE ENTIÈRE reçoit le niveau de la première règle qui matche,
 * l'ordre d'évaluation ERREUR → AVERTISSEMENT → SUCCÈS → EN-TÊTE →
 * INFO se garde, et la ligne ordinaire vit sans span (le chemin
 * rapide du rendu texte brut). Chaque niveau se rend par sa portée :
 * erreur à {@code invalid.illegal} (rouge), avertissement à
 * {@code meta.annotation} (ambre), succès à {@code string} (vert),
 * en-tête à {@code entity.name.type} (teal), info à
 * {@code comment} (gris) — la sémantique du départ vit dans les
 * portées du commerce.
 */
final class PaquetLogIntegreTest {

    /** La grammaire livrée du paquet intégré, en texte. */
    private static String grammaireLivree() {
        return lire("langages/log/grammar.json");
    }

    private static String lire(String chemin) {
        try (InputStream flux = PaquetLogIntegreTest.class.getClassLoader()
                .getResourceAsStream(chemin)) {
            if (flux == null) {
                throw new IllegalStateException("ressource absente : " + chemin);
            }
            return new String(flux.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("ressource illisible : " + chemin, e);
        }
    }

    /** Le paquet log chargé par la vraie fabrique. */
    private static LanguagePack paquetLog() throws IOException {
        PackLoader chargeur = PackLoader.with(PackGrammars.INSTANCE);
        for (PackSource.PackFiles fichiers : Packs.classpath("langages").packs()) {
            LanguagePack pack = chargeur.load(fichiers);
            if ("log".equals(pack.manifest().id())) {
                return pack;
            }
        }
        throw new IllegalStateException("le paquet intégré log manque");
    }

    /** Le lexeur du paquet, par la vraie fabrique. */
    private static GrammaireLexeur lexeur() throws IOException {
        LanguagePack pack = paquetLog();
        assertNotNull(pack.grammar(), "la grammaire du paquet log compile");
        return (GrammaireLexeur) pack.grammar().lexer();
    }

    /**
     * La grammaire écrite ici compile SANS écart — la traîne maison
     * ne demande rien au moteur qu'il ne serve.
     */
    @Test
    void laGrammaireCompileSansEcart() {
        Grammaire grammaire = Grammaires.lire(grammaireLivree());
        assertEquals(1.0, grammaire.rapport().acceptation(),
                "log : acceptation " + grammaire.rapport().acceptation()
                        + " (" + grammaire.rapport().refuses() + " refus sur "
                        + grammaire.rapport().motifs() + " motifs)");
        assertTrue(grammaire.rapport().motifs() > 0, "la grammaire a des motifs");
    }

    /**
     * L'oracle du départ rejoué : la ligne classée porte SON niveau
     * sur toute sa longueur, l'ordre d'évaluation se garde (l'erreur
     * gagne sur le succès, le succès gagne sur l'info, l'avertissement
     * gagne sur l'en-tête), la ligne ordinaire vit sans span.
     */
    @Test
    void lOracleDuDepartSeRejoue() throws IOException {
        GrammaireLexeur lexeur = lexeur();
        niveau(lexeur, "error: something", TokenKind.ERROR);
        niveau(lexeur, "BUILD FAILED in 3s", TokenKind.ERROR);
        niveau(lexeur, "e: file.kt:3", TokenKind.ERROR);
        niveau(lexeur, "échec du build", TokenKind.ERROR);
        niveau(lexeur, "What went wrong:", TokenKind.ERROR);
        niveau(lexeur, "java.lang.Exception: x", TokenKind.ERROR);
        niveau(lexeur, "warning: deprecated API", TokenKind.ANNOTATION);
        niveau(lexeur, "w: file.kt:3", TokenKind.ANNOTATION);
        niveau(lexeur, "> warning foo", TokenKind.ANNOTATION);
        niveau(lexeur, "BUILD SUCCESSFUL in 2s", TokenKind.STRING);
        niveau(lexeur, " UP-TO-DATE", TokenKind.STRING);
        niveau(lexeur, "Sync prêt", TokenKind.STRING);
        niveau(lexeur, "[Tooling] terminé avec succès", TokenKind.STRING);
        niveau(lexeur, "> Task :app:compile", TokenKind.TYPE);
        niveau(lexeur, "[Tooling] sync", TokenKind.COMMENT);
        niveau(lexeur, "[JVM-out] heap", TokenKind.COMMENT);
        repli(lexeur, "Task :app:compileJava");
        repli(lexeur, "no-source");
    }

    /**
     * La ligne ordinaire vit en repli : UN jeton pleine ligne à la
     * pile racine (la sortie du lexeur quand rien ne matche — le
     * rendu texte brut du chemin rapide, la span du départ ne
     * portait pas de couleur, le jeton d'écart n'en porte pas plus).
     */
    private static void repli(GrammaireLexeur lexeur, String ligne) {
        Token[] jetons = lexeur.lex(ligne, lexeur.initialState()).tokens();
        assertEquals(1, jetons.length,
                "'" + ligne + "' vit en un jeton d'écart");
        assertEquals(TokenKind.IDENTIFIER, jetons[0].kind(),
                "'" + ligne + "' : la ligne ordinaire reste sans couleur");
        assertEquals(0, jetons[0].start());
        assertEquals(ligne.length(), jetons[0].end());
    }

    /**
     * La ligne classée vit en UN jeton pleine ligne à SA sorte —
     * l'oracle LogTokenizer du départ : LineSpan(0, longueur, TYPE).
     */
    private static void niveau(GrammaireLexeur lexeur, String ligne,
            TokenKind attendu) {
        LexedLine lexee = lexeur.lex(ligne, lexeur.initialState());
        Token[] jetons = lexee.tokens();
        assertTrue(jetons.length == 1,
                "'" + ligne + "' vit en un jeton (" + jetons.length + ")");
        assertEquals(attendu, jetons[0].kind(), "'" + ligne + "' : " + attendu);
        assertEquals(0, jetons[0].start(), "le jeton part du début de ligne");
        assertEquals(ligne.length(), jetons[0].end(),
                "le jeton couvre la ligne entière");
        assertTrue(jetons[0].scope() != ScopeStacks.NONE,
                "le jeton porte sa pile (H-07)");
    }

    /**
     * L'échantillon se rejoue avec déterminisme : chaque jeton porte
     * sa pile, aucun incident de budget.
     */
    @Test
    void lEchantillonSeRejoueAvecDeterminisme() throws IOException {
        GrammaireLexeur lexeur = lexeur();
        String[] lignes = lire("grammaires-commerce/echantillons/log.txt")
                .split("\n", -1);
        LexState etat = lexeur.initialState();
        int jetons = 0;
        for (String ligne : lignes) {
            LexedLine premiere = lexeur.lex(ligne, etat);
            LexedLine seconde = lexeur.lex(ligne, etat);
            assertArrayEquals(premiere.tokens(), seconde.tokens(),
                    "le lexeur du paquet est déterministe");
            for (Token jeton : premiere.tokens()) {
                assertTrue(jeton.scope() != ScopeStacks.NONE,
                        "chaque jeton porte sa pile (H-07)");
            }
            jetons += premiere.tokens().length;
            etat = premiere.exitState();
        }
        assertTrue(jetons > 0, "l'échantillon produit des jetons");
        assertEquals(0, lexeur.incidents(),
                "aucun incident de budget sur l'échantillon");
    }

    /** Le registre assemblé détecte le Log par extension et par nom. */
    @Test
    void leRegistreDetecteLeLog() {
        LanguageRegistry registre = LanguagePlatform.with(PackGrammars.INSTANCE)
                .builtinPacks().build();
        LanguageSupport support = registre.byId("log");
        assertNotNull(support, "log est au registre");
        assertNotNull(support.syntax(), "log a sa source syntaxique");
        assertEquals("log", support.syntax().id());
    }
}
