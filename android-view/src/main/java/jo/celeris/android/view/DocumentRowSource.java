package jo.celeris.android.view;

import jo.celeris.core.text.Document;

/**
 * Source de rangées sur le document courant : sans pli ni retour à la
 * ligne, la rangée porte la ligne entière que la projection lui donne.
 *
 * <p>Pure et testable sans SDK : le document, la projection et les
 * métriques sont injectés ; la vue la reconstruit quand le document ou
 * les métriques changent. Le retour à la ligne souple par offsets
 * (variante code, A-18) raffinera la tranche portée par rangée.</p>
 */
public final class DocumentRowSource implements RowSource {

    private final Document document;
    private final VisualProjection projection;
    private final EditorMetrics metrics;

    /**
     * Construit la source sur le triplet document / projection / métriques.
     *
     * @param document   l'instantané de document affiché
     * @param projection la projection rangées ↔ lignes
     * @param metrics    les métriques d'éditeur courantes
     */
    public DocumentRowSource(Document document, VisualProjection projection,
            EditorMetrics metrics) {
        this.document = document;
        this.projection = projection;
        this.metrics = metrics;
    }

    @Override
    public RowLayout layoutOf(RowKey key) {
        int ligne = projection.firstLineOfRow(key.row());
        String texte = document.lineText(ligne);
        return new RowLayout(ligne, 0, texte.length(), texte,
                texte.length() * metrics.charWidth());
    }
}
