package jo.celeris.core.lsp;

import jo.celeris.core.lsp.types.EditionDocumentLsp;
import jo.celeris.core.lsp.types.EditionTexteLsp;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.Transaction;
import jo.celeris.core.lsp.types.DiagnosticLsp;
import jo.celeris.core.lsp.types.EditionEspaceLsp;
import jo.celeris.core.lsp.types.EditionGabaritLsp;
import jo.celeris.core.lsp.types.IndiceLsp;
import jo.celeris.core.lsp.types.LieuLsp;
import jo.celeris.core.lsp.types.PlageLsp;
import jo.celeris.core.lsp.types.PositionLsp;
import jo.celeris.core.lsp.types.SurvolLsp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Les types du domaine par construction directe (ADR-015) : la
 * correspondance de la frontière protocolaire dépose des valeurs
 * résolues — diagnostic, lieu, survol, indice, édit de gabarit — et
 * chaque type rend exactement ce qu'il porte. Les champs absents
 * restent nuls ou vides, jamais devinés ; les listes se figent à la
 * construction, profondes pour l'auditeur.
 */
class TypesDomaineLspTest {

    /** Une plage inversée du serveur se transpose sans exception : les
     * bornes s'ordonnent, la doctrine du clampage tient jusqu'au bout. */
    @Test
    void laPlageInverseeDuServeurSetranspose() {
        Document doc = Document.of("abcdef");
        EditionDocumentLsp edition = new EditionDocumentLsp("file:///f", 1,
                List.of(new EditionTexteLsp(new PlageLsp(
                        new PositionLsp(0, 4), new PositionLsp(0, 2)),
                        "XY")));
        Transaction transaction = EditsEspaceLsp.transaction(doc, edition);
        assertEquals(1, transaction.edits().size());
        assertEquals("cd", transaction.edits().get(0).removed(),
                "les bornes inversées s'ordonnent : le texte retiré est le bon");
        assertEquals("XY", transaction.edits().get(0).inserted());
    }

    /** Le diagnostic : la gravité porte ses constantes, les champs ses valeurs. */
    @Test
    void leDiagnosticPorteSesChamps() {
        PlageLsp plage = new PlageLsp(new PositionLsp(1, 2), new PositionLsp(3, 4));
        DiagnosticLsp diagnostic = new DiagnosticLsp(plage,
                DiagnosticLsp.AVERTISSEMENT, "balise non fermée", "E42", "analyseur");
        assertEquals(plage, diagnostic.plage());
        assertEquals(DiagnosticLsp.AVERTISSEMENT, diagnostic.gravite());
        assertEquals("balise non fermée", diagnostic.message());
        assertEquals("E42", diagnostic.code());
        assertEquals("analyseur", diagnostic.source());
        assertEquals(1, DiagnosticLsp.ERREUR);
        assertEquals(2, DiagnosticLsp.AVERTISSEMENT);
        // l'égalité par valeur : deux constructions identiques se valent
        assertEquals(diagnostic, new DiagnosticLsp(plage, 2,
                "balise non fermée", "E42", "analyseur"));
    }

    /** Le lieu et sa plage : la cible se relit, la plage absente reste nulle. */
    @Test
    void leLieuPorteSaCible() {
        LieuLsp lieu = new LieuLsp("file:///src/Calcul.kt",
                new PlageLsp(new PositionLsp(40, 2), new PositionLsp(40, 9)));
        assertEquals("file:///src/Calcul.kt", lieu.uri());
        PlageLsp plage = lieu.plage();
        assertNotNull(plage);
        assertEquals(new PositionLsp(40, 2), plage.debut());
        assertEquals(new PositionLsp(40, 9), plage.fin());
        assertNull(new LieuLsp("file:///src/Calcul.kt", null).plage(),
                "la plage absente reste nulle, jamais devinée");
    }

    /** Le survol : le vide ne devine rien, le balisé porte son genre et sa plage. */
    @Test
    void leSurvolNeDevineRien() {
        assertEquals("", SurvolLsp.VIDE.texte());
        assertEquals("", SurvolLsp.VIDE.genre());
        assertNull(SurvolLsp.VIDE.plage());
        SurvolLsp balise = new SurvolLsp("documentation du symbole", "markdown",
                new PlageLsp(new PositionLsp(2, 10), new PositionLsp(2, 17)));
        assertEquals("documentation du symbole", balise.texte());
        assertEquals("markdown", balise.genre());
        assertNotNull(balise.plage());
        assertEquals(new PositionLsp(2, 10), balise.plage().debut());
    }

    /**
     * L'indice et ses parties : le libellé en parties typées, la cible
     * cliquable de navigation, les remplissages — la liste des parties
     * se fige, jamais muable derrière le dos de l'auditeur.
     */
    @Test
    void lIndicePorteSesPartiesFigees() {
        LieuLsp cible = new LieuLsp("file:///src/Calcul.kt", null);
        List<IndiceLsp.Partie> parties = new ArrayList<>();
        parties.add(new IndiceLsp.Partie("a", null));
        parties.add(new IndiceLsp.Partie(":", cible));
        IndiceLsp indice = new IndiceLsp(new PositionLsp(3, 18), parties,
                1, true, false);
        assertEquals(new PositionLsp(3, 18), indice.position());
        assertEquals(2, indice.parties().size());
        assertEquals("a", indice.parties().get(0).valeur());
        assertNull(indice.parties().get(0).cible());
        assertEquals(cible, indice.parties().get(1).cible(),
                "la partie « : » porte sa cible de navigation");
        assertEquals(1, indice.genre());
        assertTrue(indice.remplissageGauche());
        assertFalse(indice.remplissageDroite());
        // la liste se fige : la mutation de l'originale n'atteint pas l'indice
        parties.add(new IndiceLsp.Partie(" Int", null));
        assertEquals(2, indice.parties().size());
        assertThrows(UnsupportedOperationException.class,
                () -> indice.parties().add(new IndiceLsp.Partie("x", null)));
    }

    /**
     * L'édit de gabarit (B-17) : la forme nôtre du protocole — la
     * sorte porte son nom, le corps ses trous.
     */
    @Test
    void leGabaritPorteSaSorte() {
        assertEquals("celeris/templateEdit", EditionGabaritLsp.SORTE);
        EditionGabaritLsp gabarit = new EditionGabaritLsp("file:///src/Calcul.kt",
                new PositionLsp(20, 4), "fun ${nom:soustraire}(${a: Int}): Int {}");
        assertEquals("file:///src/Calcul.kt", gabarit.uri());
        assertEquals(new PositionLsp(20, 4), gabarit.position());
        assertEquals("fun ${nom:soustraire}(${a: Int}): Int {}", gabarit.corps());
    }

    /** L'espace de travail vide : aucun document, aucun gabarit — et l'égalité par valeur. */
    @Test
    void lespaceDeTravailVideNePorteRien() {
        assertTrue(EditionEspaceLsp.VIDE.documents().isEmpty());
        assertTrue(EditionEspaceLsp.VIDE.gabarits().isEmpty());
        assertEquals(EditionEspaceLsp.VIDE, new EditionEspaceLsp(List.of(), List.of()));
    }
}
