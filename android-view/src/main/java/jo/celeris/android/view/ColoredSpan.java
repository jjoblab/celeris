package jo.celeris.android.view;

import jo.celeris.core.syntax.TokenKind;

/**
 * Une plage colorée de rangée : la sorte de jeton et ses bornes
 * RELATIVES au texte de la rangée (fin exclusive) — B-01, la coloration
 * lit les paniers du même arbre que les plis et les diagnostics
 * (ADR-004).
 *
 * <p>Les plages d'une rangée la couvrent exactement, sans trou à
 * peindre : le lexeur soude les jetons (les blancs appartiennent au
 * jeton qui les suit) et tout écart résiduel se comble à
 * l'identificateur — l'encre de base par défaut.</p>
 *
 * @param kind  la sorte de jeton qui colore la plage
 * @param start début relatif de la plage dans le texte de la rangée
 * @param end   fin exclusive de la plage
 */
public record ColoredSpan(TokenKind kind, int start, int end) {

    /** Valide la plage : bornes croissantes, fin exclusive. */
    public ColoredSpan {
        if (start < 0) {
            throw new IllegalArgumentException("negative span start: " + start);
        }
        if (end <= start) {
            throw new IllegalArgumentException(
                    "span end must be strictly after start: " + start + ".." + end);
        }
    }

    /** Longueur de la plage en caractères. */
    public int length() {
        return end - start;
    }
}
