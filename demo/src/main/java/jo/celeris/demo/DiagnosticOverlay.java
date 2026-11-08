package jo.celeris.demo;

import android.widget.TextView;

import jo.celeris.android.view.EditorView;

/**
 * La surimpression de diagnostic (M7.5, §9 — livrée) : la ligne de
 * vérité du rendu posée sur la scène — taille de police, largeur de
 * gouttière, compteur d'images décolorées, retenue d'analyse. Le
 * propriétaire y lit en direct ce que le banc prouve : la largeur tient
 * (PIEGE-25), aucune image ne se décolore (PIEGE-26), la lenteur se
 * règle.
 *
 * <p>Le rafraîchissement vit sur une boucle courte (400 ms) tant que
 * l'activité la montre — la vue d'éditeur ne paie rien par image, la
 * surimpression relit ses valeurs au repos. Le formatage vit en
 * statique pur, testé sans appareil.</p>
 */
final class DiagnosticOverlay {

    /** La période de rafraîchissement de la surimpression, en ms. */
    private static final long PERIODE_MS = 400L;

    private final EditorView editeur;
    private final TextView vue;
    private final Runnable boucle = this::surBoucle;
    private boolean active;

    /**
     * Construit la surimpression de la scène.
     *
     * @param editeur la vue d'éditeur ouverte
     * @param vue     l'étiquette qui porte la ligne
     */
    DiagnosticOverlay(EditorView editeur, TextView vue) {
        this.editeur = editeur;
        this.vue = vue;
    }

    /**
     * La ligne de vérité, pure et testable : taille, gouttière,
     * images décolorées, retenue — chiffres du monde entier, unités
     * universelles.
     *
     * @param tailleSp         la taille de police, en sp
     * @param largeurGouttiere la largeur de gouttière, en pixels
     * @param imagesDecolorees le nombre d'images décolorées
     * @param retenueMs        le délai de retenue d'analyse, en ms
     * @return la ligne à poser sur la scène
     */
    static String line(float tailleSp, float largeurGouttiere,
            long imagesDecolorees, long retenueMs) {
        return Math.round(tailleSp) + " sp · "
                + Math.round(largeurGouttiere) + " px · "
                + imagesDecolorees + " img · " + retenueMs + " ms";
    }

    /** Démarre la boucle de rafraîchissement — idempotente. */
    void demarrer() {
        if (active) {
            return;
        }
        active = true;
        rafraichir();
        vue.postDelayed(boucle, PERIODE_MS);
    }

    /** Arrête la boucle — la scène se repose. */
    void arreter() {
        active = false;
        vue.removeCallbacks(boucle);
    }

    private void surBoucle() {
        if (!active) {
            return;
        }
        rafraichir();
        vue.postDelayed(boucle, PERIODE_MS);
    }

    private void rafraichir() {
        vue.setText(line(editeur.fontSizeSp(), editeur.gutterWidth(),
                editeur.discoloredFrames(), editeur.retenueAnalyseMs()));
    }
}
