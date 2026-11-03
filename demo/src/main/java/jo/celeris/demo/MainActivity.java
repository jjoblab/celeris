package jo.celeris.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Écran d'accueil de la démonstration : liste le catalogue de scénarios
 * (vide au squelette M1) et ouvre l'écran de réglages en direct.
 * Construit sans ressources : les libellés migrent vers les ressources
 * en/fr complètes à M3 (ADR-008).
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
        titre.setText("celeris - démonstration");
        titre.setTextSize(20);
        racine.addView(titre);

        TextView catalogue = new TextView(this);
        int nombre = ScenarioCatalog.scenarios().size();
        catalogue.setText(
                nombre == 0
                        ? "Catalogue de scénarios vide : rempli à chaque jalon."
                        : nombre + " scénario(s)");
        racine.addView(catalogue);

        Button reglages = new Button(this);
        reglages.setText("Réglages en direct");
        reglages.setOnClickListener(
                vue -> startActivity(new Intent(this, SettingsActivity.class)));
        racine.addView(reglages);

        setContentView(racine);
    }
}
