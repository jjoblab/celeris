package jo.celeris.core.syntax.grammars;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.LexedLine;

/**
 * Le chantier C3 de la feuille de route coloration (prompt du
 * 2026-11-22) : la fin de ligne VIRTUELLE — vingt grammaires livrées
 * portent des motifs qui exigent le {@code \n} après la ligne
 * (fermetures {@code \n?}, shebangs, gardes fin de ligne), la ligne
 * lexée ne le portait pas : ces motifs échouaient à la fin. Le modèle
 * tm4e ({@code Grammar.tokenizeLine} : {@code lineText += '\n'}) : le
 * buffer de recherche porte le saut APRÈS la ligne, une correspondance
 * qui l'inclut se ROGNE au retour vers les unités de code.
 */
final class FinDeLigneVirtuelleTest {

    /** La grammaire à fin de ligne souple : le shebang et sa fin. */
    private static final String GRAMMAIRE = """
            {"scopeName":"source.banc",
             "patterns":[
               {"match":"#!.*\\n?", "name":"comment.line.shebang.banc"},
               {"match":"[a-z]+", "name":"variable.other.banc"}
             ]}
            """;

    /**
     * Le motif à {@code \n} matche la fin : le shebang couvre toute la
     * ligne SANS déborder (la correspondance rognée au retour).
     */
    @Test
    void leMotifAFinDeLigneMatcheSansDeborder() {
        Grammar grammaire = Grammars.read(GRAMMAIRE);
        GrammarLexer lexeur = new GrammarLexer(grammaire);
        String texte = "#! /usr/bin/env banc";
        LexedLine ligne = lexeur.lex(texte, GrammarState.EMPTY);
        boolean couvert = false;
        for (var jeton : ligne.tokens()) {
            if (jeton.start() == 0 && jeton.end() == texte.length()) {
                couvert = true;
            }
        }
        assertTrue(couvert,
                "le shebang à fin \\\\n? couvre 0-" + texte.length()
                        + " sans déborder (avant C3, le motif échouait :"
                        + " la ligne ne portait pas le saut)");
    }

    /**
     * La ligne vide : le motif {@code ^\\s*\\n} des fermetures lua y
     * matche (la fin virtuelle vit dès la position 0) — le jeton vide
     * ne se pose pas, la ligne rend simplement vide.
     */
    @Test
    void laLigneVideAccepteLesFinsSouplement() {
        Grammar grammaire = Grammars.read(GRAMMAIRE);
        GrammarLexer lexeur = new GrammarLexer(grammaire);
        // La ligne vide se lex SANS exception : la fin virtuelle vit
        // après elle (le motif \\n? y trouve son saut).
        LexedLine ligne = lexeur.lex("", GrammarState.EMPTY);
        org.junit.jupiter.api.Assertions.assertNotNull(ligne,
                "la ligne vide se lex sans exception");
    }
}
