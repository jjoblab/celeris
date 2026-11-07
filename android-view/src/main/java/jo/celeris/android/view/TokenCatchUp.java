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
        boolean dansLigne(int ligne) {
            return !structural && publishedLine == ligne;
        }
    }

    private final LexDocument publie;
    private final List<PendingEdit> editions;
    private final List<EditSite> sites;
    private final Document courant;
    private final @Nullable LanguageLexer lexeur;

    private TokenCatchUp(LexDocument publie, List<PendingEdit> editions,
            List<EditSite> sites, Document courant,
            @Nullable LanguageLexer lexeur) {
        this.publie = publie;
        this.editions = List.copyOf(editions);
        this.sites = List.copyOf(sites);
        this.courant = courant;
        this.lexeur = lexeur;
    }

    /**
     * Le rattrapage d'une publication périmée : les éditions en
     * attente, en coordonnées publiées et en ordre chronologique.
     *
     * @param publie    le document lexicographié publié
     * @param editions  les éditions en attente, coordonnées publiées
     * @param sites     les sites en lignes des mêmes éditions, en ordre
     * @param courant   le document affiché
     * @param lexeur    le lexeur par ligne du langage, null sans langage
     * @return le rattrapeur
     */
    public static TokenCatchUp of(LexDocument publie, List<PendingEdit> editions,
            List<EditSite> sites, Document courant,
            @Nullable LanguageLexer lexeur) {
        return new TokenCatchUp(publie, editions, sites, courant, lexeur);
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
        int[] ancetre = ancetrePublie(currentLine);
        int lignePubliee = ancetre[0];
        boolean touchee = ancetre[1] != 0;
        if (lignePubliee < 0 || lignePubliee >= publie.lineCount()) {
            // Ligne sans ancêtre publié (bas de document neuf) : la
            // relecture depuis le dernier état publié connu.
            return relire(currentLine, publie.lineCount() - 1);
        }
        if (!touchee) {
            return publie.line(lignePubliee);
        }
        if (editionsStructurelles()) {
            return relire(currentLine, lignePubliee);
        }
        return recalage(publie.line(lignePubliee), lignePubliee);
    }

    /** Vrai quand une édition en attente retire ou insère un saut de ligne. */
    private boolean editionsStructurelles() {
        for (PendingEdit edition : editions) {
            if (edition.structural()) {
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
    private LexedLine recalage(LexedLine publiee, int lignePubliee) {
        LexedLine resultat = publiee;
        for (PendingEdit edition : editions) {
            if (edition.dansLigne(lignePubliee)) {
                resultat = recaler(resultat, edition);
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
        LexState entree = publie.stateBefore(Math.max(0, lignePubliee));
        CharSequence texte = courant.lineText(currentLine);
        return lexeurLocal.lex(texte, entree);
    }

    /**
     * L'ancêtre publié d'une ligne courante : les sites se rejouent du
     * plus récent au plus ancien, chaque édition décale les lignes qui
     * la suivent ; la ligne DANS une zone touchée descend de la
     * première ligne de la zone — une approximation assumée pour les
     * découpes et les fusions, exacte pour les éditions d'une seule
     * ligne.
     */
    private int[] ancetrePublie(int ligneCourante) {
        int ligne = ligneCourante;
        int touchee = 0;
        for (int i = sites.size() - 1; i >= 0; i--) {
            EditSite site = sites.get(i);
            if (ligne < site.firstLine()) {
                continue;
            }
            if (ligne > site.lastLine()) {
                ligne -= site.lineDelta();
                continue;
            }
            ligne = site.firstLine();
            touchee = 1;
        }
        return new int[] {ligne, touchee};
    }

    /**
     * La chirurgie d'une édition sur un panier : les jetons entièrement
     * avant l'édition restent, ceux après se décalent du delta, celui
     * qui contient le point d'insertion s'étend — et le texte inséré
     * hérite de la catégorie du jeton de gauche, à défaut de
     * l'identificateur.
     */
    private static LexedLine recaler(LexedLine panier, PendingEdit edition) {
        Token[] jetons = panier.tokens();
        int debut = edition.startInLine();
        int finRemplacement = debut + edition.removedLength();
        int delta = edition.delta();
        List<Token> resultat = new ArrayList<>(jetons.length + 1);
        TokenKind categorieInseree = categorieAGauche(jetons, debut);
        for (Token jeton : jetons) {
            if (jeton.end() <= debut) {
                resultat.add(jeton);
                continue;
            }
            if (jeton.start() >= finRemplacement) {
                poserInseree(resultat, categorieInseree, debut,
                        edition.insertedLength());
                resultat.add(new Token(jeton.kind(), jeton.start() + delta,
                        jeton.end() + delta));
                continue;
            }
            if (jeton.start() <= debut && jeton.end() >= finRemplacement) {
                // Il contient le point d'insertion : il s'étend (ou se
                // resserre) — taper dans un identificateur ne change
                // pas sa couleur.
                int fin = jeton.end() + delta;
                if (fin > jeton.start()) {
                    resultat.add(new Token(jeton.kind(), jeton.start(), fin));
                }
                continue;
            }
            recalerACheval(jeton, debut, finRemplacement, delta,
                    categorieInseree, edition.insertedLength(), resultat);
        }
        if (resultat.isEmpty() && edition.insertedLength() > 0) {
            poserInseree(resultat, categorieInseree, debut,
                    edition.insertedLength());
        }
        return new LexedLine(resultat.toArray(new Token[0]), panier.exitState());
    }

    /**
     * La catégorie du texte inséré : celle du jeton qui précède le
     * point d'édition, à défaut l'identificateur.
     */
    private static TokenKind categorieAGauche(Token[] jetons, int debut) {
        for (Token jeton : jetons) {
            if (jeton.end() > debut) {
                break;
            }
            if (jeton.kind() != TokenKind.IDENTIFIER) {
                return jeton.kind();
            }
        }
        return TokenKind.IDENTIFIER;
    }

    /** Pose la plage du texte inséré, sauf si déjà couverte. */
    private static void poserInseree(List<Token> resultat,
            TokenKind categorie, int debut, int longueur) {
        if (longueur <= 0 || !resultat.isEmpty()
                && resultat.get(resultat.size() - 1).end() > debut) {
            return;
        }
        resultat.add(new Token(categorie, debut, debut + longueur));
    }

    /**
     * Le jeton à cheval sur la plage remplacée : son morceau gauche
     * reste, l'insertion hérite de sa catégorie, son morceau droit se
     * décale.
     */
    private static void recalerACheval(Token jeton, int debut,
            int finRemplacement, int delta, TokenKind categorieInseree,
            int longueurInseree, List<Token> resultat) {
        if (jeton.start() < debut) {
            resultat.add(new Token(jeton.kind(), jeton.start(), debut));
        }
        poserInseree(resultat, jeton.kind() != TokenKind.IDENTIFIER
                ? jeton.kind() : categorieInseree, debut, longueurInseree);
        if (jeton.end() > finRemplacement) {
            resultat.add(new Token(jeton.kind(),
                    finRemplacement + delta, jeton.end() + delta));
        }
    }
}
