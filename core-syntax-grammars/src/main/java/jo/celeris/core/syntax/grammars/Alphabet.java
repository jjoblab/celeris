package jo.celeris.core.syntax.grammars;

/**
 * L'alphabet d'un délimiteur de groupe : les caractères qu'il peut
 * matcher, tirés de sa source. Les prédicats servent au garde
 * catastrophique — décider si la suite d'un groupe peut avaler un de
 * ces caractères (classe large, négation, intervalle).
 *
 * @param source la source du motif qui porte le délimiteur
 * @param debut  le début de l'alphabet dans la source
 * @param fin    la fin (exclusive) de l'alphabet dans la source
 */
record Alphabet(String source, int debut, int fin) {

    /** Vrai si l'alphabet contient le caractère donné. */
    boolean contient(char c) {
        return existe(x -> x == c);
    }

    /** Vrai si l'alphabet compte un caractère de mot : lettre, chiffre, trait bas. */
    boolean contientMot() {
        return existe(c -> Character.isLetterOrDigit(c) || c == '_');
    }

    /** Vrai si l'alphabet compte un caractère hors des mots. */
    boolean contientNonMot() {
        return existe(c -> !(Character.isLetterOrDigit(c) || c == '_'));
    }

    /** Vrai si l'alphabet compte un chiffre. */
    boolean contientChiffre() {
        return existe(Character::isDigit);
    }

    /** Vrai si l'alphabet compte un non-chiffre. */
    boolean contientNonChiffre() {
        return existe(c -> !Character.isDigit(c));
    }

    /** Vrai si l'alphabet compte un blanc. */
    boolean contientBlanc() {
        return existe(Character::isWhitespace);
    }

    /** Vrai si l'alphabet compte un non-blanc. */
    boolean contientNonBlanc() {
        return existe(c -> !Character.isWhitespace(c));
    }

    /** Vrai si l'intervalle couvre un caractère de l'alphabet. */
    boolean couvreIntervalle(char borneInf, char borneSup) {
        return existe(c -> c >= borneInf && c <= borneSup);
    }

    /**
     * Vrai si l'échappement d'une classe large couvre l'alphabet —
     * {@code \w} couvre les mots, {@code \W} le reste ; les largeurs
     * nulles ({@code \b} et ses sœurs) ne mangent rien.
     *
     * @param echappee la lettre de l'échappement
     * @return vrai si l'échappement peut avaler un caractère du délimiteur
     */
    boolean couvreEchappement(char echappee) {
        return largeurNulle(echappee) ? false : couvreClasseLarge(echappee);
    }

    /** Les échappements de largeur nulle : assertions, jamais des atomes. */
    private static boolean largeurNulle(char echappee) {
        return "bBAzZGK".indexOf(echappee) >= 0;
    }

    /** La couverture des classes larges par lettre d'échappement. */
    private boolean couvreClasseLarge(char echappee) {
        return switch (echappee) {
            case 'w' -> contientMot();
            case 'W' -> contientNonMot();
            case 'd' -> contientChiffre();
            case 'D' -> contientNonChiffre();
            case 's', 'h' -> contientBlanc();
            case 'S', 'H' -> contientNonBlanc();
            default -> couvreLitteral(echappee);
        };
    }

    /** L'échappement restant : blanc connu, littéral, ou classe inconnue (le doute couvre). */
    private boolean couvreLitteral(char echappee) {
        if (echappee == 't' || echappee == 'n' || echappee == 'r'
                || echappee == 'f' || echappee == 'v' || echappee == 'e') {
            return contient(blanc(echappee));
        }
        return Character.isLetterOrDigit(echappee) || contient(echappee);
    }

    /** Le caractère littéral d'un échappement blanc connu. */
    private static char blanc(char echappee) {
        return switch (echappee) {
            case 't' -> '\t';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 'f' -> '\f';
            case 'v' -> '\u000B';
            default -> '\u001B';
        };
    }

    /** Vrai si un caractère de l'alphabet satisfait le prédicat. */
    private boolean existe(java.util.function.IntPredicate predicat) {
        for (int i = debut; i < fin; i++) {
            if (predicat.test(source.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
