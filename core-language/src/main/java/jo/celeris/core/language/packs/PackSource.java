package jo.celeris.core.language.packs;

import java.io.IOException;
import java.util.List;

/**
 * La source des paquets d'un hôte (M7.6) : d'où viennent les paquets —
 * les ressources du module (paquets intégrés), un dossier du stockage
 * (paquets de l'utilisateur), une archive. L'hôte peut fournir la
 * sienne ; le chargeur ne connaît que cette interface.
 */
public interface PackSource {

    /**
     * Les paquets lisibles, dans l'ordre de la source.
     *
     * @return les paquets, éventuellement vides
     * @throws IOException si la source ne se lit pas
     */
    List<PackFiles> packs() throws IOException;

    /**
     * Les fichiers d'un paquet, lus à la demande : les noms attendus
     * sont {@code pack.json}, {@code language.json}, {@code grammar.json},
     * {@code snippets.json} — un paquet reste une donnée plate, sans
     * sous-dossier.
     */
    interface PackFiles {

        /** L'identité du paquet pour les messages (chemin, nom). */
        String name();

        /**
         * Vrai si le paquet porte ce fichier.
         *
         * @param file le nom du fichier
         * @return sa présence
         */
        boolean has(String file);

        /**
         * Lit les octets d'un fichier du paquet.
         *
         * @param file le nom du fichier
         * @return ses octets
         * @throws IOException si le fichier ne se lit pas
         */
        byte[] read(String file) throws IOException;
    }
}
