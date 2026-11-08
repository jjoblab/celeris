package jo.celeris.core.syntax.grammars;

import jo.celeris.core.language.GrammarFactory;
import jo.celeris.core.language.LanguagePack;
import jo.celeris.core.syntax.LanguageSyntax;

/**
 * La fabrique de sources syntaxiques par grammaire (M7.6) :
 * l'implémentation de la couture {@link GrammarFactory} du module du
 * bas — le chargeur de paquets compile la grammaire JSON par la voie
 * de grammaires, la préférence {@code "grammar"} du
 * {@link LanguagePack.SyntaxPreference} la résout. Le code de
 * compilation vit ici (Grammaires), le paquet reste de la donnée.
 */
public final class PackGrammars implements GrammarFactory {

    /** L'instance unique — la fabrique est sans état. */
    public static final PackGrammars INSTANCE = new PackGrammars();

    private PackGrammars() {
    }

    /**
     * Compile la grammaire JSON d'un langage en source syntaxique —
     * les erreurs de Grammaires remontent telles quelles : le message
     * précis du refus.
     *
     * @param languageId  l'identifiant du langage du paquet
     * @param grammarJson le texte JSON de la grammaire
     * @return la source syntaxique de la voie de grammaires
     * @throws IllegalArgumentException si la grammaire est mal formée
     */
    @Override
    public LanguageSyntax compile(String languageId, String grammarJson) {
        return new GrammaireSyntax(languageId, Grammaires.lire(grammarJson));
    }
}
