package jo.celeris.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Écran d'accueil de la démonstration : liste le catalogue de
 * scénarios (rempli à chaque jalon) et ouvre l'écran de réglages en
 * direct. Libellés en ressources `en`/`fr` complètes (ADR-008).
 */
public final class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle etat) {
        super.onCreate(etat);
        LinearLayout racine = new LinearLayout(this);
        racine.setOrientation(LinearLayout.VERTICAL);
        float densite = getResources().getDisplayMetrics().density;
        int marge = Math.round(16 * densite);
        racine.setPadding(marge, marge, marge, marge);

        TextView titre = new TextView(this);
        titre.setText(R.string.titre_accueil);
        titre.setTextSize(20);
        racine.addView(titre);

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

        setContentView(racine);
    }
}
