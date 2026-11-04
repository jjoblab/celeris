package jo.celeris.core.text;

/**
 * Encodage d'octets d'un fichier texte (parité A-24).
 *
 * <p>Le cœur décode et encode strictement les trois familles Unicode
 * couvertes par des marques d'ordre des octets — UTF-8, UTF-16 et
 * UTF-32, chacune dans ses deux orientations — plus le Latin-1 imposé
 * par l'hôte (aucune marque ne le désigne : la détection automatique
 * reste Unicode, le Latin-1 est un choix explicite de l'hôte).
 * Les autres encodages hérités relèvent de la couche hôte : le
 * composant embarquable ne promet que l'essentiel, sans table de
 * conversion à maintenir.</p>
 */
public enum Encoding {

    /** UTF-8 sans orientation, marque EF BB BF. */
    UTF_8,

    /** UTF-16 petit-boutiste, marque FF FE. */
    UTF_16LE,

    /** UTF-16 gros-boutiste, marque FE FF. */
    UTF_16BE,

    /** UTF-32 petit-boutiste, marque FF FE 00 00. */
    UTF_32LE,

    /** UTF-32 gros-boutiste, marque 00 00 FE FF. */
    UTF_32BE,

    /** Latin-1 (ISO 8859-1), un octet par caractère U+0000 à U+00FF. */
    LATIN_1
}
