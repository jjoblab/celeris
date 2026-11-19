package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.TextSession;

/**
 * Le moteur d'analyse hors fil d'appel (ADR-007, G-01) : l'exécuteur est
 * injecté — jamais déclaré au cœur —, le jeton annule le travail supplanté
 * et une tâche supplantée ne publie jamais (PIEGE-20 : la publication
 * vérifie son jeton sous moniteur), la fenêtre visible se paie d'abord et
 * l'édition après complet suit la voie incrémentale — une seule
 * publication. L'annulation d'une session rejouée suit la même voie
 * (PIEGE-07).
 */
class MoteurAnalyseTest {

    private static final String TEXTE = """
            fn a {
              x
            }
            var b ;
            fn c {
              y
            }
            """;

    /** Exécuteur déterministe : tout tourne sur le fil appelant. */
    private static final Executor DIRECT = Runnable::run;

    /** Exécuteur à commande : les tâches s'enregistrent, on les joue à la main. */
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

    /** L'analyse publiée au bout du compte égale l'analyse complète. */
    @Test
    void analysePublieeEgaleAnalyseComplete() {
        TextSession session = TextSession.of(TEXTE, () -> 0L);
        List<Analysis> publications = new ArrayList<>();
        AnalysisEngine moteur = AnalysisEngine.on(session, MiniLangage.langage(), DIRECT);
        moteur.addListener(publications::add);
        moteur.reanalyser();
        Analysis published = moteur.analysePubliee();
        assertNotNull(published);
        assertTrue(published.isComplete());
        assertEquals(session.document().revision(), published.revision());
        Analysis complet = SyntaxService.of(MiniLangage.langage()).analyseFull(session.document());
        Arbres.verifierEquivalence(published.tree(), complet.tree(), session.document());
        assertFalse(publications.isEmpty());
        assertSame(published, publications.get(publications.size() - 1));
        moteur.close();
    }

    /** La fenêtre visible se paie d'abord : la première publication est partielle. */
    @Test
    void fenetreVisibleAnalyseeDabord() {
        TextSession session = TextSession.of(TEXTE, () -> 0L);
        List<Analysis> publications = new ArrayList<>();
        AnalysisEngine moteur = AnalysisEngine.on(session, MiniLangage.langage(), DIRECT,
                0, 1);
        moteur.addListener(publications::add);
        moteur.reanalyser();
        assertTrue(publications.size() >= 2,
                "la fenêtre d'abord, puis la complétion : au moins deux publications");
        Analysis premiere = publications.get(0);
        assertFalse(premiere.isComplete());
        assertFalse(premiere.tree().chunks().get(0).isPending());
        assertTrue(premiere.tree().chunks().get(2).isPending());
        assertTrue(moteur.analysePubliee() != null && moteur.analysePubliee().isComplete(),
                "la complétion a suivi la fenêtre");
        moteur.close();
    }

    /** Une édition avant que la tâche ne tourne la supplante : la tâche périmée se tait. */
    @Test
    void editionAvantTourneeTachePerimeeSeTait() {
        TextSession session = TextSession.of(TEXTE, () -> 0L);
        List<Analysis> publications = new ArrayList<>();
        ExecuteurACommande executor = new ExecuteurACommande();
        AnalysisEngine moteur = AnalysisEngine.on(session, MiniLangage.langage(), executor);
        moteur.addListener(publications::add);
        moteur.reanalyser();
        assertEquals(1, executor.file.size());
        int target = TEXTE.indexOf("  y");
        session.replaceRange(target, target + 3, "  omega");
        assertEquals(2, executor.file.size(), "l'édition supplante et reprogramme");
        executor.jouer();
        Analysis published = moteur.analysePubliee();
        assertNotNull(published);
        assertEquals(session.document().revision(), published.revision());
        for (Analysis publication : publications) {
            assertEquals(session.document().revision(), publication.revision(),
                    "aucune publication de révision périmée (PIEGE-20)");
        }
        moteur.close();
    }

    /**
     * Un lexeur qui se retient derrière une porte à la ligne donnée :
     * pour tenir un travail d'analyse en plein vol, déterministe.
     */
    private static LanguageSyntax retenuDerrierePorte(int lignePorte,
            CountDownLatch porte, AtomicBoolean atteinte) {
        LanguageSyntax coeur = MiniLangage.langage();
        LanguageLexer lexeur = coeur.lexer();
        class Retenu implements LanguageLexer {
            private int lignesLues;

            @Override
            public LexState initialState() {
                return lexeur.initialState();
            }

            @Override
            public LexedLine lex(CharSequence text, LexState etat) {
                if (lignesLues++ == lignePorte) {
                    atteinte.set(true);
                    try {
                        porte.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                return lexeur.lex(text, etat);
            }
        }
        Retenu retenu = new Retenu();
        return new LanguageSyntax() {
            @Override
            public String id() {
                return coeur.id();
            }

            @Override
            public LanguageLexer lexer() {
                return retenu;
            }

            @Override
            public LanguageParser parser() {
                return coeur.parser();
            }
        };
    }

    /**
     * Une tâche supplantée en plein vol — le lexeur retenu derrière une
     * porte — ne publie rien : l'annulation coûte moins cher que terminer
     * et l'instantané périmé n'écrase jamais le plus récent (PIEGE-20).
     */
    @Test
    void travailSupplanteEnPleinVolNePubliePas() throws InterruptedException {
        CountDownLatch porte = new CountDownLatch(1);
        AtomicBoolean atteinte = new AtomicBoolean();
        LanguageSyntax langageRetenu = retenuDerrierePorte(2, porte, atteinte);
        ExecutorService thread = Executors.newSingleThreadExecutor();
        try {
            TextSession session = TextSession.of(TEXTE, () -> 0L);
            List<Analysis> publications = new ArrayList<>();
            AnalysisEngine moteur = AnalysisEngine.on(session, langageRetenu, thread);
            moteur.addListener(publications::add);
            moteur.reanalyser();
            for (int i = 0; i < 500 && !atteinte.get(); i++) {
                Thread.sleep(10);
            }
            assertTrue(atteinte.get(), "le lexeur retenu a bien atteint la porte");
            int target = TEXTE.indexOf("  y");
            session.replaceRange(target, target + 3, "  omega");
            porte.countDown();
            for (int i = 0; i < 500; i++) {
                Analysis published = moteur.analysePubliee();
                if (published != null && published.revision() == session.document().revision()) {
                    break;
                }
                Thread.sleep(10);
            }
            Analysis finale = moteur.analysePubliee();
            assertNotNull(finale);
            assertTrue(finale.isComplete());
            assertEquals(session.document().revision(), finale.revision());
            for (Analysis published : publications) {
                assertTrue(published.revision() >= session.document().revision() - 1,
                        "aucun retour en arrière de révision");
            }
            moteur.close();
        } finally {
            thread.shutdownNow();
        }
    }

    /** La fermeture avant exécution : la tâche se tait, l'auditeur est détaché. */
    @Test
    void fermetureStoppeTout() {
        TextSession session = TextSession.of(TEXTE, () -> 0L);
        List<Analysis> publications = new ArrayList<>();
        ExecuteurACommande executor = new ExecuteurACommande();
        AnalysisEngine moteur = AnalysisEngine.on(session, MiniLangage.langage(), executor);
        moteur.addListener(publications::add);
        moteur.reanalyser();
        moteur.close();
        executor.jouer();
        assertTrue(publications.isEmpty(), "fermée, la tâche ne publie pas");
        assertNull(moteur.analysePubliee());
        int tailleAvant = executor.file.size();
        session.replaceRange(0, 1, "z");
        assertEquals(tailleAvant, executor.file.size(),
                "détachée, la session ne reprogramme plus rien");
    }

    /** L'édition après complet suit la voie incrémentale : une seule publication. */
    @Test
    void editionApresCompletUneSeulePublication() {
        TextSession session = TextSession.of(TEXTE, () -> 0L);
        List<Analysis> publications = new ArrayList<>();
        AnalysisEngine moteur = AnalysisEngine.on(session, MiniLangage.langage(), DIRECT,
                0, 1);
        moteur.addListener(publications::add);
        moteur.reanalyser();
        int apresOuverture = publications.size();
        int target = TEXTE.indexOf("  y");
        session.replaceRange(target, target + 3, "  omega");
        assertEquals(apresOuverture + 1, publications.size(),
                "l'incrément publie une fois, pas la voie complète");
        Analysis derniere = publications.get(publications.size() - 1);
        assertTrue(derniere.isComplete());
        assertEquals(session.document().revision(), derniere.revision());
        Analysis complet = SyntaxService.of(MiniLangage.langage())
                .analyseFull(session.document());
        Arbres.verifierEquivalence(derniere.tree(), complet.tree(), session.document());
        moteur.close();
    }

    /** Le rejeu de l'annulation suit la même voie : la publication rejoint le complet (PIEGE-07). */
    @Test
    void annulationRejoueeLAnalyseSuit() {
        TextSession session = TextSession.of(TEXTE, () -> 0L);
        List<Analysis> publications = new ArrayList<>();
        AnalysisEngine moteur = AnalysisEngine.on(session, MiniLangage.langage(), DIRECT);
        moteur.addListener(publications::add);
        moteur.reanalyser();
        int target = TEXTE.indexOf("var");
        session.replaceRange(target, target + 3, "let");
        session.undo();
        Analysis finale = moteur.analysePubliee();
        assertNotNull(finale);
        assertEquals(session.document().revision(), finale.revision());
        assertTrue(finale.isComplete());
        Analysis complet = SyntaxService.of(MiniLangage.langage())
                .analyseFull(session.document());
        Arbres.verifierEquivalence(finale.tree(), complet.tree(), session.document());
        moteur.close();
    }
}
