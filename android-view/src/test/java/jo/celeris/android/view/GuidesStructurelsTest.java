package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;

import jo.celeris.core.language.model.LanguageConfiguration;
import jo.celeris.core.language.model.LanguageDescriptor;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.LanguageLexer;
import jo.celeris.core.syntax.LanguageParser;
import jo.celeris.core.syntax.LanguageSyntax;
import jo.celeris.core.syntax.LexState;
import jo.celeris.core.syntax.LexedLine;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.NodeKind;
import jo.celeris.core.syntax.SynNode;
import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.syntax.TokenSlice;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;

/**
 * La table des guides structurels côté pont (H-09, A-17 relue) : les
 * accolades appariées — chaînes et commentaires ignorés — ouvrent les
 * guides des langages à crochets, les régions d'indentation celles des
 * langages à décalage ; l'édition remappe la table comme les plis, la
 * publication la redérive. Le banc pilote le pont sans la vue.
 */
class GuidesStructurelsTest {

    /** Le corps d'essai des langages à crochets. */
    private static final String CLASSE = """
            class A {
                void m() {
                    int x = 1;
                }
            }
            """;

    /** L'accolade dans une chaîne ou un commentaire n'ouvre rien. */
    private static final String PIEGES = """
            class A {
                String s = "{";
                // } dans un commentaire
            }
            """;

    /** Le corps d'essai des langages à décalage : définitions indentées. */
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

    /** Le support d'essai à décalage : plis par indentation, taquet de quatre. */
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
                .outlineRules(List.of())
                .syntax(LANGAGE_ESSAI)
                .build();
    }

    /** Le pont prêt : session attachée, langage branché, analyse habitée. */
    private static GuidesStructurels publier(String contenu, LanguageSupport langue) {
        EditorSyntax syntaxe = new EditorSyntax(new FoldModel(), DIRECT);
        TextSession session = TextSession.of(contenu, () -> 0L);
        syntaxe.attacher(session);
        syntaxe.setLanguage(langue);
        syntaxe.habiller(SOURCE_MUETTE, session.document());
        return syntaxe.guides;
    }

    /** Les accolades appariées ouvrent les guides : colonne de la
     *  ligne ouvrante, corps de l'ouvrante exclusive à la fermante
     *  exclusive, colonnes des crochets portées pour la coupure. */
    @Test
    void guidesDesAccoladesAppariees() {
        GuidesStructurels guides = publier(CLASSE,
                LanguageSupport.of(JavaSyntax.INSTANCE));
        assertEquals(2, guides.nombre());
        GuidesStructurels.Guide classe = guides.guide(0);
        assertEquals(0, classe.colonne());
        assertEquals(8, classe.colonneOuvrante());
        assertEquals(0, classe.ligneOuverture());
        assertEquals(0, classe.colonneFermante());
        assertEquals(4, classe.ligneApresFin());
        GuidesStructurels.Guide methode = guides.guide(1);
        assertEquals(4, methode.colonne());
        assertEquals(13, methode.colonneOuvrante());
        assertEquals(1, methode.ligneOuverture());
        assertEquals(4, methode.colonneFermante());
        assertEquals(3, methode.ligneApresFin());
    }

    /** L'accolade dans une chaîne ou un commentaire n'ouvre rien :
     *  seule la paire de la classe tient un guide. */
    @Test
    void laccoladeDansUneChaineOuUnCommentaireNouvreRien() {
        GuidesStructurels guides = publier(PIEGES,
                LanguageSupport.of(JavaSyntax.INSTANCE));
        assertEquals(1, guides.nombre());
        assertEquals(0, guides.guide(0).ligneOuverture());
        assertEquals(3, guides.guide(0).ligneApresFin());
    }

    /** L'accolade jamais fermée n'ouvre rien, celle de la même ligne
     *  non plus : rien à couvrir. */
    @Test
    void lesPairesSansCorpsNouvrentRien() {
        assertEquals(0, publier("class A {\n",
                LanguageSupport.of(JavaSyntax.INSTANCE)).nombre());
        assertEquals(0, publier("{ int a; }\n",
                LanguageSupport.of(JavaSyntax.INSTANCE)).nombre());
    }

    /** Les régions d'indentation portent les guides des langages à
     *  décalage : la colonne de l'en-tête, le corps entier — la
     *  définition, son if propre, la constante et la seconde
     *  définition, chacune sa région. */
    @Test
    void guidesDesRegionsDindentationDeclaree() {
        GuidesStructurels guides = publier(SOURCE, supportEssai());
        assertEquals(4, guides.nombre());
        assertEquals(0, guides.guide(0).colonne());
        assertEquals(0, guides.guide(0).ligneOuverture());
        assertEquals(2, guides.guide(0).ligneApresFin());
        assertEquals(4, guides.guide(1).ligneOuverture());
        assertEquals(8, guides.guide(1).ligneApresFin());
        assertEquals(5, guides.guide(2).ligneOuverture());
        assertEquals(7, guides.guide(2).ligneApresFin());
        assertEquals(9, guides.guide(3).ligneOuverture());
        assertEquals(11, guides.guide(3).ligneApresFin());
    }

    /** L'édition remappe la table : les bornes suivent le
     *  différentiel de lignes sans attendre la publication, puis la
     *  redérivation dit la même chose (la convergence). */
    @Test
    void leditionRemappeLesGuides() {
        EditorSyntax syntaxe = new EditorSyntax(new FoldModel(), DIRECT);
        TextSession session = TextSession.of(CLASSE, () -> 0L);
        syntaxe.attacher(session);
        syntaxe.setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        syntaxe.habiller(SOURCE_MUETTE, session.document());
        Document document = session.document();
        session.replaceRange(document.lineEnd(2), document.lineEnd(2), "\n");
        GuidesStructurels remappee = syntaxe.guides;
        assertEquals(session.document().revision(), remappee.revision(),
                "la table remappée vit à la révision neuve");
        assertEquals(2, remappee.nombre());
        assertEquals(1, remappee.guide(1).ligneOuverture());
        assertEquals(4, remappee.guide(1).ligneApresFin(),
                "la fermante de la méthode s'est décalée");
        assertEquals(5, remappee.guide(0).ligneApresFin());
        syntaxe.habiller(SOURCE_MUETTE, session.document());
        assertEquals(4, syntaxe.guides.guide(1).ligneApresFin(),
                "la redérivation reprend la même borne");
        assertEquals(5, syntaxe.guides.guide(0).ligneApresFin());
        syntaxe.fermer();
    }

    /** La ligne qui ouvre meurt : le guide meurt avec elle — le
     *  réapparimage tolérant raccorde ensuite la fermante orpheline. */
    @Test
    void leGuideMeurtAvecSaLigneOuverture() {
        EditorSyntax syntaxe = new EditorSyntax(new FoldModel(), DIRECT);
        TextSession session = TextSession.of(CLASSE, () -> 0L);
        syntaxe.attacher(session);
        syntaxe.setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        syntaxe.habiller(SOURCE_MUETTE, session.document());
        Document document = session.document();
        session.replaceRange(document.lineStart(1), document.lineStart(2), "");
        GuidesStructurels remappee = syntaxe.guides;
        assertEquals(1, remappee.nombre(),
                "le guide de la méthode meurt avec son ouverture");
        assertEquals(3, remappee.guide(0).ligneApresFin(),
                "la borne de la classe recule du différentiel");
        syntaxe.habiller(SOURCE_MUETTE, session.document());
        assertEquals(1, syntaxe.guides.nombre(),
                "le réapparimage raccorde la fermante orpheline");
        assertEquals(2, syntaxe.guides.guide(0).ligneApresFin());
        syntaxe.fermer();
    }
}
