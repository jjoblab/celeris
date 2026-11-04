package jo.celeris.core.syntax;

/**
 * État de lexicographie transporté d'une ligne à la suivante (ADR-004).
 * Chaque langage définit les siens (enum recommandé) ; le moteur ne les
 * interprète jamais, il les compare — deux états égaux signifient que la
 * ligne suivante relit de la même façon, la cascade s'arrête là
 * (PIEGE-19). L'égalité d'enum par identité suffit ; tout type à égalité
 * de valeur convient.
 */
public interface LexState {
}
