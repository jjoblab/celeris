package jo.celeris.sample.lsp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jo.celeris.core.lsp.types.DiagnosticLsp;
import jo.celeris.core.lsp.types.ListeCompletion;
import jo.celeris.core.lsp.types.PliLsp;
import jo.celeris.core.lsp.types.PositionLsp;
import jo.celeris.core.lsp.types.SignatureLsp;
import jo.celeris.core.lsp.types.SurlignageLsp;
import jo.celeris.core.lsp.types.SurvolLsp;
import jo.celeris.core.lsp.types.SymboleLsp;
import jo.celeris.lsp.protocole.ServeurEmbarque;

/**
 * Le mini serveur de langage JAVA de l'exemple (le retour terrain du
 * 2026-11-19 : « créé un serveur factice Java plus solide comme
 * code-editor ») : un VRAI serveur LSP monté en processus par
 * {@link ServeurEmbarque#monter} — le protocole JSON-RPC passe par la
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
public final class MiniJavaServeur implements ServeurEmbarque.Documents {

    /** Les documents suivis : l'uri vers son texte courant. */
    private final Map<String, String> documents = new HashMap<>();

    /** Construit le serveur — la publication passe par le protocole. */
    public MiniJavaServeur() {
    }

    @Override
    public void ouvrir(String uri, String langue, String texte) {
        documents.put(uri, texte);
    }

    @Override
    public void changer(String uri, int version, String texte) {
        documents.put(uri, texte);
    }

    @Override
    public void fermer(String uri) {
        documents.remove(uri);
    }

    /** Les diagnostics courants du document — publiés par le montage
     *  après chaque synchronisation (le protocole y pourvoit). */
    @Override
    public List<DiagnosticLsp> diagnosticsDe(String uri) {
        String texte = documents.get(uri);
        return texte == null ? List.of() : DiagnosticsJava.de(texte);
    }

    @Override
    public SurvolLsp survol(String uri, PositionLsp position) {
        String texte = documents.get(uri);
        if (texte == null) {
            return null;
        }
        return CompletionJava.survol(texte, position);
    }

    @Override
    public SignatureLsp signature(String uri, PositionLsp position) {
        String texte = documents.get(uri);
        if (texte == null) {
            return null;
        }
        return CompletionJava.signature(texte, offset(texte, position));
    }

    @Override
    public ListeCompletion completion(String uri, PositionLsp position) {
        String texte = documents.get(uri);
        if (texte == null) {
            return null;
        }
        return CompletionJava.completion(texte, offset(texte, position));
    }

    /** L'offset d'une position LSP dans le texte, borné au texte. */
    private static int offset(String texte, PositionLsp position) {
        int ligne = 0;
        int offset = 0;
        while (ligne < position.ligne() && offset < texte.length()) {
            if (texte.charAt(offset) == '\n') {
                ligne++;
            }
            offset++;
        }
        return Math.min(texte.length(), offset + position.colonne());
    }

    @Override
    public List<SymboleLsp> symboles(String uri) {
        String texte = documents.get(uri);
        return texte == null ? List.of() : CompletionJava.symboles(texte);
    }

    @Override
    public List<PliLsp> plis(String uri) {
        String texte = documents.get(uri);
        if (texte == null) {
            return List.of();
        }
        List<PliLsp> plis = new ArrayList<>();
        java.util.ArrayDeque<Integer> pile = new java.util.ArrayDeque<>();
        int ligne = 0;
        for (int i = 0; i < texte.length(); i++) {
            char c = texte.charAt(i);
            if (c == '\n') {
                ligne++;
            } else if (c == '{') {
                pile.push(ligne);
            } else if (c == '}' && !pile.isEmpty()) {
                plis.add(new PliLsp(pile.pop(), ligne));
            }
        }
        return plis;
    }

    @Override
    public List<SurlignageLsp> surlignages(String uri, PositionLsp position) {
        String texte = documents.get(uri);
        if (texte == null) {
            return List.of();
        }
        return CompletionJava.surlignages(texte, position);
    }
}
