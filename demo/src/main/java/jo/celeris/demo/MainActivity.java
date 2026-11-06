package jo.celeris.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Écran d'accueil de la démonstration : le catalogue des fichiers
 * d'exemple — un par langage P0 de la matrice — ouvre l'écran
 * d'éditeur complet ; le compte de scénarios et l'écran de réglages en
 * direct restent accessibles. Libellés en ressources `en`/`fr`
 * complètes (ADR-008).
 */
public final class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle etat) {
        super.onCreate(etat);
        ScrollView ascenseur = new ScrollView(this);
        LinearLayout racine = new LinearLayout(this);
        racine.setOrientation(LinearLayout.VERTICAL);
        float densite = getResources().getDisplayMetrics().density;
        int marge = Math.round(16 * densite);
        racine.setPadding(marge, marge, marge, marge);
        ascenseur.addView(racine);

        TextView titre = new TextView(this);
        titre.setText(R.string.titre_accueil);
        titre.setTextSize(20);
        racine.addView(titre);

        TextView section = new TextView(this);
        section.setText(R.string.section_exemples);
        section.setTextSize(16);
        racine.addView(section);
        TextView note = new TextView(this);
        note.setText(R.string.note_exemples);
        racine.addView(note);

        for (FichierExemple fichier : FichiersExemple.catalogue()) {
            Button ouvrir = new Button(this);
            ouvrir.setText(fichier.nom());
            ouvrir.setOnClickListener(vue -> startActivity(new Intent(this, EditorActivity.class)
                    .putExtra(EditorActivity.EXTRA_FICHIER, fichier.nom())));
            racine.addView(ouvrir, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        TextView catalogue = new TextView(this);
        int nombre = ScenarioCatalog.scenarios().size();
        catalogue.setText(nombre == 0
                ? getString(R.string.catalogue_vide)
                : getString(R.string.nombre_scenarios, nombre));
        racine.addView(catalogue);

        Button reglages = new Button(this);
        reglages.setText(R.string.action_reglages);
        reglages.setOnClickListener(
                vue -> startActivity(new Intent(this, SettingsActivity.class)));
        racine.addView(reglages);

        setContentView(ascenseur);
    }
}
