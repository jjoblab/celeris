package jo.celeris.core.language;

import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * Le descripteur d'un langage (M7.6, plateforme de langages) : tout ce
 * qui identifie un fichier comme étant de ce langage — identifiant,
 * nom affichable, alias, extensions, noms de fichier exacts
 * ({@code Dockerfile}), motifs de nom ({@code *.gradle.kts}), motif de
 * première ligne ({@code #!}), types MIME — plus la filiation
 * ({@code kts} hérite de {@code kotlin}) et l'enseigne du badge
 * (monogramme et couleur ARGB). Des données pures : aucun
 * comportement, aucune dépendance vers les analyseurs.
 *
 * <p>L'indice de contenu ({@code contentHint}) départage les
 * extensions que plusieurs langages revendiquent (un {@code .h} en C
 * ou C++) : parmi les candidats à égalité, celui dont l'indice
 * apparaît dans l'échantillon l'emporte — l'heuristique vit dans les
 * données, jamais dans un aiguillage d'identifiant.
 *
 * @param id               l'identifiant du langage (par exemple {@code java})
 * @param displayName      le nom affichable
 * @param aliases          les alias d'appellation, vides si aucun
 * @param extensions       les extensions sans point, l'extension composée
 *                         admise ({@code gradle.kts}), en minuscules
 * @param filenames        les noms de fichiers exacts ({@code Dockerfile}),
 *                         en minuscules
 * @param filenamePatterns les motifs de nom ({@code *.gradle.kts}) sur
 *                         les jokers {@code *} et {@code ?}
 * @param firstLine        le motif de regex de première ligne
 *                         ({@code ^#!.*bash}), ou null
 * @param mimeTypes        les types MIME, vides si aucun
 * @param parentId         l'identifiant du langage parent, ou null
 * @param contentHint      l'indice de contenu des extensions ambiguës,
 *                         ou null
 * @param badge            le monogramme du badge (une à trois lettres)
 * @param badgeColor       la couleur ARGB du badge
 */
public record LanguageDescriptor(
        String id,
        String displayName,
        List<String> aliases,
        List<String> extensions,
        List<String> filenames,
        List<String> filenamePatterns,
        @Nullable String firstLine,
        List<String> mimeTypes,
        @Nullable String parentId,
        @Nullable String contentHint,
        String badge,
        int badgeColor) {

    /** Constructeur canonique : listes figées et minuscules, identifiant valide. */
    public LanguageDescriptor {
        if (id == null || !id.equals(id.toLowerCase(Locale.ROOT))
                || id.isEmpty() || !id.matches("[a-z0-9+#-]+")) {
            throw new IllegalArgumentException("invalid language id: " + id);
        }
        if (displayName == null || displayName.isEmpty()) {
            throw new IllegalArgumentException("displayName must not be empty");
        }
        aliases = List.copyOf(aliases == null ? List.of() : aliases);
        extensions = List.copyOf(extensions == null ? List.of() : extensions);
        filenames = List.copyOf(filenames == null ? List.of() : filenames);
        filenamePatterns =
                List.copyOf(filenamePatterns == null ? List.of() : filenamePatterns);
        mimeTypes = List.copyOf(mimeTypes == null ? List.of() : mimeTypes);
        if (badge == null || badge.isEmpty()) {
            throw new IllegalArgumentException("badge must not be empty");
        }
        if (parentId != null && parentId.equals(id)) {
            throw new IllegalArgumentException("language " + id + " cannot be its own parent");
        }
    }

    /**
     * Le descripteur simple d'un langage : identifiant, nom et
     * extensions — le cas d'un enregistrement à la main sans paquet.
     *
     * @param id          l'identifiant du langage
     * @param displayName le nom affichable
     * @param extensions  les extensions sans point
     * @return le descripteur correspondant
     */
    public static LanguageDescriptor of(String id, String displayName,
            List<String> extensions) {
        return new LanguageDescriptor(id, displayName, List.of(), extensions,
                List.of(), List.of(), null, List.of(), null, null,
                id.substring(0, Math.min(2, id.length())).toUpperCase(Locale.ROOT),
                0xFF9AA0A6);
    }
}
