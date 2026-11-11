package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.NodeCategory;
import jo.celeris.core.syntax.OutlineEntry;
import jo.celeris.core.syntax.TreeOutline;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.TextEdit;

/**
 * La suppression sûre (B-23) : la déclaration désignée par la sélection —
 * méthode, type ou champ — ne disparaît que si le document ne l'utilise
 * nulle part ailleurs : les usages se lisent avant de trancher, le lu
 * décide, jamais le deviné. Le verdict rend les usages trouvés — le
 * client les montre avant de renoncer, à la manière de l'étalon — ou le
 * plan d'effacement quand la voie est libre. L'absence de désignation se
 * signale par un verdict nul, jamais par un plan faux : le faux positif
 * refuse (l'ombre homonyme d'une autre portée compte comme usage), le
 * faux négatif n'existe pas. Le champ qui partage sa ligne ou sa
 * déclaration refuse — l'effacement ne saurait emporter le voisin.
 */
public final class SafeDelete {

    private SafeDelete() {
    }

    /** Un usage qui barre la suppression : l'offset du nom lu, le nom. */
    public record Usage(int offset, String text) {
    }

    /**
     * Le verdict : les usages qui barrent l'effacement, ou le plan quand
     * la voie est libre — jamais les deux, jamais aucun.
     *
     * @param usages les usages barants, vides quand la voie est libre
     * @param edits  le plan d'effacement, vide quand un usage barre
     */
    public record Verdict(List<Usage> usages, List<TextEdit> edits) {

        /** Constructeur canonique : listes figées, un seul côté porteur. */
        public Verdict {
            Objects.requireNonNull(usages, "usages");
            Objects.requireNonNull(edits, "edits");
            if (usages.isEmpty() == edits.isEmpty()) {
                throw new IllegalArgumentException(
                        "exactly one of usages and edits must be present");
            }
            usages = List.copyOf(usages);
            edits = List.copyOf(edits);
        }
    }

    /**
     * Supprime la déclaration désignée par la sélection si le document ne
     * l'utilise nulle part ailleurs.
     *
     * @param doc     le document, jamais muté
     * @param analyse l'analyse courante du document
     * @param start   début de la sélection, dans la déclaration
     * @param end     fin de la sélection (exclusive), dans la déclaration
     * @return le verdict — usages barants ou plan d'effacement — ou null
     *         hors déclaration, ou sur un champ partagé
     * @throws IllegalArgumentException si la sélection est inversée ou
     *         hors bornes
     */
    public static @Nullable Verdict delete(Document doc, Analysis analyse,
            int start, int end) {
        if (start < 0 || end > doc.length() || start > end) {
            throw new IllegalArgumentException(
                    "selection [" + start + ", " + end + ") out of (0, " + doc.length() + ")");
        }
        OutlineEntry designated = designation(doc, analyse, start, end);
        if (designated == null
                || (designated.category() == NodeCategory.FIELD
                        && !fieldOwnsLine(doc, designated))) {
            return null;
        }
        List<Usage> usages = usages(doc, analyse, designated);
        if (!usages.isEmpty()) {
            return new Verdict(usages, List.of());
        }
        return new Verdict(List.of(), List.of(removal(doc, designated)));
    }

    /**
     * La déclaration la plus intime qui enclot la sélection — méthode,
     * type ou champ.
     */
    static @Nullable OutlineEntry designation(Document doc, Analysis analyse,
            int start, int end) {
        OutlineEntry best = null;
        for (OutlineEntry entry : TreeOutline.entries(doc, analyse)) {
            if (entry.category() != NodeCategory.FUNCTION
                    && entry.category() != NodeCategory.TYPE
                    && entry.category() != NodeCategory.FIELD) {
                continue;
            }
            if (entry.startOffset() <= start && end <= entry.endOffset() && moreIntimate(entry, best)) {
                best = entry;
            }
        }
        return best;
    }

    /** Vrai quand l'entrée enclot plus étroitement que la meilleure courante. */
    private static boolean moreIntimate(OutlineEntry entry, @Nullable OutlineEntry best) {
        if (best == null) {
            return true;
        }
        if (entry.startOffset() != best.startOffset()) {
            return entry.startOffset() > best.startOffset();
        }
        return entry.endOffset() < best.endOffset();
    }

    /**
     * Les usages de la déclaration hors son territoire : les appels et
     * leurs suspects pour une méthode, les occurrences du nom pour un
     * type ou un champ (l'appel homonyme d'une méthode déclarée ne lit
     * pas le champ).
     */
    private static List<Usage> usages(Document doc, Analysis analyse, OutlineEntry entry) {
        if (entry.category() == NodeCategory.FUNCTION) {
            return methodUsages(doc, analyse, entry);
        }
        return nameUsages(doc, analyse, entry);
    }

    /** Les usages d'une méthode : ses appels, et les occurrences suspectes. */
    private static List<Usage> methodUsages(Document doc, Analysis analyse,
            OutlineEntry entry) {
        FlowAnalysis.CallSites sites = FlowAnalysis.calls(doc, analyse.lex(),
                entry.name(), entry.startOffset(), entry.endOffset(),
                SignatureChange.isConstructor(doc, analyse, entry));
        List<Usage> usages = new ArrayList<>();
        for (FlowAnalysis.Call call : sites.calls()) {
            usages.add(new Usage(call.nameOffset(), entry.name()));
        }
        for (int suspect : sites.suspects()) {
            usages.add(new Usage(suspect, entry.name()));
        }
        return usages;
    }

    /** Les usages d'un type ou d'un champ : les occurrences du nom hors territoire. */
    private static List<Usage> nameUsages(Document doc, Analysis analyse,
            OutlineEntry entry) {
        List<Usage> usages = new ArrayList<>();
        List<FlowAnalysis.AbsoluteToken> tokens =
                FlowAnalysis.tokens(doc, analyse.lex(), 0, doc.length());
        boolean callNamed = entry.category() == NodeCategory.FIELD
                && methodNamed(doc, analyse, entry.name());
        for (int i = 0; i < tokens.size(); i++) {
            FlowAnalysis.AbsoluteToken token = tokens.get(i);
            if (!FlowAnalysis.isName(token.kind()) || !token.text().equals(entry.name())
                    || within(token.offset(), entry)) {
                continue;
            }
            if (callNamed && i + 1 < tokens.size()
                    && tokens.get(i + 1).text().equals("(")) {
                continue;
            }
            usages.add(new Usage(token.offset(), entry.name()));
        }
        return usages;
    }

    /**
     * L'effacement de la déclaration : ses lignes entières quand elle les
     * possède — la ligne blanche qui suit part avec elle, sinon celle qui
     * précède — son texte seul sinon.
     */
    static TextEdit removal(Document doc, OutlineEntry entry) {
        int firstLine = doc.lineForOffset(entry.startOffset());
        int lastLine = doc.lineForOffset(
                Math.max(entry.startOffset(), entry.endOffset() - 1));
        int from = doc.lineStart(firstLine);
        int to = doc.lineEnd(lastLine);
        int declarationEnd = Math.min(entry.endOffset(), to);
        if (doc.subText(from, entry.startOffset()).isBlank()
                && doc.subText(declarationEnd, to).isBlank()) {
            if (to < doc.length()) {
                to++;
            }
            if (lastLine + 1 < doc.lineCount()
                    && doc.lineText(lastLine + 1).isBlank()) {
                to = neighborLineEnd(doc, lastLine + 1);
            } else if (firstLine > 0 && doc.lineText(firstLine - 1).isBlank()) {
                from = doc.lineStart(firstLine - 1);
            }
            return new TextEdit(from, doc.subText(from, to), "");
        }
        return new TextEdit(entry.startOffset(),
                doc.subText(entry.startOffset(), entry.endOffset()), "");
    }

    /** La fin de la ligne voisine, sa fin de ligne comprise. */
    private static int neighborLineEnd(Document doc, int line) {
        int end = doc.lineEnd(line);
        return end < doc.length() ? end + 1 : end;
    }

    /** Vrai quand l'offset vit dans le territoire de la déclaration. */
    private static boolean within(int offset, OutlineEntry entry) {
        return offset >= entry.startOffset() && offset < entry.endOffset();
    }

    /** Vrai quand le document déclare une méthode du nom donné. */
    private static boolean methodNamed(Document doc, Analysis analyse, String name) {
        for (OutlineEntry entry : TreeOutline.entries(doc, analyse)) {
            if (entry.category() == NodeCategory.FUNCTION && entry.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vrai quand la ligne du champ ne porte que lui : une seule
     * déclaration de niveau zéro — la virgule de niveau zéro trahit le
     * frère, le second point-virgule trahit le voisin.
     */
    private static boolean fieldOwnsLine(Document doc, OutlineEntry entry) {
        String text = doc.lineText(doc.lineForOffset(entry.startOffset()));
        int depth = 0;
        int semicolons = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(' || c == '[' || c == '{' || c == '<') {
                depth++;
            } else if (c == ')' || c == ']' || c == '}' || c == '>') {
                depth--;
            } else if (depth == 0 && c == ',') {
                return false;
            } else if (depth == 0 && c == ';') {
                semicolons++;
            }
        }
        return semicolons == 1;
    }
}
