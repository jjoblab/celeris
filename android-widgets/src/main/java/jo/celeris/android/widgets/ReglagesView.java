package jo.celeris.android.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * La carte des réglages de l'éditeur (l'audit du propriétaire du
 * 2026-11-14 : « ajoute un système central editor settings pour
 * pouvoir activer désactiver, etc. certaines fonctionnalités ») : les
 * commutateurs des fonctionnalités s'y lisent en rangées denses — le
 * libellé à gauche, l'INTERRUPTEUR DESSINÉ à droite (la glissière et
 * son pouce, à l'accent quand la porte est ouverte, au liseré
 * fermée), la rangée pressée se surligne. La carte ne connaît AUCUNE
 * porte : l'hôte y pose les libellés et l'état, la bascule lui
 * revient — le point de vérité vit chez lui.
 *
 * <p>PIEGE-27 par construction : jamais de focus, le clavier reste à
 * l'éditeur ; l'appui sur une rangée bascule SA porte et l'annonce.</p>
 */
public final class ReglagesView extends PopupPanel {

    /** Un commutateur de la carte : son libellé, son pictogramme
     *  (null : sans glyphe), son état courant et sa bascule —
     *  l'hôte reste le point de vérité. */
    public record Interrupteur(String libelle, IconeAction picto,
            boolean ouvert, Runnable bascule) {
    }

    /** La hauteur d'une rangée de commutateur, en dp — dense, la
     *  compacité de l'IDE de référence. */
    private static final float HAUTEUR_RANGEE_DP = 34f;

    /** La largeur de la glissière de l'interrupteur, en dp. */
    private static final float GLISSIERE_DP = 26f;

    /** La hauteur de la glissière, en dp. */
    private static final float HAUTEUR_GLISSIERE_DP = 14f;

    /** Le rayon du pouce, en dp. */
    private static final float POUCE_DP = 8f;

    /** Les commutateurs courants. */
    private List<Interrupteur> interrupteurs = List.of();
    /** La rangée sous le doigt, —1 hors appui. */
    private int pressee = -1;
    /** La hauteur d'une rangée, en pixels. */
    private final float rangeePx;
    /** La glissière, en pixels. */
    private final float glissierePx;
    private final float hauteurGlissierePx;
    private final float poucePx;

    public ReglagesView(Context context, PopupPalette palette) {
        super(context, palette);
        float densite = getResources().getDisplayMetrics().density;
        this.rangeePx = HAUTEUR_RANGEE_DP * densite;
        this.glissierePx = GLISSIERE_DP * densite;
        this.hauteurGlissierePx = HAUTEUR_GLISSIERE_DP * densite;
        this.poucePx = POUCE_DP * densite;
    }

    /**
     * Pose les commutateurs de la carte — l'état se relit à chaque
     * pose, la carte ne retient rien.
     *
     * @param interrupteurs les commutateurs, dans l'ordre d'affichage
     */
    public void setInterrupteurs(List<Interrupteur> interrupteurs) {
        this.interrupteurs = List.copyOf(interrupteurs);
        requestLayout();
        invalidate();
    }

    @Override
    void drawContent(Canvas canvas) {
        for (int i = 0; i < interrupteurs.size(); i++) {
            dessinerRangee(canvas, interrupteurs.get(i), i);
        }
    }

    /** Une rangée : le libellé au palier titre, SON pictogramme à
     *  l'encre allégée, l'interrupteur dessiné à droite — la
     *  glissière et son pouce, à l'accent ouverte, au liseré fermée. */
    private void dessinerRangee(Canvas canvas, Interrupteur inter, int index) {
        float haut = index * rangeePx;
        if (pressee == index) {
            bandPaint().setColor(palette.selection());
            canvas.drawRect(-paddingPx + 1f, haut,
                    contentWidth() + paddingPx - 1f, haut + rangeePx,
                    bandPaint());
        }
        textPaint().setTextSize(tailleTexte(PopupTokens.TEXTE_TITRE));
        textPaint().setColor(palette.text());
        Paint.FontMetrics metriques = textPaint().getFontMetrics();
        float ligne = haut + (rangeePx + metriques.bottom - metriques.top)
                / 2f - metriques.bottom;
        float x = 0f;
        if (inter.picto() != null) {
            float rayon = textPaint().getTextSize()
                    * PopupTokens.FACTEUR_PICTO;
            IconeAction.dessiner(inter.picto(), canvas, linePaint(),
                    textPaint(), x + rayon, ligne - textPaint().getTextSize()
                            * 0.35f, rayon,
                    IconeAction.encreAllgeee(palette.muted(), palette.text()));
            x += 2 * rayon + 5f * getResources().getDisplayMetrics().density;
        }
        canvas.drawText(inter.libelle(), x, ligne, textPaint());
        dessinerInterrupteur(canvas, inter.ouvert(),
                contentWidth() - glissierePx,
                haut + (rangeePx - hauteurGlissierePx) / 2f);
    }

    /** L'interrupteur dessiné : la glissière arrondie — remplie à
     *  l'accent quand la porte est ouverte, au verre du liseré
     *  fermée — et son pouce, à droite ouverte, à gauche fermée. */
    private void dessinerInterrupteur(Canvas canvas, boolean ouvert,
            float gauche, float haut) {
        float rayon = hauteurGlissierePx / 2f;
        // la glissière
        bandPaint().setColor(ouvert ? palette.accent() : palette.border());
        canvas.drawRoundRect(gauche, haut, gauche + glissierePx,
                haut + hauteurGlissierePx, rayon, rayon, bandPaint());
        // le pouce — le cercle du verre, à droite ouverte
        float centreX = ouvert ? gauche + glissierePx - poucePx
                : gauche + poucePx;
        bandPaint().setColor(palette.background());
        canvas.drawCircle(centreX, haut + rayon, poucePx * 0.8f, bandPaint());
    }

    @Override
    int contentWidth() {
        float largeur = 0f;
        float densite = getResources().getDisplayMetrics().density;
        for (Interrupteur inter : interrupteurs) {
            textPaint().setTextSize(tailleTexte(PopupTokens.TEXTE_TITRE));
            float picto = inter.picto() == null ? 0f
                    : 2 * textPaint().getTextSize()
                            * PopupTokens.FACTEUR_PICTO + 5f * densite;
            largeur = Math.max(largeur, picto
                    + textPaint().measureText(inter.libelle())
                    + glissierePx + 2 * poucePx);
        }
        return Math.round(largeur);
    }

    @Override
    int contentHeight() {
        return Math.round(interrupteurs.size() * rangeePx);
    }

    /** L'ordonnée du CENTRE de la rangée donnée — les bancs y visent
     *  leur appui. */
    float rowCenterY(int index) {
        return index * rangeePx + rangeePx / 2f;
    }

    /** Le commutateur d'index donné, null s'il n'existe pas — les
     *  bancs y lisent la carte sans passer par le rendu. */
    public @Nullable Interrupteur interrupteurAt(int index) {
        return index >= 0 && index < interrupteurs.size()
                ? interrupteurs.get(index) : null;
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        if (event.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) {
            pressee = (int) ((event.getY() - paddingYPx) / rangeePx);
            if (pressee >= interrupteurs.size()) {
                pressee = -1;
            }
            invalidate();
            return true;
        }
        if (event.getActionMasked() != android.view.MotionEvent.ACTION_UP) {
            return false;
        }
        int index = (int) ((event.getY() - paddingYPx) / rangeePx);
        pressee = -1;
        invalidate();
        if (index >= 0 && index < interrupteurs.size()) {
            // la bascule prend le MÊME chemin que le doigt : le runnable
            // de l'hôte seul décide ce que basculer veut dire
            interrupteurs.get(index).bascule().run();
            performClick();
            return true;
        }
        return false;
    }

    /** Le nombre de commutateurs posés — les bancs y lisent la carte. */
    public int nombre() {
        return interrupteurs.size();
    }

    /** Les libellés posés, dans l'ordre — les bancs y comparent. */
    public List<String> libelles() {
        List<String> libelles = new ArrayList<>(interrupteurs.size());
        for (Interrupteur inter : interrupteurs) {
            libelles.add(inter.libelle());
        }
        return libelles;
    }
}
