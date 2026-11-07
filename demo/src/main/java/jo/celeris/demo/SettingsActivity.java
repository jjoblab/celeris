package jo.celeris.demo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;

/**
 * Écran de réglages en direct : trois interrupteurs Material — numéros
 * de ligne, retour souple, thème sombre de l'éditeur — chacun décrit et
 * persisté immédiatement (SharedPreferences) ; l'écran d'éditeur
 * réapplique tout à son retour. Le thème sombre se lit dans l'index de
 * thème de l'éditeur (1 = sombre). Libellés en ressources
 * {@code en}/{@code fr} complètes (ADR-008).
 */
public final class SettingsActivity extends AppCompatActivity {

    private static final String CLE_NUMEROS = "gouttiere.numeros";
    private static final String CLE_RETOUR = "retour.souple";
    private static final String CLE_THEME = "theme.index";

    @Override
    protected void onCreate(Bundle etat) {
        super.onCreate(etat);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        MaterialToolbar barre = findViewById(R.id.barre_reglages);
        barre.setNavigationOnClickListener(vue -> finish());

        SharedPreferences prefs =
                getSharedPreferences("celeris", Context.MODE_PRIVATE);
        interrupteur(R.id.interrupteur_numeros, prefs,
                prefs.getBoolean(CLE_NUMEROS, true), CLE_NUMEROS);
        interrupteur(R.id.interrupteur_retour, prefs,
                prefs.getBoolean(CLE_RETOUR, false), CLE_RETOUR);
        interrupteurTheme(prefs);
        // F-10 — le consentement IA, coupé par défaut : aucune requête
        // ne quitte l'éditeur sans cette activation explicite.
        interrupteur(R.id.interrupteur_ia, prefs,
                prefs.getBoolean(AtelierIA.CLE_CONSENTEMENT, false),
                AtelierIA.CLE_CONSENTEMENT);
        appliquerBordABord();
    }

    /**
     * Le thème sombre vit dans l'index de thème de l'éditeur (0 clair,
     * 1 sombre, 2 contraste élevé) : l'interrupteur écrit un entier,
     * jamais un booléen — l'éditeur lit un entier.
     *
     * @param prefs les préférences de la démonstration
     */
    private void interrupteurTheme(SharedPreferences prefs) {
        MaterialSwitch vue = findViewById(R.id.interrupteur_theme);
        vue.setChecked(prefs.getInt(CLE_THEME, 0) == 1);
        vue.setOnCheckedChangeListener((interrupteur, coche) -> prefs.edit()
                .putInt(CLE_THEME, coche ? 1 : 0).apply());
    }

    /**
     * Lie l'interrupteur à sa clé : l'état se relit à l'ouverture et
     * chaque bascule persiste immédiatement.
     *
     * @param identifiant l'identifiant de la vue interrupteur
     * @param prefs       les préférences de la démonstration
     * @param coche       l'état initial lu
     * @param cle         la clé de persistance
     */
    private void interrupteur(int identifiant, SharedPreferences prefs,
            boolean coche, String cle) {
        MaterialSwitch vue = findViewById(identifiant);
        vue.setChecked(coche);
        vue.setOnCheckedChangeListener(
                (interrupteur, nouvelEtat) -> prefs.edit()
                        .putBoolean(cle, nouvelEtat).apply());
    }

    /** Le chrome s'étend bord à bord : les encarts deviennent marges. */
    private void appliquerBordABord() {
        View racine = findViewById(R.id.racine_reglages);
        ViewCompat.setOnApplyWindowInsetsListener(racine, (vue, encarts) -> {
            Insets barres = encarts.getInsets(WindowInsetsCompat.Type.systemBars());
            vue.setPadding(0, barres.top, 0, barres.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
