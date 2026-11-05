package jo.celeris.android.view;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.syntax.LexDocument;
import jo.celeris.core.syntax.LexedLine;
import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;

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
 * <p>Pure et testable sans SDK : le document lexicographié est injecté,
 * immuable ; l'époque syntaxique est signée par la reconstruction de la
 * source à chaque changement de langage — la révision du document signe
 * le reste (PIEGE-02).</p>
 */
public final class SyntaxRowSource implements RowSource {

    private final RowSource deleguee;
    private final LexDocument lexique;

    /**
     * Construit la source colorée sur une source déléguée.
     *
     * @param deleguee la source des mises en page non colorées
     * @param lexique  le document lexicographié de l'analyse courante
     */
    public SyntaxRowSource(RowSource deleguee, LexDocument lexique) {
        this.deleguee = deleguee;
        this.lexique = lexique;
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
     * Les plages colorées de la tranche d'une rangée : les jetons de sa
     * ligne, rognés aux bornes de la tranche, les trous comblés à
     * l'identificateur.
     */
    private List<ColoredSpan> plagesDe(RowLayout base) {
        if (base.start() >= base.end()) {
            return List.of();
        }
        LexedLine panier = lexique.line(base.line());
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
                        debutRogne - base.start(), finRogne - base.start()));
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
}
