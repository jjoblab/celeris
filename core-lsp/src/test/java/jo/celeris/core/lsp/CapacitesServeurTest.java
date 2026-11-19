package jo.celeris.core.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Les capacités du serveur par la fabrique typée (ADR-015) : la
 * correspondance de la frontière protocolaire y dépose ce que la
 * bibliothèque a négocié à la poignée de main — chaque champ se relit
 * tel que déposé, les capacités vides servent d'origine avant la
 * négociation.
 */
class CapacitesServeurTest {

    /** La fabrique typée : la surface complète se relit champ à champ. */
    @Test
    void laFabriqueTypee() {
        ServerCapsLsp capacites = ServerCapsLsp.of(2,
                true, true, true, true, true, true, true, true, true,
                true, true, true, true,
                List.of("keyword", "variable"), List.of("declaration"));
        assertEquals(2, capacites.syncKind());
        assertTrue(capacites.supportsCompletion());
        assertTrue(capacites.supportsHover());
        assertTrue(capacites.supportsSignature());
        assertTrue(capacites.supportsDefinition());
        assertTrue(capacites.supportsReferences());
        assertTrue(capacites.supportsHighlights());
        assertTrue(capacites.supportsSymbols());
        assertTrue(capacites.supportsFolds());
        assertTrue(capacites.supportsRangeSelection());
        assertTrue(capacites.supportsRename());
        assertTrue(capacites.supportsCodeActions());
        assertTrue(capacites.supportsFormatting());
        assertTrue(capacites.supportsInlayHints());
        assertEquals(List.of("keyword", "variable"), capacites.tokenTypes());
        assertEquals(List.of("declaration"), capacites.tokenModifiers());
    }

    /** Les capacités vides : l'origine avant la poignée de main, rien de deviné. */
    @Test
    void lesCapacitesVides() {
        ServerCapsLsp empty = ServerCapsLsp.empty();
        assertEquals(-1, empty.syncKind());
        assertFalse(empty.supportsCompletion());
        assertFalse(empty.supportsHover());
        assertFalse(empty.supportsSignature());
        assertFalse(empty.supportsDefinition());
        assertFalse(empty.supportsReferences());
        assertFalse(empty.supportsHighlights());
        assertFalse(empty.supportsSymbols());
        assertFalse(empty.supportsFolds());
        assertFalse(empty.supportsRangeSelection());
        assertFalse(empty.supportsRename());
        assertFalse(empty.supportsCodeActions());
        assertFalse(empty.supportsFormatting());
        assertFalse(empty.supportsInlayHints());
        assertEquals(List.of(), empty.tokenTypes());
        assertEquals(List.of(), empty.tokenModifiers());
    }
}
