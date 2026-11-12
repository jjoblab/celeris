package jo.celeris.core.language;

import java.util.List;
import jo.celeris.core.language.model.LanguageSupport;

/**
 * Le résultat d'une détection de langage (M7.6) : un langage l'emporte,
 * plusieurs restent à départager (l'interface peut les montrer), ou
 * aucun ne revendique le fichier.
 */
public sealed interface LanguageDetection {

    /** Un langage seul l'emporte. */
    record Resolved(LanguageSupport support) implements LanguageDetection {
    }

    /**
     * Plusieurs langages revendiquent le fichier à égalité : les
     * candidats, dans l'ordre d'inscription du registre.
     */
    record Ambiguous(List<LanguageSupport> candidates) implements LanguageDetection {

        /** Constructeur canonique : candidats figés, jamais vides. */
        public Ambiguous {
            candidates = List.copyOf(candidates);
            if (candidates.isEmpty()) {
                throw new IllegalArgumentException("ambiguous detection needs candidates");
            }
        }
    }

    /** Aucun langage ne revendique le fichier. */
    record Unresolved() implements LanguageDetection {
    }
}
