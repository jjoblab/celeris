package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextSession;

/**
 * La pose de la MÊME langue (audit coloration, piste H10) : la
 * Javadoc d'{@code EditorSyntax.setLanguage} promet « la LANGUE
 * inchangée ne re-rattache rien » — mais le code vide
 * {@code editionsEnAttente}, {@code chaineRompue} et {@code guides}
 * SANS CONDITION. Une pose de la langue déjà en place perd le
 * rattrapage de coloration : les éditions en attente s'effacent,
 * {@code wrapStale} rend la source SANS COULEUR — l'utilisateur voit
 * un flash de texte brut entre la pose et la publication.
 */
class SetLangueInchangeeTest {

    private static final String SOURCE = """
            package exemple;

            class Premiere {
                void methode() {
                }
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

    /** La même langue reposée ne doit RIEN perdre : le rattrapage
     *  des éditions en attente survit — jamais de flash de texte
     *  brut entre la pose et la publication. */
    @Test
    void laLangueInchangeeNeVidePasLeRattrapage() {
        ExecuteurACommande executor = new ExecuteurACommande();
        EditorSyntax syntaxe = new EditorSyntax(new FoldModel(), executor);
        LanguageSupport java = LanguageSupport.of(JavaSyntax.INSTANCE);
        TextSession session = TextSession.of(SOURCE, () -> 0L);
        syntaxe.attach(session);
        syntaxe.setLanguage(java);
        executor.jouer();

        int cible = SOURCE.indexOf("methode");
        session.replaceRange(cible, cible + 7, "autre");
        Document edite = session.document();

        // Le contrôle : le rattrapage tient les couleurs publiées.
        RowSource muette = key -> {
            throw new UnsupportedOperationException("pas consultée");
        };
        assertFalse(syntaxe.wrap(muette, edite) == muette,
                "contrôle : le rattrapage pare la source entre édition "
                        + "et publication");

        // La faute : la MÊME langue reposée vide le rattrapage.
        syntaxe.setLanguage(java);
        assertFalse(syntaxe.wrap(muette, edite) == muette,
                "la langue inchangée ne re-rattache rien : le rattrapage "
                        + "survit — pas de flash de texte brut");
        executor.jouer();
        assertFalse(syntaxe.wrap(muette, edite) == muette,
                "après publication, la couleur revient de toute façon");
        syntaxe.close();
    }

    /** La pose d'une langue DIFFÉRENTE re-rattache : le moteur de
     *  l'ancienne langue se referme, celui de la nouvelle naît — le
     *  contrat explicite de la méthode. */
    @Test
    void uneLangueDifferenteReattacheBien() {
        EditorSyntax syntaxe = new EditorSyntax(new FoldModel(), DIRECT);
        LanguageSupport java = LanguageSupport.of(JavaSyntax.INSTANCE);
        TextSession session = TextSession.of(SOURCE, () -> 0L);
        syntaxe.attach(session);
        syntaxe.setLanguage(java);
        assertTrue(syntaxe.active(), "le moteur java vit");
        syntaxe.setLanguage(LanguageSupport.of(
                jo.celeris.core.syntax.clike.KotlinSyntax.INSTANCE));
        assertTrue(syntaxe.active(), "le moteur kotlin vit");
        syntaxe.close();
    }
}
