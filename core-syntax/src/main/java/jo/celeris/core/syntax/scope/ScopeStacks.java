package jo.celeris.core.syntax.scope;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jo.celeris.core.syntax.TokenKind;
import org.jspecify.annotations.Nullable;

/**
 * L'internement des piles de portées (M7.6, H-07) : chaque pile — la
 * portée racine du langage, celles des blocs ouverts, celle du jeton —
 * vit UNE fois, désignée par un entier ; le jeton porte l'entier,
 * jamais la chaîne. L'arbre de préfixes partage ses nœuds : deux piles
 * qui convergent partagent leur dessous, la mémoire suit les langages
 * réels, pas les documents.
 *
 * <p>La pile porte aussi sa catégorie dérivée : {@link TokenKind}
 * devient une catégorie grossière tirée de la portée de tête — les
 * consommateurs hérités (crochets, plis) y restent, la COULEUR se
 * résout depuis la pile par le thème ({@code ScopeTheme}), plus
 * jamais depuis la sorte (ADR-004 révisé, M7.6).</p>
 *
 * <p>Table partagée et câblée : la lexicographie court sur les fils
 * de travail (ADR-007), l'internement y est sans verrou au lecture ;
 * la profondeur des piles se borne (au-delà, la pile reste ouverte —
 * les portées profondes se replient sur leur dessous) et la table
 * elle-même se borne (au-delà, les piles nouvelles partagent le
 * dessous existant) : une grammaire hostile ne gonfle pas la mémoire
 * de l'éditeur, elle y perd seulement de la précision.</p>
 */
public final class ScopeStacks {

    /** L'absence de pile : le jeton des analyseurs écrits, la palette par sorte. */
    public static final int NONE = 0;

    /** La profondeur maximale d'une pile internée. */
    private static final int MAX_DEPTH = 64;

    /** Le nombre maximal de nœuds internés — le garde-fou mémoire. */
    private static final int MAX_NODES = 50_000;

    private static final Map<Integer, Node> NODES = new ConcurrentHashMap<>();

    /** Le prochain identifiant — sous verrou, jamais d'atomiques dans
     *  le cœur (ADR-007, règle d'architecture). */
    private static int suivant = 1;

    static {
        NODES.put(0, new Node(0, "", 0, 0));
    }

    private ScopeStacks() {
    }

    /**
     * La pile racine d'un langage : une seule portée, celle de la
     * grammaire ({@code source.toml}, {@code text.html.basic}…).
     *
     * @param scopeName la portée racine du langage
     * @return l'identifiant de la pile racine
     */
    public static int root(String scopeName) {
        return push(NONE, scopeName);
    }

    /**
     * Empile une portée sur une pile : la pile fille vit une fois,
     * partagée par tout jeton qui la porte.
     *
     * @param parent    l'identifiant de la pile dessous, ou NONE
     * @param scopeName la portée empilée
     * @return l'identifiant de la pile résultante — le parent si la
     *         profondeur ou la table atteint ses bornes
     */
    public static int push(int parent, @Nullable String scopeName) {
        if (scopeName == null || scopeName.isEmpty()) {
            return parent;
        }
        Node dessous = NODES.get(parent);
        if (dessous == null) {
            return parent;
        }
        if (dessous.depth >= MAX_DEPTH || NODES.size() >= MAX_NODES) {
            return parent;
        }
        Node enfant = dessous.children.computeIfAbsent(scopeName,
                nom -> {
                    Node noeud = new Node(prochain(), nom,
                            dessous.id, dessous.depth + 1);
                    NODES.put(noeud.id, noeud);
                    return noeud;
                });
        return enfant.id;
    }

    /** L'identifiant suivant, sous la garde du verrou de la classe. */
    private static synchronized int prochain() {
        return ++suivant;
    }

    /**
     * Les portées d'une pile, de la racine au sommet — la matière que
     * les sélecteurs du thème parcourent.
     *
     * @param stack l'identifiant de la pile
     * @return les portées de la pile, vide si l'identifiant ne vit plus
     */
    public static List<String> scopesOf(int stack) {
        Node noeud = NODES.get(stack);
        if (noeud == null) {
            return List.of();
        }
        List<String> portees = new ArrayList<>(noeud.depth);
        Node marche = noeud;
        while (marche.id != NONE) {
            portees.add(marche.name);
            marche = NODES.get(marche.parent);
            if (marche == null) {
                break;
            }
        }
        List<String> racineAuSommet = new ArrayList<>(portees.size());
        for (int i = portees.size() - 1; i >= 0; i--) {
            racineAuSommet.add(portees.get(i));
        }
        return racineAuSommet;
    }

    /**
     * La catégorie grossière d'une pile (M7.6) : la sorte du jeton
     * tirée de sa portée de SOMMET — les consommateurs hérités
     * (crochets, plis) la gardent, la couleur ne passe plus par elle.
     *
     * @param stack l'identifiant de la pile
     * @return la sorte dérivée, IDENTIFIER par défaut
     */
    public static TokenKind kindOf(int stack) {
        Node noeud = NODES.get(stack);
        return noeud == null ? TokenKind.IDENTIFIER : derive(noeud.name);
    }

    /**
     * La sorte tirée d'une portée : la tête commande, les queues
     * connues distinguent (documentation, nombre, opérateur, types,
     * crochets appariés).
     */
    private static TokenKind derive(String portee) {
        int point = portee.indexOf('.');
        String tete = point < 0 ? portee : portee.substring(0, point);
        return switch (tete) {
            case "comment", "constant", "keyword", "meta", "invalid" ->
                    affine(tete, portee);
            case "string" -> TokenKind.STRING;
            case "storage" -> TokenKind.KEYWORD;
            case "entity" -> entite(portee);
            case "support" -> support(portee);
            case "punctuation" -> ponctuation(portee);
            case "markup" -> markup(portee);
            default -> TokenKind.IDENTIFIER;
        };
    }

    /** Les têtes dont la queue distingue : la queue fine d'abord. */
    private static TokenKind affine(String tete, String portee) {
        return switch (tete) {
            case "comment" -> portee.startsWith("comment.block.documentation")
                    ? TokenKind.DOC : TokenKind.COMMENT;
            case "constant" -> portee.startsWith("constant.numeric")
                    ? TokenKind.NUMBER : TokenKind.CONSTANT;
            case "keyword" -> portee.startsWith("keyword.operator")
                    ? TokenKind.OPERATOR : TokenKind.KEYWORD;
            case "meta" -> portee.startsWith("meta.annotation")
                    ? TokenKind.ANNOTATION : TokenKind.IDENTIFIER;
            default -> portee.startsWith("invalid.illegal")
                    ? TokenKind.ERROR : TokenKind.IDENTIFIER;
        };
    }

    private static TokenKind entite(String portee) {
        if (portee.startsWith("entity.name.type")
                || portee.startsWith("entity.name.section")
                || portee.startsWith("entity.name.tag")) {
            return TokenKind.TYPE;
        }
        return TokenKind.IDENTIFIER;
    }

    private static TokenKind support(String portee) {
        if (portee.startsWith("support.type")) {
            return TokenKind.TYPE;
        }
        return portee.startsWith("support.constant")
                ? TokenKind.CONSTANT : TokenKind.IDENTIFIER;
    }

    private static TokenKind ponctuation(String portee) {
        if (portee.startsWith("punctuation.section.")) {
            if (aSegment(portee, "begin")) {
                return TokenKind.OPEN_BRACKET;
            }
            if (aSegment(portee, "end")) {
                return TokenKind.CLOSE_BRACKET;
            }
        }
        return TokenKind.PUNCTUATION;
    }

    private static TokenKind markup(String portee) {
        if (portee.startsWith("markup.raw")
                || portee.startsWith("markup.underline")) {
            return TokenKind.STRING;
        }
        if (portee.startsWith("markup.quote")) {
            return TokenKind.COMMENT;
        }
        if (portee.startsWith("markup.heading")) {
            return TokenKind.TYPE;
        }
        return TokenKind.CONSTANT;
    }

    /** Vrai quand la portée compte un segment exactement égal au mot donné. */
    private static boolean aSegment(String portee, String mot) {
        int depuis = 0;
        int point = portee.indexOf('.');
        while (point >= 0) {
            if (portee.substring(depuis, point).equals(mot)) {
                return true;
            }
            depuis = point + 1;
            point = portee.indexOf('.', depuis);
        }
        return portee.substring(depuis).equals(mot);
    }

    /** Un nœud de l'arbre de préfixes : identifiant, portée, dessous. */
    private static final class Node {

        private final int id;

        private final String name;

        private final int parent;

        private final int depth;

        private final Map<String, Node> children = new ConcurrentHashMap<>();

        private Node(int id, String name, int parent, int depth) {
            this.id = id;
            this.name = name;
            this.parent = parent;
            this.depth = depth;
        }
    }
}
