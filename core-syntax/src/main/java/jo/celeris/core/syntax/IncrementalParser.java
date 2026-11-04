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
                decouper(doc, lex, parser, 0, doc.lineCount() - 1, 0, 0, false));
    }

    /**
     * Analyse incrémentale après une édition : seuls les chunks
     * propriétaires des lignes touchées se ré-analysent.
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
        int premier = indiceProprietaire(anciens, site.firstLine());
        int dernier = indiceProprietaire(anciens, site.lastLine() - site.lineDelta());
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
            chunks.addAll(decouper(doc, lex, parser, debutPlage, finPlage,
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
    private static List<Chunk> decouper(Document doc, LexDocument lex, LanguageParser parser,
                                        int premiereLigne, int derniereLigne, int colonneAncre,
                                        int baseProfondeur, boolean sauterAncre) {
        int fin = Math.min(derniereLigne, doc.lineCount() - 1);
        if (fin < premiereLigne) {
            return List.of();
        }
        List<Token> collects = new ArrayList<>();
        List<Integer> lignes = new ArrayList<>();
        for (int ligne = premiereLigne; ligne <= fin; ligne++) {
            for (Token jeton : lex.line(ligne).tokens()) {
                collects.add(jeton);
                lignes.add(ligne);
            }
        }
        Token[] jetons = collects.toArray(new Token[0]);
        int[] lignesJetons = new int[lignes.size()];
        for (int i = 0; i < lignes.size(); i++) {
            lignesJetons[i] = lignes.get(i);
        }
        if (jetons.length == 0) {
            return List.of();
        }
        int debutMarche = sauterAncre
                ? indiceAncre(jetons, lignesJetons, premiereLigne, colonneAncre)
                : 0;
        return marcher(doc, parser, jetons, lignesJetons, debutMarche,
                premiereLigne, baseProfondeur);
    }

    private static List<Chunk> marcher(Document doc, LanguageParser parser, Token[] jetons,
                                       int[] lignesJetons, int debutMarche, int premiereLigne,
                                       int baseProfondeur) {
        TokenSlice tranche = new TokenSlice(doc, jetons, lignesJetons, debutMarche,
                jetons.length, doc.lineStart(premiereLigne));
        List<Integer> bornes = new ArrayList<>();
        bornes.add(0);
        int profondeur = baseProfondeur;
        for (int i = 0; i < tranche.size(); i++) {
            if (i > 0 && profondeur <= parser.chunkLevel()
                    && parser.startsChunk(tranche, i, profondeur)) {
                int etendue = etendreTrivia(tranche, i);
                if (etendue > bornes.get(bornes.size() - 1)) {
                    bornes.add(etendue);
                }
            }
            profondeur = avancer(profondeur, tranche.kind(i));
        }
        List<Chunk> chunks = new ArrayList<>();
        for (int b = 0; b < bornes.size(); b++) {
            chunks.add(construireChunk(doc, parser, jetons, lignesJetons, debutMarche,
                    bornes.get(b), b + 1 < bornes.size() ? bornes.get(b + 1) - 1
                            : tranche.size() - 1,
                    premiereLigne));
        }
        return chunks;
    }

    private static Chunk construireChunk(Document doc, LanguageParser parser, Token[] jetons,
                                         int[] lignesJetons, int debutMarche, int debut,
                                         int fin, int premiereLigne) {
        TokenSlice trancheMarche = new TokenSlice(doc, jetons, lignesJetons, debutMarche,
                jetons.length, doc.lineStart(premiereLigne));
        int baseChunk = doc.lineStart(premiereLigne) + trancheMarche.start(debut);
        TokenSlice trancheChunk = new TokenSlice(doc, jetons, lignesJetons,
                debutMarche + debut, debutMarche + fin + 1, baseChunk);
        Token premier = jetons[debutMarche + debut];
        Token dernier = jetons[debutMarche + fin];
        return new Chunk(lignesJetons[debutMarche + debut], premier.start(),
                lignesJetons[debutMarche + fin], dernier.end(),
                parser.parseChunk(trancheChunk));
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

    private static int etendreTrivia(TokenSlice tranche, int candidat) {
        int j = candidat;
        while (j > 0) {
            TokenKind precedente = tranche.kind(j - 1);
            boolean trivia = precedente == TokenKind.COMMENT || precedente == TokenKind.DOC;
            if (!trivia) {
                break;
            }
            if (j - 2 >= 0 && tranche.line(j - 1) <= tranche.line(j - 2)) {
                break;
            }
            j--;
        }
        return j;
    }

    private static int indiceAncre(Token[] jetons, int[] lignes, int ligneAncre,
                                   int colonneAncre) {
        for (int i = 0; i < jetons.length; i++) {
            if (lignes[i] > ligneAncre || jetons[i].start() >= colonneAncre) {
                return i;
            }
        }
        return jetons.length;
    }

    private static int profondeurAncre(LexDocument lex, Chunk ancre,
                                       boolean depuisDebut) {
        int ligne = depuisDebut ? 0 : ancre.startLine();
        int profondeur = lex.bracketDepthAtLineStart(ligne);
        if (depuisDebut) {
            return profondeur;
        }
        for (Token jeton : lex.line(ligne).tokens()) {
            if (jeton.start() >= ancre.startCol()) {
                break;
            }
            profondeur = avancer(profondeur, jeton.kind());
        }
        return profondeur;
    }

    private static int indiceProprietaire(List<Chunk> chunks, int ligne) {
        int bas = 0;
        int haut = chunks.size() - 1;
        int candidat = 0;
        while (bas <= haut) {
            int milieu = (bas + haut) >>> 1;
            if (chunks.get(milieu).startLine() <= ligne) {
                candidat = milieu;
                bas = milieu + 1;
            } else {
                haut = milieu - 1;
            }
        }
        return candidat;
    }
}
