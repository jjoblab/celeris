package jo.celeris.core.intelligence;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * L'historique d'acceptation des candidats (C6) — le moteur de
 * l'apprentissage du classement local. Chaque acceptation du popup
 * s'enregistre par couple (langage, identifiant de tête) : le nombre
 * d'acceptations et la fraîcheur de la dernière. Au prochain
 * classement, un candidat déjà accepté remonte à palier et groupe
 * égaux — l'apprentissage ne franchit JAMAIS la barrière d'un palier
 * ni d'un groupe (un candidat déjà accepté mais apparié plus
 * lâchement reste derrière un candidat plus précis jamais accepté),
 * et à l'intérieur d'un couple (palier, groupe) l'ordre du
 * fournisseur reste la référence pour les candidats hors historique.
 *
 * <p>Le score encode une force décroissante : fréquence d'abord,
 * fraîcheur ensuite (à fréquence égale, le plus récemment accepté
 * passe devant) ; un candidat inconnu vaut 0 — neutre pour un tri
 * stable. La persistance passe par une photographie immuable
 * ({@link Snapshot}) que l'hôte sérialise comme il l'entend ; un
 * snapshot de version inconnue est refusé d'exception, jamais
 * interprété à l'aveugle. Les garde-fous : capacité bornée par
 * langage (l'entrée au score le plus faible est évincée), bascule
 * globale (la collecte et le boost s'éteignent ensemble, sans perte
 * de l'historique collecté), purge totale ou par langage, les
 * langages en compartiments étanches. Classe synchronisée : le popup
 * enregistre et consulte sur un fil, la purge peut venir d'un
 * réglage hôte.</p>
 *
 * <p>L'algorithme descend de l'historique du dépôt de départ
 * (registre de portage, C6 — Spécifier : l'ancien sert d'oracle).</p>
 */
public final class AcceptanceHistory {

    /** Capacité maximale d'identifiants par langage (éviction du plus faible). */
    public static final int PER_LANGUAGE_CAPACITY = 200;

    /**
     * La photographie immuable de l'historique — l'unité de
     * persistance entre sessions de l'hôte. La structure est plate :
     * langage vers identifiant vers paire (fréquence, séquence) ; la
     * séquence est le compteur interne de fraîcheur, opaque pour
     * l'hôte — elle doit simplement faire l'aller-retour intacte.
     */
    public static final class Snapshot {

        /** Version courante du format. */
        public static final int FORMAT_VERSION = 1;

        private final int version;

        private final Map<String, Map<String, long[]>> byLanguage;

        /**
         * La photographie depuis des données sérialisées par l'hôte.
         *
         * @param entries les entrées : langage vers identifiant vers
         *                paire (fréquence, séquence)
         * @param version la version du format au moment de la
         *                sauvegarde
         */
        public Snapshot(Map<String, Map<String, long[]>> entries, int version) {
            Map<String, Map<String, long[]>> copy = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, long[]>> language : entries.entrySet()) {
                Map<String, long[]> compartment = new LinkedHashMap<>();
                for (Map.Entry<String, long[]> identifier : language.getValue().entrySet()) {
                    compartment.put(identifier.getKey(), identifier.getValue().clone());
                }
                copy.put(language.getKey(), compartment);
            }
            this.byLanguage = copy;
            this.version = version;
        }

        /** La version des données portées. */
        public int version() {
            return version;
        }

        /** Les entrées photographiées, copie défensive. */
        public Map<String, Map<String, long[]>> entries() {
            Map<String, Map<String, long[]>> copy = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, long[]>> language : byLanguage.entrySet()) {
                Map<String, long[]> compartment = new LinkedHashMap<>();
                for (Map.Entry<String, long[]> identifier : language.getValue().entrySet()) {
                    compartment.put(identifier.getKey(), identifier.getValue().clone());
                }
                copy.put(language.getKey(), compartment);
            }
            return copy;
        }
    }

    private final Map<String, Map<String, long[]>> byLanguage = new LinkedHashMap<>();

    private long sequence;

    private long recordedAcceptances;

    private long boostedAcceptances;

    private boolean enabled = true;

    /**
     * Enregistre l'acceptation d'un candidat : la fréquence monte,
     * la fraîcheur se renouvelle. La bascule globale coupée n'enregistre
     * rien — l'historique déjà collecté reste.
     *
     * @param language   le langage du candidat (compartiment étanche)
     * @param identifier l'identifiant de tête accepté
     */
    public synchronized void record(String language, String identifier) {
        if (!enabled) {
            return;
        }
        sequence++;
        recordedAcceptances++;
        byLanguage.computeIfAbsent(language, l -> new LinkedHashMap<>())
                .merge(identifier, new long[]{1, sequence}, (avant, nouveau) -> {
                    avant[0] += 1;
                    avant[1] = sequence;
                    return avant;
                });
        evict(language);
    }

    /**
     * Le score d'apprentissage du candidat : fréquence puis fraîcheur,
     * 0 pour un inconnu — neutre pour un tri stable. La bascule
     * globale coupée rend 0 à tous.
     *
     * @param language   le langage du candidat
     * @param identifier l'identifiant de tête
     * @return le score, 0 pour un inconnu ou une bascule coupée
     */
    public synchronized long boost(String language, String identifier) {
        if (!enabled) {
            boostedAcceptances++;
            return 0;
        }
        long[] entry = entryOf(language, identifier);
        if (entry == null) {
            return 0;
        }
        boostedAcceptances++;
        return entry[0] * 1_000_000_000L + entry[1];
    }

    /** La bascule globale : la collecte et le boost s'éteignent ensemble. */
    public synchronized void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** Vrai quand la collecte et le boost sont allumés. */
    public synchronized boolean isEnabled() {
        return enabled;
    }

    /** La purge totale : l'historique disparaît, les compteurs aussi. */
    public synchronized void clear() {
        byLanguage.clear();
        recordedAcceptances = 0;
        boostedAcceptances = 0;
    }

    /** La purge d'un langage : les autres compartiments restent. */
    public synchronized void clear(String language) {
        byLanguage.remove(language);
    }

    /** Le nombre d'identifiants appris, tous langages compris. */
    public synchronized int size() {
        int size = 0;
        for (Map<String, long[]> compartment : byLanguage.values()) {
            size += compartment.size();
        }
        return size;
    }

    /** Le nombre d'acceptations enregistrées depuis la purge totale. */
    public synchronized long recordedAcceptanceCount() {
        return recordedAcceptances;
    }

    /** Le nombre de consultations de boost depuis la purge totale. */
    public synchronized long boostedAcceptanceCount() {
        return boostedAcceptances;
    }

    /**
     * La photographie de l'état appris — l'hôte la sérialise comme il
     * l'entend.
     *
     * @return la photographie à la version courante
     */
    public synchronized Snapshot snapshot() {
        return new Snapshot(byLanguage, Snapshot.FORMAT_VERSION);
    }

    /**
     * Remplace intégralement l'état appris par la photographie. La
     * fraîcheur rebasse le compteur interne : une acceptation TOUTE
     * récente ne peut jamais paraître plus vieille qu'une entrée
     * restaurée.
     *
     * @param snapshot la photographie à restaurer
     * @throws IllegalArgumentException si la version de la photographie
     *         n'est pas la version courante — jamais interprétée à l'aveugle
     */
    public synchronized void restore(Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot.version() != Snapshot.FORMAT_VERSION) {
            throw new IllegalArgumentException(
                    "snapshot version " + snapshot.version() + " is not supported");
        }
        byLanguage.clear();
        for (Map.Entry<String, Map<String, long[]>> language
                : snapshot.entries().entrySet()) {
            Map<String, long[]> compartment = new LinkedHashMap<>();
            for (Map.Entry<String, long[]> identifier : language.getValue().entrySet()) {
                compartment.put(identifier.getKey(), identifier.getValue().clone());
                sequence = Math.max(sequence, identifier.getValue()[1]);
            }
            byLanguage.put(language.getKey(), compartment);
        }
    }

    /** L'entrée apprise, ou null. */
    private long @org.jspecify.annotations.Nullable [] entryOf(String language,
            String identifier) {
        Map<String, long[]> compartment = byLanguage.get(language);
        return compartment == null ? null : compartment.get(identifier);
    }

    /** L'éviction du plus faible quand le compartiment dépasse la capacité. */
    private void evict(String language) {
        Map<String, long[]> compartment = byLanguage.get(language);
        if (compartment == null || compartment.size() <= PER_LANGUAGE_CAPACITY) {
            return;
        }
        String weakest = null;
        long weakestScore = Long.MAX_VALUE;
        for (Map.Entry<String, long[]> entry : compartment.entrySet()) {
            long score = entry.getValue()[0] * 1_000_000_000L + entry.getValue()[1];
            if (score < weakestScore) {
                weakestScore = score;
                weakest = entry.getKey();
            }
        }
        compartment.remove(weakest);
    }
}
