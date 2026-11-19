package jo.celeris.sample.lsp;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import jo.celeris.android.host.popups.LanguageSession;
import jo.celeris.android.widgets.CompletionDocModel;
import jo.celeris.android.widgets.SignatureAideModel;
import jo.celeris.core.lsp.types.CodeActionLsp;
import jo.celeris.core.lsp.types.DiagnosticLsp;
import jo.celeris.core.lsp.types.WorkspaceEditLsp;
import jo.celeris.core.lsp.types.InlayHintLsp;
import jo.celeris.core.lsp.types.LocationLsp;
import jo.celeris.core.lsp.types.CompletionListLsp;
import jo.celeris.core.lsp.types.FoldLsp;
import jo.celeris.core.lsp.types.LspPosition;
import jo.celeris.core.lsp.types.RenamePrepareLsp;
import jo.celeris.core.lsp.types.HighlightLsp;
import jo.celeris.core.lsp.types.SymbolLsp;
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
    public void open(String uri, String language, String text) {
        session.open(uri, language, text);
    }

    @Override
    public void synchroniser(String uri, String text) {
        session.change(uri, versionSuivante(uri), text);
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
                .thenApply(help -> {
                    if (help == null || help.entrees().isEmpty()) {
                        return null;
                    }
                    List<SignatureAideModel.Entry> entrees =
                            new java.util.ArrayList<>();
                    for (var source : help.entrees()) {
                        List<String> parameters = new java.util.ArrayList<>();
                        for (var parametre : source.parameters()) {
                            parameters.add(parametre.label());
                        }
                        entrees.add(new SignatureAideModel.Entry(
                                source.label(), parameters,
                                source.documentation()));
                    }
                    return SignatureAideModel.of(entrees,
                            help.signatureActiveIndex(),
                            help.parametreCourant());
                });
    }

    @Override
    public CompletableFuture<@Nullable CompletionDocModel> hover(String uri,
            Document doc, String symbole, int caret) {
        return session.hover(uri, position(doc, caret))
                .thenApply(hover -> hover == null ? null
                        : CompletionDocModel.of(hover.text(),
                                hover.text()));
    }

    @Override
    public boolean supportsCompletion() {
        return true;
    }

    @Override
    public CompletableFuture<CompletionListLsp> completion(String uri,
            Document doc, int caret) {
        return session.completion(uri, position(doc, caret));
    }

    @Override
    public void setDiagnosticsListener(
            java.util.function.BiConsumer<String, List<DiagnosticLsp>> auditeur) {
        session.addDiagnosticsListener(
                (uri, diagnostics) -> auditeur.accept(uri, diagnostics));
    }

    // ── La couverture du protocole : les méthodes du kit passent au
    //    serveur, les vides du domaine restent aux replis. ──────────

    @Override
    public CompletableFuture<List<SymbolLsp>> symbols(String uri) {
        return session.symbols(uri);
    }

    @Override
    public CompletableFuture<List<FoldLsp>> folds(String uri) {
        return session.folds(uri);
    }

    @Override
    public CompletableFuture<List<jo.celeris.core.lsp.types.TextEditLsp>> formatage(
            String uri, jo.celeris.core.lsp.types.FormatOptionsLsp options) {
        return session.formatage(uri, options);
    }

    @Override
    public CompletableFuture<List<jo.celeris.core.lsp.types.TextEditLsp>> rangeFormatting(
            String uri, jo.celeris.core.lsp.types.RangeLsp range,
            jo.celeris.core.lsp.types.FormatOptionsLsp options) {
        return session.rangeFormatting(uri, range, options);
    }

    @Override
    public CompletableFuture<List<HighlightLsp>> highlights(String uri,
            Document doc, int caret) {
        return session.highlights(uri, position(doc, caret));
    }

    @Override
    public CompletableFuture<List<jo.celeris.core.lsp.types.RangeLsp>> selectionRanges(
            String uri, Document doc, int caret) {
        return session.selectionRanges(uri, position(doc, caret));
    }

    @Override
    public CompletableFuture<@Nullable RenamePrepareLsp> preparerRennomage(
            String uri, Document doc, int caret) {
        return session.preparerRennomage(uri, position(doc, caret));
    }

    @Override
    public CompletableFuture<WorkspaceEditLsp> rename(String uri, Document doc,
            int caret, String nouveauNom) {
        return session.rename(uri, position(doc, caret), nouveauNom);
    }

    @Override
    public CompletableFuture<List<CodeActionLsp>> correctifs(String uri,
            jo.celeris.core.lsp.types.RangeLsp range) {
        return session.correctifs(uri, range);
    }

    @Override
    public CompletableFuture<List<InlayHintLsp>> inlayHints(String uri,
            jo.celeris.core.lsp.types.RangeLsp range) {
        return session.inlayHints(uri, range);
    }

    @Override
    public CompletableFuture<jo.celeris.core.lsp.types.SemanticTokensLsp> tokens(
            String uri) {
        return session.tokens(uri);
    }

    @Override
    public CompletableFuture<jo.celeris.core.lsp.types.SemanticTokensLsp> tokensRange(
            String uri, jo.celeris.core.lsp.types.RangeLsp range) {
        return session.tokensRange(uri, range);
    }

    @Override
    public CompletableFuture<List<LocationLsp>> references(String uri, Document doc,
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
    private static LspPosition position(Document doc, int offset) {
        int borne = Math.max(0, Math.min(offset, doc.length()));
        int line = doc.lineForOffset(borne);
        return new LspPosition(line, borne - doc.lineStart(line));
    }

    /** La plage LSP d'une étendue du document. */
    private static jo.celeris.core.lsp.types.RangeLsp range(Document doc,
            int start, int end) {
        return new jo.celeris.core.lsp.types.RangeLsp(position(doc, start),
                position(doc, end));
    }
}
