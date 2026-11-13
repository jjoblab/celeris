package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.LexDocument;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;

/**
 * Les rangées composées de pli (le constat du propriétaire du
 * 2026-11-13 : « il devrait afficher les {...} lorsque c'est plié ») :
 * la ligne d'en-tête repliée se rend comme préfixe + placeholder +
 * suffixe — l'accolade ouvrante ouvre la chip, la fermante la ferme,
 * le commentaire de bloc porte la sienne, une rangée non repliée passe
 * telle quelle. La pièce porte la géométrie que la couche de fond
 * relit à l'image.
 */
class FoldedRowSourceTest {

    /** Le corps d'essai : une classe, sa javadoc, sa méthode. */
    private static final String CLASSE = """
            class A {
                /**
                 * doc
                 */
                void m() {
                    int x = 1;
                }
            }
            """;

    /** Exécuteur déterministe : tout tourne sur le fil appelant. */
    private static final Executor DIRECT = Runnable::run;

    /** Règle fixe : largeur 10 px, hauteur 20 px, ascendance 16, descendance 4. */
    private static final class RegleFixe implements FontRuler {
        @Override
        public float charWidth(FontSpec spec) {
            return 10f;
        }

        @Override
        public float lineHeight(FontSpec spec) {
            return 20f;
        }

        @Override
        public float ascent(FontSpec spec) {
            return 16f;
        }

        @Override
        public float descent(FontSpec spec) {
            return 4f;
        }
    }

    /**
     * Le pont habillé prêt : session attachée, langage Java branché,
     * analyse publiée, pli de l'en-tête replié par ses régions — la
     * source retournée est la décoration de plis sur la source du
     * document.
     */
    private static FoldedRowSource publier(String contenu, int enTete) {
        EditorSyntax syntaxe = new EditorSyntax(new FoldModel(), DIRECT);
        TextSession session = TextSession.of(contenu, () -> 0L);
        syntaxe.attacher(session);
        syntaxe.setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        EditorMetrics metrics =
                EditorMetrics.of(new FontSpec("mono", 14f), new RegleFixe());
        DocumentRowSource source = new DocumentRowSource(session.document(),
                new VisualProjection(session.document(), new FoldModel(), 0),
                metrics);
        syntaxe.habiller(source, session.document());
        if (enTete >= 0) {
            syntaxe.basculerPli(enTete);
        }
        return (FoldedRowSource) syntaxe.habiller(source, session.document());
    }

    /** La pièce d'une classe repliée : l'accolade ouvre à sa colonne. */
    @Test
    void classeRepliee_pieceAAccolades() {
        Document document = TextSession.of(CLASSE, () -> 0L).document();
        FoldedRowSource.PieceDePli piece = FoldedRowSource.pieceDe(document,
                lexicalDe(document), plisDe(0, 7), 0, 7);
        assertEquals(8, piece.colonne());
        assertEquals(1, piece.colonneFin());
        assertEquals("{…}", piece.texte());
    }

    /** La pièce d'une javadoc repliée : le commentaire ouvre à sa colonne. */
    @Test
    void javadocRepliee_pieceDeCommentaire() {
        Document document = TextSession.of(CLASSE, () -> 0L).document();
        FoldedRowSource.PieceDePli piece = FoldedRowSource.pieceDe(document,
                lexicalDe(document), plisDe(1, 3), 1, 3);
        assertEquals(4, piece.colonne());
        assertEquals(7, piece.colonneFin());
        assertEquals("/*…*/", piece.texte());
    }

    /** Sans pli, la pièce rend null — la couche de fond reste muette. */
    @Test
    void sansPli_pieceNulle() {
        Document document = TextSession.of(CLASSE, () -> 0L).document();
        assertNull(FoldedRowSource.pieceDe(document, lexicalDe(document),
                new FoldModel(), 0, 7));
    }

    /** Le texte composite : préfixe + « {…} » + suffixe, à la largeur rendue. */
    @Test
    void composite_portePrefixePlaceholderSuffixe() {
        FoldedRowSource source = publier(CLASSE, 0);
        RowLayout composite = source.layoutOf(cle());
        assertEquals("class A {…}", composite.text());
        assertEquals(0, composite.start());
        assertEquals(11, composite.end());
        assertEquals(11 * 10f, composite.width(), 1e-6f);
        assertEquals(0, composite.line());
        // la chip vit à l'encre de documentation, la couverture sans trou
        assertEquals(TokenKind.DOC, composite.spans()
                .get(indiceDe(composite, 8)).kind());
        assertEquals(composite.text().length(), composite.spans()
                .get(composite.spans().size() - 1).end());
    }

    /** La javadoc repliée compose son commentaire à sa colonne. */
    @Test
    void composite_javadocAEtSaColonne() {
        FoldedRowSource source = publier(CLASSE, 1);
        RowLayout composite = source.layoutOf(new RowKey(1L, 1, 1,
                new FontSpec("mono", 14f), 0));
        assertEquals("    /*…*/", composite.text());
        assertEquals(9 * 10f, composite.width(), 1e-6f);
    }

    /** Une rangée non repliée passe telle quelle : sa ligne, son texte, sa tranche. */
    @Test
    void rangeeNonRepliee_passeTelleQuelle() {
        FoldedRowSource source = publier(CLASSE, 0);
        RowLayout rendue = source.layoutOf(cleDe(4));
        assertEquals("    void m() {", rendue.text());
        assertEquals(4, rendue.line());
        assertEquals(0, rendue.start());
        assertEquals("    void m() {".length(), rendue.end());
    }

    /** Le document lexicographié par le lexeur Java, à l'analyse pleine. */
    private static LexDocument lexicalDe(Document document) {
        return jo.celeris.core.syntax.SyntaxService
                .of(JavaSyntax.INSTANCE).analyseFull(document).lex();
    }

    /** Le modèle de plis replié de l'en-tête donné à sa fin. */
    private static FoldModel plisDe(int debut, int fin) {
        FoldModel plis = new FoldModel();
        plis.fold(debut, fin);
        return plis;
    }

    /** La clé de la première rangée du document d'essai. */
    private static RowKey cle() {
        return cleDe(0);
    }

    /** La clé de la rangée donnée du document d'essai. */
    private static RowKey cleDe(int rangee) {
        return new RowKey(1L, rangee, 1, new FontSpec("mono", 14f), 0);
    }

    /** L'indice de la plage qui couvre la colonne donnée, —1 si vide. */
    private static int indiceDe(RowLayout composite, int colonne) {
        for (int i = 0; i < composite.spans().size(); i++) {
            ColoredSpan plage = composite.spans().get(i);
            if (plage.start() <= colonne && plage.end() > colonne) {
                return i;
            }
        }
        return -1;
    }
}
