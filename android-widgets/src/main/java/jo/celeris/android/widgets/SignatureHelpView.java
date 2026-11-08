package jo.celeris.android.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.List;

/**
 * L'aide aux paramètres (portage du design du dépôt de départ — B-07,
 * D-10) : la signature se rend en TROIS PALIERS — les paramètres déjà
 * frappés à l'encre d'accent, le paramètre ACTIF à l'accent et au
 * gras, les paramètres à venir en atténué ; la structure (nom,
 * parenthèses, virgules) garde l'encre du texte jusqu'à l'actif puis
 * s'atténue avec la queue — la ligne devient sa propre barre de
 * progression. Le placement au-dessus du curseur vit chez
 * {@link PopupAnchor} et {@link PopupPlacer}.
 */
public final class SignatureHelpView extends PopupPanel {

    private String libelle = "";

    public SignatureHelpView(Context context, PopupPalette palette) {
        super(context, palette);
    }

    /**
     * Pose la signature à montrer.
     *
     * @param model la signature active, ou null pour vider le panneau
     */
    public void setModel(SignatureAideModel model) {
        libelle = model == null ? "" : model.libelle();
        actif = model == null || model.parametreCourant() < 0
                ? -1 : model.parametreCourant();
        requestLayout();
        invalidate();
    }

    /** L'index du paramètre actif, —1 pour aucun. */
    private int actif = -1;

    @Override
    void drawContent(Canvas canvas) {
        if (libelle.isEmpty()) {
            return;
        }
        textPaint().setTextSize(tailleTexte(PopupTokens.TEXTE_CORPS));
        int[][] plages = plagesParametres(libelle);
        if (plages == null || plages.length == 0) {
            textPaint().setColor(palette.text());
            canvas.drawText(libelle, 0f, -textPaint().getFontMetrics().top,
                    textPaint());
            return;
        }
        int actifBorne = Math.max(0, Math.min(actif, plages.length - 1));
        float x = 0f;
        float base = -textPaint().getFontMetrics().top;
        int curseur = 0;
        for (int p = 0; p < plages.length; p++) {
            if (plages[p][0] > curseur) {
                x = dessinerSegment(canvas,
                        libelle.substring(curseur, plages[p][0]), x, base,
                        p <= actifBorne ? palette.text() : palette.muted(),
                        false);
            }
            boolean estActif = p == actifBorne;
            x = dessinerSegment(canvas,
                    libelle.substring(plages[p][0], plages[p][1]), x, base,
                    p <= actifBorne ? palette.accent() : palette.muted(),
                    estActif);
            curseur = plages[p][1];
        }
        if (curseur < libelle.length()) {
            dessinerSegment(canvas, libelle.substring(curseur), x, base,
                    palette.muted(), false);
        }
    }

    /** Un segment typé de la ligne de signature — renvoie le X suivant. */
    private float dessinerSegment(Canvas canvas, String segment, float x,
            float base, int couleur, boolean gras) {
        if (segment.isEmpty()) {
            return x;
        }
        textPaint().setColor(couleur);
        textPaint().setFakeBoldText(gras);
        canvas.drawText(segment, x, base, textPaint());
        textPaint().setFakeBoldText(false);
        return x + textPaint().measureText(segment);
    }

    @Override
    int contentWidth() {
        if (libelle.isEmpty()) {
            return 0;
        }
        textPaint().setTextSize(tailleTexte(PopupTokens.TEXTE_CORPS));
        return Math.round(textPaint().measureText(libelle));
    }

    @Override
    int contentHeight() {
        if (libelle.isEmpty()) {
            return 0;
        }
        textPaint().setTextSize(tailleTexte(PopupTokens.TEXTE_CORPS));
        Paint.FontMetrics metriques = textPaint().getFontMetrics();
        return Math.round(metriques.bottom - metriques.top);
    }

    /**
     * TOUTES les plages de paramètres [début, fin) d'un libellé de
     * signature — « ajouter(int a, int b) » rend deux plages —,
     * séparées par les virgules de profondeur nulle entre les
     * parenthèses, sans leurs espaces de bord. Null si le libellé
     * n'expose pas de couple de parenthèses.
     *
     * @param libelle le libellé de la signature
     * @return les plages, ou null sans parenthèses
     */
    static int[][] plagesParametres(String libelle) {
        if (libelle == null) {
            return null;
        }
        int ouverture = libelle.indexOf('(');
        if (ouverture < 0) {
            return null;
        }
        int fermeture = libelle.lastIndexOf(')');
        if (fermeture <= ouverture) {
            return null;
        }
        List<int[]> plages = new java.util.ArrayList<>();
        int profondeur = 0;
        int debutPlage = ouverture + 1;
        for (int i = ouverture + 1; i < fermeture; i++) {
            char c = libelle.charAt(i);
            if (c == '(' || c == '[' || c == '{') {
                profondeur++;
            } else if (c == ')' || c == ']' || c == '}') {
                profondeur--;
            } else if (c == ',' && profondeur == 0) {
                ajouterPlage(plages, libelle, debutPlage, i);
                debutPlage = i + 1;
            }
        }
        ajouterPlage(plages, libelle, debutPlage, fermeture);
        return plages.toArray(new int[0][]);
    }

    /** Ajoute la plage sans ses espaces de bord — une plage blanche s'ignore. */
    private static void ajouterPlage(List<int[]> plages, String libelle,
            int debut, int fin) {
        while (debut < fin && Character.isWhitespace(libelle.charAt(debut))) {
            debut++;
        }
        while (fin > debut && Character.isWhitespace(libelle.charAt(fin - 1))) {
            fin--;
        }
        if (debut < fin) {
            plages.add(new int[] {debut, fin});
        }
    }
}
