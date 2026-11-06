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
 * l'écran d'éditeur réapplique tout à son retour. Le thème vit dans
 * un index (0 clair, 1 sombre, 2 contraste élevé) que l'éditeur fait
 * aussi tourner. Libellés en ressources `en`/`fr` complètes (ADR-008).
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
        titre.setText(R.string.titre_reglages);
        titre.setTextSize(20);
        racine.addView(titre);

        racine.addView(interrupteurNumeros(prefs));
        racine.addView(interrupteur(prefs, R.string.reglage_retour, "retour.souple", false));
        racine.addView(interrupteurTheme(prefs));

        setContentView(racine);
    }

    private Switch interrupteur(SharedPreferences prefs, int libelle, String cle,
            boolean defaut) {
        Switch interrupteur = new Switch(this);
        interrupteur.setText(libelle);
        interrupteur.setChecked(prefs.getBoolean(cle, defaut));
        interrupteur.setOnCheckedChangeListener(
                (vue, coche) -> prefs.edit().putBoolean(cle, coche).apply());
        return interrupteur;
    }

    /** Le mode sombre se lit dans l'index de thème de l'éditeur (1 = sombre). */
    private Switch interrupteurTheme(SharedPreferences prefs) {
        Switch interrupteur = new Switch(this);
        interrupteur.setText(R.string.reglage_theme);
        interrupteur.setChecked(prefs.getInt("theme.index", 0) == 1);
        interrupteur.setOnCheckedChangeListener((vue, coche) -> prefs.edit()
                .putInt("theme.index", coche ? 1 : 0).apply());
        return interrupteur;
    }

    /** Les numéros sont visibles par défaut (D-01). */
    private Switch interrupteurNumeros(SharedPreferences prefs) {
        return interrupteur(prefs, R.string.reglage_numeros, "gouttiere.numeros", true);
    }
}
