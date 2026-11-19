package jo.celeris.core.syntax.treesitter;

import com.itsaky.androidide.treesitter.json.TSLanguageJson;
import com.itsaky.androidide.treesitter.python.TSLanguagePython;
import com.itsaky.androidide.treesitter.TSLanguage;

/**
 * Les grammaires servies par la voie (ADR-0025) : celles que la
 * liaison AndroidIDE PUBLIE sur Maven — json et python. Chaque
 * grammaire porte sa classe de langue (bloc statique : la bibliothèque
 * de grammaire se charge d'elle-même) ; le dépôt n'en embarque
 * aucune autre.
 */
enum GrammaireNatif {

    /** La grammaire json — l'artefact tree-sitter-json. */
    JSON("json") {
        @Override
        TSLanguage language() {
            return TSLanguageJson.getInstance();
        }
    },

    /** La grammaire python — l'artefact tree-sitter-python. */
    PYTHON("python") {
        @Override
        TSLanguage language() {
            return TSLanguagePython.getInstance();
        }
    };

    private final String id;

    GrammaireNatif(String id) {
        this.id = id;
    }

    /** La langue tree-sitter de la grammaire. */
    abstract TSLanguage language();

    /** L'identifiant du langage servi. */
    String id() {
        return id;
    }
}
