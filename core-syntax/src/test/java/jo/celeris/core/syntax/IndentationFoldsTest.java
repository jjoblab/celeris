package jo.celeris.core.syntax;

import jo.celeris.core.syntax.structure.FoldSpan;
import jo.celeris.core.syntax.structure.IndentationFolds;
import jo.celeris.core.syntax.structure.TreeFolds;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.Document;

/**
 * Les plis par indentation (M7.6, H-08 — niveau 1 de l'analyse
 * structurelle, ADR-016) : une ligne moins indentée rouvre le pli
 * (offSide, le réglage Python), les lignes vides et de commentaire
 * seul ne clôturent jamais, les tabulations comptent au taquet, les
 * blancs de queue ne se replient pas, une région couvre deux lignes
 * au moins — et sans offSide, les blocs frères se fondent.
 */
class IndentationFoldsTest {

    /** L'état neutre des lignes construites à la main. */
    private enum Neutre implements LexState {
        /** Hors de toute construction multi-lignes. */
        NORMAL
    }

    private static LexedLine code() {
        return new LexedLine(new Token[] {new Token(TokenKind.IDENTIFIER, 0, 3)},
                Neutre.NORMAL);
    }

    private static LexedLine comment() {
        return new LexedLine(new Token[] {new Token(TokenKind.COMMENT, 0, 3)},
                Neutre.NORMAL);
    }

    private static LexedLine vide() {
        return new LexedLine(new Token[0], Neutre.NORMAL);
    }

    private static LexDocument lex(LexedLine... lignes) {
        return new LexDocument(lignes, new int[lignes.length], Neutre.NORMAL);
    }

    private static List<FoldSpan> plis(String source, boolean offSide) {
        Document doc = Document.of(source);
        String[] lignes = source.split("\n", -1);
        LexedLine[] lexees = new LexedLine[lignes.length];
        for (int i = 0; i < lignes.length; i++) {
            String ligne = lignes[i];
            lexees[i] = ligne.isBlank() || ligne.strip().startsWith("#")
                    ? (ligne.isEmpty() ? vide() : comment())
                    : code();
        }
        return IndentationFolds.regions(doc, lex(lexees),
                new IndentationFolds.Settings(4, offSide));
    }

    /** Le corps Python : chaque déclaration replie son bloc, les frères se rouvrent. */
    @Test
    void corpsPythonOffSide() {
        String source = """
                # Outils d'outillage : comptage de lignes par catégorie.
                CATEGORIES = {
                    "code": 0,
                }

                def classifier(ligne):
                    if not ligne.strip():
                        return "vide"
                    if ligne.lstrip().startswith("#"):
                        return "commentaire"
                    return "code"

                def compter(texte):
                    for ligne in texte.splitlines():
                        CATEGORIES[classifier(ligne)] += 1
                    return dict(CATEGORIES)
                """;
        List<FoldSpan> regions = plis(source, true);
        assertEquals(List.of(
                new FoldSpan(1, 2),
                new FoldSpan(5, 10),
                new FoldSpan(6, 7),
                new FoldSpan(8, 9),
                new FoldSpan(12, 15),
                new FoldSpan(13, 14)), regions);
    }

    /** Sans offSide, une ligne de même décalage prolonge le pli : les frères se fondent. */
    @Test
    void freresFondusSansOffSide() {
        String source = """
                a:
                  b
                c:
                  d
                """;
        List<FoldSpan> regions = plis(source, false);
        assertEquals(List.of(new FoldSpan(0, 3)), regions);
    }

    /** Les lignes vides et de commentaire seul ne clôturent jamais un pli. */
    @Test
    void videsEtCommentairesNeClourentPas() {
        String source = """
                def f():
                    corps

                    # note du corps
                    suite
                """;
        List<FoldSpan> regions = plis(source, true);
        assertEquals(List.of(new FoldSpan(0, 4)), regions);
    }

    /** Les tabulations comptent au taquet : le mélange se compare en largeur. */
    @Test
    void tabulationsAuTaquet() {
        String source = "a:\n\tb\n  c\n";
        List<FoldSpan> regions = plis(source, true);
        assertEquals(List.of(new FoldSpan(0, 2)), regions);
    }

    /** Les blancs de queue ne se replient pas : la région finit au dernier contenu. */
    @Test
    void blancsDeQueueExclus() {
        String source = "a:\n  b\n\n\n";
        List<FoldSpan> regions = plis(source, true);
        assertEquals(List.of(new FoldSpan(0, 1)), regions);
    }

    /** Le réglage refuse le taquet nul : la donnée fausse se dit tout de suite. */
    @Test
    void taquetPositifExige() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new IndentationFolds.Settings(0, true));
    }

    /** La fusion de TreeFolds : l'arbre, les commentaires et l'indentation ensemble. */
    @Test
    void fusionAvecLArbre() {
        String source = """
                fn exterieur {
                    var a ;
                }

                fn autre {
                    var b ;
                }
                """;
        Document doc = Document.of(source);
        SyntaxService service = SyntaxService.of(MiniLangage.langage());
        Analysis analyse = service.analyseFull(doc);
        List<FoldSpan> regions = TreeFolds.regions(doc, analyse,
                new IndentationFolds.Settings(4, true));
        assertEquals(List.of(new FoldSpan(0, 2), new FoldSpan(4, 6)), regions,
                "l'arbre porte les blocs, l'indentation n'ajoute rien de parasite");
    }
}
