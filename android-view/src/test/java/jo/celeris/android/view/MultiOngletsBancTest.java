package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.clike.GroovySyntax;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.syntax.clike.KotlinSyntax;
import jo.celeris.core.text.TextSession;

import org.junit.jupiter.api.Test;

/**
 * Le banc structurel MULTI-ONGLETS de la feuille de route (A5.1) : le
 * scénario du retour terrain — trois onglets ouverts, TROIS langages,
 * dix bascules — le retour d'un onglet ne re-tokenise RIEN (le moteur
 * parqué ressort sa publication), la première ouverture de chaque
 * onglet se paie UNE fois. Le banc verrouille le critère compté : à
 * l'équilibre, le nombre de passes par onglet ne croît plus avec les
 * bascules.
 */
final class MultiOngletsBancTest {

    /** Exécuteur à commande : les passes se comptent, se jouent à la main. */
    private static final class ExecuteurCompte implements Executor {
        private final List<Runnable> file = new ArrayList<>();
        private final AtomicInteger passees = new AtomicInteger();

        @Override
        public void execute(Runnable commande) {
            file.add(commande);
        }

        void jouer() {
            List<Runnable> enAttente = new ArrayList<>(file);
            file.clear();
            enAttente.forEach(r -> {
                passees.incrementAndGet();
                r.run();
            });
        }
    }

    /**
     * Trois onglets, trois langages, dix bascules : chaque onglet se
     * paie sa première passe, AUCUN ne se re-paie au retour — le
     * compteur total cesse de croître quand les bascules continuent.
     */
    @Test
    void troisOngletsTroisLangagesDixBasculesSansRetokenisation() {
        ExecuteurCompte executeur = new ExecuteurCompte();
        EditorSyntax syntaxe = new EditorSyntax(new FoldModel(), executeur);

        LanguageSupport java = LanguageSupport.of(JavaSyntax.INSTANCE);
        LanguageSupport kotlin = LanguageSupport.of(KotlinSyntax.INSTANCE);
        LanguageSupport groovy = LanguageSupport.of(GroovySyntax.INSTANCE);

        TextSession ongletJava = TextSession.of(
                "class A { int x() { return 1; } }\n", () -> 0L);
        TextSession ongletKotlin = TextSession.of(
                "class B { fun y(): Int = 2 }\n", () -> 0L);
        TextSession ongletGroovy = TextSession.of(
                "def c() { return 3 }\n", () -> 0L);

        // Les trois onglets s'ouvrent : trois premières passes.
        syntaxe.attacher(ongletJava, java);
        executeur.jouer();
        syntaxe.attacher(ongletKotlin, kotlin);
        executeur.jouer();
        syntaxe.attacher(ongletGroovy, groovy);
        executeur.jouer();
        int apresOuvertures = executeur.passees.get();
        assertTrue(apresOuvertures >= 3,
                "chaque onglet se paie sa première passe");

        // DIX bascules en ronde : Java → Kotlin → Groovy → Java → Kotlin
// → Groovy → Java… À l'équilibre, plus AUCUNE passe.
        TextSession[] ronde = { ongletJava, ongletKotlin, ongletGroovy };
        LanguageSupport[] langues = { java, kotlin, groovy };
        for (int i = 0; i < 10; i++) {
            syntaxe.attacher(ronde[i % 3], langues[i % 3]);
            executeur.jouer();
        }
        int apresBascules = executeur.passees.get();
        assertEquals(apresOuvertures, apresBascules,
                "dix bascules : le parc ressert les publications, "
                        + "AUCUNE re-tokenisation au retour");

        // La publication survit partout : chaque onglet garde SA couleur.
        assertNotNull(syntaxe.analyseCourante(ongletJava.document()),
                "l'onglet Java garde sa publication");
        assertNotNull(syntaxe.analyseCourante(ongletKotlin.document()),
                "l'onglet Kotlin garde sa publication");
        assertNotNull(syntaxe.analyseCourante(ongletGroovy.document()),
                "l'onglet Groovy garde sa publication");

        syntaxe.fermer();
    }

    /**
     * La frappe dans l'onglet ACTIF re-tokenise SEULEMENT lui : le
     * parc ne réveille pas les onglets dormants — leurs compteurs ne
     * bougent pas.
     */
    @Test
    void laFrappeActiveNeRetokeniseQueLactif() {
        ExecuteurCompte executeur = new ExecuteurCompte();
        EditorSyntax syntaxe = new EditorSyntax(new FoldModel(), executeur);
        LanguageSupport java = LanguageSupport.of(JavaSyntax.INSTANCE);
        LanguageSupport kotlin = LanguageSupport.of(KotlinSyntax.INSTANCE);

        TextSession ongletJava = TextSession.of(
                "class A { int x() { return 1; } }\n", () -> 0L);
        TextSession ongletKotlin = TextSession.of(
                "class B { fun y(): Int = 2 }\n", () -> 0L);
        syntaxe.attacher(ongletJava, java);
        executeur.jouer();
        syntaxe.attacher(ongletKotlin, kotlin);
        executeur.jouer();
        int equilibre = executeur.passees.get();

        // La frappe dans Kotlin (actif) : SEULEMENT Kotlin re-tokenise.
        assertTrue(ongletKotlin.replaceRange(0, 0, "// "));
        executeur.jouer();
        assertTrue(executeur.passees.get() > equilibre,
                "l'onglet actif re-tokenise à la frappe");
        int apresFrappe = executeur.passees.get();

        // Le retour à Java ne rejoue PAS la frappe de Kotlin : le parc
        // tient, la publication de Java n'a pas bougé.
        syntaxe.attacher(ongletJava, java);
        executeur.jouer();
        assertEquals(apresFrappe, executeur.passees.get(),
                "le retour à Java ne re-tokenise rien");

        syntaxe.fermer();
    }
}
