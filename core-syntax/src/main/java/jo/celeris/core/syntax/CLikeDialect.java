package jo.celeris.core.syntax;

import java.util.List;
import java.util.Set;

/**
 * Le dialecte d'un langage de la famille C (M7.6, H-10) : tout ce qui
 * distingue Java, Kotlin et Groovy du socle commun vit ici, en données
 * et en quelques règles — les moteurs ({@link CLikeLexer},
 * {@link CLikeParser}) restent uniques, les spécialisations restent
 * courtes (la matrice H-10 exige au plus 150 lignes chacune) et les
 * instantanés ({@code ClikeSnapshotsTest}) prouvent que la
 * factorisation ne change rien à l'analyse.
 *
 * <p>Côté lexeur : mots réservés, opérateurs (du plus long d'abord),
 * ponctuation, et les règles des constructions multi-lignes — nichage
 * des commentaires de bloc, triples guillemets, gabarits
 * {@code ${…}}, soudure des jetons. Côté parseur : déclarations de
 * types, modificateurs, mots de membres, et la construction d'un
 * membre qui n'est pas un type imbriqué.</p>
 */
abstract class CLikeDialect {

    /** Les mots réservés du langage. */
    abstract Set<String> reservedWords();

    /** Les opérateurs, du plus long d'abord — l'ordre fait la priorité. */
    abstract List<String> operators();

    /**
     * Les signes composés de ponctuation, vérifiés avant les
     * opérateurs (le {@code ::} de Java et Groovy) — vide pour Kotlin,
     * dont le {@code ::} est un opérateur.
     */
    List<String> compositePunctuation() {
        return List.of("::");
    }

    /**
     * Les caractères de ponctuation simples — Kotlin y ajoute le
     * deux-points isolé, Java et Groovy le laissent aux opérateurs.
     */
    String simplePunctuation() {
        return ";,.";
    }

    /**
     * Vrai quand les opérateurs se testent AVANT la ponctuation simple
     * (Kotlin : {@code ..} et {@code ::} sont des opérateurs, ils
     * doivent passer avant le point et le deux-points isolés).
     */
    boolean operatorsBeforePunctuation() {
        return false;
    }

    /**
     * Vrai quand les commentaires de bloc se nichent (Kotlin, Groovy) ;
     * Java les garde plats — la première fermeture clôt tout.
     */
    boolean nestedBlockComments() {
        return true;
    }

    /** Vrai quand la chaîne triple s'ouvre aussi à l'apostrophe (Groovy). */
    boolean tripleSingleQuote() {
        return false;
    }

    /**
     * Vrai quand les jetons d'une ligne se soudent (Java) : chaque
     * jeton commence à la fin du précédent, les blancs appartiennent au
     * jeton qui les suit.
     */
    boolean weldTokens() {
        return false;
    }

    /**
     * Vrai quand la ligne vide DANS une chaîne triple émet quand même
     * un jeton vide (Java) — Kotlin et Groovy n'en émettent aucun.
     */
    boolean keepEmptyTripleLineToken() {
        return false;
    }

    /**
     * Vrai quand le caractère clôture la chaîne délimitée.
     *
     * @param c         le caractère courant
     * @param delimiter le délimiteur ouvrant de la chaîne
     */
    abstract boolean closesString(char c, char delimiter);

    /**
     * Vrai quand un gabarit {@code ${…}} s'ouvre à cette position :
     * les guillemets internes aux accolades ne clôturent pas.
     *
     * @param text      la ligne
     * @param at        la position courante
     * @param n         la longueur de la ligne
     * @param delimiter le déliminateur ouvrant de la chaîne
     */
    abstract boolean opensTemplate(CharSequence text, int at, int n,
            char delimiter);

    /** Les mots-clés qui déclarent un type. */
    abstract Set<String> typeDeclarations();

    /** Les modificateurs qu'une tête de déclaration saute. */
    abstract Set<String> modifiers();

    /**
     * Les mots qui déclarent un membre (fonction, propriété) —
     * {@code fun}, {@code val}, {@code var} pour Kotlin, {@code def}
     * pour Groovy, rien pour Java.
     */
    Set<String> memberWords() {
        return Set.of();
    }

    /**
     * Vrai quand un identificateur en tête de ligne ouvre son chunk
     * (Kotlin, Groovy : les scripts vivent d'appels de premier niveau).
     */
    boolean lineHeadChunks() {
        return false;
    }

    /**
     * Vrai quand le nom d'un type est le mot qui suit son mot-clé,
     * quelle que soit sa sorte (Java) ; sinon le mot doit être un
     * identificateur, un type ou une constante (Kotlin, Groovy).
     */
    boolean anyWordAsTypeName() {
        return false;
    }

    /**
     * L'écart de départ de la recherche de l'accolade d'un type, après
     * son mot-clé : Java cherche après le nom (deux), Kotlin et Groovy
     * dès le mot suivant (un).
     */
    int typeBraceOffset() {
        return 1;
    }

    /** Vrai quand la feuille de type absente d'accolade scanne ses erreurs (Java). */
    boolean scanLeafErrors() {
        return false;
    }

    /**
     * Vrai quand le jeton d'indice donné déclare un type — le défaut
     * lit le mot-clé ; Java ajoute l'annotation {@code @interface}.
     *
     * @param slice la tranche de jetons du chunk
     * @param index l'indice du candidat
     */
    boolean isTypeDeclaration(TokenSlice slice, int index) {
        return slice.kind(index) == TokenKind.KEYWORD
                && typeDeclarations().contains(Jetons.mot(slice, index));
    }

    /**
     * La reprise après un type imbriqué : après son accolade fermante,
     * ou la borne donnée quand la fermeture manque.
     *
     * @param slice   la tranche de jetons du chunk
     * @param keyword l'indice du mot-clé du type
     * @param to      la borne du corps contenant
     */
    int advanceAfterType(TokenSlice slice, int keyword, int to) {
        int opening = Jetons.chercherAccolade(slice, keyword + typeBraceOffset());
        int closing = opening < 0 ? to : slice.matchOf(opening);
        return closing < 0 ? to : closing + 1;
    }

    /**
     * Analyse un membre qui n'est pas un type imbriqué — la règle
     * propre à la langue — ajoute son nœud à la liste et rend la
     * position de reprise. Un membre déclaré de premier niveau (le
     * {@code fun} et le {@code val} Kotlin, le {@code def} Groovy) est
     * le DERNIER nœud ajouté : son chunk lui appartient.
     *
     * @param slice   la tranche de jetons du chunk
     * @param start   le début brut du membre, trivia compris
     * @param to      la borne du corps contenant
     * @param members la liste des membres déjà construits
     * @return la position de reprise dans la tranche
     */
    abstract int parseMember(TokenSlice slice, int start, int to,
            List<SynNode> members);

    /**
     * La tête d'une déclaration : annotations (et leurs arguments entre
     * parenthèses) et modificateurs — la reprise est le mot qui décide.
     *
     * @param slice la tranche de jetons du chunk
     * @param from  la position de départ
     * @return l'indice du premier jeton qui n'est pas une tête
     */
    final int skipHeader(TokenSlice slice, int from) {
        int i = from;
        while (i < slice.size()) {
            TokenKind sort = slice.kind(i);
            if (sort == TokenKind.ANNOTATION) {
                i++;
                if (i < slice.size() && "(".equals(Jetons.texteCrochet(slice, i))) {
                    int closing = slice.matchOf(i);
                    if (closing < 0) {
                        return slice.size();
                    }
                    i = closing + 1;
                }
                continue;
            }
            if (sort == TokenKind.KEYWORD
                    && modifiers().contains(Jetons.mot(slice, i))) {
                i++;
                continue;
            }
            return i;
        }
        return i;
    }

    /**
     * Lexe un signe débutant à {@code i} : crochets appariés,
     * ponctuation et opérateurs — l'ordre des familles suit le dialecte
     * (opérateurs d'abord pour Kotlin), le plus long d'abord au sein de
     * chaque famille. Un signe inconnu devient un opérateur d'un
     * caractère : jamais perdu, jamais d'exception.
     *
     * @param text   la ligne à lexer
     * @param tokens les jetons de la ligne, en construction
     * @param i      la position du signe
     * @param n      la longueur de la ligne
     * @return la position après le signe
     */
    final int lexSign(CharSequence text, List<Token> tokens, int i, int n) {
        char c = text.charAt(i);
        if ("(){}[]".indexOf(c) >= 0) {
            TokenKind sort = c == '(' || c == '{' || c == '['
                    ? TokenKind.OPEN_BRACKET : TokenKind.CLOSE_BRACKET;
            tokens.add(new Token(sort, i, i + 1));
            return i + 1;
        }
        if (operatorsBeforePunctuation()) {
            int after = operator(text, tokens, i);
            if (after >= 0) {
                return after;
            }
        } else {
            for (String sign : compositePunctuation()) {
                if (matches(text, i, sign)) {
                    tokens.add(new Token(TokenKind.PUNCTUATION, i, i + sign.length()));
                    return i + sign.length();
                }
            }
        }
        int after = simplePunctuationOrOperator(text, tokens, i);
        if (after >= 0) {
            return after;
        }
        if (!operatorsBeforePunctuation()) {
            after = operator(text, tokens, i);
            if (after >= 0) {
                return after;
            }
        }
        tokens.add(new Token(TokenKind.OPERATOR, i, i + 1));
        return i + 1;
    }

    /** La ponctuation simple d'un caractère, sinon -1 (l'opérateur suit). */
    private int simplePunctuationOrOperator(CharSequence text, List<Token> tokens,
            int i) {
        char c = text.charAt(i);
        if (simplePunctuation().indexOf(c) >= 0) {
            tokens.add(new Token(TokenKind.PUNCTUATION, i, i + 1));
            return i + 1;
        }
        return -1;
    }

    /** Le premier opérateur qui correspond, sinon -1 ; le dernier recours d'un caractère. */
    private int operator(CharSequence text, List<Token> tokens, int i) {
        for (String sign : operators()) {
            if (matches(text, i, sign)) {
                tokens.add(new Token(TokenKind.OPERATOR, i, i + sign.length()));
                return i + sign.length();
            }
        }
        return -1;
    }

    /** Vrai quand le signe s'écrit à cette position. */
    private static boolean matches(CharSequence text, int i, String sign) {
        return text.length() >= i + sign.length()
                && text.subSequence(i, i + sign.length()).toString().equals(sign);
    }
}
