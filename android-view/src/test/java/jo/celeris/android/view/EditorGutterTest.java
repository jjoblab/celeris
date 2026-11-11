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

    private final EditorGutter gouttiere = new EditorGutter();

    /** La largeur de la gouttière aux réglages courants. */
    private float largeur(int lignes, boolean numeros) {
        return gouttiere.suivre(lignes, CHAR_WIDTH, DENSITY, numeros);
    }

    /** Le plancher de trois chiffres : la dixième ligne ne décale rien. */
    @Test
    public void lePlancherDeTroisChiffres() {
        float uneLigne = largeur(1, true);
        assertEquals("la dixième ligne ne décale rien",
                uneLigne, largeur(10, true), 0.5f);
        assertEquals("la millième ligne croît d'un chiffre",
                uneLigne + CHAR_WIDTH, largeur(1000, true), 0.5f);
    }

    /** Le niveau d'eau croît et ne rétrécit jamais : 1 000 lignes
     *  puis 5 — la largeur tient celle du pic. */
    @Test
    public void leNiveauEauNeRetrecitJamais() {
        float pic = largeur(1000, true);
        assertEquals("la largeur tient le pic", pic, largeur(5, true), 0.5f);
        // le plancher retombe sur demande explicite (changement de
        // session ou de langage)
        gouttiere.reinitialiser();
        assertTrue("la retombée rend le plancher",
                largeur(1, true) < pic);
    }

    /** Sans numéros, la gouttière ne réserve que la coulisse et sa
     *  respiration (D-01). */
    @Test
    public void sansNumerosSeuleLaCoulisseReste() {
        gouttiere.setCoulisses(GutterLanes.AUCUNE);
        float sansNumeros = largeur(1000, false);
        gouttiere.reinitialiser();
        gouttiere.setCoulisses(GutterLanes.AUCUNE);
        float uneLigne = largeur(1, false);
        assertEquals("les chiffres ne comptent plus",
                uneLigne, sansNumeros, 0.5f);
        assertTrue("la coulisse seule reste positive", uneLigne > 0f);
    }

    /** La coulisse découle de la configuration, jamais de l'arrivée
     *  d'un résultat (PIEGE-25). */
    @Test
    public void laCoulisseDecouleDeLaConfiguration() {
        gouttiere.setCoulisses(GutterLanes.AUCUNE);
        largeur(1, true);
        assertEquals("aucune intégration, aucune coulisse",
                0f, gouttiere.coulisseCourante(), 0f);
        gouttiere.setCoulisses(new GutterLanes(true, false, false, false));
        largeur(1, true);
        assertEquals("la coulisse de syntaxe réserve deux caractères",
                2f * CHAR_WIDTH, gouttiere.coulisseCourante(), 0.5f);
        gouttiere.setCoulisses(new GutterLanes(false, true, false, false));
        largeur(1, true);
        assertEquals("la coulisse d'intégration réserve deux caractères",
                2f * CHAR_WIDTH, gouttiere.coulisseCourante(), 0.5f);
    }

    /** La classe de largeur règle la respiration (D-19) : la valeur
     *  vit en dp, la conversion se fait une fois, en pixels entiers. */
    @Test
    public void laClasseRegleLaRespiration() {
        gouttiere.setClasse(ClasseLargeur.MOYEN, DENSITY);
        float moyen = gouttiere.margePx();
        gouttiere.setClasse(ClasseLargeur.COMPACT, DENSITY);
        float compact = gouttiere.margePx();
        assertTrue("le compact resserre", compact < moyen);
        assertEquals("la conversion dp → pixels est entière",
                Math.round(ClasseLargeur.MOYEN.margeGouttiereDp() * DENSITY),
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
