package jo.celeris.core.language;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jo.celeris.core.syntax.LanguageSyntax;

import org.jspecify.annotations.Nullable;

/**
 * L'enregistrement des sources syntaxiques tree-sitter (M7.6) : la CASE
 * DÉCLARÉE du niveau 2 de l'analyse structurelle — la demande du
 * propriétaire du 2026-11-08 (« en plus de TextMate je souhaite aussi
 * ajouter l'option tree-sitter »). Un paquet peut déclarer
 * {@code "syntax": {"preferred": "tree-sitter", "fallback": "grammar"}}
 * ; la plateforme consulte cet enregistrement à l'assemblage, et
 * l'arbre commun ne sait pas qui a répondu.
 *
 * <p>L'enregistrement est VIDE à M7.6 — l'arrière natif (runtime C,
 * liaison JNI, grammaires par ABI) ne rentre pas au dépôt : l'ADR-016
 * l'écarte sur mesures (incrémental 9,6 à 44,5 ms de p50 à l'échelle du
 * mandat, arbres 26 à 31 fois le texte). Un hôte qui embarquerait un
 * jour l'arrière enregistre ici ses sources : la préférence
 * {@code tree-sitter} se met à servir, le repli déclaré cesse de
 * servir — aucun consommateur ne change. La révision de l'arrière
 * passe par ADR, mesures sur l'appareil de référence à l'appui.</p>
 */
public final class TreeSitterLanguages {

    private static final Map<String, LanguageSyntax> SYNTAXES = new ConcurrentHashMap<>();

    private TreeSitterLanguages() {
    }

    /**
     * Enregistre une source syntaxique tree-sitter pour un langage —
     * l'hôte qui embarque l'arrière natif l'appelle à son démarrage.
     *
     * @param id     l'identifiant du langage
     * @param syntax la source syntaxique tree-sitter
     */
    public static void enregistrer(String id, LanguageSyntax syntax) {
        SYNTAXES.put(id, syntax);
    }

    /**
     * Oublie l'enregistrement d'un langage — le banc de la case vide
     * s'en sert pour rendre l'enregistrement à son état M7.6.
     *
     * @param id l'identifiant du langage
     */
    public static void oublier(String id) {
        SYNTAXES.remove(id);
    }

    /**
     * La source syntaxique tree-sitter d'un langage.
     *
     * @param id l'identifiant du langage
     * @return sa source, ou null — le cas vide à M7.6
     */
    public static @Nullable LanguageSyntax syntaxOf(String id) {
        return SYNTAXES.get(id);
    }

    /** Les identifiants des langages à source tree-sitter enregistrée. */
    public static Set<String> ids() {
        return Set.copyOf(SYNTAXES.keySet());
    }
}
