package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jo.celeris.core.text.Document;

import org.junit.jupiter.api.Test;

/**
 * Tests du suivi des plis à l'édition (A-15) : le pli manuel survit à
 * l'édition voisine — il se décale quand le document bouge ailleurs, il
 * s'étend quand son corps s'épaissit, il meurt quand sa ligne d'en-tête
 * disparaît ou que son corps se vide.
 *
 * <p>Le rejeu de l'annulation traverse la même voie (PIEGE-07) : annuler
 * l'édition qui avait décalé le pli le ramène à sa place.</p>
 */
class FoldTrackerTest {

    private static FoldModel.Pli pli(FoldTracker suivi, int enTete) {
        for (FoldModel.Pli pli : suivi.modele().plis()) {
            if (pli.debut() == enTete) {
                return pli;
            }
        }
        throw new AssertionError("aucun pli porté par l'en-tête " + enTete);
    }

    private static void edite(FoldTracker suivi, Document avant, int start, int end,
            String retire, String insere) {
        suivi.surEdition(avant, start, end, retire, insere);
    }

    /** Une insertion de ligne AVANT le pli décale le pli d'autant. */
    @Test
    void insertionAvantLePliLeDecale() {
        FoldTracker suivi = new FoldTracker();
        suivi.fold(4, 9);
        // "a\nb\nc\nd\ne…" : insertion d'une ligne nouvelle en tête de ligne 1.
        Document avant = Document.of("l0\nl1\nl2\nl3\nl4\nl5\nl6\nl7\nl8\nl9\nl10");
        edite(suivi, avant, 3, 3, "", "nouvelle\n");
        assertEquals(5, pli(suivi, 5).debut());
        assertEquals(10, pli(suivi, 5).fin());
    }

    /** Une insertion APRÈS le pli ne touche pas le pli. */
    @Test
    void insertionApresLePliNeLeTouchePas() {
        FoldTracker suivi = new FoldTracker();
        suivi.fold(1, 3);
        Document avant = Document.of("l0\nl1\nl2\nl3\nl4");
        edite(suivi, avant, avant.length(), avant.length(), "", "\ntout en bas");
        assertEquals(1, pli(suivi, 1).debut());
        assertEquals(3, pli(suivi, 1).fin());
    }

    /** Une insertion au sein du corps épaissit le pli. */
    @Test
    void insertionDansLeCorpsEpaisseLePli() {
        FoldTracker suivi = new FoldTracker();
        suivi.fold(1, 4);
        Document avant = Document.of("l0\nl1\nl2\nl3\nl4\nl5");
        edite(suivi, avant, 8, 8, "", "nouvelle\nnouvelle2\n");
        assertEquals(1, pli(suivi, 1).debut());
        assertEquals(6, pli(suivi, 1).fin(), "le corps gagne deux lignes");
    }

    /** Une insertion en fin de la ligne d'en-tête n'y déplace pas l'en-tête. */
    @Test
    void insertionEnFinDeLigneDEnteteEtendLeCorps() {
        FoldTracker suivi = new FoldTracker();
        suivi.fold(1, 3);
        Document avant = Document.of("l0\nl1\nl2\nl3\nl4");
        // insertion à la fin de la ligne 1 (juste avant son saut de ligne)
        int finLigne1 = avant.lineEnd(1);
        edite(suivi, avant, finLigne1, finLigne1, "", "x\ny\n");
        assertEquals(1, pli(suivi, 1).debut(),
                "l'en-tête reste la ligne 1 : l'insertion s'attache après lui");
        assertEquals(5, pli(suivi, 1).fin(), "le corps gagne les deux lignes insérées");
    }

    /** Une insertion au milieu de la ligne d'en-tête la coupe : le pli suit le contenu. */
    @Test
    void insertionAuMilieuDeLEnteteDecaleLePli() {
        FoldTracker suivi = new FoldTracker();
        suivi.fold(1, 3);
        Document avant = Document.of("l0\nen-tete\nl2\nl3\nl4");
        edite(suivi, avant, 4, 4, "", "x\n");
        // la ligne d'en-tête coupe : son contenu glisse à la ligne 2, le pli suit
        assertEquals(2, pli(suivi, 2).debut());
        assertEquals(4, pli(suivi, 2).fin());
    }

    /** Une suppression de lignes dans le corps maigrit le pli. */
    @Test
    void suppressionDansLeCorpsMaigritLePli() {
        FoldTracker suivi = new FoldTracker();
        suivi.fold(1, 5);
        Document avant = Document.of("l0\nl1\nl2\nl3\nl4\nl5\nl6");
        // retire les lignes 3 et 4 ("l3\nl4\n")
        int debut = avant.lineStart(3);
        edite(suivi, avant, debut, debut + 6, "l3\nl4\n", "");
        assertEquals(1, pli(suivi, 1).debut());
        assertEquals(3, pli(suivi, 1).fin());
    }

    /** La suppression de la ligne d'en-tête tue le pli. */
    @Test
    void suppressionDeLEnteteTueLePli() {
        FoldTracker suivi = new FoldTracker();
        suivi.fold(2, 4);
        Document avant = Document.of("l0\nl1\nl2\nl3\nl4\nl5");
        int debut = avant.lineStart(2);
        edite(suivi, avant, debut, debut + 3, "l2\n", "");
        assertTrue(suivi.modele().plis().isEmpty(),
                "un pli sans sa ligne d'en-tête n'existe plus");
    }

    /** La suppression du corps original rétracte le pli sur la ligne résiduelle. */
    @Test
    void suppressionDuCorpsRetracteLePli() {
        FoldTracker suivi = new FoldTracker();
        suivi.fold(1, 3);
        Document avant = Document.of("l0\nl1\nl2\nl3\nl4");
        int debut = avant.lineStart(2);
        edite(suivi, avant, debut, avant.lineEnd(3), "l2\nl3", "");
        // le corps original disparaît, une ligne vide résiduelle demeure :
        // le pli se rétracte sur elle plutôt que de mourir
        assertEquals(1, suivi.modele().plis().size());
        assertEquals(1, pli(suivi, 1).debut());
        assertEquals(2, pli(suivi, 1).fin());
    }

    /** La suppression du corps ET de ses sauts de ligne tue le pli (corps vidé). */
    @Test
    void suppressionDuCorpsEtDeSesSautsTueLePli() {
        FoldTracker suivi = new FoldTracker();
        suivi.fold(1, 3);
        Document avant = Document.of("l0\nl1\nl2\nl3\nl4");
        int debut = avant.lineStart(2);
        edite(suivi, avant, debut, debut + 6, "l2\nl3\n", "");
        assertTrue(suivi.modele().plis().isEmpty(),
                "un pli au corps vidé n'a plus rien à cacher");
    }

    /** La frappe suivie (plusieurs éditions chaînées) décale le pli cumulé. */
    @Test
    void laFrappeChaineeCumuleLesDecalages() {
        FoldTracker suivi = new FoldTracker();
        suivi.fold(2, 6);
        String texte = "l0\nl1\nl2\nl3\nl4\nl5\nl6\nl7";
        Document avant = Document.of(texte);
        // trois retours à la ligne au début de la ligne 1, un par frappe
        int curseur = avant.lineStart(1);
        Document apres = avant;
        for (int i = 0; i < 3; i++) {
            edite(suivi, apres, curseur, curseur, "", "\n");
            apres = apres.replace(curseur, curseur, "\n");
        }
        assertEquals(5, pli(suivi, 5).debut());
        assertEquals(9, pli(suivi, 5).fin());
    }

    /** Le rejeu de l'annulation ramène le pli à sa place (PIEGE-07). */
    @Test
    void leRejeuDeLAnnulationRameneLePli() {
        FoldTracker suivi = new FoldTracker();
        suivi.fold(2, 4);
        Document avant = Document.of("l0\nl1\nl2\nl3\nl4\nl5");
        // une ligne insérée avant le pli…
        edite(suivi, avant, 3, 3, "", "x\n");
        assertEquals(3, pli(suivi, 3).debut());
        // …puis annulée : l'éditon inverse traverse la même voie
        Document apres = avant.replace(3, 3, "x\n");
        edite(suivi, apres, 3, 5, "x\n", "");
        assertEquals(2, pli(suivi, 2).debut());
        assertEquals(4, pli(suivi, 2).fin());
    }

    /** Le pli replié reste replié : le suivi déplace les bornes, jamais l'état. */
    @Test
    void lePliReplieResteReplie() {
        FoldTracker suivi = new FoldTracker();
        suivi.fold(1, 3);
        Document avant = Document.of("l0\nl1\nl2\nl3\nl4");
        edite(suivi, avant, 0, 0, "", "t\n");
        FoldModel modele = suivi.modele();
        assertEquals(1, modele.plis().size());
        assertTrue(modele.isFolded(2), "l'état replié survit au décalage");
        assertEquals(4, pli(suivi, 2).fin());
    }

    /** Plusieurs plis, une édition : chacun suit sa règle propre. */
    @Test
    void plusieursPlisUneEdition() {
        FoldTracker suivi = new FoldTracker();
        suivi.fold(1, 2);
        suivi.fold(5, 7);
        Document avant = Document.of("l0\nl1\nl2\nl3\nl4\nl5\nl6\nl7\nl8");
        // insère deux lignes sur la ligne 3 (entre les deux plis)
        int debut = avant.lineStart(3);
        edite(suivi, avant, debut + 1, debut + 1, "", "a\nb\n");
        assertEquals(1, pli(suivi, 1).debut());
        assertEquals(2, pli(suivi, 1).fin(), "le pli au-dessus ne bouge pas");
        assertEquals(7, pli(suivi, 7).debut());
        assertEquals(9, pli(suivi, 7).fin(), "le pli au-dessous se décale de deux");
    }
}
