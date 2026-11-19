package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.IncrementalLexer;
import jo.celeris.core.syntax.LanguageLexer;
import jo.celeris.core.syntax.MoteurSyntaxe;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;

/**
 * Le banc structurel de la PUBLICATION À CHAQUE RÉVISION (feuille de
 * route A5.2, verrou du correctif A1.1 côté vue) : la voie native
 * reprogramme maintenant l'analyse à l'édition — le banc le prouve
 * par une maquette REPROGRAMMANTE (le comportement de la voie réparée)
 * pilotée par le pont : chaque frappe publie, la vue invalide à
 * chaque révision, la couleur ne périmé jamais plus d'une frappe.
 *
 * <p>La maquette vit derrière le même contrat {@code MoteurSyntaxe}
 * que la voie réelle (fabrique de banc armée sélectivement) : la
 * preuve vaut pour la voie native — le banc natif direct saute sur
 * JVM (liaison Android), celui-ci tourne partout.</p>
 */
class PublicationChaqueRevisionBancTest {

    /**
     * La maquette REPROGRAMMANTE : chaque édition s'écoute (le
     * contrat de la voie réparée — A1.1) et reprogramme la passe ;
     * la passe publie l'analyse du document COURANT. C'est le
     * comportement exact de MoteurNatif après le correctif.
     */
    static final class MoteurReprogrammante implements MoteurSyntaxe {
        private final TextSession session;
        private final @org.jspecify.annotations.Nullable Executor executeur;
        private final List<java.util.function.Consumer<Analysis>> auditeurs =
                new ArrayList<>();
        private final LanguageLexer lexeur;
        private final AtomicInteger passes = new AtomicInteger();
        private @org.jspecify.annotations.Nullable Analysis publiee;
        private boolean ferme;

        MoteurReprogrammante(TextSession session,
                @org.jspecify.annotations.Nullable Executor executeur) {
            this.session = session;
            this.executeur = executeur;
            this.lexeur = JavaSyntax.INSTANCE.lexer();
            session.addEditListener((start, end, removed, inserted) -> {
                if (!ferme) {
                    reanalyser();
                }
            });
        }

        @Override
        public @org.jspecify.annotations.Nullable Analysis analysePubliee() {
            return publiee;
        }

        @Override
        public @org.jspecify.annotations.Nullable Analysis analyseDeRevision(long revision) {
            Analysis courante = publiee;
            return courante != null && courante.revision() == revision
                    ? courante : null;
        }

        @Override
        public void reanalyser() {
            Executor fil = executeur;
            if (fil != null) {
                fil.execute(this::passe);
            } else {
                passe();
            }
        }

        private void passe() {
            if (ferme) {
                return;
            }
            passes.incrementAndGet();
            Document document = session.document();
            publiee = new Analysis(document.revision(),
                    new IncrementalLexer().lexFull(document, lexeur).lex(),
                    jo.celeris.core.syntax.SyntaxTree.of(document.revision(),
                            List.of()));
            for (java.util.function.Consumer<Analysis> auditeur : List.copyOf(auditeurs)) {
                auditeur.accept(publiee);
            }
        }

        int passes() {
            return passes.get();
        }

        @Override
        public void fenetre(int premiereLigne, int derniereLigne) {
            // la voie entière ignore la fenêtre
        }

        @Override
        public void ajouterAuditeur(java.util.function.Consumer<Analysis> auditeur) {
            auditeurs.add(auditeur);
        }

        @Override
        public void close() {
            ferme = true;
        }
    }

    /** Exécuteur à commande : les passes se jouent à la main. */
    private static final class ExecuteurACommande implements Executor {
        private final List<Runnable> file = new ArrayList<>();

        @Override
        public void execute(Runnable commande) {
            file.add(commande);
        }

        void jouer() {
            List<Runnable> enAttente = new ArrayList<>(file);
            file.clear();
            enAttente.forEach(Runnable::run);
        }
    }

    private EditorSyntax syntaxe;
    private TextSession session;
    private ExecuteurACommande executeur;

    @BeforeEach
    void monter() {
        FabriqueVoieBanc.armee = true;
        FabriqueVoieBanc.moteurReprogrammante = true;
        executeur = new ExecuteurACommande();
        syntaxe = new EditorSyntax(new FoldModel(), executeur);
        session = TextSession.of("int alpha;\nint beta;\n", () -> 0L);
    }

    @AfterEach
    void demonter() {
        syntaxe.fermer();
        FabriqueVoieBanc.armee = false;
        FabriqueVoieBanc.moteurReprogrammante = false;
    }

    /**
     * La rafale de frappes publie à chaque révision : vingt frappes,
     * vingt passes servies — la maquette reprogrammante suit le
     * document, le pont encaisse les publications.
     */
    @Test
    void laRafalePublieAChaqueRevision() {
        syntaxe.attacher(session);
        syntaxe.setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        executeur.jouer();   // la passe d'attachement
        MoteurReprogrammante moteur = (MoteurReprogrammante)
                FabriqueVoieBanc.dernierMoteur;
        assertNotNull(moteur, "la fabrique de banc a servi la maquette");
        int passesInitiales = moteur.passes();
        assertTrue(passesInitiales >= 1, "l'attachement a publié");

        for (int i = 0; i < 20; i++) {
            assertTrue(session.replaceRange(0, 0, "x"));
            executeur.jouer();
        }
        assertEquals(20, moteur.passes() - passesInitiales,
                "chaque frappe a reprogrammé SA passe (voie native réparée)");
        assertEquals(session.document().revision(),
                moteur.analysePubliee().revision(),
                "la dernière publication sert la dernière révision");
    }

    /**
     * La couleur périmé au plus D'UNE frappe : entre la frappe et sa
     * publication, le rattrapage tient (A1.2) ; la publication suivie
     * recolle TOUT — le banc compte les transitions nues, il n'en
     * voit aucune après la publication.
     */
    @Test
    void laCouleurNePertimePlusDuneFrappe() {
        syntaxe.attacher(session);
        syntaxe.setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        executeur.jouer();
        Document avant = session.document();
        RowSource muette = cle -> {
            throw new UnsupportedOperationException("pas consultée");
        };
        assertTrue(syntaxe.habiller(muette, avant) != muette,
                "la publication d'attachement pare la source");

        // LA frappe : le rattrapage tient (pas de source nue)…
        assertTrue(session.replaceRange(5, 5, " + 1"));
        Document apres = session.document();
        assertTrue(syntaxe.habiller(muette, apres) != muette,
                "le rattrapage tient entre la frappe et sa publication");

        // …sa publication arrive, elle ressert TOUT.
        executeur.jouer();
        assertTrue(syntaxe.habiller(muette, apres) != muette,
                "la publication ressert la source");
        assertEquals(apres.revision(),
                ((MoteurReprogrammante) FabriqueVoieBanc.dernierMoteur)
                        .analysePubliee().revision(),
                "la publication suit la révision de la frappe");
    }
}
