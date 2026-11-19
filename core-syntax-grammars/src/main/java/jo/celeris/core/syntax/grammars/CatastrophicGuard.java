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
 * {@link Delimiter}. Le balayage est délibérément conservateur :
 * les formes bornées ({@code (a+){2}}) passent, les atténuations
 * subtiles (possessifs intérieurs) restent refusées — un faux négatif
 * coûte un ralentissement, un faux positif une règle écartée et
 * comptée au rapport (H-05) : la règle absente ne colore rien, le
 * paquet charge quand même, jamais un gel de l'interface.</p>
 *
 * <p>Le quantificateur non borné se rapporte à tout groupe ouvert à
 * sa position, pas au sommet seul : un groupe CONTENANT un autre
 * groupe non borné est lui-même non borné — c'est la lettre de la
 * règle, {@code ((a+))+} vit le même refus que {@code (a+)+}.</p>
 */
final class CatastrophicGuard {

    private CatastrophicGuard() {
    }

    /**
     * Vrai quand le motif porte une forme catastrophique visible.
     *
     * @param pattern le texte du motif
     * @return vrai si la forme est catastrophique
     */
    static boolean catastrophique(String pattern) {
        java.util.Deque<Groupe> pile = new java.util.ArrayDeque<>();
        boolean classe = false;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '\\') {
                i++;
            } else if (classe) {
                classe = c != ']';
            } else if (c == '[') {
                classe = true;
            } else if (c == '(') {
                pile.push(new Groupe(i));
            } else if (c == ')') {
                if (referme(pile, pattern, i)) {
                    return true;
                }
            } else if (!pile.isEmpty() && quantificateurNonBorne(c, pattern, i)) {
                // le quantificateur non borné vit dans TOUS les groupes
                // ouverts à sa position : la forme ((a+))+ le cachait
                // au sommet seul — le mal remonte jusqu'au père
                for (Groupe ouvert : pile) {
                    ouvert.nonBorne = true;
                }
            }
        }
        return false;
    }

    /** Referme le groupe au sommet : catastrophique s'il est non borné, quantifié sans borne et indélimité. */
    private static boolean referme(java.util.Deque<Groupe> pile, String pattern, int fermeture) {
        Groupe groupe = pile.poll();
        return groupe != null && groupe.nonBorne && nonBorneApres(pattern, fermeture + 1)
                && !Delimiter.delimite(pattern, groupe.start + 1, fermeture);
    }

    /** Un quantificateur non borné suit immédiatement la position. */
    private static boolean nonBorneApres(String pattern, int apres) {
        if (apres >= pattern.length()) {
            return false;
        }
        return quantificateurNonBorne(pattern.charAt(apres), pattern, apres);
    }

    /** Vrai si le caractère ouvre une répétition sans borne. */
    private static boolean quantificateurNonBorne(char c, String pattern, int i) {
        if (c == '+' || c == '*') {
            return true;
        }
        if (c != '{') {
            return false;
        }
        int end = pattern.indexOf('}', i);
        if (end < 0) {
            return false;
        }
        String body = pattern.substring(i + 1, end);
        int virgule = body.indexOf(',');
        return virgule >= 0 && body.substring(virgule + 1).isBlank();
    }

    /** Un groupe ouvert par le balayage : sa position et son contenu non borné. */
    private static final class Groupe {

        private final int start;
        private boolean nonBorne;

        private Groupe(int start) {
            this.start = start;
        }
    }
}
