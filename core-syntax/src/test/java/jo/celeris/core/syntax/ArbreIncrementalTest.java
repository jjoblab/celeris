package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;

/**
 * L'arbre tolérant et sa maintenance incrémentale (ADR-004) : les chunks de
 * haut niveau sont ancrés par (ligne, colonne), leurs sous-arbres portent
 * des offsets relatifs au début du chunk — une édition ne ré-analyse que
 * les chunks touchés, les autres conservent leur sous-arbre à l'identité.
 * Le parseur ne lève jamais d'exception sur un source malformé : la
 * malformation devient un nœud d'erreur.
 */
class ArbreIncrementalTest {

    private static final String TEXTE = "fn premier {\n"
            + "  corps\n"
            + "}\n"
            + "var a ;\n"
            + "\n"
            + "fn deuxieme {\n"
            + "  x\n"
            + "}\n"
            + "fn troisieme { y }\n";

    private final SyntaxService service = SyntaxService.of(MiniLangage.langage());

    /** L'analyse complète découpe les chunks et ancre chaque sous-arbre. */
    @Test
    void analyseCompleteDecoupeEtAncre() {
        Document doc = Document.of(TEXTE);
        Analysis analyse = service.analyseFull(doc);
        assertEquals(doc.revision(), analyse.revision());
        SyntaxTree arbre = analyse.tree();
        assertEquals(4, arbre.chunks().size());
        Chunk premier = arbre.chunks().get(0);
        assertEquals(0, premier.startLine());
        assertEquals(0, premier.startCol());
        assertEquals(2, premier.endLine());
        assertEquals(NodeCategory.FUNCTION, premier.root().category());
        assertEquals("premier", premier.root().outlineName());
        assertFalse(premier.isError());

        Chunk champ = arbre.chunks().get(1);
        assertEquals(3, champ.startLine());
        assertEquals(NodeCategory.FIELD, champ.root().category());
        assertEquals("a", champ.root().outlineName());

        Chunk deuxieme = arbre.chunks().get(2);
        assertEquals(6, deuxieme.startLine());

        int debutDeuxieme = arbre.chunkStartOffset(doc, deuxieme);
        assertEquals(doc.lineStart(6), debutDeuxieme);
        assertTrue(debutDeuxieme < arbre.chunkEndOffset(doc, deuxieme));
    }

    /** Une édition dans un chunk ne ré-analyse que lui : les autres gardent leur instance. */
    @Test
    void editionDansUnChunkNeReanalyseQueLui() {
        Document doc = Document.of(TEXTE);
        Analysis complet = service.analyseFull(doc);
        int cible = TEXTE.indexOf("  x");
        Document edite = doc.replace(cible, cible + 3, "  omega");
        Analysis diff = service.analyseIncremental(edite, complet,
                EditSite.of(doc, edite, cible, cible + 3, "  omega"));

        assertEquals(4, diff.tree().chunks().size());
        assertSame(complet.tree().chunks().get(0), diff.tree().chunks().get(0),
                "le chunk 0 garde son instance");
        assertSame(complet.tree().chunks().get(1), diff.tree().chunks().get(1),
                "le chunk 1 garde son instance");
        assertSame(complet.tree().chunks().get(3), diff.tree().chunks().get(3),
                "le chunk 3 garde son instance");
        assertNotSame(complet.tree().chunks().get(2), diff.tree().chunks().get(2),
                "le chunk édité est ré-analysé");
        assertEquals("deuxieme", diff.tree().chunks().get(2).root().outlineName());
    }

    /** Un décalage de lignes décale les ancres des chunks suivants, sous-arbres conservés. */
    @Test
    void decalageDeLignesDecaleLesAncres() {
        Document doc = Document.of(TEXTE);
        Analysis complet = service.analyseFull(doc);
        int cible = TEXTE.indexOf("  x");
        Document edite = doc.replace(cible, cible, "  nouveau\n  encore\n");
        Analysis diff = service.analyseIncremental(edite, complet,
                EditSite.of(doc, edite, cible, cible, "  nouveau\n  encore\n"));

        Chunk troisiemeAvant = complet.tree().chunks().get(3);
        Chunk troisiemeApres = diff.tree().chunks().get(3);
        assertSame(troisiemeAvant, troisiemeApres,
                "le chunk après le décalage garde son sous-arbre");
        assertEquals(troisiemeAvant.startLine() + 2, troisiemeApres.startLine());
        assertEquals(troisiemeAvant.endLine() + 2, troisiemeApres.endLine());
        assertEquals(troisiemeAvant.startCol(), troisiemeApres.startCol());
        assertEquals(doc.lineStart(troisiemeApres.startLine()),
                diff.tree().chunkStartOffset(edite, troisiemeApres));
    }

    /** Une édition sur une ligne intercalée ré-analyse le chunk propriétaire. */
    @Test
    void editionSurLigneIntercalaireReanalyseLeProprietaire() {
        Document doc = Document.of(TEXTE);
        Analysis complet = service.analyseFull(doc);
        int ligneVide = doc.lineStart(4);
        Document edite = doc.replace(ligneVide, ligneVide, "note");
        Analysis diff = service.analyseIncremental(edite, complet,
                EditSite.of(doc, edite, ligneVide, ligneVide, "note"));

        assertSame(complet.tree().chunks().get(0), diff.tree().chunks().get(0));
        assertSame(complet.tree().chunks().get(2), diff.tree().chunks().get(2));
        assertSame(complet.tree().chunks().get(3), diff.tree().chunks().get(3));
        assertNotSame(complet.tree().chunks().get(1), diff.tree().chunks().get(1),
                "la ligne intercalaire appartient au chunk précédent, qui se ré-analyse");
    }

    /** Insérer une déclaration entre deux chunks en crée un nouveau. */
    @Test
    void insertionDeDeclarationCreeUnChunk() {
        Document doc = Document.of(TEXTE);
        Analysis complet = service.analyseFull(doc);
        int ligneVide = doc.lineStart(4);
        Document edite = doc.replace(ligneVide, ligneVide, "fn neuf { }");
        Analysis diff = service.analyseIncremental(edite, complet,
                EditSite.of(doc, edite, ligneVide, ligneVide, "fn neuf { }"));

        assertEquals(5, diff.tree().chunks().size());
        assertSame(complet.tree().chunks().get(0), diff.tree().chunks().get(0));
        assertSame(complet.tree().chunks().get(3), diff.tree().chunks().get(4));
        Chunk neuf = diff.tree().chunks().get(2);
        assertEquals(4, neuf.startLine());
        assertEquals(NodeCategory.FUNCTION, neuf.root().category());
        assertEquals("neuf", neuf.root().outlineName());
    }

    /** Un crochet non fermé devient un chunk en erreur, jamais une exception. */
    @Test
    void crochetNonFermeDevientErreurPasException() {
        Document doc = Document.of(TEXTE);
        Analysis complet = service.analyseFull(doc);
        int fermeture = TEXTE.lastIndexOf('}');
        Document edite = doc.replace(fermeture, fermeture + 1, "");
        Analysis diff = service.analyseIncremental(edite, complet,
                EditSite.of(doc, edite, fermeture, fermeture + 1, ""));

        Chunk casse = diff.tree().chunks().get(3);
        assertTrue(casse.isError(), "le chunk malformé porte l'erreur");
        assertTrue(diff.tree().chunks().get(3).root().isError());
        assertFalse(diff.tree().chunks().get(0).isError());
    }

    /** Un document vide donne un arbre vide sans incident. */
    @Test
    void documentVideDonneArbreVide() {
        Analysis analyse = service.analyseFull(Document.of(""));
        assertTrue(analyse.tree().chunks().isEmpty());
        assertEquals(0, analyse.lex().line(0).tokens().length);
    }

    /** L'incrémental produit le même arbre qu'une analyse complète. */
    @Test
    void incrementEgalCompletSurLArbre() {
        Document doc = Document.of(TEXTE);
        Analysis complet = service.analyseFull(doc);
        int cible = TEXTE.indexOf("corps");
        Document edite = doc.replace(cible, cible + 5, "alpha beta");
        Analysis diff = service.analyseIncremental(edite, complet,
                EditSite.of(doc, edite, cible, cible + 5, "alpha beta"));
        Analysis temoin = service.analyseFull(edite);
        Arbres.verifierEquivalence(diff.tree(), temoin.tree(), edite);
    }
}
