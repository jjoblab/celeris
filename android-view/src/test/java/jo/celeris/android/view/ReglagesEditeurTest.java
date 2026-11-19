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
        EditorSettingsPanel settings = new EditorSettingsPanel();
        assertTrue("les indices en ligne vivent",
                settings.ouverte(EditorSettingsPanel.Porte.INDICES_EN_LIGNE));
        assertTrue("la barre de sélection vit",
                settings.ouverte(EditorSettingsPanel.Porte.BARRE_SELECTION));
        assertTrue("l'ampoule vit",
                settings.ouverte(EditorSettingsPanel.Porte.AMPOULE_INTENTIONS));
        assertTrue("la complétion automatique vit",
                settings.ouverte(EditorSettingsPanel.Porte.COMPLETION_AUTO));
        assertFalse("le retour à la ligne attend",
                settings.ouverte(EditorSettingsPanel.Porte.RETOUR_LIGNE));
    }

    /** La bascule AVERTIT l'abonné de la source unique (2.0,
     *  ADR-0024) — une bascule, une application ; la pose silencieuse
     *  n'égrène rien. */
    @Test
    public void laBasculeAvertitLaPoseSeTait() {
        EditorSettings source = new EditorSettings();
        AtomicInteger bascules = new AtomicInteger();
        source.addListener(s -> bascules.incrementAndGet());
        source.gates().place(EditorSettingsPanel.Porte.MINIMAP, false);
        assertEquals("la pose silencieuse n'égrène pas", 0, bascules.get());
        assertFalse("la porte fermée reste fermée",
                source.gates().ouverte(EditorSettingsPanel.Porte.MINIMAP));
        assertTrue("la bascule referme et avertit", source.toggleGate(
                EditorSettingsPanel.Porte.INDICES_EN_LIGNE) == false);
        assertEquals("l'abonné a vécu une fois", 1, bascules.get());
    }

    /** La clé de chaque porte est STABLE — les préférences persistées
     *  de l'hôte s'y branchent sans renégocier le monde. */
    @Test
    public void lesClesRestentStables() {
        assertEquals("editeur.inlays", EditorSettingsPanel.Porte.INDICES_EN_LIGNE.key());
        assertEquals("editeur.barreSelection", EditorSettingsPanel.Porte.BARRE_SELECTION.key());
        assertEquals("editeur.ampoule", EditorSettingsPanel.Porte.AMPOULE_INTENTIONS.key());
        assertEquals("editeur.completionAuto", EditorSettingsPanel.Porte.COMPLETION_AUTO.key());
        assertEquals("editeur.valeursPause", EditorSettingsPanel.Porte.VALEURS_PAUSE.key());
        assertEquals("editeur.fantomeIa", EditorSettingsPanel.Porte.FANTOME_IA.key());
        assertEquals("editeur.minimap", EditorSettingsPanel.Porte.MINIMAP.key());
        assertEquals("editeur.defilementRapide", EditorSettingsPanel.Porte.DEFILEMENT_RAPIDE.key());
        assertEquals("editeur.numeros", EditorSettingsPanel.Porte.NUMEROS_LIGNES.key());
        assertEquals("editeur.retourLigne", EditorSettingsPanel.Porte.RETOUR_LIGNE.key());
    }
}
