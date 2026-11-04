package jo.celeris.core.text;

import java.util.ArrayList;
import java.util.List;

/**
 * Sélection multiple de première classe : liste normalisée de plages
 * ancre-curseur (ADR-003).
 *
 * <p>Le modèle de sélection du dépôt de départ était un intervalle unique
 * et le multi-curseur y était classé refonte de génération ; ici la
 * pluralité est l'état par défaut (parité A-01). Les invariants sont
 * garantis à la construction : plages triées par début croissant, sans
 * recouvrement (les plages qui se chevauchent fusionnent en leur union,
 * la direction de la fusion venant de la plage qui portait la fin commune),
 * curseurs identiques dédupliqués.</p>
 *
 * <p>La sélection se projette à travers chaque édition appliquée par la
 * voie de transaction unique ({@link #project}) avec la gravité droite
 * d'ADR-003 : une plage posée au point d'insertion suit le texte inséré,
 * une plage recouverte par une suppression se rabat en curseur, les plages
 * suivantes se décalent. Fil d'exécution : immuable, lisible depuis tout
 * fil.</p>
 */
public final class MultiSelection {

    private static final MultiSelection EMPTY = new MultiSelection(List.of());

    private final List<SelRange> ranges;

    private MultiSelection(List<SelRange> ranges) {
        this.ranges = ranges;
    }

    /**
     * Construit une sélection normalisée à partir de plages brutes.
     *
     * @param rawRanges plages quelconques, éventuellement chevauchantes ou
     *        désordonnées
     * @return la sélection normalisée (triée, fusionnée, dédupliquée)
     */
    public static MultiSelection of(List<SelRange> rawRanges) {
        if (rawRanges.isEmpty()) {
            return EMPTY;
        }
        List<Span> spans = new ArrayList<>(rawRanges.size());
        for (SelRange range : rawRanges) {
            spans.add(new Span(range.start(), range.end(), range.isBackward()));
        }
        spans.sort((a, b) -> Integer.compare(a.start(), b.start()));
        List<SelRange> merged = new ArrayList<>(spans.size());
        Span current = spans.get(0);
        for (int i = 1; i < spans.size(); i++) {
            Span next = spans.get(i);
            if (next.start() <= current.end) {
                boolean backward = next.end >= current.end ? next.backward : current.backward;
                current = new Span(current.start, Math.max(current.end, next.end), backward);
            } else {
                merged.add(current.toRange());
                current = next;
            }
        }
        merged.add(current.toRange());
        return new MultiSelection(List.copyOf(merged));
    }

    /**
     * Construit une sélection normalisée à partir d'une liste variable de
     * plages brutes.
     *
     * @param rawRanges plages quelconques
     * @return la sélection normalisée
     */
    public static MultiSelection of(SelRange... rawRanges) {
        return of(List.of(rawRanges));
    }

    /**
     * Construit une sélection de curseurs simples.
     *
     * @param positions les positions des curseurs
     * @return la sélection normalisée réduite à des curseurs
     */
    public static MultiSelection caretsAt(int... positions) {
        List<SelRange> raw = new ArrayList<>(positions.length);
        for (int position : positions) {
            raw.add(SelRange.caretAt(position));
        }
        return of(raw);
    }

    /** La sélection vide, sans aucun curseur (instance unique). */
    public static MultiSelection empty() {
        return EMPTY;
    }

    /** Les plages normalisées : liste immuable, triée, sans recouvrement. */
    public List<SelRange> ranges() {
        return ranges;
    }

    /** Le nombre de plages, c'est-à-dire de curseurs. */
    public int caretCount() {
        return ranges.size();
    }

    /** Vrai si la sélection ne contient aucune plage. */
    public boolean isEmpty() {
        return ranges.isEmpty();
    }

    /** Les curseurs (extrémités actives), dans l'ordre des plages. */
    public List<Integer> carets() {
        List<Integer> result = new ArrayList<>(ranges.size());
        for (SelRange range : ranges) {
            result.add(range.caret());
        }
        return result;
    }

    /**
     * Projette la sélection à travers l'édition appliquée : chaque
     * extrémité suit la gravité droite, le résultat est re-normalisé
     * (deux plages rabattues sur le même point fusionnent).
     *
     * @param span la plage de l'édition qui vient d'être appliquée
     * @return la sélection projetée dans les coordonnées d'après
     */
    public MultiSelection project(EditSpan span) {
        if (ranges.isEmpty()) {
            return EMPTY;
        }
        List<SelRange> projected = new ArrayList<>(ranges.size());
        for (SelRange range : ranges) {
            projected.add(new SelRange(mapEndpoint(range.anchor(), span),
                    mapEndpoint(range.caret(), span)));
        }
        return of(projected);
    }

    /**
     * Mappe une extrémité avec la gravité droite : avant la plage elle
     * reste, dans la plage remplacée elle se rabat sur la fin du
     * remplacement, au point d'insertion elle passe après le texte inséré,
     * au-delà elle suit le delta.
     *
     * @param offset l'extrémité dans les coordonnées d'avant
     * @param span   la plage de l'édition appliquée
     * @return l'extrémité dans les coordonnées d'après
     */
    private static int mapEndpoint(int offset, EditSpan span) {
        if (offset < span.start()) {
            return offset;
        }
        if (offset < span.end()) {
            return span.start() + span.added();
        }
        return offset + span.delta();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MultiSelection other)) {
            return false;
        }
        return ranges.equals(other.ranges);
    }

    @Override
    public int hashCode() {
        return ranges.hashCode();
    }

    @Override
    public String toString() {
        return "MultiSelection[" + ranges.size() + " : " + ranges + "]";
    }

    /** Étendue interne d'une plage, sans direction apparente. */
    private record Span(int start, int end, boolean backward) {

        private SelRange toRange() {
            return backward
                    ? new SelRange(end, start)
                    : new SelRange(start, end);
        }
    }
}
