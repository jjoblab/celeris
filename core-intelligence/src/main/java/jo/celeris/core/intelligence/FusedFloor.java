package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Le plancher local fusionné (C6) : brancher un fournisseur ne retire
 * pas les mots-clés ni les gabarits — les candidats du fournisseur
 * (symboles résolus) et le plancher local (mots-clés du langage,
 * gabarits d'expansion, mots du tampon) se fondent en une seule liste,
 * filtrée par l'apparieur unifié, ordonnée par palier puis groupe, et
 * l'ordre du fournisseur reste la référence à l'intérieur d'un couple
 * (palier, groupe). Le doublon se dédie par identifiant de tête : le
 * symbole résolu du fournisseur précède le mot du plancher.
 *
 * <p>L'algorithme descend du plancher fusionné du dépôt de départ
 * (registre de portage, C6 — Spécifier : l'ancien sert d'oracle),
 * réduit au classement par labels.</p>
 */
public final class FusedFloor {

    private FusedFloor() {
    }

    /**
     * Le plancher local : les mots-clés du langage, les gabarits
     * d'expansion, les mots du tampon.
     *
     * @param keywords les mots-clés du langage
     * @param snippets les gabarits d'expansion
     * @param words    les mots du tampon
     */
    public record Floor(List<String> keywords, List<String> snippets,
            List<String> words) {

        /** Constructeur canonique : listes figées. */
        public Floor {
            Objects.requireNonNull(keywords, "keywords");
            Objects.requireNonNull(snippets, "snippets");
            Objects.requireNonNull(words, "words");
            keywords = List.copyOf(keywords);
            snippets = List.copyOf(snippets);
            words = List.copyOf(words);
        }

        /** Le plancher vide. */
        public static Floor vide() {
            return new Floor(List.of(), List.of(), List.of());
        }
    }

    /** Un candidat fusionné : son label, son palier, son groupe, son rang d'origine. */
    private record Entree(String label, int tier, int group, int rang) {
    }

    /**
     * La liste fusionnée : les candidats du fournisseur et le plancher
     * local, filtrés par le préfixe, ordonnés par palier puis groupe —
     * le fournisseur ne retire jamais le plancher, le plancher ne
     * détrône jamais un palier meilleur, l'ordre d'origine reste la
     * référence à palier et groupe égaux.
     *
     * @param provider les candidats du fournisseur, dans son ordre
     * @param floor    le plancher local
     * @param prefix   le préfixe tapé (vide = tout correspond)
     * @return les labels fusionnés, ordonnés et dédupliqués
     */
    public static List<String> merged(List<String> provider, Floor floor,
            String prefix) {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(floor, "floor");
        Objects.requireNonNull(prefix, "prefix");
        List<Entree> entrees = new ArrayList<>();
        Set<String> vus = new HashSet<>();
        collect(entrees, vus, provider, CompletionMatcher.GROUP_RESOLVED, prefix);
        collect(entrees, vus, floor.keywords(), CompletionMatcher.GROUP_KEYWORD, prefix);
        collect(entrees, vus, floor.snippets(), CompletionMatcher.GROUP_SNIPPET, prefix);
        collect(entrees, vus, floor.words(), CompletionMatcher.GROUP_BUFFER_WORD, prefix);
        entrees.sort((a, b) -> {
            int cmp = Integer.compare(a.tier(), b.tier());
            if (cmp != 0) {
                return cmp;
            }
            cmp = Integer.compare(a.group(), b.group());
            return cmp != 0 ? cmp : Integer.compare(a.rang(), b.rang());
        });
        List<String> labels = new ArrayList<>();
        for (Entree entree : entrees) {
            labels.add(entree.label());
        }
        return List.copyOf(labels);
    }

    /** Les candidats correspondants d'une liste, leur rang d'origine croissant. */
    private static void collect(List<Entree> entrees, Set<String> vus,
            List<String> labels, int group, String prefix) {
        for (String label : labels) {
            if (!CompletionMatcher.matches(label, "", prefix)) {
                continue;
            }
            String ident = CompletionMatcher.leadingIdentifier(label);
            if (!vus.add(ident)) {
                continue;
            }
            entrees.add(new Entree(label, CompletionMatcher.tier(label, "", prefix),
                    group, entrees.size()));
        }
    }
}
