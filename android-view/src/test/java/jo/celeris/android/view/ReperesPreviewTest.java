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
        List<ReperesPreview.ReperePreview> reperes = ReperesPreview.desLignes(List.of(
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
        assertEquals(2, reperes.size());
        assertEquals(2, reperes.get(0).ligne());
        assertEquals(7, reperes.get(1).ligne());
    }

    @Test
    void leNomDuComposableSeLitSurLesLignesSuivantes() {
        List<ReperesPreview.ReperePreview> reperes = ReperesPreview.desLignes(List.of(
                "@Preview",
                "@Composable",
                "fun CarteFichier(fichier: Carte) {"));
        assertEquals("CarteFichier", reperes.get(0).composable());
        assertEquals("CarteFichier", reperes.get(0).libelle());
    }

    @Test
    void leNomSurLaLigneMemeCompte() {
        List<ReperesPreview.ReperePreview> reperes = ReperesPreview.desLignes(List.of(
                "@Preview fun EcranAccueil() {"));
        assertEquals("EcranAccueil", reperes.get(0).composable());
    }

    @Test
    void leNomIllisibleRendLeNumeroDeLigne() {
        List<ReperesPreview.ReperePreview> reperes = ReperesPreview.desLignes(List.of(
                "@Preview",
                "@Composable"));
        assertEquals("", reperes.get(0).composable());
        assertEquals("ligne 1", reperes.get(0).libelle());
    }

    @Test
    void leFichierSansAnnotationNePorteAucunRepere() {
        assertTrue(ReperesPreview.desLignes(
                List.of("fun ordinaire() {", "}")).isEmpty());
    }

    @Test
    void lesReperesViventDansLesMarquesEtLeClicDelegue() {
        MarquesGouttiere marques = new MarquesGouttiere();
        assertFalse(marques.reperePreview(2));
        marques.setReperesPreview(new int[] {2, 7});
        assertTrue(marques.reperePreview(2));
        assertTrue(marques.reperePreview(7));
        assertFalse(marques.reperePreview(3));

        int[] rendues = {-1};
        marques.setSurRenduPreview(ligne -> rendues[0] = ligne);
        Document document = Document.of("l0\nl1\nl2\nl3\nl4\nl5\nl6\nl7");
        VisualProjection projection = new VisualProjection(document, new FoldModel(), 0);
        // Le clic dans la coulisse sur la ligne du repère délègue.
        assertTrue(marques.surAppuiRepere(5f, 2 * 16f + 8f, 16f, 0f, 16f,
                projection));
        assertEquals(2, rendues[0]);
        // Le clic hors repère ne se consomme pas.
        assertFalse(marques.surAppuiRepere(5f, 3 * 16f + 8f, 16f, 0f, 16f,
                projection));
        assertEquals(2, rendues[0]);
        // Le clic hors coulisse ne se consomme pas.
        assertFalse(marques.surAppuiRepere(30f, 2 * 16f + 8f, 16f, 0f, 16f,
                projection));
    }
}
