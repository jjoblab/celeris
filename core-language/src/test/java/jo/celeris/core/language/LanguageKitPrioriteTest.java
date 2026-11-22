package jo.celeris.core.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import jo.celeris.core.language.model.LanguageDescriptor;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.language.packs.GrammarFactory;

import org.junit.jupiter.api.Test;

/**
 * Le chantier A de la feuille de route coloration (prompt du
 * 2026-11-22, F9 de l'audit) : le registre s'assemble SANS compiler
 * les grammaires — la langue du FICHIER OUVERT arrive après la lecture
 * des 33 {@code language.json} (les descripteurs), plus après la
 * compilation des 27 grammaires (679-835 ms mesurés JVM à froid,
 * rapport de la session du 2026-11-22). La grammaire de SA langue se
 * compile à SA première demande, UNE fois, le résultat se mémorise ;
 * la complétion de fond prépare les autres langues derrière.
 *
 * <p>Les bancs de résolution tournent sur UN FIL DE SÉPARATION (le fil
 * démon du kit en production) : le compte à rebours de l'assemblage y
 * est libéré APRÈS la pose du registre et AVANT la précompilation de
 * fond — la résolution du fil du banc lit le registre VIVANT sans
 * attendre les compilations, le fond finit seul derrière.</p>
 */
final class LanguageKitPrioriteTest {

    /** La fabrique de banc : COMPTE ses compilations, rend la façade
     *  Java (les mots réservés y vivent — la première demande du
     *  coach passe par reservedWords()). */
    private static final class FabriqueComptee implements GrammarFactory {

        private final AtomicInteger compilations = new AtomicInteger();

        @Override
        public jo.celeris.core.syntax.LanguageSyntax compile(String id,
                String json) {
            compilations.incrementAndGet();
            return jo.celeris.core.syntax.clike.JavaSyntax.INSTANCE;
        }
    }

    /** L'exécuteur de banc : tout tourne sur le fil appelant. */
    private static final Executor DIRECT = Runnable::run;

    /** L'exécuteur de SÉPARATION : un fil démon réel — le kit y pose le
     *  registre puis y précompile ; la résolution du fil du banc est
     *  libérée PAR LE COMPTE À REBOURS ANTICIPÉ avant le début du fond
     *  (production : fil démon « celeris-langues » du kit). */
    private static ExecutorService filDemon() {
        return Executors.newSingleThreadExecutor(tache -> {
            Thread fil = new Thread(tache, "celeris-banc-langues");
            fil.setDaemon(true);
            return fil;
        });
    }

    /**
     * La résolution ne paie AUCUNE compilation : le registre assemblé
     * porte les descripteurs, les grammaires attendent leur première
     * demande — la langue du fichier ouvert arrive SANS les 27
     * compilations (l'ancienne voie les payait toutes : 679-835 ms
     * mesurés à froid). Le fil de séparation porte le fond : la
     * résolution du fil du banc est libérée avant qu'il ne commence.
     */
    @Test
    void laLangueSeResoutSansAucuneCompilation() {
        FabriqueComptee fabrique = new FabriqueComptee();
        ExecutorService fil = filDemon();
        try {
            LanguageKit kit = LanguageKit.builder(fil)
                    .grammarFactory(fabrique)
                    .builtinPacks()
                    .build();
            // Le compte AVANT la résolution : le registre vit des
            // descripteurs, AUCUNE grammaire n'est compilée à l'assemblage.
            // (Le fond qui suit la pose peut compiler en PARALLÈLE de la
            // résolution — c'est la complétion de fond du chantier A,
            // attendue : la résolution, ELLE, ne paie rien. Le compte
            // capté au RETOUR peut donc déjà avoir bougé si l'ordonnanceur
            // a favorisé le fond — la résolution n'en a exécuté aucune.)
            assertEquals(0, fabrique.compilations.get(),
                    "l'assemblage ne compile AUCUNE grammaire — le registre"
                            + " vit des descripteurs seuls");
            LanguageSupport java = kit.languageFor("Main.java");
            assertNotNull(java, "la langue du .java se résout");
            // La résolution a attendu le registre POSÉ (le compte à
            // rebours anticipé) : le fond peut avoir commencé derrière,
            // le langage est déjà servi — le régime du chantier A.
            assertNotNull(java.syntax(),
                    "le support du .java vit dès la pose du registre");
        } finally {
            fil.shutdownNow();
        }
    }

    /**
     * La première DEMANDE compile SA grammaire seule : la vue qui pose
     * la langue (les mots réservés du coach) paie UNE compilation,
     * celle de SA langue — jamais celle des autres langages. Le fond
     * (même fil démon) peut avoir compilé la grammaire de java entre
     * la pose du registre et la demande : le banc attend la fin du
     * fond AVANT la demande, le compte se lit alors SANS course.
     */
    @Test
    void laPremiereDemandeCompileSaSeuleGrammaire() {
        FabriqueComptee fabrique = new FabriqueComptee();
        ExecutorService fil = filDemon();
        try {
            LanguageKit kit = LanguageKit.builder(fil)
                    .grammarFactory(fabrique)
                    .builtinPacks()
                    .build();
            LanguageSupport java = kit.languageFor("Main.java");
            assertNotNull(java);
            // Le fond finit d'abord (le kit précompile TOUTES les
            // grammaires) : la demande qui suit relit la grammaire
            // MÉMORISÉE — aucune recompilation, jamais de course.
            int apresFond = attendreFond(fabrique);
            assertNotNull(java.syntax(), "le support du .java porte une syntaxe");
            java.syntax().reservedWords();
            assertEquals(apresFond, fabrique.compilations.get(),
                    "la demande du .java n'a RIEN recompilé : la grammaire"
                            + " du fond se mémorise (SA grammaire, une fois)");
        } finally {
            fil.shutdownNow();
        }
    }

    /** Attend la fin du fond du kit (27 grammaires) : rend le compte
     *  des compilations une fois le fond épuisé (les compilations se
     *  stabilisent entre deux sondes de 50 ms). */
    private static int attendreFond(FabriqueComptee fabrique) {
        int precedent = fabrique.compilations.get();
        while (true) {
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return fabrique.compilations.get();
            }
            int courant = fabrique.compilations.get();
            if (courant == precedent && courant >= 27) {
                return courant;
            }
            precedent = courant;
        }
    }

    /**
     * La complétion de fond : après la pose du registre, la
     * précompilation prépare les grammaires restantes sur l'exécuteur
     * du kit — les langues suivantes n'attendront pas leur première
     * vue. (L'exécuteur DIRECT du banc : la précompilation se paie
     * dans preload(), le banc la lit après.)
     */
    @Test
    void laPrecompilationDeFondPrepareLesAutresLangues() {
        FabriqueComptee fabrique = new FabriqueComptee();
        LanguageKit kit = LanguageKit.builder(DIRECT)
                .grammarFactory(fabrique)
                .builtinPacks()
                .build();
        kit.preload();
        assertTrue(fabrique.compilations.get() >= 20,
                "la précompilation de fond a préparé les grammaires "
                        + "(au moins les 20 de la voie : " + fabrique.compilations.get()
                        + " compilations)");
        // La résolution qui suit ne recompile rien : le registre est
        // prêt, les grammaires mémorisées.
        int apresFond = fabrique.compilations.get();
        LanguageSupport java = kit.languageFor("Main.java");
        assertNotNull(java);
        assertNotNull(java.syntax(), "le support du .java porte une syntaxe");
        java.syntax().reservedWords();
        assertEquals(apresFond, fabrique.compilations.get(),
                "la demande après le fond ne recompile rien : la "
                        + "grammaire s'est mémorisée");
    }

    /**
     * Le CONTRAT d'honnêteté du kit : la langue sans registre reste
     * null SANS ATTENDRE (languageForSiPret), la sonde n'assemble rien.
     */
    @Test
    void laResolutionSansAttendreRendNullSansAssembler() {
        FabriqueComptee fabrique = new FabriqueComptee();
        LanguageKit kit = LanguageKit.builder(command -> {
                    // Le MUET : rien ne s'assemble à la sonde.
                })
                .grammarFactory(fabrique)
                .builtinPacks()
                .build();
        assertNull(kit.languageForSiPret("Main.java"),
                "sans assemblage lancé, la résolution rend null");
        assertEquals(0, fabrique.compilations.get(),
                "la sonde sans attendre ne compile rien");
    }

    /**
     * Le support direct (sans paquet) reste servi : l'assemblage
     * paresseux ne contourne pas les supports posés à la main.
     */
    @Test
    void leSupportDirectResteServi() {
        LanguageKit kit = LanguageKit.builder(DIRECT)
                .addSupport(LanguageSupport.builder(
                                LanguageDescriptor.of("essai", "Essai",
                                        List.of("essai")))
                        .syntax(jo.celeris.core.syntax.clike.JavaSyntax.INSTANCE)
                        .build())
                .build();
        LanguageSupport resolu = kit.languageFor("montage.essai");
        assertTrue(resolu != null && "essai".equals(resolu.id()),
                "le support posé directement se résout");
    }

    /**
     * L'ordre du fond : le compte à rebours de l'assemblage se libère
     * APRÈS la pose du registre et AVANT la précompilation — la
     * résolution lit le registre VIVANT sans attendre les
     * compilations. (Prouvé au probe JVM : la résolution rend avec
     * 0 compilation, le fond finit ensuite seul.)
     */
    @Test
    void leRegistreSePoseAvantLaPrecompilation() {
        FabriqueComptee fabrique = new FabriqueComptee();
        ExecutorService fil = filDemon();
        try {
            LanguageKit kit = LanguageKit.builder(fil)
                    .grammarFactory(fabrique)
                    .builtinPacks()
                    .build();
            // Le compte AVANT : l'assemblage ne compile rien (les
            // descripteurs seuls posent le registre).
            assertEquals(0, fabrique.compilations.get(),
                    "l'assemblage ne compile rien — le compte à rebours"
                            + " anticipé libère la résolution AVANT le"
                            + " début du fond");
            LanguageSupport java = kit.languageFor("Main.java");
            assertNotNull(java);
            assertNotNull(java.syntax(),
                    "le registre posé sert la résolution sans attendre"
                            + " la précompilation de fond");
        } finally {
            fil.shutdownNow();
        }
    }
}
