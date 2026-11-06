package jo.celeris.core.lsp;

import java.util.ArrayList;
import java.util.List;

/**
 * Un correctif rapide du serveur (B-17) : un titre à montrer et une
 * édition d'espace de travail à appliquer — par la voie de transaction,
 * diffable et annulable. Le correctif sans édition (le serveur n'en
 * donne pas toujours) reste un correctif : l'hôte montre le titre,
 * l'édition vide ne fait rien.
 *
 * @param titre   le titre du correctif, montré à l'utilisateur
 * @param edition l'édition d'espace à appliquer, vide si absente
 */
public record CorrectifLsp(String titre, EditionEspaceLsp edition) {

    /**
     * Analyse le correctif depuis sa forme JSON.
     *
     * @param json l'objet correctif de la réponse codeAction
     * @return le correctif lu — titre vide et édition vide si la forme
     *         est incomplète
     */
    public static CorrectifLsp de(ValeurJson json) {
        if (!(json instanceof JsonObjet objet)) {
            return new CorrectifLsp("", EditionEspaceLsp.VIDE);
        }
        String titre = objet.membreTexte("title");
        ValeurJson edition = objet.membre("edit");
        return new CorrectifLsp(titre == null ? "" : titre,
                edition == null ? EditionEspaceLsp.VIDE : EditionEspaceLsp.de(edition));
    }

    /**
     * Analyse la liste des correctifs de la réponse codeAction.
     *
     * @param json la valeur « result » — un tableau de correctifs
     * @return les correctifs lus, vide si la réponse est vide ou nulle
     */
    public static List<CorrectifLsp> liste(ValeurJson json) {
        List<CorrectifLsp> correctifs = new ArrayList<>();
        if (json instanceof JsonTableau tableau) {
            for (int i = 0; i < tableau.taille(); i++) {
                correctifs.add(CorrectifLsp.de(tableau.element(i)));
            }
        }
        return List.copyOf(correctifs);
    }
}
