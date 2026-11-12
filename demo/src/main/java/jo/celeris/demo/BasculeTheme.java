package jo.celeris.demo;

import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;

import android.view.MenuItem;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.function.Consumer;

import jo.celeris.android.view.EditorTheme;
import jo.celeris.android.view.EditorThemes;
import jo.celeris.android.view.EditorView;

/**
 * La bascule de thème de l'atelier (D-13) : la rotation clair → sombre
 * → contraste élevé → thème importé (M7.6, H-07 — le fichier
 * {@code tokenColors} au format de VS Code, lu par l'importeur du
 * core-language), l'index persisté, le libellé du menu au courant —
 * extraite de l'activité (plafond de 600 lignes, ADR-002), l'activité
 * délègue.
 */
final class BasculeTheme {

    /** La clé de persistance de l'index de thème. */
    static final String CLE = "theme.index";

    private static final EditorTheme[] THEMES = {
            EditorThemes.CLAIR, EditorThemes.SOMBRE,
            EditorThemes.CONTRASTE_ELEVE, ImportedThemes.atelier() };

    private final AppCompatActivity activite;
    private final EditorView editeur;
    private final SharedPreferences prefs;
    /** Le rappel de changement — l'atelier des popups y redérive sa palette. */
    private @org.jspecify.annotations.Nullable Consumer<EditorTheme> surChangement;

    /**
     * Construit la bascule de thème.
     *
     * @param activite l'activité porteuse du menu et des libellés
     * @param editeur  la vue d'éditeur thémée
     * @param prefs    les préférences de la démonstration
     */
    BasculeTheme(AppCompatActivity activite, EditorView editeur,
            SharedPreferences prefs) {
        this.activite = activite;
        this.editeur = editeur;
        this.prefs = prefs;
    }

    /** L'index de thème courant, borné aux thèmes connus. */
    int indexCourant() {
        return Math.min(Math.max(prefs.getInt(CLE, 0), 0), THEMES.length - 1);
    }

    /** Le thème courant — l'atelier des popups en dérive sa palette. */
    EditorTheme themeCourant() {
        return THEMES[indexCourant()];
    }

    /**
     * Le rappel de changement de thème — invoqué après chaque
     * application (initiale comprise) avec le thème posé.
     *
     * @param rappel le consommateur de thème, null pour couper
     */
    void setSurChangement(
            @org.jspecify.annotations.Nullable Consumer<EditorTheme> rappel) {
        surChangement = rappel;
    }

    private void notifier(EditorTheme theme) {
        if (surChangement != null) {
            surChangement.accept(theme);
        }
    }

    /** Applique le thème persisté — l'image suivante s'y habille. */
    void appliquer() {
        editeur.setTheme(themeCourant());
        notifier(themeCourant());
        majTitre();
    }

    /** Tourne au thème suivant, persiste, le libellé du menu suit. */
    void tourner() {
        int suivant = (indexCourant() + 1) % THEMES.length;
        prefs.edit().putInt(CLE, suivant).apply();
        editeur.setTheme(THEMES[suivant]);
        notifier(THEMES[suivant]);
        majTitre();
    }

    /** Le libellé du menu dit le thème courant — l'item ne vit plus
     *  au menu système (la carte du kit le sert désormais) : la
     *  recherche ne trouve plus rien, le libellé de la CARTE dit le
     *  thème à l'ouverture. */
    void majTitre() {
        MaterialToolbar barre = activite.findViewById(R.id.barre_editeur);
        MenuItem item = barre.getMenu().findItem(R.id.action_theme);
        if (item != null) {
            item.setTitle(activite.getString(R.string.choix_theme, nom()));
        }
    }

    /** Le nom du thème courant — la carte de débordement y lit son
     *  libellé à l'ouverture (l'item du menu ne vit plus au système). */
    String nomCourant() {
        return nom();
    }

    private String nom() {
        return activite.getString(switch (indexCourant()) {
            case 1 -> R.string.theme_sombre;
            case 2 -> R.string.theme_contraste;
            case 3 -> R.string.theme_importe;
            default -> R.string.theme_clair;
        });
    }
}
