package jo.celeris.demo;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.FeuilleBas;
import jo.celeris.android.widgets.PopupPalette;

/**
 * La feuille des réglages (ADR-018, chantier C12) : l'écran plein
 * cadre de la démonstration migre en feuille du bas — le contenu ne
 * change pas (six interrupteurs, un curseur, le paquet jouet H-04), la
 * persistance non plus : chaque bascule s'écrit aussitôt, l'hôte
 * réapplique son état au rappel de fermeture. Les états se RELESENT à
 * chaque ouverture — la feuille vit plus longtemps que l'écran qu'elle
 * remplace. Le thème sombre se lit dans l'index de thème de l'éditeur
 * (1 = sombre). Libellés en ressources {@code en}/{@code fr} complètes
 * (ADR-008).
 */
public final class FeuilleReglages {

    private static final String CLE_NUMEROS = "gouttiere.numeros";
    private static final String CLE_RETOUR = "retour.souple";
    private static final String CLE_THEME = "theme.index";
    /** Le délai de retenue d'analyse, en millisecondes (M7.5, §9). */
    public static final String CLE_RETENU = "analyse.retenuMs";
    /** La surimpression de diagnostic, coupée par défaut (M7.5, §9). */
    public static final String CLE_SURIMPRESSION = "scene.surimpression";

    private final AppCompatActivity activite;
    private final SharedPreferences prefs;
    private final Supplier<PopupPalette> palettes;
    private final Runnable surFermetureHote;
    private final FeuilleBas feuille;

    /**
     * Construit la feuille des réglages — le contenu s'y inflate d'entrée.
     *
     * @param activite     l'activité hôte de la feuille
     * @param palettes     la palette courante, dérivée à chaque ouverture
     * @param surFermeture le rappel de l'hôte — il y réapplique son état
     */
    FeuilleReglages(AppCompatActivity activite,
            Supplier<PopupPalette> palettes, Runnable surFermeture) {
        this.activite = activite;
        this.palettes = palettes;
        this.surFermetureHote = surFermeture;
        this.prefs = activite.getSharedPreferences("celeris",
                Context.MODE_PRIVATE);
        feuille = new FeuilleBas(activite, palettes.get());
        feuille.titre(activite.getString(R.string.titre_reglages));
        feuille.libelleFermeture(activite.getString(R.string.action_fermer));
        feuille.contenu(activite.getLayoutInflater()
                .inflate(R.layout.feuille_reglages, null));
        feuille.setSurFermeture(() -> {
            relire();
            surFermetureHote.run();
        });
        relire();
    }

    /**
     * Montre la feuille — la palette se redérive (le thème a pu tourner
     * depuis la dernière ouverture) et les états se relisent.
     */
    void montrer() {
        feuille.setPalette(palettes.get());
        relire();
        feuille.montrer(hote());
    }

    /** La touche retour rencontre la feuille — ouverte, elle se referme. */
    boolean surRetour() {
        return feuille.surRetour();
    }

    /** Le cadre de contenu de la fenêtre — toujours un FrameLayout. */
    private ViewGroup hote() {
        return (ViewGroup) activite.findViewById(android.R.id.content);
    }

    /**
     * Relit chaque état persisté et rebranche les interrupteurs : la
     * feuille vit plus longtemps qu'un écran, ses états se resynchronisent
     * à chaque ouverture et à chaque fermeture.
     */
    private void relire() {
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
    }

    /**
     * Le thème sombre vit dans l'index de thème de l'éditeur (0 clair,
     * 1 sombre, 2 contraste élevé) : l'interrupteur écrit un entier,
     * jamais un booléen — l'éditeur lit un entier.
     *
     * @param prefs les préférences de la démonstration
     */
    private void interrupteurTheme(SharedPreferences prefs) {
        MaterialSwitch vue = feuille.findViewById(R.id.interrupteur_theme);
        vue.setOnCheckedChangeListener(null);
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
        Slider curseur = feuille.findViewById(R.id.curseur_lenteur);
        TextView valeur = feuille.findViewById(R.id.valeur_lenteur);
        int retenu = (int) Math.min(curseur.getValueTo(),
                Math.max(0L, prefs.getLong(CLE_RETENU,
                        EditorView.DELAI_RETENU_PAR_DEFAUT_MS)));
        // le cran le plus proche du réglage persisté
        int pas = (int) curseur.getStepSize();
        curseur.setValue((float) Math.round(retenu / (float) pas) * pas);
        valeur.setText(activite.getString(R.string.unite_ms, retenu));
        curseur.setLabelFormatter(valeurMs ->
                activite.getString(R.string.unite_ms, (int) valeurMs));
        // les ecouteurs se cumulent sinon : la relecture les remplace
        curseur.clearOnChangeListeners();
        curseur.addOnChangeListener((slider, nouvelle, parUtilisateur) -> {
            int ms = (int) nouvelle;
            prefs.edit().putLong(CLE_RETENU, ms).apply();
            valeur.setText(activite.getString(R.string.unite_ms, ms));
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
        MaterialSwitch vue = feuille.findViewById(identifiant);
        vue.setOnCheckedChangeListener(null);
        vue.setChecked(coche);
        vue.setOnCheckedChangeListener(
                (interrupteur, nouvelEtat) -> prefs.edit()
                        .putBoolean(cle, nouvelEtat).apply());
    }

    /**
     * Le paquet de langage jouet (H-04, M9) : la bascule COPIE le
     * paquet Logo au dossier des paquets de l'utilisateur ou le
     * RETIRE — le registre se RECHARGE à chaud, l'accueil resoumet
     * ses cartes à la fermeture : ajouter un langage ne recompile rien,
     * c'est cela que la bascule démontre.
     */
    private void interrupteurPaquetJouet() {
        MaterialSwitch vue = feuille.findViewById(R.id.interrupteur_paquet_jouet);
        Path dossier = activite.getFilesDir().toPath()
                .resolve(PaquetJouet.DOSSIER);
        LangagesDemo.dossierPaquets(dossier);
        vue.setOnCheckedChangeListener(null);
        vue.setChecked(PaquetJouet.installe(dossier));
        vue.setEnabled(true);
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
            activite.runOnUiThread(() -> retablirInterrupteur(coche));
            return;
        }
        activite.runOnUiThread(() -> {
            MaterialSwitch vue =
                    feuille.findViewById(R.id.interrupteur_paquet_jouet);
            vue.setEnabled(true);
        });
    }

    /** Rétablit l'interrupteur après un échec d'écriture. */
    private void retablirInterrupteur(boolean coche) {
        MaterialSwitch vue = feuille.findViewById(R.id.interrupteur_paquet_jouet);
        vue.setChecked(!coche);
        vue.setEnabled(true);
    }
}
