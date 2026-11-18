package jo.celeris.core.language;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.language.packs.GrammarFactory;
import jo.celeris.core.language.packs.PackSource;
import jo.celeris.core.language.packs.Packs;

import org.jspecify.annotations.Nullable;

/**
 * Le chargeur de langues du kit (ADR-0022, phase 1 — T5) : le registre
 * assemblé de la plateforme, monté UNE fois hors fil d'appel (ADR-007)
 * et PARTAGÉ entre éditeurs — l'hôte qui n'utilise pas la façade y
 * résout « la langue de ce nom de fichier » sans connaître la
 * plateforme.
 *
 * <p>La résolution est sûre avant la fin de l'assemblage : tant que le
 * registre ne vit pas, {@link #languageFor(String)} rend null — la vue
 * reste éditable sans coloration, la façade y applique la langue dès
 * qu'elle arrive (ADR-0022). Le rechargement à chaud relit les sources
 * au prochain assemblage ; les langues déjà servies aux sessions
 * ouvertes ne bougent pas (USAGE § 6).</p>
 *
 * <p>Un paquet illisible ne fait pas tomber l'éditeur : l'erreur va au
 * consommateur posé au montage (s'il y en a un) et le kit rend le
 * registre des sources lisibles restantes — la vue reste éditable,
 * l'hôte décide. L'assemblage part sur l'exécuteur injecté (le banc y
 * tourne sur le fil appelant, la production sur le fil démon partagé
 * du kit — jamais un fil par vue, chantier C2).</p>
 */
public final class LanguageKit {

    /** Le monte-charge du kit : les sources s'y posent avant le
     *  premier assemblage, la fabrique de grammaires s'y injecte. */
    public static final class Builder {

        private final Executor executor;
        private final List<PackSource> sources = new ArrayList<>();
        private final List<LanguageSupport> supports = new ArrayList<>();
        private @Nullable GrammarFactory factory;
        private @Nullable Consumer<Exception> reporter;

        private Builder(Executor executor) {
            this.executor = executor;
        }

        /**
         * Pose la fabrique de grammaires de l'hôte — la voie
         * déclarative (core-syntax-grammars) l'implémente ; sans
         * fabrique, seuls les analyseurs écrits se résolvent.
         *
         * @param grammars la fabrique de grammaires, null pour aucun
         * @return le monte-charge
         */
        public Builder grammarFactory(@Nullable GrammarFactory grammars) {
            this.factory = grammars;
            return this;
        }

        /**
         * Ajoute une source de paquets : elle se lit à l'assemblage.
         *
         * @param source la source des paquets
         * @return le monte-charge
         */
        public Builder addSource(PackSource source) {
            sources.add(source);
            return this;
        }

        /**
         * Pose un support de langage directement — la voie sans
         * paquet : l'analyse écrit à la main (WrittenLanguages) ou le
         * support de l'hôte s'y déclare tel quel.
         *
         * @param support le support de langage
         * @return le monte-charge
         */
        public Builder addSupport(LanguageSupport support) {
            supports.add(support);
            return this;
        }

        /**
         * Les paquets intégrés du chemin « langages » du classpath
         * (les trente-deux langages livrés) — la source usuelle du kit.
         *
         * @return le monte-charge
         */
        public Builder builtinPacks() {
            return addSource(Packs.classpath("langages"));
        }

        /**
         * Le consommateur des erreurs d'assemblage : sans lui, un
         * paquet illisible lève à la première résolution ; avec lui,
         * l'erreur lui est confiée et le kit continue sans le paquet
         * fautif — la vue reste éditable, l'hôte décide.
         *
         * @param reporting le consommateur des erreurs d'assemblage
         * @return le monte-charge
         */
        public Builder onError(Consumer<Exception> reporting) {
            this.reporter = reporting;
            return this;
        }

        /**
         * Assemble le kit : l'assemblage part sur l'exécuteur injecté,
         * les résolutions qui suivent attendent sa fin.
         *
         * @return le kit prêt à résoudre
         */
        public LanguageKit build() {
            return new LanguageKit(executor, factory, List.copyOf(sources),
                    List.copyOf(supports), reporter);
        }
    }

    /**
     * Le monte-charge du kit sur l'exécuteur donné.
     *
     * @param executor l'exécuteur de l'assemblage (hors fil d'appel en
     *                 production, le banc y tourne direct)
     * @return le monte-charge
     */
    public static Builder builder(Executor executor) {
        return new Builder(executor);
    }

    private final Executor executor;
    private final @Nullable GrammarFactory factory;
    private final List<PackSource> sources;
    private final List<LanguageSupport> supports;
    private final @Nullable Consumer<Exception> reporter;
    private volatile @Nullable LanguageRegistry registre;
    private volatile boolean assemblageLance;

    private LanguageKit(Executor executor, @Nullable GrammarFactory factory,
            List<PackSource> sources, List<LanguageSupport> supports,
            @Nullable Consumer<Exception> reporter) {
        this.executor = executor;
        this.factory = factory;
        this.sources = sources;
        this.supports = supports;
        this.reporter = reporter;
    }

    /**
     * Précharge le registre : l'assemblage part immédiatement sur
     * l'exécuteur du kit — l'hôte qui veut la langue prête à la
     * première ouverture l'appelle au démarrage.
     */
    public void preload() {
        registre();
    }

    /**
     * La langue d'un nom de fichier — null sans langage et pendant
     * l'assemblage (jamais d'exception : la vue reste éditable sans
     * coloration entre-temps, la façade applique la langue dès qu'elle
     * arrive). La résolution attend la fin de l'assemblage en cours.
     *
     * @param filename le nom du fichier ouvert
     * @return le support résolu, null sans langage
     */
    public @Nullable LanguageSupport languageFor(String filename) {
        return registre().byFilename(filename);
    }

    /**
     * La langue d'un nom de fichier SANS ATTENDRE : null tant que
     * l'assemblage court (jamais de blocage du fil appelant — la revue
     * du propriétaire du 2026-11-19 : l'ouverture de l'éditeur
     * patientait l'assemblage des paquets, l'écran restait noir).
     * L'hôte asynchrone y lit, puis pose la langue quand l'assemblage
     * vit — {@link #estPret()} le dit, {@link #languageFor} résout
     * alors sans attente.
     *
     * @param filename le nom du fichier ouvert
     * @return le support résolu, null sans langage ou pendant l'assemblage
     */
    public @Nullable LanguageSupport languageForSiPret(String filename) {
        LanguageRegistry present = registre;
        return present == null ? null : present.byFilename(filename);
    }

    /**
     * Le registre est-il assemblé — la résolution sans attente peut
     * commencer. L'hôte y sonde, le poller léger ou l'avis du fil
     * démon y ramène.
     *
     * @return vrai quand le registre vit
     */
    public boolean estPret() {
        return registre != null;
    }

    /**
     * Recharge à chaud : les sources se relisent au prochain
     * assemblage — le langage ajouté au stockage entre, le retiré
     * sort. Les sessions ouvertes gardent leur langage (USAGE § 6) ;
     * seuls les prochains fichiers parlent au registre rechargé.
     */
    public void reload() {
        registre = null;
        assemblageLance = false;
    }

    /** Le registre assemblé — bâti une fois, relu ensuite. */
    private LanguageRegistry registre() {
        LanguageRegistry present = registre;
        if (present != null) {
            return present;
        }
        synchronized (this) {
            present = registre;
            if (present != null) {
                return present;
            }
            // L'assemblage est déjà parti : la résolution attend sa
            // fin — le contrat du kit (« la langue arrive dès que le
            // registre vit ») prime sur le rendu immédiat.
            if (assemblageLance) {
                attendreAssemblage();
                LanguageRegistry assemble = registre;
                return assemble == null
                        ? LanguageRegistry.builder().build()
                        : assemble;
            }
            assemblageLance = true;
            final LanguageRegistry assemble = assembler();
            if (assemble != null) {
                registre = assemble;
                return assemble;
            }
            attendreAssemblage();
            LanguageRegistry fini = registre;
            return fini == null
                    ? LanguageRegistry.builder().build()
                    : fini;
        }
    }

    /** Attend la fin de l'assemblage parti (au plus le délai du
     *  garde-fou) — le fil qui résout ne pilote pas l'assemblage,
     *  il attend son terme. */
    private void attendreAssemblage() {
        CountDownLatch fin = finAssemblage;
        if (fin != null) {
            try {
                fin.await(30L, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** Lance l'assemblage sur l'exécuteur — une seule fois, les
     *  paquets fautifs s'y signalent au consommateur d'erreurs. */
    private @Nullable LanguageRegistry assembler() {
        final LanguagePlatform plateforme = factory == null
                ? LanguagePlatform.empty()
                : LanguagePlatform.with(factory);
        for (LanguageSupport support : supports) {
            plateforme.support(support);
        }
        for (PackSource source : sources) {
            try {
                plateforme.addPackSource(source);
            } catch (RuntimeException e) {
                Consumer<Exception> rapporteur = reporter;
                if (rapporteur == null) {
                    throw e;
                }
                rapporteur.accept(e);
            }
        }
        final LanguageRegistry assemble = plateforme.build();
        final CountDownLatch fin = new CountDownLatch(1);
        finAssemblage = fin;
        executor.execute(() -> {
            synchronized (LanguageKit.this) {
                registre = assemble;
            }
            fin.countDown();
        });
        // L'exécuteur peut être direct (le banc) : le registre est
        // déjà posé au retour d'execute — le ramener tout de suite.
        LanguageRegistry pose = registre;
        return pose == assemble ? assemble : null;
    }

    /** Le signal de fin de l'assemblage en cours, null sinon. */
    private volatile @Nullable CountDownLatch finAssemblage;

    /**
     * Le fil démon partagé du kit (ADR-007, chantier C2) : UN fil pour
     * tous les éditeurs du processus, jamais un fil par vue — la
     * production y monte son assemblage.
     *
     * @return l'exécuteur du fil démon « celeris-langues »
     */
    public static Executor sharedDaemon() {
        return FIL_PARTAGE;
    }

    private static final java.util.concurrent.ExecutorService FIL_PARTAGE =
            Executors.newSingleThreadExecutor(runnable -> {
                Thread fil = new Thread(runnable, "celeris-langues");
                fil.setDaemon(true);
                return fil;
            });
}
