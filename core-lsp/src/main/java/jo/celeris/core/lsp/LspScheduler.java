package jo.celeris.core.lsp;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * L'ordonnancement des requêtes LSP (ADR-005) : annulation, priorités,
 * fusion, resynchronisation — la couche qu'il aurait fallu écrire
 * au-dessus de toute liaison tierce, la raison d'être du cœur.
 *
 * <p>Depuis la frontière protocolaire (ADR-015), l'ordonnanceur ne
 * connaît plus la voie qui porte les requêtes : il ordonnance des
 * futurs opaques. L'émetteur — comment la requête part vraiment — se
 * donne à chaque demande ; l'annulation au serveur passe par
 * {@link CompletableFuture#cancel(boolean)} du futur émis, la voie
 * sous-jacente y traduit la notification {@code $/cancelRequest} qui
 * convient. Les versions de documents viennent d'un
 * {@link VersionTracker}, pas d'un client.</p>
 *
 * <ul>
 *   <li><b>Fusion</b> : deux demandes identiques en vol (même famille,
 *   même identité) ne font QU'UNE requête sur le fil — les deux
 *   appelants partagent le futur gardé ;</li>
 *   <li><b>Annulation</b> : une demande plus fraîche de la même
 *   famille (clé uri + famille) remplace celle en vol — le futur émis
 *   est annulé, le futur visible échoue (REMPLACÉE) ;</li>
 *   <li><b>Priorités</b> : une demande interactive devance toute
 *   demande de fond en vol du même document (DEVANCÉE) — jamais
 *   l'inverse ;</li>
 *   <li><b>Resynchronisation</b> : une réponse qui arrive après un
 *   changement de version du document est périmée (PÉRIMÉE) —
 *   l'appelant l'apprend, il re-émet s'il veut.</li>
 * </ul>
 *
 * <p>Les règles ne sont pas des options : les frappes rapides
 * déclenchent des complétions en cascade, seule la dernière compte —
 * le dépôt de départ laissait tout partir et attendait tout.</p>
 */
public final class LspScheduler {

    /** La priorité d'une demande : interactive (l'utilisateur attend) ou de fond. */
    public enum Priority {

        /** L'utilisateur attend la réponse : survol, complétion, signature. */
        INTERACTIF,

        /** Travail de fond : formatage, jetons, symboles d'espace de travail. */
        FOND
    }

    /** La clé de remplacement : un document, une famille de demande. */
    public record Key(String uri, String family) {
    }

    /** Une requête en vol : son futur émis, son futur visible, sa priorité. */
    private static final class InFlight {
        private final CompletableFuture<?> emis;
        private final CompletableFuture<?> visible;
        private final Priority priorite;

        InFlight(CompletableFuture<?> emis, CompletableFuture<?> visible,
                Priority priorite) {
            this.emis = emis;
            this.visible = visible;
            this.priorite = priorite;
        }
    }

    /** La clé de fusion : une famille de demande, une identité de paramètres. */
    private record FusionKey(String family, Object identite) {
    }

    private final VersionTracker versions;
    private final Map<FusionKey, CompletableFuture<?>> fusions =
            new ConcurrentHashMap<>();
    private final Map<Key, InFlight> enVol = new ConcurrentHashMap<>();

    private LspScheduler(VersionTracker versions) {
        this.versions = versions;
    }

    /**
     * Crée l'ordonnanceur sur un suivi de versions.
     *
     * @param versions le suivi des documents synchronisés
     * @return l'ordonnanceur prêt à servir
     */
    public static LspScheduler on(VersionTracker versions) {
        return new LspScheduler(versions);
    }

    /**
     * Ordonnance une demande sous les quatre règles.
     *
     * @param <T>      le type de la réponse
     * @param family  la famille de demande (remplacement et fusion)
     * @param uri      l'uri du document porteur
     * @param priorite la priorité de la demande
     * @param identite l'identité des paramètres — deux demandes de
     *                 même famille et même identité en vol ne font
     *                 qu'une requête (l'objet doit avoir une égalité
     *                 de valeur)
     * @param emission comment la requête part — le futur rendu peut
     *                 être annulé par l'ordonnanceur : la voie
     *                 sous-jacente y traduit l'annulation au serveur
     * @return le futur de la réponse — il peut échouer en
     *         {@link RequestCancelledException}
     */
    public <T> CompletableFuture<T> schedule(String family, String uri,
            Priority priorite, Object identite,
            Supplier<CompletableFuture<T>> emission) {
        nettoyer();
        FusionKey cleFusion = new FusionKey(family, identite);
        CompletableFuture<?> fusionnee = fusions.get(cleFusion);
        if (fusionnee != null) {
            @SuppressWarnings("unchecked")
            CompletableFuture<T> partage = (CompletableFuture<T>) fusionnee;
            return partage;
        }
        Key cle = new Key(uri, family);
        remplacerEnVol(cle);
        if (priorite == Priority.INTERACTIF) {
            devancerLeFond(cle);
        }
        return emettre(cleFusion, cle, priorite, emission);
    }

    /** Élague les entrées terminées — les cartes ne portent que le vol, jamais l'histoire. */
    private void nettoyer() {
        fusions.values().removeIf(CompletableFuture::isDone);
        enVol.values().removeIf(enVol -> enVol.visible.isDone());
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private <T> CompletableFuture<T> emettre(FusionKey cleFusion, Key cle,
            Priority priorite, Supplier<CompletableFuture<T>> emission) {
        int versionAuDepart = versions.versionOf(cle.uri());
        CompletableFuture<T> visible = new CompletableFuture<>();
        CompletableFuture<T> emis;
        try {
            emis = emission.get();
        } catch (RuntimeException e) {
            visible.completeExceptionally(e);
            fusions.put(cleFusion, visible);
            return visible;
        }
        emis.whenComplete((reponse, echec) -> {
            if (visible.isDone()) {
                // remplacée ou devancée entre-temps : le verdict est rendu
                return;
            }
            if (echec != null) {
                visible.completeExceptionally(echec);
                return;
            }
            if (versions.versionOf(cle.uri()) != versionAuDepart) {
                visible.completeExceptionally(
                        new RequestCancelledException(RequestCancelledException.Reason.STALE));
                return;
            }
            visible.complete(reponse);
        });
        fusions.put(cleFusion, visible);
        enVol.put(cle, new InFlight(emis, visible, priorite));
        return visible;
    }

    private void remplacerEnVol(Key cle) {
        InFlight ancienne = enVol.get(cle);
        if (ancienne != null) {
            annuler(ancienne, RequestCancelledException.Reason.REMPLACEE);
        }
    }

    private void devancerLeFond(Key cle) {
        for (Map.Entry<Key, InFlight> entree : enVol.entrySet()) {
            if (entree.getKey().uri().equals(cle.uri())
                    && entree.getValue().priorite == Priority.FOND) {
                annuler(entree.getValue(), RequestCancelledException.Reason.SUPERSEDED);
            }
        }
    }

    /**
     * Annule un vol : le futur visible échoue pour sa raison, le futur
     * émis est annulé — la voie protocolaire y traduit la notification
     * {@code $/cancelRequest} (ADR-015).
     */
    private void annuler(InFlight enVol, RequestCancelledException.Reason reason) {
        enVol.visible.completeExceptionally(new RequestCancelledException(reason));
        enVol.emis.cancel(true);
    }
}
