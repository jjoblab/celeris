package jo.celeris.core.language.packs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jo.celeris.core.language.JsonReader;
import jo.celeris.core.language.model.LanguageConfiguration;
import jo.celeris.core.language.model.LanguageDescriptor;
import jo.celeris.core.language.model.LanguageInjection;
import jo.celeris.core.syntax.structure.OutlineRule;
import org.jspecify.annotations.Nullable;



/**
 * L'analyseur du manifeste, du descripteur et des extraits d'un paquet
 * (M7.6) ; la configuration vit dans son analyseur dédié
 * ({@link ConfigurationParser}), les champs JSON se lisent par les
 * aides partagées ({@link JsonChamps}). Les noms de champs suivent
 * le {@code language-configuration.json} de VS Code quand il en a un
 * (comments, brackets, autoClosingPairs, surroundingPairs, wordPattern,
 * indentationRules, folding), et les extensions celeris sinon
 * (badge, badgeColor, indentSize, tabIndent, syntax, injections) —
 * chaque extension est documentée au champ correspondant. Les clés
 * inconnues s'ignorent : un paquet écrit pour une version plus récente
 * du schéma reste lisible ; les valeurs fausses se refusent avec le
 * champ en cause.
 */
final class PackParser {

    private PackParser() {
    }

    /** Le manifeste d'un pack.json lu. */
    static PackManifest manifest(String json) {
        Map<String, Object> champs = JsonChamps.objet(JsonReader.read(json), "pack.json");
        int schema = JsonChamps.entierRequis(champs, "schemaVersion", "pack.json");
        if (schema != PackManifest.CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("pack.json: unknown schemaVersion "
                    + schema + " (expected "
                    + PackManifest.CURRENT_SCHEMA_VERSION + ")");
        }
        return new PackManifest(
                JsonChamps.chaineRequise(champs, "id", "pack.json"),
                JsonChamps.chaineRequise(champs, "version", "pack.json"),
                schema,
                JsonChamps.chaineRequise(champs, "license", "pack.json"),
                JsonChamps.chaineRequise(champs, "provenance", "pack.json"));
    }

    /** Le descripteur d'un language.json lu. */
    static LanguageDescriptor descriptor(String json) {
        Map<String, Object> champs = JsonChamps.objet(JsonReader.read(json), "language.json");
        return new LanguageDescriptor(
                JsonChamps.chaineRequise(champs, "id", "language.json"),
                JsonChamps.chaineRequise(champs, "name", "language.json"),
                JsonChamps.chaines(champs.get("aliases")),
                extensions(champs.get("extensions")),
                JsonChamps.chainesMinuscules(champs.get("filenames")),
                JsonChamps.chaines(champs.get("filenamePatterns")),
                JsonChamps.chaineNulle(champs.get("firstLine")),
                JsonChamps.chaines(champs.get("mimetypes")),
                JsonChamps.chaineNulle(champs.get("parent")),
                JsonChamps.chaineNulle(champs.get("contentHint")),
                badge(champs),
                couleurBadge(champs));
    }

    /** La configuration d'un language.json lu (son analyseur dédié). */
    static LanguageConfiguration configuration(String json) {
        return ConfigurationParser.parse(json);
    }

    /** La préférence syntaxique d'un language.json lu. */
    static LanguagePack.SyntaxPreference syntaxPreference(String json) {
        Object brut = JsonChamps.objet(JsonReader.read(json), "language.json").get("syntax");
        if (brut == null) {
            return new LanguagePack.SyntaxPreference("grammar", null);
        }
        Map<String, Object> champs = JsonChamps.objet(brut, "syntax");
        String preferred = JsonChamps.chaineRequise(champs, "preferred", "syntax");
        return new LanguagePack.SyntaxPreference(preferred,
                JsonChamps.chaineNulle(champs.get("fallback")));
    }

    /** Les incorporations d'un language.json lu. */
    static List<LanguageInjection> injections(String json) {
        Object brut = JsonChamps.objet(JsonReader.read(json), "language.json").get("injections");
        if (brut == null) {
            return List.of();
        }
        List<LanguageInjection> injections = new ArrayList<>();
        for (Object element : JsonChamps.liste(brut, "injections")) {
            Map<String, Object> champs = JsonChamps.objet(element, "injections[]");
            injections.add(new LanguageInjection(
                    JsonChamps.chaineRequise(champs, "scope", "injections[]"),
                    JsonChamps.chaineRequise(champs, "language", "injections[]")));
        }
        return injections;
    }

    /** Les extraits d'un snippets.json lu (le format de VS Code). */
    static List<PackSnippet> snippets(String json) {
        Map<String, Object> champs = JsonChamps.objet(JsonReader.read(json), "snippets.json");
        List<PackSnippet> extraits = new ArrayList<>();
        for (Map.Entry<String, Object> entree : champs.entrySet()) {
            Map<String, Object> extrait = JsonChamps.objet(entree.getValue(), "snippets.json");
            extraits.add(new PackSnippet(
                    entree.getKey(),
                    JsonChamps.chaineRequise(extrait, "prefix", "snippets.json entry"),
                    corps(extrait.get("body"), entree.getKey()),
                    JsonChamps.chaineNulle(extrait.get("description"))));
        }
        return extraits;
    }

    /**
     * Les règles du plan d'un outline.json lu (M7.6, H-08) : motif,
     * catégorie et groupe du nom — la première règle qui matche nomme
     * la ligne. Chaque méprise (motif qui ne compile pas, catégorie
     * inconnue, groupe hors bornes) se refuse avec le champ en cause.
     */
    static List<OutlineRule> outline(String json) {
        Object brut = JsonChamps.objet(JsonReader.read(json), "outline.json")
                .get("rules");
        if (brut == null) {
            return List.of();
        }
        List<OutlineRule> regles = new ArrayList<>();
        int indice = 0;
        for (Object element : JsonChamps.liste(brut, "rules")) {
            Map<String, Object> champs = JsonChamps.objet(element, "rules[]");
            String ou = "rules[" + indice + "]";
            try {
                regles.add(OutlineRule.of(
                        JsonChamps.chaineRequise(champs, "pattern", ou),
                        JsonChamps.chaineRequise(champs, "category", ou),
                        JsonChamps.entierRequis(champs, "name", ou)));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("outline.json " + ou + ": "
                        + e.getMessage(), e);
            }
            indice++;
        }
        return List.copyOf(regles);
    }

    /** Le badge du descripteur : deux lettres par défaut. */
    private static String badge(Map<String, Object> champs) {
        String badge = JsonChamps.chaineNulle(champs.get("badge"));
        if (badge != null) {
            return badge;
        }
        String id = JsonChamps.chaineRequise(champs, "id", "language.json");
        return id.substring(0, Math.min(2, id.length())).toUpperCase(Locale.ROOT);
    }

    /** La couleur du badge : #RRGGBB ou #AARRGGBB, gris par défaut. */
    private static int couleurBadge(Map<String, Object> champs) {
        String hex = JsonChamps.chaineNulle(champs.get("badgeColor"));
        if (hex == null) {
            return 0xFF9AA0A6;
        }
        String chiffres = hex.startsWith("#") ? hex.substring(1) : hex;
        if (chiffres.length() != 6 && chiffres.length() != 8) {
            throw new IllegalArgumentException(
                    "badgeColor must be #RRGGBB or #AARRGGBB: " + hex);
        }
        try {
            long valeur = Long.parseLong(chiffres, 16);
            if (chiffres.length() == 6) {
                return (int) (0xFF000000L | valeur);
            }
            return (int) valeur;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "badgeColor must be hexadecimal: " + hex);
        }
    }

    /** Les extensions : le point de tête de VS Code se retire, en minuscules. */
    private static List<String> extensions(@Nullable Object brut) {
        List<String> extensions = new ArrayList<>();
        if (brut == null) {
            return extensions;
        }
        for (Object element : JsonChamps.liste(brut, "extensions")) {
            String extension = String.valueOf(element).toLowerCase(Locale.ROOT);
            while (extension.startsWith(".")) {
                extension = extension.substring(1);
            }
            if (extension.isEmpty()) {
                throw new IllegalArgumentException("extensions[] must not be empty");
            }
            extensions.add(extension);
        }
        return extensions;
    }

    /** Le corps d'un extrait : chaîne ou liste de lignes. */
    private static List<String> corps(@Nullable Object brut, String nom) {
        if (brut == null) {
            throw new IllegalArgumentException(
                    "snippet body is missing: " + nom);
        }
        if (brut instanceof List<?> lignes) {
            List<String> corps = new ArrayList<>();
            for (Object ligne : lignes) {
                corps.add(String.valueOf(ligne));
            }
            if (corps.isEmpty()) {
                throw new IllegalArgumentException(
                        "snippet body must not be empty: " + nom);
            }
            return corps;
        }
        return List.of(String.valueOf(brut));
    }

}
