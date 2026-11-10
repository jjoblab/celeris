package jo.celeris.demo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

import java.io.IOException;
import java.nio.file.Path;

import jo.celeris.android.view.EditorView;

/**
 * Écran de réglages en direct : six interrupteurs et un curseur
 * Material — numéros de ligne, retour souple, thème sombre de
 * l'éditeur, consentement IA, lenteur d'analyse, surimpression de
 * diagnostic et paquet de langage jouet (H-04) — chacun décrit et
 * persisté immédiatement (SharedPreferences, le paquet jouet au
 * stockage) ; l'écran d'éditeur réapplique tout à son retour. Le
 * thème sombre se lit dans l'index de thème de l'éditeur
 * (1 = sombre). Libellés en ressources {@code en}/{@code fr}
 * complètes (ADR-008).
 */
public final class SettingsActivity extends AppCompatActivity {

    private static final String CLE_NUMEROS = "gouttiere.numeros";
    private static final String CLE_RETOUR = "retour.souple";
    private static final String CLE_THEME = "theme.index";
    /** Le délai de retenue d'analyse, en millisecondes (M7.5, §9). */
    public static final String CLE_RETENU = "analyse.retenuMs";
    /** La surimpression de diagnostic, coupée par défaut (M7.5, §9). */
    public static final String CLE_SURIMPRESSION = "scene.surimpression";

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
        curseurLenteur(prefs);
        interrupteur(R.id.interrupteur_surimpression, prefs,
                prefs.getBoolean(CLE_SURIMPRESSION, false), CLE_SURIMPRESSION);
        interrupteurPaquetJouet();
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
     * Le curseur de lenteur d'analyse (M7.5, §9) : de zéro (immédiat)
     * à six cents millisecondes par crans de cinquante — la valeur se
     * persiste en entier, l'étiquette la dit en direct.
     *
     * @param prefs les préférences de la démonstration
     */
    private void curseurLenteur(SharedPreferences prefs) {
        Slider curseur = findViewById(R.id.curseur_lenteur);
        TextView valeur = findViewById(R.id.valeur_lenteur);
        int retenu = (int) Math.min(curseur.getValueTo(),
                Math.max(0L, prefs.getLong(CLE_RETENU,
                        EditorView.DELAI_RETENU_PAR_DEFAUT_MS)));
        // le cran le plus proche du réglage persisté
        int pas = (int) curseur.getStepSize();
        curseur.setValue((float) Math.round(retenu / (float) pas) * pas);
        valeur.setText(getString(R.string.unite_ms, retenu));
        curseur.setLabelFormatter(valeurMs ->
                getString(R.string.unite_ms, (int) valeurMs));
        curseur.addOnChangeListener((slider, nouvelle, parUtilisateur) -> {
            int ms = (int) nouvelle;
            prefs.edit().putLong(CLE_RETENU, ms).apply();
            valeur.setText(getString(R.string.unite_ms, ms));
        });
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

    /**
     * Le paquet de langage jouet (H-04, M9) : la bascule COPIE le
     * paquet Logo au dossier des paquets de l'utilisateur ou le
     * RETIRE — le registre se RECHARGE à chaud, l'accueil resoumet
     * ses cartes au retour : ajouter un langage ne recompile rien,
     * c'est cela que la bascule démontre.
     */
    private void interrupteurPaquetJouet() {
        MaterialSwitch vue = findViewById(R.id.interrupteur_paquet_jouet);
        Path dossier = getFilesDir().toPath().resolve(PaquetJouet.DOSSIER);
        LangagesDemo.dossierPaquets(dossier);
        vue.setChecked(PaquetJouet.installe(dossier));
        vue.setOnCheckedChangeListener((interrupteur, coche) -> {
            vue.setEnabled(false);
            new Thread(() -> basculerPaquet(dossier, coche),
                    "celeris-paquet-jouet").start();
        });
    }

    /**
     * La bascule exécutée hors fil UI : l'écriture au stockage, puis le
     * rechargement du registre — l'interrupteur se réactive à la fin,
     * échec ou pas.
     *
     * @param dossier le dossier des paquets de l'utilisateur
     * @param coche    l'état demandé par la bascule
     */
    private void basculerPaquet(Path dossier, boolean coche) {
        try {
            if (coche) {
                PaquetJouet.installer(dossier);
            } else {
                PaquetJouet.retirer(dossier);
            }
            LangagesDemo.recharger();
        } catch (IOException echec) {
            runOnUiThread(() -> retablirInterrupteur(coche));
            return;
        }
        runOnUiThread(() ->
                findViewById(R.id.interrupteur_paquet_jouet).setEnabled(true));
    }

    /** Rétablit l'interrupteur après un échec d'écriture. */
    private void retablirInterrupteur(boolean coche) {
        MaterialSwitch vue = findViewById(R.id.interrupteur_paquet_jouet);
        vue.setChecked(!coche);
        vue.setEnabled(true);
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
