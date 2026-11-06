package jo.celeris.core.lsp;

import org.jspecify.annotations.Nullable;

/**
 * Un élément de complétion du protocole (B-04) : ce que le serveur
 * propose, dans ses termes — le modèle ne devine aucun champ absent.
 * Les textes de tri et de filtre appartiennent au serveur : les
 * utiliser, c'est respecter sa priorité (PIEGE-17).
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

    /**
     * Analyse l'élément depuis sa forme JSON.
     *
     * @param json l'objet élément du protocole
     * @return l'élément lu — les champs absents sont nuls ou nuls, jamais devinés
     */
    public static ElementCompletion de(ValeurJson json) {
        if (!(json instanceof JsonObjet objet)) {
            throw new IllegalArgumentException("completion item must be an object");
        }
        String libelle = objet.membreTexte("label");
        if (libelle == null) {
            throw new IllegalArgumentException("completion item requires label");
        }
        Long genre = objet.membreEntier("kind");
        return new ElementCompletion(libelle,
                genre == null ? 0 : genre.intValue(),
                objet.membreTexte("detail"),
                objet.membreTexte("documentation"),
                objet.membreTexte("sortText"),
                objet.membreTexte("filterText"),
                objet.membreTexte("insertText"),
                Boolean.TRUE.equals(objet.membreBooleen("deprecated")));
    }
}
