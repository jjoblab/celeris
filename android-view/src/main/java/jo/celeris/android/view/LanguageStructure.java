package jo.celeris.android.view;

import java.util.List;

import jo.celeris.core.language.LanguageConfiguration;
import jo.celeris.core.language.LanguageSupport;
import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.FoldSpan;
import jo.celeris.core.syntax.IndentationFolds;
import jo.celeris.core.syntax.OutlineEntry;
import jo.celeris.core.syntax.RuleOutline;
import jo.celeris.core.syntax.TreeFolds;
import jo.celeris.core.syntax.TreeOutline;
import jo.celeris.core.text.Document;

import org.jspecify.annotations.Nullable;

/**
 * Les plis et le plan d'un langage au niveau 1 de l'analyse
 * structurelle (M7.6, H-08, ADR-016) : les régions pliables suivent la
 * configuration du paquet — l'arbre et les commentaires pour tous,
 * l'indentation en plus quand elle se déclare (Python, YAML) —, le plan
 * suit ses règles quand il en porte ({@code outline.json}), l'arbre de
 * l'analyse sinon. Un langage sans analyseur dédié se replie et se
 * structure comme les autres : la vue n'y change rien.
 */
final class LanguageStructure {

    private LanguageStructure() {
    }

    /**
     * Les régions pliables du document selon son langage.
     *
     * @param langue   le support du langage, ou null
     * @param document l'instantané affiché
     * @param analyse  l'analyse courante
     * @return les régions triées, dédoublonnées
     */
    static List<FoldSpan> plis(@Nullable LanguageSupport langue, Document document,
            Analysis analyse) {
        if (langue == null) {
            return TreeFolds.regions(document, analyse);
        }
        LanguageConfiguration.Folding pliage = langue.configuration().folding();
        if (!pliage.byIndentation()) {
            return TreeFolds.regions(document, analyse);
        }
        return TreeFolds.regions(document, analyse, new IndentationFolds.Settings(
                langue.configuration().indentSize(), pliage.offSide()));
    }

    /**
     * Le plan du document selon son langage : les règles du paquet
     * prolongées sur ses régions pliables, l'arbre sinon.
     *
     * @param langue   le support du langage, ou null
     * @param document l'instantané affiché
     * @param analyse  l'analyse courante
     * @param plis     les régions pliables du même instantané
     * @return les entrées du plan, dans l'ordre du source
     */
    static List<OutlineEntry> plan(@Nullable LanguageSupport langue, Document document,
            Analysis analyse, List<FoldSpan> plis) {
        if (langue == null || langue.outlineRules().isEmpty()) {
            return TreeOutline.entries(document, analyse);
        }
        return RuleOutline.entries(document, langue.outlineRules(), plis);
    }
}
