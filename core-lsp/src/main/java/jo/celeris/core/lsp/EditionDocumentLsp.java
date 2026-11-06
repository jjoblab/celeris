package jo.celeris.core.lsp;

import java.util.ArrayList;
import java.util.List;

/**
 * Les éditions d'un document de l'espace de travail : l'uri, la
 * version, la liste des éditions — une entrée
 * {@code textDocumentEdit} de {@code documentChanges}.
 *
 * @param uri      l'uri du document édité
 * @param version  la version du document, -1 quand le serveur ne la
 *                 donne pas (la forme héritée « changes »)
 * @param editions les éditions, dans l'ordre du serveur
 */
public record EditionDocumentLsp(String uri, int version,
                                 List<EditionTexteLsp> editions) {

    /** Constructeur canonique : la liste figée, jamais nulle. */
    public EditionDocumentLsp {
        editions = List.copyOf(editions);
    }

    /**
     * Analyse les éditions du document depuis sa forme JSON.
     *
     * @param json l'objet « textDocumentEdit »
     * @return les éditions lues — uri vide et liste vide si la forme
     *         est incomplète
     */
    public static EditionDocumentLsp de(ValeurJson json) {
        if (!(json instanceof JsonObjet objet)) {
            return new EditionDocumentLsp("", -1, List.of());
        }
        String uri = "";
        int version = -1;
        if (objet.membre("textDocument") instanceof JsonObjet document) {
            String lu = document.membreTexte("uri");
            uri = lu == null ? "" : lu;
            Long versionLue = document.membreEntier("version");
            version = versionLue == null ? -1 : versionLue.intValue();
        }
        List<EditionTexteLsp> editions = new ArrayList<>();
        if (objet.membre("edits") instanceof JsonTableau tableau) {
            for (int i = 0; i < tableau.taille(); i++) {
                editions.add(EditionTexteLsp.de(tableau.element(i)));
            }
        }
        return new EditionDocumentLsp(uri, version, editions);
    }

    /** La forme JSON de l'édition de document. */
    public JsonObjet versJson() {
        JsonObjet document = JsonObjet.vide().ajouter("uri", JsonChaine.of(uri));
        if (version >= 0) {
            document = document.ajouter("version", JsonNombre.of(version));
        }
        JsonTableau liste = JsonTableau.vide();
        for (EditionTexteLsp edition : editions) {
            liste = liste.ajouter(edition.versJson());
        }
        return JsonObjet.vide()
                .ajouter("textDocument", document)
                .ajouter("edits", liste);
    }
}
