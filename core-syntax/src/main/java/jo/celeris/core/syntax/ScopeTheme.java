package jo.celeris.core.syntax;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

/**
 * Le thème par portées (M7.6, H-07) : des règles « sélecteur → style »
 * résolues contre la PILE de portées du jeton, jamais contre sa
 * sorte. La résolution fusionne les règles qui matchent — l'encre
 * vient de la meilleure qui en fixe une, les attributs se cumulent —
 * et le résultat se GARDE EN CACHE par identifiant de pile : le tracé
 * ne manipule aucune chaîne et n'alloue rien par image (PIEGE-15).
 *
 * <p>Sans règle qui matche, la consultation rend null : la couleur du
 * consommateur fait foi (la palette par sorte du thème d'éditeur).</p>
 */
public final class ScopeTheme {

    /** Le thème vide : aucune règle, tout retombe à la palette. */
    private static final ScopeTheme VIDE = new ScopeTheme(List.of());

    /** Le marqueur de cache « aucune règle ne matche ». */
    private static final Optional<ScopeStyle> AUCUNE = Optional.empty();

    private final List<Rule> rules;

    private final Map<Integer, Optional<ScopeStyle>> cache =
            new ConcurrentHashMap<>();

    private ScopeTheme(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    /**
     * Le thème vide — aucune règle, tout retombe à la palette par sorte.
     *
     * @return le thème vide
     */
    public static ScopeTheme empty() {
        return VIDE;
    }

    /**
     * Bâtit un thème de ses règles : chaque sélecteur se lit ENTIER à
     * la construction — un sélecteur mal formé refuse le thème, avec
     * la position de la faute.
     *
     * @param rules les règles « sélecteur → style », le score le plus
     *              haut emporte l'encre
     * @return le thème bâti
     */
    public static ScopeTheme of(List<Rule> rules) {
        return new ScopeTheme(rules);
    }

    /**
     * Le style d'une pile de portées : les règles qui matchent se
     * fusionnent — l'encre de la meilleure, les attributs cumulés —,
     * le résultat se cache par identifiant de pile. Rend null quand
     * aucune règle ne matche.
     *
     * @param stack l'identifiant de la pile du jeton
     * @return le style résolu, ou null sans règle qui matche
     */
    public @Nullable ScopeStyle styleFor(int stack) {
        Optional<ScopeStyle> enCache = cache.get(stack);
        if (enCache == null) {
            ScopeStyle resolu = resoudre(ScopeStacks.scopesOf(stack));
            enCache = resolu == null ? AUCUNE : Optional.of(resolu);
            cache.put(stack, enCache);
        }
        return enCache.orElse(null);
    }

    /** Les règles du thème, dans leur ordre de déclaration. */
    public List<Rule> rules() {
        return rules;
    }

    /**
     * La fusion des règles qui matchent la pile donnée : l'encre de la
     * meilleure qui en fixe une (score le plus haut, la première
     * déclarée en dernier recours), les attributs cumulés de toutes.
     */
    private @Nullable ScopeStyle resoudre(List<String> scopes) {
        int meilleureEncre = -1;
        int encre = 0;
        boolean gras = false;
        boolean italique = false;
        boolean souligne = false;
        boolean quelqueChose = false;
        for (Rule regle : rules) {
            Integer score = regle.parsed().score(scopes);
            if (score == null) {
                continue;
            }
            quelqueChose = true;
            ScopeStyle style = regle.style();
            gras |= style.bold();
            italique |= style.italic();
            souligne |= style.underline();
            if (style.hasInk() && score > meilleureEncre) {
                meilleureEncre = score;
                encre = style.foreground();
            }
        }
        if (!quelqueChose) {
            return null;
        }
        return new ScopeStyle(encre, gras, italique, souligne);
    }

    /**
     * Une règle du thème : le sélecteur de portées et le style qu'il
     * pose. Le sélecteur se lit à la construction — mal formé, la
     * règle se refuse avec la position de sa faute.
     */
    public static final class Rule {

        private final ScopeSelector selector;

        private final ScopeStyle style;

        /**
         * La règle de son texte de sélecteur et de son style.
         *
         * @param selector le sélecteur TextMate, virgules comprises
         * @param style    le style posé aux jetons qui matchent
         */
        public Rule(String selector, ScopeStyle style) {
            this.selector = ScopeSelector.parse(selector);
            this.style = Objects.requireNonNull(style, "style");
        }

        /** Le texte du sélecteur, tel que déclaré. */
        public String selector() {
            return selector.toString();
        }

        /** Le sélecteur parsé — la résolution s'en sert. */
        ScopeSelector parsed() {
            return selector;
        }

        /** Le style posé. */
        public ScopeStyle style() {
            return style;
        }
    }
}
