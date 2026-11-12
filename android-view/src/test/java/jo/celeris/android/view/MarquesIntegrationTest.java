package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jo.celeris.core.syntax.structure.FoldSpan;

import org.junit.jupiter.api.Test;

/**
 * Tests des marques d'intégration de la gouttière (D-03, D-04, D-05) :
 * les marques VCS par ligne, les points d'arrêt posés et désactivés,
 * la ligne courante et les lignes de pile — fusionnées dans les marques
 * de gouttière, la coulisse s'élargit quand les intégrations parlent.
 *
 * <p>Les lignes de la vue sont numérotées à partir de zéro (convention
 * de la vue et de l'arbre) ; la conversion depuis les contrats de
 * l'hôte, numérotés à partir de un, vit dans l'adaptateur de la
 * démonstration.</p>
 */
final class MarquesIntegrationTest {

    /** L'état vide : aucune marque, aucun arrêt, aucune pause. */
    @Test
    void etatVideNeMarqueRien() {
        assertTrue(MarquesIntegration.VIDE.estVide());
        assertEquals(MarqueLigne.INCHANGE, MarquesIntegration.VIDE.marqueVcs(3));
        assertEquals(EtatArret.AUCUN, MarquesIntegration.VIDE.arret(2));
        assertNull(MarquesIntegration.VIDE.ligneCourante());
        assertFalse(MarquesIntegration.VIDE.dansPile(1));
    }

    /** Les marques VCS se posent par ligne, inchangée par défaut. */
    @Test
    void marquesVcsParLigne() {
        MarquesIntegration marques = MarquesIntegration.of(
                Map.of(2, MarqueLigne.MODIFIE, 5, MarqueLigne.AJOUTE),
                Map.of(), null, Set.of());
        assertFalse(marques.estVide());
        assertEquals(MarqueLigne.MODIFIE, marques.marqueVcs(2));
        assertEquals(MarqueLigne.AJOUTE, marques.marqueVcs(5));
        assertEquals(MarqueLigne.INCHANGE, marques.marqueVcs(0));
    }

    /** Les arrêts portent leur état : posé ou désactivé, aucun par défaut. */
    @Test
    void arretsPoseEtDesactive() {
        MarquesIntegration marques = MarquesIntegration.of(
                Map.of(), Map.of(4, EtatArret.POSE, 7, EtatArret.DESACTIVE),
                null, Set.of());
        assertEquals(EtatArret.POSE, marques.arret(4));
        assertEquals(EtatArret.DESACTIVE, marques.arret(7));
        assertEquals(EtatArret.AUCUN, marques.arret(1));
    }

    /** La ligne courante et les lignes de pile se distinguent (D-05). */
    @Test
    void ligneCouranteEtPile() {
        MarquesIntegration marques = MarquesIntegration.of(
                Map.of(), Map.of(), 6, Set.of(6, 9));
        assertEquals(6, marques.ligneCourante());
        assertTrue(marques.dansPile(6));
        assertTrue(marques.dansPile(9));
        assertFalse(marques.dansPile(2));
    }

    /** La fusion garde les marques existantes et ajoute les intégrations. */
    @Test
    void fusionDansLesMarquesDeGouttiere() {
        FoldModel replis = new FoldModel();
        GutterMarks sansIntegration = GutterMarks.of(
                List.of(new FoldSpan(1, 3)), replis, new int[] {4});
        MarquesIntegration integration = MarquesIntegration.of(
                Map.of(2, MarqueLigne.MODIFIE), Map.of(3, EtatArret.POSE), 3,
                Set.of(3));
        GutterMarks fondues = sansIntegration.avec(integration);

        assertTrue(fondues.pliable(1), "les plis survivent à la fusion");
        assertTrue(fondues.erreur(4), "les erreurs survivent à la fusion");
        assertEquals(MarqueLigne.MODIFIE, fondues.marqueVcs(2));
        assertEquals(EtatArret.POSE, fondues.arret(3));
        assertEquals(3, fondues.ligneCourante());
        assertTrue(fondues.dansPile(3));
        assertEquals(MarqueLigne.INCHANGE, sansIntegration.marqueVcs(2),
                "les marques d'origine restent muettes sans fusion");
    }

    /**
     * La coulisse découle de la CONFIGURATION, pas des données
     * (PIEGE-25) : les marques qui arrivent sans coulisse réservée ne
     * décalent rien.
     */
    @Test
    void coulisseDecouleDeLaConfiguration() {
        MarquesGouttiere gestion = new MarquesGouttiere();
        assertEquals(0f, gestion.coulisses(false).laneWidth(10f, 8f), 1e-6f,
                "rien d'activé : aucune coulisse réservée");
        gestion.setIntegration(MarquesIntegration.of(
                Map.of(1, MarqueLigne.MODIFIE), Map.of(), null, Set.of()));
        assertEquals(0f, gestion.coulisses(false).laneWidth(10f, 8f), 1e-6f,
                "des marques sans coulisse activée ne l'élargissent pas");
        assertEquals(20f, gestion.coulisses(true).laneWidth(10f, 8f), 1e-6f,
                "la syntaxe activée réserve deux caractères — le chevron y vit à l'aise");
        gestion.setCoulisseIntegration(true);
        assertEquals(20f, gestion.coulisses(false).laneWidth(10f, 8f), 1e-6f,
                "la coulisse activée réserve deux caractères");
        assertEquals(20f, gestion.coulisses(true).laneWidth(10f, 8f), 1e-6f,
                "la plus large des coulisses activées gagne");
    }

    /** Le clic dans le champ des numéros rend la ligne, la coulisse reste aux plis. */
    @Test
    void clicDansLeChampDesNumeresRendLaLigne() {
        MarquesGouttiere gestion = new MarquesGouttiere();
        List<Integer> cliques = new java.util.ArrayList<>();
        gestion.setSurClicLigne(cliques::add);
        VisualProjection projection = new VisualProjection(3, new FoldModel());

        assertTrue(gestion.surAppui(20f, 45f, 40f, 10f, 0f, 0f, 20f,
                projection),
                "le champ des numéros consomme le clic");
        assertEquals(List.of(2), cliques, "y 45, ligne 20 px : la ligne 2");

        assertFalse(gestion.surAppui(5f, 45f, 40f, 10f, 0f, 0f, 20f,
                projection),
                "la coulisse des marqueurs reste aux plis et aux arrêts");
        assertFalse(gestion.surAppui(45f, 45f, 40f, 10f, 0f, 0f, 20f,
                projection),
                "hors gouttière, le clic traverse au texte");
    }

    /** Sans écouteur, le clic de gouttière ne se consomme pas. */
    @Test
    void sansEcouteurLeClicTraverse() {
        MarquesGouttiere gestion = new MarquesGouttiere();
        VisualProjection projection = new VisualProjection(3, new FoldModel());
        assertFalse(gestion.surAppui(20f, 45f, 40f, 10f, 0f, 0f, 20f,
                projection));
    }

    /** Le clic suit le défilement vertical : la rangée sous le doigt décide. */
    @Test
    void clicSuitLeDefilementVertical() {
        MarquesGouttiere gestion = new MarquesGouttiere();
        List<Integer> cliques = new java.util.ArrayList<>();
        gestion.setSurClicLigne(cliques::add);
        VisualProjection projection = new VisualProjection(30, new FoldModel());

        assertTrue(gestion.surAppui(20f, 5f, 40f, 10f, 0f, 40f, 20f,
                projection));
        assertEquals(List.of(2), cliques,
                "y 5 avec 40 px de défilement : la rangée 2 sous le doigt");
    }

    /** Le champ des numéros s'arrête à la BANDE DE PLIS : le chevron
     *  garde sa cible au bord droit de la gouttière. */
    @Test
    void leChampSArreteALaBandeDePlis() {
        MarquesGouttiere gestion = new MarquesGouttiere();
        List<Integer> cliques = new java.util.ArrayList<>();
        gestion.setSurClicLigne(cliques::add);
        VisualProjection projection = new VisualProjection(3, new FoldModel());
        // la bande de plis vit au bord droit : les huit derniers pixels
        assertFalse(gestion.surAppui(35f, 45f, 40f, 10f, 8f, 0f, 20f,
                        projection),
                "la bande de plis garde sa cible au chevron");
        // juste avant la bande : le champ consomme
        assertTrue(gestion.surAppui(31f, 45f, 40f, 10f, 8f, 0f, 20f,
                projection));
        assertEquals(List.of(2), cliques, "y 45, ligne 20 px : la ligne 2");
    }
}
