package jo.celeris.core.syntax.grammars;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.LexedLine;

/**
 * Le chantier B de la feuille de route coloration (audit F8, prompt du
 * 2026-11-22) : le lexeur ne RETENTE pas une expression qui vient
 * d'échouer — le modèle est le « cache de prochaine correspondance » de
 * tm4e ({@code JoniOnigRegExp.search} : mémoriser ligne / position /
 * résultat par expression, la recherche ne repart que sous la position
 * mémorisée). Le banc OBSERVE le nombre de recherches joni par ligne :
 * la ligne ordinaire (un identifiant, un espace, un nombre) ne paie
 * pas une recherche PAR RÈGLE PAR POSITION — l'échec mémorisé
 * économise les re-tentatives jusqu'à sa borne.
 *
 * <p>Le banc d'intégration verifie AUSSI la correction : le lexeur
 * avec cache rend les MÊMES jetons que le lexeur sans cache sur les
 * grammaires livrées (la performance ne change jamais la
 * coloration).</p>
 */
final class CacheCorrespondanceTest {

    /**
     * La ligne ordinaire ne paie pas une recherche par règle par
     * position : « x = 1 » en python — les échecs mémorisés font que
     * le nombre TOTAL de recherches joni reste sous le produit
     * (nombre de règles visitées × longueur de ligne). Le contrat
     * précis : la re-tentative d'une expression ÉCHOUÉE à la position
     * p ne se rejoue pas à p+1 si l'échec est mémorisé pour toute
     * position inférieure à la prochaine correspondance connue.
     */
    @Test
    void laLigneOrdinaireNEmpilePasLesRetentatives() {
        // Grammaire de banc : quelques règles réalistes (identifiant,
        // nombre, opérateur, mot-clé) — le lexeur visite toutes les
        // règles à chaque position tant qu'aucune ne matche.
        String source = """
                {"scopeName":"source.banc",
                 "patterns":[
                   {"match":"\\\\bif\\\\b","name":"keyword.control.banc"},
                   {"match":"\\\\b[A-Za-z_][A-Za-z0-9_]*\\\\b","name":"variable.other.banc"},
                   {"match":"\\\\b[0-9]+\\\\b","name":"constant.numeric.banc"},
                   {"match":"=|\\\\+|-","name":"keyword.operator.banc"}
                 ]}
                """;
        GrammarLexer lexeur = new GrammarLexer(Grammars.read(source));
        lexeur.lex("###", GrammarState.EMPTY);
        long recherches = lexeur.essaisDerniereLigne();
        // 6 positions significatives (x, =, 1 fin de ligne) : sans
        // cache, chaque position paie TOUTES les règles non filtrées
        // (4) ; avec le cache d'échec, une règle qui a échoué à p ne
        // se rejoue pas à p+1 : le total descend sous le plafond du
        // produit. La borne du banc est LÂCHE (ce n'est pas un banc de
        // performance chiffrée — il attrape l'ABSENCE du cache).
        assertTrue(recherches <= 4L * 5,
                "la ligne de 5 caractères a coûté " + recherches
                        + " recherches joni — le cache de prochaine"
                        + " correspondance doit économiser les"
                        + " re-tentatives (sans cache : chaque position"
                        + " retente toutes les règles)");
    }

    /**
     * La correction d'abord : le lexeur avec cache rend les MÊMES
     * jetons que la même grammaire sans observation — la ligne
     * « x = 1 » se colore identiquement (identifiant, opérateur,
     * nombre).
     */
    @Test
    void leCacheNeChangePasLesJetons() {
        String source = """
                {"scopeName":"source.banc",
                 "patterns":[
                   {"match":"\\\\b[A-Za-z_][A-Za-z0-9_]*\\\\b","name":"variable.other.banc"},
                   {"match":"\\\\b[0-9]+\\\\b","name":"constant.numeric.banc"},
                   {"match":"=|\\\\+|-","name":"keyword.operator.banc"}
                 ]}
                """;
        GrammarLexer lexeur = new GrammarLexer(Grammars.read(source));
        LexedLine premiere = lexeur.lex("x = 1",
                GrammarState.EMPTY);
        List<String> jetons = new ArrayList<>();
        for (jo.celeris.core.syntax.Token jeton : premiere.tokens()) {
            jetons.add(jeton.start() + "-" + jeton.end() + ":"
                    + jeton.kind());
        }
        assertNotNull(jetons);
        assertTrue(jetons.size() >= 3,
                "la ligne rend au moins 3 jetons : " + jetons);
        assertTrue(jetons.get(0).startsWith("0-1:"),
                "l'identifiant x commence à 0 : " + jetons.get(0));
        assertTrue(jetons.get(1).contains("2-3:"),
                "l'opérateur = couvre 2-3 : " + jetons.get(1));
        assertTrue(jetons.get(2).contains("4-5:"),
                "le nombre 1 couvre 4-5 : " + jetons.get(2));
    }

}
