package jo.celeris.core.lsp;

/**
 * Le curseur partagé des lecteurs JSON : la source, la position, les
 * sauts de blancs et les attentes — chaque lecteur (structures,
 * chaînes, nombres) avance le même curseur.
 */
final class CurseurSource {

    final String source;
    int position;

    CurseurSource(String source) {
        this.source = source;
    }

    /** Vrai quand la source est épuisée. */
    boolean aFini() {
        return position >= source.length();
    }

    /** Le caractère sous le curseur. */
    char courrant() {
        return source.charAt(position);
    }

    /** Vrai quand le mot donné commence sous le curseur. */
    boolean commencePar(String mot) {
        return source.startsWith(mot, position);
    }

    /** Avale les blancs usuels (espace, tabulation, sauts de ligne). */
    void passerBlancs() {
        while (!aFini()) {
            char courrant = courrant();
            if (courrant != ' ' && courrant != '\t'
                    && courrant != '\n' && courrant != '\r') {
                return;
            }
            position++;
        }
    }

    /**
     * Exige le caractère donné sous le curseur et l'avale.
     *
     * @param attendu le caractère exigé
     * @param nom     le nom lisible du caractère, pour le message
     */
    void exiger(char attendu, String nom) {
        if (aFini() || courrant() != attendu) {
            throw new IllegalArgumentException("expected " + nom + " at " + position);
        }
        position++;
    }
}
