package jo.celeris.core.syntax.treesitter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jo.celeris.core.language.packs.TreeSitterLanguages;

/**
 * Le registre des grammaires de la voie (ADR-0033 — la généralisation
 * demandée par le propriétaire le 2026-11-23 : « comme l'éditeur de
 * référence, l'utilisateur choisit sa voie et ses langues par
 * dépendance »). L'hôte embarque les artefacts de grammaires qu'il
 * veut ({@code com.itsaky.androidide.treesitter:tree-sitter-&lt;langue&gt;}
 * — chaque langue ne pèse QUE si on l'embarque), y rend la classe de
 * langue par un {@link TSLanguageSupplier} et l'enregistre ici avec
 * SA requête {@code highlights.scm}.
 *
 * <p>Le registre est l'unique chemin de la voie vers la plateforme :
 * chaque grammaire enregistrée y pose sa source syntaxique
 * ({@link TreeSitterLanguages#enregistrer}) au moment de
 * l'enregistrement — la préférence « tree-sitter » du paquet du
 * langage se met à servir, le repli déclaré cesse de servir. Sans
 * enregistrement, le registre reste vide : la voie des grammaires
 * TextMate sert, personne ne change (l'orthogonalité des deux voies
 * — le module absent ou le registre vide ne coûtent RIEN).</p>
 *
 * <p>L'enregistrement est VOLONTAIRE et EXPLICITE : le module ne
 * registre plus rien tout seul (la fabrique fabrique, c'est tout) —
 * le choix de l'hôte fait foi, comme le modèle de l'éditeur de
 * référence où l'utilisateur déclare sa dépendance textmate OU
 * treesitter pour son application.</p>
 */
public final class TreeSitterGrammars {

    private static final Map<String, GrammaireNatif> GRAMMAIRES =
            new ConcurrentHashMap<>();

    private TreeSitterGrammars() {
    }

    /**
     * Enregistre la grammaire d'un langage — la langue de la liaison
     * et SA requête de surbrillance. La source syntaxique de la voie
     * se pose au registre de la plateforme dans la foulée : la
     * préférence « tree-sitter » du paquet du langage se met à
     * servir. Le chargement de la langue reste paresseux (première
     * vue).
     *
     * @param id      l'identifiant du langage (celui du paquet : "json")
     * @param langue le fournisseur de la langue de la liaison
     * @param requete le texte highlights.scm du langage
     */
    public static void enregistrer(String id, TSLanguageSupplier langue,
            String requete) {
        GrammaireNatif grammaire = new GrammaireNatif(id, langue, requete);
        GRAMMAIRES.put(id, grammaire);
        TreeSitterLanguages.enregistrer(id, new SyntaxeNatif(grammaire));
    }

    /**
     * Oublie la grammaire d'un langage — le symétrique de
     * l'enregistrement : la source se retire du registre de la
     * plateforme, le repli déclaré du paquet reprend.
     *
     * @param id l'identifiant du langage
     */
    public static void oublier(String id) {
        GRAMMAIRES.remove(id);
        TreeSitterLanguages.forget(id);
    }

    /**
     * La grammaire enregistrée d'un langage.
     *
     * @param id l'identifiant du langage
     * @return sa grammaire, ou null — la langue n'est pas enregistrée
     */
    public static GrammaireNatif grammaireDe(String id) {
        return GRAMMAIRES.get(id);
    }

    /** Les identifiants des langages enregistrés. */
    public static Set<String> ids() {
        return Set.copyOf(GRAMMAIRES.keySet());
    }
}
