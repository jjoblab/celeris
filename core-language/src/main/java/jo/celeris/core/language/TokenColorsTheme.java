package jo.celeris.core.language;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jo.celeris.core.syntax.ScopeStyle;
import jo.celeris.core.syntax.ScopeTheme;

/**
 * L'importeur de thèmes {@code tokenColors} de VS Code (M7.6, H-07) :
 * le fichier se lit ici, ses règles « sélecteur → réglages »
 * deviennent celles du thème par portées — l'éditeur n'apprend
 * aucune couleur, il en reçoit. Les couleurs d'éditeur connues
 * ({@code editor.background}, {@code editor.foreground}) se lisent
 * pour l'hôte qui veut y fonder son chrome ; les autres clés
 * s'ignorent.
 *
 * <p>Le format lu : {@code tokenColors} porte des règles dont le
 * sélecteur est une chaîne ou une liste de chaînes (séparées par des
 * virgules dans les deux cas), les réglages portent
 * {@code foreground} (aux formats #RRGGBB ou #AARRGGBB) et
 * {@code fontStyle} (les mots {@code bold}, {@code italic},
 * {@code underline} se combinent). Une règle sans sélecteur se
 * refuse — l'encre globale vit dans {@code editor.foreground}. Les
 * inclusions ({@code include}) ne se suivent pas : un fichier, un
 * thème.</p>
 *
 * @param name       le nom du thème, éventuellement vide
 * @param background l'encre de fond lue, ou zéro
 * @param foreground l'encre de base lue, ou zéro
 * @param scopes     le thème par portées bâti des règles lues
 */
public record TokenColorsTheme(String name, int background, int foreground,
        ScopeTheme scopes) {

    /**
     * Lit un thème de son texte JSON — chaque règle se valide entière,
     * la première faute nomme sa position.
     *
     * @param json le texte du fichier de thème
     * @return le thème lu
     */
    public static TokenColorsTheme parse(String json) {
        Map<String, Object> racine =
                JsonChamps.objet(JsonReader.read(json), "theme");
        String nom = JsonChamps.chaineNulle(racine.get("name"));
        Map<String, Object> couleurs = couleurs(racine.get("colors"));
        List<ScopeTheme.Rule> regles = new ArrayList<>();
        Object brut = racine.get("tokenColors");
        if (brut != null) {
            int indice = 0;
            for (Object element : JsonChamps.liste(brut, "tokenColors")) {
                String ou = "tokenColors[" + indice + "]";
                regles.add(regle(JsonChamps.objet(element, ou), ou));
                indice++;
            }
        }
        return new TokenColorsTheme(nom == null ? "" : nom,
                encre(couleurs.get("editor.background"),
                        "colors.editor.background"),
                encre(couleurs.get("editor.foreground"),
                        "colors.editor.foreground"),
                ScopeTheme.of(regles));
    }

    /** Les couleurs d'éditeur du thème, vides sans section colors. */
    private static Map<String, Object> couleurs(@org.jspecify.annotations.Nullable
            Object brut) {
        if (brut == null) {
            return Map.of();
        }
        return JsonChamps.objet(brut, "colors");
    }

    /** Une règle tokenColors : sélecteur exigé, réglages lus. */
    private static ScopeTheme.Rule regle(Map<String, Object> entree, String ou) {
        List<String> selecteurs = new ArrayList<>();
        Object brut = entree.get("scope");
        if (brut instanceof List<?> liste) {
            for (Object element : liste) {
                selecteurs.add(String.valueOf(element));
            }
        } else if (brut instanceof String chaine) {
            selecteurs.add(chaine);
        } else {
            throw new IllegalArgumentException(ou
                    + ": scope must be a string or an array of strings");
        }
        selecteurs.removeIf(String::isEmpty);
        if (selecteurs.isEmpty()) {
            throw new IllegalArgumentException(ou + ": scope is empty —"
                    + " the global ink lives in colors.editor.foreground");
        }
        Object reglagesBruts = entree.get("settings");
        if (reglagesBruts == null) {
            throw new IllegalArgumentException(ou + ": settings is missing");
        }
        Map<String, Object> reglages =
                JsonChamps.objet(reglagesBruts, ou + ".settings");
        ScopeStyle style = new ScopeStyle(
                encre(reglages.get("foreground"), ou + ".settings.foreground"),
                attribut(reglages.get("fontStyle"), "bold"),
                attribut(reglages.get("fontStyle"), "italic"),
                attribut(reglages.get("fontStyle"), "underline"));
        return new ScopeTheme.Rule(String.join(", ", selecteurs), style);
    }

    /** Vrai quand le style de police compte le mot donné. */
    private static boolean attribut(@org.jspecify.annotations.Nullable Object brut,
            String mot) {
        return brut instanceof String chaine
                && chaine.toLowerCase(Locale.ROOT).contains(mot);
    }

    /** Une encre #RRGGBB ou #AARRGGBB — zéro absente. */
    private static int encre(@org.jspecify.annotations.Nullable Object brut,
            String ou) {
        if (brut == null) {
            return 0;
        }
        if (!(brut instanceof String texte) || !texte.startsWith("#")) {
            throw new IllegalArgumentException(ou + " must be #RRGGBB or"
                    + " #AARRGGBB: " + brut);
        }
        String chiffres = texte.substring(1);
        try {
            if (chiffres.length() == 6) {
                return (int) (0xFF000000L
                        | Long.parseLong(chiffres, 16));
            }
            if (chiffres.length() == 8) {
                return (int) Long.parseLong(chiffres, 16);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(ou + " is not hexadecimal: "
                    + texte, e);
        }
        throw new IllegalArgumentException(ou + " must be #RRGGBB or"
                + " #AARRGGBB: " + texte);
    }
}
