package jo.celeris.android.view.internal.model;

import jo.celeris.core.text.Document;

/**
 * Les bornes de l'identifiant SOUS une position (sélection par appui
 * long, M7.5) : lettres, chiffres et tiret bas forment le mot que
 * l'appui long sélectionne — le doigt doit vivre SUR un caractère du
 * mot ; le blanc entre deux mots, le point-virgule, la parenthèse ne
 * sélectionnent rien.
 *
 * <p>Pur et testable sans SDK : le document n'est jamais muté, les
 * bornes rendues vivent toujours dans [0, longueur du document].</p>
 */
public final class IdentifierBounds  {

    /** Les bornes rendues : le début inclus, la fin exclusive. */
    public record Around(int start, int end) {
    }

    private IdentifierBounds() {
    }

    /**
     * Les bornes de l'identifiant sous la position donnée.
     *
     * @param doc    le document consulté, jamais muté
     * @param offset la position du doigt dans le document
     * @return les bornes, vides (début = fin = position) hors de tout
     *         identifiant
     */
    public static Around around(Document doc, int offset) {
        int borne = Math.max(0, Math.min(offset, doc.length()));
        if (borne >= doc.length() || !estIdentifiant(doc.charAt(borne))) {
            return new Around(borne, borne);
        }
        int start = borne;
        int end = borne + 1;
        while (start > 0 && estIdentifiant(doc.charAt(start - 1))) {
            start--;
        }
        while (end < doc.length() && estIdentifiant(doc.charAt(end))) {
            end++;
        }
        return new Around(start, end);
    }

    private static boolean estIdentifiant(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
