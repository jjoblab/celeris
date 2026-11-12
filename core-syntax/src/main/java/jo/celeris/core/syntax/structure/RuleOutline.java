package jo.celeris.core.syntax.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import jo.celeris.core.text.Document;

/**
 * Le plan par règles (M7.6, H-08 — analyse structurelle niveau 1) :
 * les langages sans analyseur dédié — Python, YAML, SQL — déclarent
 * leur plan en données ({@code outline.json} du paquet), chaque règle
 * nomme une ligne qui parle. La première règle qui matche gagne,
 * l'ordre du paquet fait la précédence ; l'entrée couvre sa ligne
 * d'en-tête, prolongée jusqu'au bout de sa région de pli quand elle en
 * ouvre une — le fil d'Ariane et la profondeur du plan s'y retrouvent.
 *
 * <p>Le plan reste plat et ordonné du source, comme celui de l'arbre
 * ({@link TreeOutline}) : la profondeur d'affichage se déduit de
 * l'imbrication des bornes, jamais d'un compteur.</p>
 */
public final class RuleOutline {

    private RuleOutline() {
    }

    /**
     * Les entrées du plan d'un document par ses règles déclarées.
     *
     * @param doc     le document porteur
     * @param rules   les règles du paquet, dans leur ordre de précédence
     * @param regions les régions de pli du document, pour prolonger les entrées
     * @return les entrées dans l'ordre du source, jamais null
     */
    public static List<OutlineEntry> entries(Document doc, List<OutlineRule> rules,
            List<FoldSpan> regions) {
        List<OutlineEntry> entries = new ArrayList<>();
        if (rules.isEmpty()) {
            return List.of();
        }
        for (int line = 0; line < doc.lineCount(); line++) {
            String text = doc.lineText(line);
            for (OutlineRule rule : rules) {
                Matcher matcher = rule.pattern().matcher(text);
                if (!matcher.find()) {
                    continue;
                }
                String name = matcher.group(rule.nameGroup());
                if (name == null || name.isBlank()) {
                    break;
                }
                entries.add(entry(doc, rule, name, line, regions));
                break;
            }
        }
        return List.copyOf(entries);
    }

    /** L'entrée d'une ligne déclarée : sa ligne, prolongée à sa région de pli. */
    private static OutlineEntry entry(Document doc, OutlineRule rule, String name,
            int line, List<FoldSpan> regions) {
        int endLine = line;
        for (FoldSpan region : regions) {
            if (region.startLine() == line && region.endLine() > endLine) {
                endLine = region.endLine();
            }
        }
        int start = doc.lineStart(line);
        int end = doc.lineEnd(endLine);
        return new OutlineEntry(rule.category(), name, start, end, line, endLine);
    }
}
