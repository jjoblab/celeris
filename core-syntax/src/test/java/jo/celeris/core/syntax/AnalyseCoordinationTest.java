package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.TextSession;

/**
 * La coordination session ↔ analyse (ADR-004 : l'arbre est versionné par
 * instantané de document, annulable et rejouable) : le coordinateur écoute
 * la voie de transaction unique, analyse paresseusement à la première
 * demande puis par incrément à chaque édition — y compris le rejeu de
 * l'annulation (PIEGE-07 : l'auditeur est notifié du rejeu, l'analyse suit
 * la même voie que tous les consommateurs).
 */
class AnalyseCoordinationTest {

    private static final String TEXTE = "fn a {\n"
            + "  x\n"
            + "}\n"
            + "var b ;\n"
            + "fn c {\n"
            + "  y\n"
            + "}\n";

    /** L'analyse naît paresseusement, puis suit chaque édition par incrément. */
    @Test
    void analyseParesseusePuisIncrementale() {
        TextSession session = TextSession.of(TEXTE, () -> 0L);
        SyntaxCoordinator coordinateur = new SyntaxCoordinator(session, MiniLangage.langage());
        Analysis premiere = coordinateur.analysis();
        assertEquals(session.document().revision(), premiere.revision());
        assertEquals(3, premiere.tree().chunks().size());

        int cible = TEXTE.indexOf("  y");
        session.replaceRange(cible, cible + 3, "  omega");
        Analysis seconde = coordinateur.analysis();
        assertEquals(session.document().revision(), seconde.revision());
        assertSame(premiere.lex().line(0), seconde.lex().line(0),
                "la ligne non éditée garde son panier — preuve d'incrément");
        Arbres.verifierEquivalence(seconde.tree(),
                SyntaxService.of(MiniLangage.langage()).analyseFull(session.document()).tree(),
                session.document());

        coordinateur.close();
    }

    /** L'annulation rejoue une édition : l'analyse suit et rejoint le complet. */
    @Test
    void annulationRejoueeLAnalyseSuit() {
        TextSession session = TextSession.of(TEXTE, () -> 0L);
        SyntaxCoordinator coordinateur = new SyntaxCoordinator(session, MiniLangage.langage());
        Analysis premiere = coordinateur.analysis();

        int cible = TEXTE.indexOf("var");
        session.replaceRange(cible, cible + 3, "let");
        Analysis apresEdition = coordinateur.analysis();
        assertEquals(3, apresEdition.tree().chunks().size());

        session.undo();
        Analysis apresAnnulation = coordinateur.analysis();
        assertEquals(session.document().revision(), apresAnnulation.revision());
        Arbres.verifierEquivalence(apresAnnulation.tree(), premiere.tree(),
                session.document());
        Arbres.verifierEgalite(apresAnnulation.lex(), premiere.lex());

        coordinateur.close();
    }

    /** Après fermeture, le coordinateur ne suit plus les éditions. */
    @Test
    void fermetureStopLeSuivi() {
        TextSession session = TextSession.of(TEXTE, () -> 0L);
        SyntaxCoordinator coordinateur = new SyntaxCoordinator(session, MiniLangage.langage());
        Analysis premiere = coordinateur.analysis();
        int revision = premiere.revision();
        coordinateur.close();

        int cible = TEXTE.indexOf("  x");
        session.replaceRange(cible, cible + 3, "  z");
        assertEquals(revision, coordinateur.analysis().revision(),
                "l'auditeur détaché : la dernière analyse reste celle d'avant");
    }

    /** Une session jamais interrogée ne construit jamais l'arbre. */
    @Test
    void jamaisInterrogeeJamaisConstruite() {
        TextSession session = TextSession.of(TEXTE, () -> 0L);
        SyntaxCoordinator coordinateur = new SyntaxCoordinator(session, MiniLangage.langage());
        int cible = TEXTE.indexOf("  x");
        session.replaceRange(cible, cible + 3, "  z");
        Analysis analyse = coordinateur.analysis();
        assertTrue(analyse.revision() >= 1);
        assertEquals(3, analyse.tree().chunks().size(),
                "l'analyse différée voit l'état courant, pas l'état initial");
        coordinateur.close();
    }
}
