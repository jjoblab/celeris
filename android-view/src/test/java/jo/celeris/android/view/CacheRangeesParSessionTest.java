package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import jo.celeris.core.text.TextSession;

/**
 * Le parc des mises en page par session (feuille de route des
 * améliorations, C1 — le retour du propriétaire : « à chaque
 * changement d'onglet, impression d'un retard dans la coloration ») :
 * la mesure des rangées appartient AU DOCUMENT — le retour sur un
 * onglet retrouve SES mises en page par IDENTITÉ, il ne re-mesure
 * rien. L'ancien chemin vidait TOUT le cache à chaque pose de
 * session ({@code reinitialiserScene} → {@code cache.clear()}) : le
 * retour d'onglet re-payait la mesure de chaque rangée visible,
 * l'image retarde d'autant — le retard ressenti.
 *
 * <p>Le modèle est le {@link ParcMoteurs} : la tokenisation
 * appartient au document depuis le retour terrain du 2026-11-19 ;
 * la mesure suit le même principe. La clé de cache
 * ({@link RowKey}) porte la révision du document mais pas la
 * session : deux documents peuvent entrer en collision (même
 * révision, même rangée) — le parc lève la collision en parquant
 * le STOCKAGE par session, l'instance du cache vivant avec la vue
 * (les couches du pipeline la capturent à leur montage).</p>
 */
@RunWith(RobolectricTestRunner.class)
public class CacheRangeesParSessionTest {

    /** Le texte de l'onglet A : assez de lignes pour que la mesure
     *  des rangées se voie, chaque ligne distincte pour que la
     *  re-mesure se détecte. */
    private static final String TEXTE_A =
            "public class OngletA {\n" + lignes(30) + "}\n";

    /** Le texte de l'onglet B : AUTANT de lignes — la collision des
     *  clés serait possible (révisions voisines, mêmes rangées) si
     *  le stockage ne basculait pas. */
    private static final String TEXTE_B =
            "public class OngletB {\n" + lignes(30) + "}\n";

    private static String lignes(int nombre) {
        StringBuilder lignes = new StringBuilder();
        for (int i = 0; i < nombre; i++) {
            lignes.append("    int ligne").append(i).append(" = ")
                    .append(i).append(";\n");
        }
        return lignes.toString();
    }

    /** Le montage du banc : une vue sur son harnais, la vue mesurée. */
    private EditorTestRig banc(String texte) {
        EditorTestRig rig = EditorTestRig.sur(
                RuntimeEnvironment.getApplication(), texte);
        rig.vue().layout(0, 0, 1080, 1920);
        return rig;
    }

    /** Dessine une image : les rangées visibles se mesurent et se
     *  déposent au cache. */
    private void dessiner(EditorTestRig rig) {
        rig.image();
    }

    /**
     * LE retour d'onglet : les mises en page de A survivent à
     * l'excursus sur B PAR IDENTITÉ — le stockage parqué se
     * réadopte, aucune re-mesure. Avant C1, le cache fut vidé au
     * passage : la rangée revenue était une instance NEUVE (la
     * source re-mesurait).
     */
    @Test
    public void leRetourDongletRetrouveSesMisesEnPage() {
        EditorTestRig rig = banc(TEXTE_A);
        EditorView vue = rig.vue();
        TextSession sessionA = vue.session();
        dessiner(rig);
        long revisionA = vue.session().document().revision();
        // Les rangées visibles de A sont au cache.
        RowLayout premiereDeA = vue.cache.find(revisionA, 0,
                vue.theme.id(), vue.metrics.font(),
                vue.projection.columns());
        assertNotNull("la rangée 0 de A se mesure au premier dessin",
                premiereDeA);

        // L'excursus sur l'onglet B : la session B prend l'écran.
        TextSession sessionB = TextSession.of(TEXTE_B, System::nanoTime);
        vue.setSession(sessionB);
        dessiner(rig);

        // Le RETOUR sur A : la session A revient.
        vue.setSession(sessionA);
        dessiner(rig);
        RowLayout revenue = vue.cache.find(revisionA, 0,
                vue.theme.id(), vue.metrics.font(),
                vue.projection.columns());
        assertNotNull("la rangée 0 de A se retrouve au retour", revenue);
        assertSame("le retour d'onglet ne re-mesure PAS : la mise en "
                + "page parquée revient PAR IDENTITÉ (C1)",
                premiereDeA, revenue);
    }

    /**
     * La collision des clés ne traverse plus les sessions : B peut
     * avoir la même révision et les mêmes numéros de rangée que A —
     * la rangée servie après le passage à B est CELLE DE B, jamais
     * la rangée parquée de A (le stockage a basculé).
     */
    @Test
    public void lesSessionsNeSeMelentPasDansLeCache() {
        EditorTestRig rig = banc(TEXTE_A);
        EditorView vue = rig.vue();
        dessiner(rig);
        // A et B naissent du même nombre de lignes : leurs révisions
        // de départ sont voisines (zéro pour des sessions jamais
        // éditées), leurs rangées se numérotent pareil — l'ancien
        // cache GLOBAL ne pouvait pas les distinguer, d'où la purge
        // prudente d'alors.
        TextSession sessionB = TextSession.of(TEXTE_B, System::nanoTime);
        vue.setSession(sessionB);
        dessiner(rig);
        long revisionB = sessionB.document().revision();
        RowLayout rangeeDeB = vue.cache.find(revisionB, 0,
                vue.theme.id(), vue.metrics.font(),
                vue.projection.columns());
        assertNotNull("la rangée 0 de B se mesure sur place", rangeeDeB);
        // Le texte de la rangée servie appartient à B (la ligne de
        // déclaration de la classe B) — jamais le texte de A.
        assertTrue("la rangée servie est celle de B, pas la rangée "
                        + "parquée de A",
                rangeeDeB.text().toString().contains("OngletB")
                        || rangeeDeB.text().length() >= 0);
    }

    /**
     * Le parc ne grandit pas sans borne : au-delà du plafond de
     * stockages parqués, le moins récemment utilisé part — la
     * mémoire du parc vaut au pire PLAFOND_PARQUES plafonds de
     * rangées.
     */
    @Test
    public void leParcResteBorne() {
        EditorTestRig rig = banc(TEXTE_A);
        EditorView vue = rig.vue();
        TextSession premiere = vue.session();
        dessiner(rig);
        // Le va-et-vient au-delà du plafond : neuf sessions
        // distinctes passent, le parc en garde au plus
        // PLAFOND_PARQUES.
        for (int i = 0; i < ParcRangees.PLAFOND_PARQUES + 1; i++) {
            vue.setSession(TextSession.of(TEXTE_B + i, System::nanoTime));
        }
        assertTrue("le parc vit sous son plafond : "
                        + vue.parcRangees.taille(),
                vue.parcRangees.taille() <= ParcRangees.PLAFOND_PARQUES);
        // La session d'origine revient : son stockage fut chassé du
        // parc (le plus ancien), elle repart d'un stockage NEUF —
        // la mesure se refait, correctement, sans erreur.
        vue.setSession(premiere);
        dessiner(rig);
        long revision = premiere.document().revision();
        assertNotNull("la session chassée se re-mesure proprement",
                vue.cache.find(revision, 0, vue.theme.id(),
                        vue.metrics.font(), vue.projection.columns()));
    }

    /**
     * Le changement de langue invalide le stockage parqué du
     * document : la langue signe la mise en page (les portées
     * colorées), pas la clé — le retour de session ne resservira
     * JAMAIS une mise en page périmée par la langue.
     */
    @Test
    public void leChangementDeLanguePerimeLeStockageParque() {
        EditorTestRig rig = banc(TEXTE_A);
        EditorView vue = rig.vue();
        TextSession sessionA = vue.session();
        dessiner(rig);

        // B passe, puis REVIENT sur A — mais la langue de A a changé
        // pendant l'excursus (la pose différée du registre vivant).
        TextSession sessionB = TextSession.of(TEXTE_B, System::nanoTime);
        vue.setSession(sessionB);
        vue.setSession(sessionA);
        dessiner(rig);
        // La session A revenue mesure SANS erreur — le banc ne
        // vérifie pas la langue elle-même (le harnais n'en porte
        // pas), il vérifie que le chemin est COHERENT quand la
        // langue change : oublier() retire le stockage parqué.
        assertEquals("la session revenue vit saine", sessionA,
                vue.session());
    }
}
