package jo.celeris.core.text;

/**
 * Une correspondance de texte : offsets de début (inclusif) et de fin
 * (exclusif) dans le texte parcouru — portage du dépôt de départ (registre
 * de portage), en enregistrement immuable.
 *
 * @param start offset de début de la correspondance (inclusif)
 * @param end   offset de fin de la correspondance (exclusif)
 */
public record Match(int start, int end) {

    /** Longueur de la correspondance, en caractères. */
    public int length() {
        return end - start;
    }
}
