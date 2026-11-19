package jo.celeris.android.view;

import jo.celeris.core.text.Document;

import java.util.List;

/**
 * Source de rangées sur le document courant : sans pli ni retour à la
 * ligne, la rangée porte la ligne entière que la projection lui donne ;
 * sous retour à la ligne par offsets (variante code, A-18), elle porte
 * la tranche d'offsets que la projection lui donne — jamais la ligne
 * entière par identité (PIEGE-04).
 *
 * <p>Pure et testable sans SDK : le document, la projection et les
 * métriques sont injectés ; la vue la reconstruit quand le document ou
 * les métriques changent. Le budget de colonnes doit être celui que la
 * projection signe : la clé de cache y entre (PIEGE-02).</p>
 */
public final class DocumentRowSource implements RowSource {

    /** Le plafond de fenêtre d'une ligne monstre (B-04, l'analyse
     *  croisée) : une ligne minifiée de centaines de kilo-octets ne
     *  peut pas façonner sa rangée entière — subText géant, runs
     *  bidirectionnels sur tout le texte, allocation monstrueuse à
     *  chaque miss. Le dépôt de départ coupe pareil à 5000 ; le
     *  contenu au-delà reste édité mais invisible (le scroll suit la
     *  fenêtre). */
    public static final int MONSTER_WINDOW = 5000;

    private final Document document;
    private final VisualProjection projection;
    private final EditorMetrics metrics;
    private final int columns;

    /**
     * Construit la source sur le triplet document / projection / métriques,
     * sans retour à la ligne.
     *
     * @param document   l'instantané de document affiché
     * @param projection la projection rangées ↔ lignes
     * @param metrics    les métriques d'éditeur courantes
     */
    public DocumentRowSource(Document document, VisualProjection projection,
            EditorMetrics metrics) {
        this(document, projection, metrics, 0);
    }

    /**
     * Construit la source avec budget de colonnes du retour à la ligne.
     *
     * @param document   l'instantané de document affiché
     * @param projection la projection rangées ↔ lignes, signée du même
     *                   budget
     * @param metrics    les métriques d'éditeur courantes
     * @param columns    le budget de colonnes par rangée (0 sans coupure)
     */
    public DocumentRowSource(Document document, VisualProjection projection,
            EditorMetrics metrics, int columns) {
        this.document = document;
        this.projection = projection;
        this.metrics = metrics;
        this.columns = Math.max(0, columns);
    }

    @Override
    public RowLayout layoutOf(RowKey key) {
        int rangee = key.row();
        int line = projection.firstLineOfRow(rangee);
        int start = projection.spanStartAtRow(rangee);
        int end = Math.min(projection.spanEndAtRow(rangee),
                document.lineEnd(line) - document.lineStart(line));
        // La fenêtre monstre (B-04) : la tranche mesurée ne dépasse
        // jamais le plafond — la coloration rogne ses jetons à la
        // tranche, le peintre suit, l'édition reste entière.
        end = Math.min(end, start + MONSTER_WINDOW);
        if (end < start) {
            end = start;
        }
        String text = document.subText(document.lineStart(line) + start,
                document.lineStart(line) + end);
        List<VisualRuns.Run> segments = VisualRuns.runs(text);
        // Le chemin rapide du peintre : une rangée purement
        // gauche-à-droite porte des segments vides, jamais une liste
        // d'un seul segment pleine largeur (PIEGE-15).
        boolean purGaucheADroite = segments.size() == 1
                && !segments.get(0).rtl()
                && segments.get(0).start() == 0
                && segments.get(0).end() == text.length();
        return new RowLayout(line, start, end, text,
                text.length() * metrics.charWidth(),
                purGaucheADroite ? List.of() : segments);
    }
}
