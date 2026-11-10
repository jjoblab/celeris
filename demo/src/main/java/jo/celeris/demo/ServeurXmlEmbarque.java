package jo.celeris.demo;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

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
import jo.celeris.core.lsp.PositionsLsp;
import jo.celeris.core.lsp.PreparationRennomageLsp;
import jo.celeris.core.lsp.SurlignageLsp;
import jo.celeris.core.lsp.SymboleLsp;
import jo.celeris.core.text.Document;
import jo.celeris.lsp.protocole.ServeurEmbarque;

import org.jspecify.annotations.Nullable;

/**
 * Le serveur de langage XML RÉEL, embarqué et lancé EN PROCESSUS
 * (M8.6 — LemMinX, voir NOTICE pour l'identité et la licence) : la
 * démonstration y monte la session de la frontière protocolaire
 * (ADR-015) par le montage du module — tubes à file, cadrage,
 * poignée de main, documents versionnés, complétion, survol et
 * diagnostics de validation, tout le protocole y passe par la
 * bibliothèque DERRIÈRE la frontière, les types rendus restent ceux
 * du domaine.
 *
 * <p>L'ordonnancement du domaine (ADR-005) vit DANS la session : les
 * frappes rapides ne s'empilent jamais (PIEGE-18), la demande fraîche
 * remplace celle en vol, l'annulation traverse le fil. C'est le banc
 * de parité de l'étape 7 : les sept tests doivent passer ici SANS
 * MODIFICATION sur la voie de la frontière — la voie maison ne
 * disparaîtra qu'après.</p>
 *
 * <p>La liaison protocolaire ne voyage pas dans l'archive du serveur
 * (copie unique, ADR-015 décision 3) : le chemin de classes fournit
 * la version épinglée au catalogue aux DEUX bouts — le client de la
 * frontière comme le serveur réel.</p>
 *
 * <p>La fermeture ne lui envoie JAMAIS {@code exit} : l'implémentation
 * y appelle la sortie de machine virtuelle, fatale au processus hôte
 * (constat M8.6). Le montage demande {@code shutdown}, ferme les
 * tubes, arrête sa piscine — les fils sont des démons.</p>
 */
public final class ServeurXmlEmbarque implements LanguageSession {

    /** La classe du serveur réel, en toutes lettres (identité au NOTICE). */
    private static final String CLASSE_SERVEUR =
            "org.eclipse.lemminx.XMLLanguageServer";

    /** L'interface cliente étendue du serveur réel — la convention de ses services propres. */
    private static final String INTERFACE_CLIENT =
            "org.eclipse.lemminx.customservice.XMLLanguageClientAPI";

    private final ServeurEmbarque montage;

    private ServeurXmlEmbarque(ServeurEmbarque montage) {
        this.montage = montage;
    }

    /**
     * Démarre le serveur XML en processus : le montage par désignation
     * du module y lance le serveur réel par son API sur des tubes à
     * file, la session de la frontière poignée-de-main sur l'autre
     * bout — le protocole circule par la bibliothèque, les règles du
     * domaine ordonnancent les vols.
     *
     * @return la session démarrée, prête à ouvrir un document
     */
    public static ServeurXmlEmbarque demarrer() {
        ServeurEmbarque montage = ServeurEmbarque.monter(
                CLASSE_SERVEUR, INTERFACE_CLIENT, "celeris-demo");
        montage.session().initialiser();
        return new ServeurXmlEmbarque(montage);
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
        // le XML n'a pas d'appel de fonction : jamais de signature
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<@Nullable CompletionDocModel> survol(String uri,
            Document doc, String symbole, int caret) {
        return montage.session().survol(uri,
                PositionsLsp.position(doc, caret))
                .thenApply(survol -> ServeurLangage.enModele(survol, symbole));
    }

    @Override
    public boolean sertCompletion() {
        return true;
    }

    @Override
    public CompletableFuture<ListeCompletion> completion(String uri,
            Document doc, int caret) {
        return montage.session().completion(uri,
                PositionsLsp.position(doc, caret));
    }

    @Override
    public void poserAuditeurDiagnostics(
            BiConsumer<String, java.util.List<DiagnosticLsp>> auditeur) {
        montage.session().ajouterAuditeurDiagnostics(auditeur::accept);
    }

    // ── La couverture du jalon (H-12) : le serveur RÉEL sert ce
    //    qu'il annonce — la session rend le domaine, jamais la
    //    bibliothèque. ────────────────────────────────────────────────

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
