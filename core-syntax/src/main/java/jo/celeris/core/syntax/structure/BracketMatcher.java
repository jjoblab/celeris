package jo.celeris.core.syntax.structure;

import jo.celeris.core.syntax.LexDocument;
import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.syntax.TokenMatch;
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
        int line = doc.lineForOffset(offset);
        int lineStart = doc.lineStart(line);
        for (Token token : lex.line(line).tokens()) {
            boolean ouvrant = token.kind() == TokenKind.OPEN_BRACKET;
            boolean fermant = token.kind() == TokenKind.CLOSE_BRACKET;
            if (!ouvrant && !fermant) {
                continue;
            }
            int debutAbsolu = lineStart + token.start();
            int finAbsolue = lineStart + token.end();
            if (debutAbsolu <= offset && offset <= finAbsolue) {
                return ouvrant
                        ? chercherApres(doc, lex, line, token)
                        : chercherAvant(doc, lex, line, token);
            }
            if (debutAbsolu > offset) {
                return null;
            }
        }
        return null;
    }

    private static @Nullable TokenMatch chercherApres(Document doc, LexDocument lex,
                                                       int line, Token ouvrant) {
        int profondeur = 0;
        for (int l = line; l < lex.lineCount(); l++) {
            for (Token token : lex.line(l).tokens()) {
                if (l == line && token.start() < ouvrant.start()) {
                    continue;
                }
                if (token.kind() == TokenKind.OPEN_BRACKET) {
                    profondeur++;
                } else if (token.kind() == TokenKind.CLOSE_BRACKET) {
                    profondeur--;
                    if (profondeur == 0) {
                        return match(doc, line, ouvrant, l, token);
                    }
                }
            }
        }
        return null;
    }

    private static @Nullable TokenMatch chercherAvant(Document doc, LexDocument lex,
                                                       int line, Token fermant) {
        int profondeur = 0;
        for (int l = line; l >= 0; l--) {
            Token[] tokens = lex.line(l).tokens();
            for (int i = tokens.length - 1; i >= 0; i--) {
                Token token = tokens[i];
                if (l == line && token.start() > fermant.start()) {
                    continue;
                }
                if (token.kind() == TokenKind.CLOSE_BRACKET) {
                    profondeur++;
                } else if (token.kind() == TokenKind.OPEN_BRACKET) {
                    profondeur--;
                    if (profondeur == 0) {
                        return match(doc, line, fermant, l, token);
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
