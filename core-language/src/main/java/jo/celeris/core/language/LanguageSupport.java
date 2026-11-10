package jo.celeris.core.language;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import jo.celeris.core.syntax.LanguageSyntax;
import jo.celeris.core.syntax.OutlineRule;

import org.jspecify.annotations.Nullable;

/**
 * L'agrégat immuable d'un langage (M7.6) : descripteur, configuration,
 * source syntaxique, capacités et incorporations. C'est le seul objet
 * que la vue, l'intelligence, la démo et le cli reçoivent — personne
 * en aval ne sait quel langage a répondu.
 *
 * <p>La résolution d'une capacité remonte la chaîne : le fournisseur
 * du langage, sinon celui de sa famille (langage parent), sinon le
 * fournisseur générique du registre (la complétion par les mots du
 * code). Un langage sans source syntaxique reste un langage légitime
 * (coloration seule ou texte intelligent sans analyse).
 */
public final class LanguageSupport {

    private final LanguageDescriptor descriptor;

    private final LanguageConfiguration configuration;

    private final @Nullable LanguageSyntax syntax;

    private final List<OutlineRule> outlineRules;

    private final List<LanguageServerDeclaration> servers;

    private final Map<Class<? extends LanguageCapability>, LanguageCapability> capabilities;

    private final List<LanguageInjection> injections;

    private final @Nullable LanguageSupport parent;

    private final Map<Class<? extends LanguageCapability>, LanguageCapability> genericFallbacks;

    private LanguageSupport(Builder builder) {
        this.descriptor = builder.descriptor;
        this.configuration = builder.configuration;
        this.syntax = builder.syntax;
        this.outlineRules = List.copyOf(builder.outlineRules);
        this.servers = List.copyOf(builder.servers);
        this.capabilities = Map.copyOf(builder.capabilities);
        this.injections = List.copyOf(builder.injections);
        this.parent = builder.parent;
        this.genericFallbacks = Map.copyOf(builder.genericFallbacks);
    }

    /**
     * Le bâtisseur d'un support : descripteur obligatoire, le reste à
     * ses vides.
     *
     * @param descriptor le descripteur du langage
     * @return le bâtisseur prêt à compléter
     */
    public static Builder builder(LanguageDescriptor descriptor) {
        return new Builder(descriptor);
    }

    /**
     * La passerelle de migration : un support minimal autour d'une
     * source syntaxique seule — descripteur synthétique (identifiant,
     * nom, badge), configuration générique. Pour les bancs de test et
     * les hôtes qui n'ont pas encore de paquet ; les paquets intégrés
     * passent par le bâtisseur complet.
     *
     * @param syntax la source syntaxique du langage
     * @return son support minimal
     */
    public static LanguageSupport of(LanguageSyntax syntax) {
        String id = syntax.id();
        String displayName = Character.toUpperCase(id.charAt(0))
                + id.substring(1);
        return builder(new LanguageDescriptor(id, displayName, List.of(), List.of(),
                List.of(), List.of(), null, List.of(), null, null,
                id.substring(0, Math.min(2, id.length())).toUpperCase(Locale.ROOT),
                0xFF9AA0A6))
                .configuration(LanguageConfiguration.generic())
                .syntax(syntax)
                .build();
    }

    /** Le descripteur du langage. */
    public LanguageDescriptor descriptor() {
        return descriptor;
    }

    /** L'identifiant du langage — le raccourci du descripteur. */
    public String id() {
        return descriptor.id();
    }

    /** Le nom affichable du langage. */
    public String displayName() {
        return descriptor.displayName();
    }

    /** La configuration du langage. */
    public LanguageConfiguration configuration() {
        return configuration;
    }

    /** La source syntaxique du langage, ou null (coloration seule). */
    public @Nullable LanguageSyntax syntax() {
        return syntax;
    }

    /**
     * Les règles du plan par données (M7.6, H-08) : le paquet les
     * porte pour les langages sans analyseur dédié — vides, le plan
     * vient de l'arbre de l'analyse.
     *
     * @return les règles du plan, éventuellement vides
     */
    public List<OutlineRule> outlineRules() {
        return outlineRules;
    }

    /**
     * Les serveurs de langage déclarés (M7.6, H-13) : de la donnée —
     * celle du paquet, celle de l'hôte qui assemble — jamais du code.
     * L'éditeur ne connaît aucun serveur : il porte la déclaration,
     * l'hôte décide de la disponibilité et du montage ; sans serveur
     * disponible, les fournisseurs locaux servent (ADR-005).
     *
     * @return les déclarations, éventuellement vides
     */
    public List<LanguageServerDeclaration> serverDeclarations() {
        return servers;
    }

    /** Les incorporations déclarées du langage. */
    public List<LanguageInjection> injections() {
        return injections;
    }

    /** Le support du langage parent, ou null en bout de famille. */
    public @Nullable LanguageSupport parent() {
        return parent;
    }

    /**
     * Le fournisseur d'une capacité : celui du langage, sinon celui de
     * sa famille, sinon le fournisseur générique.
     *
     * @param key la clé de la capacité
     * @param <T> le type du fournisseur
     * @return le fournisseur, ou null si personne ne sert cette capacité
     */
    public <T extends LanguageCapability> @Nullable T capability(Class<T> key) {
        LanguageCapability own = capabilities.get(key);
        if (own != null) {
            return key.cast(own);
        }
        if (parent != null) {
            return parent.capability(key);
        }
        LanguageCapability generic = genericFallbacks.get(key);
        return generic == null ? null : key.cast(generic);
    }

    /**
     * Une copie avec les fournisseurs génériques du registre — plomberie
     * de l'assemblage (la plateforme l'applique à tous les supports
     * d'un registre), jamais appelée en aval.
     *
     * @param fallbacks les fournisseurs génériques par clé
     * @return le support enrichi
     */
    LanguageSupport withGenericFallbacks(
            Map<Class<? extends LanguageCapability>, LanguageCapability> fallbacks) {
        Builder copy = builder(descriptor)
                .configuration(configuration)
                .injections(injections)
                .outlineRules(outlineRules)
                .servers(servers)
                .capabilities(capabilities);
        if (syntax != null) {
            copy.syntax(syntax);
        }
        if (parent != null) {
            copy.parent(parent);
        }
        copy.genericFallbacks.putAll(fallbacks);
        return copy.build();
    }

    /** Le bâtisseur de {@link LanguageSupport}. */
    public static final class Builder {

        private final LanguageDescriptor descriptor;

        private LanguageConfiguration configuration = LanguageConfiguration.EMPTY;

        private @Nullable LanguageSyntax syntax;

        private final List<OutlineRule> outlineRules = new ArrayList<>();

        private final List<LanguageServerDeclaration> servers = new ArrayList<>();

        private final Map<Class<? extends LanguageCapability>, LanguageCapability>
                capabilities = new LinkedHashMap<>();

        private List<LanguageInjection> injections = List.of();

        private @Nullable LanguageSupport parent;

        private final Map<Class<? extends LanguageCapability>, LanguageCapability>
                genericFallbacks = new LinkedHashMap<>();

        private Builder(LanguageDescriptor descriptor) {
            this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        }

        /**
         * Fixe la configuration du langage.
         *
         * @param configuration la configuration
         * @return le bâtisseur
         */
        public Builder configuration(LanguageConfiguration configuration) {
            this.configuration = Objects.requireNonNull(configuration, "configuration");
            return this;
        }

        /**
         * Fixe la source syntaxique du langage.
         *
         * @param syntax la source syntaxique, ou null
         * @return le bâtisseur
         */
        public Builder syntax(@Nullable LanguageSyntax syntax) {
            this.syntax = syntax;
            return this;
        }

        /**
         * Fixe les règles du plan par données.
         *
         * @param rules les règles du plan du paquet
         * @return le bâtisseur
         */
        public Builder outlineRules(List<OutlineRule> rules) {
            this.outlineRules.clear();
            this.outlineRules.addAll(rules);
            return this;
        }

        /**
         * Fixe les serveurs de langage déclarés (H-13) — la donnée du
         * paquet ou celle de l'hôte.
         *
         * @param declarations les déclarations de serveurs
         * @return le bâtisseur
         */
        public Builder servers(List<LanguageServerDeclaration> declarations) {
            this.servers.clear();
            this.servers.addAll(declarations);
            return this;
        }

        /**
         * Ajoute un fournisseur de capacité.
         *
         * @param key      la clé de la capacité
         * @param provider le fournisseur
         * @param <T>      le type du fournisseur
         * @return le bâtisseur
         */
        public <T extends LanguageCapability> Builder capability(Class<T> key, T provider) {
            this.capabilities.put(Objects.requireNonNull(key, "key"),
                    Objects.requireNonNull(provider, "provider"));
            return this;
        }

        /**
         * Ajoute des capacités en bloc (l'assemblage d'un registre).
         *
         * @param existing les capacités déjà résolues
         * @return le bâtisseur
         */
        public Builder capabilities(
                Map<Class<? extends LanguageCapability>, LanguageCapability> existing) {
            this.capabilities.putAll(existing);
            return this;
        }

        /**
         * Fixe les incorporations du langage.
         *
         * @param injections les incorporations
         * @return le bâtisseur
         */
        public Builder injections(List<LanguageInjection> injections) {
            this.injections = List.copyOf(injections);
            return this;
        }

        /**
         * Fixe le support du langage parent — la famille.
         *
         * @param parent le support parent, ou null
         * @return le bâtisseur
         */
        public Builder parent(@Nullable LanguageSupport parent) {
            this.parent = parent;
            return this;
        }

        /**
         * Bâtit le support immuable.
         *
         * @return le support
         */
        public LanguageSupport build() {
            return new LanguageSupport(this);
        }
    }
}
