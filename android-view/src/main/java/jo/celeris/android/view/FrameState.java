package jo.celeris.android.view;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;

/**
 * État d'image : le sac d'état muté sur place à chaque image (PIEGE-15).
 *
 * <p>La vue possède UNE instance, configurée par {@link #beginFrame} au
 * début de chaque image, lue par les couches pendant le tracé — jamais
 * recréée, jamais allouée dans la boucle de dessin. Toute lecture avant
 * la première image est une erreur de programmation.</p>
 */
public final class FrameState {

    private Document document;
    private MultiSelection selection;
    private VisualProjection projection;
    private EditorMetrics metrics;
    private EditorTheme theme;
    private long revision;
    private int firstRow;
    private int visibleRows;
    private float gutterWidth;
    private float scrollX;
    private float scrollY;
    /** Une publication d'analyse existe (M7.5, §9) : une rangée sans
     *  couleur se comptera comme décolorée — avant la première
     *  publication, rien n'avait de couleur à perdre. */
    private boolean couleurAttendue;
    /** L'image en cours a tracé une rangée sans couleur qu'elle
     *  possédait (M7.5, §9 — la surimpression de diagnostic le lit). */
    private boolean imageDecoloree;

    /**
     * Configure l'état de l'image à venir.
     *
     * @param document    l'instantané de document affiché
     * @param selection   la sélection multiple courante
     * @param projection  la projection rangées ↔ lignes courante
     * @param revision    la révision de l'instantané (clé de cache)
     * @param metrics     les métriques d'éditeur courantes
     * @param theme       le thème résolu courant
     * @param firstRow    la première rangée visible
     * @param visibleRows le nombre de rangées visibles
     * @param gutterWidth la largeur de gouttière courante, en pixels
     * @param scrollX     le défilement horizontal, en pixels
     * @param scrollY     le défilement vertical, en pixels
     */
    public void beginFrame(Document document, MultiSelection selection,
            VisualProjection projection, long revision, EditorMetrics metrics,
            EditorTheme theme, int firstRow, int visibleRows, float gutterWidth,
            float scrollX, float scrollY) {
        this.document = document;
        this.selection = selection;
        this.projection = projection;
        this.revision = revision;
        this.metrics = metrics;
        this.theme = theme;
        // La fenêtre se borne au contenu : les rangées au-delà de la
        // dernière ne se tracent pas — elles répéteraient la dernière
        // ligne sans coupure, et déborderaient sous coupure.
        this.firstRow = Math.max(0, Math.min(firstRow, projection.rowCount() - 1));
        this.visibleRows = ScrollGeometry.borneesAuContenu(this.firstRow,
                visibleRows, projection.rowCount());
        this.gutterWidth = gutterWidth;
        this.scrollX = scrollX;
        this.scrollY = scrollY;
        this.couleurAttendue = false;
        this.imageDecoloree = false;
    }

    /**
     * Pose l'attente de couleur de l'image (M7.5, §9) : l'assembleur
     * l'annonce après {@link #beginFrame} — une publication d'analyse
     * existe, une rangée sans couleur se comptera comme décolorée.
     *
     * @param attendue vrai quand une publication existe
     */
    public void attendCouleur(boolean attendue) {
        this.couleurAttendue = attendue;
    }

    /**
     * Vrai quand une publication d'analyse existe : le peintre de
     * texte y marque les rangées décolorées (M7.5, §9).
     */
    public boolean couleurAttendue() {
        return couleurAttendue;
    }

    /**
     * Marque l'image : une rangée sans couleur qu'elle possédait y a
     * été tracée (M7.5, §9) — la vue la compte après le tracé.
     */
    public void marquerImageDecoloree() {
        imageDecoloree = true;
    }

    /**
     * Vrai quand l'image a tracé une rangée sans couleur qu'elle
     * possédait (M7.5, §9) — lue après le tracé, remise à l'image
     * suivante.
     */
    public boolean imageDecoloree() {
        return imageDecoloree;
    }

    /** L'instantané de document affiché. */
    public Document document() {
        Document local = document;
        if (local == null) {
            throw new IllegalStateException("beginFrame required before reading frame state");
        }
        return local;
    }

    /** La sélection multiple courante. */
    public MultiSelection selection() {
        MultiSelection local = selection;
        if (local == null) {
            throw new IllegalStateException("beginFrame required before reading frame state");
        }
        return local;
    }

    /** La projection rangées ↔ lignes courante. */
    public VisualProjection projection() {
        VisualProjection local = projection;
        if (local == null) {
            throw new IllegalStateException("beginFrame required before reading frame state");
        }
        return local;
    }

    /** La révision de l'instantané. */
    public long revision() {
        exigeImage();
        return revision;
    }

    /** Les métriques d'éditeur courantes. */
    public EditorMetrics metrics() {
        EditorMetrics local = metrics;
        if (local == null) {
            throw new IllegalStateException("beginFrame required before reading frame state");
        }
        return local;
    }

    /** Le thème résolu courant. */
    public EditorTheme theme() {
        EditorTheme local = theme;
        if (local == null) {
            throw new IllegalStateException("beginFrame required before reading frame state");
        }
        return local;
    }

    /** La première rangée visible. */
    public int firstRow() {
        exigeImage();
        return firstRow;
    }

    /** Le nombre de rangées visibles. */
    public int visibleRows() {
        exigeImage();
        return visibleRows;
    }

    /** La largeur de gouttière courante, en pixels. */
    public float gutterWidth() {
        exigeImage();
        return gutterWidth;
    }

    /** Le défilement horizontal, en pixels. */
    public float scrollX() {
        exigeImage();
        return scrollX;
    }

    /** Le défilement vertical, en pixels. */
    public float scrollY() {
        exigeImage();
        return scrollY;
    }

    private void exigeImage() {
        if (document == null) {
            throw new IllegalStateException("beginFrame required before reading frame state");
        }
    }
}
