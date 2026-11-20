package jo.celeris.android.view;

import java.util.List;

/**
 * La source de rangées raffinée par la surcouche sémantique (ADR-0027,
 * B1) : chaque rangée habillée par la syntaxe passe au peigne des
 * jetons du serveur — les plages prouvées DES DEUX se coupent à
 * l'intersection, la sorte du jeton l'emporte sur la part couverte ;
 * jamais une plage ne s'ajoute (règle 1 : le jeton RAFFINE, il ne
 * remplace pas).
 *
 * <p>La fusion se paie ICI, une fois par rangée servie (le même
 * endroit que le rattrapage D-05) — jamais par image : la publication
 * sémantique repousse les rangées touchées au cache, le rendu lit des
 * mises en page toutes prêtes.</p>
 *
 * <p>La fraîcheur vit dans la surcouche elle-même (PIEGE-20) : elle
 * porte la révision que le serveur a vue et se vide sur une révision
 * plus récente — le recalage fin appartient à l'hôte, qui sait
 * l'édition.</p>
 */
public final class SemanticRowSource implements RowSource {

    private final RowSource deleguee;

    private final SemanticOverlay surcouche;

    /**
     * Construit la source raffinée.
     *
     * @param deleguee  la source habillée par la syntaxe
     * @param surcouche la surcouche des jetons du serveur
     */
    public SemanticRowSource(RowSource deleguee, SemanticOverlay surcouche) {
        this.deleguee = deleguee;
        this.surcouche = surcouche;
    }

    @Override
    public RowLayout layoutOf(RowKey key) {
        RowLayout base = deleguee.layoutOf(key);
        List<ColoredSpan> raffinees = surcouche.raffiner(base.spans(),
                base.start(), base.end());
        if (raffinees == base.spans()) {
            return base;
        }
        return new RowLayout(base.line(), base.start(), base.end(),
                base.text(), base.width(), base.runs(), raffinees);
    }

    /**
     * RE-PARE la mise en page donnée des plages raffinées : la mesure
     * survit (même instance sans changement, la même voie que le
     * respan syntaxique, PIEGE-26) — la publication sémantique ne
     * rejette pas les mesures plus que la syntaxique.
     *
     * @param base la mise en page en cache
     * @return la mise en page raffinée — la même si rien n'a changé
     */
    public RowLayout respan(RowLayout base) {
        List<ColoredSpan> raffinees = surcouche.raffiner(base.spans(),
                base.start(), base.end());
        if (raffinees.equals(base.spans())) {
            return base;
        }
        return new RowLayout(base.line(), base.start(), base.end(),
                base.text(), base.width(), base.runs(), raffinees);
    }
}
