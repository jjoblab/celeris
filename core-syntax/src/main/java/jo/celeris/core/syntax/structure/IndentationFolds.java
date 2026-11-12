package jo.celeris.core.syntax.structure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jo.celeris.core.syntax.LexDocument;
import jo.celeris.core.syntax.Token;
import jo.celeris.core.syntax.TokenKind;
import jo.celeris.core.text.Document;

/**
 * Les plis par indentation (M7.6, H-08 — analyse structurelle niveau 1,
 * ADR-016) : les langages à décalage — Python, YAML — se replient sans
 * un arbre, par la seule géométrie de leurs lignes. Une ligne moins
 * indentée que l'ouverture rouvre le pli ({@code offSide}, le réglage
 * Python de la référence) ; sans lui, une ligne de même décalage
 * prolonge le pli ouvert — les blocs frères se fondent en une région.
 * Les lignes vides et les lignes de commentaire seul ne clôturent
 * JAMAIS : elles appartiennent au pli qui les porte, la frappe d'un
 * blanc dans un corps ne replie rien.
 *
 * <p>Le réglage de l'indentation (espaces, tabulations au taquet
 * donné) vit dans la configuration du langage — le moteur ne décide
 * rien, la donnée parle. Les régions rendues couvrent au moins deux
 * lignes : l'en-tête visible et les lignes repliées.</p>
 */
public final class IndentationFolds {

    /**
     * Le réglage des plis par indentation d'un langage.
     *
     * @param indentSize la taille du taquet de tabulation
     * @param offSide    vrai quand une ligne moins indentée que
     *                   l'ouverture rouvre le pli
     */
    public record Settings(int indentSize, boolean offSide) {

        /** Constructeur canonique : le taquet est positif. */
        public Settings {
            if (indentSize <= 0) {
                throw new IllegalArgumentException(
                        "indentSize must be positive: " + indentSize);
            }
        }
    }

    private IndentationFolds() {
    }

    /**
     * Les régions de pli par indentation d'un document lexicographié.
     *
     * @param doc      le document porteur
     * @param lex      le lexique du document (lignes de commentaire)
     * @param settings le réglage des plis, jamais null
     * @return les régions triées par ligne de début, chacune de deux lignes au moins
     */
    public static List<FoldSpan> regions(Document doc, LexDocument lex,
            Settings settings) {
        int lineCount = doc.lineCount();
        List<Opening> opened = new ArrayList<>();
        List<FoldSpan> regions = new ArrayList<>();
        for (int line = 0; line < lineCount; line++) {
            if (triviaOnly(lex, line)) {
                continue;
            }
            int indent = indentation(doc.lineText(line), settings.indentSize());
            while (!opened.isEmpty() && indent < opened.get(opened.size() - 1).indent) {
                close(opened.remove(opened.size() - 1), line - 1, doc, regions);
            }
            if (settings.offSide() && !opened.isEmpty()
                    && indent == opened.get(opened.size() - 1).indent) {
                close(opened.remove(opened.size() - 1), line - 1, doc, regions);
            }
            if (opened.isEmpty() || indent != opened.get(opened.size() - 1).indent
                    || settings.offSide()) {
                if (!opened.isEmpty() && indent > opened.get(opened.size() - 1).indent) {
                    opened.get(opened.size() - 1).deeperChild = true;
                }
                opened.add(new Opening(indent, line));
            }
        }
        int last = lastNonBlank(doc);
        while (!opened.isEmpty()) {
            close(opened.remove(opened.size() - 1), last, doc, regions);
        }
        regions.sort(Comparator.comparingInt(FoldSpan::startLine)
                .thenComparingInt(FoldSpan::endLine));
        return List.copyOf(regions);
    }

    /** Une ouverture de pli en attente : son décalage, sa ligne, son contenu plus profond. */
    private static final class Opening {

        private final int indent;

        private final int line;

        private boolean deeperChild;

        private Opening(int indent, int line) {
            this.indent = indent;
            this.line = line;
        }
    }

    /**
     * Ferme une ouverture : la région ne vit que si une ligne plus
     * indentée s'y est nichée (sans elle, rien à replier) et que ses
     * blancs de queue se rognent — deux lignes au moins.
     */
    private static void close(Opening opening, int end, Document doc,
            List<FoldSpan> regions) {
        if (!opening.deeperChild) {
            return;
        }
        while (end > opening.line && doc.lineText(end).isBlank()) {
            end--;
        }
        if (end > opening.line) {
            regions.add(new FoldSpan(opening.line, end));
        }
    }

    /** Vrai quand la ligne ne porte que du trivia : vide, blanche ou commentaire seul. */
    private static boolean triviaOnly(LexDocument lex, int line) {
        if (line >= lex.lineCount()) {
            return true;
        }
        Token[] tokens = lex.line(line).tokens();
        if (tokens.length == 0) {
            return true;
        }
        for (Token token : tokens) {
            if (token.kind() != TokenKind.COMMENT && token.kind() != TokenKind.DOC) {
                return false;
            }
        }
        return true;
    }

    /** La largeur d'indentation d'une ligne : l'espace compte un, la tabulation au taquet. */
    private static int indentation(String text, int tabSize) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ' ') {
                width++;
            } else if (c == '\t') {
                width += tabSize - (width % tabSize);
            } else {
                break;
            }
        }
        return width;
    }

    /** La dernière ligne non blanche du document — les blancs de queue ne se replient pas. */
    private static int lastNonBlank(Document doc) {
        int line = doc.lineCount() - 1;
        while (line > 0 && doc.lineText(line).isBlank()) {
            line--;
        }
        return line;
    }

}
