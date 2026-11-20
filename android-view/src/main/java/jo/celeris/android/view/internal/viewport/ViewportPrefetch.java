package jo.celeris.android.view.internal.viewport;

import org.jspecify.annotations.Nullable;
import jo.celeris.android.view.FontSpec;
import jo.celeris.android.view.RowKey;
import jo.celeris.android.view.internal.access.EditorAccess;

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
public final class ViewportPrefetch  {

    /** Le calme avant le préchauffage, en millisecondes. */
    static final long CALME_MS = 150;

    /** La portée du préchauffage de chaque côté, en rangées. */
    static final int PORTEE = 8;

    private final EditorAccess acces;
    private @Nullable Runnable tache;

    /**
     * Construit le préfetch sur la vue.
     *
     * @param vue la vue propriétaire du cache et de la source
     */
    public ViewportPrefetch(EditorAccess acces) {
        this.acces = acces;
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
    public void programmer(int premiere, int visibles) {
        stop();
        int rowStart = Math.max(0, premiere - PORTEE);
        int rowEnd = premiere + visibles + PORTEE;
        tache = () -> prechauffer(rowStart, rowEnd);
        acces.postDelayed(tache, CALME_MS);
    }

    /** Arrête toute programmation — la vue se détache ou se libère. */
    public void stop() {
        if (tache != null) {
            acces.removeCallbacks(tache);
            tache = null;
        }
    }

    /** Le préchauffage : les rangées de la marge se mesurent au
     *  cache, hors de la boucle de dessin. */
    private void prechauffer(int rowStart, int rowEnd) {
        tache = null;
        long revision = acces.session().document().revision();
        int theme = acces.theme().id();
        FontSpec font = acces.metrics().font();
        int colonnes = acces.projection().columns();
        for (int rangee = rowStart; rangee < rowEnd; rangee++) {
            if (rangee < 0 || rangee >= acces.projection().rowCount()) {
                continue;
            }
            if (acces.cache().find(revision, rangee, theme, font, colonnes) != null) {
                continue;
            }
            RowKey key = new RowKey(revision, rangee, theme, font, colonnes);
            acces.cache().store(key, acces.sourceLiee().layoutOf(key));
        }
    }
}
