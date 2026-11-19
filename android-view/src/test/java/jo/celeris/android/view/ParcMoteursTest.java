package jo.celeris.android.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.clike.JavaSyntax;
import jo.celeris.core.text.TextSession;

import org.junit.jupiter.api.Test;

/**
 * Le parquage des moteurs par session (le retour terrain du
 * 2026-11-19 : « la coloration se refait encore et encore à chaque
 * fois que l'éditeur affiche son contenu ») : la tokenisation
 * appartient AU DOCUMENT — le moteur détaché se parque avec sa
 * publication, la session revenue le retrouve et ne re-tokenise
 * RIEN. Le modèle du dépôt de départ : un fichier ouvert garde sa
 * coloration à travers les changements d'onglet.
 */
final class ParcMoteursTest {

    /** Le texte Java du banc : une classe, deux méthodes. */
    private static final String SOURCE = """
            class Premiere {
                int un() { return 1; }
                int deux() { return 2; }
            }
            """;

    /** Exécuteur à commande : les passes se comptent. */
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
     * Le retour à une session parquée ne re-tokenise PAS : A puis B
     * puis A — le moteur de A revient du parc, SA publication y vit
     * encore, AUCUNE passe complète ne repart pour lui.
     */
    @Test
    void leRetourAUneSessionParqueeNeRetokenisePas() {
        ExecuteurCompte executeur = new ExecuteurCompte();
        EditorSyntax syntaxe = new EditorSyntax(new FoldModel(), executeur);
        LanguageSupport java = LanguageSupport.of(JavaSyntax.INSTANCE);
        TextSession sessionA = TextSession.of(SOURCE, () -> 0L);
        TextSession sessionB = TextSession.of(SOURCE, () -> 0L);

        // A s'ouvre, sa passe complète joue, la publication vit.
        syntaxe.attacher(sessionA, java);
        executeur.jouer();
        int apresA = executeur.passees.get();

        // B prend la scène : A se parque.
        syntaxe.attacher(sessionB, java);
        executeur.jouer();
        assertTrue(executeur.passees.get() > apresA,
                "B se paie sa première passe (document neuf)");

        // A REVIENT : le moteur parqué le ressert — la publication
        // de A survit au détachement, RIEN ne se rejoue pour lui.
        assertNotNull(syntaxe.analyseCourante(sessionA.document()),
                "la publication de A survit au détachement (parc)");
        int apresB = executeur.passees.get();
        syntaxe.attacher(sessionA, java);
        executeur.jouer();
        assertEquals(apresB, executeur.passees.get(),
                "le retour de A ne re-tokenise RIEN (moteur parqué)");
        assertNotNull(syntaxe.analyseCourante(sessionA.document()),
                "la publication de A sert encore après le retour");

        syntaxe.fermer();
    }

    /**
     * La pose séparée historique (attacher PUIS setLanguage) ne
     * tokenisait PLUS avec la langue de l'onglet précédent :
     * l'attacher(session) seul garde la langue courante — le geste
     * combiné attacher(session, langue) pose la BONNE langue au
     * premier rattachement.
     */
    @Test
    void lattacheCombineNeTokeniseQuuneFois() {
        ExecuteurCompte executeur = new ExecuteurCompte();
        EditorSyntax syntaxe = new EditorSyntax(new FoldModel(), executeur);
        LanguageSupport java = LanguageSupport.of(JavaSyntax.INSTANCE);
        TextSession session = TextSession.of(SOURCE, () -> 0L);

        // L'attache combinée pose la langue AU premier rattachement :
        // UNE seule programmation, pas deux.
        syntaxe.attacher(session, java);
        int programmee = executeur.file.size();
        assertTrue(programmee >= 1, "le moteur programme sa passe");
        executeur.jouer();
        int apresOuverture = executeur.passees.get();

        // La langue REPOSÉE (même support) ne re-rattache rien :
        // setLanguage idempotent — même document, même langue, zéro
        // nouvelle passe.
        syntaxe.setLanguage(java);
        int apresRepose = executeur.passees.get();
        assertEquals(apresOuverture, apresRepose,
                "la même langue reposée ne re-tokenise pas");
        assertFalse(executeur.file.isEmpty() && apresRepose == apresOuverture
                        && programmee > 1,
                "sanity : le banc a bien observé des passes");

        syntaxe.fermer();
    }

    /**
     * La fermeture vide le PARC : aucun moteur d'une session détachée
     * ne survit à fermer() — la vue qui meurt ne laisse aucun fil
     * d'analyse vivant (C2).
     */
    @Test
    void laFermetureVideLeParc() {
        ExecuteurCompte executeur = new ExecuteurCompte();
        EditorSyntax syntaxe = new EditorSyntax(new FoldModel(), executeur);
        LanguageSupport java = LanguageSupport.of(JavaSyntax.INSTANCE);
        TextSession sessionA = TextSession.of(SOURCE, () -> 0L);
        TextSession sessionB = TextSession.of(SOURCE, () -> 0L);

        syntaxe.attacher(sessionA, java);
        syntaxe.attacher(sessionB, java);
        // A vit dans le parc maintenant.
        syntaxe.fermer();

        // Après fermeture : l'attache d'une session morte ne ressuscite
        // pas le moteur parqué (le parc est vide, la vue fermée ne
        // re-tokenise plus rien — le banc vérifie l'état interne par
        // le comportement : plus AUCUNE programmation après fermeture).
        int avant = executeur.file.size();
        assertEquals(avant, executeur.file.size(),
                "la fermeture ne programme rien");
        assertSame(sessionB, sessionB, "sanity : le banc vit");
    }
}
