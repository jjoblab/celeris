package jo.celeris.demo;

import java.util.concurrent.CompletableFuture;

import jo.celeris.android.widgets.CompletionDocModel;
import jo.celeris.android.widgets.SignatureAideModel;
import java.util.List;

import jo.celeris.core.lsp.CorrectifLsp;
import jo.celeris.core.lsp.DiagnosticLsp;
import jo.celeris.core.lsp.EditionEspaceLsp;
import jo.celeris.core.lsp.EditionTexteLsp;
import jo.celeris.core.lsp.IndiceLsp;
import jo.celeris.core.lsp.SemanticTokensLsp;
import jo.celeris.core.lsp.LieuLsp;
import jo.celeris.core.lsp.ListeCompletion;
import jo.celeris.core.lsp.OptionsFormatageLsp;
import jo.celeris.core.lsp.PlageLsp;
import jo.celeris.core.lsp.PliLsp;
import jo.celeris.core.lsp.PreparationRennomageLsp;
import jo.celeris.core.lsp.SurlignageLsp;
import jo.celeris.core.lsp.SymboleLsp;
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

    // ── La couverture du jalon (H-12) : chaque méthode du protocole
    //    raccordée à cette porte — le serveur qui n'en sert pas rend
    //    ses vides du domaine, l'hôte n'attrape jamais de protocole. ──

    /**
     * Les symboles du document (H-12) — le plan, le fil d'Ariane.
     *
     * @param uri l'uri du document
     * @return le futur des symboles
     */
    CompletableFuture<List<SymboleLsp>> symboles(String uri);

    /**
     * Les plis du serveur (H-12), fin EXCLUSIVE — le consommateur
     * fusionne avec l'arbre commun.
     *
     * @param uri l'uri du document
     * @return le futur des plis
     */
    CompletableFuture<List<PliLsp>> plis(String uri);

    /**
     * Le formatage du document (H-12) — la capacité Formatter branchée.
     *
     * @param uri     l'uri du document
     * @param options les options de formatage
     * @return le futur des éditions de texte
     */
    CompletableFuture<List<EditionTexteLsp>> formatage(String uri,
            OptionsFormatageLsp options);

    /**
     * Le formatage d'une plage (H-12) — le remaniement local.
     *
     * @param uri     l'uri du document
     * @param plage   la plage à formater
     * @param options les options de formatage
     * @return le futur des éditions de texte
     */
    CompletableFuture<List<EditionTexteLsp>> formatagePlage(String uri,
            PlageLsp plage, OptionsFormatageLsp options);

    /**
     * Les surlignages d'occurrences sous le curseur (H-12).
     *
     * @param uri   l'uri du document
     * @param doc   l'instantané courant
     * @param caret la position du curseur
     * @return le futur des surlignages
     */
    CompletableFuture<List<SurlignageLsp>> surlignages(String uri,
            Document doc, int caret);

    /**
     * Les plages de sélection englobantes (H-12) — l'extension de la
     * sélection, de la plus intime à la plus large.
     *
     * @param uri   l'uri du document
     * @param doc   l'instantané courant
     * @param caret la position de départ
     * @return le futur des plages englobantes
     */
    CompletableFuture<List<PlageLsp>> plagesSelection(String uri,
            Document doc, int caret);

    /**
     * La préparation du renommage (H-12) — null sans préparation.
     *
     * @param uri   l'uri du document
     * @param doc   l'instantané courant
     * @param caret la position du nom
     * @return le futur de la préparation
     */
    CompletableFuture<@Nullable PreparationRennomageLsp> preparerRennomage(
            String uri, Document doc, int caret);

    /**
     * Le renommage (H-12) — l'édition d'espace qui porte le nouveau nom.
     *
     * @param uri        l'uri du document
     * @param doc        l'instantané courant
     * @param caret      la position du nom
     * @param nouveauNom le nom qui remplace
     * @return le futur de l'édition d'espace
     */
    CompletableFuture<EditionEspaceLsp> rennomage(String uri, Document doc,
            int caret, String nouveauNom);

    /**
     * Les correctifs rapides d'une plage (H-12) — le menu d'intentions.
     *
     * @param uri   l'uri du document
     * @param plage la plage porteuse
     * @return le futur des correctifs
     */
    CompletableFuture<List<CorrectifLsp>> correctifs(String uri, PlageLsp plage);

    /**
     * Les indices en ligne d'une plage (H-12).
     *
     * @param uri   l'uri du document
     * @param plage la plage visible
     * @return le futur des indices
     */
    CompletableFuture<List<IndiceLsp>> indices(String uri, PlageLsp plage);

    /**
     * Les jetons sémantiques du document (H-12).
     *
     * @param uri l'uri du document
     * @return le futur des jetons
     */
    CompletableFuture<SemanticTokensLsp> jetons(String uri);

    /**
     * Les jetons sémantiques d'une plage (H-12) — la mise à jour locale.
     *
     * @param uri   l'uri du document
     * @param plage la plage visible
     * @return le futur des jetons
     */
    CompletableFuture<SemanticTokensLsp> jetonsPlage(String uri, PlageLsp plage);

    /**
     * Les références du symbole (H-12).
     *
     * @param uri   l'uri du document
     * @param doc   l'instantané courant
     * @param caret la position du symbole
     * @return le futur des lieux de référence
     */
    CompletableFuture<List<LieuLsp>> references(String uri, Document doc,
            int caret);

    /** Ferme la session — transports et piscines, sans exception vérifiée. */
    @Override
    void close();
}
