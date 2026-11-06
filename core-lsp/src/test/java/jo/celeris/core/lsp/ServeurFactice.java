package jo.celeris.core.lsp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Le serveur factice des tests du client LSP : branché sur un bout de
 * paire en processus, il répond à l'initialisation, journalise tout ce
 * qu'il reçoit, sert des réponses scriptées par méthode et émet les
 * notifications du serveur (diagnostics, jetons) à la demande — tout
 * sur le fil de livraison, sans artifice d'ordonnancement.
 */
public final class ServeurFactice implements AutoCloseable {

    private final Transport transport;
    private final List<String> trames = new ArrayList<>();
    private final Map<String, ValeurJson> reponsesScriptees = new HashMap<>();
    private ValeurJson capacites = JsonObjet.vide();
    private final List<RequeteRpc> requetesEnAttente = new ArrayList<>();
    private @Nullable String methodeDifferee;
    private long compteurIds;

    private ServeurFactice(Transport transport) {
        this.transport = transport;
        transport.brancher(new Recepteur() {
            @Override
            public void recoit(String trame) {
                servir(trame);
            }
        });
    }

    /** Branche le serveur factice sur le bout serveur d'une paire. */
    public static ServeurFactice brancher(TransportMemoire.Paire paire) {
        return new ServeurFactice(paire.boutB());
    }

    private synchronized void servir(String trame) {
        trames.add(trame);
        MessageRpc message = MessageRpc.de(JsonLecteur.lire(trame));
        if (message instanceof RequeteRpc requete) {
            repondreA(requete);
        }
    }

    private void repondreA(RequeteRpc requete) {
        if (methodeDifferee != null && methodeDifferee.equals(requete.methode())) {
            requetesEnAttente.add(requete);
            return;
        }
        if ("initialize".equals(requete.methode())) {
            JsonObjet resultat = JsonObjet.vide().ajouter("capabilities", capacites);
            envoyerReponse(requete.id(), resultat, null);
        } else if ("shutdown".equals(requete.methode())) {
            envoyerReponse(requete.id(), JsonNul.nul(), null);
        } else if (reponsesScriptees.containsKey(requete.methode())) {
            envoyerReponse(requete.id(), reponsesScriptees.get(requete.methode()), null);
        } else {
            envoyerReponse(requete.id(), null,
                    new ErreurRpc(ErreurRpc.METHOD_NOT_FOUND,
                            "method not found: " + requete.methode(), null));
        }
    }

    private void envoyerReponse(long id, @Nullable ValeurJson resultat,
                                @Nullable ErreurRpc erreur) {
        transport.envoyer(JsonEcrivain.ecrire(new ReponseRpc(id, resultat, erreur).versJson()));
    }

    /** Les capacités que le serveur annoncera à l'initialisation. */
    public synchronized void capacites(ValeurJson capacites) {
        this.capacites = capacites;
    }

    /** Scripte la réponse d'une méthode — les requêtes suivantes la recevront. */
    public synchronized void reponseScriptee(String methode, ValeurJson resultat) {
        reponsesScriptees.put(methode, resultat);
    }

    /** Les trames reçues depuis la création, dans l'ordre. */
    public synchronized List<String> tramesRecues() {
        return List.copyOf(trames);
    }

    /** Émet une notification vers le client. */
    public synchronized void notifier(String methode, @Nullable ValeurJson params) {
        transport.envoyer(JsonEcrivain.ecrire(new NotificationRpc(methode, params).versJson()));
    }

    /** Émet une requête du serveur vers le client — rend l'identifiant émis. */
    public synchronized long envoyerRequete(String methode, @Nullable ValeurJson params) {
        compteurIds++;
        transport.envoyer(JsonEcrivain.ecrire(
                new RequeteRpc(compteurIds, methode, params).versJson()));
        return compteurIds;
    }

    /** Retient les requêtes de la méthode au lieu d'y répondre — le test les achèvera. */
    public synchronized void reponseDifferee(String methode) {
        this.methodeDifferee = methode;
    }

    /** Acheve la plus ancienne requête retenue de la méthode — répond au client. */
    public synchronized void acheverRequete(String methode, ValeurJson resultat) {
        for (int i = 0; i < requetesEnAttente.size(); i++) {
            RequeteRpc requete = requetesEnAttente.get(i);
            if (requete.methode().equals(methode)) {
                requetesEnAttente.remove(i);
                envoyerReponse(requete.id(), resultat, null);
                return;
            }
        }
        throw new IllegalStateException("no held request for " + methode);
    }

    /** Le nombre de requêtes retenues pour la méthode. */
    public synchronized int requetesEnAttente(String methode) {
        int compteur = 0;
        for (RequeteRpc requete : requetesEnAttente) {
            if (requete.methode().equals(methode)) {
                compteur++;
            }
        }
        return compteur;
    }

    @Override
    public void close() {
        transport.close();
    }
}
