package jo.celeris.core.syntax.grammars;

import org.jspecify.annotations.Nullable;

/**
 * Une capture au format TextMate réel : un objet par numéro de groupe,
 * portant le nom de portée du texte capturé. Le format des extensions
 * de VS Code admet aussi des sous-motifs dans une capture — ils restent
 * hors du périmètre M7.6 (dégradés au seul nom, comptés au rapport de
 * la grammaire, jamais en silence).
 *
 * @param nom la portée du groupe capturé, null si la capture n'en porte pas
 */
record Capture(@Nullable String nom) {

    /** Une capture sans portée. */
    static final Capture SANS_PORTEE = new Capture(null);
}
