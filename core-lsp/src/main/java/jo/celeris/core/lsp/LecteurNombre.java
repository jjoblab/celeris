package jo.celeris.core.lsp;

/**
 * Le lecteur de nombres JSON : signe, chiffres, fraction, exposant —
 * le moindre manquement à la grammaire (zéro en tête, point sans
 * chiffre, exposant vide) est refusé.
 */
final class LecteurNombre {

    private LecteurNombre() {
    }

    /**
     * Lit un nombre sous le curseur.
     *
     * @param curseur le curseur partagé
     * @return le nombre lu
     * @throws IllegalArgumentException si la grammaire n'est pas respectée
     */
    static JsonNombre lire(CurseurSource curseur) {
        int debut = curseur.position;
        if (!curseur.aFini() && curseur.courrant() == '-') {
            curseur.position++;
        }
        if (compterChiffres(curseur) == 0) {
            throw new IllegalArgumentException("invalid number at " + debut);
        }
        avalerFraction(curseur, debut);
        avalerExposant(curseur, debut);
        return new JsonNombre(Double.parseDouble(
                curseur.source.substring(debut, curseur.position)));
    }

    private static void avalerFraction(CurseurSource curseur, int debut) {
        if (curseur.aFini() || curseur.courrant() != '.') {
            return;
        }
        curseur.position++;
        if (compterChiffres(curseur) == 0) {
            throw new IllegalArgumentException("invalid fraction at " + debut);
        }
    }

    private static void avalerExposant(CurseurSource curseur, int debut) {
        if (curseur.aFini() || (curseur.courrant() != 'e' && curseur.courrant() != 'E')) {
            return;
        }
        curseur.position++;
        if (!curseur.aFini() && (curseur.courrant() == '+' || curseur.courrant() == '-')) {
            curseur.position++;
        }
        if (compterChiffres(curseur) == 0) {
            throw new IllegalArgumentException("invalid exponent at " + debut);
        }
    }

    /** Les chiffres consécutifs — un zéro seul n'accepte pas de suite (« 01 » refusé). */
    private static int compterChiffres(CurseurSource curseur) {
        if (curseur.aFini() || !Character.isDigit(curseur.courrant())) {
            return 0;
        }
        if (curseur.courrant() == '0') {
            curseur.position++;
            return 1;
        }
        int compteur = 0;
        while (!curseur.aFini() && Character.isDigit(curseur.courrant())) {
            curseur.position++;
            compteur++;
        }
        return compteur;
    }
}
