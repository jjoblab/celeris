package jo.celeris.core.syntax;

import jo.celeris.core.text.Document;

/**
 * Site d'une édition, exprimé en lignes : les coordonnées que le moteur
 * incrémental attend. Dérivé d'un couple (avant, après) et de la plage
 * remplacée — les lignes avant l'édition sont identiques dans les deux
 * documents, donc l'indice de la première ligne touchée est le même des
 * deux côtés.
 *
 * @param firstLine ligne (nouvelle) la première dont le contenu peut différer
 * @param lastLine  ligne (nouvelle) contenant la fin du texte inséré
 * @param lineDelta nombre de lignes nouvelles moins nombre de lignes anciennes
 */
public record EditSite(int firstLine, int lastLine, int lineDelta) {

    /**
     * Dérive le site d'une édition appliquée.
     *
     * @param avant     l'instantané avant l'édition
     * @param apres     l'instantané après l'édition
     * @param start     début de la plage remplacée (coordonnées avant)
     * @param end       fin exclusive de la plage remplacée (coordonnées avant)
     * @param insertion le texte inséré à la place de la plage
     * @return le site en lignes de cette édition
     */
    public static EditSite of(Document avant, Document apres, int start, int end, String insertion) {
        int premiere = apres.lineForOffset(start);
        int derniere = apres.lineForOffset(start + insertion.length());
        int delta = apres.lineCount() - avant.lineCount();
        return new EditSite(premiere, derniere, delta);
    }

    /** Vérifie la cohérence du site avec un lexique précédent, sinon refuse. */
    void verifier(int lignesPrecedentes, int lignesNouvelles) {
        if (firstLine < 0 || lastLine < firstLine) {
            throw new IllegalArgumentException("edit site lines inverted: " + this);
        }
        if (lignesPrecedentes + lineDelta != lignesNouvelles) {
            throw new IllegalArgumentException(
                    "line delta " + lineDelta + " inconsistent: "
                            + lignesPrecedentes + " -> " + lignesNouvelles);
        }
    }
}
