package jo.celeris.core.lsp;

import jo.celeris.core.lsp.types.DocumentEditLsp;
import jo.celeris.core.lsp.types.TextEditLsp;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.Transaction;
import jo.celeris.core.lsp.types.DiagnosticLsp;
import jo.celeris.core.lsp.types.WorkspaceEditLsp;
import jo.celeris.core.lsp.types.TemplateEditLsp;
import jo.celeris.core.lsp.types.InlayHintLsp;
import jo.celeris.core.lsp.types.LocationLsp;
import jo.celeris.core.lsp.types.RangeLsp;
import jo.celeris.core.lsp.types.LspPosition;
import jo.celeris.core.lsp.types.HoverLsp;
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
        DocumentEditLsp edit = new DocumentEditLsp("file:///f", 1,
                List.of(new TextEditLsp(new RangeLsp(
                        new LspPosition(0, 4), new LspPosition(0, 2)),
                        "XY")));
        Transaction transaction = WorkspaceEditsLsp.transaction(doc, edit);
        assertEquals(1, transaction.edits().size());
        assertEquals("cd", transaction.edits().get(0).removed(),
                "les bornes inversées s'ordonnent : le texte retiré est le bon");
        assertEquals("XY", transaction.edits().get(0).inserted());
    }

    /** Le diagnostic : la gravité porte ses constantes, les champs ses valeurs. */
    @Test
    void leDiagnosticPorteSesChamps() {
        RangeLsp range = new RangeLsp(new LspPosition(1, 2), new LspPosition(3, 4));
        DiagnosticLsp diagnostic = new DiagnosticLsp(range,
                DiagnosticLsp.AVERTISSEMENT, "balise non fermée", "E42", "analyseur");
        assertEquals(range, diagnostic.range());
        assertEquals(DiagnosticLsp.AVERTISSEMENT, diagnostic.severity());
        assertEquals("balise non fermée", diagnostic.message());
        assertEquals("E42", diagnostic.code());
        assertEquals("analyseur", diagnostic.source());
        assertEquals(1, DiagnosticLsp.ERREUR);
        assertEquals(2, DiagnosticLsp.AVERTISSEMENT);
        // l'égalité par valeur : deux constructions identiques se valent
        assertEquals(diagnostic, new DiagnosticLsp(range, 2,
                "balise non fermée", "E42", "analyseur"));
    }

    /** Le lieu et sa plage : la cible se relit, la plage absente reste nulle. */
    @Test
    void leLieuPorteSaCible() {
        LocationLsp lieu = new LocationLsp("file:///src/Calcul.kt",
                new RangeLsp(new LspPosition(40, 2), new LspPosition(40, 9)));
        assertEquals("file:///src/Calcul.kt", lieu.uri());
        RangeLsp range = lieu.range();
        assertNotNull(range);
        assertEquals(new LspPosition(40, 2), range.start());
        assertEquals(new LspPosition(40, 9), range.end());
        assertNull(new LocationLsp("file:///src/Calcul.kt", null).range(),
                "la plage absente reste nulle, jamais devinée");
    }

    /** Le survol : le vide ne devine rien, le balisé porte son genre et sa plage. */
    @Test
    void leSurvolNeDevineRien() {
        assertEquals("", HoverLsp.EMPTY.text());
        assertEquals("", HoverLsp.EMPTY.kind());
        assertNull(HoverLsp.EMPTY.range());
        HoverLsp balise = new HoverLsp("documentation du symbole", "markdown",
                new RangeLsp(new LspPosition(2, 10), new LspPosition(2, 17)));
        assertEquals("documentation du symbole", balise.text());
        assertEquals("markdown", balise.kind());
        assertNotNull(balise.range());
        assertEquals(new LspPosition(2, 10), balise.range().start());
    }

    /**
     * L'indice et ses parties : le libellé en parties typées, la cible
     * cliquable de navigation, les remplissages — la liste des parties
     * se fige, jamais muable derrière le dos de l'auditeur.
     */
    @Test
    void lIndicePorteSesPartiesFigees() {
        LocationLsp target = new LocationLsp("file:///src/Calcul.kt", null);
        List<InlayHintLsp.Part> parties = new ArrayList<>();
        parties.add(new InlayHintLsp.Part("a", null));
        parties.add(new InlayHintLsp.Part(":", target));
        InlayHintLsp indice = new InlayHintLsp(new LspPosition(3, 18), parties,
                1, true, false);
        assertEquals(new LspPosition(3, 18), indice.position());
        assertEquals(2, indice.parties().size());
        assertEquals("a", indice.parties().get(0).value());
        assertNull(indice.parties().get(0).target());
        assertEquals(target, indice.parties().get(1).target(),
                "la partie « : » porte sa cible de navigation");
        assertEquals(1, indice.kind());
        assertTrue(indice.remplissageGauche());
        assertFalse(indice.remplissageDroite());
        // la liste se fige : la mutation de l'originale n'atteint pas l'indice
        parties.add(new InlayHintLsp.Part(" Int", null));
        assertEquals(2, indice.parties().size());
        assertThrows(UnsupportedOperationException.class,
                () -> indice.parties().add(new InlayHintLsp.Part("x", null)));
    }

    /**
     * L'édit de gabarit (B-17) : la forme nôtre du protocole — la
     * sorte porte son nom, le corps ses trous.
     */
    @Test
    void leGabaritPorteSaSorte() {
        assertEquals("celeris/templateEdit", TemplateEditLsp.SORTE);
        TemplateEditLsp gabarit = new TemplateEditLsp("file:///src/Calcul.kt",
                new LspPosition(20, 4), "fun ${nom:soustraire}(${a: Int}): Int {}");
        assertEquals("file:///src/Calcul.kt", gabarit.uri());
        assertEquals(new LspPosition(20, 4), gabarit.position());
        assertEquals("fun ${nom:soustraire}(${a: Int}): Int {}", gabarit.body());
    }

    /** L'espace de travail vide : aucun document, aucun gabarit — et l'égalité par valeur. */
    @Test
    void lespaceDeTravailVideNePorteRien() {
        assertTrue(WorkspaceEditLsp.EMPTY.documents().isEmpty());
        assertTrue(WorkspaceEditLsp.EMPTY.templates().isEmpty());
        assertEquals(WorkspaceEditLsp.EMPTY, new WorkspaceEditLsp(List.of(), List.of()));
    }
}
