package jo.celeris.core.syntax.grammars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Le budget d'essais par ligne à sa valeur PAR DÉFAUT (audit
 * coloration, piste H9) : chaque recherche de règle consomme un essai
 * ({@code GrammarLexer.chercher}), le plafond livré est 20 000 par
 * ligne. La sonde de l'audit l'a mesuré : la grammaire javascript
 * consomme ses 20 000 essais en ~120 jetons — une ligne de HAUT
 * NIVEAU de 315 caractères (vingt appels {@code foo(42, "x");})
 * déborde déjà ; le bundle minifié réel (des dizaines de kilo-octets
 * sur une ligne) ne colore que ses ~119 premiers jetons, le reste
 * retombe en portée de contexte SANS erreur visible (un
 * avertissement de journal seul). (Le JSON, lui, vit par l'analyseur
 * écrit — le paquet json ne porte pas de grammaire.)
 *
 * <p>Le banc fige le défaut : vingt appels sur UNE ligne débordent le
 * budget par défaut ; la même matière répartie sur des lignes ne
 * déborde pas — c'est bien la densité par ligne, pas le volume.</p>
 */
class BudgetLigneDenseTest {

    /** Vingt appels minifiés sur une ligne : 315 caractères réels. */
    private static String jsUneLigne(int appels) {
        StringBuilder sb = new StringBuilder(appels * 16 + 4);
        for (int i = 0; i < appels; i++) {
            sb.append("foo").append(i).append("(42, \"x\"); ");
        }
        return sb.toString();
    }

    /** La grammaire d'un paquet intégré, chargée par le vrai chargeur. */
    private static Grammar grammaireDePaquet(String id) {
        var flux = BudgetLigneDenseTest.class.getResourceAsStream(
                "/langages/" + id + "/grammar.json");
        if (flux == null) {
            throw new IllegalArgumentException("grammaire du paquet " + id);
        }
        try (flux) {
            return Grammars.read(new String(flux.readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Vingt appels sur une ligne de 315 caractères : un cas réaliste
     * COURT ne doit pas déborder — la ligne se colore ENTIÈREMENT au
     * budget par défaut. (Mesuré avant correctif : la grammaire
     * javascript consomme ses 20 000 essais en ~120 jetons, la ligne
     * déborde, sa seconde moitié reste en portée de contexte.)
     */
    @Test
    void vingtAppelsSurUneLigneSeColorentEntierement() {
        Grammar grammaire = grammaireDePaquet("javascript");
        String ligne = jsUneLigne(20);
        assertTrue(ligne.length() < 400,
                "garde du banc : la ligne reste un cas réaliste court");
        GrammarLexer defaut = new GrammarLexer(grammaire);
        jo.celeris.core.syntax.LexedLine ligneDefaut =
                defaut.lex(ligne, GrammarState.EMPTY);
        assertEquals(0, defaut.incidents(),
                "une ligne javascript de " + ligne.length() + " caractères "
                        + "ne doit pas déborder le budget par défaut");
        GrammarLexer large = new GrammarLexer(grammaire, 2_000_000);
        jo.celeris.core.syntax.LexedLine ligneComplete =
                large.lex(ligne, GrammarState.EMPTY);
        assertEquals(ligneComplete.tokens().length, ligneDefaut.tokens().length,
                "aucun jeton perdu : la ligne courte se colore entière");
    }

    /**
     * Le contenu réparti ne déborde pas : le budget est PAR LIGNE —
     * la même matière, un appel par ligne, passe sans incident.
     */
    @Test
    void laMemeMatiereRepartieNeDebordePas() {
        Grammar grammaire = grammaireDePaquet("javascript");
        GrammarLexer lexeur = new GrammarLexer(grammaire);
        GrammarState etat = GrammarState.EMPTY;
        for (int i = 0; i < 20; i++) {
            etat = (GrammarState) java.util.Objects.requireNonNull(
                    lexeur.lex("foo" + i + "(42, \"x\");", etat).exitState());
        }
        assertEquals(0, lexeur.incidents(),
                "un appel par ligne ne déborde jamais le budget");
    }

    /**
     * La dégradation est SILENCIEUSE côté jetons (mesuré avant
     * correctif, pour mémoire) : la ligne débordée porte MOINS de
     * jetons que la même ligne à budget large — la sonde de l'audit :
     * 119 jetons émis pour 40 appels (630 caractères) au budget par
     * défaut, la moitié de la ligne restait monochrome. Le banc
     * exige l'état désiré : la ligne de 40 appels se colore entière.
     */
    @Test
    void laDegradationNEstPasSignaleeDansLesJetons() {
        Grammar grammaire = grammaireDePaquet("javascript");
        String ligne = jsUneLigne(40);
        GrammarLexer deborde = new GrammarLexer(grammaire);
        jo.celeris.core.syntax.LexedLine ligneDebordee =
                deborde.lex(ligne, GrammarState.EMPTY);
        assertEquals(0, deborde.incidents(),
                "la ligne de 40 appels ne doit pas déborder");
        GrammarLexer large = new GrammarLexer(grammaire, 2_000_000);
        jo.celeris.core.syntax.LexedLine ligneComplete =
                large.lex(ligne, GrammarState.EMPTY);
        assertEquals(0, large.incidents(), "à budget large, aucun incident");
        assertEquals(ligneComplete.tokens().length, ligneDebordee.tokens().length,
                "aucun jeton perdu sur la ligne entière — la truncature "
                        + "silencieuse (119 jetons pour 630 caractères, "
                        + "mesuré avant correctif) ne revient pas");
    }
}
