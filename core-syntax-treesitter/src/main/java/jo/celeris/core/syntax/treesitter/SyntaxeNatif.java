package jo.celeris.core.syntax.treesitter;

import jo.celeris.core.syntax.LanguageLexer;
import jo.celeris.core.syntax.LanguageParser;

import jo.celeris.core.syntax.LanguageSyntax;
import org.jspecify.annotations.Nullable;

/**
 * La source syntaxique du niveau 2 (ADR-020, révisé par l'ADR-0025
 * et généralisée par l'ADR-0033) : l'arrière natif analyse le
 * document ENTIER en une passe — lexeur et parseur ligne à ligne ne
 * s'y servent jamais, le pont de vue ({@code EditorSyntax}) y
 * branche le moteur natif dès qu'il reconnaît la source. Le contrat
 * {@link LanguageSyntax} reste tenu : la plateforme peut
 * l'enregistrer dans la case déclarée du niveau 2, ses composants
 * ligne à ligne restent muets.
 *
 * <p>La source vit par une GRAMMAIRE enregistrée
 * ({@link TreeSitterGrammars#enregistrer}) : la langue vient de la
 * liaison (l'artefact de la grammaire chez l'hôte), la requête de
 * surbrillance l'accompagne. Absente la liaison (banc JVM sans
 * Android), la langue ne se charge pas : les paquets retombent sur
 * leur repli déclaré, aucun consommateur ne change.</p>
 */
public final class SyntaxeNatif implements LanguageSyntax {

    /** La grammaire servie par cette source. */
    private final GrammaireNatif grammaire;

    SyntaxeNatif(GrammaireNatif grammaire) {
        this.grammaire = grammaire;
    }

    /** La grammaire auprès de la liaison — le moteur y lit. */
    GrammaireNatif grammaire() {
        return grammaire;
    }

    @Override
    public String id() {
        return grammaire.id();
    }

    @Override
    public LanguageLexer lexer() {
        throw new IllegalStateException(
                "the native lane serves the whole document, not lines");
    }

    @Override
    public LanguageParser parser() {
        throw new IllegalStateException(
                "the native lane serves the whole document, not chunks");
    }
}
