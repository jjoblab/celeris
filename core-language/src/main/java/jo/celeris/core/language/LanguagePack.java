package jo.celeris.core.language;

import java.util.List;

import jo.celeris.core.syntax.LanguageSyntax;
import jo.celeris.core.syntax.OutlineRule;

import org.jspecify.annotations.Nullable;

/**
 * Un paquet de langage chargé et validé (M7.6) : manifeste,
 * descripteur, configuration, grammaire (compilée par la fabrique de
 * l'hôte), extraits, incorporations, règles du plan et préférence de
 * source syntaxique. Le paquet reste de la donnée ; l'assemblage en
 * {@link LanguageSupport} — le choix de la source, la famille, les
 * capacités — appartient à la plateforme.
 *
 * @param manifest   le manifeste du paquet
 * @param descriptor le descripteur du langage
 * @param configuration la configuration du langage
 * @param grammar    la grammaire compilée, ou null si le paquet n'en porte pas
 * @param grammarJson le texte JSON de la grammaire, ou null
 * @param snippets   les extraits du paquet, éventuellement vides
 * @param injections les incorporations déclarées
 * @param outlineRules les règles du plan par données (H-08), éventuellement vides
 * @param syntaxPreference la préférence de source syntaxique
 *                         ({@code handwritten}, {@code grammar})
 * @param fingerprint l'empreinte SHA-256 des fichiers du paquet
 */
public record LanguagePack(
        PackManifest manifest,
        LanguageDescriptor descriptor,
        LanguageConfiguration configuration,
        @Nullable LanguageSyntax grammar,
        @Nullable String grammarJson,
        List<PackSnippet> snippets,
        List<LanguageInjection> injections,
        List<OutlineRule> outlineRules,
        SyntaxPreference syntaxPreference,
        String fingerprint) {

    /** Constructeur canonique : listes figées, cohérence vérifiée. */
    public LanguagePack {
        snippets = List.copyOf(snippets);
        injections = List.copyOf(injections);
        outlineRules = List.copyOf(outlineRules);
        if (fingerprint == null || fingerprint.isEmpty()) {
            throw new IllegalArgumentException("pack fingerprint must not be empty");
        }
    }

    /**
     * La préférence de source syntaxique d'un paquet (M7.6) : le niveau
     * voulu d'abord — {@code handwritten} pour les analyseurs écrits à
     * la main, {@code grammar} pour la voie de grammaires — et le repli
     * déclaré ensuite, ou null.
     *
     * @param preferred le niveau voulu
     * @param fallback  le niveau de repli, ou null
     */
    public record SyntaxPreference(String preferred, @Nullable String fallback) {
    }
}
