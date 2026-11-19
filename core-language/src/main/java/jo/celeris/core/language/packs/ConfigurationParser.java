package jo.celeris.core.language.packs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jo.celeris.core.language.JsonReader;
import jo.celeris.core.language.model.LanguageConfiguration;
import org.jspecify.annotations.Nullable;

/**
 * L'analyseur de la configuration d'un langage (M7.6) : la partie
 * {@code language-configuration.json} de VS Code du paquet —
 * commentaires, crochets, fermeture automatique, entourage, plis,
 * règles d'indentation, taille d'indentation — séparé de l'analyseur
 * du manifeste et du descripteur : chaque classe reste sous ses
 * plafonds de complexité (ADR-002), chaque champ fautif se nomme.
 */
final class ConfigurationParser {

    private ConfigurationParser() {
    }

    /** La configuration d'un language.json lu. */
    static LanguageConfiguration parse(String json) {
        Map<String, Object> champs = JsonChamps.objet(JsonReader.read(json),
                "language.json");
        return new LanguageConfiguration(
                commentaires(champs.get("comments")),
                bracketPairs(champs.get("brackets")),
                autoPairs(champs.get("autoClosingPairs")),
                bracketPairs(champs.get("surroundingPairs")),
                JsonChamps.chaineNulle(champs.get("wordPattern")),
                folds(champs.get("folding")),
                indentation(champs.get("indentationRules")),
                indentSize(champs.get("indentSize")),
                JsonChamps.booleen(champs.get("tabIndent"), false));
    }


    /** Les commentaires : ligne, bloc (la paire de VS Code). */
    static LanguageConfiguration.Comments commentaires(@Nullable Object brut) {
        if (brut == null) {
            return new LanguageConfiguration.Comments(null, null, null);
        }
        Map<String, Object> champs = JsonChamps.objet(brut, "comments");
        String start = null;
        String end = null;
        if (champs.get("blockComment") != null) {
            List<?> pair = JsonChamps.liste(champs.get("blockComment"), "blockComment");
            if (pair.size() != 2 || pair.get(0) == null || pair.get(1) == null) {
                throw new IllegalArgumentException(
                        "blockComment must be a pair [start, end]");
            }
            start = String.valueOf(pair.get(0));
            end = String.valueOf(pair.get(1));
        }
        return new LanguageConfiguration.Comments(
                JsonChamps.chaineNulle(champs.get("lineComment")), start, end);
    }

    /** Les paires de crochets : [["(", ")"], …]. */
    static List<LanguageConfiguration.BracketPair> bracketPairs(
            @Nullable Object brut) {
        List<LanguageConfiguration.BracketPair> paires = new ArrayList<>();
        if (brut == null) {
            return paires;
        }
        for (Object element : JsonChamps.liste(brut, "brackets")) {
            List<?> pair = JsonChamps.liste(element, "brackets[]");
            if (pair.size() != 2) {
                throw new IllegalArgumentException(
                        "brackets[] must be a pair, found " + pair.size());
            }
            paires.add(new LanguageConfiguration.BracketPair(
                    JsonChamps.caractere(pair.get(0), "brackets[0]"),
                    JsonChamps.caractere(pair.get(1), "brackets[1]")));
        }
        return paires;
    }

    /** Les paires à fermeture automatique : objets {open, close, notIn}. */
    static List<LanguageConfiguration.AutoClosePair> autoPairs(
            @Nullable Object brut) {
        List<LanguageConfiguration.AutoClosePair> paires = new ArrayList<>();
        if (brut == null) {
            return paires;
        }
        for (Object element : JsonChamps.liste(brut, "autoClosingPairs")) {
            Map<String, Object> champs = JsonChamps.objet(element, "autoClosingPairs[]");
            List<String> exclus = new ArrayList<>();
            if (champs.get("notIn") != null) {
                for (Object contexte : JsonChamps.liste(champs.get("notIn"), "notIn")) {
                    exclus.add(String.valueOf(contexte));
                }
            }
            paires.add(new LanguageConfiguration.AutoClosePair(
                    JsonChamps.caractere(champs.get("open"), "autoClosingPairs[].open"),
                    JsonChamps.caractere(champs.get("close"), "autoClosingPairs[].close"),
                    Set.copyOf(exclus)));
        }
        return paires;
    }

    /** Les plis : offSide et marqueurs de VS Code, parIndentation en plus. */
    static LanguageConfiguration.Folding folds(@Nullable Object brut) {
        boolean parIndentation = false;
        boolean offSide = false;
        String start = null;
        String end = null;
        if (brut != null) {
            Map<String, Object> champs = JsonChamps.objet(brut, "folding");
            parIndentation = JsonChamps.booleen(champs.get("byIndentation"), false);
            offSide = JsonChamps.booleen(champs.get("offSide"), false);
            if (champs.get("markers") != null) {
                Map<String, Object> marqueurs = JsonChamps.objet(champs.get("markers"),
                        "folding.markers");
                start = JsonChamps.chaineNulle(marqueurs.get("start"));
                end = JsonChamps.chaineNulle(marqueurs.get("end"));
            }
        }
        return new LanguageConfiguration.Folding(parIndentation, offSide, start, end);
    }

    /** Les règles d'indentation de VS Code. */
    static LanguageConfiguration.IndentRules indentation(@Nullable Object brut) {
        if (brut == null) {
            return new LanguageConfiguration.IndentRules(null, null, null, null);
        }
        Map<String, Object> champs = JsonChamps.objet(brut, "indentationRules");
        return new LanguageConfiguration.IndentRules(
                JsonChamps.chaineNulle(champs.get("increaseIndentPattern")),
                JsonChamps.chaineNulle(champs.get("decreaseIndentPattern")),
                JsonChamps.chaineNulle(champs.get("indentNextLinePattern")),
                JsonChamps.chaineNulle(champs.get("unIndentedLinePattern")));
    }

    /** La taille d'indentation : 4 par défaut, entier strictement positif. */
    static int indentSize(@Nullable Object brut) {
        if (brut == null) {
            return 4;
        }
        if (!(brut instanceof Double nombre) || nombre != Math.floor(nombre)
                || nombre < 1 || nombre > 16) {
            throw new IllegalArgumentException(
                    "indentSize must be an integer between 1 and 16");
        }
        return nombre.intValue();
    }

}
