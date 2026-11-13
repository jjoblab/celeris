package jo.celeris.android.view;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Paint;

/**
 * La loupe du dragage (D-16 — le redressement du propriétaire
 * 2026-11-13 : « ajoute la fonctionnalité de magnifier, quand on fait
 * longclick et on commence à draguer ») : le texte sous le doigt
 * s'agrandit dans une BULLE au-dessus du geste — le réglage se voit.
 *
 * <p>Le design est celui du dépôt de départ : bulle circulaire de
 * 60 dp de rayon, zoom ×2 par défaut (réglable), au-dessus du doigt
 * quand la place y suffit — SINON sous le doigt, SINON bornée dans ce
 * qui reste de la vue : la bulle ne sort jamais de la fenêtre (les
 * bornes vivent dans {@link MagnifierGeometry}, LA source unique). La
 * position y est SNAPPÉE au centre de la rangée réglée (le contenu de
 * la bulle ne tremble JAMAIS verticalement, quel que soit le jitter du
 * capteur dans la rangée — changer de rangée fait un PAS net d'une
 * ligne, pas une oscillation) ; la position x est AMORTIE
 * exponentiellement (α = 0,55 par image : le tremblement haute
 * fréquence du doigt se filtre, le suivi reste réactif). C'est la
 * sémantique des loupes de plateforme.</p>
 *
 * <p>Pure pour la géométrie (les bornes se testent sans appareil) ;
 * l'image s'y photographie : l'assemblage y rend la frame dans un
 * bitmap retenu, la bulle y sert la zone du doigt agrandie — la loupe
 * montre la VRAIE frame (voile de sélection, curseur, poignées), pas
 * une redite simplifiée.</p>
 */
public final class Loupe {

    /** Le rayon de la bulle, en dp (le dépôt de départ : 60 dp). */
    public static final float RAYON_DP = 60f;

    /** Le zoom par défaut (le dépôt de départ : ×2). */
    public static final float ZOOM_DEFAUT = 2f;

    /** L'amortissement de x par image (α = 0,55, le dépôt de départ). */
    private static final float AMORTISSEMENT_X = 0.55f;

    private boolean active;
    private float x;
    private float y;
    private float zoom = ZOOM_DEFAUT;
    /** La photographie de la frame — retenu, realloc à la taille. */
    private Bitmap image;
    /** La toile de la photographie — retenue, recyclée par image. */
    private Canvas toile;
    /** Le clip circulaire de la bulle — chemin de travail réutilisé. */
    private final Path bulle = new Path();
    /** L'anneau de la bulle, 2 dp à l'encre de gouttière voilée à 50 %. */
    private final Paint anneau = new Paint(Paint.ANTI_ALIAS_FLAG);
    /** L'ombre intérieure de la bulle, 1 dp voilée à 20 %. */
    private final Paint ombre = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** Vrai quand la loupe vit — le dragage l'arme, le geste levé la referme. */
    public boolean active() {
        return active;
    }

    /** L'abscisse stabilisée du doigt (amortie). */
    public float x() {
        return x;
    }

    /** L'ordonnée stabilisée du doigt (snappée à la rangée réglée). */
    public float y() {
        return y;
    }

    /** Le zoom courant de la bulle. */
    public float zoom() {
        return zoom;
    }

    /**
     * Pose le zoom de la bulle — la lecture des grands caractères se
     * règle de ×1,5 à ×3 (le dépôt de départ).
     *
     * @param facteur le facteur d'agrandissement, au moins 1
     */
    public void setZoom(float facteur) {
        zoom = Math.max(1f, facteur);
    }

    /**
     * Suit le doigt du dragage : l'ordonnée SNAPPÉE au centre de la
     * rangée réglée y passe (le contenu ne tremble pas), l'abscisse
     * s'amortit — α = 0,55 par image.
     *
     * @param doigtX      l'abscisse brute du doigt, en pixels
     * @param centreRangee l'ordonnée du centre de la rangée réglée, en
     *                    pixels
     */
    public void suivre(float doigtX, float centreRangee) {
        x = active ? x + (doigtX - x) * AMORTISSEMENT_X : doigtX;
        y = centreRangee;
        active = true;
    }

    /** Referme la loupe — le geste se lève, la bulle disparaît. */
    public void cesser() {
        active = false;
    }

    /**
     * La photographie de la dernière frame rendue dans la toile — la
     * bulle y sert sa zone ; null avant la première pose.
     *
     * @return la bitmap photographiée, ou null
     */
    public Bitmap photographie() {
        return image;
    }

    /**
     * La toile de la photographie : la frame s'y rend, la bulle y sert
     * sa zone. La bitmap se recrée à la taille qui change, la toile se
     * recycle — rien ne se ralloue au doigt levé.
     *
     * @param largeur  la largeur de la vue, en pixels
     * @param hauteur  la hauteur de la vue, en pixels
     * @return la toile de la frame, prête à recevoir l'image
     */
    public Canvas toileDe(int largeur, int hauteur) {
        if (largeur <= 0 || hauteur <= 0) {
            return null;
        }
        if (image == null || image.getWidth() != largeur
                || image.getHeight() != hauteur) {
            image = Bitmap.createBitmap(largeur, hauteur,
                    Bitmap.Config.ARGB_8888);
            toile = new Canvas(image);
        } else {
            image.eraseColor(0);
        }
        return toile;
    }

    /**
     * Trace la bulle sur le canvas de la vue : le clip circulaire
     * borne la zone, le fond de l'éditeur la couvre, la photographie
     * agrandie y passe — le point sous le doigt au centre de la
     * bulle —, l'anneau la ferme (2 dp à l'encre de gouttière voilée
     * à 50 %, l'ombre intérieure à 20 %).
     *
     * @param canvas     le canvas de la frame de la vue
     * @param photographie la photographie de la frame (la toile de
     *                    {@link #toileDe(int, int)})
     * @param theme      le thème courant — le fond et l'anneau y vivent
     * @param largeurVue la largeur de la vue, en pixels
     * @param hauteurVue la hauteur de la vue, en pixels
     * @param densite    la densité de l'appareil — les dp de l'anneau
     */
    public void tracer(Canvas canvas, Bitmap photographie, EditorTheme theme,
            float largeurVue, float hauteurVue, float densite) {
        if (!active || photographie == null || largeurVue <= 0f
                || hauteurVue <= 0f) {
            return;
        }
        float rayon = RAYON_DP * densite;
        float cx = borneCx(x, largeurVue, rayon);
        float cy = resoudreCy(y, hauteurVue, rayon);
        int sauve = canvas.save();
        bulle.reset();
        bulle.addCircle(cx, cy, rayon, Path.Direction.CW);
        canvas.clipPath(bulle);
        // le fond de la bulle : le fond de l'éditeur, jamais le
        // transparent de la photographie aux bords de la vue
        canvas.drawColor(theme.backgroundColor());
        canvas.save();
        // l'agrandissement : le point sous le doigt gagne le centre de
        // la bulle, le zoom s'y pose autour
        canvas.scale(zoom, zoom, cx, cy);
        canvas.translate(-(x - cx), -(y - cy));
        canvas.drawBitmap(photographie, 0f, 0f, null);
        canvas.restore();
        canvas.restoreToCount(sauve);
        // l'anneau de la bulle (le dépôt de départ : 2 dp, encre de
        // gouttière voilée à 50 %) et son ombre intérieure discrète
        anneau.setStyle(Paint.Style.STROKE);
        anneau.setStrokeWidth(2f * densite);
        anneau.setColor(voile(theme.gutterTextColor(), 0.5f));
        canvas.drawCircle(cx, cy, rayon, anneau);
        ombre.setStyle(Paint.Style.STROKE);
        ombre.setStrokeWidth(1f * densite);
        ombre.setColor(voile(theme.gutterTextColor(), 0.2f));
        canvas.drawCircle(cx, cy, rayon - 2f * densite, ombre);
    }

    /**
     * Le centre x de la bulle, borné dans la largeur de la vue : la
     * bulle ne sort JAMAIS des bords horizontaux — la géométrie vit
     * dans {@link MagnifierGeometry}, LA source unique.
     */
    public static float borneCx(float doigtX, float largeurVue, float rayon) {
        return MagnifierGeometry.centerX(doigtX, largeurVue, 2f * rayon);
    }

    /**
     * Le centre y de la bulle : AU-DESSUS du doigt si la place suffit,
     * SINON sous le doigt, SINON borné dans ce qui reste de la vue —
     * la géométrie vit dans {@link MagnifierGeometry}, LA source
     * unique.
     */
    public static float resoudreCy(float doigtY, float hauteurVue, float rayon) {
        return MagnifierGeometry.resoudreCy(doigtY, hauteurVue, rayon);
    }

    /** La couleur à l'alpha donné — l'anneau voilé de l'encre de gouttière. */
    private static int voile(int couleur, float alpha) {
        return (couleur & 0x00FFFFFF) | (Math.round((couleur >>> 24) * alpha)
                << 24);
    }
}
