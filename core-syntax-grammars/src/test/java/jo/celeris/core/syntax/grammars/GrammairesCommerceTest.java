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
 * TREIZE grammaires TextMate des extensions de VS Code — reprises telles
 * que publiées, SANS MODIFICATION, licence MIT et provenance au
 * registre de portage — se chargent par le moteur réel, rejouent leur
 * échantillon avec déterminisme et sans incident de budget. Ce que le
 * moteur écarte se compte au rapport, jamais en silence : le seuil par
 * grammaire garde la dérive visible d'une montée de version.
 *
 * <p>Le dépôt de ces grammaires : {@code grammaires-commerce/} aux
 * ressources de test — des données de banc, hors des paquets intégrés
 * et des artefacts publiés.</p>
 */
class GrammairesCommerceTest {

    /** Les grammaires du commerce et leur échantillon, par langage. */
    private static final String[][] CATALOGUE = {
        {"json", "0.99"}, {"yaml", "0.80"}, {"python", "0.97"},
        {"shell", "0.98"}, {"c", "0.93"}, {"cpp", "0.70"},
        {"markdown", "0.92"}, {"html", "0.97"}, {"css", "0.95"},
        {"javascript", "0.88"}, {"typescript", "0.88"},
        {"php", "0.95"}, {"ruby", "0.98"}};

    /**
     * Chaque grammaire du commerce charge SANS MODIFICATION et son
     * taux d'acceptation tient le seuil du banc — les écarts se
     * comptent au rapport, la dérive d'une montée de version crève le
     * seuil avant d'atteindre l'utilisateur.
     */
    @Test
    void chaqueGrammaireDuCommerceChargeSansModification() {
        for (String[] entree : CATALOGUE) {
            Grammaire grammaire = grammaireDuCommerce(entree[0]);
            double seuil = Double.parseDouble(entree[1]);
            assertTrue(grammaire.rapport().acceptation() >= seuil,
                    entree[0] + " : acceptation "
                            + grammaire.rapport().acceptation()
                            + " sous le seuil " + seuil + " ("
                            + grammaire.rapport().refuses() + " refus sur "
                            + grammaire.rapport().motifs() + ")");
        }
    }

    /**
     * Chaque grammaire rejoue son échantillon : des jetons à piles de
     * portées, le déterminisme, aucun incident de budget.
     */
    @Test
    void lesEchantillonsSeRejouent() {
        for (String[] entree : CATALOGUE) {
            Grammaire grammaire = grammaireDuCommerce(entree[0]);
            String[] lignes = echantillon(entree[0]).split("\n", -1);
            GrammaireLexeur lexeur = new GrammaireLexeur(grammaire);
            LexState etat = lexeur.initialState();
            int jetons = 0;
            for (String ligne : lignes) {
                LexedLine premiere = lexeur.lex(ligne, etat);
                jetons += premiere.tokens().length;
                LexedLine seconde = lexeur.lex(ligne, etat);
                assertArrayEquals(premiere.tokens(), seconde.tokens(),
                        entree[0] + " : le lexeur est déterministe");
                for (jo.celeris.core.syntax.Token jeton : premiere.tokens()) {
                    assertTrue(jeton.scope() != ScopeStacks.NONE,
                            entree[0] + " : chaque jeton porte sa pile (H-07)");
                }
                etat = premiere.exitState();
            }
            assertTrue(jetons > 0,
                    entree[0] + " : l'échantillon produit des jetons");
            assertEquals(0, lexeur.incidents(),
                    entree[0] + " : aucun incident de budget sur l'échantillon");
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
    private static Grammaire grammaireDuCommerce(String langage) {
        return Grammaires.lire(lire(
                "grammaires-commerce/" + langage + ".tmLanguage.json"));
    }

    /** L'échantillon d'un langage, lu de ses ressources de test. */
    private static String echantillon(String langage) {
        return lire("grammaires-commerce/echantillons/" + langage + ".txt");
    }

    private static String lire(String chemin) {
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
