package jo.celeris.core.ai;

/**
 * L'étiquette d'une proposition d'agent (F-04, ADR-012) : qui propose
 * et pourquoi — les éditions d'agent arrivent comme transactions
 * étiquetées, l'étiquette les distingue des gestes de l'utilisateur
 * dans la revue.
 *
 * @param auteur     l'auteur de la proposition (nom d'agent, version)
 * @param intention  l'intention déclarée, lue par l'utilisateur au
 *                   moment de décider
 */
public record EtiquetteAgent(String auteur, String intention) {

    /**
     * Valide l'étiquette : auteur et intention présents — une
     * proposition anonyme ne se révise pas.
     *
     * @throws IllegalArgumentException si l'auteur ou l'intention est vide
     */
    public EtiquetteAgent {
        if (auteur == null || auteur.isBlank()) {
            throw new IllegalArgumentException("auteur absent");
        }
        if (intention == null || intention.isBlank()) {
            throw new IllegalArgumentException("intention absente");
        }
    }
}
