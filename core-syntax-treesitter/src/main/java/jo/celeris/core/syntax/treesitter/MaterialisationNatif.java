package jo.celeris.core.syntax.treesitter;

import jo.celeris.core.syntax.LexDocument;
import jo.celeris.core.syntax.NodeCategory;

import jo.celeris.core.syntax.LexedLine;

import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.syntax.SynNode;

import jo.celeris.core.syntax.SyntaxTree;
import jo.celeris.core.syntax.Chunk;
import jo.celeris.core.syntax.Analysis;
import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.text.Document;

/**
 * La matérialisation Java de l'arbre natif aplati (ADR-020) : l'arbre
 * entier se lit UNE fois — les feuilles y deviennent les jetons du
 * lexique (rognés à leur ligne, les blancs soudés au jeton qui les
 * suit, la convention du socle), les nœuds de premier niveau qui
 * traversent les lignes y deviennent les chunks du pli. Tout vit en
 * Java après la passe : l'arbre natif ne survit qu'entre deux éditions
 * de la voie bornée (jamais de natif dans le chemin de rendu — le
 * jalon de l'ADR-004).
 *
 * <p>L'arbre aplati porte déjà des offsets CARACTÈRES du modèle
 * (l'aplatisseur de la liaison divise par deux, ADR-0025) : la table
 * octet→caractère n'a plus lieu — la matérialisation lit direct.</p>
 */
final class MaterialisationNatif {

    /** Installe une requête de surbrillance : chaque entrée lue y
     *  pose sa teinte — le nom du nœud capture la couleur de SA
     *  capture. Le langage l'appelle au chargement de SA requête ;
     *  sans appel, les heuristiques de repli font la couleur (la
     *  requête absente ne casse rien).
     *
     * @param entrees les entrées lues du fichier de requête
     */
    static void installerRequete(List<LecteurRequetes.Entree> entrees) {
        CapturesSurbrillance captures = new CapturesSurbrillance();
        for (LecteurRequetes.Entree entree : entrees) {
            DEPUIS_REQUETES.putIfAbsent(entree.noeud(),
                    captures.teinte(entree.capture()));
        }
    }

    /** La carte des noms de nœuds → teintes, remplie par la requête
     *  de surbrillance du langage à l'installation (chantier D) :
     *  chaque entrée lue de {@code highlights.scm} y pose sa teinte
     *  — le nœud capture la couleur de SA capture, la table de mots
     *  codée en dur est retirée. La carte vit sous moniteur : la
     *  requête s'installe au chargement du langage, la
     *  matérialisation la lit au fil des passes. */
    private static final java.util.concurrent.ConcurrentHashMap<String, TokenKind> DEPUIS_REQUETES =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** Le jeton d'une feuille : sa colonne (caractères) et sa longueur. */
    private record LeafToken(TokenKind sorte, int start, int end) {
    }

    private final Document document;
    /** Le début (caractères) de chaque ligne du document. */
    private final int[] lineStart;

    private MaterialisationNatif(Document document) {
        this.document = document;
        this.lineStart = lineStarts();
    }

    /**
     * L'analyse matérialisée d'un arbre aplati : le lexique du document
     * (jetons par ligne, profondeurs de crochets) et l'arbre des chunks
     * (les nœuds de premier niveau qui traversent les lignes — le pli
     * exact du niveau 2).
     *
     * @param document   l'instantané analysé
     * @param aplatit    l'arbre aplati (six entiers par nœud, préordre)
     * @param symbols   la table des noms de symboles du langage
     * @param revision   la révision de l'instantané
     * @return l'analyse entière, versionnée
     */
    static Analysis of(Document document, int[] aplatit, String[] symbols,
            int revision) {
        MaterialisationNatif matiere = new MaterialisationNatif(document);
        return matiere.materiel(aplatit, symbols, revision);
    }

    /** Les débuts (caractères) de chaque ligne du document. */
    private int[] lineStarts() {
        int nombre = document.lineCount();
        int[] debuts = new int[nombre];
        for (int line = 0; line < nombre; line++) {
            debuts[line] = document.lineStart(line);
        }
        return debuts;
    }

    /** La ligne (indice) d'un caractère donné — recherche dichotomique. */
    private int lineOf(int caractere) {
        int bas = 0;
        int haut = lineStart.length - 1;
        while (bas < haut) {
            int milieu = (bas + haut + 1) >>> 1;
            if (lineStart[milieu] <= caractere) {
                bas = milieu;
            } else {
                haut = milieu - 1;
            }
        }
        return bas;
    }

    /** La matière : le lexique et l'arbre des chunks. */
    private Analysis materiel(int[] aplatit, String[] symbols, int revision) {
        List<LeafToken> leaves = new ArrayList<>();
        List<int[]> blocks = new ArrayList<>();
        parcourir(aplatit, symbols, leaves, blocks);
        LexedLine[] lines = lexicon(leaves);
        int[] profondeurs = profondeurs(lines);
        SyntaxTree arbre = arbre(blocks, revision);
        return new Analysis(revision,
                LexDocument.of(lines, profondeurs, NativeState.PREMIER), arbre);
    }

    /**
     * Le parcours préordre de l'arbre aplati : les FEUILLES se ramassent
     * en jetons, les ENFANTS DE PREMIER NIVEAU qui traversent les lignes
     * se ramassent en blocs — la pile des enfants restants tient la
     * profondeur (un nœud sans enfant n'y descend jamais).
     */
    private void parcourir(int[] aplatit, String[] symbols,
            List<LeafToken> leaves, List<int[]> blocks) {
        int[] restants = new int[64];
        int profondeur = 0;
        for (int i = 0; i < aplatit.length; i += 6) {
            int enfants = aplatit[i + 5];
            if (profondeur > 0) {
                restants[profondeur - 1]--;
            }
            int symbole = aplatit[i];
            int start = aplatit[i + 1];
            int end = aplatit[i + 2];
            if (profondeur == 1 && end > start) {
                blocks.add(new int[] {start, end, aplatit[i + 3]});
            }
            if (enfants == 0) {
                if (end > start) {
                    leaves.add(new LeafToken(kindOf(symbols[symbole]),
                            start, end));
                }
            } else {
                if (profondeur >= restants.length) {
                    restants = java.util.Arrays.copyOf(restants,
                            restants.length * 2);
                }
                restants[profondeur] = enfants;
                profondeur++;
            }
            while (profondeur > 1 && restants[profondeur - 1] == 0) {
                profondeur--;
            }
        }
    }

    /** Le lexique du document : les jetons rognés à leur ligne, les
     *  feuilles CONTIGUËS de même sorte soudées en un jeton (les
     *  guillemets d'une chaîne, le contenu du commentaire — la
     *  grammaire les découpe, le lexique du socle les soude), les
     *  blancs APPARTIENNENT au jeton qui les suit — la convention du
     *  socle, sauf le blanc de TÊTE de ligne, qui n'est à personne. */
    private LexedLine[] lexicon(List<LeafToken> leaves) {
        souder(leaves);
        int nombre = document.lineCount();
        LexedLine[] lines = new LexedLine[nombre];
        List<Token> panier = new ArrayList<>();
        int index = 0;
        for (int line = 0; line < nombre; line++) {
            panier.clear();
            int start = lineStart[line];
            int end = document.lineEnd(line);
            int curseur = -1;
            while (index < leaves.size() && leaves.get(index).start() < end) {
                LeafToken token = leaves.get(index);
                if (token.end() <= start) {
                    index++;
                    continue;
                }
                int finRogne = Math.min(token.end(), end);
                int debutRogne = curseur < 0
                        ? Math.max(token.start(), start) : curseur;
                if (finRogne > debutRogne) {
                    panier.add(new Token(token.sorte(),
                            debutRogne - start, finRogne - start));
                }
                if (token.end() > end) {
                    break;
                }
                index++;
                curseur = token.end();
            }
            lines[line] = new LexedLine(panier.toArray(new Token[0]),
                    NativeState.PREMIER);
        }
        return lines;
    }

    /** Soude les feuilles contiguës de même sorte — la chaîne de la
     *  grammaire redevient le jeton du socle ; les crochets et la
     *  ponctuation ne se soudent JAMAIS (deux fermantes voisines
     *  restent deux jetons, l'appariement le exige). */
    private static void souder(List<LeafToken> leaves) {
        int ecrit = 0;
        for (LeafToken token : leaves) {
            if (ecrit > 0) {
                LeafToken precedent = leaves.get(ecrit - 1);
                if (souirable(precedent.sorte())
                        && precedent.sorte() == token.sorte()
                        && precedent.end() == token.start()) {
                    leaves.set(ecrit - 1, new LeafToken(precedent.sorte(),
                            precedent.start(), token.end()));
                    continue;
                }
            }
            leaves.set(ecrit, token);
            ecrit++;
        }
        if (ecrit < leaves.size()) {
            leaves.subList(ecrit, leaves.size()).clear();
        }
    }

    /** Vrai quand la sorte se soude : la chaîne et le commentaire —
     *  les morceaux d'une même construction multi-feuilles. */
    private static boolean souirable(TokenKind sorte) {
        return sorte == TokenKind.STRING || sorte == TokenKind.COMMENT;
    }

    /** Les profondeurs de crochets au début de chaque ligne. */
    private int[] profondeurs(LexedLine[] lines) {
        int[] profondeurs = new int[lines.length];
        int profondeur = 0;
        for (int line = 0; line < lines.length; line++) {
            profondeurs[line] = profondeur;
            for (Token token : lines[line].tokens()) {
                if (token.kind() == TokenKind.OPEN_BRACKET) {
                    profondeur++;
                } else if (token.kind() == TokenKind.CLOSE_BRACKET
                        && profondeur > 0) {
                    profondeur--;
                }
            }
        }
        return profondeurs;
    }

    /** L'arbre des chunks : chaque bloc de premier niveau porte un nœud
     *  BLOC couvrant son étendue — le pli exact du niveau 2. */
    private SyntaxTree arbre(List<int[]> blocks, int revision) {
        List<Chunk> chunks = new ArrayList<>();
        for (int[] bloc : blocks) {
            int startLine = bloc[2];
            int endLine = lineOf(bloc[1] - 1);
            if (endLine <= startLine) {
                continue;
            }
            int start = document.lineStart(startLine);
            SynNode racine = SynNode.of(NativeNodeKind.NODE, NodeCategory.BLOCK,
                    0, bloc[1] - start, new SynNode[0], null, false);
            chunks.add(new Chunk(startLine, bloc[0] - start, endLine,
                    finDeColonne(bloc[1], endLine), racine));
        }
        return SyntaxTree.of(revision, List.copyOf(chunks));
    }

    /** La colonne (caractères) d'un début de ligne. */
    private int finDeColonne(int caractere, int line) {
        return caractere - document.lineStart(line);
    }

    /** Les opérateurs d'un seul caractère — la table qui les dit. */
    private static final String OPERATEURS = "+-*/%=<>!~&|^?";

    /** La sorte de jeton d'un symbole de grammaire. */
    private static TokenKind kindOf(String nom) {
        if (nom.length() == 1) {
            return kindOfChar(nom.charAt(0));
        }
        return kindOfName(nom);
    }

    /** La sorte d'un symbole d'un caractère : crochet, chaîne,
     *  opérateur de la table, ponctuation sinon. */
    private static TokenKind kindOfChar(char caractere) {
        return switch (caractere) {
            case '{', '[', '(' -> TokenKind.OPEN_BRACKET;
            case '}', ']', ')' -> TokenKind.CLOSE_BRACKET;
            case '"', '\'', '`' -> TokenKind.STRING;
            default -> OPERATEURS.indexOf(caractere) >= 0
                    ? TokenKind.OPERATOR : TokenKind.PUNCTUATION;
        };
    }

    /** La sorte d'un symbole nommé : les captures lues de la requête
     *  du langage (le NOM du nœud y porte sa capture — @keyword,
     *  @comment…), les heuristiques de repli pour les noms que la
     *  requête ne couvre pas — la table de mots codée en dur est
     *  RETIRÉE (chantier D : la couleur vient de la requête). */
    private static TokenKind kindOfName(String nom) {
        TokenKind requete = DEPUIS_REQUETES.get(nom);
        if (requete != null) {
            return requete;
        }
        if (nom.contains("comment")) {
            return TokenKind.COMMENT;
        }
        if (nom.contains("string") || nom.contains("regex")
                || nom.contains("escape")) {
            return TokenKind.STRING;
        }
        if (nom.contains("integer") || nom.contains("float")
                || nom.equals("number")) {
            return TokenKind.NUMBER;
        }
        if (nom.contains("identifier")) {
            return TokenKind.IDENTIFIER;
        }
        if (nom.contains("decorator") || nom.contains("attribute")) {
            return TokenKind.ANNOTATION;
        }
        return TokenKind.IDENTIFIER;
    }
}
