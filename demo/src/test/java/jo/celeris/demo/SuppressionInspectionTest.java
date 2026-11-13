package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jo.celeris.android.host.spi.AnalyseurLint;
import jo.celeris.core.intelligence.EditPlan;
import jo.celeris.core.text.TextSession;

import org.junit.jupiter.api.Test;

/**
 * La suppression d'inspection (catégorie B de l'inventaire — « Suppress
 * Inspection », le geste dormait, la marque naît) : le commentaire
 * « noinspection » se pose en fin de ligne, la transaction s'applique et
 * se défait d'un geste ; le faux build de lint HONORE la marque à sa
 * relance — la règle masquée ne se rapporte plus, les autres demeurent.
 * Pur, sans appareil.
 */
class SuppressionInspectionTest {

    @Test
    void laMarqueSePoseEnFinDeLigneEtSeDefait() {
        TextSession session = TextSession.of(
                "Attendez... la suite\napres\n", () -> 0L);
        EditPlan plan = SuppressionInspection.masquer(
                session.document(), 1, "TypographyEllipsis");
        assertNotNull(plan, "la ligne porteuse a son plan");
        assertTrue(session.apply(plan.transaction(), plan.selection()),
                "la transaction s'applique");
        assertEquals("Attendez... la suite // noinspection TypographyEllipsis\napres\n",
                session.document().text(),
                "la marque se pose en fin de ligne — les suivantes ne bougent pas");
        session.undo();
        assertEquals("Attendez... la suite\napres\n", session.document().text(),
                "un geste défait le masquage");
    }

    @Test
    void laLigneDejaMasqueeSeRefuse() {
        TextSession session = TextSession.of(
                "Attendez... // noinspection TypographyEllipsis\n", () -> 0L);
        assertNull(SuppressionInspection.masquer(session.document(), 1,
                        "TypographyEllipsis"),
                "la marque déjà posée ne se repose pas — le geste muet");
        assertNull(SuppressionInspection.masquer(session.document(), 9,
                        "TypographyEllipsis"),
                "la ligne hors document ne se masque pas");
    }

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
