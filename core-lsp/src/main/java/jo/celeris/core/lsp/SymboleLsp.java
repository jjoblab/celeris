package jo.celeris.core.lsp;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Un symbole de document (H-12) : le nom, le genre au protocole, la
 * plage qui l'étend et celle que la sélection retient — le plan, le fil
 * d'Ariane et l'aller au symbole y trouvent leur matière. Les enfants
 * portent la hiérarchie du serveur, figée à la construction.
 *
 * @param nom             le nom montré
 * @param genre           le genre au protocole (1 fichier, 2 module,
 *                        5 classe, 9 constructeur, 12 fonction…),
 *                        zéro si le serveur n'en dit rien
 * @param plage           la plage qui étend le symbole, jamais nulle
 * @param plageSelection la plage que la sélection retient, ou null
 * @param enfants         les symboles enfants, dans l'ordre du serveur
 */
public record SymboleLsp(String nom, int genre, PlageLsp plage,
                         @Nullable PlageLsp plageSelection,
                         List<SymboleLsp> enfants) {

    /** Constructeur canonique : les enfants figés, jamais nuls. */
    public SymboleLsp {
        enfants = List.copyOf(enfants);
    }
}
