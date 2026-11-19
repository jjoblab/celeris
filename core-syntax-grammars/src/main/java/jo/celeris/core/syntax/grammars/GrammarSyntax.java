package jo.celeris.core.syntax.grammars;

import java.util.List;

import jo.celeris.core.syntax.LanguageLexer;
import jo.celeris.core.syntax.LanguageParser;
import jo.celeris.core.syntax.LanguageSyntax;

/**
 * Un langage servi par la voie de grammaires (ADR-004 révisé, ADR-014,
 * C-15/C-16) : l'identifiant du langage et sa grammaire déclarative au
 * format TextMate réel — lexeur et parseur génériques en sortent, le
 * même socle d'analyse incrémentale que les analyseurs dédiés les
 * porte. Coloration et plis seulement : le plan reste l'affaire des
 * analyseurs dédiés.
 */
public final class GrammarSyntax implements LanguageSyntax {

    private final String id;
    private final Grammar grammaire;

    /**
     * Un langage par sa grammaire.
     *
     * @param id        l'identifiant du langage (par exemple toml)
     * @param grammaire la grammaire compilée
     */
    public GrammarSyntax(String id, Grammar grammaire) {
        this.id = id;
        this.grammaire = grammaire;
    }

    @Override
    public String id() {
        return id;
    }

    /** La grammaire du langage. */
    public Grammar grammaire() {
        return grammaire;
    }

    @Override
    public LanguageLexer lexer() {
        return new GrammarLexer(grammaire);
    }

    @Override
    public LanguageParser parser() {
        return new GrammarParser();
    }

    /** Les mots réservés moissonnés de la grammaire au chargement —
     *  les alternatives littérales des règles keyword.control : la
     *  complétion de base les sert sans serveur ni table de l'hôte. */
    @Override
    public List<String> reservedWords() {
        return grammaire.keywords();
    }
}
