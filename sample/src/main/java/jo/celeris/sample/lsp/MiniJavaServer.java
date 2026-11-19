package jo.celeris.sample.lsp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jo.celeris.core.lsp.types.DiagnosticLsp;
import jo.celeris.core.lsp.types.CompletionListLsp;
import jo.celeris.core.lsp.types.FoldLsp;
import jo.celeris.core.lsp.types.LspPosition;
import jo.celeris.core.lsp.types.SignatureLsp;
import jo.celeris.core.lsp.types.HighlightLsp;
import jo.celeris.core.lsp.types.HoverLsp;
import jo.celeris.core.lsp.types.SymbolLsp;
import jo.celeris.lsp.protocole.EmbeddedServer;

/**
 * Le mini serveur de langage JAVA de l'exemple (le retour terrain du
 * 2026-11-19 : « créé un serveur factice Java plus solide comme
 * code-editor ») : un VRAI serveur LSP monté en processus par
 * {@link EmbeddedServer#start} — le protocole JSON-RPC passe par la
 * bibliothèque, seul le transport est en mémoire. Les diagnostics
 * naissent d'un balayage réel du document : accolades non fermées et
 * fermantes orphelines, point-virgule manquant des déclarations,
 * parenthèses et crochets désappariés — les mêmes règles que le mini
 * Java du dépôt de départ, à sa mesure.
 *
 * <p>La complétion sert le contexte : les mots réservés du Java
 * toujours, les membres de {@code String} après un point derrière un
 * littéral, les méthodes déclarées du document. L'aide aux signatures
 * couvre {@code System.out.println} et {@code print} ; le survol rend
 * la déclaration locale du symbole sous le curseur.</p>
 */
public final class MiniJavaServer implements EmbeddedServer.Docs {

    /** Les documents suivis : l'uri vers son texte courant. */
    private final Map<String, String> documents = new HashMap<>();

    /** Construit le serveur — la publication passe par le protocole. */
    public MiniJavaServer() {
    }

    @Override
    public void open(String uri, String language, String text) {
        documents.put(uri, text);
    }

    @Override
    public void change(String uri, int version, String text) {
        documents.put(uri, text);
    }

    @Override
    public void close(String uri) {
        documents.remove(uri);
    }

    /** Les diagnostics courants du document — publiés par le montage
     *  après chaque synchronisation (le protocole y pourvoit). */
    @Override
    public List<DiagnosticLsp> diagnosticsOf(String uri) {
        String text = documents.get(uri);
        return text == null ? List.of() : DiagnosticsJava.of(text);
    }

    @Override
    public HoverLsp hover(String uri, LspPosition position) {
        String text = documents.get(uri);
        if (text == null) {
            return null;
        }
        return CompletionJava.hover(text, position);
    }

    @Override
    public SignatureLsp signature(String uri, LspPosition position) {
        String text = documents.get(uri);
        if (text == null) {
            return null;
        }
        return CompletionJava.signature(text, offset(text, position));
    }

    @Override
    public CompletionListLsp completion(String uri, LspPosition position) {
        String text = documents.get(uri);
        if (text == null) {
            return null;
        }
        return CompletionJava.completion(text, offset(text, position));
    }

    /** L'offset d'une position LSP dans le texte, borné au texte. */
    private static int offset(String text, LspPosition position) {
        int line = 0;
        int offset = 0;
        while (line < position.line() && offset < text.length()) {
            if (text.charAt(offset) == '\n') {
                line++;
            }
            offset++;
        }
        return Math.min(text.length(), offset + position.column());
    }

    @Override
    public List<SymbolLsp> symbols(String uri) {
        String text = documents.get(uri);
        return text == null ? List.of() : CompletionJava.symbols(text);
    }

    @Override
    public List<FoldLsp> folds(String uri) {
        String text = documents.get(uri);
        if (text == null) {
            return List.of();
        }
        List<FoldLsp> folds = new ArrayList<>();
        java.util.ArrayDeque<Integer> pile = new java.util.ArrayDeque<>();
        int line = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                line++;
            } else if (c == '{') {
                pile.push(line);
            } else if (c == '}' && !pile.isEmpty()) {
                folds.add(new FoldLsp(pile.pop(), line));
            }
        }
        return folds;
    }

    @Override
    public List<HighlightLsp> highlights(String uri, LspPosition position) {
        String text = documents.get(uri);
        if (text == null) {
            return List.of();
        }
        return CompletionJava.highlights(text, position);
    }
}
