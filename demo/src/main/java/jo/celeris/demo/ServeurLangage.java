package jo.celeris.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import jo.celeris.android.widgets.CompletionDocModel;
import jo.celeris.android.widgets.SignatureAideModel;
import jo.celeris.core.lsp.types.CorrectifLsp;
import jo.celeris.core.lsp.types.EditionEspaceLsp;
import jo.celeris.core.lsp.types.EditionTexteLsp;
import jo.celeris.core.lsp.types.IndiceLsp;
import jo.celeris.core.lsp.types.SemanticTokensLsp;
import jo.celeris.core.lsp.types.LieuLsp;
import jo.celeris.core.lsp.types.OptionsFormatageLsp;
import jo.celeris.core.lsp.types.PlageLsp;
import jo.celeris.core.lsp.types.PliLsp;
import jo.celeris.core.lsp.PositionsLsp;
import jo.celeris.core.lsp.types.PreparationRennomageLsp;
import jo.celeris.core.lsp.types.SignatureLsp;
import jo.celeris.core.lsp.types.SurlignageLsp;
import jo.celeris.core.lsp.types.SurvolLsp;
import jo.celeris.core.lsp.types.SymboleLsp;


import jo.celeris.core.text.Document;
import jo.celeris.lsp.protocole.ServeurEmbarque;

import org.jspecify.annotations.Nullable;

/**
 * La session de langage de référence de la démonstration (M7.5, §8) :
 * la session de la frontière protocolaire (ADR-015) montée sur le
 * serveur de référence du terrain, EN PROCESSUS — tubes à file, aucun
 * réseau. Les popups y demandent l'aide aux paramètres et le survol
 * par les futurs du protocole ; la synchronisation complète suit la
 * session à chaque édition — l'ordonnancement vit dans la session,
 * les frappes rapides ne s'empilent jamais (PIEGE-18).
 */
public final class ServeurLangage implements LanguageSession {

    private final ServeurEmbarque montage;

    private ServeurLangage(ServeurEmbarque montage) {
        this.montage = montage;
    }

    /**
     * Démarre la session de langage : le montage du module y lance le
     * serveur de référence sur un bout des tubes, la session
     * poignée-de-main sur l'autre.
     *
     * @return la session démarrée, prête à ouvrir un document
     */
    public static ServeurLangage demarrer() {
        ServeurEmbarque montage = ServeurEmbarque.monter(
                new ServeurReference(), "celeris-demo");
        montage.session().initialiser();
        return new ServeurLangage(montage);
    }

    @Override
    public void ouvrir(String uri, String langue, String texte) {
        montage.session().ouvrir(uri, langue, texte);
    }

    @Override
    public void synchroniser(String uri, String texte) {
        montage.session().changer(uri,
                montage.session().versionDe(uri) + 1, texte);
    }

    @Override
    public CompletableFuture<@Nullable SignatureAideModel> signature(
            String uri, Document doc, int caret) {
        return montage.session().signature(uri,
                PositionsLsp.position(doc, caret))
                .thenApply(ServeurLangage::enModele);
    }

    @Override
    public CompletableFuture<@Nullable CompletionDocModel> survol(
            String uri, Document doc, String symbole, int caret) {
        return montage.session().survol(uri,
                PositionsLsp.position(doc, caret))
                .thenApply(survol -> enModele(survol, symbole));
    }

    @Override
    public boolean sertCompletion() {
        // la complétion reste l'affaire des mots du code — le serveur
        // de référence ne sert que la signature et le survol
        return false;
    }

    @Override
    public CompletableFuture<jo.celeris.core.lsp.types.ListeCompletion> completion(
            String uri, Document doc, int caret) {
        throw new UnsupportedOperationException(
                "le serveur de référence ne sert pas la complétion");
    }

    @Override
    public void poserAuditeurDiagnostics(
            java.util.function.BiConsumer<String,
                    java.util.List<jo.celeris.core.lsp.types.DiagnosticLsp>> auditeur) {
        // le serveur de référence ne publie jamais de diagnostics
    }

    /**
     * La conversion : la signature du protocole au modèle riche de la
     * vue (C7) — TOUTES les entrées candidates avec leur documentation
     * (le peek), l'active et le paramètre courant borné.
     */
    static @Nullable SignatureAideModel enModele(SignatureLsp lsp) {
        if (lsp.entrees().isEmpty()) {
            return null;
        }
        List<SignatureAideModel.Entree> entrees = new ArrayList<>();
        for (SignatureLsp.Entree source : lsp.entrees()) {
            List<String> libelles = new ArrayList<>();
            for (SignatureLsp.Parametre parametre : source.parametres()) {
                libelles.add(parametre.libelle());
            }
            entrees.add(new SignatureAideModel.Entree(source.libelle(),
                    libelles, source.documentation()));
        }
        int active = Math.max(0, lsp.signatureActiveIndex());
        return SignatureAideModel.of(entrees, active, lsp.parametreCourant());
    }

    /** La conversion : le survol du protocole au modèle de documentation. */
    static @Nullable CompletionDocModel enModele(SurvolLsp survol,
            String symbole) {
        if (survol.texte().isEmpty()) {
            return null;
        }
        return CompletionDocModel.of(symbole, survol.texte());
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
