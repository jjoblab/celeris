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
     * son palier, sa bande teintée, son filet de tête, son niveau
     * d'indentation (le motif PARAM du dépôt de départ — une
     * respiration par niveau, l'arbre des hiérarchies y creuse ses
     * branches) et son état d'EN-TÊTE DE SECTION (le motif NavMenu du
     * dépôt de départ : une section en petites capitales muettes ne se
     * prend pas — elle sert les rangées qui la suivent).
     */
    protected record Row(String text, int ink, Tier tier, boolean teintee,
            boolean filetAvant, int retrait, boolean enTete,
            @Nullable IconeCarte icone) {

        static Row of(String text) {
            return new Row(text, -1, Tier.CORPS, false, false, 0, false,
                    null);
        }

        static Row of(String text, int ink) {
            return new Row(text, ink, Tier.CORPS, false, false, 0, false,
                    null);
        }

        /** Une rangée à palier explicite. */
        static Row tier(String text, int ink, Tier tier) {
            return new Row(text, ink, tier, false, false, 0, false, null);
        }

        /** Une rangée sur bande teintée douce (le motif signature). */
        static Row teintee(String text, int ink, Tier tier) {
            return new Row(text, ink, tier, true, false, 0, false, null);
        }

        /** Une rangée indentée (le motif PARAM du dépôt de départ). */
        static Row indentee(String text, int ink, Tier tier) {
            return new Row(text, ink, tier, false, false, 1, false, null);
        }

        /** Une rangée d'arbre — une respiration par PROFONDEUR : la
         *  branche s'y creuse, le sommet racine vit à découvert. */
        static Row arbre(String text, int ink, Tier tier, int profondeur) {
            return new Row(text, ink, tier, false, false,
                    Math.max(0, profondeur), false, null);
        }

        /** Une rangée ICONISÉE — le glyphe vectoriel précède le texte
         *  (l'ampoule des intentions, le menu de l'IDE de référence). */
        static Row icone(IconeCarte glyphe, String text, int ink,
                Tier tier) {
            return new Row(text, ink, tier, false, false, 0, false, glyphe);
        }

        /**
         * Un EN-TÊTE DE SECTION du dépôt de départ (NavMenu) : petites
         * capitales à l'encre muette, jamais cliquable — elle annonce
         * les rangées qui la suivent.
         */
        static Row section(String texte) {
            return new Row(texte, -2, Tier.MICRO, false, false, 0, true,
                    null);
        }

        /** La même rangée, précédée d'un filet de séparation. */
        static Row apresFilet(Row rangee) {
            return new Row(rangee.text(), rangee.ink(), rangee.tier(),
                    rangee.teintee(), true, rangee.retrait(),
                    rangee.enTete(), rangee.icone());
        }
    }

    private final List<Row> rows = new ArrayList<>();
    /** La hauteur de chaque rangée posée — les paliers diffèrent. */
    private final List<Integer> hauteurs = new ArrayList<>();
    /** L'écart de respiration entre rangées, en pixels. */
    private final int ecartPx;
    /** L'indentation des rangées PARAM, en pixels. */
    private final int indentPx;
    /** La respiration entre l'icône et le texte, en pixels. */
    private final int ecartIconePx;
    /** La rangée surlignée (bande de teinte de sélection) — —1 pour aucune. */
    private int highlighted = -1;
    private @Nullable IntConsumer onPick;

    TextRowsPopup(Context context, PopupPalette palette) {
        super(context, palette);
        float densite = getResources().getDisplayMetrics().density;
        this.ecartPx = Math.round(PopupTokens.ECART_DP * densite);
        this.indentPx = Math.round(INDENT_DP * densite);
        this.ecartIconePx = Math.round(4f * densite);
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
            // l'en-tête de section prend l'encre muette et ses
            // petites capitales ; —2 le dit, —1 garde l'encre du panneau —
            // l'encre EXPLICITE se pose telle quelle (l'audit Android
            // Studio du 2026-11-14 : les encres de la palette vivent en
            // entiers NÉGATIFS (alpha opaque), le plonger au texte du
            // panneau mentait — le marqueur d'erreur, l'introuvable muet
            // et le cadre étranger rendaient à l'encre du texte)
            textPaint().setColor(encreDe(rangee.ink()));
            String texte = rangee.enTete()
                    ? rangee.text().toUpperCase() : rangee.text();
            Paint.FontMetrics metriques = textPaint().getFontMetrics();
            float ligne = y - metriques.top;
            float x = rangee.retrait() * indentPx;
            // l'icône précède le texte — le glyphe vectoriel à son
            // encre sémantique, le texte respire après sa cellule
            if (rangee.icone() != null) {
                dessinerIcone(canvas, rangee.icone(), x, y, hauteur);
                x += largeurIconePx(hauteur) + ecartIconePx;
            }
            canvas.drawText(texte, x, ligne, textPaint());
            y += hauteur;
        }
    }

    /**
     * L'icône d'une rangée — le glyphe vectoriel dans sa cellule
     * carrée (le côté de la hauteur utile, centré au milieu visuel
     * de la rangée) : l'AMPOULE vit à l'encre d'avertissement — le
     * disque du verre, le culot dessous, le glyphe de l'IDE de
     * référence.
     */
    private void dessinerIcone(Canvas canvas, IconeCarte icone, float x,
            float y, int hauteur) {
        float cellule = Math.max(0, hauteur - ecartPx);
        float centreX = x + cellule / 2f;
        float centreY = y + cellule / 2f;
        float rayon = cellule * 0.3f;
        bandPaint().setColor(icone.encre(palette));
        // le verre de l'ampoule : un disque plein
        canvas.drawCircle(centreX, centreY - rayon * 0.15f, rayon,
                bandPaint());
        // le culot : le petit rectangulaire sous le verre
        float demiCulot = rayon * 0.45f;
        canvas.drawRect(centreX - demiCulot, centreY + rayon * 0.7f,
                centreX + demiCulot, centreY + rayon * 1.2f, bandPaint());
    }

    /** La largeur de la cellule d'icône — le côté de la cellule
     *  carrée de la rangée. */
    private float largeurIconePx(int hauteur) {
        return Math.max(0, hauteur - ecartPx);
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
        for (int i = 0; i < rows.size(); i++) {
            Row rangee = rows.get(i);
            textPaint().setTextSize(tailleTexte(facteurDe(rangee.tier())));
            String texte = rangee.enTete()
                    ? rangee.text().toUpperCase() : rangee.text();
            float cellule = rangee.icone() == null ? 0f
                    : largeurIconePx(hauteurs.get(i)) + ecartIconePx;
            largeur = Math.max(largeur,
                    rangee.retrait() * indentPx + cellule
                            + textPaint().measureText(texte));
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

    /** L'icône de la rangée d'index donné — null si elle n'en porte
     *  pas ; les bancs y vérifient l'iconisation sans passer par le
     *  rendu. */
    protected final @Nullable IconeCarte iconeDe(int index) {
        if (index < 0 || index >= rows.size()) {
            return null;
        }
        return rows.get(index).icone();
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

    /** L'encre résolue d'une rangée : —1 garde l'encre du panneau, —2
     *  l'atténuée, toute autre valeur se pose TELLE QUELLE — l'encre
     *  explicite (les entiers négatifs des couleurs opaques comprises)
     *  n'est plus confondue avec les sentinelles. Les bancs y lisent
     *  la résolution sans passer par le rendu. */
    final int encreDe(int encreRangee) {
        if (encreRangee == -1) {
            return palette.text();
        }
        return encreRangee == -2 ? palette.muted() : encreRangee;
    }

    /** La rangée visée par l'ordonnée donnée, —1 si hors contenu ou
     *  sur un EN-TÊTE DE SECTION — une section ne se prend jamais. */
    private int rowAt(float y) {
        float curseur = 0f;
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).filetAvant()) {
                curseur += ecartPx;
            }
            if (y >= curseur && y < curseur + hauteurs.get(i)) {
                return rows.get(i).enTete() ? -1 : i;
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
