package jo.celeris.demo;

import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import jo.celeris.android.view.EditorTheme;
import jo.celeris.android.view.EditorThemes;
import jo.celeris.android.view.EditorView;

/**
 * La bascule de thème de l'atelier (D-13) : la rotation clair → sombre
 * → contraste élevé, l'index persisté, le libellé du menu au courant —
 * extraite de l'activité (plafond de 600 lignes, ADR-002), l'activité
 * délègue.
 */
final class BasculeTheme {

    /** La clé de persistance de l'index de thème. */
    static final String CLE = "theme.index";

    private static final EditorTheme[] THEMES = {
            EditorThemes.CLAIR, EditorThemes.SOMBRE,
            EditorThemes.CONTRASTE_ELEVE };

    private final AppCompatActivity activite;
    private final EditorView editeur;
    private final SharedPreferences prefs;

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

    /** Applique le thème persisté — l'image suivante s'y habille. */
    void appliquer() {
        editeur.setTheme(THEMES[indexCourant()]);
        majTitre();
    }

    /** Tourne au thème suivant, persiste, le libellé du menu suit. */
    void tourner() {
        int suivant = (indexCourant() + 1) % THEMES.length;
        prefs.edit().putInt(CLE, suivant).apply();
        editeur.setTheme(THEMES[suivant]);
        majTitre();
    }

    /** Le libellé du menu dit le thème courant. */
    void majTitre() {
        MaterialToolbar barre = activite.findViewById(R.id.barre_editeur);
        barre.getMenu().findItem(R.id.action_theme)
                .setTitle(activite.getString(R.string.choix_theme, nom()));
    }

    private String nom() {
        return activite.getString(switch (indexCourant()) {
            case 1 -> R.string.theme_sombre;
            case 2 -> R.string.theme_contraste;
            default -> R.string.theme_clair;
        });
    }
}
