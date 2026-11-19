package jo.celeris.core.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import jo.celeris.core.text.TextSession;

/**
 * La frappe rapide (B-06, l'analyse croisée) : une deuxième frappe
 * avant publication faisait basculer le moteur sur la voie COMPLÈTE
 * (cheminComplet = true) — le lexing déjà fait était jeté, le document
 * entier re-lexiqué. Le dépôt de départ épisse ses pistes à CHAQUE
 * édition : les sites se cumulent, l'incrément repart de la
 * publication avec la plage fusionnée.
 */
public class FrappeRapideTest {

    private static final String TEXTE = """
            fn a {
              x
            }
            var b ;
            fn c {
              y
            }
            """;

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

    /** Après une publication, deux frappes DRAINÉES EN UNE FOIS
     *  s'épissent : la ligne épargnée garde son panier d'origine —
     *  le lexique publié sert de base, les sites fusionnent en une
     *  passe incrémentale, jamais la voie complète. */
    @Test
    public void deuxFrappesDraineesEnsembleGardentLePanierEpargne() {
        TextSession session = TextSession.of(TEXTE, () -> 0L);
        ExecuteurACommande executor = new ExecuteurACommande();
        AtomicReference<Analysis> published = new AtomicReference<>();
        AnalysisEngine moteur = AnalysisEngine.on(session,
                MiniLangage.langage(), executor);
        moteur.addListener(published::set);

        // La publication initiale : la voie complète réclamée, la
        // fenêtre couvre tout.
        moteur.window(0, 10);
        moteur.reanalyser();
        executor.jouer();
        Analysis premiere = published.get();
        assertNotNull(premiere, "la publication initiale existe");
        assertEquals(session.document().revision(), premiere.revision());
        Object panierLigne0 = premiere.lex().line(0);

        // Frappe 1 : ligne 1 (« x » → « xx »).
        int cibleX = TEXTE.indexOf("  x");
        session.replaceRange(cibleX + 2, cibleX + 3, "xx");
        // Frappe 2 AVANT toute publication : ligne 3 (« b » → « bb »).
        String text = session.document().text();
        int cibleB = text.indexOf("b ;");
        session.replaceRange(cibleB, cibleB + 1, "bb");

        // Le drain joue les DEUX tâches d'un coup : la première est
        // supplantée (jeton annulé), la seconde court — les deux sites
        // doivent avoir FUSIONNÉ, la ligne 0 garde son panier.
        executor.jouer();

        Analysis seconde = published.get();
        assertNotNull(seconde, "la publication des frappes existe");
        assertEquals(session.document().revision(), seconde.revision());
        assertSame(panierLigne0, seconde.lex().line(0),
                "la ligne 0 épargnée garde son panier d'origine — les "
                + "deux frappes se sont épissées, pas re-lexiquées");
        Arbres.verifierEquivalence(seconde.tree(),
                SyntaxService.of(MiniLangage.langage())
                        .analyseFull(session.document()).tree(),
                session.document());
        moteur.close();
    }
}
