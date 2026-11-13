package jo.celeris.core.syntax;

import org.jspecify.annotations.Nullable;

/**
 * La source syntaxique du niveau 2 (ADR-020 — la révision de l'ADR-016
 * sur signal du propriétaire du 2026-11-13 : « tu dois lancer le
 * chantier tree sitter ») : l'arrière natif analyse le document ENTIER
 * en une passe — lexeur et parseur ligne à ligne ne s'y servent jamais,
 * le pont de vue ({@code EditorSyntax}) y branche le moteur natif dès
 * qu'il reconnaît la source. Le contrat {@link LanguageSyntax} reste
 * tenu : la plateforme peut l'enregistrer dans la case déclarée du
 * niveau 2, ses composants ligne à ligne restent muets.
 *
 * <p>Les langages servis : json, python, javascript — les grammaires
 * épinglées du spike (MIT, provenance par {@code natif/construire}).
 * Absente la bibliothèque partagée, les fabriques rendent null : les
 * paquets retombent sur leur repli déclaré, aucun consommateur ne
 * change.</p>
 */
public final class SyntaxeNatif implements LanguageSyntax {

    /** L'index du langage auprès de la liaison native. */
    private final int langue;

    private SyntaxeNatif(int langue) {
        this.langue = langue;
    }

    /**
     * La source json du niveau 2 — null sans bibliothèque partagée.
     *
     * @return la source, ou null
     */
    public static @Nullable SyntaxeNatif json() {
        return deLangue(0);
    }

    /**
     * La source python du niveau 2 — null sans bibliothèque partagée.
     *
     * @return la source, ou null
     */
    public static @Nullable SyntaxeNatif python() {
        return deLangue(1);
    }

    /**
     * La source javascript du niveau 2 — null sans bibliothèque partagée.
     *
     * @return la source, ou null
     */
    public static @Nullable SyntaxeNatif javascript() {
        return deLangue(2);
    }

    /** La source du langage d'index donné, null sans bibliothèque. */
    private static @Nullable SyntaxeNatif deLangue(int index) {
        return LiaisonArbre.presente() ? new SyntaxeNatif(index) : null;
    }

    /** L'index du langage auprès de la liaison — le moteur y lit. */
    int langue() {
        return langue;
    }

    @Override
    public String id() {
        return switch (langue) {
            case 0 -> "json";
            case 1 -> "python";
            default -> "javascript";
        };
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
