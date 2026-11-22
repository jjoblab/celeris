package jo.celeris.core.syntax.grammars;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.LexedLine;
import jo.celeris.core.syntax.Token;

/**
 * Le chantier C2/C3 de la feuille de route coloration (prompt du
 * 2026-11-22) : les ANCRES de document. tm4e remplace {@code \A} par
 * un « A » littéral sur la PREMIÈRE ligne (allowA) et par un caractère
 * impossible ({@code \uFFFF}) sur les autres ; notre lexeur
 * ligne-indépendant lisait {@code \A} au sens joni (début de LA
 * ligne, puisque chaque ligne est le texte entier de la recherche) :
 * la grammaire csharp qui exige un {@code \A} d'en-tête matchait
 * CHAQUE ligne, pas la seule première.
 */
final class AncresDocumentTest {

    /** La grammaire de banc : un motif qui exige le début de document. */
    private static final String GRAMMAIRE = """
            {"scopeName":"source.banc",
             "patterns":[
               {"match":"\\\\A\\\\s*#pragma", "name":"meta.directive.banc"},
               {"match":"[a-z]+", "name":"variable.other.banc"}
             ]}
            """;

    /**
     * La ligne 0 porte l'ancre : « #pragma once » en tête de document
     * se colore en directive ; la MÊME ligne sur une ligne > 0 ne
     * matche PAS la directive (l'ancre du document y est impossible).
     * Le critère : la PORTÉE meta.directive.banc vit sur 0-7 en
     * première ligne, jamais sur les suivantes.
     */
    @Test
    void lAncreDeDocumentNeVitQueSurLaPremiereLigne() {
        Grammar grammaire = Grammars.read(GRAMMAIRE);
        GrammarLexer lexeur = new GrammarLexer(grammaire);
        LexedLine ligne0 = lexeur.lex("#pragma once", GrammarState.EMPTY);
        assertTrue(porteDirective(ligne0),
                "la ligne 0 porte \\A : la directive s'y colore");
        LexedLine ligneSuivante = lexeur.lex("#pragma once", GrammarState.EMPTY);
        assertTrue(!porteDirective(ligneSuivante),
                "la ligne suivante ne porte pas \\A : la directive ne"
                        + " s'y colore pas (avant le chantier C2, le lexeur"
                        + " lisait \\A au sens de la ligne : la directive"
                        + " s'y colorait aussi)");
    }

    /** Vrai si la ligne porte la portée de directive de banc. */
    private static boolean porteDirective(LexedLine ligne) {
        for (Token jeton : ligne.tokens()) {
            for (String portee : jo.celeris.core.syntax.scope.ScopeStacks
                    .scopesOf(jeton.scope())) {
                if ("meta.directive.banc".equals(portee)) {
                    return true;
                }
            }
        }
        return false;
    }
}
