package jo.celeris.core.language.packs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Les aides de lecture des champs JSON d'un paquet (M7.6) : objets,
 * tableaux, chaînes exigées ou nulles, entiers, booléens, caractères,
 * listes de chaînes — chaque méprise se refuse avec le champ en cause,
 * et la profondeur d'imbrication reste bornée. Partagées par
 * l'analyseur du manifeste/descripteur et celui de la configuration.
 */
public final class JsonChamps {

    /** La profondeur maximale d'imbrication du JSON d'un paquet. */
    public static final int MAX_DEPTH = 64;

    private JsonChamps() {
    }

    /** Une valeur en objet, le champ nommé dans l'erreur. */
    public static Map<String, Object> objet(Object brut, String ou) {
        if (!(brut instanceof Map<?, ?> champs)) {
            throw new IllegalArgumentException(ou + " must be an object");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> types = (Map<String, Object>) champs;
        verifierProfondeur(types, 0);
        return types;
    }

    /** Une valeur en liste, le champ nommé dans l'erreur. */
    public static List<?> liste(Object brut, String ou) {
        if (!(brut instanceof List<?> elements)) {
            throw new IllegalArgumentException(ou + " must be an array");
        }
        return elements;
    }

    /** La profondeur d'imbrication reste bornée : jamais de pile épuisée. */
    public static void verifierProfondeur(Object valeur, int profondeur) {
        if (profondeur > MAX_DEPTH) {
            throw new IllegalArgumentException(
                    "pack JSON nests deeper than " + MAX_DEPTH);
        }
        if (valeur instanceof Map<?, ?> champs) {
            for (Object enfant : champs.values()) {
                verifierProfondeur(enfant, profondeur + 1);
            }
        } else if (valeur instanceof List<?> elements) {
            for (Object enfant : elements) {
                verifierProfondeur(enfant, profondeur + 1);
            }
        }
    }

    public static String chaineRequise(Map<String, Object> champs, String cle,
            String ou) {
        Object brut = champs.get(cle);
        if (!(brut instanceof String valeur) || valeur.isEmpty()) {
            throw new IllegalArgumentException(ou + ": " + cle + " must be a string");
        }
        return valeur;
    }

    public static @Nullable String chaineNulle(@Nullable Object brut) {
        return brut instanceof String valeur ? valeur : null;
    }

    public static List<String> chaines(@Nullable Object brut) {
        List<String> valeurs = new ArrayList<>();
        if (brut instanceof List<?> elements) {
            for (Object element : elements) {
                valeurs.add(String.valueOf(element));
            }
        }
        return valeurs;
    }

    public static List<String> chainesMinuscules(@Nullable Object brut) {
        List<String> valeurs = new ArrayList<>();
        if (brut instanceof List<?> elements) {
            for (Object element : elements) {
                valeurs.add(String.valueOf(element).toLowerCase(Locale.ROOT));
            }
        }
        return valeurs;
    }

    public static int entierRequis(Map<String, Object> champs, String cle,
            String ou) {
        Object brut = champs.get(cle);
        if (!(brut instanceof Double nombre) || nombre != Math.floor(nombre)) {
            throw new IllegalArgumentException(ou + ": " + cle + " must be an integer");
        }
        return nombre.intValue();
    }

    public static boolean booleen(@Nullable Object brut, boolean defaut) {
        return brut instanceof Boolean valeur ? valeur : defaut;
    }

    /** Un caractère : les paires sont des chaînes d'un caractère. */
    public static char caractere(@Nullable Object brut, String ou) {
        if (!(brut instanceof String valeur) || valeur.length() != 1) {
            throw new IllegalArgumentException(ou + " must be a single character");
        }
        return valeur.charAt(0);
    }
}
