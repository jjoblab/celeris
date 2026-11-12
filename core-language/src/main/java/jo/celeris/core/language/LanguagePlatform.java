package jo.celeris.core.language;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jo.celeris.core.language.model.LanguageCapability;
import jo.celeris.core.language.model.LanguageServerDeclaration;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.language.packs.GrammarFactory;
import jo.celeris.core.language.packs.LanguagePack;
import jo.celeris.core.language.packs.PackLoader;
import jo.celeris.core.language.packs.PackSource;
import jo.celeris.core.language.packs.Packs;
import jo.celeris.core.language.packs.TreeSitterLanguages;
import jo.celeris.core.syntax.LanguageSyntax;

import org.jspecify.annotations.Nullable;

/**
 * La plateforme de langages (M7.6) : l'assemblage d'un registre depuis
 * des paquets (intégrés ou de l'utilisateur), des analyseurs écrits
 * (l'enregistrement {@link WrittenLanguages}) et des contributeurs de
 * capacités. C'est ici que la donnée devient un
 * {@link LanguageSupport} : la source syntaxique suit la préférence du
 * paquet (avec son repli), les familles se lient (kts hérite de
 * kotlin), les fournisseurs génériques servent le bout de chaîne.
 *
 * <p>L'assemblage se fait hors fil UI — c'est l'hôte qui appelle sur
 * son exécuteur de travail ; ici, aucun travail différé, tout est
 * immédiat et vérifié : un paquet invalide, un parent inconnu, une
 * préférence sans source se refusent avec le message précis, le
 * registre ne se bâtit jamais à moitié.
 */
public final class LanguagePlatform {

    /** Les niveaux de préférence syntaxique connus (le niveau 2 y vit). */
    private static final java.util.Set<String> NIVEAUX =
            java.util.Set.of("handwritten", "grammar", "tree-sitter");

    private final PackLoader loader;

    private final List<LanguageSupport> supports;

    private final Map<String, Map<Class<? extends LanguageCapability>,
            LanguageCapability>> contributions;

    private final Map<String, List<LanguageServerDeclaration>> hostServers;

    private final Map<Class<? extends LanguageCapability>, LanguageCapability>
            generics;

    private final Map<String, String> userChoices;

    private LanguagePlatform(PackLoader loader, List<LanguageSupport> supports,
            Map<String, Map<Class<? extends LanguageCapability>, LanguageCapability>> contributions,
            Map<String, List<LanguageServerDeclaration>> hostServers,
            Map<Class<? extends LanguageCapability>, LanguageCapability> generics,
            Map<String, String> userChoices) {
        this.loader = loader;
        this.supports = supports;
        this.contributions = contributions;
        this.hostServers = hostServers;
        this.generics = generics;
        this.userChoices = userChoices;
    }

    /**
     * La plateforme vide, sans fabrique de grammaires.
     *
     * @return la plateforme à assembler
     */
    public static LanguagePlatform empty() {
        return new LanguagePlatform(PackLoader.create(), new ArrayList<>(),
                new LinkedHashMap<>(), new LinkedHashMap<>(),
                new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    /**
     * La plateforme avec la fabrique de grammaires de l'hôte — la voie
     * de grammaires (core-syntax-grammars) l'implémente.
     *
     * @param factory la fabrique de grammaires
     * @return la plateforme à assembler
     */
    public static LanguagePlatform with(GrammarFactory factory) {
        return new LanguagePlatform(PackLoader.with(factory), new ArrayList<>(),
                new LinkedHashMap<>(), new LinkedHashMap<>(),
                new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    /**
     * Ajoute les paquets d'une source : chacun se charge et se valide
     * entier, le premier refus arrête l'assemblage.
     *
     * @param source la source des paquets
     * @return la plateforme
     */
    public LanguagePlatform addPackSource(PackSource source) {
        try {
            for (PackSource.PackFiles files : source.packs()) {
                LanguagePack pack = loader.load(files);
                supports.add(supportDuPaquet(pack));
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "pack source unreadable: " + e.getMessage(), e);
        }
        return this;
    }

    /**
     * Ajoute les paquets intégrés — les ressources du module sous
     * {@code langages/}, énumérées par l'index généré à la
     * construction. Les analyseurs écrits s'y attachent par la
     * préférence {@code handwritten} des paquets (l'enregistrement
     * {@link WrittenLanguages}) — les ajouter à part dupliquerait les
     * identifiants.
     *
     * @return la plateforme
     */
    public LanguagePlatform builtinPacks() {
        return addPackSource(Packs.classpath("langages"));
    }

    /**
     * Ajoute un support directement — le cas d'un hôte qui bâtit à la
     * main.
     *
     * @param support le support à ajouter
     * @return la plateforme
     */
    public LanguagePlatform support(LanguageSupport support) {
        supports.add(support);
        return this;
    }

    /**
     * Ajoute les capacités d'un contributeur.
     *
     * @param contributor le contributeur
     * @return la plateforme
     */
    public LanguagePlatform contributor(LanguageContributor contributor) {
        contributor.contribute(new ContributionCollector());
        return this;
    }

    /**
     * Ajoute un fournisseur générique — le repli de tous les langages
     * pour cette clé (la complétion par les mots du code).
     *
     * @param key      la clé de la capacité
     * @param provider le fournisseur générique
     * @param <T>      le type du fournisseur
     * @return la plateforme
     */
    public <T extends LanguageCapability> LanguagePlatform generic(Class<T> key,
            T provider) {
        generics.put(key, provider);
        return this;
    }

    /**
     * Ajoute les serveurs de langage déclarés par l'HÔTE (H-13) : de
     * la donnée lue hors des paquets — le fichier
     * {@code servers.json} de l'hôte, par exemple. Chaque déclaration
     * porte son langage ; l'éditeur ne connaît aucun serveur, il porte
     * la déclaration jusqu'à son hôte.
     *
     * @param declarations les déclarations de l'hôte
     * @return la plateforme
     */
    public LanguagePlatform servers(
            List<LanguageServerDeclaration> declarations) {
        for (LanguageServerDeclaration declaration : declarations) {
            hostServers.computeIfAbsent(declaration.language(),
                    id -> new ArrayList<>()).add(declaration);
        }
        return this;
    }

    /**
     * Le réglage de l'utilisateur : ce fichier parle ce langage.
     *
     * @param filename   le nom de fichier exact
     * @param languageId l'identifiant du langage imposé
     * @return la plateforme
     */
    public LanguagePlatform userChoice(String filename, String languageId) {
        userChoices.put(filename, languageId);
        return this;
    }

    /**
     * Bâtit le registre : familles liées, capacités et replis
     * appliqués, unicité vérifiée.
     *
     * @return le registre assemblé
     */
    public LanguageRegistry build() {
        Map<String, LanguageSupport> parId = new LinkedHashMap<>();
        for (LanguageSupport support : supports) {
            parId.putIfAbsent(support.id(), support);
        }
        Map<String, LanguageSupport> lies = new LinkedHashMap<>();
        for (LanguageSupport support : supports) {
            lies.putIfAbsent(support.id(),
                    avecFamilleEtCapacites(support, parId, lies));
        }
        LanguageRegistry.Builder batisseur = LanguageRegistry.builder();
        for (LanguageSupport support : lies.values()) {
            batisseur.support(support);
        }
        userChoices.forEach(batisseur::userChoice);
        return batisseur.build();
    }

    /** Le support d'un paquet : la source suit la préférence et son repli. */
    private LanguageSupport supportDuPaquet(LanguagePack pack) {
        LanguageSupport.Builder batisseur = LanguageSupport
                .builder(pack.descriptor())
                .configuration(pack.configuration())
                .injections(pack.injections())
                .outlineRules(pack.outlineRules())
                .servers(pack.servers());
        LanguageSyntax source = sourceSyntaxique(pack);
        if (source != null) {
            batisseur.syntax(source);
        }
        return batisseur.build();
    }

    /** La source syntaxique d'un paquet : préférence, repli, refus précis. */
    private @Nullable LanguageSyntax sourceSyntaxique(LanguagePack pack) {
        String preferee = pack.syntaxPreference().preferred();
        LanguageSyntax cherie = prefereeCherie(pack, preferee);
        if (cherie != null) {
            return cherie;
        }
        if (!NIVEAUX.contains(preferee)) {
            throw new IllegalArgumentException(pack.manifest().id()
                    + ": unknown syntax preference " + preferee);
        }
        String repli = pack.syntaxPreference().fallback();
        if ("grammar".equals(repli) && pack.grammar() != null) {
            return pack.grammar();
        }
        if ("handwritten".equals(repli)) {
            LanguageSyntax ecrite = WrittenLanguages.syntaxOf(pack.descriptor().id());
            if (ecrite != null) {
                return ecrite;
            }
        }
        if ("tree-sitter".equals(repli) && TreeSitterLanguages.syntaxOf(
                pack.descriptor().id()) != null) {
            return TreeSitterLanguages.syntaxOf(pack.descriptor().id());
        }
        if (preferee.equals("grammar") && pack.grammar() == null) {
            throw new IllegalArgumentException(pack.manifest().id()
                    + ": syntax preference \"grammar\" without grammar.json");
        }
        throw new IllegalArgumentException(pack.manifest().id()
                + ": syntax preference \"" + preferee + "\" has no source");
    }

    /** La source de la préférence voulue, ou null — le niveau 2 vit ici. */
    private @Nullable LanguageSyntax prefereeCherie(LanguagePack pack, String preferee) {
        if ("handwritten".equals(preferee)) {
            return WrittenLanguages.syntaxOf(pack.descriptor().id());
        }
        if ("grammar".equals(preferee)) {
            return pack.grammar();
        }
        if ("tree-sitter".equals(preferee)) {
            return TreeSitterLanguages.syntaxOf(pack.descriptor().id());
        }
        return null;
    }

    /**
     * Le support lié à sa famille, portant ses capacités et ses replis.
     * Les liens déjà bâtis se réemploient : le parent d'un support du
     * registre EST l'instance du registre.
     */
    private LanguageSupport avecFamilleEtCapacites(LanguageSupport support,
            Map<String, LanguageSupport> parId,
            Map<String, LanguageSupport> lies) {
        String parentId = support.descriptor().parentId();
        LanguageSupport parent = null;
        if (parentId != null) {
            parent = lies.get(parentId);
            if (parent == null) {
                LanguageSupport brut = parId.get(parentId);
                if (brut == null) {
                    throw new IllegalArgumentException("language " + support.id()
                            + ": unknown parent language " + parentId);
                }
                parent = avecFamilleEtCapacites(brut, parId, lies);
                lies.put(parentId, parent);
            }
        }
        LanguageSupport.Builder batisseur = LanguageSupport
                .builder(support.descriptor())
                .configuration(support.configuration())
                .injections(support.injections())
                .outlineRules(support.outlineRules())
                .servers(serveursDe(support.id()))
                .parent(parent);
        if (support.syntax() != null) {
            batisseur.syntax(support.syntax());
        }
        Map<Class<? extends LanguageCapability>, LanguageCapability> capacites =
                contributions.get(support.id());
        if (capacites != null) {
            batisseur.capabilities(capacites);
        }
        return batisseur.build().withGenericFallbacks(generics);
    }

    /**
     * Les serveurs d'un langage assemblé (H-13) : ceux du paquet,
     * puis ceux de l'hôte — la donnée seule décide.
     */
    private List<LanguageServerDeclaration> serveursDe(String languageId) {
        List<LanguageServerDeclaration> duPaquet = new ArrayList<>();
        for (LanguageSupport support : supports) {
            if (support.id().equals(languageId)) {
                duPaquet.addAll(support.serverDeclarations());
            }
        }
        duPaquet.addAll(hostServers.getOrDefault(languageId, List.of()));
        return duPaquet;
    }

    /** Le collecteur des capacités d'un contributeur. */
    private final class ContributionCollector
            implements LanguageContributor.Contribution {

        @Override
        public <T extends LanguageCapability> void capability(String languageId,
                Class<T> key, T provider) {
            contributions.computeIfAbsent(languageId, id -> new LinkedHashMap<>())
                    .put(key, provider);
        }

        @Override
        public <T extends LanguageCapability> @Nullable T declared(
                String languageId, Class<T> key) {
            Map<Class<? extends LanguageCapability>, LanguageCapability> capacites =
                    contributions.get(languageId);
            return capacites == null ? null : key.cast(capacites.get(key));
        }
    }
}
