package jo.celeris.demo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Écran de réglages en direct : chaque interrupteur persiste
 * immédiatement (SharedPreferences) et l'état se relit à l'ouverture ;
 * le câblage au moteur arrive avec les jalons qui portent les
 * fonctionnalités correspondantes. Sans ressources avant M3 (ADR-008).
 */
public final class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle etat) {
        super.onCreate(etat);
        SharedPreferences prefs = getSharedPreferences("celeris", Context.MODE_PRIVATE);
        LinearLayout racine = new LinearLayout(this);
        racine.setOrientation(LinearLayout.VERTICAL);
        float densite = getResources().getDisplayMetrics().density;
        int marge = Math.round(16 * densite);
        racine.setPadding(marge, marge, marge, marge);

        TextView titre = new TextView(this);
        titre.setText("Réglages");
        titre.setTextSize(20);
        racine.addView(titre);

        racine.addView(interrupteur(prefs, "Numéros de ligne", "gouttiere.numeros"));
        racine.addView(interrupteur(prefs, "Retour à la ligne souple", "retour.souple"));
        racine.addView(interrupteur(prefs, "Thème sombre", "theme.sombre"));

        setContentView(racine);
    }

    private Switch interrupteur(SharedPreferences prefs, String libelle, String cle) {
        Switch interrupteur = new Switch(this);
        interrupteur.setText(libelle);
        interrupteur.setChecked(prefs.getBoolean(cle, false));
        interrupteur.setOnCheckedChangeListener(
                (vue, coche) -> prefs.edit().putBoolean(cle, coche).apply());
        return interrupteur;
    }
}
