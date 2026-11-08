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
                documentationDe(objet.membre("documentation")),
                objet.membreTexte("sortText"),
                objet.membreTexte("filterText"),
                objet.membreTexte("insertText"),
                Boolean.TRUE.equals(objet.membreBooleen("deprecated")));
    }

    /**
     * La documentation dans SES TROIS formes du protocole (le
     * redressement du terrain réel, M8.6 : le serveur XML embarqué
     * sert du contenu balisé) : chaîne nue, contenu balisé
     * {@code {kind, value}} — la valeur s'en tire —, ou tableau de
     * contenus balisés dont les valeurs se joignent.
     *
     * @param valeur le membre documentation, ou null
     * @return le texte de la documentation, null si absent
     */
    static @Nullable String documentationDe(@Nullable ValeurJson valeur) {
        if (valeur instanceof JsonChaine texte) {
            return texte.texte();
        }
        if (valeur instanceof JsonObjet balise) {
            return balise.membreTexte("value");
        }
        if (valeur instanceof JsonTableau morceaux) {
            return joindreValeurs(morceaux);
        }
        return null;
    }

    /** Les valeurs d'un tableau de contenus balisés, jointes au retour à la ligne. */
    private static @Nullable String joindreValeurs(JsonTableau morceaux) {
        if (morceaux.taille() == 0) {
            return null;
        }
        StringBuilder joint = new StringBuilder();
        for (int i = 0; i < morceaux.taille(); i++) {
            String texte = documentationDe(morceaux.element(i));
            if (texte != null) {
                if (joint.length() > 0) {
                    joint.append('\n');
                }
                joint.append(texte);
            }
        }
        return joint.length() == 0 ? null : joint.toString();
    }
}
