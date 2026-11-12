package jo.celeris.core.language.packs;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Un gabarit d'extrait d'un paquet (M7.6) : le format
 * {@code snippets.json} de VS Code, lu comme donnée — préfixe, corps,
 * description. La conversion en {@code Gabarit} troué se fera au
 * câblage de la capacité correspondante ; ici, le paquet ne porte que
 * la donnée validée.
 *
 * @param name        le nom de l'extrait
 * @param prefix      le préfixe déclencheur
 * @param body        le corps, lignes concaténées
 * @param description la description, ou null
 */
public record PackSnippet(String name, String prefix, List<String> body,
        @Nullable String description) {

    /** Constructeur canonique : nom, préfixe et corps figés, jamais vides. */
    public PackSnippet {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("snippet name must not be empty");
        }
        if (prefix == null) {
            throw new IllegalArgumentException("snippet prefix must not be null");
        }
        body = List.copyOf(body);
        if (body.isEmpty()) {
            throw new IllegalArgumentException("snippet body must not be empty");
        }
    }
}
