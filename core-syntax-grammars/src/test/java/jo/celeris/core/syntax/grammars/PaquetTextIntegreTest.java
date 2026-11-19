package jo.celeris.core.syntax.grammars;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.language.packs.LanguagePack;
import jo.celeris.core.language.packs.PackLoader;
import jo.celeris.core.language.packs.PackSource;
import jo.celeris.core.language.packs.Packs;
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

import jo.celeris.core.syntax.TokenKind;


/**
 * Le banc de conformité du paquet intégré Text (chantier C5, la
 * longue traîne) : la grammaire écrite ici reprend l'oracle du dépôt
 * de départ — le profil {@code text}/{@code plaintext} (gel f310a39)
 * : aucun commentaire, aucun mot-clé, AUCUNE span — le texte brut ne
 * colore rien, le rendu texte fait foi. L'échantillon entier vit
 * sans jeton : c'est la spécification, pas un manque.
 */
final class PaquetTextIntegreTest {

    /** La grammaire livrée du paquet intégré, en texte. */
    private static String grammaireLivree() {
        return read("langages/text/grammar.json");
    }

    private static String read(String chemin) {
        try (InputStream flux = PaquetTextIntegreTest.class.getClassLoader()
                .getResourceAsStream(chemin)) {
            if (flux == null) {
                throw new IllegalStateException("ressource absente : " + chemin);
            }
            return new String(flux.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("ressource illisible : " + chemin, e);
        }
    }

    /** Le paquet text chargé par la vraie fabrique. */
    private static LanguagePack paquetText() throws IOException {
        PackLoader chargeur = PackLoader.with(PackGrammars.INSTANCE);
        for (PackSource.PackFiles fichiers : Packs.classpath("langages").packs()) {
            LanguagePack pack = chargeur.load(fichiers);
            if ("text".equals(pack.manifest().id())) {
                return pack;
            }
        }
        throw new IllegalStateException("le paquet intégré text manque");
    }

    /**
     * La grammaire vide compile sans écart et vit pleinement
     * acceptée — le rapport du moteur le dit : une grammaire sans
     * expression est pleinement acceptée.
     */
    @Test
    void laGrammaireVideCompileSansEcart() {
        Grammar grammaire = Grammars.read(grammaireLivree());
        assertEquals(1.0, grammaire.report().acceptation(),
                "text : acceptation " + grammaire.report().acceptation()
                        + " (" + grammaire.report().refuses() + " refus sur "
                        + grammaire.report().patterns() + " motifs)");
        assertEquals(0, grammaire.report().patterns(),
                "le texte brut n'a pas d'expression — c'est la spécification");
    }

    /**
     * L'échantillon ENTIER vit en repli : chaque ligne non blanche
     * rend UN jeton d'écart à la pile racine (la sortie du lexeur
     * quand rien ne matche — la sorte IDENTIFIER ne se colore pas,
     * c'est le rendu texte brut de l'oracle du départ) ; la ligne
     * blanche ne rend rien. Le déterminisme se garde (deux lexes
     * identiques, le même repli).
     */
    @Test
    void lEchantillonVitEnRepliIdentifiant() throws IOException {
        LanguagePack pack = paquetText();
        assertNotNull(pack.grammar(), "la grammaire du paquet text compile");
        GrammarLexer lexeur = (GrammarLexer) pack.grammar().lexer();
        String[] lines = read("grammaires-commerce/echantillons/text.txt")
                .split("\n", -1);
        LexState etat = lexeur.initialState();
        int tokens = 0;
        for (String line : lines) {
            LexedLine premiere = lexeur.lex(line, etat);
            LexedLine seconde = lexeur.lex(line, etat);
            assertEquals(premiere.tokens().length, seconde.tokens().length,
                    "le lexeur du paquet est déterministe");
            if (line.isBlank()) {
                assertEquals(0, premiere.tokens().length,
                        "la ligne blanche ne rend rien");
            } else {
                assertEquals(1, premiere.tokens().length,
                        "'" + line + "' vit en un jeton d'écart");
                assertEquals(TokenKind.IDENTIFIER, premiere.tokens()[0].kind(),
                        "'" + line + "' : le texte brut ne colore rien");
                assertEquals(line.strip().length(),
                        premiere.tokens()[0].length(),
                        "le jeton couvre la ligne utile, blancs d'escorte rognés");
            }
            tokens += premiere.tokens().length;
            etat = premiere.exitState();
        }
        assertTrue(tokens > 0, "l'échantillon vit en repli");
        assertEquals(0, lexeur.incidents(),
                "aucun incident de budget sur l'échantillon");
    }

    /** Le registre assemblé détecte le Texte par extension et par nom. */
    @Test
    void leRegistreDetecteLeTexte() {
        LanguageRegistry registre = LanguagePlatform.with(PackGrammars.INSTANCE)
                .builtinPacks().build();
        LanguageSupport support = registre.byId("text");
        assertNotNull(support, "text est au registre");
        assertNotNull(support.syntax(), "text a sa source syntaxique");
        assertEquals("text", support.syntax().id());
    }
}
