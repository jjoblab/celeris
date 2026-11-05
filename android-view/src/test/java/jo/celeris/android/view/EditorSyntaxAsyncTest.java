package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import jo.celeris.core.syntax.JavaSyntax;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;

/**
 * Le pont session ↔ vue en mode hors fil d'appel (tranche 6 M4, G-01,
 * PIEGE-20) : l'exécuteur s'injecte, la première image ne paie jamais
 * l'arbre — sans analyse publiée, la source passe sans couleur ; la
 * publication habille les rangées et prévient la vue (le rappel
 * d'invalidation) ; l'édition pendant le vol retombe sans couleur puis
 * recolore à la publication de la bonne révision — jamais le lexique
 * d'une autre révision ne colore un document ; le retrait du langage
 * referme le moteur.
 */
class EditorSyntaxAsyncTest {

    private static final String SOURCE = """
            package demo;

            class Premiere {
                void methode() {
                }
            }

            class Deuxieme {
            }
            """;

    /** Exécuteur déterministe : tout tourne sur le fil appelant. */
    private static final Executor DIRECT = Runnable::run;

    /** Exécuteur à commande : les tâches s'enregistrent, on les joue à la main. */
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

    /** Une source qui ne rend rien : habiller l'enveloppe ou la laisse, jamais ne la consulte ici. */
    private static final RowSource SOURCE_MUETTE = cle -> {
        throw new UnsupportedOperationException("pas consultée par le pont");
    };

    /**
     * Sans analyse publiée, la source passe telle quelle : la première
     * image ne paie jamais l'arbre (PIEGE-20, facette vue).
     */
    @Test
    void sansAnalysePublieeLaSourceResteSansCouleur() {
        ExecuteurACommande executeur = new ExecuteurACommande();
        EditorSyntax syntaxe = new EditorSyntax(new FoldModel(), executeur);
        TextSession session = TextSession.of(SOURCE, () -> 0L);
        syntaxe.attacher(session);
        syntaxe.setLanguage(JavaSyntax.INSTANCE);
        assertFalse(executeur.file.isEmpty(), "le moteur a programmé son ouverture");
        Document document = session.document();
        assertSame(SOURCE_MUETTE, syntaxe.habiller(SOURCE_MUETTE, document),
                "sans analyse publiée, pas de coloration");
        executeur.jouer();
        assertFalse(syntaxe.habiller(SOURCE_MUETTE, document) == SOURCE_MUETTE,
                "après publication, la source se pare");
        syntaxe.fermer();
    }

    /** La publication prévient la vue : le rappel d'invalidation part à chaque instantané. */
    @Test
    void publicationPrevientLaVue() {
        AtomicBoolean prevenue = new AtomicBoolean();
        EditorSyntax syntaxe = new EditorSyntax(new FoldModel(), DIRECT);
        syntaxe.setSurAnalysePrete(() -> prevenue.set(true));
        TextSession session = TextSession.of(SOURCE, () -> 0L);
        syntaxe.attacher(session);
        syntaxe.setLanguage(JavaSyntax.INSTANCE);
        assertTrue(prevenue.get(), "la publication directe prévient la vue");
        syntaxe.fermer();
    }

    /**
     * L'édition pendant le vol retombe sans couleur puis recolore à la
     * publication de la bonne révision — le lexique d'une autre révision
     * ne colore jamais un document (PIEGE-20).
     */
    @Test
    void editionPendantLeVolRetombeSansCouleurPuisRecolore() {
        ExecuteurACommande executeur = new ExecuteurACommande();
        EditorSyntax syntaxe = new EditorSyntax(new FoldModel(), executeur);
        TextSession session = TextSession.of(SOURCE, () -> 0L);
        syntaxe.attacher(session);
        syntaxe.setLanguage(JavaSyntax.INSTANCE);
        int cible = SOURCE.indexOf("methode");
        session.replaceRange(cible, cible + 7, "autre");
        Document edite = session.document();
        assertSame(SOURCE_MUETTE, syntaxe.habiller(SOURCE_MUETTE, edite),
                "aucune analyse publiée pour la révision courante : sans couleur");
        executeur.jouer();
        assertFalse(syntaxe.habiller(SOURCE_MUETTE, edite) == SOURCE_MUETTE,
                "la publication de la bonne révision recolore");
        syntaxe.fermer();
    }

    /** Le retrait du langage referme le moteur et vide la coloration. */
    @Test
    void retraitDuLangageRefermeLeMoteur() {
        EditorSyntax syntaxe = new EditorSyntax(new FoldModel(), DIRECT);
        TextSession session = TextSession.of(SOURCE, () -> 0L);
        syntaxe.attacher(session);
        syntaxe.setLanguage(JavaSyntax.INSTANCE);
        syntaxe.setLanguage(null);
        assertFalse(syntaxe.active(), "sans langage, plus de moteur");
        assertSame(SOURCE_MUETTE, syntaxe.habiller(SOURCE_MUETTE, session.document()));
        syntaxe.fermer();
    }
}
