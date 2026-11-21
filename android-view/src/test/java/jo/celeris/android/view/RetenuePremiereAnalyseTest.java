package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;

import jo.celeris.android.view.internal.exec.RetainedExecutor;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.AnalysisEngine;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;

/**
 * La retenue d'analyse au banc de l'audit coloration (pistes H1 et
 * H2) : la retenue de 250 ms ne s'applique PLUS à la première analyse
 * complète d'une ouverture (H1 — l'ouverture part direct, la retenue
 * sert la frappe) et le créneau d'attente est PAR MOTEUR (H2) : les
 * ouvertures rapprochées de deux onglets ne se suppriment plus leurs
 * tâches — le premier garde sa couleur.
 *
 * <p>Horloge injectable (ADR-007) : le minuteur captif fige le temps,
 * aucune attente réelle ne vit dans ces bancs.</p>
 */
class RetenuePremiereAnalyseTest {

    private static final String SOURCE = """
            package exemple;

            class Premiere {
                void methode() {
                }
            }
            """;

    /** Le minuteur captif : les tâches planifiées se capturent, le
     *  temps ne court que quand le banc le décide — l'horloge
     *  injectable du contrat ADR-007. Chaque planification rend SA
     *  commande d'annulation, comme le minuteur démon de production. */
    private static final class MinuteurCaptif implements RetainedExecutor.Delayer {

        private final List<Runnable> planifiees = new ArrayList<>();
        private int programmations;

        @Override
        public Runnable schedule(long delayMs, Runnable task) {
            programmations++;
            planifiees.add(task);
            return () -> planifiees.remove(task);
        }

        @Override
        public void shutdown() {
            // le minuteur de banc ne pose aucun fil
        }

        /** Le terme du délai arrive : toutes les planifiées partent. */
        void tirer() {
            List<Runnable> enAttente = new ArrayList<>(planifiees);
            planifiees.clear();
            enAttente.forEach(Runnable::run);
        }

        /** Les tâches encore en attente du minuteur. */
        int enAttente() {
            return planifiees.size();
        }
    }

    /** H1 : la première analyse ne patiente PAS la retenue — la
     *  retenue sert la frappe rapide, pas l'ouverture : la première
     *  soumission part direct au travail, les suivantes se retiennent. */
    @Test
    void laPremiereAnalyseNePatientePasLaRetenue() {
        MinuteurCaptif minuteur = new MinuteurCaptif();
        List<Runnable> file = new ArrayList<>();
        RetainedExecutor retenu = new RetainedExecutor(file::add, minuteur,
                250L);
        TextSession session = TextSession.of(SOURCE, () -> 0L);
        AnalysisEngine moteur =
                AnalysisEngine.on(session, JavaSyntax.INSTANCE, retenu);
        moteur.reanalyser();
        assertEquals(0, minuteur.programmations,
                "l'ouverture ne se programme pas au minuteur : elle part "
                        + "direct — 250 ms de retard en moins sur la "
                        + "première couleur");
        assertEquals(1, file.size(), "la tâche part direct à la base");
        file.forEach(Runnable::run);
        assertNotNull(moteur.analysePubliee(),
                "la première analyse est servie sans attendre le calme");
        // La frappe suivante, elle, se retient : le calme du doigt.
        int cible = SOURCE.indexOf("methode");
        session.replaceRange(cible, cible + 7, "autre");
        assertEquals(1, minuteur.programmations,
                "l'édition suivante se retient — la coalescence garde "
                        + "son rôle");
        moteur.close();
    }

    /** H2 : deux moteurs, une vue, moins de 250 ms d'écart — la
     *  retenue est PAR MOTEUR : la soumission de l'un ne supprime pas
     *  la tâche de l'autre, chacun garde sa publication. */
    @Test
    void deuxMoteursEnMoinsDeLaRetenueChacunGardeSaTache() {
        MinuteurCaptif minuteur = new MinuteurCaptif();
        RetainedExecutor modele =
                new RetainedExecutor(Runnable::run, minuteur, 250L);
        TextSession sessionA = TextSession.of(SOURCE, () -> 0L);
        AnalysisEngine moteurA =
                AnalysisEngine.on(sessionA, JavaSyntax.INSTANCE,
                        modele.pourMoteur());
        moteurA.reanalyser();
        TextSession sessionB =
                TextSession.of(SOURCE + "\nclass B {}\n", () -> 0L);
        AnalysisEngine moteurB =
                AnalysisEngine.on(sessionB, JavaSyntax.INSTANCE,
                        modele.pourMoteur());
        moteurB.reanalyser();
        assertNotNull(moteurA.analysePubliee(),
                "A a publié : les ouvertures ne patientent pas (H1)");
        assertNotNull(moteurB.analysePubliee(),
                "B a publié : l'ouverture de B n'a rien coûté à A");

        // La frappe rapide sur CHAQUE moteur se retient — sans que
        // l'une supprime l'autre (le créneau est par moteur).
        int cible = SOURCE.indexOf("methode");
        sessionA.replaceRange(cible, cible + 7, "autre");
        sessionB.replaceRange(cible, cible + 7, "autre");
        assertEquals(2, minuteur.enAttente(),
                "chaque moteur garde SA tâche en attente : le créneau "
                        + "ne se partage plus au détriment du premier");
        minuteur.tirer();
        assertNotNull(moteurA.analyseDeRevision(
                        sessionA.document().revision()),
                "A publie sa révision éditée");
        assertNotNull(moteurB.analyseDeRevision(
                        sessionB.document().revision()),
                "B publie sa révision éditée");
        moteurA.close();
        moteurB.close();
    }

    /** H2 côté vue (le scénario des onglets) : deux attachements de
     *  session en moins de 250 ms — le retour au premier onglet doit
     *  y retrouver SA publication : la couleur revient SANS nouvelle
     *  analyse. */
    @Test
    void lePremierOngletGardeSaPublicationMalgreLouvertureRapide() {
        MinuteurCaptif minuteur = new MinuteurCaptif();
        RetainedExecutor retenu =
                new RetainedExecutor(Runnable::run, minuteur, 250L);
        EditorSyntax syntaxe = new EditorSyntax(new FoldModel(), retenu);
        LanguageSupport java = LanguageSupport.of(JavaSyntax.INSTANCE);
        TextSession ongletA = TextSession.of(SOURCE, () -> 0L);
        syntaxe.attach(ongletA, java);
        TextSession ongletB =
                TextSession.of(SOURCE + "\nclass B {}\n", () -> 0L);
        syntaxe.attach(ongletB, java);
        minuteur.tirer();
        // Retour à l'onglet A : sa publication doit exister — la
        // couleur s'y sert SANS attendre une nouvelle passe.
        syntaxe.attach(ongletA, java);
        Document documentA = ongletA.document();
        RowSource muette = key -> {
            throw new UnsupportedOperationException("pas consultée");
        };
        assertFalse(syntaxe.wrap(muette, documentA) == muette,
                "l'onglet A ne doit pas avoir perdu sa première analyse "
                        + "à l'ouverture rapide de l'onglet B");
        syntaxe.close();
    }

    /** Le délai nul rend la main immédiate — la retenue ne retient
     *  rien quand l'hôte l'a réglée à zéro (le réglage documenté). */
    @Test
    void leDelaiNulRendLaMainImmediate() {
        MinuteurCaptif minuteur = new MinuteurCaptif();
        List<Runnable> file = new ArrayList<>();
        Executor base = file::add;
        RetainedExecutor retenu = new RetainedExecutor(base, minuteur, 0L);
        TextSession session = TextSession.of(SOURCE, () -> 0L);
        AnalysisEngine moteur =
                AnalysisEngine.on(session, JavaSyntax.INSTANCE, retenu);
        moteur.reanalyser();
        assertEquals(0, minuteur.programmations,
                "aucune programmation au délai nul");
        assertEquals(1, file.size(), "la tâche part direct à la base");
        file.forEach(Runnable::run);
        assertNotNull(moteur.analysePubliee());
        moteur.close();
    }
}
