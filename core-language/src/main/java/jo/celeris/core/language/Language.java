package jo.celeris.core.language;

import java.util.List;

/**
 * Un langage au sens du registre (core-language) : identifiant, nom
 * affichable et extensions de fichiers — des données pures, sans aucune
 * dépendance vers les analyseurs. L'identification des dialectes composés
 * (scripts de construction) se fait par extension la plus longue.
 *
 * @param id          l'identifiant du langage (par exemple {@code java})
 * @param displayName le nom affichable
 * @param extensions  les extensions de fichiers, sans point
 */
public record Language(String id, String displayName, List<String> extensions) {

    /** Constructeur canonique : extensions figées, jamais nulles. */
    public Language {
        extensions = List.copyOf(extensions);
    }
}
