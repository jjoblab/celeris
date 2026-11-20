package jo.celeris.android.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import jo.celeris.android.widgets.internal.PopupPanel;

import java.util.List;
import java.util.function.IntConsumer;

import org.jspecify.annotations.Nullable;

/**
 * L'aide aux paramètres (portage du design du dépôt de départ — B-07,
 * D-10) : la signature se rend en TROIS PALIERS — les paramètres déjà
 * frappés à l'encre d'accent, le paramètre ACTIF à l'accent et au
 * gras, les paramètres à venir en atténué ; la structure (nom,
 * parenthèses, virgules) garde l'encre du texte jusqu'à l'actif puis
 * s'atténue avec la queue — la ligne devient sa propre barre de
 * progression. Le placement au-dessus du curseur vit chez
 * {@link PopupAnchor} et {@link PopupPlacer}.
 *
 * <p>La signature riche (chantier C7) : les surcharges vivent au
 * modèle — les voisines de la fenêtre ±1 se rendent atténuées autour
 * de l'active, le stepper ‹ i/n › navigue au toucher (le cycle
 * revient, jamais bloqué) et l'appui long ouvre le peek — la
 * documentation de la signature active sous un filet. Le stepper
 * prévient l'hôte par le rappel {@link #setOnStep(IntConsumer)}.</p>
 */
public final class SignatureHelpView extends PopupPanel {

    /** Le modèle courant, null pour vider le panneau. */
    private @Nullable SignatureAideModel model;
    /** Le rappel du pas du stepper — reçoit le nouvel index actif. */
    private @Nullable IntConsumer onStep;
    /** Le peek ouvert par l'appui long — la documentation montrée. */
    private boolean peekVisible;
    /** Le porteur de la bascule de l'appui long — le handler
     *  principal, la vue n'est pas encore attachée au premier appui. */
    private final Handler porteur = new Handler(Looper.getMainLooper());
    /** La bascule de l'appui long — retirée au relèvement du doigt. */
    private final Runnable basculePeek = new Runnable() {
        @Override
        public void run() {
            setPeekVisible(!peekVisible);
        }
    };

    public SignatureHelpView(Context context, PopupPalette palette) {
        super(context, palette);
    }

    /**
     * Pose la signature à montrer.
     *
     * @param model la signature active, ou null pour vider le panneau
     */
    public void setModel(SignatureAideModel model) {
        this.model = model;
        requestLayout();
        invalidate();
    }

    /**
     * Pose le rappel du stepper : chaque pas reçoit l'index de la
     * nouvelle signature active — l'hôte y lit, la vue a déjà marché.
     *
     * @param rappel le consommateur d'index, null pour couper
     */
    public void setOnStep(@Nullable IntConsumer rappel) {
        this.onStep = rappel;
    }

    /** Le peek est-il ouvert ? */
    public boolean isPeekVisible() {
        return peekVisible;
    }

    /** Ouvre ou ferme le peek — l'appui long bascule, l'hôte peut forcer. */
    public void setPeekVisible(boolean visible) {
        this.peekVisible = visible;
        requestLayout();
        invalidate();
    }

    @Override
    public void drawContent(Canvas canvas) {
        SignatureAideModel courant = model;
        if (courant == null) {
            return;
        }
        float y = 0f;
        y = drawNeighbor(canvas, courant, -1, y);
        y = drawActive(canvas, courant, y);
        y = drawNeighbor(canvas, courant, 1, y);
        if (courant.nombreSignatures() > 1) {
            y = drawStepper(canvas, courant, y);
        }
        if (peekVisible && courant.peek() != null) {
            drawPeek(canvas, courant, y);
        }
    }

    /** La voisine de la fenêtre au-dessus (-1) ou en dessous (1) de l'active. */
    private float drawNeighbor(Canvas canvas, SignatureAideModel courant,
            int cote, float y) {
        int index = courant.signatureActive() + cote;
        if (index < 0 || index >= courant.nombreSignatures()) {
            return y;
        }
        textPaint().setTextSize(textSize(PopupTokens.TEXTE_META));
        textPaint().setColor(palette.muted());
        float base = y - textPaint().getFontMetrics().top;
        canvas.drawText(courant.window().get(index - borneBasse(courant)).label(),
                0f, base, textPaint());
        return y + metaHeight();
    }

    /** La signature active : ses trois paliers, la structure en barre de progression. */
    private float drawActive(Canvas canvas, SignatureAideModel courant, float y) {
        String label = courant.label();
        textPaint().setTextSize(textSize(PopupTokens.TEXTE_CORPS));
        int actif = courant.parametreCourant();
        int[][] ranges = plagesParametres(label);
        if (ranges == null || ranges.length == 0) {
            textPaint().setColor(palette.text());
            canvas.drawText(label, 0f, y - textPaint().getFontMetrics().top,
                    textPaint());
            return y + bodyHeight();
        }
        int actifBorne = Math.max(0, Math.min(actif, ranges.length - 1));
        float x = 0f;
        float base = y - textPaint().getFontMetrics().top;
        int cursor = 0;
        for (int p = 0; p < ranges.length; p++) {
            if (ranges[p][0] > cursor) {
                x = drawSegment(canvas,
                        label.substring(cursor, ranges[p][0]), x, base,
                        p <= actifBorne ? palette.text() : palette.muted(),
                        false);
            }
            boolean estActif = p == actifBorne;
            x = drawSegment(canvas,
                    label.substring(ranges[p][0], ranges[p][1]), x, base,
                    p <= actifBorne ? palette.accent() : palette.muted(),
                    estActif);
            cursor = ranges[p][1];
        }
        if (cursor < label.length()) {
            drawSegment(canvas, label.substring(cursor), x, base,
                    palette.muted(), false);
        }
        return y + bodyHeight();
    }

    /** Le stepper ‹ i/n › — les chevrons aux ailes, le compte au centre. */
    private float drawStepper(Canvas canvas, SignatureAideModel courant, float y) {
        textPaint().setTextSize(textSize(PopupTokens.TEXTE_MICRO));
        textPaint().setColor(palette.muted());
        float base = y - textPaint().getFontMetrics().top;
        String compte = (courant.signatureActive() + 1) + "/"
                + courant.nombreSignatures();
        float largeurCompte = textPaint().measureText(compte);
        float centre = contentWidth() / 2f;
        canvas.drawText("<", centre - largeurCompte / 2f - aileStepper(), base,
                textPaint());
        textPaint().setColor(palette.text());
        canvas.drawText(compte, centre - largeurCompte / 2f, base, textPaint());
        textPaint().setColor(palette.muted());
        canvas.drawText(">", centre + largeurCompte / 2f + aileStepper(), base,
                textPaint());
        return y + microHeight();
    }

    /** Le peek : la documentation sous un filet, atténuée. */
    private void drawPeek(Canvas canvas, SignatureAideModel courant, float y) {
        textPaint().setTextSize(textSize(PopupTokens.TEXTE_META));
        textPaint().setColor(palette.muted());
        float line = y + gapPx();
        traceRule(canvas, line - gapPx() / 2f);
        float base = line - textPaint().getFontMetrics().top;
        canvas.drawText(courant.peek(), 0f, base, textPaint());
    }

    /** Un segment typé de la ligne de signature — renvoie le X suivant. */
    private float drawSegment(Canvas canvas, String segment, float x,
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

    /** La borne basse de la fenêtre ±1 du modèle. */
    private int borneBasse(SignatureAideModel courant) {
        return Math.max(0, courant.signatureActive() - 1);
    }

    /** La hauteur d'une rangée de voisine, au palier méta. */
    private int metaHeight() {
        textPaint().setTextSize(textSize(PopupTokens.TEXTE_META));
        Paint.FontMetrics metriques = textPaint().getFontMetrics();
        return Math.round(metriques.bottom - metriques.top);
    }

    /** La hauteur du stepper, au palier micro. */
    private int microHeight() {
        textPaint().setTextSize(textSize(PopupTokens.TEXTE_MICRO));
        Paint.FontMetrics metriques = textPaint().getFontMetrics();
        return Math.round(metriques.bottom - metriques.top);
    }

    /** La hauteur de la signature active, au palier corps. */
    private int bodyHeight() {
        textPaint().setTextSize(textSize(PopupTokens.TEXTE_CORPS));
        Paint.FontMetrics metriques = textPaint().getFontMetrics();
        return Math.round(metriques.bottom - metriques.top);
    }

    /** L'aile du stepper : l'écart entre un chevron et le compte (dp). */
    private float aileStepper() {
        return 8f * getResources().getDisplayMetrics().density;
    }

    /** L'écart d'une section de peek (dp). */
    private float gapPx() {
        return PopupTokens.GAP_DP * getResources().getDisplayMetrics().density;
    }

    @Override
    public int contentWidth() {
        SignatureAideModel courant = model;
        if (courant == null) {
            return 0;
        }
        float width = 0f;
        for (SignatureAideModel.Entry input : courant.window()) {
            textPaint().setTextSize(textSize(input == courant.active()
                    ? PopupTokens.TEXTE_CORPS : PopupTokens.TEXTE_META));
            width = Math.max(width,
                    textPaint().measureText(input.label()));
        }
        if (peekVisible && courant.peek() != null) {
            textPaint().setTextSize(textSize(PopupTokens.TEXTE_META));
            width = Math.max(width,
                    textPaint().measureText(courant.peek()));
        }
        return Math.round(width);
    }

    @Override
    public int contentHeight() {
        SignatureAideModel courant = model;
        if (courant == null) {
            return 0;
        }
        int height = 0;
        int voisines = courant.window().size() - 1;
        height += voisines * metaHeight();
        height += bodyHeight();
        if (courant.nombreSignatures() > 1) {
            textPaint().setTextSize(textSize(PopupTokens.TEXTE_MICRO));
            Paint.FontMetrics micro = textPaint().getFontMetrics();
            height += Math.round(micro.bottom - micro.top);
        }
        if (peekVisible && courant.peek() != null) {
            height += Math.round(2f * gapPx()) + metaHeight();
        }
        return height;
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        SignatureAideModel courant = model;
        if (courant == null) {
            return false;
        }
        switch (event.getActionMasked()) {
            case android.view.MotionEvent.ACTION_DOWN -> {
                // l'appui long bascule le peek (C7) : la demi-seconde du doigt
                porteur.removeCallbacks(basculePeek);
                porteur.postDelayed(basculePeek, 500L);
                return true;
            }
            case android.view.MotionEvent.ACTION_UP -> {
                porteur.removeCallbacks(basculePeek);
                return pasDuStepper(courant, event.getX());
            }
            default -> {
                porteur.removeCallbacks(basculePeek);
                return false;
            }
        }
    }

    /**
     * Le pas du stepper : le toucher à gauche du centre recule, à
     * droite avance — une seule signature ne passe pas, le rappel
     * reçoit la nouvelle active.
     */
    private boolean pasDuStepper(SignatureAideModel courant, float x) {
        if (courant.nombreSignatures() <= 1) {
            return false;
        }
        SignatureAideModel voisin = x < contentWidth() / 2f
                ? courant.precedente() : courant.suivante();
        setModel(voisin);
        if (onStep != null) {
            onStep.accept(voisin.signatureActive());
        }
        performClick();
        return true;
    }

    /**
     * TOUTES les plages de paramètres [début, fin) d'un libellé de
     * signature — « ajouter(int a, int b) » rend deux plages —,
     * séparées par les virgules de profondeur nulle entre les
     * parenthèses, sans leurs espaces de bord. Null si le libellé
     * n'expose pas de couple de parenthèses.
     *
     * @param label le libellé de la signature
     * @return les plages, ou null sans parenthèses
     */
    static int[][] plagesParametres(String label) {
        if (label == null) {
            return null;
        }
        int ouverture = label.indexOf('(');
        if (ouverture < 0) {
            return null;
        }
        int fermeture = label.lastIndexOf(')');
        if (fermeture <= ouverture) {
            return null;
        }
        List<int[]> ranges = new java.util.ArrayList<>();
        int profondeur = 0;
        int debutPlage = ouverture + 1;
        for (int i = ouverture + 1; i < fermeture; i++) {
            char c = label.charAt(i);
            if (c == '(' || c == '[' || c == '{') {
                profondeur++;
            } else if (c == ')' || c == ']' || c == '}') {
                profondeur--;
            } else if (c == ',' && profondeur == 0) {
                addRange(ranges, label, debutPlage, i);
                debutPlage = i + 1;
            }
        }
        addRange(ranges, label, debutPlage, fermeture);
        return ranges.toArray(new int[0][]);
    }

    /** Ajoute la plage sans ses espaces de bord — une plage blanche s'ignore. */
    private static void addRange(List<int[]> ranges, String label,
            int start, int end) {
        while (start < end && Character.isWhitespace(label.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(label.charAt(end - 1))) {
            end--;
        }
        if (start < end) {
            ranges.add(new int[] {start, end});
        }
    }
}
