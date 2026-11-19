package jo.celeris.core.syntax;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.text.Document;

/**
 * Moteur d'analyse incrémentale de l'arbre (ADR-004) : les chunks de haut
 * niveau pavent le document sans s'imbriquer ; une édition ne ré-analyse
 * que les chunks propriétaires des lignes touchées — la preuve en est
 * l'identité des sous-arbres conservés. Le découpage repasse la plage
 * touchée (prédicat du langage à profondeur de découpe), les chunks en
 * dehors gardent leur sous-arbre et ne portent que leurs ancres décalées
 * quand le nombre de lignes bouge.
 *
 * <p>Tolérance : le parseur du langage ne lève jamais ; un chunk malformé
 * ressort avec son drapeau d'erreur, l'arbre couvre toujours tout.</p>
 *
 * <p>Ouverture progressive (G-01) : la marche des jetons calcule la
 * géométrie de TOUS les chunks — c'est bon marché — et l'analyse d'un
 * chunk (parseChunk) se paie séparément : la fenêtre visible d'abord,
 * les autres posés en attente, la complétion les construit un à un
 * sans retoucher les sous-arbres déjà là (points de contrôle du jeton
 * entre les chunks, ADR-007).</p>
 */
final class IncrementalParser {

    /**
     * Analyse complète : le découpage parcourt tout le document.
     *
     * @param doc      le document
     * @param lex      son lexique
     * @param parser   le parseur du langage
     * @param revision la révision du document
     * @return l'arbre versionné
     */
    SyntaxTree parseFull(Document doc, LexDocument lex, LanguageParser parser, int revision) {
        return new SyntaxTree(revision,
                split(doc, lex, parser, 0, doc.lineCount() - 1, 0, 0, false));
    }

    /**
     * Analyse par fenêtre (G-01) : la géométrie de tous les chunks se
     * calcule en marchant les jetons, seuls les chunks coupés par la
     * fenêtre se construisent — les autres attendent.
     *
     * @param doc              le document
     * @param lex              son lexique complet
     * @param parser           le parseur du langage
     * @param revision         la révision du document
     * @param windowFirst  la première ligne visible
     * @param windowLast  la dernière ligne visible
     * @param token            le jeton d'annulation, interrogé avant chaque chunk
     * @return l'arbre versionné, partiel : chunks hors fenêtre en attente
     */
    SyntaxTree parseFenetre(Document doc, LexDocument lex, LanguageParser parser, int revision,
                            int windowFirst, int windowLast, TokenCancellation token) {
        int nombre = doc.lineCount();
        if (nombre == 0) {
            return new SyntaxTree(revision, List.of());
        }
        PasseTokens passe = collecter(lex, 0, nombre - 1);
        if (passe.tokens().length == 0) {
            return new SyntaxTree(revision, List.of());
        }
        TokenSlice tranche = new TokenSlice(doc, passe.tokens(), passe.lines(), 0,
                passe.tokens().length, doc.lineStart(0));
        List<Integer> bounds = bounds(tranche, parser, 0);
        List<Chunk> chunks = new ArrayList<>(bounds.size());
        for (int b = 0; b < bounds.size(); b++) {
            token.check();
            int start = bounds.get(b);
            int end = b + 1 < bounds.size() ? bounds.get(b + 1) - 1 : tranche.size() - 1;
            chunks.add(place(doc, parser, passe, start, end,
                    windowFirst, windowLast));
        }
        return new SyntaxTree(revision, chunks);
    }

    /**
     * Complétion d'un arbre partiel : construit au plus {@code maxChunks}
     * chunks en attente, dans l'ordre du document — les autres gardent
     * leur marqueur, les sous-arbres déjà construits leur identité.
     *
     * @param doc       le document de l'analyse partielle
     * @param lex       son lexique
     * @param parser    le parseur du langage
     * @param revision  la révision du document
     * @param previous  l'arbre partiel
     * @param token     le jeton d'annulation, interrogé avant chaque chunk
     * @param maxChunks le nombre maximal de chunks à construire
     * @return l'arbre versionné, complété d'autant de chunks
     */
    SyntaxTree completer(Document doc, LexDocument lex, LanguageParser parser, int revision,
                         SyntaxTree previous, TokenCancellation token, int maxChunks) {
        List<Chunk> anciens = previous.chunks();
        if (anciens.isEmpty() || maxChunks <= 0) {
            return previous;
        }
        List<Chunk> chunks = new ArrayList<>(anciens);
        int construits = 0;
        for (int i = 0; i < chunks.size() && construits < maxChunks; i++) {
            Chunk ancien = chunks.get(i);
            if (!ancien.isPending()) {
                continue;
            }
            token.check();
            chunks.set(i, analyser(doc, parser, lex, ancien));
            construits++;
        }
        if (construits == 0) {
            return previous;
        }
        return new SyntaxTree(revision, chunks);
    }

    /**
     * Analyse incrémentale après une édition : seuls les chunks
     * propriétaires des lignes touchées se ré-analysent — les chunks en
     * attente se décalent et restent en attente.
     *
     * @param doc      le document après l'édition
     * @param lex      son lexique incrémental
     * @param parser   le parseur du langage
     * @param revision la révision du document
     * @param previous l'arbre précédent
     * @param diff     la passe lexicographique incrémentale
     * @param site     le site de l'édition en lignes
     * @return l'arbre versionné, sous-arbres conservés à l'identité
     */
    SyntaxTree parseIncremental(Document doc, LexDocument lex, LanguageParser parser,
                                int revision, SyntaxTree previous, LexResult diff,
                                EditSite site) {
        List<Chunk> anciens = previous.chunks();
        if (anciens.isEmpty()) {
            return parseFull(doc, lex, parser, revision);
        }
        int premier = ownerIndex(anciens, site.firstLine());
        int dernier = ownerIndex(anciens, site.lastLine() - site.lineDelta());
        int debutPlage = premier == 0 ? 0 : anciens.get(premier).startLine();
        int finPlage = dernier + 1 < anciens.size()
                ? anciens.get(dernier + 1).startLine() + site.lineDelta() - 1
                : doc.lineCount() - 1;
        List<Chunk> chunks = new ArrayList<>();
        for (int i = 0; i < premier; i++) {
            chunks.add(anciens.get(i));
        }
        if (finPlage >= debutPlage) {
            Chunk ancre = anciens.get(premier);
            chunks.addAll(split(doc, lex, parser, debutPlage, finPlage,
                    premier == 0 ? 0 : ancre.startCol(),
                    profondeurAncre(lex, ancre, premier == 0), premier != 0));
        }
        for (int i = dernier + 1; i < anciens.size(); i++) {
            chunks.add(decale(anciens.get(i), site.lineDelta()));
        }
        return new SyntaxTree(revision, chunks);
    }

    private static Chunk decale(Chunk chunk, int delta) {
        if (delta == 0) {
            return chunk;
        }
        return new Chunk(chunk.startLine() + delta, chunk.startCol(),
                chunk.endLine() + delta, chunk.endCol(), chunk.root());
    }

    /**
     * Découpe une plage de lignes en chunks : matérialisation, marche à
     * profondeur, prédicat du langage, extension arrière des trivia de
     * documentation vers le chunk qui suit.
     */
    private static List<Chunk> split(Document doc, LexDocument lex, LanguageParser parser,
                                        int premiereLigne, int derniereLigne, int colonneAncre,
                                        int baseProfondeur, boolean sauterAncre) {
        int end = Math.min(derniereLigne, doc.lineCount() - 1);
        if (end < premiereLigne) {
            return List.of();
        }
        PasseTokens passe = collecter(lex, premiereLigne, end);
        if (passe.tokens().length == 0) {
            return List.of();
        }
        int debutMarche = sauterAncre
                ? anchorIndex(passe.tokens(), passe.lines(), premiereLigne, colonneAncre)
                : 0;
        return marcher(doc, parser, passe, debutMarche, premiereLigne, baseProfondeur);
    }

    /** Les jetons d'une plage de lignes, avec leur ligne porteuse chacun. */
    private static PasseTokens collecter(LexDocument lex, int premiereLigne, int end) {
        List<Token> collects = new ArrayList<>();
        List<Integer> lines = new ArrayList<>();
        for (int line = premiereLigne; line <= end; line++) {
            for (Token token : lex.line(line).tokens()) {
                collects.add(token);
                lines.add(line);
            }
        }
        Token[] tokens = collects.toArray(new Token[0]);
        int[] lignesTokens = new int[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            lignesTokens[i] = lines.get(i);
        }
        return new PasseTokens(tokens, lignesTokens);
    }

    /** Les jetons d'une plage, avec la ligne de chacun. */
    private static final class PasseTokens {
        private final Token[] tokens;
        private final int[] lines;

        PasseTokens(Token[] tokens, int[] lines) {
            this.tokens = tokens;
            this.lines = lines;
        }

        Token[] tokens() {
            return tokens;
        }

        int[] lines() {
            return lines;
        }
    }

    private static List<Chunk> marcher(Document doc, LanguageParser parser, PasseTokens passe,
                                       int debutMarche, int premiereLigne, int baseProfondeur) {
        TokenSlice tranche = new TokenSlice(doc, passe.tokens(), passe.lines(), debutMarche,
                passe.tokens().length, doc.lineStart(premiereLigne));
        List<Integer> bounds = bounds(tranche, parser, baseProfondeur);
        List<Chunk> chunks = new ArrayList<>(bounds.size());
        for (int b = 0; b < bounds.size(); b++) {
            chunks.add(construireChunk(doc, parser, passe, debutMarche, bounds.get(b),
                    b + 1 < bounds.size() ? bounds.get(b + 1) - 1 : tranche.size() - 1,
                    premiereLigne));
        }
        return chunks;
    }

    /**
     * Les frontières de chunks de la marche : index du premier jeton de
     * chaque chunk (le premier commence à zéro), prédicat du langage à
     * profondeur de découpe, documentation entraînée en arrière.
     */
    private static List<Integer> bounds(TokenSlice tranche, LanguageParser parser,
                                        int baseProfondeur) {
        List<Integer> bounds = new ArrayList<>();
        bounds.add(0);
        int profondeur = baseProfondeur;
        for (int i = 0; i < tranche.size(); i++) {
            if (i > 0 && profondeur <= parser.chunkLevel()
                    && parser.startsChunk(tranche, i, profondeur)) {
                int etendue = etendreTrivia(tranche, i);
                if (etendue > bounds.get(bounds.size() - 1)) {
                    bounds.add(etendue);
                }
            }
            profondeur = avancer(profondeur, tranche.kind(i));
        }
        return bounds;
    }

    /**
     * Pose un chunk de la fenêtre : construit s'il coupe la fenêtre
     * visible, en attente sinon — la géométrie, elle, est toujours celle
     * de la marche.
     */
    private static Chunk place(Document doc, LanguageParser parser, PasseTokens passe,
                               int start, int end,
                               int windowFirst, int windowLast) {
        Token premier = passe.tokens()[start];
        Token dernier = passe.tokens()[end];
        int startLine = passe.lines()[start];
        int endLine = passe.lines()[end];
        if (endLine < windowFirst || startLine > windowLast) {
            return Chunk.enAttente(startLine, premier.start(), endLine, dernier.end());
        }
        return construireChunk(doc, parser, passe, 0, start, end, startLine);
    }

    private static Chunk construireChunk(Document doc, LanguageParser parser, PasseTokens passe,
                                         int debutMarche, int start, int end, int premiereLigne) {
        // l'édition peut vider la tranche de marche de ses jetons (la
        // suppression du chunk ancre) : pas de jeton à lire, pas de chunk
        if (debutMarche + start >= passe.tokens().length) {
            return Chunk.enAttente(premiereLigne, doc.lineStart(premiereLigne),
                    premiereLigne, doc.lineEnd(premiereLigne));
        }
        TokenSlice trancheMarche = new TokenSlice(doc, passe.tokens(), passe.lines(), debutMarche,
                passe.tokens().length, doc.lineStart(premiereLigne));
        int baseChunk = doc.lineStart(premiereLigne) + trancheMarche.start(start);
        TokenSlice trancheChunk = new TokenSlice(doc, passe.tokens(), passe.lines(),
                debutMarche + start, debutMarche + end + 1, baseChunk);
        Token premier = passe.tokens()[debutMarche + start];
        Token dernier = passe.tokens()[debutMarche + end];
        return new Chunk(passe.lines()[debutMarche + start], premier.start(),
                passe.lines()[debutMarche + end], dernier.end(),
                parser.parseChunk(trancheChunk));
    }

    /**
     * Construit le sous-arbre d'un chunk en attente : ses jetons se
     * relisent du lexique entre ses bornes — deux chunks peuvent partager
     * une ligne, seuls les jetons du chunk entrent dans la tranche —
     * l'ancre du chunk porte la base absolue, la géométrie ne bouge pas.
     */
    private static Chunk analyser(Document doc, LanguageParser parser, LexDocument lex,
                                  Chunk enAttente) {
        List<Token> collects = new ArrayList<>();
        List<Integer> lines = new ArrayList<>();
        for (int line = enAttente.startLine(); line <= enAttente.endLine(); line++) {
            for (Token token : lex.line(line).tokens()) {
                boolean apresDebut = line > enAttente.startLine()
                        || token.start() >= enAttente.startCol();
                boolean avantFin = line < enAttente.endLine()
                        || token.end() <= enAttente.endCol();
                if (apresDebut && avantFin) {
                    collects.add(token);
                    lines.add(line);
                }
            }
        }
        if (collects.isEmpty()) {
            return enAttente;
        }
        Token[] tokens = collects.toArray(new Token[0]);
        int[] lignesTokens = new int[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            lignesTokens[i] = lines.get(i);
        }
        int base = doc.lineStart(enAttente.startLine()) + enAttente.startCol();
        TokenSlice tranche = new TokenSlice(doc, tokens, lignesTokens, 0,
                tokens.length, base);
        return new Chunk(enAttente.startLine(), enAttente.startCol(),
                enAttente.endLine(), enAttente.endCol(), parser.parseChunk(tranche));
    }

    private static int avancer(int profondeur, TokenKind sorte) {
        if (sorte == TokenKind.OPEN_BRACKET) {
            return profondeur + 1;
        }
        if (sorte == TokenKind.CLOSE_BRACKET) {
            return profondeur - 1;
        }
        return profondeur;
    }

    /**
     * Étend la frontière en arrière sur la documentation qui précède la
     * déclaration — les commentaires ordinaires ne suivent pas : seule la
     * documentation (DOC) s'attache à ce qui la suit.
     */
    private static int etendreTrivia(TokenSlice tranche, int candidat) {
        int j = candidat;
        while (j > 0) {
            TokenKind precedente = tranche.kind(j - 1);
            if (precedente != TokenKind.DOC) {
                break;
            }
            if (j - 2 >= 0 && tranche.line(j - 1) <= tranche.line(j - 2)) {
                break;
            }
            j--;
        }
        return j;
    }

    private static int anchorIndex(Token[] tokens, int[] lines, int ligneAncre,
                                   int colonneAncre) {
        for (int i = 0; i < tokens.length; i++) {
            if (lines[i] > ligneAncre || tokens[i].start() >= colonneAncre) {
                return i;
            }
        }
        return tokens.length;
    }

    private static int profondeurAncre(LexDocument lex, Chunk ancre,
                                       boolean depuisDebut) {
        int line = depuisDebut ? 0 : ancre.startLine();
        int profondeur = lex.bracketDepthAtLineStart(line);
        if (depuisDebut) {
            return profondeur;
        }
        for (Token token : lex.line(line).tokens()) {
            if (token.start() >= ancre.startCol()) {
                break;
            }
            profondeur = avancer(profondeur, token.kind());
        }
        return profondeur;
    }

    private static int ownerIndex(List<Chunk> chunks, int line) {
        int bas = 0;
        int haut = chunks.size() - 1;
        int candidat = 0;
        while (bas <= haut) {
            int milieu = (bas + haut) >>> 1;
            if (chunks.get(milieu).startLine() <= line) {
                candidat = milieu;
                bas = milieu + 1;
            } else {
                haut = milieu - 1;
            }
        }
        return candidat;
    }
}
