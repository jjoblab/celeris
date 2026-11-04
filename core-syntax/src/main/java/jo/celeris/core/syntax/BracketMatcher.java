package jo.celeris.core.syntax;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.text.Document;

/**
 * Visée de crochets (socle commun, ADR-004) : désigne le partenaire du
 * crochet ADJACENT à l'offset visé — le crochet qui contient l'offset ou
 * celui qui commence juste après (le curseur collé au crochet compte) —
 * par balayage local de la ligne, jamais une passe sur le document. Les
 * crochets dans les commentaires et les chaînes n'existent pas pour le
 * lexique, donc jamais pour la visée.
 */
public final class BracketMatcher {

    private BracketMatcher() {
    }

    /**
     * Le crochet adjacent à l'offset donné et son partenaire.
     *
     * @param doc    le document porteur
     * @param lex    son lexique
     * @param offset l'offset visé (position du curseur)
     * @return la paire (crochet, partenaire) en offsets absolus, ou null
     *         si aucun crochet n'est adjacent à l'offset
     */
    public static @Nullable TokenMatch matchAt(Document doc, LexDocument lex, int offset) {
        int ligne = doc.lineForOffset(offset);
        int debutLigne = doc.lineStart(ligne);
        for (Token jeton : lex.line(ligne).tokens()) {
            boolean ouvrant = jeton.kind() == TokenKind.OPEN_BRACKET;
            boolean fermant = jeton.kind() == TokenKind.CLOSE_BRACKET;
            if (!ouvrant && !fermant) {
                continue;
            }
            int debutAbsolu = debutLigne + jeton.start();
            int finAbsolue = debutLigne + jeton.end();
            if (debutAbsolu <= offset && offset <= finAbsolue) {
                return ouvrant
                        ? chercherApres(doc, lex, ligne, jeton)
                        : chercherAvant(doc, lex, ligne, jeton);
            }
            if (debutAbsolu > offset) {
                return null;
            }
        }
        return null;
    }

    private static @Nullable TokenMatch chercherApres(Document doc, LexDocument lex,
                                                       int ligne, Token ouvrant) {
        int profondeur = 0;
        for (int l = ligne; l < lex.lineCount(); l++) {
            for (Token jeton : lex.line(l).tokens()) {
                if (l == ligne && jeton.start() < ouvrant.start()) {
                    continue;
                }
                if (jeton.kind() == TokenKind.OPEN_BRACKET) {
                    profondeur++;
                } else if (jeton.kind() == TokenKind.CLOSE_BRACKET) {
                    profondeur--;
                    if (profondeur == 0) {
                        return match(doc, ligne, ouvrant, l, jeton);
                    }
                }
            }
        }
        return null;
    }

    private static @Nullable TokenMatch chercherAvant(Document doc, LexDocument lex,
                                                       int ligne, Token fermant) {
        int profondeur = 0;
        for (int l = ligne; l >= 0; l--) {
            Token[] jetons = lex.line(l).tokens();
            for (int i = jetons.length - 1; i >= 0; i--) {
                Token jeton = jetons[i];
                if (l == ligne && jeton.start() > fermant.start()) {
                    continue;
                }
                if (jeton.kind() == TokenKind.CLOSE_BRACKET) {
                    profondeur++;
                } else if (jeton.kind() == TokenKind.OPEN_BRACKET) {
                    profondeur--;
                    if (profondeur == 0) {
                        return match(doc, ligne, fermant, l, jeton);
                    }
                }
            }
        }
        return null;
    }

    private static TokenMatch match(Document doc, int ligneOuvrant, Token ouvrant,
                                    int ligneFermant, Token fermant) {
        return new TokenMatch(
                doc.lineStart(ligneOuvrant) + ouvrant.start(),
                doc.lineStart(ligneOuvrant) + ouvrant.end(),
                doc.lineStart(ligneFermant) + fermant.start(),
                doc.lineStart(ligneFermant) + fermant.end());
    }
}
