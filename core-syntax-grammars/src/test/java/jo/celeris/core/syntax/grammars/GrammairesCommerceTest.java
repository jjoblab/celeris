package jo.celeris.core.syntax.grammars;

import jo.celeris.core.syntax.scope.ScopeStacks;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.LexState;
import jo.celeris.core.syntax.LexedLine;


/**
 * Le banc de conformité des grammaires du commerce (M7.6, H-05) : les
 * VINGT grammaires TextMate des extensions de VS Code et du commerce
 * (la traîne du chantier C5 y ajoute swift, dart, scala, csharp et
 * lua) — reprises telles que publiées, SANS MODIFICATION, licence MIT
 * et provenance au registre de portage — se chargent par le moteur
 * réel, rejouent leur échantillon avec déterminisme et sans incident
 * de budget. Ce que le moteur écarte se compte au rapport, jamais en
 * silence : le seuil par grammaire garde la dérive visible d'une
 * montée de version.
 *
 * <p>Le dépôt de ces grammaires : {@code grammaires-commerce/} aux
 * ressources de test — des données de banc, hors des paquets intégrés
 * et des artefacts publiés.</p>
 */
class GrammairesCommerceTest {

    /**
     * Les grammaires du commerce et leur échantillon, par langage.
     * Les seuils shell et go datent du correctif du chargeur de
     * référentiel (audit coloration) : les ouvertures et fermetures
     * des règles COMPLÈTES du référentiel — jamais compilées avant,
     * leurs motifs n'entraient pas dans la mesure — s'y compilent
     * désormais ; certaines tombent à la garde catastrophique et se
     * comptent. La population a changé, PAS la garde : shell 4 refus
     * sur 187 (0,979), c 22 sur 271 (0,919), cpp 135 sur 349 (0,613),
     * go 25 sur 160 (0,844) — les seuils repartent de la mesure
     * corrigée, cliquet sur la NOUVELLE population.
     */
    private static final String[][] CATALOGUE = {
        {"json", "0.99"}, {"yaml", "0.80"}, {"python", "0.97"},
        {"shell", "0.97"}, {"c", "0.91"}, {"cpp", "0.61"},
        {"markdown", "0.92"}, {"html", "0.97"}, {"css", "0.95"},
        {"javascript", "0.88"}, {"typescript", "0.88"},
        {"php", "0.95"}, {"ruby", "0.98"},
        {"go", "0.84"}, {"rust", "0.98"},
        {"swift", "0.95"}, {"dart", "0.95"}, {"scala", "0.95"},
        {"csharp", "0.93"}, {"lua", "0.98"}};

    /**
     * Chaque grammaire du commerce charge SANS MODIFICATION et son
     * taux d'acceptation tient le seuil du banc — les écarts se
     * comptent au rapport, la dérive d'une montée de version crève le
     * seuil avant d'atteindre l'utilisateur.
     */
    @Test
    void chaqueGrammaireDuCommerceChargeSansModification() {
        for (String[] input : CATALOGUE) {
            Grammar grammaire = grammaireDuCommerce(input[0]);
            double seuil = Double.parseDouble(input[1]);
            assertTrue(grammaire.report().acceptation() >= seuil,
                    input[0] + " : acceptation "
                            + grammaire.report().acceptation()
                            + " sous le seuil " + seuil + " ("
                            + grammaire.report().refuses() + " refus sur "
                            + grammaire.report().patterns() + ")");
        }
    }

    /**
     * Chaque grammaire rejoue son échantillon : des jetons à piles de
     * portées, le déterminisme, aucun incident de budget.
     */
    @Test
    void lesEchantillonsSeRejouent() {
        for (String[] input : CATALOGUE) {
            Grammar grammaire = grammaireDuCommerce(input[0]);
            String[] lines = echantillon(input[0]).split("\n", -1);
            GrammarLexer lexeur = new GrammarLexer(grammaire);
            LexState etat = lexeur.initialState();
            int tokens = 0;
            for (String line : lines) {
                LexedLine premiere = lexeur.lex(line, etat);
                tokens += premiere.tokens().length;
                LexedLine seconde = lexeur.lex(line, etat);
                assertArrayEquals(premiere.tokens(), seconde.tokens(),
                        input[0] + " : le lexeur est déterministe");
                for (jo.celeris.core.syntax.Token token : premiere.tokens()) {
                    assertTrue(token.scope() != ScopeStacks.NONE,
                            input[0] + " : chaque jeton porte sa pile (H-07)");
                }
                etat = premiere.exitState();
            }
            assertTrue(tokens > 0,
                    input[0] + " : l'échantillon produit des jetons");
            assertEquals(0, lexeur.incidents(),
                    input[0] + " : aucun incident de budget sur l'échantillon");
        }
    }

    /** Les grammaires qui déclarent des injections les portent (H-05). */
    @Test
    void lesInjectionsDeclareesSeChargent() {
        assertTrue(grammaireDuCommerce("html").injections().size() >= 1,
                "html déclare ses injections et elles se chargent");
        assertTrue(!grammaireDuCommerce("html").injections().get(0)
                .prioriteGauche(),
                "l'injection html est R: — l'hôte reste maître chez lui");
    }

    /** La grammaire du commerce, lue de ses ressources de test. */
    private static Grammar grammaireDuCommerce(String langage) {
        return Grammars.read(read(
                "grammaires-commerce/" + langage + ".tmLanguage.json"));
    }

    /** L'échantillon d'un langage, lu de ses ressources de test. */
    private static String echantillon(String langage) {
        return read("grammaires-commerce/echantillons/" + langage + ".txt");
    }

    private static String read(String chemin) {
        try (InputStream flux = GrammairesCommerceTest.class.getClassLoader()
                .getResourceAsStream(chemin)) {
            if (flux == null) {
                throw new IllegalStateException("ressource absente : " + chemin);
            }
            return new String(flux.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("ressource illisible : " + chemin, e);
        }
    }
}
