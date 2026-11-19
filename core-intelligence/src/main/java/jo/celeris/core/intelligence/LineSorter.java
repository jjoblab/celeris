package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextEdit;
import jo.celeris.core.text.Transaction;

/**
 * Le tri des lignes du bloc touché (M5, A-12) : ordre naturel — les
 * tranches numériques se comparent en nombres (« item2 » avant
 * « item10 »), le reste en ordre lexicographique — la sensibilité à la
 * casse appartient à l'hôte. Le bloc trié remplace le bloc d'origine et
 * reste sélectionné.
 */
public final class LineSorter {

    private LineSorter() {
    }

    /**
     * Trie les lignes du bloc touché, de la première à la dernière.
     *
     * @param doc           le document, jamais muté
     * @param selection     la sélection courante
     * @param caseSensitive vrai pour distinguer la casse
     * @return le plan du geste
     */
    public static EditPlan sort(Document doc, MultiSelection selection, boolean caseSensitive) {
        int premiere = doc.lineCount();
        int derniere = -1;
        for (SelRange range : selection.ranges()) {
            premiere = Math.min(premiere, doc.lineForOffset(range.start()));
            derniere = Math.max(derniere,
                    doc.lineForOffset(Math.max(range.start(), range.end() - 1)));
        }
        if (derniere < 0) {
            premiere = 0;
            derniere = doc.lineCount() - 1;
        }
        List<String> lines = new ArrayList<>();
        for (int l = premiere; l <= derniere; l++) {
            lines.add(doc.lineText(l));
        }
        lines.sort(naturel(caseSensitive));
        StringBuilder trie = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                trie.append('\n');
            }
            trie.append(lines.get(i));
        }
        int start = doc.lineStart(premiere);
        int end = doc.lineEnd(derniere);
        TextEdit edit = new TextEdit(start, doc.subText(start, end), trie.toString());
        SelRange blocTrie = new SelRange(start, start + trie.length());
        return new EditPlan(Transaction.of(edit), MultiSelection.of(blocTrie));
    }

    /**
     * Le comparateur naturel : tranches numériques en nombres, le reste
     * en chaînes, la casse au choix.
     */
    private static Comparator<String> naturel(boolean caseSensitive) {
        return (a, b) -> {
            int i = 0;
            int j = 0;
            while (i < a.length() && j < b.length()) {
                char ca = a.charAt(i);
                char cb = b.charAt(j);
                boolean chiffreA = Character.isDigit(ca);
                boolean chiffreB = Character.isDigit(cb);
                if (chiffreA && chiffreB) {
                    long na = trancheNumerique(a, i)[0];
                    long nb = trancheNumerique(b, j)[0];
                    if (na != nb) {
                        return Long.compare(na, nb);
                    }
                    i = (int) trancheNumerique(a, i)[1];
                    j = (int) trancheNumerique(b, j)[1];
                    continue;
                }
                String sa = trancheTexte(a, i, caseSensitive);
                String sb = trancheTexte(b, j, caseSensitive);
                int comparaison = sa.compareTo(sb);
                if (comparaison != 0) {
                    return comparaison;
                }
                i += sa.length();
                j += sb.length();
            }
            return Integer.compare(a.length() - i, b.length() - j);
        };
    }

    /** La valeur de la tranche numérique en {@code start} et sa fin exclusive. */
    private static long[] trancheNumerique(String s, int start) {
        int end = start;
        while (end < s.length() && Character.isDigit(s.charAt(end))) {
            end++;
        }
        return new long[] {Long.parseLong(s.substring(start, end)), end};
    }

    /** La tranche de texte (non numérique) en {@code start}, un caractère au moins. */
    private static String trancheTexte(String s, int start, boolean caseSensitive) {
        int end = start + 1;
        while (end < s.length() && !Character.isDigit(s.charAt(end))) {
            end++;
        }
        String text = s.substring(start, end);
        return caseSensitive ? text : text.toLowerCase(java.util.Locale.ROOT);
    }
}
