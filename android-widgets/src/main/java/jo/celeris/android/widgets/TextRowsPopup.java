package jo.celeris.android.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import org.jspecify.annotations.Nullable;

/**
 * Le panneau à rangées de texte des popups (portage du rendu du dépôt
 * de départ) : les contenus verticaux — complétion, documentation,
 * signature, intentions, info-bulle — partagent la rangée typée du kit
 * : un texte, une encre, un palier typographique
 * ({@link Tier}), une bande de surlignage (la teinte de sélection,
 * encadrée à un pixel comme au dépôt de départ), une bande teintée
 * douce (les signatures de la documentation) et un filet de séparation
 * devant la rangée qui l'arme. L'appui sur une rangée prévient l'hôte
 * : la sélection d'un candidat ou d'une intention.
 */
abstract class TextRowsPopup extends PopupPanel {

    /** Les paliers typographiques d'une rangée (l'échelle du kit). */
    protected enum Tier {
        /** Titre de carte / libellé principal. */
        TITRE,
        /** Corps de texte. */
        CORPS,
        /** Méta — détail, conteneur. */
        META,
        /** Micro-typographie — compteur, légende. */
        MICRO
    }

    /** La respiration d'une indentation de rangée, en dp (le motif
     *  PARAM du dépôt de départ). */
    private static final float INDENT_DP = 8f;

    /**
     * Une rangée : son texte, son encre (—1 pour l'encre du panneau),
     * son palier, sa bande teintée, son filet de tête et son
     * indentation éventuelle.
     */
    protected record Row(String text, int ink, Tier tier, boolean teintee,
            boolean filetAvant, boolean indentee) {

        static Row of(String text) {
            return new Row(text, -1, Tier.CORPS, false, false, false);
        }

        static Row of(String text, int ink) {
            return new Row(text, ink, Tier.CORPS, false, false, false);
        }

        /** Une rangée à palier explicite. */
        static Row tier(String text, int ink, Tier tier) {
            return new Row(text, ink, tier, false, false, false);
        }

        /** Une rangée sur bande teintée douce (le motif signature). */
        static Row teintee(String text, int ink, Tier tier) {
            return new Row(text, ink, tier, true, false, false);
        }

        /** Une rangée indentée (le motif PARAM du dépôt de départ). */
        static Row indentee(String text, int ink, Tier tier) {
            return new Row(text, ink, tier, false, false, true);
        }

        /** La même rangée, précédée d'un filet de séparation. */
        static Row apresFilet(Row rangee) {
            return new Row(rangee.text(), rangee.ink(), rangee.tier(),
                    rangee.teintee(), true, rangee.indentee());
        }
    }

    private final List<Row> rows = new ArrayList<>();
    /** La hauteur de chaque rangée posée — les paliers diffèrent. */
    private final List<Integer> hauteurs = new ArrayList<>();
    /** L'écart de respiration entre rangées, en pixels. */
    private final int ecartPx;
    /** L'indentation des rangées PARAM, en pixels. */
    private final int indentPx;
    /** La rangée surlignée (bande de teinte de sélection) — —1 pour aucune. */
    private int highlighted = -1;
    private @Nullable IntConsumer onPick;

    TextRowsPopup(Context context, PopupPalette palette) {
        super(context, palette);
        float densite = getResources().getDisplayMetrics().density;
        this.ecartPx = Math.round(PopupTokens.ECART_DP * densite);
        this.indentPx = Math.round(INDENT_DP * densite);
    }

    /** Pose les rangées — le panneau se redimensionne et se redessine. */
    protected final void setRows(List<Row> nouvelles) {
        rows.clear();
        rows.addAll(nouvelles);
        hauteurs.clear();
        for (Row rangee : rows) {
            Paint.FontMetrics metriques = metriquesDe(rangee.tier());
            hauteurs.add(Math.round(metriques.bottom - metriques.top
                    + ecartPx));
        }
        requestLayout();
        invalidate();
    }

    /** Les métriques du pinceau posé au palier donné. */
    private Paint.FontMetrics metriquesDe(Tier tier) {
        textPaint().setTextSize(tailleTexte(facteurDe(tier)));
        return textPaint().getFontMetrics();
    }

    /** Le facteur d'échelle d'un palier. */
    private static float facteurDe(Tier tier) {
        return switch (tier) {
            case TITRE -> PopupTokens.TEXTE_TITRE;
            case CORPS -> PopupTokens.TEXTE_CORPS;
            case META -> PopupTokens.TEXTE_META;
            case MICRO -> PopupTokens.TEXTE_MICRO;
        };
    }

    /**
     * La rangée surlignée (bande de teinte de sélection), —1 pour
     * aucune.
     */
    protected final void setHighlighted(int index) {
        highlighted = index;
        invalidate();
    }

    /** Le rappel de l'appui sur une rangée — l'hôte exécute. */
    public final void setOnPick(@Nullable IntConsumer rappel) {
        onPick = rappel;
    }

    @Override
    void drawContent(Canvas canvas) {
        float y = 0f;
        for (int i = 0; i < rows.size(); i++) {
            Row rangee = rows.get(i);
            int hauteur = hauteurs.get(i);
            if (rangee.filetAvant()) {
                tracerFilet(canvas, y);
                y += ecartPx;
            }
            if (rangee.teintee()) {
                dessinerBande(canvas, y, hauteur + ecartPx,
                        palette.selection());
            }
            if (i == highlighted) {
                dessinerBande(canvas, y, hauteur,
                        palette.selection());
            }
            textPaint().setTextSize(tailleTexte(facteurDe(rangee.tier())));
            textPaint().setColor(
                    rangee.ink() < 0 ? palette.text() : rangee.ink());
            Paint.FontMetrics metriques = textPaint().getFontMetrics();
            float ligne = y - metriques.top;
            float x = rangee.indentee() ? indentPx : 0f;
            canvas.drawText(rangee.text(), x, ligne, textPaint());
            y += hauteur;
        }
    }

    /**
     * La bande pleine largeur, encadrée à un pixel (le motif du dépôt
     * de départ : la bande vit sous le clip arrondi de la carte).
     */
    private void dessinerBande(Canvas canvas, float y, float hauteur,
            int couleur) {
        bandPaint().setColor(couleur);
        canvas.drawRect(-paddingPx + 1f, y,
                contentWidth() + paddingPx - 1f, y + hauteur, bandPaint());
    }

    @Override
    int contentWidth() {
        float largeur = 0f;
        for (Row rangee : rows) {
            textPaint().setTextSize(tailleTexte(facteurDe(rangee.tier())));
            largeur = Math.max(largeur,
                    (rangee.indentee() ? indentPx : 0f)
                            + textPaint().measureText(rangee.text()));
        }
        return Math.round(largeur);
    }

    @Override
    int contentHeight() {
        if (rows.isEmpty()) {
            return 0;
        }
        int hauteur = 0;
        for (int i = 0; i < rows.size(); i++) {
            hauteur += hauteurs.get(i);
            if (rows.get(i).filetAvant()) {
                hauteur += ecartPx;
            }
        }
        return hauteur;
    }

    /** Le nombre de rangées courantes — lisibilité des sous-classes. */
    protected final int rowCount() {
        return rows.size();
    }

    /**
     * L'ordonnée du CENTRE de la rangée donnée, respirations comprises
     * — les bancs y visent leur appui.
     */
    final float rowCenterY(int index) {
        if (hauteurs.isEmpty()) {
            return paddingYPx;
        }
        float y = 0f;
        for (int i = 0; i < index && i < hauteurs.size(); i++) {
            y += hauteurs.get(i);
            if (rows.get(i).filetAvant()) {
                y += ecartPx;
            }
        }
        return y + hauteurs.get(Math.min(index, hauteurs.size() - 1)) / 2f;
    }

    /** La rangée visée par l'ordonnée donnée, —1 si hors contenu. */
    private int rowAt(float y) {
        float curseur = 0f;
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).filetAvant()) {
                curseur += ecartPx;
            }
            if (y >= curseur && y < curseur + hauteurs.get(i)) {
                return i;
            }
            curseur += hauteurs.get(i);
        }
        return -1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_UP || onPick == null) {
            return event.getActionMasked() == MotionEvent.ACTION_DOWN;
        }
        int index = rowAt(event.getY() - paddingYPx);
        if (index >= 0) {
            onPick.accept(index);
            performClick();
            return true;
        }
        return false;
    }
}
