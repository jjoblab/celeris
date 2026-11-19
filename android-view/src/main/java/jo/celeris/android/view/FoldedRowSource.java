package jo.celeris.android.view;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.syntax.LexDocument;
import jo.celeris.core.syntax.LexedLine;
import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.text.Document;

import org.jspecify.annotations.Nullable;

/**
 * La source des rangées composées de pli (le constat du propriétaire
 * du 2026-11-13 : « il devrait afficher les {...} lorsque c'est plié »)
 * : une ligne d'en-tête repliée se rend comme PRÉFIXE + PLACEHOLDER +
 * SUFFIXE — le préfixe garde la coloration de sa ligne jusqu'à la
 * colonne du crochet ouvrant, le placeholder « {…} » (ou « […] »,
 * celui du commentaire de bloc pour une javadoc) remplace le corps
 * replié, le suffixe rapporte la fin de la dernière ligne cachée
 * après sa fermante, à la couleur de SA ligne. Le motif est celui du
 * dépôt de départ : la chip se distingue d'une ligne normale.
 *
 * <p>La décoration vit AU-DESSUS de la source colorée : une rangée non
 * pliée passe telle quelle (plages comprises), la première tranche
 * d'une ligne repliée se compose, ses tranches de continuation se
 * passent telles quelles (le pli cache rarement une ligne coupée).
 * La géométrie du placeholder vit dans {@link #pieceDe} — LA source
 * unique que la couche de fond ({@link FoldPlaceholder}) relit à
 * l'image ; le pli ne change pas la révision du document, le cache de
 * rangées se vide donc au repli (PIEGE-02, la discipline déjà posée par
 * la bascule au doigt).</p>
 */
public final class FoldedRowSource implements RowSource {

    /**
     * La pièce d'un pli replié : la colonne du crochet ouvrant dans SA
     * ligne (la longueur du préfixe), la colonne de FIN EXCLUSIVE de la
     * fermante dans la dernière ligne cachée (le début du suffixe), et
     * le texte du placeholder.
     *
     * @param column   la colonne de l'ouvrante dans la ligne d'en-tête
     * @param colonneFin la fin exclusive de la fermante dans la dernière
     *                   ligne cachée
     * @param text     le texte du placeholder (« {…} », « […] »,
     *                  celui du commentaire de bloc, « … »)
     */
    public record PieceDePli(int column, int colonneFin, String text) {
    }

    /** Le placeholder d'un bloc d'accolades. */
    private static final String BRACES = "{…}";

    /** Le placeholder d'un bloc de crochets carrés. */
    private static final String CROCHETS = "[…]";

    /** Le placeholder d'un commentaire de bloc (javadoc comprise). */
    private static final String COMMENTAIRE = "/*…*/";

    /** Le placeholder de repli — indentation ou ligne seule. */
    private static final String POINTS = "…";

    private final RowSource deleguee;
    private final Document document;
    private final FoldModel folds;
    private final LexDocument lexicon;

    /**
     * Construit la décoration de plis sur une source déléguée.
     *
     * @param deleguee la source des rangées à l'image (colorée ou non)
     * @param document l'instantané affiché
     * @param folds     le modèle de plis, lu en direct
     * @param lexicon  le document lexicographié de la publication à jour
     */
    public FoldedRowSource(RowSource deleguee, Document document,
            FoldModel folds, LexDocument lexicon) {
        this.deleguee = deleguee;
        this.document = document;
        this.folds = folds;
        this.lexicon = lexicon;
    }

    /**
     * La source colorée enveloppée — le respan du cache s'y sert pour
     * les rangées non composées (PIEGE-26 : une publication ne rejette
     * pas les mesures).
     *
     * @return la source colorée déléguée
     */
    public RowSource deleguee() {
        return deleguee;
    }

    @Override
    public RowLayout layoutOf(RowKey key) {
        RowLayout base = deleguee.layoutOf(key);
        int line = base.line();
        int end = folds.finDe(line);
        if (end <= line || base.start() != 0) {
            return base;
        }
        PieceDePli piece = pieceDe(document, lexicon, folds, line, end);
        if (piece == null) {
            return base;
        }
        return composer(base, piece, end);
    }

    /**
     * La pièce du pli composé de la ligne d'en-tête donnée — la géométrie
     * de la source courante, null sans pli sur cette ligne.
     *
     * @param line la ligne d'en-tête interrogée
     * @return sa pièce, null sans pli
     */
    public PieceDePli piece(int line) {
        return pieceDe(document, lexicon, folds, line, folds.finDe(line));
    }

    /**
     * RE-SPAM une mise en page en cache : une rangée composée se
     * recompose entière (le texte plié a pu changer d'une publication à
     * l'autre), une rangée ordinaire respanne chez sa source colorée
     * déléguée (PIEGE-26).
     *
     * @param base la mise en page en cache
     * @return la mise en page à jour — la même si rien n'a changé
     */
    public RowLayout respan(RowLayout base) {
        int line = base.line();
        int end = folds.finDe(line);
        if (end > line && base.start() == 0) {
            PieceDePli piece = pieceDe(document, lexicon, folds, line, end);
            if (piece != null) {
                return composer(base, piece, end);
            }
        }
        if (deleguee instanceof SyntaxRowSource coloree) {
            return coloree.respan(base);
        }
        return base;
    }

    /**
     * La pièce d'un pli replié : la colonne de l'ouvrante, la fin
     * exclusive de sa fermante appariée, et le placeholder qui la
     * remplace — null sans pli, sans paire ou sans commentaire.
     *
     * <p>L'appariement balaye les jetons de l'en-tête à la fermante,
     * pile des ouvrantes en main : parmi les paires qui ouvrent sur
     * l'en-tête et ferment sur la dernière ligne cachée, la PREMIÈRE
     * colonne ouvrante gagne — la plus extérieure, celle du bloc que
     * le pli replie. Un commentaire de bloc (javadoc comprise) ouvre
     * à sa colonne et ferme à la fin de son jeton sur la dernière
     * ligne ; une suite de commentaires de ligne se replie sur ses
     * points.</p>
     *
     * @param document l'instantané affiché
     * @param lexicon  le document lexicographié à jour
     * @param folds     le modèle de plis, lu en direct
     * @param line    la ligne d'en-tête du pli
     * @param end      la dernière ligne cachée du pli
     * @return la pièce du placeholder, null sans géométrie
     */
    public static @Nullable PieceDePli pieceDe(Document document,
            LexDocument lexicon, FoldModel folds, int line, int end) {
        if (!folds.isFolded(line) || end <= line) {
            return null;
        }
        PieceDePli commentaire = pieceCommentaire(document, lexicon, line, end);
        if (commentaire != null) {
            return commentaire;
        }
        return pieceCrochets(document, lexicon, line, end);
    }

    /**
     * La pièce d'un pli de commentaire : la première ligne du pli
     * porte un jeton de commentaire ou de documentation en tête — le
     * pli des commentaires consécutifs (l'arbre des plis les ramasse,
     * la région fusionnée d'une déclaration documentée commence à sa
     * javadoc). Un bloc « slash-étoile » ouvre à sa colonne et ferme à
     * la fin du jeton de sa dernière ligne ; une région fusionnée
     * (javadoc + déclaration) ferme au crochet fermant de sa dernière
     * ligne ; une suite de commentaires de ligne se replie sur ses
     * points, à la fin du dernier jeton.
     */
    private static @Nullable PieceDePli pieceCommentaire(Document document,
            LexDocument lexicon, int line, int end) {
        String texteEntete = document.lineText(line);
        Token premier = premierJeton(lexicon, line);
        if (premier == null || !estCommentaire(premier)) {
            return null;
        }
        int column = colonneDuGlyphe(texteEntete, premier);
        String texteFin = document.lineText(end);
        int colonneFin = colonneFinDe(lexicon, end, texteFin);
        boolean bloc = texteEntete.startsWith("/*", column);
        return new PieceDePli(column, colonneFin,
                bloc ? COMMENTAIRE : POINTS);
    }

    /**
     * La fin du pli de commentaire dans sa dernière ligne : la fin de
     * son dernier jeton de commentaire, le crochet fermant d'une
     * région fusionnée (la déclaration documentée ferme à sa
     * fermante), la fin de la ligne en dernier recours.
     */
    private static int colonneFinDe(LexDocument lexicon, int end,
            String texteFin) {
        Token dernier = dernierJetonCommentaire(lexicon, end);
        if (dernier != null) {
            return Math.min(dernier.end(), texteFin.length());
        }
        for (Token token : lexicon.line(end).tokens()) {
            if (token.kind() == TokenKind.CLOSE_BRACKET) {
                int column = colonneDuGlyphe(texteFin, token);
                if (column < texteFin.length()) {
                    return column + 1;
                }
            }
        }
        return texteFin.length();
    }

    /**
     * La pièce d'un pli de crochets : l'appariement balaye les jetons
     * de l'en-tête à la dernière ligne cachée, pile des ouvrantes en
     * main — les accolades d'une chaîne ou d'un commentaire n'y
     * entrent pas (le lexeur ne leur donne pas de jeton de crochet).
     */
    private static @Nullable PieceDePli pieceCrochets(Document document,
            LexDocument lexicon, int line, int end) {
        List<int[]> pile = new ArrayList<>();
        int colonneOuvrante = -1;
        char ouvrante = 0;
        int colonneFermante = -1;
        for (int l = line; l <= end; l++) {
            String text = document.lineText(l);
            for (Token token : lexicon.line(l).tokens()) {
                if (token.kind() == TokenKind.OPEN_BRACKET) {
                    int column = colonneDuGlyphe(text, token);
                    if (column < text.length()) {
                        pile.add(new int[] {l, column,
                                text.charAt(column)});
                    }
                } else if (token.kind() == TokenKind.CLOSE_BRACKET) {
                    int column = colonneDuGlyphe(text, token);
                    if (column >= text.length() || pile.isEmpty()) {
                        continue;
                    }
                    int[] sommet = pile.remove(pile.size() - 1);
                    if (sommet[0] == line && l == end
                            && (colonneOuvrante < 0
                                    || sommet[1] < colonneOuvrante)) {
                        colonneOuvrante = sommet[1];
                        ouvrante = (char) sommet[2];
                        colonneFermante = column + 1;
                    }
                }
            }
        }
        if (colonneOuvrante < 0) {
            return null;
        }
        return new PieceDePli(colonneOuvrante, colonneFermante,
                ouvrante == '{' ? BRACES : CROCHETS);
    }

    /**
     * Compose la rangée d'un pli : préfixe coloré jusqu'à l'ouvrante,
     * placeholder à l'encre de documentation, suffixe coloré de la
     * fermante — aucune trouée, la couverture reste sans interstice.
     */
    private RowLayout composer(RowLayout base, PieceDePli piece, int end) {
        String texteEntete = document.lineText(base.line());
        String texteFin = document.lineText(end);
        int ouvrante = Math.min(piece.column(), texteEntete.length());
        int fermante = Math.min(piece.colonneFin(), texteFin.length());
        String prefixe = texteEntete.substring(0, ouvrante);
        String suffixe = fermante < texteFin.length()
                ? texteFin.substring(fermante) : "";
        String text = prefixe + piece.text() + suffixe;
        float largeurCar = charWidthOf(base);
        List<ColoredSpan> ranges = new ArrayList<>();
        ranges.addAll(fragmente(lexicon.line(base.line()), 0, ouvrante, 0));
        ranges.add(new ColoredSpan(TokenKind.DOC, prefixe.length(),
                prefixe.length() + piece.text().length()));
        ranges.addAll(fragmente(lexicon.line(end), fermante,
                texteFin.length(), prefixe.length() + piece.text().length()
                        - fermante));
        return new RowLayout(base.line(), 0, text.length(), text,
                text.length() * largeurCar, List.of(), ranges);
    }

    /**
     * Les plages colorées d'un FRAGMENT de ligne : les jetons rognés à
     * la fenêtre du fragment, décalés au rang voulu du composite, les
     * trous comblés à l'identificateur — la couverture sans trou du
     * décorateur coloré, servie au fragment.
     */
    private static List<ColoredSpan> fragmente(LexedLine panier,
            int debutFragment, int finFragment, int decalage) {
        List<ColoredSpan> ranges = new ArrayList<>();
        if (finFragment <= debutFragment) {
            return ranges;
        }
        int cursor = debutFragment;
        for (Token token : panier.tokens()) {
            if (token.end() <= cursor) {
                continue;
            }
            if (token.start() >= finFragment) {
                break;
            }
            if (token.start() > cursor) {
                ranges.add(new ColoredSpan(TokenKind.IDENTIFIER,
                        cursor + decalage, token.start() + decalage));
            }
            int debutRogne = Math.max(token.start(), cursor);
            int finRogne = Math.min(token.end(), finFragment);
            if (finRogne > debutRogne) {
                ranges.add(new ColoredSpan(token.kind(),
                        debutRogne + decalage, finRogne + decalage,
                        token.scope()));
            }
            cursor = Math.max(cursor, token.end());
            if (cursor >= finFragment) {
                break;
            }
        }
        if (cursor < finFragment) {
            ranges.add(new ColoredSpan(TokenKind.IDENTIFIER,
                    cursor + decalage, finFragment + decalage));
        }
        return ranges;
    }

    /** La largeur d'un caractère de la rangée de base, en pixels. */
    private static float charWidthOf(RowLayout base) {
        if (base.text().isEmpty()) {
            return 0f;
        }
        return base.width() / base.text().length();
    }

    /** Le premier jeton d'une ligne, null si sa corbeille est vide. */
    private static @Nullable Token premierJeton(LexDocument lexicon, int line) {
        Token[] tokens = lexicon.line(line).tokens();
        return tokens.length == 0 ? null : tokens[0];
    }

    /** Le dernier jeton de commentaire de la ligne donnée, null sans. */
    private static @Nullable Token dernierJetonCommentaire(LexDocument lexicon,
            int line) {
        Token dernier = null;
        for (Token token : lexicon.line(line).tokens()) {
            if (estCommentaire(token)) {
                dernier = token;
            }
        }
        return dernier;
    }

    /** Vrai quand le jeton parle en commentaire ou documentation. */
    private static boolean estCommentaire(Token token) {
        return token.kind() == TokenKind.COMMENT
                || token.kind() == TokenKind.DOC;
    }

    /**
     * La colonne du glyphe d'un jeton : son premier caractère non blanc
     * dans SA portée (les blancs soudés ne sont pas le glyphe).
     */
    private static int colonneDuGlyphe(String text, Token token) {
        int column = token.start();
        int end = Math.min(token.end(), text.length());
        while (column < end
                && (text.charAt(column) == ' ' || text.charAt(column) == '\t')) {
            column++;
        }
        return column;
    }
}
