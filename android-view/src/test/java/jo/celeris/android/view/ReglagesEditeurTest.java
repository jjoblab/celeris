package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

/**
 * Les réglages de l'éditeur (l'audit du propriétaire du 2026-11-14 :
 * « ajoute un système central editor settings pour pouvoir activer
 * désactiver, etc. certaines fonctionnalités ») : les portes vivent
 * par défaut comme l'IDE de référence les pose, la bascule AVERTIT,
 * la pose silencieuse des préférences n'égrène pas ses écouteurs —
 * la table reste l'unique point de vérité.
 */
public class ReglagesEditeurTest {

    /** Les portes par défaut : tout ce qui aide la lecture vit, le
     *  retour à la ligne attend son tour. */
    @Test
    public void lesPortesViventParDefaut() {
        ReglagesEditeur reglages = new ReglagesEditeur();
        assertTrue("les indices en ligne vivent",
                reglages.ouverte(ReglagesEditeur.Porte.INDICES_EN_LIGNE));
        assertTrue("la barre de sélection vit",
                reglages.ouverte(ReglagesEditeur.Porte.BARRE_SELECTION));
        assertTrue("l'ampoule vit",
                reglages.ouverte(ReglagesEditeur.Porte.AMPOULE_INTENTIONS));
        assertTrue("la complétion automatique vit",
                reglages.ouverte(ReglagesEditeur.Porte.COMPLETION_AUTO));
        assertFalse("le retour à la ligne attend",
                reglages.ouverte(ReglagesEditeur.Porte.RETOUR_LIGNE));
    }

    /** La bascule AVERTIT l'écouteur — une bascule, une application ;
     *  la pose silencieuse n'égrène rien. */
    @Test
    public void laBasculeAvertitLaPoseSeTait() {
        ReglagesEditeur reglages = new ReglagesEditeur();
        AtomicInteger bascules = new AtomicInteger();
        reglages.setSurBascule(porte -> bascules.incrementAndGet());
        reglages.poser(ReglagesEditeur.Porte.MINIMAP, false);
        assertEquals("la pose silencieuse n'égrène pas", 0, bascules.get());
        assertFalse("la porte fermée reste fermée",
                reglages.ouverte(ReglagesEditeur.Porte.MINIMAP));
        assertTrue("la bascule referme et avertit", reglages.basculer(
                ReglagesEditeur.Porte.INDICES_EN_LIGNE) == false);
        assertEquals("l'écouteur a vécu une fois", 1, bascules.get());
    }

    /** La clé de chaque porte est STABLE — les préférences persistées
     *  de l'hôte s'y branchent sans renégocier le monde. */
    @Test
    public void lesClesRestentStables() {
        assertEquals("editeur.inlays", ReglagesEditeur.Porte.INDICES_EN_LIGNE.cle());
        assertEquals("editeur.barreSelection", ReglagesEditeur.Porte.BARRE_SELECTION.cle());
        assertEquals("editeur.ampoule", ReglagesEditeur.Porte.AMPOULE_INTENTIONS.cle());
        assertEquals("editeur.completionAuto", ReglagesEditeur.Porte.COMPLETION_AUTO.cle());
        assertEquals("editeur.valeursPause", ReglagesEditeur.Porte.VALEURS_PAUSE.cle());
        assertEquals("editeur.fantomeIa", ReglagesEditeur.Porte.FANTOME_IA.cle());
        assertEquals("editeur.minimap", ReglagesEditeur.Porte.MINIMAP.cle());
        assertEquals("editeur.defilementRapide", ReglagesEditeur.Porte.DEFILEMENT_RAPIDE.cle());
        assertEquals("editeur.numeros", ReglagesEditeur.Porte.NUMEROS_LIGNES.cle());
        assertEquals("editeur.retourLigne", ReglagesEditeur.Porte.RETOUR_LIGNE.cle());
    }
}
