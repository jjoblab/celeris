package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * La navigation des références de ressources (C-04) : aller à la
 * ressource définie — {@code @string/x} vers le {@code <string
 * name="x">} du fichier de valeurs, {@code @layout/x} vers le fichier
 * de mise en page — et aller à l'usage, chaque occurrence de la
 * référence dans le projet.
 *
 * <p>Le projet se donne en dictionnaire chemin → contenu : le cœur ne
 * connaît aucun système de fichiers (ADR-002), l'hôte sert les
 * contenus par son contrat E-01. La recherche est exacte —
 * {@code @string/app} ne mène pas à {@code @string/app_name} : la
 * référence suivie d'un caractère de nom ne compte pas.</p>
 */
public final class ReferencesRessources {

    private ReferencesRessources() {
    }

    /**
     * La définition d'une ressource : sa balise dans un fichier de
     * valeurs ou son fichier propre pour les mises en page et les
     * dessins.
     *
     * @param fichiers les fichiers du projet, chemin vers contenu
     * @param type     le type de la ressource ({@code string},
     *                 {@code layout}, {@code drawable}…)
     * @param nom      le nom de la ressource
     * @return le lieu de la définition, vide si introuvable
     */
    public static Optional<Lieu> definition(Map<String, String> fichiers,
            String type, String nom) {
        for (Map.Entry<String, String> fichier : fichiers.entrySet()) {
            Optional<Integer> balise = baliseDefinition(fichier.getKey(),
                    fichier.getValue(), type, nom);
            if (balise.isPresent()) {
                return Optional.of(new Lieu(fichier.getKey(), balise.get()));
            }
        }
        return Optional.empty();
    }

    /**
     * Les usages d'une ressource : chaque référence {@code @type/nom}
     * du projet, aux offsets exacts.
     *
     * @param fichiers les fichiers du projet, chemin vers contenu
     * @param type     le type de la ressource
     * @param nom      le nom de la ressource
     * @return les lieux des usages, dans l'ordre des fichiers (peut
     *         être vide, jamais null)
     */
    public static List<Lieu> usages(Map<String, String> fichiers, String type,
            String nom) {
        String reference = "@" + type + "/" + nom;
        List<Lieu> usages = new ArrayList<>();
        for (Map.Entry<String, String> fichier : fichiers.entrySet()) {
            String contenu = fichier.getValue();
            int depuis = 0;
            while (true) {
                int trouve = contenu.indexOf(reference, depuis);
                if (trouve < 0) {
                    break;
                }
                if (!estProlongee(contenu, trouve + reference.length())) {
                    usages.add(new Lieu(fichier.getKey(), trouve));
                }
                depuis = trouve + reference.length();
            }
        }
        return List.copyOf(usages);
    }

    /**
     * L'offset de la balise de définition dans un fichier : la balise
     * nommée du type dans un fichier de valeurs, la première balise du
     * fichier pour une ressource de fichier ({@code layout/x},
     * {@code drawable/x}).
     */
    private static Optional<Integer> baliseDefinition(String chemin, String contenu,
            String type, String nom) {
        if (estFichierDeType(chemin, type, nom)) {
            return Optional.of(contenu.indexOf('<'));
        }
        String balise = "<" + type + " name=\"" + nom + "\"";
        int offset = contenu.indexOf(balise);
        if (offset >= 0) {
            return Optional.of(offset);
        }
        return Optional.empty();
    }

    /** Vrai quand le chemin désigne le fichier propre de la ressource. */
    private static boolean estFichierDeType(String chemin, String type, String nom) {
        return chemin.endsWith("/" + type + "/" + nom + ".xml");
    }

    /** Vrai quand le caractère après la référence prolonge le nom. */
    private static boolean estProlongee(String contenu, int apres) {
        if (apres >= contenu.length()) {
            return false;
        }
        char c = contenu.charAt(apres);
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /** Un lieu dans un fichier : son chemin et son offset. */
    public record Lieu(String chemin, int offset) {
    }
}
