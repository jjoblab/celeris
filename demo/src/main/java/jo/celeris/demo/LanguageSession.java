package jo.celeris.demo;

import java.util.concurrent.CompletableFuture;

import jo.celeris.android.widgets.CompletionDocModel;
import jo.celeris.android.widgets.SignatureAideModel;
import jo.celeris.core.lsp.DiagnosticLsp;
import jo.celeris.core.lsp.ListeCompletion;
import jo.celeris.core.text.Document;

import org.jspecify.annotations.Nullable;

/**
 * La session de langage de la démonstration (M8.6) : la surface commune
 * des serveurs que l'atelier des popups consomme — le serveur de
 * référence en mémoire (M7.5, §8) comme le serveur XML RÉEL embarqué,
 * lancé en processus sur des tubes mémoire. L'hôte n'en connaît que
 * cette porte : ouvrir, synchroniser, et les trois canaux du protocole
 * — signature, survol, complétion — plus les diagnostics publiés.
 */
interface LanguageSession extends AutoCloseable {

    /**
     * Ouvre le document suivi — version 1, synchronisation complète.
     *
     * @param uri    l'uri du document
     * @param langue l'identifiant du langage
     * @param texte  le contenu initial
     */
    void ouvrir(String uri, String langue, String texte);

    /**
     * La synchronisation complète : le texte entier, la version
     * suivante — l'état du serveur suit la session d'édition.
     *
     * @param uri   l'uri du document
     * @param texte le contenu complet courant
     */
    void synchroniser(String uri, String texte);

    /**
     * L'aide aux paramètres au curseur — le futur du modèle de la
     * vue, déjà converti (B-07) ; null dans le futur sans signature.
     *
     * @param uri   l'uri du document
     * @param doc   l'instantané courant
     * @param caret la position du curseur
     * @return le futur du modèle, null si le serveur n'en dit rien
     */
    CompletableFuture<@Nullable SignatureAideModel> signature(String uri,
            Document doc, int caret);

    /**
     * Le survol du symbole au curseur — le futur du modèle de
     * documentation, déjà converti (B-09) ; null sans documentation.
     *
     * @param uri      l'uri du document
     * @param doc      l'instantané courant
     * @param symbole  le symbole survolé — le titre du modèle
     * @param caret    la position du curseur
     * @return le futur du modèle, null si le serveur n'en dit rien
     */
    CompletableFuture<@Nullable CompletionDocModel> survol(String uri,
            Document doc, String symbole, int caret);

    /**
     * Vrai quand le serveur SERT la complétion par le protocole —
     * l'hôte y préfère alors ses mots locaux.
     *
     * @return vrai quand la complétion passe par le serveur
     */
    boolean sertCompletion();

    /**
     * La complétion au curseur, la réponse BRUTE du protocole —
     * l'hôte en tire ses candidats typés et leur documentation.
     *
     * @param uri   l'uri du document
     * @param doc   l'instantané courant
     * @param caret la position du curseur
     * @return le futur de la liste de complétion du protocole
     */
    CompletableFuture<ListeCompletion> completion(String uri, Document doc,
            int caret);

    /**
     * Pose l'auditeur des diagnostics publiés — un serveur muet ne
     * prévient jamais, l'auditeur reste silencieux.
     *
     * @param auditeur l'auditeur des diagnostics du protocole
     */
    void poserAuditeurDiagnostics(
            java.util.function.BiConsumer<String, java.util.List<DiagnosticLsp>> auditeur);

    /** Ferme la session — transports et piscines, sans exception vérifiée. */
    @Override
    void close();
}
