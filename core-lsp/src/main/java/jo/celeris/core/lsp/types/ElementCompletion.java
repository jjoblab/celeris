package jo.celeris.core.lsp.types;

import org.jspecify.annotations.Nullable;

/**
 * Un élément de complétion du protocole (B-04) : ce que le serveur
 * propose, dans ses termes — le modèle ne devine aucun champ absent.
 * Les textes de tri et de filtre appartiennent au serveur : les
 * utiliser, c'est respecter sa priorité (PIEGE-17). La lecture depuis
 * les types de la bibliothèque — documentation dans ses formes du
 * protocole comprise (PIEGE-16, le redressement M8.6) — vit derrière
 * la frontière protocolaire (ADR-015, CorrespondanceDocuments).
 *
 * @param libelle        le libellé affiché et inséré par défaut
 * @param genre          le genre (méthode, champ, mot-clé…), zéro si absent
 * @param detail         le détail (signature courte), ou null
 * @param documentation  la documentation, ou null
 * @param texteTri       la clé de tri du serveur, ou null
 * @param texteFiltre    le texte de filtrage du serveur, ou null
 * @param texteInsertion le texte à insérer, ou null (le libellé alors)
 * @param obsolete       vrai quand le serveur marque l'élément obsolète
 */
public record ElementCompletion(String libelle, int genre, @Nullable String detail,
        @Nullable String documentation, @Nullable String texteTri,
        @Nullable String texteFiltre, @Nullable String texteInsertion, boolean obsolete) {
}
