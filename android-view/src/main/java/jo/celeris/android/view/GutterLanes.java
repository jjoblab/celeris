package jo.celeris.android.view;

/**
 * La configuration de la coulisse de marqueurs de la gouttière (M7.5,
 * §5) : quelles INTÉGRATIONS sont activées — jamais lesquelles ont
 * quelque chose à afficher.
 *
 * <p>La largeur de coulisse découle de cette configuration seule : une
 * marque qui arrive se dessine dans la coulisse déjà réservée, elle ne
 * l'élargit pas — le texte ne se décale jamais à l'arrivée d'un
 * résultat (PIEGE-25). La coulisse de syntaxe réserve DEUX caractères
 * (M7.5, redressement) : le chevron de pli y vit à l'aise — une
 * cellule cohérente pour chaque marque, la cible du doigt comprise.</p>
 *
 * @param syntax              vrai pour la coulisse de la syntaxe (plis,
 *                            chevrons, lignes en erreur) — réservée dès
 *                            l'attachement d'un langage
 * @param integration         vrai pour la coulisse des marques
 *                            d'intégration (état VCS, arrêts)
 * @param importedDiagnostics vrai pour la coulisse des diagnostics
 *                            importés du build
 * @param preview             vrai pour la coulisse des repères
 *                            {@code @Preview}
 */
public record GutterLanes(boolean syntax, boolean integration,
        boolean importedDiagnostics, boolean preview) {

    /** La configuration sans coulisse : rien n'est réservé. */
    public static final GutterLanes AUCUNE = new GutterLanes(false,
            false, false, false);

    /**
     * La largeur de coulisse réservée par cette configuration.
     *
     * @param charWidth la largeur d'un caractère, en pixels
     * @param padding   la respiration minimale, en pixels
     * @return la largeur de coulisse, en pixels
     */
    public float laneWidth(float charWidth, float padding) {
        float coulisse = 0f;
        if (syntax) {
            coulisse = Math.max(coulisse, Math.max(padding, 2f * charWidth));
        }
        if (integration || importedDiagnostics || preview) {
            coulisse = Math.max(coulisse, 2f * charWidth);
        }
        return coulisse;
    }
}
