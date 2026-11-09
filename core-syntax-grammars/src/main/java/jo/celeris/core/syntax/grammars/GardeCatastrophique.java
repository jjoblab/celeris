package jo.celeris.core.syntax.grammars;

/**
 * Le garde des formes catastrophiques (ADR-014, exigence du prompt) :
 * la validation d'un paquet rejette le quantificateur non borné
 * appliqué à un groupe qui en contient un — la forme exponentielle
 * classique, {@code (a+)+}, {@code (?:\w+\s*)*}, {@code (a{2,})*}.
 *
 * <p>La forme délimitée passe : un groupe dont chaque itération
 * COMMENCE par un atome obligatoire qu'aucun autre atome du contenu ne
 * peut avaler ({@code (?:\.\w+)+} — le point des noms qualifiés)
 * sépare ses répétitions sans ambiguïté, le retour arrière y reste
 * polynomial — l'analyse de la délimitation vit dans
 * {@link Delimitation}. Le balayage est délibérément conservateur :
 * les formes bornées ({@code (a+){2}}) passent, les atténuations
 * subtiles (possessifs intérieurs) restent refusées — un faux négatif
 * coûte un ralentissement, un faux positif un message explicite que
 * l'auteur de la grammaire corrige.</p>
 */
final class GardeCatastrophique {

    private GardeCatastrophique() {
    }

    /**
     * Vrai quand le motif porte une forme catastrophique visible.
     *
     * @param motif le texte du motif
     * @return vrai si la forme est catastrophique
     */
    static boolean catastrophique(String motif) {
        java.util.Deque<Groupe> pile = new java.util.ArrayDeque<>();
        boolean classe = false;
        for (int i = 0; i < motif.length(); i++) {
            char c = motif.charAt(i);
            if (c == '\\') {
                i++;
            } else if (classe) {
                classe = c != ']';
            } else if (c == '[') {
                classe = true;
            } else if (c == '(') {
                pile.push(new Groupe(i));
            } else if (c == ')') {
                if (referme(pile, motif, i)) {
                    return true;
                }
            } else if (!pile.isEmpty() && quantificateurNonBorne(c, motif, i)) {
                pile.peek().nonBorne = true;
            }
        }
        return false;
    }

    /** Referme le groupe au sommet : catastrophique s'il est non borné, quantifié sans borne et indélimité. */
    private static boolean referme(java.util.Deque<Groupe> pile, String motif, int fermeture) {
        Groupe groupe = pile.poll();
        return groupe != null && groupe.nonBorne && nonBorneApres(motif, fermeture + 1)
                && !Delimitation.delimite(motif, groupe.debut + 1, fermeture);
    }

    /** Un quantificateur non borné suit immédiatement la position. */
    private static boolean nonBorneApres(String motif, int apres) {
        if (apres >= motif.length()) {
            return false;
        }
        return quantificateurNonBorne(motif.charAt(apres), motif, apres);
    }

    /** Vrai si le caractère ouvre une répétition sans borne. */
    private static boolean quantificateurNonBorne(char c, String motif, int i) {
        if (c == '+' || c == '*') {
            return true;
        }
        if (c != '{') {
            return false;
        }
        int fin = motif.indexOf('}', i);
        if (fin < 0) {
            return false;
        }
        String corps = motif.substring(i + 1, fin);
        int virgule = corps.indexOf(',');
        return virgule >= 0 && corps.substring(virgule + 1).isBlank();
    }

    /** Un groupe ouvert par le balayage : sa position et son contenu non borné. */
    private static final class Groupe {

        private final int debut;
        private boolean nonBorne;

        private Groupe(int debut) {
            this.debut = debut;
        }
    }
}
