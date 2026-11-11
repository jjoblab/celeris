package jo.celeris.demo;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import jo.celeris.android.widgets.CompletionDocModel;
import jo.celeris.android.widgets.SignatureAideModel;
import jo.celeris.core.language.LanguageServerDeclaration;
import jo.celeris.core.lsp.CorrectifLsp;
import jo.celeris.core.lsp.DiagnosticLsp;
import jo.celeris.core.lsp.EditionEspaceLsp;
import jo.celeris.core.lsp.EditionTexteLsp;
import jo.celeris.core.lsp.IndiceLsp;
import jo.celeris.core.lsp.SemanticTokensLsp;
import jo.celeris.core.lsp.LieuLsp;
import jo.celeris.core.lsp.ListeCompletion;
import jo.celeris.core.lsp.CapacitesServeur;
import jo.celeris.core.lsp.OptionsFormatageLsp;
import jo.celeris.core.lsp.PlageLsp;
import jo.celeris.core.lsp.PliLsp;
import jo.celeris.core.lsp.PositionsLsp;
import jo.celeris.core.lsp.PreparationRennomageLsp;
import jo.celeris.core.lsp.SurlignageLsp;
import jo.celeris.core.lsp.SymboleLsp;
import jo.celeris.core.text.Document;
import jo.celeris.lsp.protocole.ServeurEmbarque;

import org.jspecify.annotations.Nullable;

/**
 * La session de langage d'un serveur DÉCLARÉ (M7.6, H-13) : le montage
 * générique de la frontière protocolaire (ADR-015), lancé par la
 * DONNÉE de la déclaration — classe du serveur, interface cliente et
 * nom annoncés par le fichier {@code servers.json}, jamais par le
 * code. Tout serveur embarqué de l'hôte y monte comme un autre : cette
 * session ne connaît ni son nom ni ses particularités, elle lit les
 * capacités négociées à la poignée de main et ne demande que ce que
 * le serveur annonce servir.
 *
 * <p>La vue ne sait toujours pas qui a répondu (ADR-005) ; la
 * démonstration, elle, ne sait plus qu'un serveur existe — elle le
 * déclare, le montage l'exécute.</p>
 */
final class DeclaredServerSession implements LanguageSession {

    private final ServeurEmbarque montage;

    private final CapacitesServeur capacites;

    private DeclaredServerSession(ServeurEmbarque montage,
            CapacitesServeur capacites) {
        this.montage = montage;
        this.capacites = capacites;
    }

    /**
     * Monte le serveur déclaré : le montage du module instancie par les
     * noms de la déclaration, la poignée de main emporte les options
     * d'initialisation telles quelles, les capacités lues décident des
     * demandes.
     *
     * @param declaration la déclaration du serveur à monter
     * @return la session démarrée, prête à ouvrir un document
     */
    static DeclaredServerSession start(
            LanguageServerDeclaration declaration) {
        ServeurEmbarque montage = ServeurEmbarque.monter(
                declaration.serverClass(), declaration.clientInterface(),
                declaration.clientName());
        CapacitesServeur lues = montage.session()
                .initialiser(declaration.initializationOptions());
        return new DeclaredServerSession(montage, lues);
    }

    @Override
    public void ouvrir(String uri, String langue, String texte) {
        montage.session().ouvrir(uri, langue, texte);
    }

    @Override
    public void synchroniser(String uri, String texte) {
        // la session porte la version ; la synchronisation reste complète
        montage.session().changer(uri,
                montage.session().versionDe(uri) + 1, texte);
    }

    @Override
    public CompletableFuture<@Nullable SignatureAideModel> signature(
            String uri, Document doc, int caret) {
        if (!capacites.sertSignature()) {
            // le serveur n'annonce pas l'aide aux paramètres : la
            // demande ne part pas, le futur rend son vide
            return CompletableFuture.completedFuture(null);
        }
        return montage.session().signature(uri,
                PositionsLsp.position(doc, caret))
                .thenApply(ServeurLangage::enModele);
    }

    @Override
    public CompletableFuture<@Nullable CompletionDocModel> survol(
            String uri, Document doc, String symbole, int caret) {
        if (!capacites.sertSurvol()) {
            return CompletableFuture.completedFuture(null);
        }
        return montage.session().survol(uri,
                PositionsLsp.position(doc, caret))
                .thenApply(survol -> ServeurLangage.enModele(survol, symbole));
    }

    @Override
    public boolean sertCompletion() {
        // le serveur l'a dit à la poignée de main — la session le croit
        return capacites.sertCompletion();
    }

    @Override
    public CompletableFuture<ListeCompletion> completion(String uri,
            Document doc, int caret) {
        if (!capacites.sertCompletion()) {
            throw new UnsupportedOperationException(
                    "the declared server does not serve completion");
        }
        return montage.session().completion(uri,
                PositionsLsp.position(doc, caret));
    }

    @Override
    public void poserAuditeurDiagnostics(
            BiConsumer<String, List<DiagnosticLsp>> auditeur) {
        montage.session().ajouterAuditeurDiagnostics(auditeur::accept);
    }

    // ── La couverture du jalon (H-12) : la session de la frontière
    //    porte le protocole, cette porte rend le domaine. ────────────

    @Override
    public CompletableFuture<List<SymboleLsp>> symboles(String uri) {
        return montage.session().symboles(uri);
    }

    @Override
    public CompletableFuture<List<PliLsp>> plis(String uri) {
        return montage.session().plis(uri);
    }

    @Override
    public CompletableFuture<List<EditionTexteLsp>> formatage(String uri,
            OptionsFormatageLsp options) {
        return montage.session().formatage(uri, options);
    }

    @Override
    public CompletableFuture<List<EditionTexteLsp>> formatagePlage(String uri,
            PlageLsp plage, OptionsFormatageLsp options) {
        return montage.session().formatagePlage(uri, plage, options);
    }

    @Override
    public CompletableFuture<List<SurlignageLsp>> surlignages(String uri,
            Document doc, int caret) {
        return montage.session().surlignages(uri,
                PositionsLsp.position(doc, caret));
    }

    @Override
    public CompletableFuture<List<PlageLsp>> plagesSelection(String uri,
            Document doc, int caret) {
        return montage.session().plagesSelection(uri,
                PositionsLsp.position(doc, caret));
    }

    @Override
    public CompletableFuture<@Nullable PreparationRennomageLsp> preparerRennomage(
            String uri, Document doc, int caret) {
        return montage.session().preparerRennomage(uri,
                PositionsLsp.position(doc, caret));
    }

    @Override
    public CompletableFuture<EditionEspaceLsp> rennomage(String uri,
            Document doc, int caret, String nouveauNom) {
        return montage.session().rennomage(uri,
                PositionsLsp.position(doc, caret), nouveauNom);
    }

    @Override
    public CompletableFuture<List<CorrectifLsp>> correctifs(String uri,
            PlageLsp plage) {
        return montage.session().correctifs(uri, plage);
    }

    @Override
    public CompletableFuture<List<IndiceLsp>> indices(String uri,
            PlageLsp plage) {
        return montage.session().indices(uri, plage);
    }

    @Override
    public CompletableFuture<SemanticTokensLsp> jetons(String uri) {
        return montage.session().jetons(uri);
    }

    @Override
    public CompletableFuture<SemanticTokensLsp> jetonsPlage(String uri,
            PlageLsp plage) {
        return montage.session().jetonsPlage(uri, plage);
    }

    @Override
    public CompletableFuture<List<LieuLsp>> references(String uri,
            Document doc, int caret) {
        return montage.session().references(uri,
                PositionsLsp.position(doc, caret));
    }

    @Override
    public void close() {
        montage.close();
    }
}
