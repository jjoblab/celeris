package jo.celeris.core.text;

import java.util.ArrayList;
import java.util.List;

/**
 * Suivi de plages ancrées : le portage du décalage des annotations du dépôt
 * de départ (registre de portage), en projecteur concret de positions.
 *
 * <p>Gravité droite pour les débuts (ils suivent l'édition), gravité gauche
 * pour les fins (elles restent en place à la frontière de l'édition) —
 * exactement la sémantique du pipeline commun du dépôt de départ, citée par
 * PIEGE-07. Les plages réduites à rien par une édition tombent du suivi.</p>
 */
public final class AnchoredRanges implements PositionProjector {

    private List<AnchoredRange> ranges = List.of();

    /** Crée un suivi vide. */
    public AnchoredRanges() {
    }

    /**
     * Remplace toutes les plages suivies (nouvelles données distantes, p.
     * ex. diagnostics reçus d'un serveur).
     *
     * @param nouvelles les nouvelles plages
     */
    public void set(List<AnchoredRange> nouvelles) {
        this.ranges = List.copyOf(nouvelles);
    }

    /** Les plages suivies, dans l'état projeté courant. */
    public List<AnchoredRange> ranges() {
        return ranges;
    }

    /**
     * Projette chaque plage à travers l'édition appliquée ; les plages
     * réduites à rien tombent.
     *
     * @param span la plage de l'édition appliquée
     */
    @Override
    public void project(EditSpan span) {
        List<AnchoredRange> projetees = new ArrayList<>(ranges.size());
        for (AnchoredRange plage : ranges) {
            int debut = mapStart(plage.start(), span);
            int fin = mapEnd(plage.end(), span);
            if (fin > debut) {
                projetees.add(new AnchoredRange(debut, fin));
            }
        }
        ranges = List.copyOf(projetees);
    }

    /**
     * Mappe un offset de début (gravité droite) : à la frontière de
     * l'édition il la suit.
     */
    static int mapStart(int offset, EditSpan span) {
        if (offset <= span.start()) {
            return offset;
        }
        if (offset <= span.end()) {
            return span.start() + span.added();
        }
        return offset + span.delta();
    }

    /**
     * Mappe un offset de fin (gravité gauche) : à l'intérieur de l'édition
     * il retombe au début de la plage remplacée.
     */
    static int mapEnd(int offset, EditSpan span) {
        if (offset < span.start()) {
            return offset;
        }
        if (offset < span.end()) {
            return span.start();
        }
        return offset + span.delta();
    }
}
