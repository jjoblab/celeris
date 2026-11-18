package jo.celeris.sample.lsp;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import jo.celeris.android.host.popups.LanguageSession;
import jo.celeris.android.widgets.CompletionDocModel;
import jo.celeris.android.widgets.SignatureAideModel;
import jo.celeris.core.lsp.types.CorrectifLsp;
import jo.celeris.core.lsp.types.DiagnosticLsp;
import jo.celeris.core.lsp.types.EditionEspaceLsp;
import jo.celeris.core.lsp.types.IndiceLsp;
import jo.celeris.core.lsp.types.LieuLsp;
import jo.celeris.core.lsp.types.ListeCompletion;
import jo.celeris.core.lsp.types.PliLsp;
import jo.celeris.core.lsp.types.PositionLsp;
import jo.celeris.core.lsp.types.PreparationRennomageLsp;
import jo.celeris.core.lsp.types.SurlignageLsp;
import jo.celeris.core.lsp.types.SymboleLsp;
import jo.celeris.core.text.Document;
import jo.celeris.lsp.protocole.LspSession;

import org.jspecify.annotations.Nullable;

/**
 * L'adaptateur du sample : une {@link LspSession} du protocole portée
 * à la {@link LanguageSession} de l'hôte — le pont entre le mini
 * serveur Java (monté en processus) et les popups de l'éditeur. Les
 * offsets du document se convertissent en positions LSP (ligne,
 * colonne) au passage ; les modèles du kit (signature, documentation)
 * naissent des types du protocole.
 */
public final class SessionLspAdaptee implements LanguageSession {

    private final LspSession session;

    /**
     * Construit l'adaptateur sur sa session de protocole.
     *
     * @param session la session initialisée du mini serveur
     */
    public SessionLspAdaptee(LspSession session) {
        this.session = session;
    }

    @Override
    public void ouvrir(String uri, String langue, String texte) {
        session.ouvrir(uri, langue, texte);
    }

    @Override
    public void synchroniser(String uri, String texte) {
        session.changer(uri, versionSuivante(uri), texte);
    }

    /** Les versions suivies — la resynchronisation annonce la suivante. */
    private final java.util.Map<String, Integer> versions =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** La version suivante d'un document, comptée depuis l'ouverture. */
    private int versionSuivante(String uri) {
        Integer courante = versions.get(uri);
        int suivante = (courante == null ? 1 : courante) + 1;
        versions.put(uri, suivante);
        return suivante;
    }

    @Override
    public CompletableFuture<@Nullable SignatureAideModel> signature(String uri,
            Document doc, int caret) {
        return session.signature(uri, position(doc, caret))
                .thenApply(aide -> {
                    if (aide == null || aide.entrees().isEmpty()) {
                        return null;
                    }
                    List<SignatureAideModel.Entree> entrees =
                            new java.util.ArrayList<>();
                    for (var source : aide.entrees()) {
                        List<String> parametres = new java.util.ArrayList<>();
                        for (var parametre : source.parametres()) {
                            parametres.add(parametre.libelle());
                        }
                        entrees.add(new SignatureAideModel.Entree(
                                source.libelle(), parametres,
                                source.documentation()));
                    }
                    return SignatureAideModel.of(entrees,
                            aide.signatureActiveIndex(),
                            aide.parametreCourant());
                });
    }

    @Override
    public CompletableFuture<@Nullable CompletionDocModel> survol(String uri,
            Document doc, String symbole, int caret) {
        return session.survol(uri, position(doc, caret))
                .thenApply(survol -> survol == null ? null
                        : CompletionDocModel.of(survol.texte(),
                                survol.texte()));
    }

    @Override
    public boolean sertCompletion() {
        return true;
    }

    @Override
    public CompletableFuture<ListeCompletion> completion(String uri,
            Document doc, int caret) {
        return session.completion(uri, position(doc, caret));
    }

    @Override
    public void poserAuditeurDiagnostics(
            java.util.function.BiConsumer<String, List<DiagnosticLsp>> auditeur) {
        session.ajouterAuditeurDiagnostics(
                (uri, diagnostics) -> auditeur.accept(uri, diagnostics));
    }

    // ── La couverture du protocole : les méthodes du kit passent au
    //    serveur, les vides du domaine restent aux replis. ──────────

    @Override
    public CompletableFuture<List<SymboleLsp>> symboles(String uri) {
        return session.symboles(uri);
    }

    @Override
    public CompletableFuture<List<PliLsp>> plis(String uri) {
        return session.plis(uri);
    }

    @Override
    public CompletableFuture<List<jo.celeris.core.lsp.types.EditionTexteLsp>> formatage(
            String uri, jo.celeris.core.lsp.types.OptionsFormatageLsp options) {
        return session.formatage(uri, options);
    }

    @Override
    public CompletableFuture<List<jo.celeris.core.lsp.types.EditionTexteLsp>> formatagePlage(
            String uri, jo.celeris.core.lsp.types.PlageLsp plage,
            jo.celeris.core.lsp.types.OptionsFormatageLsp options) {
        return session.formatagePlage(uri, plage, options);
    }

    @Override
    public CompletableFuture<List<SurlignageLsp>> surlignages(String uri,
            Document doc, int caret) {
        return session.surlignages(uri, position(doc, caret));
    }

    @Override
    public CompletableFuture<List<jo.celeris.core.lsp.types.PlageLsp>> plagesSelection(
            String uri, Document doc, int caret) {
        return session.plagesSelection(uri, position(doc, caret));
    }

    @Override
    public CompletableFuture<@Nullable PreparationRennomageLsp> preparerRennomage(
            String uri, Document doc, int caret) {
        return session.preparerRennomage(uri, position(doc, caret));
    }

    @Override
    public CompletableFuture<EditionEspaceLsp> rennomage(String uri, Document doc,
            int caret, String nouveauNom) {
        return session.rennomage(uri, position(doc, caret), nouveauNom);
    }

    @Override
    public CompletableFuture<List<CorrectifLsp>> correctifs(String uri,
            jo.celeris.core.lsp.types.PlageLsp plage) {
        return session.correctifs(uri, plage);
    }

    @Override
    public CompletableFuture<List<IndiceLsp>> indices(String uri,
            jo.celeris.core.lsp.types.PlageLsp plage) {
        return session.indices(uri, plage);
    }

    @Override
    public CompletableFuture<jo.celeris.core.lsp.types.SemanticTokensLsp> jetons(
            String uri) {
        return session.jetons(uri);
    }

    @Override
    public CompletableFuture<jo.celeris.core.lsp.types.SemanticTokensLsp> jetonsPlage(
            String uri, jo.celeris.core.lsp.types.PlageLsp plage) {
        return session.jetonsPlage(uri, plage);
    }

    @Override
    public CompletableFuture<List<LieuLsp>> references(String uri, Document doc,
            int caret) {
        return session.references(uri, position(doc, caret));
    }

    @Override
    public void close() {
        try {
            session.close();
        } catch (Exception e) {
            // la fermeture d'une session morte ne crie jamais — le
            // logcat seul l'entend
            android.util.Log.w("celeris-lsp",
                    "fermeture de session : " + e.getMessage());
        }
    }

    // ── Les conversions offset ↔ position LSP ──────────────────────

    /** La position LSP d'un offset du document. */
    private static PositionLsp position(Document doc, int offset) {
        int borne = Math.max(0, Math.min(offset, doc.length()));
        int ligne = doc.lineForOffset(borne);
        return new PositionLsp(ligne, borne - doc.lineStart(ligne));
    }

    /** La plage LSP d'une étendue du document. */
    private static jo.celeris.core.lsp.types.PlageLsp plage(Document doc,
            int debut, int fin) {
        return new jo.celeris.core.lsp.types.PlageLsp(position(doc, debut),
                position(doc, fin));
    }
}
