package jo.celeris.core.lsp;

import java.util.List;

/**
 * Les éditions d'un document de l'espace de travail : l'uri, la
 * version, la liste des éditions — une entrée
 * {@code textDocumentEdit} de {@code documentChanges}. La lecture
 * depuis les types de la bibliothèque vit derrière la frontière
 * protocolaire (ADR-015).
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
}
