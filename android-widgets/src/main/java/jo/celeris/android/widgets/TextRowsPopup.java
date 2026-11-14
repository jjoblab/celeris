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

    /** La respiration derrière un pictogramme de rangée, en dp — les
     *  actions des menus portent leur glyphe, le libellé respire
     *  après lui (l'audit du 2026-11-14 : jamais la même icône pour
     *  toutes). */
    private static final float ECART_PICTO_DP = 5f;

    /**
     * Une rangée : son texte, son encre (—1 pour l'encre du panneau),
     * son palier, sa bande teintée, son filet de tête, son niveau
     * d'indentation (le motif PARAM du dépôt de départ — une
     * respiration par niveau, l'arbre des hiérarchies y creuse ses
     * branches), son pictogramme d'action (null : rangée sans glyphe)
     * et son état d'EN-TÊTE DE SECTION (le motif NavMenu du dépôt de
     * départ : une section en petites capitales muettes ne se prend
     * pas — elle sert les rangées qui la suivent).
     */
    protected record Row(String text, int ink, Tier tier, boolean teintee,
            boolean filetAvant, int retrait, IconeAction picto,
            boolean enTete) {

        static Row of(String text) {
            return new Row(text, -1, Tier.CORPS, false, false, 0, null, false);
        }

        static Row of(String text, int ink) {
            return new Row(text, ink, Tier.CORPS, false, false, 0, null, false);
        }

        /** Une rangée à palier explicite. */
        static Row tier(String text, int ink, Tier tier) {
            return new Row(text, ink, tier, false, false, 0, null, false);
        }

        /** Une rangée d'action à palier titre et pictogramme propre —
         *  les menus y posent le glyphe de leur action, jamais la
         *  même pour toutes (l'audit du 2026-11-14). */
        static Row action(String text, int ink, IconeAction picto) {
            return new Row(text, ink, Tier.TITRE, false, false, 0, picto,
                    false);
        }

        /** Une rangée sur bande teintée douce (le motif signature). */
        static Row teintee(String text, int ink, Tier tier) {
            return new Row(text, ink, tier, true, false, 0, null, false);
        }

        /** Une rangée indentée (le motif PARAM du dépôt de départ). */
        static Row indentee(String text, int ink, Tier tier) {
            return new Row(text, ink, tier, false, false, 1, null, false);
        }

        /** Une rangée d'arbre — une respiration par PROFONDEUR : la
         *  branche s'y creuse, le sommet racine vit à découvert. */
        static Row arbre(String text, int ink, Tier tier, int profondeur) {
            return new Row(text, ink, tier, false, false,
                    Math.max(0, profondeur), null, false);
        }

        /**
         * Un EN-TÊTE DE SECTION du dépôt de départ (NavMenu) : petites
         * capitales à l'encre muette, jamais cliquable — elle annonce
         * les rangées qui la suivent.
         */
        static Row section(String texte) {
            return new Row(texte, -2, Tier.MICRO, false, false, 0, null, true);
        }

        /** La même rangée, précédée d'un filet de séparation. */
        static Row apresFilet(Row rangee) {
            return new Row(rangee.text(), rangee.ink(), rangee.tier(),
                    rangee.teintee(), true, rangee.retrait(),
                    rangee.picto(), rangee.enTete());
        }
    }

    private final List<Row> rows = new ArrayList<>();
    /** La hauteur de chaque rangée posée — les paliers diffèrent. */
    private final List<Integer> hauteurs = new ArrayList<>();
    /** L'écart de respiration entre rangées, en pixels. */
    private final int ecartPx;
    /** L'indentation des rangées PARAM, en pixels. */
    private final int indentPx;
    /** La respiration derrière un pictogramme de rangée, en pixels. */
    private final int ecartPictoPx;
    /** La rangée surlignée (bande de teinte de sélection) — —1 pour aucune. */
    private int highlighted = -1;
    private @Nullable IntConsumer onPick;

    TextRowsPopup(Context context, PopupPalette palette) {
        super(context, palette);
        float densite = getResources().getDisplayMetrics().density;
        this.ecartPx = Math.round(PopupTokens.ECART_DP * densite);
        this.indentPx = Math.round(INDENT_DP * densite);
        this.ecartPictoPx = Math.round(ECART_PICTO_DP * densite);
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
            if (rangee.picto() != null) {
                // le glyphe de l'action précède son libellé — l'encre de
                // la rangée l'habille, la graisse suit le rayon
                dessinerPicto(rangee, canvas, x, ligne);
                x += emprisePicto(rangee.tier());
            }
            canvas.drawText(texte, x, ligne, textPaint());
            y += hauteur;
        }
    }

    /** Le pictogramme de la rangée — tracé au centre vertical du
     *  texte, au demi-rayon de sa taille : le glyphe suit le texte
     *  (FACTEUR_PICTO), il ne le domine plus (l'audit du
     *  propriétaire du 2026-11-14 : « les icônes sont un peu trop
     *  grosses ») ; l'encre ALLÉGÉE le sert — l'ampoule seule garde
     *  sa couleur (« il est trop foncé »). */
    private void dessinerPicto(Row rangee, Canvas canvas, float x,
            float ligne) {
        float taille = textPaint().getTextSize();
        float rayon = taille * PopupTokens.FACTEUR_PICTO;
        IconeAction.dessiner(rangee.picto(), canvas, linePaint(),
                textPaint(), x + rayon, ligne - taille * 0.35f, rayon,
                IconeAction.encreAllgeee(palette.muted(), encreDe(rangee.ink())));
    }

    /** L'emprise horizontale du pictogramme au palier donné — le
     *  glyphe et sa respiration, la mesure y compte le libellé. */
    private float emprisePicto(Tier tier) {
        return tailleTexte(facteurDe(tier)) * PopupTokens.FACTEUR_PICTO
                + ecartPictoPx;
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
            String texte = rangee.enTete()
                    ? rangee.text().toUpperCase() : rangee.text();
            float picto = rangee.picto() == null ? 0f
                    : emprisePicto(rangee.tier());
            largeur = Math.max(largeur,
                    rangee.retrait() * indentPx + picto
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

    /** Le pictogramme de la rangée d'index donné, null hors contenu ou
     *  sur une rangée sans glyphe — les bancs y lisent les icônes
     *  propres sans passer par le rendu. */
    protected final @Nullable IconeAction pictoDe(int index) {
        if (index < 0 || index >= rows.size()) {
            return null;
        }
        return rows.get(index).picto();
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
