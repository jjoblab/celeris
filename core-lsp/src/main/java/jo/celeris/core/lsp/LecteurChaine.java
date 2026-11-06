package jo.celeris.core.lsp;

/**
 * Le lecteur de chaînes JSON : le texte décodé, guillemets exclus —
 * échappements usuels et Unicode échappé compris, les contrôles bruts
 * refusés.
 */
final class LecteurChaine {

    private LecteurChaine() {
    }

    /**
     * Lit une chaîne — le guillemet ouvrant est déjà sous le curseur.
     *
     * @param curseur le curseur partagé
     * @return la chaîne lue
     * @throws IllegalArgumentException si la chaîne est ouverte ou mal échappée
     */
    static JsonChaine lire(CurseurSource curseur) {
        curseur.position++;
        return new JsonChaine(lireTexte(curseur));
    }

    /**
     * Lit le texte d'une chaîne — le guillemet ouvrant est déjà consommé
     * par l'appelant, la lecture s'arrête au guillemet fermant.
     *
     * @param curseur le curseur partagé, juste après l'ouverture
     * @return le texte décodé
     * @throws IllegalArgumentException si la chaîne est ouverte ou mal échappée
     */
    static String lireTexte(CurseurSource curseur) {
        StringBuilder texte = new StringBuilder();
        while (!curseur.aFini()) {
            char courrant = curseur.courrant();
            curseur.position++;
            if (courrant == '"') {
                return texte.toString();
            }
            if (courrant < 0x20) {
                throw new IllegalArgumentException("raw control char in string");
            }
            if (courrant != '\\') {
                texte.append(courrant);
                continue;
            }
            texte.append(echappement(curseur));
        }
        throw new IllegalArgumentException("unterminated string");
    }

    private static char echappement(CurseurSource curseur) {
        if (curseur.aFini()) {
            throw new IllegalArgumentException("unterminated escape");
        }
        char code = curseur.courrant();
        curseur.position++;
        return switch (code) {
            case '"' -> '"';
            case '\\' -> '\\';
            case '/' -> '/';
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case 'u' -> unicode(curseur);
            default -> throw new IllegalArgumentException(
                    "invalid escape at " + (curseur.position - 1));
        };
    }

    private static char unicode(CurseurSource curseur) {
        if (curseur.position + 4 > curseur.source.length()) {
            throw new IllegalArgumentException("short unicode escape");
        }
        int valeur = 0;
        for (int i = 0; i < 4; i++) {
            int chiffre = Character.digit(curseur.source.charAt(curseur.position + i), 16);
            if (chiffre < 0) {
                throw new IllegalArgumentException("invalid unicode escape");
            }
            valeur = valeur * 16 + chiffre;
        }
        curseur.position += 4;
        return (char) valeur;
    }
}
