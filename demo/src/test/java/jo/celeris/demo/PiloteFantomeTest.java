package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import jo.celeris.android.view.GesteFantome;
import jo.celeris.core.ai.FauxModeleLocal;
import jo.celeris.core.ai.SessionTexteFantome;
import jo.celeris.core.ai.SuggestionFantome;
import jo.celeris.core.text.TextSession;

/**
 * Le pilote du texte fantôme de la démonstration (F-01) : la boucle
 * complète sans appareil — la demande, l'acceptation par mot qui
 * décale l'ancre du reste, l'acceptation par ligne et en bloc, le
 * refus, la frappe qui ferme le fantôme (F-09).
 */
class PiloteFantomeTest {

    private PiloteFantome pilote(TextSession session, FauxModeleLocal modele) {
        return new PiloteFantome(session,
                new SessionTexteFantome(modele, Runnable::run));
    }

    /**
     * F-01 — l'acceptation par mot applique le fragment à l'ancre, le
     * reste survit ré-ancré au décalage : la vue n'affiche jamais une
     * ancre périmée, le mot suivant s'insère après le premier.
     */
    @Test
    void acceptationParMotDecaleLancreDuReste() {
        TextSession session = TextSession.of("val reponse = ", System::nanoTime);
        FauxModeleLocal modele = new FauxModeleLocal();
        PiloteFantome pilote = pilote(session, modele);
        modele.fantome(new SuggestionFantome("lireAuClavier() .premier()",
                14, 0));

        pilote.demander(session.document(), 14);
        assertNotNull(pilote.suggestionVue());

        pilote.surGeste(GesteFantome.ACCEPTER_MOT);
        assertEquals("val reponse = lireAuClavier() ",
                session.document().text(),
                "le premier mot et sa séparante s'insèrent à l'ancre");
        assertEquals(1, session.undoDepth(), "l'acceptation est annulable");

        SuggestionFantome reste = pilote.suggestionVue();
        assertNotNull(reste);
        assertEquals(".premier()", reste.texte(), "le reste survit");
        assertEquals(30, reste.offset(),
                "le reste se ré-ancre après le fragment accepté");

        pilote.surGeste(GesteFantome.ACCEPTER_MOT);
        assertEquals("val reponse = lireAuClavier() .premier()",
                session.document().text(),
                "le second mot s'insère à l'ancre décalée");
        assertNull(pilote.suggestionVue(), "tout accepté, le fantôme ferme");
    }

    /**
     * F-01 — l'acceptation par ligne prend le texte jusqu'au saut
     * compris, l'acceptation en bloc prend tout ; le refus ferme sans
     * rien écrire.
     */
    @Test
    void acceptationParLigneBlocEtRefus() {
        TextSession session = TextSession.of("val x = ", System::nanoTime);
        FauxModeleLocal modele = new FauxModeleLocal();
        PiloteFantome pilote = pilote(session, modele);
        modele.fantome(new SuggestionFantome("1\n    .premier()\n", 8, 0));

        pilote.demander(session.document(), 8);
        pilote.surGeste(GesteFantome.ACCEPTER_LIGNE);
        assertEquals("val x = 1\n", session.document().text(),
                "la ligne s'accepte jusqu'au saut compris");
        assertEquals("    .premier()\n", pilote.suggestionVue().texte());

        pilote.surGeste(GesteFantome.ACCEPTER_TOUT);
        assertEquals("val x = 1\n    .premier()\n",
                session.document().text(), "le bloc prend le reste entier");

        modele.fantome(new SuggestionFantome("suite()", 20, 2));
        pilote.demander(session.document(), 20);
        pilote.surGeste(GesteFantome.REFUSER);
        assertEquals("val x = 1\n    .premier()\n", session.document().text(),
                "le refus n'écrit rien");
        assertNull(pilote.suggestionVue());
    }

    /**
     * F-09 — la frappe suivante refuse le fantôme et annule le vol :
     * le pilote suit l'édition, la suggestion visible disparaît.
     */
    @Test
    void laFrappeSuivanteFermeLeFantome() {
        TextSession session = TextSession.of("val x = ", System::nanoTime);
        FauxModeleLocal modele = new FauxModeleLocal();
        PiloteFantome pilote = pilote(session, modele);
        modele.fantome(new SuggestionFantome("1", 8, 0));

        pilote.demander(session.document(), 8);
        assertNotNull(pilote.suggestionVue());

        session.type("1");
        pilote.surEdition();
        assertNull(pilote.suggestionVue(),
                "la frappe ferme le fantôme — le vol meurt avec");
    }
}
