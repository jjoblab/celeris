package jo.celeris.core.syntax.grammars;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.LexedLine;

/**
 * Le chantier C4 de la feuille de route coloration (prompt du
 * 2026-11-22) : les inclusions EXTERNES — une grammaire qui inclut
 * {@code source.css} y trouve celle du même montage par le registre
 * partagé de la fabrique (html colore {@code <style>} par la
 * grammaire css). Avant : {@code PackGrammars.compile} lisait sans
 * registre — l'inclusion externe se sautait, comptée au rapport ; le
 * bloc {@code <style>} restait coloré au seul html.
 */
final class InclusionsExternesTest {

    /** La grammaire INVITÉE : du css simple. */
    private static final String CSS = """
            {"scopeName":"source.css",
             "patterns":[
               {"match":"[a-z-]+:", "name":"support.type.property.css"},
               {"match":"[{}]", "name":"punctuation.definition.bracket.css"}
             ]}
            """;

    /** La grammaire HÔTE : un bloc qui inclut l'invitée. */
    private static final String HOTE = """
            {"scopeName":"source.hote",
             "patterns":[
               {"begin":"<style>", "end":"</style>",
                "contentName":"source.css.embedded",
                "patterns":[{"include":"source.css"}]},
               {"match":"[a-z]+", "name":"variable.other.hote"}
             ]}
            """;

    /**
     * L'inclusion externe se résout par le registre : le contenu du
     * bloc {@code <style>} porte les portées de la grammaire invitée
     * (la propriété css s'y colore) — pas seulement l'hôte.
     */
    @Test
    void linclusionExterneColoreParLaGrammaireInvitee() {
        PackGrammars fabrique = PackGrammars.avecRegistre();
        // L'invitée se compile D'ABORD : elle se pose au registre.
        assertNotNull(fabrique.compile("css", CSS));
        // L'hôte inclut source.css : le registre la lui rend.
        GrammarLexer lexeur = new GrammarLexer(
                ((GrammarSyntax) fabrique.compile("hote", HOTE)).grammaire());
        LexedLine ligne = lexeur.lex("<style>color: red</style>",
                GrammarState.EMPTY);
        boolean proprieteCss = false;
        for (var jeton : ligne.tokens()) {
            for (String portee : jo.celeris.core.syntax.scope.ScopeStacks
                    .scopesOf(jeton.scope())) {
                if ("support.type.property.css".equals(portee)) {
                    proprieteCss = true;
                }
            }
        }
        assertTrue(proprieteCss,
                "le contenu du bloc <style> porte la portée css de"
                        + " l'invitée (l'inclusion externe s'est résolue"
                        + " par le registre partagé)");
    }

    /**
     * Le CONTRAT de repli : sans registre (INSTANCE historique), le
     * même hôte ne REFUSE pas — l'inclusion se saute, le bloc se
     * colore au seul hôte (jamais de refus de paquet).
     */
    @Test
    void sansRegistreLinclusionSeSauteSansRefus() {
        GrammarLexer lexeur = new GrammarLexer(
                ((GrammarSyntax) PackGrammars.INSTANCE.compile("hote", HOTE))
                        .grammaire());
        LexedLine ligne = lexeur.lex("<style>color: red</style>",
                GrammarState.EMPTY);
        assertNotNull(ligne,
                "l'hôte sans registre se lex sans refus (l'inclusion"
                        + " externe absente se saute, comptée)");
    }

}
