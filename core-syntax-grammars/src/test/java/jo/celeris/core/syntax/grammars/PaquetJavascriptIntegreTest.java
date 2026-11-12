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

/**
 * Le banc de conformité du paquet intégré JavaScript (chantier C5) :
 * la grammaire du commerce de la famille web livrée en paquet — la
 * copie épinglée SANS MODIFICATION (octet pour octet la ressource du
 * banc, la dérive d'une montée de version crève l'égalité avant
 * d'atteindre l'utilisateur), l'acceptation au seuil du banc du
 * commerce, et l'échantillon rejoué avec déterminisme — chaque jeton
 * porte sa pile (H-07), aucun incident de budget.
 */
final class PaquetJavascriptIntegreTest {

    /** Le seuil d'acceptation du banc du commerce pour JavaScript. */
    private static final double SEUIL_ACCEPTATION = 0.88;

    /** La grammaire livrée du paquet intégré, en texte. */
    private static String grammaireLivree() {
        return lire("langages/javascript/grammar.json");
    }

    /** La grammaire épinglée du banc du commerce, en texte. */
    private static String grammaireDuBanc() {
        return lire("grammaires-commerce/javascript.tmLanguage.json");
    }

    private static String lire(String chemin) {
        try (InputStream flux = PaquetJavascriptIntegreTest.class.getClassLoader()
                .getResourceAsStream(chemin)) {
            if (flux == null) {
                throw new IllegalStateException("ressource absente : " + chemin);
            }
            return new String(flux.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("ressource illisible : " + chemin, e);
        }
    }

    /** Le paquet JavaScript chargé par la vraie fabrique. */
    private static LanguagePack paquetJavascript() throws IOException {
        PackLoader chargeur = PackLoader.with(PackGrammars.INSTANCE);
        for (PackSource.PackFiles fichiers : Packs.classpath("langages").packs()) {
            LanguagePack pack = chargeur.load(fichiers);
            if ("javascript".equals(pack.manifest().id())) {
                return pack;
            }
        }
        throw new IllegalStateException("le paquet intégré javascript manque");
    }

    /**
     * La copie livrée est la copie épinglée : SANS MODIFICATION, octet
     * pour octet — la dérive d'une montée de version crève l'égalité
     * avant d'atteindre l'utilisateur.
     */
    @Test
    void laCopieLivreeEstLaCopieEpingleeDuBanc() {
        assertEquals(grammaireDuBanc(), grammaireLivree(),
                "la grammaire du paquet est la grammaire du banc, sans modification");
    }

    /**
     * L'acceptation de la grammaire livrée tient le seuil du banc du
     * commerce — ce que le moteur écarte se compte au rapport, jamais
     * en silence.
     */
    @Test
    void lAcceptationTientLeSeuilDuBanc() {
        Grammaire grammaire = Grammaires.lire(grammaireLivree());
        assertTrue(grammaire.rapport().acceptation() >= SEUIL_ACCEPTATION,
                "javascript : acceptation " + grammaire.rapport().acceptation()
                        + " sous le seuil " + SEUIL_ACCEPTATION + " ("
                        + grammaire.rapport().refuses() + " refus sur "
                        + grammaire.rapport().motifs() + ")");
    }

    /**
     * Le paquet compile par la vraie fabrique et l'échantillon se
     * rejoue avec déterminisme : des jetons à piles de portées, aucun
     * incident de budget — la voie du paquet vit la même discipline
     * que la voie du banc.
     */
    @Test
    void lEchantillonSeRejoueAvecDeterminisme() throws IOException {
        LanguagePack pack = paquetJavascript();
        assertNotNull(pack.grammar(), "la grammaire du paquet compile");
        assertEquals("javascript", pack.grammar().id());
        GrammaireLexeur lexeur = (GrammaireLexeur) pack.grammar().lexer();
        String[] lignes = lire("grammaires-commerce/echantillons/javascript.txt")
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

    /** Le registre assemblé détecte le JavaScript par extension et par nom. */
    @Test
    void leRegistreDetecteLeJavascript() {
        LanguageRegistry registre = LanguagePlatform.with(PackGrammars.INSTANCE)
                .builtinPacks().build();
        LanguageSupport support = registre.byId("javascript");
        assertNotNull(support, "javascript est au registre");
        assertNotNull(support.syntax(), "javascript a sa source syntaxique");
        assertEquals("javascript", support.syntax().id());
    }
}
