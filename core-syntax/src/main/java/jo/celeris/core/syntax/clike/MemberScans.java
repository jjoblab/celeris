package jo.celeris.core.syntax.clike;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.syntax.Jetons;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.SynNode;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.syntax.TokenSlice;
import org.jspecify.annotations.Nullable;

/**
 * Les scans de membres de la famille C (M7.6, H-10) : les deux
 * familles que les dialectes choisissent — le membre à point-virgule
 * (Java : la fin au séparateur, le nom deviné, la méthode si des
 * parenthèses précèdent le corps) et le membre à ligne (Kotlin,
 * Groovy : la fin à la ligne suivante, ces langues vivent sans
 * point-virgule). Sans état : chaque scan lit la tranche et rend sa
 * réponse, la construction délègue les corps à {@link Corps}.
 */
final class MemberScans {

    /**
     * Le scan d'un membre à point-virgule (Java) : son corps
     * d'accolades avant toute affectation, sa fin au point-virgule ou
     * au crochet fermant, l'erreur au crochet non fermé.
     */
    record SemicolonScan(int end, int body, int bodyEnd, boolean equalSeen,
            boolean error) {
    }

    /**
     * Le scan d'un membre à ligne (Kotlin, Groovy) : son corps
     * d'accolades avant toute affectation, sa fin à la ligne suivante
     * ou au crochet fermant.
     */
    record LineScan(int opening, int closing, int end) {
    }

    private MemberScans() {
    }

    /**
     * Construit et ajoute le membre à point-virgule : le scan depuis
     * le début utile, le nom deviné, la méthode si des parenthèses
     * précèdent le corps, le champ sinon — rend sa fin.
     *
     * @param slice   la tranche de jetons du chunk
     * @param start   le début brut du membre, trivia compris
     * @param to      la borne du corps contenant
     * @param members la liste des membres en construction
     * @return la position de reprise dans la tranche
     */
    static int parseSemicolonMember(TokenSlice slice, int start, int to,
            List<SynNode> members) {
        int afterTrivia = Jetons.sauterTrivia(slice, start);
        SemicolonScan scan = scanSemicolonMember(slice, afterTrivia, to);
        members.add(buildSemicolonMember(slice, start, scan));
        return scan.end();
    }

    /** Le membre à point-virgule construit : son corps, sa sorte, son nom deviné. */
    private static SynNode buildSemicolonMember(TokenSlice slice, int start,
            SemicolonScan scan) {
        String name = findName(slice, start, scan);
        if (scan.body() >= 0) {
            int to = scan.bodyEnd() < 0 ? slice.size() : scan.bodyEnd();
            List<SynNode> body = Corps.instructions(slice, scan.body() + 1, to,
                    CLikeKind.STATEMENT, CLikeKind.BLOC, CLikeKind.ERREUR);
            boolean error = scan.bodyEnd() < 0;
            if (error) {
                body.add(SynNode.of(CLikeKind.ERREUR, NodeCategory.ERROR,
                        slice.start(scan.body()), Jetons.finDe(slice, slice.size() - 1),
                        new SynNode[0], null, true));
            }
            boolean method = firstParenthesis(slice, start, scan) >= 0;
            return SynNode.of(method ? CLikeKind.METHOD : CLikeKind.INITIALIZER,
                    method ? NodeCategory.FUNCTION : NodeCategory.FIELD,
                    slice.start(start), Jetons.finDe(slice, scan.end() - 1),
                    body.toArray(new SynNode[0]), name, error);
        }
        return SynNode.of(CLikeKind.FIELD, NodeCategory.FIELD, slice.start(start),
                Jetons.finDe(slice, scan.end() - 1), new SynNode[0], name,
                scan.error());
    }

    /** La première parenthèse du membre, ou -1 (elle décide méthode ou champ). */
    private static int firstParenthesis(TokenSlice slice, int start,
            SemicolonScan scan) {
        for (int i = start; i < scan.end() && i < slice.size(); i++) {
            if (slice.kind(i) == TokenKind.OPEN_BRACKET
                    && "(".equals(Jetons.texteCrochet(slice, i))) {
                return i;
            }
        }
        return -1;
    }

    /** Le nom d'un membre à point-virgule : identificateur puis parenthèse ou séparateur. */
    private static @Nullable String findName(TokenSlice slice, int start,
            SemicolonScan scan) {
        for (int i = start; i < scan.end() && i < slice.size(); i++) {
            TokenKind sort = slice.kind(i);
            if (sort != TokenKind.IDENTIFIER && sort != TokenKind.TYPE
                    && sort != TokenKind.CONSTANT) {
                continue;
            }
            if (i + 1 >= slice.size()) {
                return sort == TokenKind.IDENTIFIER ? Jetons.mot(slice, i) : null;
            }
            TokenKind next = slice.kind(i + 1);
            if (next == TokenKind.OPEN_BRACKET
                    && "(".equals(Jetons.texteCrochet(slice, i + 1))) {
                return Jetons.mot(slice, i);
            }
            if (next == TokenKind.OPERATOR || next == TokenKind.PUNCTUATION) {
                if (sort == TokenKind.IDENTIFIER || sort == TokenKind.CONSTANT) {
                    return Jetons.mot(slice, i);
                }
            }
        }
        return null;
    }

    /** Le scan à point-virgule d'un membre Java, de {@code from} à {@code to}. */
    static SemicolonScan scanSemicolonMember(TokenSlice slice, int from, int to) {
        boolean equalSeen = false;
        for (int i = from; i < to; i++) {
            TokenKind sort = slice.kind(i);
            if (sort == TokenKind.OPEN_BRACKET) {
                if (!equalSeen && "{".equals(Jetons.texteCrochet(slice, i))) {
                    int closing = slice.matchOf(i);
                    return new SemicolonScan(closing < 0 ? to : closing + 1,
                            i, closing, equalSeen, false);
                }
                int closing = slice.matchOf(i);
                if (closing < 0) {
                    return new SemicolonScan(to, -1, -1, equalSeen, true);
                }
                i = closing;
            } else if (sort == TokenKind.CLOSE_BRACKET) {
                return new SemicolonScan(i, -1, -1, equalSeen, false);
            } else if (sort == TokenKind.OPERATOR
                    && "=".equals(Jetons.mot(slice, i))) {
                equalSeen = true;
            } else if (sort == TokenKind.PUNCTUATION
                    && ";".equals(Jetons.mot(slice, i))) {
                return new SemicolonScan(i + 1, -1, -1, equalSeen, false);
            }
        }
        return new SemicolonScan(to, -1, -1, equalSeen, false);
    }

    /** Le scan à ligne d'un membre Kotlin ou Groovy, depuis son mot-clé. */
    static LineScan scanLineMember(TokenSlice slice, int keyword) {
        int i = keyword + 1;
        boolean equalSeen = false;
        while (i < slice.size()) {
            TokenKind sort = slice.kind(i);
            if (sort == TokenKind.OPEN_BRACKET) {
                int close = slice.matchOf(i);
                if ("{".equals(Jetons.texteCrochet(slice, i)) && !equalSeen) {
                    return new LineScan(i, close, close < 0 ? slice.size() : close + 1);
                }
                if (close < 0) {
                    return new LineScan(-1, -1, slice.size());
                }
                i = close + 1;
                continue;
            }
            if (sort == TokenKind.CLOSE_BRACKET
                    || slice.line(i) > slice.line(keyword)) {
                return new LineScan(-1, -1, i);
            }
            if (sort == TokenKind.OPERATOR && "=".equals(Jetons.mot(slice, i))) {
                equalSeen = true;
            }
            i++;
        }
        return new LineScan(-1, -1, slice.size());
    }

    /** Le nom qui suit un mot-clé de déclaration, ou null (objet anonyme). */
    static @Nullable String nameAfter(TokenSlice slice, int keyword) {
        if (keyword + 1 >= slice.size()) {
            return null;
        }
        TokenKind sort = slice.kind(keyword + 1);
        if (sort == TokenKind.IDENTIFIER || sort == TokenKind.TYPE
                || sort == TokenKind.CONSTANT) {
            return Jetons.mot(slice, keyword + 1);
        }
        return null;
    }

    /** La fin d'un membre déclaré à la ligne : après son corps ou à sa fin de ligne. */
    static int memberEnd(TokenSlice slice, int keyword, int to) {
        LineScan scan = scanLineMember(slice, keyword);
        if (scan.opening() >= 0 && scan.closing() >= 0) {
            return scan.closing() + 1;
        }
        if (scan.end() < to) {
            return scan.end();
        }
        return to;
    }

    /**
     * Construit le membre à ligne : fonction ou champ selon le
     * dialecte, le corps d'accolades scanné depuis son mot-clé,
     * l'erreur à la fermeture manquante, la fin à la ligne.
     *
     * @param slice    la tranche de jetons du chunk
     * @param start    le début brut du membre, trivia compris
     * @param keyword  l'indice du mot-clé de déclaration
     * @param function vrai pour une fonction, faux pour un champ
     * @param name     le nom du membre, ou null (objet anonyme)
     * @return le nœud du membre
     */
    static SynNode lineMember(TokenSlice slice, int start, int keyword,
            boolean function, @Nullable String name) {
        LineScan scan = scanLineMember(slice, keyword);
        int opening = scan.opening();
        int closing = scan.closing();
        int end = scan.end();
        boolean error = opening >= 0 && closing < 0;
        List<SynNode> body = opening < 0 ? new ArrayList<>()
                : Corps.instructions(slice, opening + 1,
                        closing < 0 ? slice.size() : closing,
                        CLikeKind.STATEMENT, CLikeKind.BLOC, CLikeKind.ERREUR);
        if (error) {
            body.add(SynNode.of(CLikeKind.ERREUR, NodeCategory.ERROR,
                    slice.start(opening), Jetons.finDe(slice, slice.size() - 1),
                    new SynNode[0], null, true));
        }
        return SynNode.of(function ? CLikeKind.METHOD : CLikeKind.FIELD,
                function ? NodeCategory.FUNCTION : NodeCategory.FIELD,
                slice.start(start),
                Jetons.finDe(slice, Math.min(end, slice.size()) - 1),
                body.toArray(new SynNode[0]), name, error);
    }
}
