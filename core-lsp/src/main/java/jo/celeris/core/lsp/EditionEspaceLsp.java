package jo.celeris.core.lsp;

import java.util.List;

/**
 * L'édition d'espace de travail (B-17, B-19) : les éditions de
 * document ET les édits de gabarit — le bloqué historique porté par le
 * protocole nôtre. Les deux formes vivent dans {@code documentChanges}
 * ; la forme héritée « changes » (carte uri → éditions, sans version)
 * se plie dans les documents. La lecture de la réponse du serveur —
 * l'objet {@code WorkspaceEdit} de la bibliothèque — vit derrière la
 * frontière protocolaire (ADR-015) et rejoint la correspondance à la
 * couverture nouvelle (H-12).
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
}
