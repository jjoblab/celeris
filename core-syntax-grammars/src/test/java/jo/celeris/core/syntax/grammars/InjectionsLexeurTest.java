package jo.celeris.core.syntax.grammars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.LexedLine;
import jo.celeris.core.syntax.ScopeStacks;
import jo.celeris.core.syntax.Token;

/**
 * Les injections au lexicographe (M7.6, H-05) : les règles injectées
 * s'appliquent partout où leur sélecteur matche la PILE courante — à
 * gauche ({@code L:}) elles gagnent les ex æquo contre l'hôte, à
 * droite ({@code R:}, et sans préfixe) elles les cèdent. Le jeton
 * injecté porte sa pile : la portée de l'injection s'empile sur le
 * contexte qui l'accueille.
 */
final class InjectionsLexeurTest {

    private static GrammaireLexeur lexeurDe(String json) {
        return new GrammaireLexeur(Grammaires.lire(json));
    }

    @Test
    void linjectionGaucheGagneLesExAequo() {
        GrammaireLexeur lexeur = lexeurDe("""
                {
                  "name": "Hote", "scopeName": "text.hote",
                  "patterns": [ { "match": "mot", "name": "keyword.other.hote" } ],
                  "injections": {
                    "L:text.hote": { "patterns": [
                      { "match": "mot", "name": "support.function.hote" } ] }
                  }
                }
                """);
        LexedLine ligne = lexeur.lex("mot", lexeur.initialState());
        Token jeton = ligne.tokens()[0];
        assertEquals("support.function.hote",
                sommet(ScopeStacks.scopesOf(jeton.scope())),
                "l'injection L: gagne l'ex æquo contre la règle de l'hôte");
    }

    @Test
    void linjectionDroiteCedeLesExAequo() {
        GrammaireLexeur lexeur = lexeurDe("""
                {
                  "name": "Hote", "scopeName": "text.hote",
                  "patterns": [ { "match": "mot", "name": "keyword.other.hote" } ],
                  "injections": {
                    "R:text.hote": { "patterns": [
                      { "match": "mot", "name": "support.function.hote" } ] }
                  }
                }
                """);
        LexedLine ligne = lexeur.lex("mot", lexeur.initialState());
        Token jeton = ligne.tokens()[0];
        assertEquals("keyword.other.hote",
                sommet(ScopeStacks.scopesOf(jeton.scope())),
                "l'injection R: cède l'ex æquo à la règle de l'hôte");
    }

    @Test
    void leSelecteurNApplyQueSurSaPile() {
        GrammaireLexeur lexeur = lexeurDe("""
                {
                  "name": "Hote", "scopeName": "text.hote",
                  "patterns": [
                    { "begin": "\\\"", "end": "\\\"",
                      "contentName": "meta.string.hote" },
                    { "match": "mot", "name": "keyword.other.hote" }
                  ],
                  "injections": {
                    "L:meta.string": { "patterns": [
                      { "match": "mot", "name": "variable.parameter.hote" } ] }
                  }
                }
                """);
        LexedLine dehors = lexeur.lex("mot", lexeur.initialState());
        assertEquals("keyword.other.hote",
                sommet(ScopeStacks.scopesOf(dehors.tokens()[0].scope())),
                "hors de la chaîne, l'injection ne s'applique pas");

        LexedLine ouverture = lexeur.lex("\"", lexeur.initialState());
        LexedLine dedans = lexeur.lex("mot", ouverture.exitState());
        Token injecte = dedans.tokens()[0];
        List<String> pile = ScopeStacks.scopesOf(injecte.scope());
        assertEquals("variable.parameter.hote", sommet(pile),
                "dans la chaîne, l'injection s'applique");
        assertTrue(pile.contains("meta.string.hote"),
                "la portée injectée s'empile sur le contexte accueil : " + pile);
    }

    @Test
    void lexclusionDuSelecteurEcarteLinjection() {
        GrammaireLexeur lexeur = lexeurDe("""
                {
                  "name": "Hote", "scopeName": "text.hote",
                  "patterns": [
                    { "begin": "\\\"", "end": "\\\"",
                      "contentName": "meta.string.hote" },
                    { "match": "mot", "name": "keyword.other.hote" }
                  ],
                  "injections": {
                    "L:text.hote - meta.string": { "patterns": [
                      { "match": "mot", "name": "variable.parameter.hote" } ] }
                  }
                }
                """);
        LexedLine ouverture = lexeur.lex("\"", lexeur.initialState());
        LexedLine dedans = lexeur.lex("mot", ouverture.exitState());
        assertEquals("meta.string.hote",
                sommet(ScopeStacks.scopesOf(dedans.tokens()[0].scope())),
                "l'exclusion écarte l'injection de la chaîne");
    }

    /** La portée du sommet d'une pile — le diagnostic lisible. */
    private static String sommet(List<String> pile) {
        return pile.get(pile.size() - 1);
    }
}
