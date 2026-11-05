package jo.celeris.core.syntax.grammars;

import jo.celeris.core.syntax.LanguageLexer;
import jo.celeris.core.syntax.LanguageParser;
import jo.celeris.core.syntax.LanguageSyntax;

/**
 * Un langage servi par la voie de grammaires (ADR-004, C-15/C-16) :
 * l'identifiant du langage et sa grammaire déclarative — lexeur et
 * parseur génériques en sortent, le même socle d'analyse incrémentale
 * que les analyseurs dédiés les porte. Coloration et plis seulement :
 * le plan reste l'affaire des analyseurs dédiés.
 */
public final class GrammaireSyntax implements LanguageSyntax {

    private final String id;
    private final Grammaire grammaire;

    /**
     * Un langage par sa grammaire.
     *
     * @param id        l'identifiant du langage (par exemple toml)
     * @param grammaire la grammaire compilée
     */
    public GrammaireSyntax(String id, Grammaire grammaire) {
        this.id = id;
        this.grammaire = grammaire;
    }

    @Override
    public String id() {
        return id;
    }

    /** La grammaire du langage. */
    public Grammaire grammaire() {
        return grammaire;
    }

    @Override
    public LanguageLexer lexer() {
        return new GrammaireLexeur(grammaire);
    }

    @Override
    public LanguageParser parser() {
        return new GrammaireParseur();
    }
}
