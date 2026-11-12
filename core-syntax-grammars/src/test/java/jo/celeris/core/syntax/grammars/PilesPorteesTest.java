package jo.celeris.core.syntax.grammars;

import jo.celeris.core.syntax.scope.ScopeStacks;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.LexState;
import jo.celeris.core.syntax.LexedLine;

import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;

/**
 * Les piles de portées du lexeur de grammaires (M7.6, H-07) : chaque
 * jeton porte sa PILE internée — la racine du langage, les blocs
 * ouverts, sa portée propre — la couleur s'y résout par le thème, la
 * sorte n'en reste que la catégorie dérivée. La correspondance des
 * têtes (l'ancienne voie {@code Portees.de}) se garde ici : les
 * consommateurs hérités ne changent pas.
 */
final class PilesPorteesTest {

    /** La grammaire du paquet python, chargée par le vrai chargeur. */
    private static Grammaire grammairePython() {
        var flux = PilesPorteesTest.class.getResourceAsStream(
                "/langages/python/grammar.json");
        if (flux == null) {
            throw new IllegalArgumentException("grammaire du paquet python");
        }
        try (flux) {
            return Grammaires.lire(new String(flux.readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void leJetonPorteSaPileDeLaRacineASaPortee() {
        GrammaireLexeur lexeur = new GrammaireLexeur(grammairePython());
        LexedLine ligne = lexeur.lex("import os", lexeur.initialState());
        assertTrue(ligne.tokens().length > 0);
        for (Token jeton : ligne.tokens()) {
            assertTrue(jeton.scope() != ScopeStacks.NONE,
                    "chaque jeton de la voie des grammaires porte sa pile");
            List<String> portees = ScopeStacks.scopesOf(jeton.scope());
            assertEquals("source.python", portees.get(0),
                    "la pile commence à la racine du langage");
        }
    }

    @Test
    void laChainePorteLaPileDeSonBlocOuvert() {
        GrammaireLexeur lexeur = new GrammaireLexeur(grammairePython());
        LexState etat = lexeur.initialState();
        // la première ligne ouvre la chaîne triple, la seconde vit dedans
        LexedLine premiere = lexeur.lex("doc = \"\"\"", etat);
        etat = premiere.exitState();
        LexedLine seconde = lexeur.lex("du texte", etat);
        Token texte = seconde.tokens()[0];
        List<String> portees = ScopeStacks.scopesOf(texte.scope());
        assertTrue(portees.contains("string.quoted.triple.double.python"),
                "l'écart vit dans la pile de son bloc : " + portees);
        assertEquals(TokenKind.STRING, texte.kind(),
                "la sorte dérivée de la pile reste la catégorie commune");
    }

    @Test
    void lesJetonsDuneMemePorteePartagentLeurPile() {
        GrammaireLexeur lexeur = new GrammaireLexeur(grammairePython());
        LexedLine ligne = lexeur.lex("import os", lexeur.initialState());
        Token premier = ligne.tokens()[0];
        LexedLine relue = lexeur.lex("import os", lexeur.initialState());
        assertEquals(premier.scope(), relue.tokens()[0].scope(),
                "la pile internée est déterministe : même texte, même entier");
    }
}
