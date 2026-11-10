package jo.celeris.android.view;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.syntax.LexDocument;
import jo.celeris.core.syntax.LexedLine;
import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;

import org.jspecify.annotations.Nullable;

/**
 * Source de rangées colorées (B-01) : le décorateur greffe sur chaque
 * mise en page les plages de sa ligne — le rendu coloré lit les paniers
 * du même arbre lexicographique que les plis et les diagnostics
 * (ADR-004 : un seul arbre, tous les consommateurs).
 *
 * <p>Les plages couvrent la tranche de la rangée EXACTEMENT, sans trou
 * à peindre : le lexeur soude les jetons (les blancs appartiennent au
 * jeton qui les suit) et tout écart résiduel se comble à
 * l'identificateur — l'encre de base par défaut. Une rangée vide reste
 * sans plage : le chemin rapide du peintre.</p>
 *
 * <p>Deux habillages : à jour, la ligne de la rangée se lit directement
 * dans le lexique ; périmé (l'analyse publiée date d'avant les éditions
 * en attente), le rattrapage {@link TokenCatchUp} sert les jetons de la
 * ligne publiée RECALÉS sur l'édition en attente — la ligne touchée
 * garde ses couleurs, elle ne retombe plus à l'encre de base (PIEGE-26,
 * stale-while-revalidate).</p>
 *
 * <p>Pure et testable sans SDK : le document lexicographié est injecté,
 * immuable ; l'époque syntaxique est signée par la reconstruction de la
 * source à chaque changement de langage — la révision du document signe
 * le reste (PIEGE-02).</p>
 */
public final class SyntaxRowSource implements RowSource {

    private final RowSource deleguee;
    private final LexDocument lexique;
    private final @Nullable TokenCatchUp remappeur;

    /**
     * Construit la source colorée sur une source déléguée.
     *
     * @param deleguee la source des mises en page non colorées
     * @param lexique  le document lexicographié de l'analyse courante
     */
    public SyntaxRowSource(RowSource deleguee, LexDocument lexique) {
        this(deleguee, lexique, null);
    }

    /**
     * Construit la source colorée rattrapée sur une publication
     * périmée : chaque rangée se pare des jetons de sa ligne, servie
     * par le rattrapeur — recalés sur l'édition en attente pour la
     * ligne touchée, tels quels pour les lignes épargnées.
     *
     * @param deleguee  la source des mises en page non colorées
     * @param lexique   le document lexicographié de la publication
     * @param remappeur le rattrapage des jetons sur l'édition en
     *                  attente, null pour l'habillage à jour
     */
    public SyntaxRowSource(RowSource deleguee, LexDocument lexique,
            @Nullable TokenCatchUp remappeur) {
        this.deleguee = deleguee;
        this.lexique = lexique;
        this.remappeur = remappeur;
    }

    @Override
    public RowLayout layoutOf(RowKey key) {
        RowLayout base = deleguee.layoutOf(key);
        List<ColoredSpan> plages = plagesDe(base);
        if (plages.isEmpty()) {
            return base;
        }
        return new RowLayout(base.line(), base.start(), base.end(), base.text(),
                base.width(), base.runs(), plages);
    }

    /**
     * RE-SPAM la mise en page donnée des plages de l'habillage courant
     * : la mesure survit — même instance quand les plages n'ont pas
     * changé, nouvelle instance (même mesure, plages neuves) sinon
     * (PIEGE-26 : une publication ne rejette pas les mesures).
     *
     * @param base la mise en page en cache
     * @return la mise en page à jour — la même si rien n'a changé
     */
    public RowLayout respan(RowLayout base) {
        List<ColoredSpan> plages = plagesDe(base);
        if (plages.equals(base.spans())) {
            return base;
        }
        return new RowLayout(base.line(), base.start(), base.end(), base.text(),
                base.width(), base.runs(), plages);
    }

    /**
     * Les plages colorées de la tranche d'une rangée : les jetons de sa
     * ligne, rognés aux bornes de la tranche, les trous comblés à
     * l'identificateur. Les jetons qui portent une PILE de portées la
     * gardent dans leur plage (H-07) — la couleur s'y résout par le
     * thème, la sorte reste le repli.
     */
    private List<ColoredSpan> plagesDe(RowLayout base) {
        if (base.start() >= base.end()) {
            return List.of();
        }
        LexedLine panier = panierDe(base);
        if (panier == null) {
            // Le rattrapage n'a rien à servir pour cette ligne (ligne
            // neuve sans lexeur) : encre de base — le seul cas restant.
            return List.of();
        }
        Token[] jetons = panier.tokens();
        List<ColoredSpan> plages = new ArrayList<>();
        int curseur = base.start();
        for (Token jeton : jetons) {
            if (jeton.end() <= curseur) {
                continue;
            }
            if (jeton.start() >= base.end()) {
                break;
            }
            if (jeton.start() > curseur) {
                plages.add(new ColoredSpan(TokenKind.IDENTIFIER,
                        curseur - base.start(), jeton.start() - base.start()));
            }
            int debutRogne = Math.max(jeton.start(), curseur);
            int finRogne = Math.min(jeton.end(), base.end());
            if (finRogne > debutRogne) {
                plages.add(new ColoredSpan(jeton.kind(),
                        debutRogne - base.start(), finRogne - base.start(),
                        jeton.scope()));
            }
            curseur = Math.max(curseur, jeton.end());
            if (curseur >= base.end()) {
                break;
            }
        }
        if (curseur < base.end()) {
            plages.add(new ColoredSpan(TokenKind.IDENTIFIER,
                    curseur - base.start(), base.end() - base.start()));
        }
        return plages;
    }

    /**
     * Le panier de jetons de la ligne d'une rangée : le sien à jour, ou
     * celui que sert le rattrapage — jetons publiés recalés pour la
     * ligne touchée, panier publié pour les lignes épargnées.
     */
    private @Nullable LexedLine panierDe(RowLayout base) {
        TokenCatchUp remap = remappeur;
        if (remap == null) {
            return lexique.line(base.line());
        }
        return remap.basketFor(base.line());
    }
}
