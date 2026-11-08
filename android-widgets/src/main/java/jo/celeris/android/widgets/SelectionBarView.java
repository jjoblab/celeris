package jo.celeris.android.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * La barre de sélection (M7.5, §7, reprise — kit du dépôt de départ) :
 * les actions principales de la sélection en pastilles horizontales —
 * couper, copier, coller — centrées AU-DESSUS de la sélection,
 * l'appui exécute ; les actions qui ne tiennent pas la barre vivent
 * derrière la PUCE DE DÉBORDEMENT, carré arrondi au rayon du kit,
 * trois points à l'encre du texte — l'appui prévient l'hôte, qui y
 * ouvre le menu du même design. La pastille se surligne à l'appui de
 * la teinte de sélection, les séparateurs restent des filets
 * hairline. Cède à la frappe (la frappe remplace la sélection),
 * survit au défilement ; jamais de focus (PIEGE-27), le clavier
 * reste à l'éditeur.
 */
public final class SelectionBarView extends PopupPanel {

    /** Une action de la barre : son libellé et ce qu'elle fait. */
    public record Item(String label, Runnable action) {
    }

    /** La respiration entre pastilles, en dp. */
    private static final float ECART_DP = 10f;

    /** La part de la hauteur de rangée qui fait le côté de la puce. */
    private static final float PUCE_PART = 1.35f;

    /** L'index virtuel de la puce de débordement chez le toucher. */
    private static final int PUCE_DEBORDEMENT = -2;

    private final List<Item> actions = new ArrayList<>();
    private final List<Float> finsDePuce = new ArrayList<>();
    /** Les actions du débordement — servies par le menu de l'hôte. */
    private List<Item> debordement = List.of();
    private final int ecartPx;
    /** La pastille sous le doigt, —1 hors appui, PUCE_DEBORDEMENT
     *  quand le doigt pose la puce. */
    private int pressee = -1;
    /** Le rappel de l'appui sur la puce — l'hôte y ouvre son menu. */
    private @Nullable Runnable onOverflow;

    public SelectionBarView(Context context, PopupPalette palette) {
        super(context, palette);
        this.ecartPx = Math.round(ECART_DP
                * getResources().getDisplayMetrics().density);
    }

    /**
     * Pose les actions de la barre : les principales en pastilles, le
     * débordement derrière la puce — le menu de l'hôte les sert.
     *
     * @param principales les actions visibles, dans l'ordre d'affichage
     * @param debordement les actions repliées, vides pour cacher la puce
     */
    public void setActions(List<Item> principales, List<Item> debordement) {
        actions.clear();
        actions.addAll(principales);
        this.debordement = List.copyOf(debordement);
        finsDePuce.clear();
        textPaint().setTextSize(tailleTexte(PopupTokens.TEXTE_TITRE));
        float curseur = 0f;
        for (Item item : principales) {
            curseur += textPaint().measureText(item.label());
            finsDePuce.add(curseur);
            curseur += ecartPx;
        }
        requestLayout();
        invalidate();
    }

    /** Vrai quand la puce de débordement se montre — l'hôte y branche. */
    public boolean aDebordement() {
        return !debordement.isEmpty();
    }

    /**
     * Le rappel de l'appui sur la puce de débordement — l'hôte y ouvre
     * le menu qui sert les actions repliées.
     *
     * @param rappel le rappel, null pour le retirer
     */
    public void setOnOverflow(@Nullable Runnable rappel) {
        onOverflow = rappel;
    }

    /**
     * L'abscisse du CENTRE de la puce de débordement, dans le repère
     * du contenu — l'hôte y ancre son menu.
     */
    float puceDebordementCentreX() {
        return debutPuce() + cotePuce() * 0.5f;
    }

    /**
     * L'abscisse du CENTRE de la puce de débordement, dans le repère de
     * la VUE (respirations comprises) — l'hôte y ancre son menu.
     */
    public int ancrePuceDebordementX() {
        return Math.round(paddingPx + debutPuce() + cotePuce() * 0.5f);
    }

    @Override
    void drawContent(Canvas canvas) {
        textPaint().setTextSize(tailleTexte(PopupTokens.TEXTE_TITRE));
        float ligne = -textPaint().getFontMetrics().top;
        float x = 0f;
        for (int i = 0; i < actions.size(); i++) {
            if (i == pressee) {
                dessinerBandePressee(canvas, x, finsDePuce.get(i), ligne);
            }
            textPaint().setColor(palette.text());
            canvas.drawText(actions.get(i).label(), x, ligne, textPaint());
            x = finsDePuce.get(i) + ecartPx;
            if (i < actions.size() - 1) {
                tracerFiletVertical(canvas, x - ecartPx / 2f, ligne);
            }
        }
        if (!debordement.isEmpty()) {
            dessinerPuce(canvas, debutPuce(), ligne);
        }
    }

    /** La bande de la pastille pressée — la teinte de sélection du kit. */
    private void dessinerBandePressee(Canvas canvas, float x, float fin,
            float ligne) {
        bandPaint().setColor(palette.selection());
        float rayon = PopupTokens.RAYON_RANGEE_DP
                * getResources().getDisplayMetrics().density;
        canvas.drawRoundRect(x - 4f, ligne - rowHeightPx, fin + 4f, ligne,
                rayon, rayon, bandPaint());
    }

    /** Le filet hairline qui sépare deux pastilles. */
    private void tracerFiletVertical(Canvas canvas, float x, float ligne) {
        linePaint().setStrokeWidth(1f);
        canvas.drawLine(x, ligne - rowHeightPx, x, ligne, linePaint());
    }

    /**
     * La puce de débordement (le motif badge du kit) : carré arrondi
     * teinté à vingt pour cent de l'encre du texte — la teinte de
     * sélection à l'appui —, trois points à l'encre du texte.
     */
    private void dessinerPuce(Canvas canvas, float gauche, float ligne) {
        float cote = cotePuce();
        float haut = ligne - cote;
        float rayon = PopupTokens.RAYON_PUCE_DP
                * getResources().getDisplayMetrics().density;
        int fond = pressee == PUCE_DEBORDEMENT
                ? palette.selection()
                : (palette.text() & 0x00FFFFFF) | 0x33000000;
        bandPaint().setColor(fond);
        canvas.drawRoundRect(gauche, haut, gauche + cote, ligne,
                rayon, rayon, bandPaint());
        bandPaint().setColor(palette.text());
        for (int i = 0; i < 3; i++) {
            float cy = haut + cote * (0.25f + 0.25f * i);
            canvas.drawCircle(gauche + cote * 0.5f, cy,
                    Math.max(1f, cote * 0.08f), bandPaint());
        }
    }

    /** Le côté de la puce : une part de la hauteur de rangée, au moins
     *  la hauteur du texte. */
    private float cotePuce() {
        return rowHeightPx * PUCE_PART;
    }

    /** L'abscisse du bord gauche de la puce — après la dernière pastille. */
    private float debutPuce() {
        return finsDePuce.isEmpty() ? 0f
                : finsDePuce.get(finsDePuce.size() - 1) + ecartPx;
    }

    @Override
    int contentWidth() {
        if (finsDePuce.isEmpty()) {
            return 0;
        }
        float largeur = finsDePuce.get(finsDePuce.size() - 1);
        if (!debordement.isEmpty()) {
            largeur += ecartPx + cotePuce();
        }
        return Math.round(largeur);
    }

    @Override
    int contentHeight() {
        return actions.isEmpty() ? 0 : Math.max(rowHeightPx,
                Math.round(cotePuce()));
    }

    /** L'ordonnée du centre du contenu — les bancs y visent. */
    float centerY() {
        return paddingYPx + rowHeightPx / 2f;
    }

    /**
     * L'index de la pastille visée par l'abscisse, —1 si hors barre,
     * PUCE_DEBORDEMENT si la puce y tombe.
     */
    private int chipAt(float x) {
        for (int i = 0; i < finsDePuce.size(); i++) {
            if (x <= finsDePuce.get(i)) {
                return i;
            }
        }
        if (!debordement.isEmpty() && x >= debutPuce()
                && x <= debutPuce() + cotePuce()) {
            return PUCE_DEBORDEMENT;
        }
        return -1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            pressee = chipAt(event.getX() - paddingPx);
            invalidate();
            return true;
        }
        if (event.getActionMasked() != MotionEvent.ACTION_UP) {
            return false;
        }
        int index = chipAt(event.getX() - paddingPx);
        pressee = -1;
        invalidate();
        if (index == PUCE_DEBORDEMENT) {
            if (onOverflow != null) {
                onOverflow.run();
            }
            performClick();
            return true;
        }
        if (index >= 0 && index < actions.size()) {
            actions.get(index).action().run();
            performClick();
            return true;
        }
        return false;
    }
}
