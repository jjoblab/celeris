package jo.celeris.core.lsp;

import java.util.ArrayList;
import java.util.List;

/**
 * L'édition d'espace de travail (B-17, B-19) : les éditions de
 * document ET les édits de gabarit — le bloqué historique porté par le
 * protocole nôtre. Les deux formes vivent dans {@code documentChanges}
 * ; la forme héritée « changes » (carte uri → éditions, sans version)
 * se plie dans les documents.
 */
public record EditionEspaceLsp(List<EditionDocumentLsp> documents,
                               List<EditionGabaritLsp> gabarits) {

    /** L'espace vide — aucune édition, aucun gabarit. */
    public static final EditionEspaceLsp VIDE =
            new EditionEspaceLsp(List.of(), List.of());

    /** Constructeur canonique : les listes figées, jamais nulles. */
    public EditionEspaceLsp {
        documents = List.copyOf(documents);
        gabarits = List.copyOf(gabarits);
    }

    /**
     * Analyse l'édition d'espace depuis la réponse du serveur.
     *
     * @param json la valeur « result » (ou « edit » d'un correctif)
     * @return l'édition lue, vide si la réponse est vide ou nulle
     */
    public static EditionEspaceLsp de(ValeurJson json) {
        if (!(json instanceof JsonObjet objet)) {
            return VIDE;
        }
        List<EditionDocumentLsp> documents = new ArrayList<>();
        List<EditionGabaritLsp> gabarits = new ArrayList<>();
        if (objet.membre("documentChanges") instanceof JsonTableau changements) {
            for (int i = 0; i < changements.taille(); i++) {
                if (!(changements.element(i) instanceof JsonObjet entree)) {
                    continue;
                }
                if (EditionGabaritLsp.SORTE.equals(entree.membreTexte("kind"))) {
                    gabarits.add(EditionGabaritLsp.de(entree));
                } else {
                    documents.add(EditionDocumentLsp.de(entree));
                }
            }
        }
        if (documents.isEmpty() && gabarits.isEmpty()
                && objet.membre("changes") instanceof JsonObjet carte) {
            for (String uri : carte.cles()) {
                List<EditionTexteLsp> editions = new ArrayList<>();
                if (carte.membre(uri) instanceof JsonTableau tableau) {
                    for (int i = 0; i < tableau.taille(); i++) {
                        editions.add(EditionTexteLsp.de(tableau.element(i)));
                    }
                }
                documents.add(new EditionDocumentLsp(uri, -1, editions));
            }
        }
        return new EditionEspaceLsp(documents, gabarits);
    }

    /** La forme JSON de l'édition d'espace — documentChanges, les deux sortes. */
    public JsonObjet versJson() {
        JsonTableau changements = JsonTableau.vide();
        for (EditionDocumentLsp document : documents) {
            changements = changements.ajouter(document.versJson());
        }
        for (EditionGabaritLsp gabarit : gabarits) {
            changements = changements.ajouter(gabarit.versJson());
        }
        return JsonObjet.vide().ajouter("documentChanges", changements);
    }
}
