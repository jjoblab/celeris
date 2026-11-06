package jo.celeris.core.lsp;

import org.jspecify.annotations.Nullable;

/**
 * Le survol du serveur (B-09) : la documentation résolue À LA DEMANDE —
 * la requête part quand l'utilisateur survole, jamais avant. Le contenu
 * arrive en deux formes bornées : la chaîne nue ou le contenu balisé
 * (genre + texte) — la documentation balisée en clôtures Markdown
 * alimente le modèle de sections distinctes (PIEGE-16), le titre jamais
 * fondu dans le corps.
 *
 * @param texte la documentation survolée, vide si le serveur ne dit rien
 * @param genre le genre du contenu balisé (« markdown », « plaintext »),
 *              vide pour la chaîne nue
 * @param plage la plage qui porte le survol, nulle si le serveur n'en
 *              donne pas — jamais devinée
 */
public record SurvolLsp(String texte, String genre, @Nullable PlageLsp plage) {

    /** Le survol vide : aucune documentation, aucune plage. */
    public static final SurvolLsp VIDE = new SurvolLsp("", "", null);

    /**
     * Analyse le survol depuis sa réponse.
     *
     * @param json la valeur « result » de la requête de survol
     * @return le survol lu — vide si la réponse est vide ou nulle
     */
    public static SurvolLsp de(ValeurJson json) {
        if (json instanceof JsonChaine chaine) {
            return new SurvolLsp(chaine.texte(), "", null);
        }
        if (!(json instanceof JsonObjet objet)) {
            return VIDE;
        }
        String texte = "";
        String genre = "";
        if (objet.membre("contents") instanceof JsonObjet contenu) {
            String valeur = contenu.membreTexte("value");
            texte = valeur == null ? "" : valeur;
            String lu = contenu.membreTexte("kind");
            genre = lu == null ? "" : lu;
        }
        PlageLsp plage = null;
        if (objet.membre("range") instanceof JsonObjet portee) {
            plage = PlageLsp.de(portee);
        }
        return new SurvolLsp(texte, genre, plage);
    }
}
