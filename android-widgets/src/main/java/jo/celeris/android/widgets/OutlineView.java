package jo.celeris.android.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;
import java.util.function.Consumer;

import jo.celeris.core.syntax.structure.OutlineEntry;

import org.jspecify.annotations.Nullable;

/**
 * Le plan du fichier en vue (B-16) : une rangée par déclaration —
 * glyphe de catégorie, nom, retrait par niveau d'imbrication. L'hôte
 * nourrit la vue par {@link #setEntries} depuis le pont session ↔ vue
 * (le plan sort du même arbre que la coloration) et écoute les appuis
 * par {@link #setSurClic} pour naviguer vers la déclaration visée.
 *
 * <p>Vue fine : le modèle porte les entrées et le test au point, le
 * peintre porte le tracé — la vue ne fait qu'adapter le canvas à la
 * surface et le modèle aux mesures. La hauteur suit le nombre
 * d'entrées : l'hôte l'enveloppe d'une vue défilante quand le plan
 * dépasse l'écran.</p>
 */
public final class OutlineView extends View {

    private static final float FACTEUR_HAUTEUR = 2.2f;
    private static final float FACTEUR_GLYPHE = 0.6f;
    private static final int ENCRE_TEXTE = 0xFF202124;
    private static final int ENCRE_GLYPHE = 0xFF5F6368;

    private final OutlineModel modele = new OutlineModel();
    private final Paint pinceau = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final OutlinePainter peintre;
    private final float hauteurLigne;
    private @Nullable Consumer<OutlineEntry> surClic;

    /**
     * Construit la vue du plan, code ou XML.
     *
     * @param context le contexte hôte
     * @param attrs   les attributs XML, ignorés
     */
    public OutlineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float tailleTexte = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 14f,
                getResources().getDisplayMetrics());
        hauteurLigne = tailleTexte * FACTEUR_HAUTEUR;
        pinceau.setTextSize(tailleTexte);
        peintre = new OutlinePainter(hauteurLigne, tailleTexte,
                tailleTexte * FACTEUR_GLYPHE, ENCRE_TEXTE, ENCRE_GLYPHE);
    }

    /**
     * Nourrit la vue d'un nouveau plan : le modèle conserve le préfixe
     * intact, la mesure et le tracé suivent.
     *
     * @param entrees les entrées du plan, dans l'ordre du source
     */
    public void setEntries(List<OutlineEntry> entrees) {
        modele.appliquer(entrees);
        requestLayout();
        invalidate();
    }

    /**
     * Le consommateur des appuis : l'entrée visée part au clic.
     *
     * @param consommateur le consommateur, null pour le retirer
     */
    public void setSurClic(@Nullable Consumer<OutlineEntry> consommateur) {
        surClic = consommateur;
    }

    /** Le nombre d'entrées affichées. */
    public int nombre() {
        return modele.nombre();
    }

    @Override
    protected void onMeasure(int largeurMesuree, int hauteurMesuree) {
        int hauteur = Math.round(peintre.hauteur(modele.nombre()));
        setMeasuredDimension(
                resoudre(largeurMesuree, getSuggestedMinimumWidth()),
                resoudre(hauteurMesuree, hauteur));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        peintre.dessiner(modele, new SurfaceCanvas(canvas));
    }

    @Override
    public boolean onTouchEvent(MotionEvent evenement) {
        if (evenement.getAction() != MotionEvent.ACTION_UP || surClic == null) {
            return true;
        }
        OutlineEntry visee = modele.auPoint(evenement.getY(), hauteurLigne);
        if (visee != null) {
            surClic.accept(visee);
        }
        return true;
    }

    private static int resoudre(int mesure, int contenu) {
        switch (MeasureSpec.getMode(mesure)) {
            case MeasureSpec.EXACTLY:
                return MeasureSpec.getSize(mesure);
            case MeasureSpec.AT_MOST:
                return Math.min(MeasureSpec.getSize(mesure), contenu);
            default:
                return contenu;
        }
    }

    /** L'adaptateur canvas ↔ surface : un pinceau partagé, la couleur posée à l'appel. */
    private final class SurfaceCanvas implements OutlineSurface {

        private final Canvas canvas;

        /** L'adaptateur du canvas d'une image. */
        private SurfaceCanvas(Canvas canvas) {
            this.canvas = canvas;
        }

        @Override
        public void drawText(String text, float x, float y, int couleur) {
            pinceau.setColor(couleur);
            canvas.drawText(text, x, y, pinceau);
        }

        @Override
        public void drawLine(float x1, float y1, float x2, float y2, int couleur) {
            pinceau.setColor(couleur);
            pinceau.setStrokeWidth(2f);
            canvas.drawLine(x1, y1, x2, y2, pinceau);
        }

        @Override
        public void drawRect(float left, float top, float right, float bottom,
                int couleur) {
            pinceau.setColor(couleur);
            canvas.drawRect(left, top, right, bottom, pinceau);
        }

        @Override
        public void drawCircle(float cx, float cy, float rayon, int couleur) {
            pinceau.setColor(couleur);
            canvas.drawCircle(cx, cy, rayon, pinceau);
        }
    }
}
