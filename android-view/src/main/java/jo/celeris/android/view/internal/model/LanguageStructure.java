package jo.celeris.android.view.internal.model;

import java.util.List;

import jo.celeris.core.language.model.LanguageConfiguration;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.structure.FoldSpan;
import jo.celeris.core.syntax.structure.IndentationFolds;
import jo.celeris.core.syntax.structure.OutlineEntry;
import jo.celeris.core.syntax.structure.RuleOutline;
import jo.celeris.core.syntax.structure.TreeFolds;
import jo.celeris.core.syntax.structure.TreeOutline;
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
public final class LanguageStructure {

    private LanguageStructure() {
    }

    /**
     * Les régions pliables du document selon son langage.
     *
     * @param language   le support du langage, ou null
     * @param document l'instantané affiché
     * @param analyse  l'analyse courante
     * @return les régions triées, dédoublonnées
     */
    public static List<FoldSpan> folds(@Nullable LanguageSupport language, Document document,
            Analysis analyse) {
        if (language == null) {
            return TreeFolds.regions(document, analyse);
        }
        LanguageConfiguration.Folding pliage = language.configuration().folding();
        if (!pliage.byIndentation()) {
            return TreeFolds.regions(document, analyse);
        }
        return TreeFolds.regions(document, analyse, new IndentationFolds.Settings(
                language.configuration().indentSize(), pliage.offSide()));
    }

    /**
     * Le plan du document selon son langage : les règles du paquet
     * prolongées sur ses régions pliables, l'arbre sinon.
     *
     * @param language   le support du langage, ou null
     * @param document l'instantané affiché
     * @param analyse  l'analyse courante
     * @param folds     les régions pliables du même instantané
     * @return les entrées du plan, dans l'ordre du source
     */
    public static List<OutlineEntry> plan(@Nullable LanguageSupport language, Document document,
            Analysis analyse, List<FoldSpan> folds) {
        if (language == null || language.outlineRules().isEmpty()) {
            return TreeOutline.entries(document, analyse);
        }
        return RuleOutline.entries(document, language.outlineRules(), folds);
    }
}
