package jo.celeris.core.lsp.types;

import org.jspecify.annotations.Nullable;

/**
 * Le survol du serveur (B-09) : la documentation résolue À LA DEMANDE —
 * la requête part quand l'utilisateur survole, jamais avant. Le contenu
 * arrive en deux formes bornées : la chaîne nue ou le contenu balisé
 * (genre + texte) — la documentation balisée en clôtures Markdown
 * alimente le modèle de sections distinctes (PIEGE-16), le titre jamais
 * fondu dans le corps. La lecture des formes du protocole vit derrière
 * la frontière protocolaire (ADR-015, CorrespondanceDocuments).
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
}
