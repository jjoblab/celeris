package jo.celeris.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.TextView;

import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

/**
 * Le banc du montage de l'exemple (le retour terrain du
 * 2026-11-19 : « utiliser de vrais layout pour le demo, qu'il soit
 * plus moderne ») : l'activité gonfle la scène XML Material 3 — le
 * tiroir DrawerLayout + NavigationView porte TOUS les échantillons
 * du dépôt, la barre d'onglets porte le fichier ouvert de départ
 * (sélectionné), la barre d'outils suit son titre, le journal vit
 * en panneau bas. Le montage du § 4.2 d'USAGE s'y branche : la
 * façade attachée, le document ouvert.
 */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class MontageExempleTest {

    /** L'activité gonfle, branche et vit — toute la scène XML. */
    @Test
    public void leMontageGonfleLaSceneMaterielle() {
        MainActivity activite = Robolectric
                .buildActivity(MainActivity.class)
                .setup()
                .get();
        DrawerLayout tiroir = activite.findViewById(R.id.tiroir_racine);
        assertNotNull("le DrawerLayout racine vit", tiroir);
        NavigationView navigation = activite.findViewById(R.id.tiroir_navigation);
        assertNotNull("le NavigationView du tiroir vit", navigation);
        assertEquals("le tiroir modernisé porte échantillons, moteur et thèmes",
                Samples.tous().size() + 2 + 9, navigation.getMenu().size());
        int echantillons = 0;
        for (int i = 0; i < navigation.getMenu().size(); i++) {
            if (navigation.getMenu().getItem(i).getGroupId() == 1) {
                echantillons++;
            }
        }
        assertEquals("le groupe des échantillons vit entier",
                Samples.tous().size(), echantillons);
        MaterialToolbar barre = activite.findViewById(R.id.barre_outils);
        assertEquals("la barre d'outils suit le fichier ouvert",
                Samples.tous().get(0).nomFichier(),
                String.valueOf(barre.getTitle()));
        TabLayout onglets = activite.findViewById(R.id.barre_onglets);
        assertEquals("l'onglet du fichier de départ vit",
                Samples.tous().get(0).nomFichier(),
                nomDeLOnglet(onglets, 0));
        assertNotNull("la vue d'éditeur du kit vit",
                activite.findViewById(R.id.vue_editeur));
        assertNotNull("la couche porte-popups du kit vit",
                activite.findViewById(R.id.couche_popups));
        View journal = activite.findViewById(R.id.panneau_journal);
        assertNotNull("le panneau du journal vit", journal);
        TextView lines = activite.findViewById(R.id.lignes_journal);
        assertTrue("le journal a déjà parlé (montage prêt)",
                lines.getText().toString().contains("montage prêt"));
        activite.finish();
    }

    private static String nomDeLOnglet(TabLayout onglets, int indice) {
        TabLayout.Tab onglet = onglets.getTabAt(indice);
        if (onglet == null || onglet.getCustomView() == null) {
            return "";
        }
        TextView nom = onglet.getCustomView().findViewById(R.id.nom_onglet);
        return nom == null ? "" : nom.getText().toString();
    }
}
