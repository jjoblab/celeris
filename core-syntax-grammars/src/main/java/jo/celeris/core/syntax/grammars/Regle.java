package jo.celeris.core.syntax.grammars;

/**
 * Une règle de grammaire TextMate : un motif simple, un bloc début/fin
 * ou une référence d'inclusion ({@code #clé} du référentiel,
 * {@code $self}, {@code $base}, portée externe) — les inclusions
 * survivent au chargement, parcourues au fil de la lexicographie.
 */
public interface Regle {
}
