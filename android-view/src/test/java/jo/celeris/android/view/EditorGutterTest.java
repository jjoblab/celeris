package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Le collaborateur de gouttière (M7.5, §5) : la largeur STABLE — le
 * niveau d'eau des chiffres croît au besoin, jamais ne rétrécit, la
 * coulisse découle de la configuration (PIEGE-25 : le texte ne se
 * décale jamais à l'arrivée d'un résultat).
 */
public class EditorGutterTest {

    private static final float CHAR_WIDTH = 10f;
    private static final float DENSITY = 2f;

    private final EditorGutter gutter = new EditorGutter();

    /** La largeur de la gouttière aux réglages courants. */
    private float width(int lines, boolean numbers) {
        return gutter.suivre(lines, CHAR_WIDTH, DENSITY, numbers);
    }

    /** Le plancher de trois chiffres : la dixième ligne ne décale rien. */
    @Test
    public void lePlancherDeTroisChiffres() {
        float uneLigne = width(1, true);
        assertEquals("la dixième ligne ne décale rien",
                uneLigne, width(10, true), 0.5f);
        assertEquals("la millième ligne croît d'un chiffre",
                uneLigne + CHAR_WIDTH, width(1000, true), 0.5f);
    }

    /** Le niveau d'eau croît et ne rétrécit jamais : 1 000 lignes
     *  puis 5 — la largeur tient celle du pic. */
    @Test
    public void leNiveauEauNeRetrecitJamais() {
        float pic = width(1000, true);
        assertEquals("la largeur tient le pic", pic, width(5, true), 0.5f);
        // le plancher retombe sur demande explicite (changement de
        // session ou de langage)
        gutter.reinitialiser();
        assertTrue("la retombée rend le plancher",
                width(1, true) < pic);
    }

    /** Sans numéros, la gouttière ne réserve que la coulisse et sa
     *  respiration (D-01). */
    @Test
    public void sansNumerosSeuleLaCoulisseReste() {
        gutter.setCoulisses(GutterLanes.AUCUNE);
        float sansNumeros = width(1000, false);
        gutter.reinitialiser();
        gutter.setCoulisses(GutterLanes.AUCUNE);
        float uneLigne = width(1, false);
        assertEquals("les chiffres ne comptent plus",
                uneLigne, sansNumeros, 0.5f);
        assertTrue("la coulisse seule reste positive", uneLigne > 0f);
    }

    /** La coulisse découle de la configuration, jamais de l'arrivée
     *  d'un résultat (PIEGE-25). */
    @Test
    public void laCoulisseDecouleDeLaConfiguration() {
        gutter.setCoulisses(GutterLanes.AUCUNE);
        width(1, true);
        assertEquals("aucune intégration, aucune coulisse",
                0f, gutter.currentLane(), 0f);
        gutter.setCoulisses(new GutterLanes(true, false, false, false));
        width(1, true);
        assertEquals("la coulisse de syntaxe réserve deux caractères",
                2f * CHAR_WIDTH, gutter.currentLane(), 0.5f);
        gutter.setCoulisses(new GutterLanes(false, true, false, false));
        width(1, true);
        assertEquals("la coulisse d'intégration réserve deux caractères",
                2f * CHAR_WIDTH, gutter.currentLane(), 0.5f);
    }

    /** La classe de largeur règle la respiration (D-19) : la valeur
     *  vit en dp, la conversion se fait une fois, en pixels entiers. */
    @Test
    public void laClasseRegleLaRespiration() {
        gutter.setClasse(WidthClass.MOYEN, DENSITY);
        float moyen = gutter.margePx();
        gutter.setClasse(WidthClass.COMPACT, DENSITY);
        float compact = gutter.margePx();
        assertTrue("le compact resserre", compact < moyen);
        assertEquals("la conversion dp → pixels est entière",
                Math.round(WidthClass.MOYEN.gutterMarginDp() * DENSITY),
                Math.round(moyen));
    }

    /** Le plancher de chiffres reste trois, même sans suivi. */
    @Test
    public void lePlancherEstTrois() {
        assertEquals("le plancher de la géométrie", 3,
                GutterLayout.digitsFor(1));
        assertEquals("dix lignes, trois chiffres", 3,
                GutterLayout.digitsFor(10));
        assertEquals("cent lignes, trois chiffres", 3,
                GutterLayout.digitsFor(100));
        assertEquals("mille lignes, quatre chiffres", 4,
                GutterLayout.digitsFor(1000));
    }
}
