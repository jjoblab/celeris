package jo.celeris.android.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.text.TextSession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Le cycle de libération de la vue (chantiers C1/C2 de la revue du
 * 2026-11-17) : {@link EditorView#release()} détache la session (ses
 * écouteurs ne la retiennent plus), referme le pont de syntaxe et
 * arrête les exécuteurs d'analyse — les fils « celeris-analyse » ne
 * prolifèrent plus ; le rattachement à la fenêtre ré-arme la vue. Le
 * détachement de la fenêtre, sans release() explicite, ne casse rien :
 * la vue ré-attachée continue d'éditer.
 */
@RunWith(RobolectricTestRunner.class)
public class CycleVieVueTest {

    /** Les noms des fils d'analyse vivants, filtrés du kit. */
    private static Set<Thread> filsAnalyse() {
        Set<Thread> vivants = ConcurrentHashMap.newKeySet();
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.isAlive() && thread.getName().startsWith("celeris-analyse")) {
                vivants.add(thread);
            }
        }
        return vivants;
    }

    /**
     * C2 — les exécuteurs s'arrêtent à la libération : après
     * release(), les fils « celeris-analyse » du kit meurent (C1/C2 :
     * deux fils par vue aujourd'hui, jamais arrêtés).
     */
    @Test
    public void releaseBreakpointeLesFilsDAnalyse() throws Exception {
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        TextSession session = TextSession.of("class A {}\n", System::nanoTime);
        vue.setSession(session);
        vue.setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        // La première analyse part DIRECT au travail (audit H1 : la
        // retenue ne s'applique plus à l'ouverture) — une édition
        // réveille la retenue, qui seule ouvre le minuteur.
        session.replaceRange(0, 0, "//");

        // Le démon nait paresseusement : l'analyse ET sa retenue
        // forcées, les fils ouvrent (celeris-analyse et son minuteur
        // celeris-analyse-retenu).
        int attendus = 2;
        CountDownLatch naisance = new CountDownLatch(attendus);
        AtomicInteger vus = new AtomicInteger();
        for (int tour = 0; tour < 200 && naisance.getCount() > 0; tour++) {
            vus.set(0);
            for (Thread thread : Thread.getAllStackTraces().keySet()) {
                if (thread.isAlive() && thread.getName().startsWith("celeris-analyse")) {
                    vus.incrementAndGet();
                }
            }
            if (vus.get() >= attendus) {
                for (int reste = 0; reste < attendus; reste++) {
                    naisance.countDown();
                }
                break;
            }
            Thread.sleep(10L);
        }
        assertTrue("les fils d'analyse du kit sont nés (vus : "
                + vus.get() + ")", naisance.getCount() == 0);

        Set<Thread> avant = filsAnalyse();
        assertTrue("au moins un fil d'analyse vit avant la libération",
                !avant.isEmpty());

        vue.release();

        // Les fils démons s'éteignent par eux-mêmes : on les attend
        // poliment (au plus 10 s), ils ne doivent plus être vivants.
        long limite = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(10L);
        while (!filsAnalyse().isEmpty() && System.nanoTime() < limite) {
            Thread.sleep(10L);
        }
        assertTrue("après release(), plus aucun fil celeris-analyse (restants : "
                + filsAnalyse().size() + ")", filsAnalyse().isEmpty());
    }

    /**
     * C2 — la non-prolifération : N vues montées puis libérées ne
     * laissent pas N paires de fils vivantes. La régression C2 (deux
     * fils par vue, jamais arrêtés) fait grossir le compte à chaque
     * tour.
     */
    @Test
    public void libererNeProliferePasLesFils() throws Exception {
        int tours = 4;
        for (int tour = 0; tour < tours; tour++) {
            EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
            TextSession session = TextSession.of("class A {}\n", System::nanoTime);
            vue.setSession(session);
            vue.setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
            vue.release();
        }
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10L);
        while (!filsAnalyse().isEmpty() && System.nanoTime() < limite) {
            Thread.sleep(10L);
        }
        assertEquals("aucun fil celeris-analyse ne survit aux libérations",
                0, filsAnalyse().size());
    }

    /**
     * C1 — la session ne retient plus la vue après release() : la
     * vue libérée devient collectable même si la session vit encore
     * (c'est la fuite relevée par la revue — session en ViewModel,
     * recréation d'activité). Le banc sonde le COMPORTEMENT (la
     * collectabilité), pas l'introspection des listes internes.
     */
    @Test
    public void releaseRendLaVueCollectable() throws Exception {
        TextSession session = TextSession.of("int x = 1;\n", System::nanoTime);
        // La vue naît dans sa propre méthode : le frame du banc ne
        // garde aucune référence forte après le retour (une variable
        // locale du banc survivrait au ramasse-miettes dans son slot
        // de pile).
        WeakReference<EditorView> vueFaible = monterPuisLiberer(session);
        forcerRamasseMiettes();
        assertNull("la vue libérée ne survit pas au ramasse-miettes "
                + "(la session ne la retient plus)", vueFaible.get());
    }

    /** Monte une vue sur la session, la libère, ne rend que sa
     *  référence faible — aucune référence forte ne s'échappe. */
    private static WeakReference<EditorView> monterPuisLiberer(
            TextSession session) {
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        vue.setSession(session);
        vue.setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        vue.release();
        return new WeakReference<>(vue);
    }

    /** Force le ramasse-miettes : plusieurs tours, jusqu'à ce que la
     *  référence faible meure (ou que les tours s'épuisent). */
    private static void forcerRamasseMiettes() throws Exception {
        for (int tour = 0; tour < 50; tour++) {
            System.gc();
            System.runFinalization();
            Thread.sleep(10L);
        }
    }

    /**
     * C1 — le rattachement à la fenêtre ré-arme la vue : un detach /
     * attach de fenêtre ne libère rien (le contrat du propriétaire
     * reste « release() explicite »), la vue continue d'éditer.
     */
    @Test
    public void detachementDeFenetreNAutoliberePasLaVue() {
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        TextSession session = TextSession.of("abc\n", System::nanoTime);
        vue.setSession(session);

        vue.onDetachedFromWindow();
        // La vue ré-attachée continue d'éditer : le détachement de
        // fenêtre n'est PAS une libération (release() explicite).
        vue.onAttachedToWindow();
        // Le caret au début du document : la frappe s'y insère.
        session.type("d");
        assertTrue("la vue ré-attachée édite encore",
                session.document().text().contains("dabc")
                        || session.document().text().contains("abcd"));
        assertFalse("la vue n'est pas libérée par le détachement seul",
                vue.estLiberee());
    }

    /** release() coupe aussi l'analyse : setLanguage d'après ne
     *  réveille rien (le pont est refermé). */
    @Test
    public void releaseRefermeLePontDeSyntaxe() throws Exception {
        EditorView vue = new EditorView(RuntimeEnvironment.getApplication());
        TextSession session = TextSession.of("class A {}\n", System::nanoTime);
        vue.setSession(session);
        vue.setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        vue.release();

        // Après release(), le pont est refermé : changer de langage ne
        // programme plus rien — aucun fil ne renaît.
        vue.setLanguage(null);
        vue.setLanguage(LanguageSupport.of(JavaSyntax.INSTANCE));
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(2L);
        while (System.nanoTime() < limite) {
            Thread.sleep(10L);
        }
        // Le seul fil possible serait né du setLanguage post-release :
        // il n'existe pas — la vue libérée reste muette.
        assertEquals("aucun fil d'analyse ne renaît après release()",
                0, filsAnalyse().size());
    }
    /** D11 (suite) : release() vide AUSSI le champ session — la vue
     *  libérée ne retient plus le document de l'onglet fermé ; la
     *  session lue y est vide et inerte. */
    @Test
    public void releaseVideLeChampSession() {
        EditorTestRig banc = EditorTestRig.on(
                RuntimeEnvironment.getApplication(),
                "un document qui ne doit pas survivre a la vue");
        EditorView vue = banc.vue();
        TextSession avant = vue.session();
        vue.release();
        assertTrue("la vue est libérée", vue.estLiberee());
        assertTrue("le champ session est vide (inerte)",
                vue.session() != avant
                        && vue.session().document().length() == 0);
    }

}
