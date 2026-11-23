package jo.celeris.core.syntax.grammars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.LexState;
import jo.celeris.core.syntax.LexedLine;
import jo.celeris.core.syntax.TokenKind;

/**
 * Le bornage du lexeur de grammaires (ADR-014, exigence du prompt) :
 * le budget par limite d'essais se dépense, se déborde et se compte —
 * au dépassement, la fin de ligne garde la portée de son contexte,
 * l'incident se journalise, jamais un gel de l'interface.
 */
class GrammaireBornageTest {

    /** Le budget épuisé dégrade la ligne en portée de contexte, comptée. */
    @Test
    void budgetDepasseDegradeEtCompte() {
        Grammar grammaire = grammaireDePaquet("python");
        GrammarLexer lexeur = new GrammarLexer(grammaire, 8);
        LexedLine line = lexeur.lex("if x >= 2:  # borne", GrammarState.EMPTY);
        assertEquals(1, lexeur.incidents(), "l'incident se compte");
        assertTrue(line.tokens().length > 0, "la ligne dégradée a des jetons quand même");
        boolean degrades = false;
        for (var token : line.tokens()) {
            if (token.kind() == TokenKind.IDENTIFIER) {
                degrades = true;
            }
        }
        assertTrue(degrades, "la fin de ligne retombe à la portée racine");
    }

    /** Le budget large ne se déclenche pas : ligne complète, aucun incident. */
    @Test
    void budgetLargeNeSeDeclenchePas() {
        Grammar grammaire = grammaireDePaquet("python");
        GrammarLexer lexeur = new GrammarLexer(grammaire, 20_000);
        lexeur.lex("if x >= 2:  # borne", GrammarState.EMPTY);
        assertEquals(0, lexeur.incidents());
    }

    /** Une seconde ligne repart à zéro : le budget est par ligne. */
    @Test
    void budgetParLigne() {
        Grammar grammaire = grammaireDePaquet("python");
        GrammarLexer lexeur = new GrammarLexer(grammaire, 60);
        lexeur.lex("if x >= 2:  # borne", GrammarState.EMPTY);
        assertEquals(1, lexeur.incidents());
        lexeur.lex("# court", GrammarState.EMPTY);
        assertEquals(1, lexeur.incidents(), "la ligne suivante repart à zéro");
    }

    /** Deux lignes qui débordent comptent deux incidents, une journalisation chacune. */
    @Test
    void deuxLignesQuiDebordentComptentDeux() {
        Grammar grammaire = grammaireDePaquet("python");
        GrammarLexer lexeur = new GrammarLexer(grammaire, 8);
        lexeur.lex("if x >= 2:  # borne", GrammarState.EMPTY);
        lexeur.lex("y = 3  # note", GrammarState.EMPTY);
        assertEquals(2, lexeur.incidents(), "chaque ligne qui déborde se compte");
    }

    /** Le rapport refuse les compteurs négatifs ; sans motif, tout est accepté. */
    @Test
    void rapportCoherent() {
        assertThrows(IllegalArgumentException.class,
                () -> new GrammarReport(-1, 0, 0, 0, 0, 0, 0, 0));
        assertEquals(1.0, new GrammarReport(0, 0, 0, 0, 0, 0, 0, 0).acceptation(),
                "sans expression, l'acceptation est entière");
    }

    /** L'état de sortie d'une ligne dégradée reste cohérent avec ses jetons. */
    @Test
    void etatCoherentApresDepassement() {
        Grammar grammaire = grammaireDePaquet("python");
        GrammarLexer lexeur = new GrammarLexer(grammaire, 6);
        LexState etat = GrammarState.EMPTY;
        LexedLine line = lexeur.lex("doc = \"\"\"", etat);
        assertEquals(((GrammarState) java.util.Objects.requireNonNull(line.exitState())).dansBloc(),
                true, "l'ouverture qui a matché reste ouverte à la sortie");
    }

    /** La grammaire d'un paquet intégré, chargée par le vrai chargeur. */
    private static Grammar grammaireDePaquet(String id) {
        var flux = GrammaireBornageTest.class.getResourceAsStream("/langages/" + id + "/grammar.json");
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
}
