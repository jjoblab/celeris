package jo.celeris.core.text;

/**
 * Instantané immuable d'un document : une rope, un numéro de révision.
 *
 * <p>Chaque état du document est un instantané complet, lisible sans verrou
 * depuis tout fil (ADR-003) ; l'éditeur d'un instantané produit un nouvel
 * instantané, l'original n'est jamais muté. Le numéro de révision s'incrémente
 * à chaque édition effective — les consommateurs distants (pont LSP, agents)
 * datent leurs résultats par instantané et jettent les résultats périmés
 * (ADR-007).</p>
 *
 * <p>Portage du document du dépôt de départ (registre de portage) ; l'index
 * de lignes n'y est pas encore porté — il étendra cet instantané à la tranche
 * suivante, la rope seule suffit aux transactions.</p>
 */
public final class Document {

    private final Rope rope;
    private final int revision;

    private Document(Rope rope, int revision) {
        this.rope = rope;
        this.revision = revision;
    }

    /**
     * Crée un instantané du texte donné.
     *
     * @param text le contenu initial, jamais nul (contract {@code @NullMarked})
     */
    public static Document of(String text) {
        return new Document(Rope.fromString(text), 0);
    }

    /** Nombre total de caractères (unités de code UTF-16). */
    public int length() {
        return rope.length();
    }

    /** Contenu complet du document. */
    public String text() {
        return rope.toString();
    }

    /** Numéro de révision : zéro à la création, incrémenté par édition. */
    public int revision() {
        return revision;
    }

    /**
     * Extrait la plage {@code [start, end)} sans matérialiser le document
     * entier (descente d'arbre + tranche seule).
     *
     * @param start offset de début (inclusif)
     * @param end   offset de fin (exclusif)
     * @return le texte de la plage
     * @throws IndexOutOfBoundsException si la plage sort du document
     */
    public String subText(int start, int end) {
        if (start < 0 || end > rope.length() || start > end) {
            throw new IndexOutOfBoundsException(
                "start=" + start + ", end=" + end + ", length=" + rope.length());
        }
        if (start == end) {
            return "";
        }
        return rope.sub(start, end).toString();
    }

    /**
     * Remplace la plage {@code [start, end)} par l'insertion donnée et
     * retourne le nouvel instantané (révision incrémentée). Une édition sans
     * effet retourne ce même instantané, révision inchangée.
     *
     * @param start     offset de début (inclusif)
     * @param end       offset de fin (exclusif)
     * @param insertion le texte de remplacement
     * @return le nouvel instantané
     * @throws IndexOutOfBoundsException si la plage sort du document
     */
    public Document replace(int start, int end, String insertion) {
        if (start < 0 || end > rope.length() || start > end) {
            throw new IndexOutOfBoundsException(
                "start=" + start + ", end=" + end + ", length=" + rope.length());
        }
        if (start == end && insertion.isEmpty()) {
            return this;
        }
        Rope nouvelle = rope.replace(start, end, insertion);
        return new Document(nouvelle, revision + 1);
    }
}
