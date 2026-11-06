package jo.celeris.core.lsp;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;

/**
 * Le client LSP (ADR-005) : poignée de main, documents versionnés à
 * synchronisation complète, diagnostics et jetons sémantiques — deux
 * flux indépendants (PIEGE-18), jamais chaînés.
 *
 * <p>Modèle d'exécution (ADR-007) : un fil de travail propre consomme
 * la file des messages entrants — les réponses complètent leurs
 * futurs, les notifications alimentent les auditeurs (appelés sur ce
 * fil), les requêtes du serveur reçoivent l'erreur de méthode, la
 * surface bornée n'en supporte aucune. Les méthodes de document
 * s'appellent depuis le fil d'écriture de l'hôte ; les requêtes
 * rendent des futurs, joints par l'appelant.</p>
 *
 * <p>Les versions suivent la convention : ouverture à 1, chaque
 * changement à la version donnée par l'hôte, fermeture qui oublie.
 * La synchronisation est complète (le texte entier voyage à chaque
 * changement) — le choix robuste, documenté à l'ADR-005.</p>
 */
public final class ClientLsp implements AutoCloseable {

    /** L'auditeur des diagnostics publiés. */
    public interface AuditeurDiagnostics {

        /**
         * Des diagnostics publiés pour un document.
         *
         * @param uri        l'uri du document
         * @param diagnostics les diagnostics, le dernier état du serveur
         */
        void diagnostics(String uri, List<DiagnosticLsp> diagnostics);
    }

    /** L'auditeur des jetons sémantiques. */
    public interface AuditeurJetons {

        /**
         * Des jetons publiés pour un document.
         *
         * @param uri    l'uri du document
         * @param jetons les jetons, le dernier état du serveur
         */
        void jetons(String uri, JetonsSemantiques jetons);
    }

    private static final long DELAI_INITIALISATION = 30;

    private final Transport transport;
    private final String nomClient;
    private final Map<Long, CompletableFuture<ValeurJson>> enAttente =
            new ConcurrentHashMap<>();
    private final LinkedBlockingQueue<MessageRpc> entrants = new LinkedBlockingQueue<>();
    private final Map<String, Integer> versions = new ConcurrentHashMap<>();
    private final Map<String, List<DiagnosticLsp>> diagnostics = new ConcurrentHashMap<>();
    private final Map<String, JetonsSemantiques> jetons = new ConcurrentHashMap<>();
    private final List<AuditeurDiagnostics> auditeursDiagnostics =
            new java.util.concurrent.CopyOnWriteArrayList<>();
    private final List<AuditeurJetons> auditeursJetons =
            new java.util.concurrent.CopyOnWriteArrayList<>();
    private final Thread fil;
    private volatile boolean initialises;
    private volatile boolean ferme;
    private long compteurIds;
    private CapacitesServeur capacites = CapacitesServeur.vides();

    private ClientLsp(Transport transport, String nomClient) {
        this.transport = transport;
        this.nomClient = nomClient;
        this.fil = new Thread(this::travailler, "celeris-lsp-client");
        this.fil.setDaemon(true);
        transport.brancher(new Recepteur() {
            @Override
            public void recoit(String trame) {
                entrants.add(MessageRpc.de(JsonLecteur.lire(trame)));
            }
        });
        this.fil.start();
    }

    /**
     * Crée le client sur un transport.
     *
     * @param transport le transport vers le serveur
     * @param nomClient le nom du client, annoncé à l'initialisation
     * @return le client prêt à s'initialiser
     */
    public static ClientLsp sur(Transport transport, String nomClient) {
        return new ClientLsp(transport, nomClient);
    }

    /**
     * La poignée de main : requête initialize (bloquante, délai borné)
     * puis notification initialized.
     *
     * @param capacitesClient les capacités du client
     * @return les capacités du serveur
     * @throws ErreurLspException si le serveur refuse
     */
    public CapacitesServeur initialiser(JsonObjet capacitesClient) {
        exigerOuvert();
        JsonObjet params = JsonObjet.vide()
                .ajouter("clientInfo", JsonObjet.vide()
                        .ajouter("name", JsonChaine.of(nomClient)))
                .ajouter("capabilities", capacitesClient);
        ValeurJson resultat = joindre(requete("initialize", params), DELAI_INITIALISATION);
        capacites = CapacitesServeur.de(resultat);
        envoyer(new NotificationRpc("initialized", JsonObjet.vide()));
        initialises = true;
        return capacites;
    }

    /** Les capacités du serveur, lues à l'initialisation. */
    public CapacitesServeur capacites() {
        return capacites;
    }

    /**
     * Ouvre un document — version 1.
     *
     * @param uri        l'uri du document
     * @param languageId le langage déclaré
     * @param texte      le contenu initial
     */
    public void ouvrir(String uri, String languageId, String texte) {
        exigerInitialise();
        versions.put(uri, 1);
        envoyer(new NotificationRpc("textDocument/didOpen", JsonObjet.vide()
                .ajouter("textDocument", JsonObjet.vide()
                        .ajouter("uri", JsonChaine.of(uri))
                        .ajouter("languageId", JsonChaine.of(languageId))
                        .ajouter("version", JsonNombre.of(1))
                        .ajouter("text", JsonChaine.of(texte)))));
    }

    /**
     * Change un document — synchronisation complète, à la version donnée.
     *
     * @param uri     l'uri du document
     * @param version la nouvelle version
     * @param texte   le contenu complet
     */
    public void changer(String uri, int version, String texte) {
        exigerInitialise();
        versions.put(uri, version);
        envoyer(new NotificationRpc("textDocument/didChange", JsonObjet.vide()
                .ajouter("textDocument", JsonObjet.vide()
                        .ajouter("uri", JsonChaine.of(uri))
                        .ajouter("version", JsonNombre.of(version)))
                .ajouter("contentChanges", JsonTableau.vide()
                        .ajouter(JsonObjet.vide()
                                .ajouter("text", JsonChaine.of(texte))))));
    }

    /**
     * Ferme un document — l'état suivi est oublié.
     *
     * @param uri l'uri du document
     */
    public void fermer(String uri) {
        exigerInitialise();
        versions.remove(uri);
        envoyer(new NotificationRpc("textDocument/didClose", JsonObjet.vide()
                .ajouter("textDocument", JsonObjet.vide()
                        .ajouter("uri", JsonChaine.of(uri)))));
    }

    /**
     * La dernière version envoyée pour un document.
     *
     * @param uri l'uri du document
     * @return la version, ou -1 si le document n'est pas suivi
     */
    public int versionDe(String uri) {
        Integer version = versions.get(uri);
        return version == null ? -1 : version;
    }

    /** Les derniers diagnostics publiés pour un document — vide par défaut. */
    public List<DiagnosticLsp> diagnosticsDe(String uri) {
        return diagnostics.getOrDefault(uri, List.of());
    }

    /** Les derniers jetons sémantiques publiés pour un document — vide par défaut. */
    public JetonsSemantiques jetonsDe(String uri) {
        return jetons.getOrDefault(uri, JetonsSemantiques.de(JsonObjet.vide()));
    }

    /**
     * Tire les jetons sémantiques d'un document : requête au serveur,
     * réponse décodée, stockée et publiée à l'auditeur — un flux qui ne
     * passe JAMAIS par les diagnostics (PIEGE-18), les deux vivent seuls.
     *
     * @param uri l'uri du document
     * @return les jetons tirés, vides si le serveur n'en rend pas
     */
    public JetonsSemantiques tirerJetons(String uri) {
        exigerInitialise();
        ValeurJson resultat = joindre(requete("textDocument/semanticTokens/full",
                JsonObjet.vide().ajouter("textDocument", JsonObjet.vide()
                        .ajouter("uri", JsonChaine.of(uri)))), 5);
        JetonsSemantiques decode = JetonsSemantiques.de(resultat);
        jetons.put(uri, decode);
        for (AuditeurJetons auditeur : auditeursJetons) {
            auditeur.jetons(uri, decode);
        }
        return decode;
    }

    /** Branche un auditeur de diagnostics. */
    public void ajouterAuditeurDiagnostics(AuditeurDiagnostics auditeur) {
        auditeursDiagnostics.add(auditeur);
    }

    /** Branche un auditeur de jetons sémantiques. */
    public void ajouterAuditeurJetons(AuditeurJetons auditeur) {
        auditeursJetons.add(auditeur);
    }

    @Override
    public void close() {
        if (ferme) {
            return;
        }
        if (initialises) {
            try {
                joindre(requete("shutdown", null), 5);
            } catch (RuntimeException e) {
                // le serveur ne répond plus : la fermeture continue son chemin
                ignorerFermetureSansReponse();
            }
            envoyer(new NotificationRpc("exit", null));
        }
        ferme = true;
        transport.close();
        fil.interrupt();
    }

    /** Marque la fermeture silencieuse : le serveur ne répond plus, rien à faire. */
    private static void ignorerFermetureSansReponse() {
        // volontairement vide — le protocole ne prévoit rien de mieux
    }

    // ── La plomberie ──────────────────────────────────────────────────

    /** Émet une requête et rend son futur — la couche d'ordonnancement appelle ici. */
    CompletableFuture<ValeurJson> requete(String methode, @Nullable ValeurJson params) {
        exigerOuvert();
        CompletableFuture<ValeurJson> futur = new CompletableFuture<>();
        long id;
        synchronized (this) {
            compteurIds++;
            id = compteurIds;
            enAttente.put(id, futur);
        }
        envoyer(new RequeteRpc(id, methode, params));
        return futur;
    }

    private static ValeurJson joindre(CompletableFuture<ValeurJson> futur, long secondes) {
        try {
            return futur.get(secondes, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting for server", e);
        } catch (java.util.concurrent.ExecutionException e) {
            throw (RuntimeException) relancer(e.getCause());
        } catch (java.util.concurrent.TimeoutException e) {
            throw new IllegalStateException("server did not answer in time", e);
        }
    }

    private static Throwable relancer(@Nullable Throwable cause) {
        if (cause instanceof RuntimeException runtime) {
            return runtime;
        }
        return new IllegalStateException(cause);
    }

    private void envoyer(MessageRpc message) {
        transport.envoyer(JsonEcrivain.ecrire(message.versJson()));
    }

    private void exigerOuvert() {
        if (ferme) {
            throw new IllegalStateException("client is closed");
        }
    }

    private void exigerInitialise() {
        exigerOuvert();
        if (!initialises) {
            throw new IllegalStateException("client is not initialized");
        }
    }

    private void travailler() {
        while (!ferme) {
            try {
                distribuer(entrants.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void distribuer(MessageRpc message) {
        if (message instanceof ReponseRpc reponse) {
            completer(reponse);
        } else if (message instanceof NotificationRpc notification) {
            notifier(notification);
        } else {
            RequeteRpc requete = (RequeteRpc) message;
            envoyer(new ReponseRpc(requete.id(), null,
                    new ErreurRpc(ErreurRpc.METHOD_NOT_FOUND,
                            "method not found: " + requete.methode(), null)));
        }
    }

    private void completer(ReponseRpc reponse) {
        CompletableFuture<ValeurJson> futur = enAttente.remove(reponse.id());
        if (futur == null) {
            return;
        }
        if (reponse.erreur() != null) {
            futur.completeExceptionally(new ErreurLspException(reponse.erreur()));
        } else {
            futur.complete(reponse.resultat() == null ? JsonNul.nul() : reponse.resultat());
        }
    }

    private void notifier(NotificationRpc notification) {
        if ("textDocument/publishDiagnostics".equals(notification.methode())) {
            publierDiagnostics(notification.params());
        }
    }

    private void publierDiagnostics(@Nullable ValeurJson params) {
        if (!(params instanceof JsonObjet objet)) {
            return;
        }
        String uri = objet.membreTexte("uri");
        if (uri == null) {
            return;
        }
        List<DiagnosticLsp> liste = new java.util.ArrayList<>();
        if (objet.membre("diagnostics") instanceof JsonTableau tableau) {
            for (int i = 0; i < tableau.taille(); i++) {
                liste.add(DiagnosticLsp.de(tableau.element(i)));
            }
        }
        List<DiagnosticLsp> figee = List.copyOf(liste);
        diagnostics.put(uri, figee);
        for (AuditeurDiagnostics auditeur : auditeursDiagnostics) {
            auditeur.diagnostics(uri, figee);
        }
    }

}
