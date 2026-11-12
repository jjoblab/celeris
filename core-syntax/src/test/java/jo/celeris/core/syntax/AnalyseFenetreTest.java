package jo.celeris.core.syntax;

import jo.celeris.core.syntax.structure.FoldSpan;
import jo.celeris.core.syntax.structure.TreeDiagnostics;
import jo.celeris.core.syntax.structure.TreeFolds;
import jo.celeris.core.syntax.structure.TreeOutline;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;

/**
 * L'analyse par fenêtre (G-01, ADR-004) : l'ouverture progressive d'un
 * grand document paie d'abord la fenêtre visible — les chunks hors
 * fenêtre restent en attente, inertes pour plis, plan et diagnostics ;
 * la complétion les analyse par lots sans retoucher les sous-arbres
 * déjà construits ; l'édition incrémentale part d'un arbre partiel
 * comme d'un complet.
 */
class AnalyseFenetreTest {

    private static final String TEXTE = """
            fn a {
              x
            }
            var b ;
            fn c {
              y
            }
            """;

    private final SyntaxService service = SyntaxService.of(MiniLangage.langage());

    /**
     * La fenêtre n'analyse que les chunks qu'elle coupe : le premier
     * est construit, les suivants restent en attente — l'arbre est
     * partiel mais couvre déjà tout le document.
     */
    @Test
    void fenetreAnalysee_leResteEnAttente() {
        Document doc = Document.of(TEXTE);
        Analysis partielle = service.analyseFenetre(doc, 0, 1, new JetonAnnulation());
        assertFalse(partielle.estComplet());
        List<Chunk> chunks = partielle.tree().chunks();
        assertEquals(3, chunks.size());
        assertFalse(chunks.get(0).estEnAttente(), "le chunk de la fenêtre est analysé");
        assertTrue(chunks.get(1).estEnAttente(), "le chunk hors fenêtre attend");
        assertTrue(chunks.get(2).estEnAttente(), "le chunk hors fenêtre attend");
    }

    /** Les plis ne viennent que des chunks analysés : la fenêtre seule se sert. */
    @Test
    void plisSeulementDesChunksAnalyses() {
        Document doc = Document.of(TEXTE);
        Analysis partielle = service.analyseFenetre(doc, 0, 1, new JetonAnnulation());
        List<FoldSpan> plisPartiels = TreeFolds.regions(doc, partielle);
        List<FoldSpan> plisComplets = TreeFolds.regions(doc, service.analyseFull(doc));
        assertFalse(plisComplets.isEmpty(), "le langage d'essai plie ses blocs");
        assertTrue(plisPartiels.size() < plisComplets.size(),
                "l'arbre partiel plie moins que le complet");
        assertTrue(plisComplets.containsAll(plisPartiels),
                "les plis partiels sont exactement les premiers plis du complet");
    }

    /** La complétion termine l'arbre : plis et plan égaux au complet. */
    @Test
    void completionsTerminentArbre_plisEgauxAuComplet() {
        Document doc = Document.of(TEXTE);
        Analysis courant = service.analyseFenetre(doc, 0, 1, new JetonAnnulation());
        while (!courant.estComplet()) {
            courant = service.completer(doc, courant, new JetonAnnulation(), 1);
        }
        Analysis complet = service.analyseFull(doc);
        assertEquals(TreeFolds.regions(doc, complet), TreeFolds.regions(doc, courant));
        assertEquals(TreeOutline.entries(doc, complet), TreeOutline.entries(doc, courant));
        Arbres.verifierEquivalence(courant.tree(), complet.tree(), doc);
    }

    /** Un chunk analysé garde son sous-arbre à l'identité à chaque étape. */
    @Test
    void identiteDesSousArbresAuxEtapes() {
        Document doc = Document.of(TEXTE);
        Analysis courant = service.analyseFenetre(doc, 0, 1, new JetonAnnulation());
        SynNode racinePremier = courant.tree().chunks().get(0).root();
        while (!courant.estComplet()) {
            courant = service.completer(doc, courant, new JetonAnnulation(), 1);
        }
        assertSame(racinePremier, courant.tree().chunks().get(0).root(),
                "la complétion ne retouche pas le sous-arbre déjà construit");
    }

    /**
     * L'édition dans la fenêtre analysée suit la voie incrémentale depuis
     * un arbre partiel : le chunk propriétaire se ré-analyse, les chunks
     * en attente se décalent et restent en attente — la complétion
     * rejoint l'analyse complète du même instantané.
     */
    @Test
    void editionDansFenetre_analyseIncrementaleDepuisPartiel() {
        Document doc = Document.of(TEXTE);
        Analysis partielle = service.analyseFenetre(doc, 0, 1, new JetonAnnulation());
        Document edite = doc.replace(TEXTE.indexOf("x"), TEXTE.indexOf("x") + 1, "z");
        EditSite site = EditSite.of(doc, edite,
                TEXTE.indexOf("x"), TEXTE.indexOf("x") + 1, "z");
        Analysis diff = service.analyseIncremental(edite, partielle, site);
        assertTrue(diff.tree().chunks().size() >= 3);
        assertFalse(diff.tree().chunks().get(0).estEnAttente(),
                "le chunk propriétaire édité se ré-analyse");
        assertTrue(diff.tree().chunks().get(1).estEnAttente(),
                "les chunks hors fenêtre restent en attente");
        assertFalse(diff.estComplet());
        Analysis courant = diff;
        while (!courant.estComplet()) {
            courant = service.completer(edite, courant, new JetonAnnulation(), 1);
        }
        Arbres.verifierEquivalence(courant.tree(),
                service.analyseFull(edite).tree(), edite);
    }

    /** Un malformé hors fenêtre ne diagnostique qu'une fois analysé. */
    @Test
    void diagnosticsSeulementDesChunksAnalyses() {
        String source = """
                fn a {
                  x
                }
                fn cassee {
                  y
                """;
        Document doc = Document.of(source);
        Analysis partielle = service.analyseFenetre(doc, 0, 2, new JetonAnnulation());
        assertTrue(TreeDiagnostics.issues(doc, partielle).isEmpty(),
                "le chunk en attente ne diagnostique rien");
        Analysis courant = partielle;
        while (!courant.estComplet()) {
            courant = service.completer(doc, courant, new JetonAnnulation(), 1);
        }
        assertFalse(TreeDiagnostics.issues(doc, courant).isEmpty(),
                "le malformé se diagnostique une fois analysé");
    }
}
