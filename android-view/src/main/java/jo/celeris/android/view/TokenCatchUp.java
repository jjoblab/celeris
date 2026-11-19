package jo.celeris.android.view;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.text.Document;
import jo.celeris.core.syntax.EditSite;
import jo.celeris.core.syntax.LanguageLexer;
import jo.celeris.core.syntax.LexDocument;
import jo.celeris.core.syntax.LexState;
import jo.celeris.core.syntax.LexedLine;
import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;

import org.jspecify.annotations.Nullable;

/**
 * Le rattrapage des jetons d'une publication périmée (M7.5, §6,
 * PIEGE-26) : la ligne touchée par une édition en attente ne retombe
 * PLUS à l'encre de base — ses jetons publiés se recalent sur
 * l'édition, stale-while-revalidate.
 *
 * <p>Trois voies, dans l'ordre :</p>
 * <ol>
 * <li>la ligne NON touchée sert SON panier publié — le contenu est
 * démontré identique (la garantie de l'ancien RemapLignes, tenue) ;</li>
 * <li>la ligne touchée par des éditions SANS changement de structure
 * (ni saut de ligne retiré ni inséré) reçoit ses jetons publiés
 * recalés : avant l'édition ils restent, après ils se décalent du
 * delta, celui qui contient le point d'insertion s'étend — taper dans
 * un identificateur ne change pas sa couleur — et le texte inséré
 * hérite de la catégorie du jeton de gauche ;</li>
 * <li>la ligne touchée par un changement de structure se relit à
 * partir de l'état publié qui la précède — le lexeur par ligne du
 * langage, quand il existe : le provisoire peut être imparfait, il ne
 * doit jamais être vide.</li>
 * </ol>
 *
 * <p>Pure et testable sans SDK : le lexique publié, les éditions en
 * attente (décalées en coordonnées PUBLIÉES dès leur enregistrement),
 * le document courant et le lexeur sont injectés.</p>
 */
public final class TokenCatchUp {

    /**
     * Une édition en attente, en coordonnées de la ligne PUBLIÉE
     * qu'elle touche : le début de la plage remplacée DANS SA LIGNE,
     * les longueurs retirée et insérée, et le changement de structure
     * (saut de ligne retiré ou inséré).
     *
     * @param startInLine   le début de la plage remplacée, relatif à la
     *                      ligne publiée touchée
     * @param removedLength la longueur retirée
     * @param insertedLength la longueur insérée
     * @param publishedLine la ligne publiée touchée
     * @param structural    vrai si l'édition retire ou insère un saut de ligne
     */
    public record PendingEdit(int startInLine, int removedLength,
            int insertedLength, int publishedLine, boolean structural) {

        /** Le delta de longueur apporté par l'édition. */
        int delta() {
            return insertedLength - removedLength;
        }

        /** Vrai si l'édition touche la ligne publiée donnée. */
        boolean dansLigne(int line) {
            return !structural && publishedLine == line;
        }
    }

    private final LexDocument published;
    private final List<PendingEdit> edits;
    private final List<EditSite> sites;
    private final Document courant;
    private final @Nullable LanguageLexer lexeur;

    private TokenCatchUp(LexDocument published, List<PendingEdit> edits,
            List<EditSite> sites, Document courant,
            @Nullable LanguageLexer lexeur) {
        this.published = published;
        this.edits = List.copyOf(edits);
        this.sites = List.copyOf(sites);
        this.courant = courant;
        this.lexeur = lexeur;
    }

    /**
     * Le rattrapage d'une publication périmée : les éditions en
     * attente, en coordonnées publiées et en ordre chronologique.
     *
     * @param published    le document lexicographié publié
     * @param edits  les éditions en attente, coordonnées publiées
     * @param sites     les sites en lignes des mêmes éditions, en ordre
     * @param courant   le document affiché
     * @param lexeur    le lexeur par ligne du langage, null sans langage
     * @return le rattrapeur
     */
    public static TokenCatchUp of(LexDocument published, List<PendingEdit> edits,
            List<EditSite> sites, Document courant,
            @Nullable LanguageLexer lexeur) {
        return new TokenCatchUp(published, edits, sites, courant, lexeur);
    }

    /**
     * Le panier provisoire de la ligne courante donnée : SON panier
     * publié si son contenu a survécu, ses jetons recalés si elle est
     * touchée, sa relecture si la structure a changé — null quand
     * rien ne peut la colorer (ligne neuve sans lexeur).
     *
     * @param currentLine la ligne du document affiché
     * @return le panier provisoire, null sans rien à servir
     */
    public @Nullable LexedLine basketFor(int currentLine) {
        int[] ancetre = publishedAncestor(currentLine);
        int lignePubliee = ancetre[0];
        boolean touchee = ancetre[1] != 0;
        if (lignePubliee < 0 || lignePubliee >= published.lineCount()) {
            // Ligne sans ancêtre publié (bas de document neuf) : la
            // relecture depuis le dernier état publié connu.
            return relire(currentLine, published.lineCount() - 1);
        }
        if (!touchee) {
            return published.line(lignePubliee);
        }
        if (editionsStructurelles()) {
            return relire(currentLine, lignePubliee);
        }
        return realignment(published.line(lignePubliee), lignePubliee);
    }

    /** Vrai quand une édition en attente retire ou insère un saut de ligne. */
    private boolean editionsStructurelles() {
        for (PendingEdit edit : edits) {
            if (edit.structural()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Le recalage : les jetons de la ligne publiée, recalés sur les
     * éditions en attente de CETTE ligne — chirurgie fine, les
     * caractères non touchés gardent leur catégorie exacte.
     */
    private LexedLine realignment(LexedLine published, int lignePubliee) {
        LexedLine resultat = published;
        for (PendingEdit edit : edits) {
            if (edit.dansLigne(lignePubliee)) {
                resultat = recaler(resultat, edit);
            }
        }
        return resultat;
    }

    /**
     * La relecture : la ligne courante se lexicographie depuis l'état
     * publié qui précède son ancêtre — provisoire, jamais vide.
     */
    private @Nullable LexedLine relire(int currentLine, int lignePubliee) {
        LanguageLexer lexeurLocal = lexeur;
        if (lexeurLocal == null) {
            return null;
        }
        LexState input = published.stateBefore(Math.max(0, lignePubliee));
        CharSequence text = courant.lineText(currentLine);
        return lexeurLocal.lex(text, input);
    }

    /**
     * L'ancêtre publié d'une ligne courante : les sites se rejouent du
     * plus récent au plus ancien, chaque édition décale les lignes qui
     * la suivent ; la ligne DANS une zone touchée descend de la
     * première ligne de la zone — une approximation assumée pour les
     * découpes et les fusions, exacte pour les éditions d'une seule
     * ligne.
     */
    private int[] publishedAncestor(int currentLine) {
        int line = currentLine;
        int touchee = 0;
        for (int i = sites.size() - 1; i >= 0; i--) {
            EditSite site = sites.get(i);
            if (line < site.firstLine()) {
                continue;
            }
            if (line > site.lastLine()) {
                line -= site.lineDelta();
                continue;
            }
            line = site.firstLine();
            touchee = 1;
        }
        return new int[] {line, touchee};
    }

    /**
     * La chirurgie d'une édition sur un panier : les jetons entièrement
     * avant l'édition restent, ceux après se décalent du delta, celui
     * qui contient le point d'insertion s'étend — et le texte inséré
     * hérite de la catégorie du jeton de gauche, à défaut de
     * l'identificateur.
     */
    private static LexedLine recaler(LexedLine panier, PendingEdit edit) {
        Token[] tokens = panier.tokens();
        int start = edit.startInLine();
        int finRemplacement = start + edit.removedLength();
        int delta = edit.delta();
        List<Token> resultat = new ArrayList<>(tokens.length + 1);
        TokenKind categorieInseree = categorieAGauche(tokens, start);
        for (Token token : tokens) {
            if (token.end() <= start) {
                resultat.add(token);
                continue;
            }
            if (token.start() >= finRemplacement) {
                setInserted(resultat, categorieInseree, start,
                        edit.insertedLength());
                resultat.add(new Token(token.kind(), token.start() + delta,
                        token.end() + delta));
                continue;
            }
            if (token.start() <= start && token.end() >= finRemplacement) {
                // Il contient le point d'insertion : il s'étend (ou se
                // resserre) — taper dans un identificateur ne change
                // pas sa couleur.
                int end = token.end() + delta;
                if (end > token.start()) {
                    resultat.add(new Token(token.kind(), token.start(), end));
                }
                continue;
            }
            recalerACheval(token, start, finRemplacement, delta,
                    categorieInseree, edit.insertedLength(), resultat);
        }
        if (resultat.isEmpty() && edit.insertedLength() > 0) {
            setInserted(resultat, categorieInseree, start,
                    edit.insertedLength());
        }
        return new LexedLine(resultat.toArray(new Token[0]), panier.exitState());
    }

    /**
     * La catégorie du texte inséré : celle du jeton qui précède le
     * point d'édition, à défaut l'identificateur.
     */
    private static TokenKind categorieAGauche(Token[] tokens, int start) {
        for (Token token : tokens) {
            if (token.end() > start) {
                break;
            }
            if (token.kind() != TokenKind.IDENTIFIER) {
                return token.kind();
            }
        }
        return TokenKind.IDENTIFIER;
    }

    /** Pose la plage du texte inséré, sauf si déjà couverte. */
    private static void setInserted(List<Token> resultat,
            TokenKind categorie, int start, int length) {
        if (length <= 0 || !resultat.isEmpty()
                && resultat.get(resultat.size() - 1).end() > start) {
            return;
        }
        resultat.add(new Token(categorie, start, start + length));
    }

    /**
     * Le jeton à cheval sur la plage remplacée : son morceau gauche
     * reste, l'insertion hérite de sa catégorie, son morceau droit se
     * décale.
     */
    private static void recalerACheval(Token token, int start,
            int finRemplacement, int delta, TokenKind categorieInseree,
            int longueurInseree, List<Token> resultat) {
        if (token.start() < start) {
            resultat.add(new Token(token.kind(), token.start(), start));
        }
        setInserted(resultat, token.kind() != TokenKind.IDENTIFIER
                ? token.kind() : categorieInseree, start, longueurInseree);
        if (token.end() > finRemplacement) {
            resultat.add(new Token(token.kind(),
                    finRemplacement + delta, token.end() + delta));
        }
    }
}
