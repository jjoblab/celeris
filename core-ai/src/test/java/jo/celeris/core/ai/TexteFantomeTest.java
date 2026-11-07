package jo.celeris.core.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;

/**
 * La session de texte fantôme (F-01, ADR-012) : la proposition datée ne
 * s'affiche que fraîche, la frappe suivante refuse et annule, l'acceptation
 * se prend par mot, par ligne ou en bloc — le reste survit ancré pareil.
 *
 * <p>Le fil de publication est direct sauf mention : le modèle local
 * livre sur le fil de l'appelant, la pompe manuelle défère ce qui doit
 * arriver en retard.</p>
 */
class TexteFantomeTest {

    private static final java.util.concurrent.Executor DIRECT = Runnable::run;

    /** Une suggestion sans texte n'a rien à faire ici. */
    @Test
    void suggestionSansTexteRejetee() {
        assertThrows(IllegalArgumentException.class,
                () -> new SuggestionFantome("", 0, 0));
    }

    /** Une ancre négative ou une révision négative ne passe pas. */
    @Test
    void suggestionAuxBornesInvalidesRejetee() {
        assertThrows(IllegalArgumentException.class,
                () -> new SuggestionFantome("x", -1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new SuggestionFantome("x", 0, -1));
    }

    /** La proposition fraîche devient le fantôme visible. */
    @Test
    void propositionFraicheDevientVisible() {
        FauxModeleLocal modele = new FauxModeleLocal();
        SessionTexteFantome session = new SessionTexteFantome(modele, DIRECT);
        modele.fantome(new SuggestionFantome("42;", 8, 0));
        session.demander(Document.of("int x = "), 8);
        assertEquals(new SuggestionFantome("42;", 8, 0), session.suggestionActive());
    }

    /** La proposition d'une autre révision se jette — jamais ré-ancrée. */
    @Test
    void propositionPerimeeJetee() {
        FauxModeleLocal modele = new FauxModeleLocal();
        SessionTexteFantome session = new SessionTexteFantome(modele, DIRECT);
        modele.fantome(new SuggestionFantome("42;", 4, 7));
        session.demander(Document.of("int x = "), 8);
        assertNull(session.suggestionActive(),
                "une suggestion d'une autre révision ne s'affiche pas");
    }

    /** La proposition ancrée hors document se jette. */
    @Test
    void propositionHorsDocumentJetee() {
        FauxModeleLocal modele = new FauxModeleLocal();
        SessionTexteFantome session = new SessionTexteFantome(modele, DIRECT);
        modele.fantome(new SuggestionFantome("42;", 99, 0));
        session.demander(Document.of("int x = "), 8);
        assertNull(session.suggestionActive(),
                "une suggestion ancrée hors document ne s'affiche pas");
    }

    /** L'aveu du modèle ferme le fantôme visible. */
    @Test
    void aveuDuModeleFermeLeFantome() {
        FauxModeleLocal modele = new FauxModeleLocal();
        SessionTexteFantome session = new SessionTexteFantome(modele, DIRECT);
        modele.fantome(new SuggestionFantome("42;", 8, 0));
        session.demander(Document.of("int x = "), 8);
        modele.fantome(null);
        session.demander(Document.of("int x = "), 8);
        assertNull(session.suggestionActive(),
                "l'aveu qu'il n'y a rien ferme le fantôme");
    }

    /**
     * L'aveu d'un jeton mort ne ferme rien : le vol supplante est mort,
     * son aveu ne compte pas — seul l'aveu du vol vivant ferme le
     * fantôme.
     */
    @Test
    void aveuDunVolMortIgnore() {
        FauxModeleLocal modele = new FauxModeleLocal();
        PompeManuelle publication = new PompeManuelle();
        SessionTexteFantome session = new SessionTexteFantome(modele, publication);
        Document document = Document.of("int x = ");
        modele.fantome(new SuggestionFantome("42;", 8, 0));
        session.demander(document, 8);
        publication.executerTout();
        assertEquals(new SuggestionFantome("42;", 8, 0), session.suggestionActive());
        modele.fantome(null);
        session.demander(document, 8);
        modele.fantome(new SuggestionFantome("b;", 8, 0));
        session.demander(document, 8);
        publication.executerUne();
        assertEquals(new SuggestionFantome("42;", 8, 0), session.suggestionActive(),
                "l'aveu du vol mort (supplanté) ne ferme pas le fantôme frais");
        publication.executerUne();
        assertEquals(new SuggestionFantome("b;", 8, 0), session.suggestionActive(),
                "la réponse du vol vivant, elle, s'affiche");
    }

    /**
     * F-01 — l'acceptation par mot : le mot, ses espaces meneuses et
     * l'espace qui le sépare du suivant ; le reste survit ancré pareil,
     * jusqu'à épuisement.
     */
    @Test
    void acceptationParMot() {
        FauxModeleLocal modele = new FauxModeleLocal();
        SessionTexteFantome session = new SessionTexteFantome(modele, DIRECT);
        modele.fantome(new SuggestionFantome("mot suivant ligne", 3, 0));
        session.demander(Document.of("ab "), 3);
        assertEquals("mot ", session.accepterMot());
        assertEquals(new SuggestionFantome("suivant ligne", 3, 0),
                session.suggestionActive());
        assertEquals("suivant ", session.accepterMot());
        assertEquals(new SuggestionFantome("ligne", 3, 0), session.suggestionActive());
        assertEquals("ligne", session.accepterMot());
        assertNull(session.suggestionActive(), "le fantôme épuisé disparaît");
        assertEquals("", session.accepterMot(), "épuisé, l'acceptation ne rend rien");
    }

    /** Le mot meneur emporte ses espaces et les sauts de ligne. */
    @Test
    void acceptationParMot_espacesEtSauts() {
        FauxModeleLocal modele = new FauxModeleLocal();
        SessionTexteFantome session = new SessionTexteFantome(modele, DIRECT);
        modele.fantome(new SuggestionFantome("  retrait\nsuite", 0, 0));
        session.demander(Document.of(""), 0);
        assertEquals("  retrait\n", session.accepterMot());
        assertEquals("suite", session.accepterMot());
        assertNull(session.suggestionActive());
    }

    /** Un fantôme tout en blancs se prend d'un mot — entier. */
    @Test
    void acceptationParMot_toutBlancs() {
        FauxModeleLocal modele = new FauxModeleLocal();
        SessionTexteFantome session = new SessionTexteFantome(modele, DIRECT);
        modele.fantome(new SuggestionFantome("   ", 0, 0));
        session.demander(Document.of(""), 0);
        assertEquals("   ", session.accepterMot());
        assertNull(session.suggestionActive());
    }

    /**
     * F-01 — l'acceptation par ligne : jusqu'au premier saut compris ;
     * sans saut, tout le reste.
     */
    @Test
    void acceptationParLigne() {
        FauxModeleLocal modele = new FauxModeleLocal();
        SessionTexteFantome session = new SessionTexteFantome(modele, DIRECT);
        modele.fantome(new SuggestionFantome("premiere ligne\ndeuxieme", 0, 0));
        session.demander(Document.of(""), 0);
        assertEquals("premiere ligne\n", session.accepterLigne());
        assertEquals(new SuggestionFantome("deuxieme", 0, 0), session.suggestionActive());
        assertEquals("deuxieme", session.accepterLigne(), "sans saut, le reste entier");
        assertNull(session.suggestionActive());
    }

    /** F-01 — l'acceptation en bloc : tout, d'un geste. */
    @Test
    void acceptationEnBloc() {
        FauxModeleLocal modele = new FauxModeleLocal();
        SessionTexteFantome session = new SessionTexteFantome(modele, DIRECT);
        modele.fantome(new SuggestionFantome("a\nb c", 2, 0));
        session.demander(Document.of("x "), 2);
        assertEquals("a\nb c", session.accepterTout());
        assertNull(session.suggestionActive());
    }

    /** Sans fantôme, l'acceptation ne rend rien — jamais ne plante. */
    @Test
    void acceptationSansFantomeNeRendRien() {
        SessionTexteFantome session =
                new SessionTexteFantome(new FauxModeleLocal(), DIRECT);
        assertEquals("", session.accepterMot());
        assertEquals("", session.accepterLigne());
        assertEquals("", session.accepterTout());
        assertNull(session.suggestionActive());
    }

    /**
     * F-01, F-09 — la frappe suivante refuse le fantôme et annule le
     * vol : l'annulation est immédiate, l'attente ne bloque jamais la
     * frappe.
     */
    @Test
    void frappeRefuseLeFantomeEtAnnuleLeVol() {
        FauxModeleLocal modele = new FauxModeleLocal();
        SessionTexteFantome session = new SessionTexteFantome(modele, DIRECT);
        modele.fantome(new SuggestionFantome("42;", 8, 0));
        session.demander(Document.of("int x = "), 8);
        session.frappe();
        assertNull(session.suggestionActive(), "la frappe suivante refuse le fantôme");
        assertTrue(modele.requetes().get(0).jeton().estAnnule(),
                "le vol est annulé à la frappe");
    }

    /** Le refus explicite — clavier ou geste — ferme pareil. */
    @Test
    void refusExpliciteFermeEtAnnule() {
        FauxModeleLocal modele = new FauxModeleLocal();
        SessionTexteFantome session = new SessionTexteFantome(modele, DIRECT);
        modele.fantome(new SuggestionFantome("42;", 8, 0));
        session.demander(Document.of("int x = "), 8);
        session.refuser();
        assertNull(session.suggestionActive());
        assertTrue(modele.requetes().get(0).jeton().estAnnule());
    }

    /**
     * F-09 — la demande suivante supplante la précédente : son jeton
     * meurt, seule la réponse fraîche s'affiche.
     */
    @Test
    void demandeSuivanteSupplanteLaPrecedente() {
        FauxModeleLocal modele = new FauxModeleLocal();
        SessionTexteFantome session = new SessionTexteFantome(modele, DIRECT);
        Document document = Document.of("int x = ");
        modele.fantome(new SuggestionFantome("41;", 8, 0));
        session.demander(document, 8);
        modele.fantome(new SuggestionFantome("42;", 8, 0));
        session.demander(document, 8);
        assertTrue(modele.requetes().get(0).jeton().estAnnule(),
                "la première requête meurt supplémentée");
        assertEquals(new SuggestionFantome("42;", 8, 0), session.suggestionActive());
    }

    /**
     * La livraison qui arrive après la frappe ne s'affiche pas : le
     * fantôme refusé reste refusé, même retard.
     */
    @Test
    void livraisonRetardeeApresLaFrappeJetee() {
        PompeManuelle reseau = new PompeManuelle();
        FauxModeleDistant modele = new FauxModeleDistant(reseau);
        SessionTexteFantome session = new SessionTexteFantome(modele, DIRECT);
        modele.fantome(new SuggestionFantome("42;", 8, 0));
        session.demander(Document.of("int x = "), 8);
        session.frappe();
        reseau.executerTout();
        assertNull(session.suggestionActive(),
                "la livraison d'un vol mort ne s'affiche pas");
    }

    /** L'acceptation clôt le vol : le retard ne remplace pas le reste. */
    @Test
    void acceptationClotLeVol() {
        PompeManuelle reseau = new PompeManuelle();
        FauxModeleDistant modele = new FauxModeleDistant(reseau);
        SessionTexteFantome session = new SessionTexteFantome(modele, DIRECT);
        Document document = Document.of("");
        modele.fantome(new SuggestionFantome("mot suite", 0, 0));
        session.demander(document, 0);
        reseau.executerUne();
        modele.fantome(new SuggestionFantome("mot suite ENTIER", 0, 0));
        session.demander(document, 0);
        assertEquals("mot ", session.accepterMot());
        reseau.executerTout();
        assertEquals(new SuggestionFantome("suite", 0, 0), session.suggestionActive(),
                "le retard du vol clôturé ne remplace pas le reste accepté");
    }

    /** L'ancre ne survit pas aux bornes du document de la requête. */
    @Test
    void requeteHorsDocumentRejetee() {
        FauxModeleLocal modele = new FauxModeleLocal();
        SessionTexteFantome session = new SessionTexteFantome(modele, DIRECT);
        assertThrows(IllegalArgumentException.class,
                () -> session.demander(Document.of("ab"), 9));
    }
}
