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
        int ligne = projection.firstLineOfRow(rangee);
        int debut = projection.spanStartAtRow(rangee);
        int fin = Math.min(projection.spanEndAtRow(rangee),
                document.lineEnd(ligne) - document.lineStart(ligne));
        if (fin < debut) {
            fin = debut;
        }
        String texte = document.subText(document.lineStart(ligne) + debut,
                document.lineStart(ligne) + fin);
        List<VisualRuns.Run> segments = VisualRuns.runs(texte);
        // Le chemin rapide du peintre : une rangée purement
        // gauche-à-droite porte des segments vides, jamais une liste
        // d'un seul segment pleine largeur (PIEGE-15).
        boolean purGaucheADroite = segments.size() == 1
                && !segments.get(0).rtl()
                && segments.get(0).start() == 0
                && segments.get(0).end() == texte.length();
        return new RowLayout(ligne, debut, fin, texte,
                texte.length() * metrics.charWidth(),
                purGaucheADroite ? List.of() : segments);
    }
}
