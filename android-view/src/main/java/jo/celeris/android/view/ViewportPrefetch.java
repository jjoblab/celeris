package jo.celeris.android.view;

import org.jspecify.annotations.Nullable;

/**
 * Le préfetch de viewport (B-03, l'analyse croisée) : après un calme
 * de 150 ms sans nouvelle frame, les rangées au-dessus et en-dessous
 * du viewport se préchauffent au cache — le fling ne paie plus la
 * mesure dans la frame. Le dépôt de départ réchauffe pareil son
 * LineRenderCache (morceaux mesurés hors de la boucle de dessin,
 * pauses entre morceaux) ; celeris ne mesurait que dans la boucle de
 * dessin.
 *
 * <p>Porteur mince sur le modèle du clignotement (KeyboardCursor) :
 * la vue y reporte la tâche, le défilement la reprogramme au nouveau
 * viewport — jamais un préchauffage périmé après un nouveau scroll.
 * La mesure emprunte la voie du peintre : la source de rangées, le
 * même cléage (PIEGE-02), le même cache.</p>
 */
final class ViewportPrefetch {

    /** Le calme avant le préchauffage, en millisecondes. */
    static final long CALME_MS = 150;

    /** La portée du préchauffage de chaque côté, en rangées. */
    static final int PORTEE = 8;

    private final EditorView vue;
    private @Nullable Runnable tache;

    /**
     * Construit le préfetch sur la vue.
     *
     * @param vue la vue propriétaire du cache et de la source
     */
    ViewportPrefetch(EditorView vue) {
        this.vue = vue;
    }

    /**
     * Programme le préchauffage au viewport courant — chaque frame le
     * reprogramme : seul le DERNIER viewport se préchauffe, le calme
     * écoulé. Les bornes vivent DANS la tâche : jamais une tâche
     * tardive ne préchauffe les bornes d'une autre.
     *
     * @param premiere la première rangée visible
     * @param visibles le nombre de rangées visibles
     */
    void programmer(int premiere, int visibles) {
        arreter();
        int debutRangee = Math.max(0, premiere - PORTEE);
        int finRangee = premiere + visibles + PORTEE;
        tache = () -> prechauffer(debutRangee, finRangee);
        vue.postDelayed(tache, CALME_MS);
    }

    /** Arrête toute programmation — la vue se détache ou se libère. */
    void arreter() {
        if (tache != null) {
            vue.removeCallbacks(tache);
            tache = null;
        }
    }

    /** Le préchauffage : les rangées de la marge se mesurent au
     *  cache, hors de la boucle de dessin. */
    private void prechauffer(int debutRangee, int finRangee) {
        tache = null;
        long revision = vue.session.document().revision();
        int theme = vue.theme.id();
        FontSpec police = vue.metrics.font();
        int colonnes = vue.projection.columns();
        for (int rangee = debutRangee; rangee < finRangee; rangee++) {
            if (rangee < 0 || rangee >= vue.projection.rowCount()) {
                continue;
            }
            if (vue.cache.find(revision, rangee, theme, police, colonnes) != null) {
                continue;
            }
            RowKey cle = new RowKey(revision, rangee, theme, police, colonnes);
            vue.cache.store(cle, vue.sourceLiee.layoutOf(cle));
        }
    }
}
