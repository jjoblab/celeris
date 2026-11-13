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
     * @param colonne   la colonne de l'ouvrante dans la ligne d'en-tête
     * @param colonneFin la fin exclusive de la fermante dans la dernière
     *                   ligne cachée
     * @param texte     le texte du placeholder (« {…} », « […] »,
     *                  celui du commentaire de bloc, « … »)
     */
    public record PieceDePli(int colonne, int colonneFin, String texte) {
    }

    /** Le placeholder d'un bloc d'accolades. */
    private static final String ACCOLADES = "{…}";

    /** Le placeholder d'un bloc de crochets carrés. */
    private static final String CROCHETS = "[…]";

    /** Le placeholder d'un commentaire de bloc (javadoc comprise). */
    private static final String COMMENTAIRE = "/*…*/";

    /** Le placeholder de repli — indentation ou ligne seule. */
    private static final String POINTS = "…";

    private final RowSource deleguee;
    private final Document document;
    private final FoldModel plis;
    private final LexDocument lexique;

    /**
     * Construit la décoration de plis sur une source déléguée.
     *
     * @param deleguee la source des rangées à l'image (colorée ou non)
     * @param document l'instantané affiché
     * @param plis     le modèle de plis, lu en direct
     * @param lexique  le document lexicographié de la publication à jour
     */
    public FoldedRowSource(RowSource deleguee, Document document,
            FoldModel plis, LexDocument lexique) {
        this.deleguee = deleguee;
        this.document = document;
        this.plis = plis;
        this.lexique = lexique;
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
    public RowLayout layoutOf(RowKey cle) {
        RowLayout base = deleguee.layoutOf(cle);
        int ligne = base.line();
        int fin = plis.finDe(ligne);
        if (fin <= ligne || base.start() != 0) {
            return base;
        }
        PieceDePli piece = pieceDe(document, lexique, plis, ligne, fin);
        if (piece == null) {
            return base;
        }
        return composer(base, piece, fin);
    }

    /**
     * La pièce du pli composé de la ligne d'en-tête donnée — la géométrie
     * de la source courante, null sans pli sur cette ligne.
     *
     * @param ligne la ligne d'en-tête interrogée
     * @return sa pièce, null sans pli
     */
    public PieceDePli piece(int ligne) {
        return pieceDe(document, lexique, plis, ligne, plis.finDe(ligne));
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
        int ligne = base.line();
        int fin = plis.finDe(ligne);
        if (fin > ligne && base.start() == 0) {
            PieceDePli piece = pieceDe(document, lexique, plis, ligne, fin);
            if (piece != null) {
                return composer(base, piece, fin);
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
     * @param lexique  le document lexicographié à jour
     * @param plis     le modèle de plis, lu en direct
     * @param ligne    la ligne d'en-tête du pli
     * @param fin      la dernière ligne cachée du pli
     * @return la pièce du placeholder, null sans géométrie
     */
    public static @Nullable PieceDePli pieceDe(Document document,
            LexDocument lexique, FoldModel plis, int ligne, int fin) {
        if (!plis.isFolded(ligne) || fin <= ligne) {
            return null;
        }
        PieceDePli commentaire = pieceCommentaire(document, lexique, ligne, fin);
        if (commentaire != null) {
            return commentaire;
        }
        return pieceCrochets(document, lexique, ligne, fin);
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
            LexDocument lexique, int ligne, int fin) {
        String texteEntete = document.lineText(ligne);
        Token premier = premierJeton(lexique, ligne);
        if (premier == null || !estCommentaire(premier)) {
            return null;
        }
        int colonne = colonneDuGlyphe(texteEntete, premier);
        String texteFin = document.lineText(fin);
        int colonneFin = colonneFinDe(lexique, fin, texteFin);
        boolean bloc = texteEntete.startsWith("/*", colonne);
        return new PieceDePli(colonne, colonneFin,
                bloc ? COMMENTAIRE : POINTS);
    }

    /**
     * La fin du pli de commentaire dans sa dernière ligne : la fin de
     * son dernier jeton de commentaire, le crochet fermant d'une
     * région fusionnée (la déclaration documentée ferme à sa
     * fermante), la fin de la ligne en dernier recours.
     */
    private static int colonneFinDe(LexDocument lexique, int fin,
            String texteFin) {
        Token dernier = dernierJetonCommentaire(lexique, fin);
        if (dernier != null) {
            return Math.min(dernier.end(), texteFin.length());
        }
        for (Token jeton : lexique.line(fin).tokens()) {
            if (jeton.kind() == TokenKind.CLOSE_BRACKET) {
                int colonne = colonneDuGlyphe(texteFin, jeton);
                if (colonne < texteFin.length()) {
                    return colonne + 1;
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
            LexDocument lexique, int ligne, int fin) {
        List<int[]> pile = new ArrayList<>();
        int colonneOuvrante = -1;
        char ouvrante = 0;
        int colonneFermante = -1;
        for (int l = ligne; l <= fin; l++) {
            String texte = document.lineText(l);
            for (Token jeton : lexique.line(l).tokens()) {
                if (jeton.kind() == TokenKind.OPEN_BRACKET) {
                    int colonne = colonneDuGlyphe(texte, jeton);
                    if (colonne < texte.length()) {
                        pile.add(new int[] {l, colonne,
                                texte.charAt(colonne)});
                    }
                } else if (jeton.kind() == TokenKind.CLOSE_BRACKET) {
                    int colonne = colonneDuGlyphe(texte, jeton);
                    if (colonne >= texte.length() || pile.isEmpty()) {
                        continue;
                    }
                    int[] sommet = pile.remove(pile.size() - 1);
                    if (sommet[0] == ligne && l == fin
                            && (colonneOuvrante < 0
                                    || sommet[1] < colonneOuvrante)) {
                        colonneOuvrante = sommet[1];
                        ouvrante = (char) sommet[2];
                        colonneFermante = colonne + 1;
                    }
                }
            }
        }
        if (colonneOuvrante < 0) {
            return null;
        }
        return new PieceDePli(colonneOuvrante, colonneFermante,
                ouvrante == '{' ? ACCOLADES : CROCHETS);
    }

    /**
     * Compose la rangée d'un pli : préfixe coloré jusqu'à l'ouvrante,
     * placeholder à l'encre de documentation, suffixe coloré de la
     * fermante — aucune trouée, la couverture reste sans interstice.
     */
    private RowLayout composer(RowLayout base, PieceDePli piece, int fin) {
        String texteEntete = document.lineText(base.line());
        String texteFin = document.lineText(fin);
        int ouvrante = Math.min(piece.colonne(), texteEntete.length());
        int fermante = Math.min(piece.colonneFin(), texteFin.length());
        String prefixe = texteEntete.substring(0, ouvrante);
        String suffixe = fermante < texteFin.length()
                ? texteFin.substring(fermante) : "";
        String texte = prefixe + piece.texte() + suffixe;
        float largeurCar = largeurCarDe(base);
        List<ColoredSpan> plages = new ArrayList<>();
        plages.addAll(fragmente(lexique.line(base.line()), 0, ouvrante, 0));
        plages.add(new ColoredSpan(TokenKind.DOC, prefixe.length(),
                prefixe.length() + piece.texte().length()));
        plages.addAll(fragmente(lexique.line(fin), fermante,
                texteFin.length(), prefixe.length() + piece.texte().length()
                        - fermante));
        return new RowLayout(base.line(), 0, texte.length(), texte,
                texte.length() * largeurCar, List.of(), plages);
    }

    /**
     * Les plages colorées d'un FRAGMENT de ligne : les jetons rognés à
     * la fenêtre du fragment, décalés au rang voulu du composite, les
     * trous comblés à l'identificateur — la couverture sans trou du
     * décorateur coloré, servie au fragment.
     */
    private static List<ColoredSpan> fragmente(LexedLine panier,
            int debutFragment, int finFragment, int decalage) {
        List<ColoredSpan> plages = new ArrayList<>();
        if (finFragment <= debutFragment) {
            return plages;
        }
        int curseur = debutFragment;
        for (Token jeton : panier.tokens()) {
            if (jeton.end() <= curseur) {
                continue;
            }
            if (jeton.start() >= finFragment) {
                break;
            }
            if (jeton.start() > curseur) {
                plages.add(new ColoredSpan(TokenKind.IDENTIFIER,
                        curseur + decalage, jeton.start() + decalage));
            }
            int debutRogne = Math.max(jeton.start(), curseur);
            int finRogne = Math.min(jeton.end(), finFragment);
            if (finRogne > debutRogne) {
                plages.add(new ColoredSpan(jeton.kind(),
                        debutRogne + decalage, finRogne + decalage,
                        jeton.scope()));
            }
            curseur = Math.max(curseur, jeton.end());
            if (curseur >= finFragment) {
                break;
            }
        }
        if (curseur < finFragment) {
            plages.add(new ColoredSpan(TokenKind.IDENTIFIER,
                    curseur + decalage, finFragment + decalage));
        }
        return plages;
    }

    /** La largeur d'un caractère de la rangée de base, en pixels. */
    private static float largeurCarDe(RowLayout base) {
        if (base.text().isEmpty()) {
            return 0f;
        }
        return base.width() / base.text().length();
    }

    /** Le premier jeton d'une ligne, null si sa corbeille est vide. */
    private static @Nullable Token premierJeton(LexDocument lexique, int ligne) {
        Token[] jetons = lexique.line(ligne).tokens();
        return jetons.length == 0 ? null : jetons[0];
    }

    /** Le dernier jeton de commentaire de la ligne donnée, null sans. */
    private static @Nullable Token dernierJetonCommentaire(LexDocument lexique,
            int ligne) {
        Token dernier = null;
        for (Token jeton : lexique.line(ligne).tokens()) {
            if (estCommentaire(jeton)) {
                dernier = jeton;
            }
        }
        return dernier;
    }

    /** Vrai quand le jeton parle en commentaire ou documentation. */
    private static boolean estCommentaire(Token jeton) {
        return jeton.kind() == TokenKind.COMMENT
                || jeton.kind() == TokenKind.DOC;
    }

    /**
     * La colonne du glyphe d'un jeton : son premier caractère non blanc
     * dans SA portée (les blancs soudés ne sont pas le glyphe).
     */
    private static int colonneDuGlyphe(String texte, Token jeton) {
        int colonne = jeton.start();
        int fin = Math.min(jeton.end(), texte.length());
        while (colonne < fin
                && (texte.charAt(colonne) == ' ' || texte.charAt(colonne) == '\t')) {
            colonne++;
        }
        return colonne;
    }
}
