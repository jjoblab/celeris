package jo.celeris.core.syntax;

/**
 * Document lexicographié : un panier de jetons et un état de sortie par
 * ligne, plus la profondeur de crochets au début de chaque ligne. Les
 * offsets des jetons sont relatifs à leur ligne — les paniers d'une ligne
 * au contenu inchangé se partagent entre relectures successives, c'est le
 * réemploi incrémental (ADR-004).
 *
 * <p>Immuable : chaque relecture produit une nouvelle instance, les lignes
 * non concernées y désignent les mêmes objets {@link LexedLine}.</p>
 */
public final class LexDocument {

    private final LexedLine[] lines;
    private final int[] profondeurs;
    private final LexState initial;

    LexDocument(LexedLine[] lines, int[] profondeurs, LexState initial) {
        this.lines = lines;
        this.profondeurs = profondeurs;
        this.initial = initial;
    }

    /**
     * Le lexique d'un document (ADR-0024) : fabrique PUBLIQUE — la
     * voie tree-sitter, module séparé, y bâtit son lexique sans
     * toucher au constructeur interne.
     *
     * @param lines     les lignes lexicographiées
     * @param profondeurs les profondeurs de crochets par ligne
     * @param initial    l'état du lexeur en tête de document
     * @return le lexique versionné
     */
    public static LexDocument of(LexedLine[] lines, int[] profondeurs,
            LexState initial) {
        return new LexDocument(lines, profondeurs, initial);
    }

    /** Nombre de lignes lexicographiées (une ligne vide finale comprise). */
    public int lineCount() {
        return lines.length;
    }

    /**
     * Le panier et l'état de sortie d'une ligne.
     *
     * @param line numéro de ligne, borné au document
     * @return la ligne lexicographiée (jetons vides pour une ligne blanche)
     */
    public LexedLine line(int line) {
        return lines[Math.min(Math.max(line, 0), lines.length - 1)];
    }

    /**
     * L'état d'entrée d'une ligne — sortie de la précédente, état initial
     * pour la première. La ligne VIDE du lexique partiel (fenêtre
     * d'ouverture, G-01 la suite) rend l'état initial : la complétion
     * relira la ligne depuis zéro, jamais depuis un état fantôme.
     *
     * @param line numéro de ligne
     * @return l'état avant la lecture de la ligne
     */
    public LexState stateBefore(int line) {
        if (line <= 0) {
            return initial;
        }
        LexedLine precedente = lines[Math.min(line, lines.length) - 1];
        if (precedente == EmptyLexicon.EMPTY_LINE) {
            return initial;
        }
        LexState sortie = precedente.exitState();
        // La ligne VIDE porte l'état null (lexique partiel) : l'état
        // INITIAL du document sert — la complétion relit depuis zéro.
        return sortie == null ? initial : sortie;
    }

    /**
     * La profondeur de crochets au début de la ligne — indentation et plis
     * s'y servent ; un document sans crochets reste à zéro partout.
     *
     * @param line numéro de ligne
     * @return la profondeur (nombre d'ouvrants non fermés avant la ligne)
     */
    public int bracketDepthAtLineStart(int line) {
        return profondeurs[Math.min(Math.max(line, 0), profondeurs.length - 1)];
    }

    /**
     * Le lexique porte-t-il des lignes encore VIDES (l'analyse née
     * d'une fenêtre d'ouverture dont la complétion n'a pas tout relu) :
     * l'arbre qui s'y bâtit n'a pas valeur de géométrie — le service
     * la reconnaît et rebâtit l'arbre au lexique complet.
     *
     * @return vrai quand au moins une ligne vit vide
     */
    public boolean porteLignesVides() {
        for (LexedLine ligne : lines) {
            if (ligne == EmptyLexicon.EMPTY_LINE) {
                return true;
            }
        }
        return false;
    }

    LexedLine[] lines() {
        return lines;
    }

    int[] profondeurs() {
        return profondeurs;
    }

    LexState initial() {
        return initial;
    }
}
