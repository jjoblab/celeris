package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;

import jo.celeris.core.language.LanguageConfiguration;
import jo.celeris.core.language.LanguageDescriptor;
import jo.celeris.core.language.LanguageSupport;
import jo.celeris.core.syntax.LanguageLexer;
import jo.celeris.core.syntax.LanguageParser;
import jo.celeris.core.syntax.LanguageSyntax;
import jo.celeris.core.syntax.LexState;
import jo.celeris.core.syntax.LexedLine;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.NodeKind;
import jo.celeris.core.syntax.OutlineEntry;
import jo.celeris.core.syntax.OutlineRule;
import jo.celeris.core.syntax.SynNode;
import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.syntax.TokenSlice;
import jo.celeris.core.text.TextSession;

/**
 * Le plan et les plis par données côté pont (M7.6, H-08) : un langage
 * sans analyseur dédié — la configuration déclare ses plis
 * d'indentation, le paquet porte ses règles de plan — reçoit ses
 * régions pliables à la gouttière et ses entrées au consommateur du
 * plan, prolongées sur leurs régions : Python, YAML et SQL se
 * structurent sans une ligne de Java dédiée, la vue n'y change rien.
 */
class EditorSyntaxDataPlanTest {

    /** Le corps d'essai : constantes, définitions, indentations. */
    private static final String SOURCE = """
            CATEGORIES = {
                "code": 0,
            }

            def classifier(ligne):
                if not ligne.strip():
                    return "vide"
                return "code"

            def compter(texte):
                return 1
            """;

    /** Une source qui ne rend rien : habiller l'enveloppe, jamais ne la consulte ici. */
    private static final RowSource SOURCE_MUETTE = cle -> {
        throw new UnsupportedOperationException("pas consultée par le pont");
    };

    /** Exécuteur déterministe : tout tourne sur le fil appelant. */
    private static final Executor DIRECT = Runnable::run;

    /** L'état neutre du langage d'essai. */
    private enum EtatEssai implements LexState {
        /** Hors de toute construction multi-lignes. */
        NORMAL
    }

    /** La sorte unique du langage d'essai. */
    private enum SorteEssai implements NodeKind {
        /** Trivia du langage d'essai. */
        AUTRE
    }

    /** Le langage d'essai des données : « # » commente, tout le reste est code. */
    private static final LanguageSyntax LANGAGE_ESSAI = new LanguageSyntax() {

        @Override
        public String id() {
            return "essai";
        }

        @Override
        public LanguageLexer lexer() {
            return new LanguageLexer() {

                @Override
                public LexState initialState() {
                    return EtatEssai.NORMAL;
                }

                @Override
                public LexedLine lex(CharSequence texte, LexState entree) {
                    String ligne = texte.toString();
                    if (ligne.isBlank()) {
                        return new LexedLine(new Token[0], EtatEssai.NORMAL);
                    }
                    if (ligne.strip().startsWith("#")) {
                        return new LexedLine(new Token[] {
                                new Token(TokenKind.COMMENT, 0, ligne.length()) },
                                EtatEssai.NORMAL);
                    }
                    return new LexedLine(
                            new Token[] {new Token(TokenKind.IDENTIFIER, 0, 1)},
                            EtatEssai.NORMAL);
                }
            };
        }

        @Override
        public LanguageParser parser() {
            return new LanguageParser() {

                @Override
                public int chunkLevel() {
                    return 0;
                }

                @Override
                public boolean startsChunk(TokenSlice slice, int index, int depth) {
                    return false;
                }

                @Override
                public SynNode parseChunk(TokenSlice slice) {
                    if (slice.size() == 0) {
                        return SynNode.of(SorteEssai.AUTRE, NodeCategory.OTHER, 0, 0,
                                new SynNode[0], null, false);
                    }
                    return SynNode.of(SorteEssai.AUTRE, NodeCategory.OTHER,
                            slice.start(0), slice.end(slice.size() - 1),
                            new SynNode[0], null, false);
                }
            };
        }
    };

    /** Le support du langage d'essai : plis par indentation, plan par règles. */
    private static LanguageSupport supportEssai() {
        return LanguageSupport
                .builder(new LanguageDescriptor("essai", "Essai", List.of(), List.of(),
                        List.of(), List.of(), null, List.of(), null, null, "Es",
                        0xFF82AAFF))
                .configuration(new LanguageConfiguration(
                        new LanguageConfiguration.Comments("# ", null, null),
                        List.of(), List.of(), List.of(), null,
                        new LanguageConfiguration.Folding(true, true, null, null),
                        new LanguageConfiguration.IndentRules(null, null, null, null),
                        4, false))
                .outlineRules(List.of(
                        OutlineRule.of("^\\s*def\\s+([A-Za-z_][A-Za-z0-9_]*)",
                                "function", 1),
                        OutlineRule.of("^([A-Za-z_][A-Za-z0-9_]*)\\s*=", "field", 1)))
                .syntax(LANGAGE_ESSAI)
                .build();
    }

    /** Le plan poussé vient des règles du paquet, prolongées à ses régions de pli. */
    @Test
    void planPousseParLesReglesDuPaquet() {
        List<List<OutlineEntry>> plans = new ArrayList<>();
        EditorSyntax syntaxe = new EditorSyntax(new FoldModel(), DIRECT);
        syntaxe.setSurPlan(plans::add);
        TextSession session = TextSession.of(SOURCE, () -> 0L);
        syntaxe.attacher(session);
        syntaxe.setLanguage(supportEssai());

        syntaxe.habiller(SOURCE_MUETTE, session.document());

        List<OutlineEntry> pousse = plans.get(plans.size() - 1);
        assertEquals(3, pousse.size(), "le plan des règles : " + pousse);
        assertEquals("CATEGORIES", pousse.get(0).name());
        assertEquals(NodeCategory.FIELD, pousse.get(0).category());
        assertEquals(0, pousse.get(0).startLine());
        assertEquals(1, pousse.get(0).endLine(), "l'entrée couvre sa région de pli");
        assertEquals("classifier", pousse.get(1).name());
        assertEquals(NodeCategory.FUNCTION, pousse.get(1).category());
        assertEquals(4, pousse.get(1).startLine());
        assertEquals(7, pousse.get(1).endLine(), "la définition couvre son bloc");
        assertEquals("compter", pousse.get(2).name());
        assertEquals(10, pousse.get(2).endLine());
        syntaxe.fermer();
    }

    /** Les régions pliables suivent l'indentation déclarée : la gouttière s'y retrouve. */
    @Test
    void gouttierePlieALIndentationDeclaree() {
        FoldModel plis = new FoldModel();
        EditorSyntax syntaxe = new EditorSyntax(plis, DIRECT);
        TextSession session = TextSession.of(SOURCE, () -> 0L);
        syntaxe.attacher(session);
        syntaxe.setLanguage(supportEssai());

        syntaxe.habiller(SOURCE_MUETTE, session.document());

        assertTrue(syntaxe.marques().pliable(0), "la constante se replie");
        assertTrue(syntaxe.marques().pliable(4), "la définition se replie");
        assertTrue(syntaxe.marques().pliable(9), "la seconde définition se replie");
        assertTrue(syntaxe.basculerPli(4), "l'appui dans la coulisse consomme");
        assertTrue(plis.plis().stream().anyMatch(p -> p.debut() == 4 && p.fin() == 7),
                "le bloc replié couvre les lignes 4-7 : " + plis.plis());
        syntaxe.fermer();
    }
}
