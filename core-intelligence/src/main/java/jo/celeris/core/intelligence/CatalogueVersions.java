package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Les catalogues de versions (C-08) : la lecture d'un
 * {@code libs.versions.toml} rend ses sections — versions,
 * bibliothèques, extensions, greffons — et les alias suggérés dans
 * les fichiers de build suivent l'accès par accesseur : les tirets
 * deviennent des points, {@code bundles} ouvre son espace de noms.
 *
 * <p>La lecture est ligne à ligne, tolérante : une section s'ouvre par
 * son en-tête entre crochets, un alias se déclare avant le signe
 * égale, les commentaires et les lignes sans égale traversent. Le
 * format TOML complet vit dans la voie de grammaires (M4) — ici, seul
 * le squelette du catalogue intéresse la complétion des builds.</p>
 */
public final class CatalogueVersions {

    private final Set<String> versions;

    private final Set<String> bibliotheques;

    private final Set<String> extensions;

    private final Set<String> greffons;

    private CatalogueVersions(Set<String> versions, Set<String> bibliotheques,
            Set<String> extensions, Set<String> greffons) {
        this.versions = versions;
        this.bibliotheques = bibliotheques;
        this.extensions = extensions;
        this.greffons = greffons;
    }

    /**
     * Lit un catalogue de versions.
     *
     * @param toml le contenu du fichier {@code libs.versions.toml}
     * @return le catalogue lu (vide sur un contenu vide ou sans section)
     */
    public static CatalogueVersions lire(String toml) {
        Set<String> versions = new LinkedHashSet<>();
        Set<String> bibliotheques = new LinkedHashSet<>();
        Set<String> extensions = new LinkedHashSet<>();
        Set<String> greffons = new LinkedHashSet<>();
        String section = "";
        for (String ligne : toml.split("\n", -1)) {
            String propre = ligne.strip();
            if (propre.startsWith("#") || propre.isEmpty()) {
                continue;
            }
            if (propre.startsWith("[") && propre.endsWith("]")) {
                section = propre.substring(1, propre.length() - 1);
                continue;
            }
            int egal = propre.indexOf('=');
            if (egal <= 0) {
                continue;
            }
            String alias = propre.substring(0, egal).strip();
            if (alias.isEmpty()) {
                continue;
            }
            switch (section) {
                case "versions" -> versions.add(alias);
                case "libraries" -> bibliotheques.add(alias);
                case "bundles" -> extensions.add(alias);
                case "plugins" -> greffons.add(alias);
                default -> {
                    // hors des sections connues : rien ne se lit
                }
            }
        }
        return new CatalogueVersions(versions, bibliotheques, extensions, greffons);
    }

    /**
     * Les alias suggérés dans un fichier de build pour le préfixe
     * tapé : {@code libs.} suggère les bibliothèques,
     * {@code libs.bundles.} les extensions.
     *
     * @param catalogue le catalogue lu
     * @param prefixe   le préfixe tapé ({@code libs.},
     *                  {@code libs.bundles.}…)
     * @return les accès suggérés, tirets en points (peut être vide)
     */
    public static List<String> completerAlias(CatalogueVersions catalogue, String prefixe) {
        List<String> candidats = new ArrayList<>();
        if (prefixe.equals("libs.")) {
            for (String alias : catalogue.bibliotheques) {
                candidats.add("libs." + alias.replace('-', '.'));
            }
        } else if (prefixe.equals("libs.bundles.")) {
            for (String alias : catalogue.extensions) {
                candidats.add("libs.bundles." + alias.replace('-', '.'));
            }
        }
        return List.copyOf(candidats);
    }

    /** Les alias de la section versions. */
    public Set<String> versions() {
        return versions;
    }

    /** Les alias de la section libraries. */
    public Set<String> bibliotheques() {
        return bibliotheques;
    }

    /** Les alias de la section bundles. */
    public Set<String> extensions() {
        return extensions;
    }

    /** Les alias de la section plugins. */
    public Set<String> greffons() {
        return greffons;
    }
}
