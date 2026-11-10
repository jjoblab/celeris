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
        CapacitesServeur capacites = CapacitesServeur.de(2,
                true, true, true, true, true, true, true, true, true,
                true, true, true, true,
                List.of("keyword", "variable"), List.of("declaration"));
        assertEquals(2, capacites.genreSynchro());
        assertTrue(capacites.sertCompletion());
        assertTrue(capacites.sertSurvol());
        assertTrue(capacites.sertSignature());
        assertTrue(capacites.sertDefinition());
        assertTrue(capacites.sertReferences());
        assertTrue(capacites.sertSurlignages());
        assertTrue(capacites.sertSymboles());
        assertTrue(capacites.sertPlis());
        assertTrue(capacites.sertSelectionPlages());
        assertTrue(capacites.sertRennomage());
        assertTrue(capacites.sertCorrectifs());
        assertTrue(capacites.sertFormatage());
        assertTrue(capacites.sertIndices());
        assertEquals(List.of("keyword", "variable"), capacites.typesJetons());
        assertEquals(List.of("declaration"), capacites.modificateursJetons());
    }

    /** Les capacités vides : l'origine avant la poignée de main, rien de deviné. */
    @Test
    void lesCapacitesVides() {
        CapacitesServeur vides = CapacitesServeur.vides();
        assertEquals(-1, vides.genreSynchro());
        assertFalse(vides.sertCompletion());
        assertFalse(vides.sertSurvol());
        assertFalse(vides.sertSignature());
        assertFalse(vides.sertDefinition());
        assertFalse(vides.sertReferences());
        assertFalse(vides.sertSurlignages());
        assertFalse(vides.sertSymboles());
        assertFalse(vides.sertPlis());
        assertFalse(vides.sertSelectionPlages());
        assertFalse(vides.sertRennomage());
        assertFalse(vides.sertCorrectifs());
        assertFalse(vides.sertFormatage());
        assertFalse(vides.sertIndices());
        assertEquals(List.of(), vides.typesJetons());
        assertEquals(List.of(), vides.modificateursJetons());
    }
}
