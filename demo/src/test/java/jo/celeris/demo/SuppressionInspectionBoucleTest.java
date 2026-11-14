package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jo.celeris.android.host.spi.AnalyseurLint;
import jo.celeris.core.intelligence.EditPlan;
import jo.celeris.core.intelligence.SuppressionInspection;
import jo.celeris.core.text.TextSession;

import org.junit.jupiter.api.Test;

/**
 * La boucle complète du masquage d'inspection (catégorie B — « Suppress
 * Inspection ») : le geste pose la marque, le FAUX BUILD de lint — le
 * harnais de test de la démonstration — honore la marque à sa relance :
 * la règle masquée ne se rapporte plus, les autres demeurent. Le moteur
 * pur vit dans core-intelligence, la boucle intègre l'analyseur de
 * l'hôte et le faux build du terrain.
 */
class SuppressionInspectionBoucleTest {

    @Test
    void laBoucleCompleteMasquePuisRelance() {
        TextSession session = TextSession.of(
                "Attendez... puis -- repartez\n", () -> 0L);
        assertEquals(2, AnalyseurLint.parser(FauxLint.sortie("l.txt",
                FauxLint.lignes(session.document().text()))).size(),
                "l'avant masquage rapporte les deux règles");
        EditPlan plan = SuppressionInspection.masquer(
                session.document(), 1, "TypographyEllipsis");
        assertTrue(session.apply(plan.transaction(), plan.selection()));
        assertEquals("""
                l.txt:1: Warning: Use an em-dash instead of two hyphens [TypographyDashes]
                """, FauxLint.sortie("l.txt",
                        FauxLint.lignes(session.document().text())),
                "la règle masquée ne se rapporte plus — l'autre demeure");
    }
}
