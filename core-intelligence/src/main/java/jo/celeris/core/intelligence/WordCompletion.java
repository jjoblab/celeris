package jo.celeris.core.intelligence;

import java.util.List;

import jo.celeris.core.language.Completion;
import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.text.Document;

import org.jspecify.annotations.Nullable;

/**
 * La complétion générique par les mots du code (M7.6) : le fournisseur
 * de repli de la plateforme — tout langage sans complétion dédiée
 * garde la complétion, elle vient des mots du document. Le moteur
 * ({@link CompletionMots}, M7) ne bouge pas ; cette classe l'habille
 * en capacité de langage.
 */
public final class WordCompletion implements Completion {

    /** L'instance unique — le fournisseur est sans état. */
    public static final WordCompletion INSTANCE = new WordCompletion();

    private WordCompletion() {
    }

    /**
     * Les candidats : les mots du document autour du curseur — il faut
     * une analyse publiée (les mots se relèvent sur les jetons) ;
     * sans elle, rien ne se propose encore : la première publication
     * servira.
     *
     * @param document     le document édité
     * @param analysis     l'analyse publiée, ou null tant qu'aucune
     *                     n'existe
     * @param caretOffset  la position du curseur
     * @return les mots candidats, jamais nuls
     */
    @Override
    public List<String> candidates(Document document,
            @Nullable Analysis analysis, int caretOffset) {
        if (analysis == null) {
            return List.of();
        }
        return CompletionMots.completer(document, analysis, caretOffset,
                List.of());
    }
}
