package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.syntax.structure.TreeOutline;
import jo.celeris.core.text.Document;

/**
 * Un fichier du projet des hiérarchies (B-12) : son uri, son document,
 * son analyse — les entrées de types, de méthodes et les jetons absolus
 * s'y chargent à la demande, une fois par lecture.
 */
final class FichierProjet {

    private final String uri;

    private final Document doc;

    private final Analysis analyse;

    private @Nullable List<OutlineEntry> types;

    private @Nullable List<OutlineEntry> methodes;

    private @Nullable List<FlowAnalysis.AbsoluteToken> jetons;

    FichierProjet(String uri, Document doc, Analysis analyse) {
        this.uri = uri;
        this.doc = doc;
        this.analyse = analyse;
    }

    /** L'uri du fichier dans le projet. */
    String uri() {
        return uri;
    }

    /** Le document porteur. */
    Document doc() {
        return doc;
    }

    /** L'analyse courante du document. */
    Analysis analyse() {
        return analyse;
    }

    /** Les entrées de types du plan, chargées à la demande. */
    List<OutlineEntry> types() {
        List<OutlineEntry> charge = types;
        if (charge == null) {
            charge = new ArrayList<>();
            for (OutlineEntry entree : TreeOutline.entries(doc, analyse)) {
                if (entree.category() == NodeCategory.TYPE) {
                    charge.add(entree);
                }
            }
            types = charge;
        }
        return charge;
    }

    /** Les entrées de méthodes du plan, chargées à la demande. */
    List<OutlineEntry> methodes() {
        List<OutlineEntry> charge = methodes;
        if (charge == null) {
            charge = new ArrayList<>();
            for (OutlineEntry entree : TreeOutline.entries(doc, analyse)) {
                if (entree.category() == NodeCategory.FUNCTION) {
                    charge.add(entree);
                }
            }
            methodes = charge;
        }
        return charge;
    }

    /** Les jetons absolus du document, chargés à la demande. */
    List<FlowAnalysis.AbsoluteToken> jetons() {
        List<FlowAnalysis.AbsoluteToken> charge = jetons;
        if (charge == null) {
            charge = FlowAnalysis.tokens(doc, analyse.lex(), 0, doc.length());
            jetons = charge;
        }
        return charge;
    }
}
