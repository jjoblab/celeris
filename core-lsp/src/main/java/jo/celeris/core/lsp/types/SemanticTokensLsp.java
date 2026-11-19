package jo.celeris.core.lsp.types;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Les jetons sémantiques du protocole (B-02) : le delta encodé du
 * serveur — chaque jeton relatif au précédent — décodé en positions
 * absolues. Le décodage est une fonction pure de la charge « data » :
 * aucun autre flux n'y entre (PIEGE-18 — les diagnostics et les jetons
 * sont deux sources indépendantes, jamais chaînées). La bibliothèque
 * rend la liste d'entiers telle quelle derrière la frontière
 * protocolaire (ADR-015) ; le décodage relatif-à-absolu reste au
 * domaine. Les jetons rejoindront la session à la couverture nouvelle
 * (H-12).
 */
public final class SemanticTokensLsp {

    private final @Nullable String identifiantResultat;
    private final List<SemanticTokenLsp> tokens;

    private SemanticTokensLsp(@Nullable String identifiantResultat,
                              List<SemanticTokenLsp> tokens) {
        this.identifiantResultat = identifiantResultat;
        this.tokens = List.copyOf(tokens);
    }

    /**
     * Décode les jetons de leur charge encodée — la voie de la
     * frontière protocolaire (ADR-015) : la bibliothèque rend la liste
     * d'entiers telle quelle, le décodage relatif-à-absolu reste au
     * domaine.
     *
     * @param identifiant l'identifiant de résultat du serveur, ou null
     * @param donnees      la charge « data » du protocole, quintuples
     *                    relatifs
     * @return les jetons en positions absolues, vide sans données
     * @throws IllegalArgumentException si les données ne sont pas une
     *         suite de quintuples
     */
    public static SemanticTokensLsp of(@Nullable String identifiant,
            List<Integer> donnees) {
        if (donnees.size() % 5 != 0) {
            throw new IllegalArgumentException(
                    "semantic token data must be a list of quintuples");
        }
        List<SemanticTokenLsp> tokens = new ArrayList<>();
        int line = 0;
        int column = 0;
        for (int i = 0; i < donnees.size(); i += 5) {
            line += donnees.get(i);
            column = donnees.get(i) != 0 ? donnees.get(i + 1)
                    : column + donnees.get(i + 1);
            tokens.add(new SemanticTokenLsp(line, column, donnees.get(i + 2),
                    donnees.get(i + 3), donnees.get(i + 4)));
        }
        return new SemanticTokensLsp(identifiant, tokens);
    }

    /** L'identifiant de résultat du serveur, pour un décodage incrémental — ou null. */
    public @Nullable String identifiantResultat() {
        return identifiantResultat;
    }

    /** Les jetons en positions absolues, dans l'ordre du flux. */
    public List<SemanticTokenLsp> tokens() {
        return tokens;
    }
}
