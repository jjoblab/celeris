package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jo.celeris.core.text.Document;

import org.junit.jupiter.api.Test;

/**
 * Les repères {@code @Preview} de Compose (C-11) : chaque annotation
 * ancre un repère à sa ligne — arguments compris —, le nom du
 * composable se lit sur la ligne même ou juste en dessous, et le clic
 * sur le repère délègue le rendu à l'hôte.
 */
class ReperesPreviewTest {

    @Test
    void chaqueAnnotationAncreUnRepere() {
        List<PreviewMarks.PreviewMark> marks = PreviewMarks.ofLines(List.of(
                "package com.exemple",
                "",
                "@Preview",
                "@Composable",
                "fun CarteFichier(fichier: Carte) {",
                "}",
                "",
                "@Preview(showBackground = true)",
                "fun EcranAccueil() {",
                "}"));
        assertEquals(2, marks.size());
        assertEquals(2, marks.get(0).line());
        assertEquals(7, marks.get(1).line());
    }

    @Test
    void leNomDuComposableSeLitSurLesLignesSuivantes() {
        List<PreviewMarks.PreviewMark> marks = PreviewMarks.ofLines(List.of(
                "@Preview",
                "@Composable",
                "fun CarteFichier(fichier: Carte) {"));
        assertEquals("CarteFichier", marks.get(0).composable());
        assertEquals("CarteFichier", marks.get(0).label());
    }

    @Test
    void leNomSurLaLigneMemeCompte() {
        List<PreviewMarks.PreviewMark> marks = PreviewMarks.ofLines(List.of(
                "@Preview fun EcranAccueil() {"));
        assertEquals("EcranAccueil", marks.get(0).composable());
    }

    @Test
    void leNomIllisibleRendLeNumeroDeLigne() {
        List<PreviewMarks.PreviewMark> marks = PreviewMarks.ofLines(List.of(
                "@Preview",
                "@Composable"));
        assertEquals("", marks.get(0).composable());
        assertEquals("ligne 1", marks.get(0).label());
    }

    @Test
    void leFichierSansAnnotationNePorteAucunRepere() {
        assertTrue(PreviewMarks.ofLines(
                List.of("fun ordinaire() {", "}")).isEmpty());
    }

    @Test
    void lesReperesViventDansLesMarquesEtLeClicDelegue() {
        GutterIntegrations marks = new GutterIntegrations();
        assertFalse(marks.reperePreview(2));
        marks.setPreviewMarks(new int[] {2, 7});
        assertTrue(marks.reperePreview(2));
        assertTrue(marks.reperePreview(7));
        assertFalse(marks.reperePreview(3));

        int[] rendues = {-1};
        marks.setSurRenduPreview(line -> rendues[0] = line);
        Document document = Document.of("l0\nl1\nl2\nl3\nl4\nl5\nl6\nl7");
        VisualProjection projection = new VisualProjection(document, new FoldModel(), 0);
        // Le clic dans la coulisse sur la ligne du repère délègue.
        assertTrue(marks.onMarkPress(5f, 2 * 16f + 8f, 16f, 0f, 16f,
                projection));
        assertEquals(2, rendues[0]);
        // Le clic hors repère ne se consomme pas.
        assertFalse(marks.onMarkPress(5f, 3 * 16f + 8f, 16f, 0f, 16f,
                projection));
        assertEquals(2, rendues[0]);
        // Le clic hors coulisse ne se consomme pas.
        assertFalse(marks.onMarkPress(30f, 2 * 16f + 8f, 16f, 0f, 16f,
                projection));
    }
}
