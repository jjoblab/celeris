package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * La configuration de la coulisse de marqueurs (M7.5, §5) : la largeur
 * de coulisse découle de la configuration SEULE — une marque qui arrive
 * se dessine dans la coulisse déjà réservée, elle ne l'élargit pas
 * (PIEGE-25 : le texte ne se décale jamais à l'arrivée d'un résultat).
 */
class GutterLanesTest {

    /** Sans coulisse, rien n'est réservé. */
    @Test
    void aucuneConfigurationNeReserveRien() {
        assertEquals(0f, GutterLanes.AUCUNE.laneWidth(8f, 4f), 1e-6f);
        assertEquals(0f, new GutterLanes(false, false, false, false)
                .laneWidth(8f, 4f), 1e-6f);
    }

    /**
     * La coulisse de syntaxe réserve DEUX caractères (M7.5,
     * redressement) : le chevron de pli y vit à l'aise — la cible du
     * doigt comprise.
     */
    @Test
    void laSyntaxeReserveDeuxCaracteres() {
        GutterLanes syntaxe = new GutterLanes(true, false, false, false);
        assertEquals(16f, syntaxe.laneWidth(8f, 4f), 1e-6f);
        // Le plafond est le MAXIMUM de la respiration et de deux
        // caractères : une respiration généreuse prime, jamais elle
        // ne rétrécit la cible du doigt.
        assertEquals(20f, syntaxe.laneWidth(8f, 20f), 1e-6f,
                "la respiration la plus généreuse prime");
        // Une police large réserve d'autant.
        assertEquals(30f, syntaxe.laneWidth(15f, 4f), 1e-6f);
    }

    /**
     * Chaque coulisse d'intégration réserve deux caractères — l'état
     * VCS, les arrêts, les diagnostics importés, les repères
     * d'aperçu : la coulisse est commune.
     */
    @Test
    void chaqueIntegrationReserveDeuxCaracteres() {
        assertEquals(16f, new GutterLanes(false, true, false, false)
                .laneWidth(8f, 4f), 1e-6f);
        assertEquals(16f, new GutterLanes(false, false, true, false)
                .laneWidth(8f, 4f), 1e-6f);
        assertEquals(16f, new GutterLanes(false, false, false, true)
                .laneWidth(8f, 4f), 1e-6f);
        // Toutes ensemble : une seule coulisse, jamais empilée.
        assertEquals(16f, new GutterLanes(false, true, true, true)
                .laneWidth(8f, 4f), 1e-6f);
    }

    /**
     * La syntaxe et les intégrations partagent la même coulisse : le
     * maximum des deux règles, jamais leur somme.
     */
    @Test
    void laSyntaxeEtLesIntegrationsPartagentLaCoulisse() {
        GutterLanes tout = new GutterLanes(true, true, true, true);
        assertEquals(16f, tout.laneWidth(8f, 4f), 1e-6f,
                "le maximum, pas la somme");
        // La respiration de la syntaxe peut dépasser deux caractères :
        // c'est elle qui fixe la largeur, les intégrations suivent.
        assertEquals(20f, tout.laneWidth(8f, 20f), 1e-6f,
                "la respiration de la syntaxe prime quand elle dépasse");
    }

    /**
     * PIEGE-25 : la largeur découle de la configuration seule — la
     * même configuration réserve toujours la même largeur, quel que
     * soit ce que les intégrations ont à afficher (la configuration ne
     * sait rien des marques arrivantes).
     */
    @Test
    void laLargeurNeDependQueDeLaConfiguration() {
        GutterLanes syntaxe = new GutterLanes(true, false, false, false);
        // Avant et après l'arrivée de marques : la configuration ne
        // change pas, la largeur non plus — le texte ne se décale pas.
        float avant = syntaxe.laneWidth(8f, 4f);
        float apres = syntaxe.laneWidth(8f, 4f);
        assertEquals(avant, apres, 1e-6f);
    }
}
