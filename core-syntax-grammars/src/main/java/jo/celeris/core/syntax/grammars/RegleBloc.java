package jo.celeris.core.syntax.grammars;

import java.util.List;
import java.util.Map;

import org.joni.Regex;
import org.jspecify.annotations.Nullable;

/**
 * Bloc début/fin d'une grammaire TextMate : l'ouverture et la
 * fermeture peuvent vivre sur des lignes différentes — l'état
 * transporté du lexeur porte le bloc ouvert d'une ligne à la suivante,
 * la fermeture se cherche au fil du contenu. La fermeture peut porter
 * des références aux captures de l'ouverture ({@code \1}) — les
 * grammaires réelles s'en servent pour les clôtures d'ici-documents et
 * les clôtures de blocs de code Markdown : la source est conservée, la
 * fermeture effective se substitue à l'ouverture de chaque bloc.
 *
 * @param debut        l'expression d'ouverture compilée
 * @param sourceFin    la source de l'expression de fermeture
 * @param fin          l'expression de fermeture compilée (sans substitution)
 * @param referencesFin vrai si la fermeture référence des captures de l'ouverture
 * @param portee       la portée de la construction, null si absente
 * @param contenu      la portée de contenu (contentName), null si absente
 * @param capturesDebut les captures de l'ouverture par numéro de groupe
 * @param capturesFin  les captures de la fermeture par numéro de groupe
 * @param motifs       les règles applicables au contenu du bloc
 */
record RegleBloc(Regex debut, String sourceFin, Regex fin, boolean referencesFin,
        @Nullable String portee, @Nullable String contenu,
        @Nullable Map<Integer, Capture> capturesDebut,
        @Nullable Map<Integer, Capture> capturesFin,
        List<Regle> motifs) implements Regle {

    /** Constructeur canonique : les motifs de contenu sont figés, jamais nuls. */
    RegleBloc {
        motifs = List.copyOf(motifs);
    }
}
