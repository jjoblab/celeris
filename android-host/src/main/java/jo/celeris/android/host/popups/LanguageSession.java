package jo.celeris.android.host.popups;

import java.util.concurrent.CompletableFuture;
import java.util.List;

import jo.celeris.core.lsp.types.CodeActionLsp;
import jo.celeris.core.lsp.types.DiagnosticLsp;
import jo.celeris.core.lsp.types.WorkspaceEditLsp;
import jo.celeris.core.lsp.types.TextEditLsp;
import jo.celeris.core.lsp.types.InlayHintLsp;
import jo.celeris.core.lsp.types.SemanticTokensLsp;
import jo.celeris.core.lsp.types.LocationLsp;
import jo.celeris.core.lsp.types.CompletionListLsp;
import jo.celeris.core.lsp.types.FormatOptionsLsp;
import jo.celeris.core.lsp.types.RangeLsp;
import jo.celeris.core.lsp.types.FoldLsp;
import jo.celeris.core.lsp.types.RenamePrepareLsp;
import jo.celeris.core.lsp.types.HighlightLsp;
import jo.celeris.core.lsp.types.SymbolLsp;

import jo.celeris.android.widgets.CompletionDocModel;
import jo.celeris.android.widgets.SignatureAideModel;

import jo.celeris.core.text.Document;

import org.jspecify.annotations.Nullable;

/**
 * La session de langage de l'éditeur : la surface commune des serveurs
 * que {@link EditorPopups} consomme — tout serveur déclaré par
 * l'hôte, lancé en processus ou en mémoire, entre par cette porte.
 * L'hôte n'en connaît que cette surface : ouvrir, synchroniser, et les
 * trois canaux des popups — signature, survol, complétion — plus les
 * diagnostics publiés. Les modèles du kit y arrivent DÉJÀ convertis :
 * l'orchestration n'attrape jamais le protocole.
 */
public interface LanguageSession extends AutoCloseable {

    /**
     * Ouvre le document suivi — version 1, synchronisation complète.
     *
     * @param uri    l'uri du document
     * @param language l'identifiant du langage
     * @param text  le contenu initial
     */
    void open(String uri, String language, String text);

    /**
     * La synchronisation complète : le texte entier, la version
     * suivante — l'état du serveur suit la session d'édition.
     *
     * @param uri   l'uri du document
     * @param text le contenu complet courant
     */
    void synchroniser(String uri, String text);

    /**
     * L'aide aux paramètres au curseur — le futur du modèle de la
     * vue, déjà converti ; null dans le futur sans signature.
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
     * documentation, déjà converti ; null sans documentation.
     *
     * @param uri      l'uri du document
     * @param doc      l'instantané courant
     * @param symbole  le symbole survolé — le titre du modèle
     * @param caret    la position du curseur
     * @return le futur du modèle, null si le serveur n'en dit rien
     */
    CompletableFuture<@Nullable CompletionDocModel> hover(String uri,
            Document doc, String symbole, int caret);

    /**
     * Vrai quand le serveur SERT la complétion par le protocole —
     * l'hôte y préfère alors ses mots locaux.
     *
     * @return vrai quand la complétion passe par le serveur
     */
    boolean supportsCompletion();

    /**
     * La complétion au curseur, la réponse BRUTE du protocole —
     * l'hôte en tire ses candidats typés et leur documentation.
     *
     * @param uri   l'uri du document
     * @param doc   l'instantané courant
     * @param caret la position du curseur
     * @return le futur de la liste de complétion du protocole
     */
    CompletableFuture<CompletionListLsp> completion(String uri, Document doc,
            int caret);

    /**
     * Pose l'auditeur des diagnostics publiés — un serveur muet ne
     * prévient jamais, l'auditeur reste silencieux.
     *
     * @param auditeur l'auditeur des diagnostics du protocole
     */
    void setDiagnosticsListener(
            java.util.function.BiConsumer<String, java.util.List<DiagnosticLsp>> auditeur);

    // ── La couverture du protocole : chaque méthode du protocole
    //    raccordée à cette porte — le serveur qui n'en sert pas rend
    //    ses vides du domaine, l'hôte n'attrape jamais de protocole. ──

    /**
     * Les symboles du document — le plan, le fil d'Ariane.
     *
     * @param uri l'uri du document
     * @return le futur des symboles
     */
    CompletableFuture<List<SymbolLsp>> symbols(String uri);

    /**
     * Les plis du serveur, fin EXCLUSIVE — le consommateur fusionne
     * avec l'arbre commun.
     *
     * @param uri l'uri du document
     * @return le futur des plis
     */
    CompletableFuture<List<FoldLsp>> folds(String uri);

    /**
     * Le formatage du document — la capacité Formatter branchée.
     *
     * @param uri     l'uri du document
     * @param options les options de formatage
     * @return le futur des éditions de texte
     */
    CompletableFuture<List<TextEditLsp>> formatage(String uri,
            FormatOptionsLsp options);

    /**
     * Le formatage d'une plage — le remaniement local.
     *
     * @param uri     l'uri du document
     * @param range   la plage à formater
     * @param options les options de formatage
     * @return le futur des éditions de texte
     */
    CompletableFuture<List<TextEditLsp>> rangeFormatting(String uri,
            RangeLsp range, FormatOptionsLsp options);

    /**
     * Les surlignages d'occurrences sous le curseur.
     *
     * @param uri   l'uri du document
     * @param doc   l'instantané courant
     * @param caret la position du curseur
     * @return le futur des surlignages
     */
    CompletableFuture<List<HighlightLsp>> highlights(String uri,
            Document doc, int caret);

    /**
     * Les plages de sélection englobantes — l'extension de la
     * sélection, de la plus intime à la plus large.
     *
     * @param uri   l'uri du document
     * @param doc   l'instantané courant
     * @param caret la position de départ
     * @return le futur des plages englobantes
     */
    CompletableFuture<List<RangeLsp>> selectionRanges(String uri,
            Document doc, int caret);

    /**
     * La préparation du renommage — null sans préparation.
     *
     * @param uri   l'uri du document
     * @param doc   l'instantané courant
     * @param caret la position du nom
     * @return le futur de la préparation
     */
    CompletableFuture<@Nullable RenamePrepareLsp> preparerRennomage(
            String uri, Document doc, int caret);

    /**
     * Le renommage — l'édition d'espace qui porte le nouveau nom.
     *
     * @param uri        l'uri du document
     * @param doc        l'instantané courant
     * @param caret      la position du nom
     * @param nouveauNom le nom qui remplace
     * @return le futur de l'édition d'espace
     */
    CompletableFuture<WorkspaceEditLsp> rename(String uri, Document doc,
            int caret, String nouveauNom);

    /**
     * Les correctifs rapides d'une plage — le menu d'intentions.
     *
     * @param uri   l'uri du document
     * @param range la plage porteuse
     * @return le futur des correctifs
     */
    CompletableFuture<List<CodeActionLsp>> correctifs(String uri, RangeLsp range);

    /**
     * Les indices en ligne d'une plage.
     *
     * @param uri   l'uri du document
     * @param range la plage visible
     * @return le futur des indices
     */
    CompletableFuture<List<InlayHintLsp>> inlayHints(String uri, RangeLsp range);

    /**
     * Les jetons sémantiques du document.
     *
     * @param uri l'uri du document
     * @return le futur des jetons
     */
    CompletableFuture<SemanticTokensLsp> tokens(String uri);

    /**
     * Les jetons sémantiques d'une plage — la mise à jour locale.
     *
     * @param uri   l'uri du document
     * @param range la plage visible
     * @return le futur des jetons
     */
    CompletableFuture<SemanticTokensLsp> tokensRange(String uri, RangeLsp range);

    /**
     * Les références du symbole.
     *
     * @param uri   l'uri du document
     * @param doc   l'instantané courant
     * @param caret la position du symbole
     * @return le futur des lieux de référence
     */
    CompletableFuture<List<LocationLsp>> references(String uri, Document doc,
            int caret);

    /** Ferme la session — transports et piscines, sans exception vérifiée. */
    @Override
    void close();
}
