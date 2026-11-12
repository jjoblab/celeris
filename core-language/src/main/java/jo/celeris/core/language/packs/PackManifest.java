package jo.celeris.core.language.packs;

/**
 * Le manifeste d'un paquet de langage (M7.6) : l'identité du paquet —
 * identifiant, version, version de schéma, licence de chaque fichier,
 * provenance. Le paquet est de la donnée non fiable : le manifeste se
 * lit strictement, la version de schéma décide de l'interprétation.
 *
 * @param id            l'identifiant du paquet, celui du langage
 * @param version       la version du paquet
 * @param schemaVersion la version de schéma lue — seule la courante
 *                      s'interprète
 * @param license       la licence des fichiers du paquet
 * @param provenance    la provenance des données (dépôt, URL, auteur)
 */
public record PackManifest(String id, String version, int schemaVersion,
        String license, String provenance) {

    /** La version de schéma courante — les autres se refusent. */
    public static final int CURRENT_SCHEMA_VERSION = 1;

    /** Constructeur canonique : aucun champ vide. */
    public PackManifest {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("pack id must not be empty");
        }
        if (version == null || version.isEmpty()) {
            throw new IllegalArgumentException("pack version must not be empty");
        }
        if (license == null || license.isEmpty()) {
            throw new IllegalArgumentException("pack license must not be empty");
        }
        if (provenance == null || provenance.isEmpty()) {
            throw new IllegalArgumentException("pack provenance must not be empty");
        }
    }
}
