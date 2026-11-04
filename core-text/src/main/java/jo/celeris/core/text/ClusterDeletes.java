package jo.celeris.core.text;

import java.util.ArrayList;
import java.util.List;

/**
 * Les suppressions par clusters de graphèmes, arrière et avant, tous
 * curseurs confondus (ADR-003 — jamais une demi-paire de substitut, ni
 * un emoji amputé de son sélecteur).
 *
 * <p>Une plage non vide est remplacée par le vide ; un curseur retire le
 * cluster qui le précède (arrière) ou le suit (avant) ; les plages de
 * suppression issues de curseurs voisins fusionnent si elles se
 * touchent. Les positions séquentielles s'ajustent du décalage cumulé —
 * le texte retiré se lit aux positions d'origine. Les frontières se
 * cherchent par fenêtre croissante : un cluster plus long que la
 * fenêtre ne se coupe jamais.</p>
 */
final class ClusterDeletes {

    private final Document document;

    ClusterDeletes(Document document) {
        this.document = document;
    }

    /**
     * Les éditions de la suppression arrière de la sélection donnée.
     *
     * @param selection la sélection multi-curseurs à supprimer
     * @return les éditions en ordre de document, vide si rien à retirer
     */
    List<TextEdit> backward(MultiSelection selection) {
        List<int[]> spans = new ArrayList<>(selection.caretCount());
        for (SelRange plage : selection.ranges()) {
            if (!plage.isEmpty()) {
                spans.add(new int[] {plage.start(), plage.end()});
            } else if (plage.caret() > 0) {
                spans.add(new int[] {boundaryBefore(plage.caret()), plage.caret()});
            }
        }
        return mergedDeletions(spans);
    }

    /**
     * Les éditions de la suppression avant de la sélection donnée.
     *
     * @param selection la sélection multi-curseurs à supprimer
     * @return les éditions en ordre de document, vide si rien à retirer
     */
    List<TextEdit> forward(MultiSelection selection) {
        List<int[]> spans = new ArrayList<>(selection.caretCount());
        for (SelRange plage : selection.ranges()) {
            if (!plage.isEmpty()) {
                spans.add(new int[] {plage.start(), plage.end()});
            } else if (plage.caret() < document.length()) {
                spans.add(new int[] {plage.caret(), boundaryAfter(plage.caret())});
            }
        }
        return mergedDeletions(spans);
    }

    /**
     * Fusionne les plages qui se touchent, puis pose les éditions ajustées
     * du décalage cumulé.
     */
    private List<TextEdit> mergedDeletions(List<int[]> spans) {
        spans.sort((a, b) -> Integer.compare(a[0], b[0]));
        List<TextEdit> editions = new ArrayList<>(spans.size());
        int decalage = 0;
        int[] courante = null;
        for (int[] span : spans) {
            if (courante != null && span[0] <= courante[1]) {
                courante[1] = Math.max(courante[1], span[1]);
            } else {
                if (courante != null) {
                    decalage = addDeletion(editions, courante, decalage);
                }
                courante = span;
            }
        }
        if (courante != null) {
            addDeletion(editions, courante, decalage);
        }
        return editions;
    }

    /**
     * Ajoute l'édition de suppression d'une plage fusionnée ; renvoie le
     * décalage cumulé après celle-ci.
     */
    private int addDeletion(List<TextEdit> editions, int[] span, int decalage) {
        String retire = document.subText(span[0], span[1]);
        editions.add(new TextEdit(span[0] + decalage, retire, ""));
        return decalage - retire.length();
    }

    /**
     * La frontière de graphème qui précède l'offset : fenêtre croissante
     * depuis l'offset, la segmentation se replaçant dans la fenêtre
     * jusqu'à trouver une frontière certaine.
     */
    private int boundaryBefore(int offset) {
        int window = 32;
        while (true) {
            int start = Math.max(0, offset - window);
            String chunk = document.subText(start, offset);
            int local = Grapheme.previousBoundary(chunk, chunk.length());
            if (local > 0 || start == 0) {
                return start + local;
            }
            window *= 4;
        }
    }

    /**
     * La frontière de graphème qui suit l'offset : fenêtre croissante
     * depuis l'offset, la segmentation se replaçant dans la fenêtre
     * jusqu'à trouver une frontière certaine — un cluster plus long que
     * la fenêtre ne se coupe jamais.
     */
    private int boundaryAfter(int offset) {
        int window = 32;
        while (true) {
            int end = Math.min(document.length(), offset + window);
            String chunk = document.subText(offset, end);
            int local = Grapheme.nextBoundary(chunk, 0);
            if (local < chunk.length() || end >= document.length()) {
                return offset + local;
            }
            window *= 4;
        }
    }
}
