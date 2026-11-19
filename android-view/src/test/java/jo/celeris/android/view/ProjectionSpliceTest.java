package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;

import jo.celeris.core.text.Document;

import org.junit.Test;

/**
 * L'épissure de la projection à l'édition (B-05, l'analyse croisée) :
 * la projection se reconstruisait ENTIÈRE à chaque frappe (longueurs,
 * indentations, sommes préfixées : quatre passages et deux allocations
 * int[n] par frappe). Le dépôt de départ épisse sur place. L'épissure
 * ici : la projection neuve naît de l'ancienne — les entrées d'avant
 * la ligne touchée se recopient, celles d'après se recalculent du
 * delta, jamais un balayage complet des longueurs.
 */
public class ProjectionSpliceTest {

    /** Un document de 6 lignes, la 3e longue sous wrap 10. */
    private static Document doc() {
        return Document.of("alpha\nbeta\n gamma delta epsilon\nzeta\n"
                + "eta theta iota\nkappa\n");
    }

    /** L'épissure égale la reconstruction fraîche : même nombre de
     *  rangées, mêmes débuts de tranche, mêmes indentations — la
     *  justesse de l'épissure se prouve par équivalence. */
    @Test
    public void lepineEgalLaReconstruction() {
        Document document = doc();
        VisualProjection avant = new VisualProjection(document,
                new FoldModel(), 10);
        // Édition : « zeta » devient « zeta lambda mu » (même ligne,
        // plus longue).
        Document edite = document.replace(32, 36, "zeta lambda mu");
        VisualProjection fraiche = new VisualProjection(edite,
                new FoldModel(), 10);
        VisualProjection epissee = avant.splice(edite, 3, 0);
        assertEquals(fraiche.rowCount(), epissee.rowCount());
        for (int rangee = 0; rangee < fraiche.rowCount(); rangee++) {
            assertEquals("tranche " + rangee,
                    fraiche.spanStartAtRow(rangee),
                    epissee.spanStartAtRow(rangee));
            assertEquals("indent " + rangee,
                    fraiche.indentAtRow(rangee),
                    epissee.indentAtRow(rangee));
        }
    }

    /** L'épissure d'une édition qui AJOUTE des lignes égale la
     *  reconstruction : les sommes préfixées décalent correctement. */
    @Test
    public void lepineAvecLignesAjouteesEgaleLaReconstruction() {
        Document document = doc();
        VisualProjection avant = new VisualProjection(document,
                new FoldModel(), 10);
        // « zeta » devient trois lignes.
        Document edite = document.replace(32, 36, "zeta\nlambda\nmu");
        VisualProjection fraiche = new VisualProjection(edite,
                new FoldModel(), 10);
        VisualProjection epissee = avant.splice(edite, 3, 2);
        assertEquals(fraiche.rowCount(), epissee.rowCount());
        for (int rangee = 0; rangee < fraiche.rowCount(); rangee++) {
            assertEquals("tranche " + rangee,
                    fraiche.spanStartAtRow(rangee),
                    epissee.spanStartAtRow(rangee));
        }
    }
}
