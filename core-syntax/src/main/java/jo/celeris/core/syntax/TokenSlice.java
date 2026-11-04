package jo.celeris.core.syntax;

import jo.celeris.core.text.Document;

/**
 * Tranche de jetons matérialisée pour un parcours ou un chunk (ADR-004) :
 * une vue immuable sur les tableaux de jetons d'une plage de lignes, à
 * offsets RELATIFS au début de la tranche (le chunk choisit sa base).
 * Le parseur du langage y lit sans jamais toucher le document entier —
 * la matérialisation est le seul coût, et il est proportionnel à la
 * plage.
 *
 * <p>Porte aussi l'appariement local des crochets ({@link #matchOf}) —
 * le parseur tolérant s'en sert pour borner les blocs sans pile à lui.</p>
 */
public final class TokenSlice {

    private final Document doc;
    private final Token[] jetons;
    private final int[] lignes;
    private final int from;
    private final int to;
    private final int baseOffset;

    TokenSlice(Document doc, Token[] jetons, int[] lignes, int from, int to, int baseOffset) {
        this.doc = doc;
        this.jetons = jetons;
        this.lignes = lignes;
        this.from = from;
        this.to = to;
        this.baseOffset = baseOffset;
    }

    /** Nombre de jetons de la tranche. */
    public int size() {
        return to - from;
    }

    /** La sorte du jeton d'indice donné (indices locaux à la tranche). */
    public TokenKind kind(int index) {
        return jetons[from + index].kind();
    }

    /** Le début du jeton, relatif à la base de la tranche. */
    public int start(int index) {
        Token jeton = jetons[from + index];
        return doc.lineStart(lignes[from + index]) + jeton.start() - baseOffset;
    }

    /** La fin exclusive du jeton, relative à la base de la tranche. */
    public int end(int index) {
        Token jeton = jetons[from + index];
        return doc.lineStart(lignes[from + index]) + jeton.end() - baseOffset;
    }

    /** La ligne absolue du jeton. */
    public int line(int index) {
        return lignes[from + index];
    }

    /** Le texte du jeton. */
    public String text(int index) {
        Token jeton = jetons[from + index];
        return doc.lineText(lignes[from + index])
                .substring(jeton.start(), jeton.end());
    }

    /**
     * L'indice du crochet fermant qui apparie l'ouvrant d'indice donné —
     * balayage local avec profondeur, sans pile allouée ; -1 si non fermé
     * dans la tranche.
     *
     * @param open l'indice d'un jeton OPEN_BRACKET
     * @return l'indice du CLOSE_BRACKET partenaire, ou -1
     */
    public int matchOf(int open) {
        int profondeur = 0;
        for (int i = open; i < size(); i++) {
            TokenKind sorte = kind(i);
            if (sorte == TokenKind.OPEN_BRACKET) {
                profondeur++;
            } else if (sorte == TokenKind.CLOSE_BRACKET) {
                profondeur--;
                if (profondeur == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}
