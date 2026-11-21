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
         * (les trente-trois langages livrés) — la source usuelle du kit.
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
     * Précharge le registre : l'assemblage ENTIER part sur
     * l'exécuteur du kit — le fil appelant rend LA MAIN IMMÉDIATEMENT
     * (le retour terrain du 2026-11-19 : « à chaque fois que j'ouvre
     * l'application démo c'est plus de 30 s d'attente » — l'ancien
     * chemin assemblait les 32 paquets sur le fil appelant, seul le
     * POSAGE du registre partait sur l'exécuteur). L'hôte qui veut
     * suivre l'assemblage sonde {@link #estPret()} ; les erreurs de
     * paquets partent au consommateur d'erreurs comme avant.
     */
    public void preload() {
        synchronized (this) {
            if (registre != null || assemblageLance) {
                return;
            }
            assemblageLance = true;
            finAssemblage = partirAssemblage();
        }
    }

    /** Part l'assemblage sur l'exécuteur et rend SON compte à
     *  rebours — l'appelant tient le verrou du kit (assemblageLance
     *  vient d'être posé). L'échec d'assemblage ne tue JAMAIS le kit
     *  (le retour terrain du 2026-11-19 : le fil d'assemblage mort
     *  par exception laissait le latch armé à vie — chaque
     *  résolution patientait 120 s puis lisait un registre vide).
     *  L'exception part au consommateur d'erreurs comme un paquet
     *  fautif, le compte à rebours vit en finally : la résolution
     *  repart sur le registre posé (vide au pire), jamais sur un
     *  latch mort. La GÉNÉRATION captée ici signe le résultat : un
     *  rechargement entre-temps la tourne, le résultat trop vieux se
     *  jette à la pose (audit H15). */
    private CountDownLatch partirAssemblage() {
        final CountDownLatch end = new CountDownLatch(1);
        final long generation = generationAssemblage;
        executor.execute(() -> {
            try {
                LanguageRegistry assemble = assembleOnThread();
                synchronized (LanguageKit.this) {
                    if (registre == null && generation == generationAssemblage) {
                        registre = assemble;
                    }
                }
            } catch (RuntimeException echec) {
                Consumer<Exception> rapporteur = reporter;
                if (rapporteur != null) {
                    rapporteur.accept(echec);
                }
            } finally {
                end.countDown();
            }
        });
        return end;
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
     *
     * <p>La méthode tourne la GÉNÉRATION d'assemblage (audit
     * coloration, piste H15) : un assemblage parti AVANT le
     * rechargement porte l'ancienne génération — son résultat se
     * jette à la pose, le registre périmé d'avant rechargement ne
     * gagne plus la course en silence ; l'assemblage relancé lit les
     * sources relues.</p>
     */
    public synchronized void reload() {
        generationAssemblage++;
        registre = null;
        assemblageLance = false;
    }

    /** Le registre assemblé — bâti une fois, relu ensuite. */
    private LanguageRegistry registre() {
        LanguageRegistry present = registre;
        if (present != null) {
            return present;
        }
        final CountDownLatch aAttendre;
        synchronized (this) {
            present = registre;
            if (present != null) {
                return present;
            }
            if (!assemblageLance) {
                // Le premier appelant PART l'assemblage : le verrou ne
                // survit pas à l'attente (le retour terrain du
                // 2026-11-19 : la résolution tenait le verrou DU KIT
                // en attendant le latch, le fil d'assemblage attendait
                // le même verrou pour POSER le registre — verrou mort
                // de 30 s, puis registre vide, aucune langue).
                assemblageLance = true;
                finAssemblage = partirAssemblage();
            }
            aAttendre = finAssemblage;
        }
        // L'attente se joue HORS du verrou : le fil d'assemblage le
        // prend pour poser le registre, le compte à rebours libère.
        attendre(aAttendre);
        LanguageRegistry fini = registre;
        return fini == null
                ? LanguageRegistry.builder().build()
                : fini;
    }

    /** Attend la fin d'un assemblage parti (au plus le délai du
     *  garde-fou, 120 s : les 33 paquets réels prennent jusqu'à
     *  une minute sur un appareil lent — l'ancien 30 s coupait
     *  l'assemblage en cours de route, la résolution repartait
     *  avec un registre VIDE, la langue n'arrivait jamais).
     *  Le fil qui résout ne pilote pas l'assemblage, il attend. */
    private static void attendre(
            @org.jspecify.annotations.Nullable CountDownLatch end) {
        if (end == null) {
            return;
        }
        try {
            end.await(120L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Assemble le registre SUR LE FIL COURANT (l'exécuteur du kit
     *  en production, le fil du banc en test direct) — les paquets
     *  fautifs s'y signalent au consommateur d'erreurs, le registre
     *  retourné ne se pose pas ici : l'appelant (preload ou registre)
     *  le pose sous le verrou.
     */
    private LanguageRegistry assembleOnThread() {
        final LanguagePlatform plateforme = factory == null
                ? LanguagePlatform.empty()
                : LanguagePlatform.with(factory);
        for (LanguageSupport support : supports) {
            plateforme.support(support);
        }
        for (PackSource source : sources) {
            // La tolérance vit PAR PAQUET (le retour terrain du
            // 2026-11-19) : le refus d'un paquet se confie au
            // consommateur quand il existe — sinon la surcharge
            // stricte jette au premier refus (le contrat public
            // d'addPackSource ne change pas).
            Consumer<Exception> rapporteur = reporter;
            if (rapporteur == null) {
                plateforme.addPackSource(source);
            } else {
                plateforme.addPackSource(source, rapporteur);
            }
        }
        return plateforme.build();
    }

    /** Le signal de fin de l'assemblage en cours, null sinon. */
    private volatile @Nullable CountDownLatch finAssemblage;

    /** La génération d'assemblage courante — tournée par chaque
     *  {@link #reload()} : le résultat d'une génération dépassée se
     *  jette à la pose (audit coloration, piste H15). Lue et écrite
     *  sous le moniteur du kit. */
    private long generationAssemblage;

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
                Thread thread = new Thread(runnable, "celeris-langues");
                thread.setDaemon(true);
                return thread;
            });
}
