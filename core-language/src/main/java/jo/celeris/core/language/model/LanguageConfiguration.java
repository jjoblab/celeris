package jo.celeris.core.language.model;

import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * La configuration d'un langage (M7.6) : commentaires, crochets,
 * fermeture automatique, entourage, motif de mot, plis et règles
 * d'indentation — un sous-ensemble compatible du
 * {@code language-configuration.json} de VS Code, plus les extensions
 * celeris ({@code indentSize}, {@code tabIndent}) documentées ici.
 * Les moteurs ({@code Commenter}, {@code SmartTyping}) en sont de
 * simples consommateurs : plus aucun comportement ne se décide par
 * identifiant de langage.
 *
 * <p>Les sections absentes valent leur vide ({@link Comments} sans
 * délimiteur, listes vides, plis par crochets) : un langage de données
 * sans commentaire est une configuration légitime, pas une erreur.
 *
 * @param comments          les délimiteurs de commentaire
 * @param brackets          les paires de crochets appariés
 * @param autoClosingPairs  les paires qui se ferment seules, avec leurs
 *                          contextes d'exclusion de VS Code — une paire
 *                          ouvrante égale à sa fermante est une citation
 *                          qui se dédouble
 * @param surroundingPairs  les paires d'entourage de la sélection
 * @param wordPattern       le motif de regex d'un mot, ou null pour le
 *                          mot par défaut du moteur
 * @param folding           les règles de pli (par crochets, par
 *                          indentation, marqueurs)
 * @param indentRules       les règles d'indentation de VS Code, ou
 *                          leurs vides
 * @param indentSize        la taille d'indentation par défaut
 * @param tabIndent         vrai pour indenter à la tabulation
 */
public record LanguageConfiguration(
        Comments comments,
        List<BracketPair> brackets,
        List<AutoClosePair> autoClosingPairs,
        List<BracketPair> surroundingPairs,
        @Nullable String wordPattern,
        Folding folding,
        IndentRules indentRules,
        int indentSize,
        boolean tabIndent) {

    /** Les délimiteurs de commentaire : ligne, bloc ou rien. */
    public record Comments(@Nullable String lineComment,
            @Nullable String blockStart, @Nullable String blockEnd) {

        /** Vrai si le langage se commente : au moins une forme existe. */
        public boolean commentable() {
            return lineComment != null || blockStart != null;
        }
    }

    /** Une paire de crochets appariés. */
    public record BracketPair(char open, char close) {
    }

    /**
     * Une paire à fermeture automatique : l'ouvrant inséré, le fermant
     * suit, sauf dans les contextes exclus (dans une chaîne, dans un
     * commentaire — les valeurs de VS Code).
     *
     * @param open  l'ouvrant
     * @param close le fermant
     * @param notIn les contextes d'exclusion
     */
    public record AutoClosePair(char open, char close, Set<String> notIn) {

        /** Constructeur canonique : contexte figé, jamais nul. */
        public AutoClosePair {
            notIn = Set.copyOf(notIn == null ? Set.of() : notIn);
        }
    }

    /**
     * Les règles de pli : par crochets (le défaut), par indentation
     * pour les langages à décalage (Python, YAML), avec marqueurs
     * optionnels.
     *
     * @param byIndentation vrai pour plier à l'indentation
     * @param offSide       vrai quand une ligne moins indentée que sa
     *                      marque rouvre le pli (le réglage Python de
     *                      la référence)
     * @param beginMarker   le marqueur d'ouverture, ou null
     * @param endMarker     le marqueur de fermeture, ou null
     */
    public record Folding(boolean byIndentation, boolean offSide,
            @Nullable String beginMarker, @Nullable String endMarker) {
    }

    /**
     * Les règles d'indentation de VS Code : motifs de regex sur la
     * ligne courante et la suivante.
     *
     * @param increase        la ligne dont le contenu augmente l'indentation
     * @param decrease        la ligne dont le contenu la diminue
     * @param indentNextLine  la ligne qui n'indente que la suivante
     * @param unindentedLine  la ligne qui ne s'indente jamais
     */
    public record IndentRules(@Nullable String increase, @Nullable String decrease,
            @Nullable String indentNextLine, @Nullable String unindentedLine) {
    }

    /** La configuration vide : rien de déclaré, tout au moteur par défaut. */
    public static final LanguageConfiguration EMPTY =
            new LanguageConfiguration(new Comments(null, null, null), List.of(),
                    List.of(), List.of(), null, new Folding(false, false, null, null),
                    new IndentRules(null, null, null, null), 4, false);

    /** Constructeur canonique : sections absentes ramenées à leur vide. */
    public LanguageConfiguration {
        comments = comments == null ? new Comments(null, null, null) : comments;
        brackets = List.copyOf(brackets == null ? List.of() : brackets);
        autoClosingPairs =
                List.copyOf(autoClosingPairs == null ? List.of() : autoClosingPairs);
        surroundingPairs =
                List.copyOf(surroundingPairs == null ? List.of() : surroundingPairs);
        folding = folding == null ? new Folding(false, false, null, null) : folding;
        indentRules = indentRules == null
                ? new IndentRules(null, null, null, null)
                : indentRules;
        if (indentSize <= 0) {
            throw new IllegalArgumentException("indentSize must be positive: " + indentSize);
        }
    }

    /**
     * La configuration générique des langages inconnus : les paires
     * communes, les citations de la famille C, quatre espaces — les
     * règles que la frappe suivait avant la plateforme, en données.
     *
     * @return la configuration générique
     */
    public static LanguageConfiguration generic() {
        return new LanguageConfiguration(
                new Comments(null, null, null),
                List.of(new BracketPair('(', ')'), new BracketPair('[', ']'),
                        new BracketPair('{', '}')),
                List.of(new AutoClosePair('(', ')', Set.of()),
                        new AutoClosePair('[', ']', Set.of()),
                        new AutoClosePair('{', '}', Set.of()),
                        new AutoClosePair('"', '"', Set.of("string")),
                        new AutoClosePair('\'', '\'', Set.of("string", "comment"))),
                List.of(new BracketPair('(', ')'), new BracketPair('[', ']'),
                        new BracketPair('{', '}'), new BracketPair('"', '"'),
                        new BracketPair('\'', '\'')),
                null,
                new Folding(false, false, null, null),
                new IndentRules(null, null, null, null),
                4,
                false);
    }

    /**
     * Les paires à fermeture automatique effectives : les déclarées,
     * ou celles dérivées des crochets quand aucune n'est déclarée (la
     * convention de VS Code).
     *
     * @return les paires effectives, jamais vides de sens quand les
     *         crochets existent
     */
    public List<AutoClosePair> effectiveAutoClosingPairs() {
        if (!autoClosingPairs.isEmpty()) {
            return autoClosingPairs;
        }
        return brackets.stream()
                .map(pair -> new AutoClosePair(pair.open(), pair.close(), Set.of()))
                .toList();
    }
}
