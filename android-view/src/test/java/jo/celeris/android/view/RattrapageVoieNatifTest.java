package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.IncrementalLexer;
import jo.celeris.core.syntax.LanguageLexer;
import jo.celeris.core.syntax.SyntaxEngine;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;

/**
 * Le banc du rattrapage de coloration sur la VOIE NATIVE (feuille de
 * route A1.2) : l'habillage périmé ne servait que la voie hors fil du
 * socle — quand la voie native servait, la frappe rendait la source
 * TELLE QUELLE, sans couleur, jusqu'à la publication suivante (et
 * pour toujours tant qu'elle ne venait pas). Le banc verrouille le
 * contrat nouveau : la publication périmée de N'IMPORTE QUELLE voie
 * rattrape par recalage, la frappe garde ses couleurs.
 *
 * <p>La voie y vit par une FABRIQUE DE BANC (META-INF/services de ce
 * module de test) : un moteur maquette qui publie une analyse réelle
 * du lexeur Java à l'attachement, puis reste silencieux — le pire cas
 * de la voie native avant A1.1. Le pont ne connaît que le contrat
 * {@code SyntaxEngine} : la preuve vaut pour la vraie voie.</p>
 */
class RattrapageVoieNatifTest {

    /** Le lexeur réel : la maquette publie une VRAIE analyse. */
    private static final LanguageLexer LEXEUR = JavaSyntax.INSTANCE.lexer();

    /** Le moteur maquette : publie à la première reanalyser(), puis
     *  silence — l'édition ne reprogramme RIEN (le comportement natif
     *  avant A1.1, le pire cas du rattrapage). */
    static final class MoteurMaquette implements SyntaxEngine {
        private final TextSession session;
        private final @org.jspecify.annotations.Nullable Executor executor;
        private final List<java.util.function.Consumer<Analysis>> auditeurs =
                new ArrayList<>();
        private @org.jspecify.annotations.Nullable Analysis published;

        MoteurMaquette(TextSession session, @org.jspecify.annotations.Nullable Executor executor) {
            this.session = session;
            this.executor = executor;
        }

        @Override
        public @org.jspecify.annotations.Nullable Analysis analysePubliee() {
            return published;
        }

        @Override
        public @org.jspecify.annotations.Nullable Analysis analyseDeRevision(long revision) {
            Analysis courante = published;
            return courante != null && courante.revision() == revision
                    ? courante : null;
        }

        @Override
        public void reanalyser() {
            Executor thread = executor;
            if (thread != null) {
                thread.execute(this::passe);
            } else {
                passe();
            }
        }

        private void passe() {
            Document document = session.document();
            published = new Analysis(document.revision(),
                    new IncrementalLexer().lexFull(document, LEXEUR).lex(),
                    jo.celeris.core.syntax.SyntaxTree.of(document.revision(),
                            java.util.List.of()));
            for (java.util.function.Consumer<Analysis> auditeur : List.copyOf(auditeurs)) {
                auditeur.accept(published);
            }
        }

        @Override
        public void window(int premiereLigne, int derniereLigne) {
            // la voie entière ignore la fenêtre
        }

        @Override
        public void addListener(java.util.function.Consumer<Analysis> auditeur) {
            auditeurs.add(auditeur);
        }

        @Override
        public void close() {
            // rien à libérer
        }
    }

    /** Exécuteur à commande : les passes se jouent à la main. */
    private static final class ExecuteurACommande implements Executor {
        private final List<Runnable> file = new ArrayList<>();

        @Override
        public void execute(Runnable command) {
            file.add(command);
        }

        void jouer() {
            List<Runnable> enAttente = new ArrayList<>(file);
            file.clear();
            enAttente.forEach(Runnable::run);
        }
    }

    /** Source muette : habiller ne la consulte jamais ici. */
    private static final RowSource SOURCE_MUETTE = key -> {
        throw new UnsupportedOperationException("pas consultée par le pont");
    };

    private EditorSyntax syntaxe;
    private TextSession session;
    private ExecuteurACommande executor;

    @BeforeEach
    void start() {
        FabriqueVoieBanc.armee = true;
        executor = new ExecuteurACommande();
        syntaxe = new EditorSyntax(new FoldModel(), executor);
        session = TextSession.of("int alpha;\nint beta;\n", () -> 0L);
    }

    @AfterEach
    void demonter() {
        syntaxe.close();
        FabriqueVoieBanc.armee = false;
    }

    /**
     * La publication de la voie native habille : après la passe
     * d'attachement, la source se pare (le chemin ordinaire).
     */
    @Test
    void laPublicationNativesHabille() {
        syntaxe.attach(session);
        syntaxe.setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        executor.jouer();
        Document document = session.document();
        assertNotNull(syntaxe.wrap(SOURCE_MUETTE, document));
        assertTrue(syntaxe.wrap(SOURCE_MUETTE, document) != SOURCE_MUETTE,
                "après publication, la source se pare");
    }

    /**
     * LA FRAPPE GARDE SES COULEURS sur la voie native (A1.2) : la
     * publication périmée rattrape — la ligne touchée reçoit ses
     * jetons recalés, jamais l'encre de base nue. Le moteur maquette
     * reste silencieux après l'édition : c'est le rattrapage seul qui
     * recolle.
     */
    @Test
    void laFrappeGardeSesCouleursSurLaVoieNative() {
        syntaxe.attach(session);
        syntaxe.setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        executor.jouer();   // la publication d'attachement
        Document avant = session.document();
        assertTrue(syntaxe.wrap(SOURCE_MUETTE, avant) != SOURCE_MUETTE);

        // LA frappe : "alpha" -> "alphabet" (non structurelle)
        int pos = "int alpha".length();
        assertTrue(session.replaceRange(pos, pos, "bet"));
        Document apres = session.document();

        RowSource habillee = syntaxe.wrap(SOURCE_MUETTE, apres);
        assertTrue(habillee != SOURCE_MUETTE,
                "la publication périmée de la voie native DOIT rattraper — "
                        + "jamais la source nue après une frappe");
        // La couleur attendue reste posée : la vue compte les rangées
        // décolorées, elle ne doit rien compter ici.
        assertTrue(syntaxe.expectedColor(),
                "une publication existe, la couleur reste attendue");
    }

    /**
     * La rafale structurelle sur la voie native : la chaîne d'éditions
     * se borne comme pour la voie du socle — au-delà du plafond (256
     * éditions en attente, le plafond du pont), le rattrapage cède
     * (la source nue, la publication suivie la recolore), jamais ne
     * devine.
     */
    @Test
    void laRafaleBoreeResteHonnete() {
        syntaxe.attach(session);
        syntaxe.setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        executor.jouer();
        for (int i = 0; i < 266; i++) {
            assertTrue(session.replaceRange(0, 0, "\n"));
        }
        Document apres = session.document();
        // chaîne rompue ou pas, la source reste NUE ou rattrape : le
        // contrat est « jamais de couleur devinée ». Ici la chaîne est
        // rompue (rafale au-delà du plafond) : la source passe nue.
        RowSource habillee = syntaxe.wrap(SOURCE_MUETTE, apres);
        assertEquals(SOURCE_MUETTE, habillee,
                "rafale au-delà du plafond : le rattrapage cède, la source nue");
    }
}
