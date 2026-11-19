package jo.celeris.core.syntax.treesitter;

import jo.celeris.core.syntax.LanguageLexer;
import jo.celeris.core.syntax.LanguageParser;

import jo.celeris.core.syntax.LanguageSyntax;
import org.jspecify.annotations.Nullable;

/**
 * La source syntaxique du niveau 2 (ADR-020, révisé par l'ADR-0025 :
 * la liaison AndroidIDE vit par Maven) : l'arrière natif analyse le
 * document ENTIER en une passe — lexeur et parseur ligne à ligne ne
 * s'y servent jamais, le pont de vue ({@code EditorSyntax}) y
 * branche le moteur natif dès qu'il reconnaît la source. Le contrat
 * {@link LanguageSyntax} reste tenu : la plateforme peut
 * l'enregistrer dans la case déclarée du niveau 2, ses composants
 * ligne à ligne restent muets.
 *
 * <p>Les langages servis : json, python — les grammaires PUBLIÉES de
 * la liaison (ADR-0025 ; javascript n'a pas d'artefact publié, son
 * paquet garde son repli TextMate déclaré). Absente la liaison (banc
 * JVM sans Android), les fabriques rendent null : les paquets
 * retombent sur leur repli déclaré, aucun consommateur ne change.</p>
 */
public final class SyntaxeNatif implements LanguageSyntax {

    /** La grammaire servie par cette source. */
    private final GrammaireNatif grammaire;

    private SyntaxeNatif(GrammaireNatif grammaire) {
        this.grammaire = grammaire;
    }

    /**
     * La source json du niveau 2 — null sans liaison chargée.
     *
     * @return la source, ou null
     */
    public static @Nullable SyntaxeNatif json() {
        return deLangue(GrammaireNatif.JSON);
    }

    /**
     * La source python du niveau 2 — null sans liaison chargée.
     *
     * @return la source, ou null
     */
    public static @Nullable SyntaxeNatif python() {
        return deLangue(GrammaireNatif.PYTHON);
    }

    /** La source du langage donné, null sans liaison. */
    private static @Nullable SyntaxeNatif deLangue(GrammaireNatif g) {
        return LiaisonArbre.presente() ? new SyntaxeNatif(g) : null;
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
