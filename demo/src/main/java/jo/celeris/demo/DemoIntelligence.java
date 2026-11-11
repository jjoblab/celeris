package jo.celeris.demo;

import java.util.List;
import java.util.function.Supplier;

import jo.celeris.android.view.IntelligenceHub;
import jo.celeris.core.intelligence.CompletionMots;
import jo.celeris.core.intelligence.EditPlan;
import jo.celeris.core.intelligence.FrappeJava;
import jo.celeris.core.intelligence.IndentStyle;
import jo.celeris.core.intelligence.SmartTyping;
import jo.celeris.core.language.LanguageSupport;
import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.syntax.JavaSyntax;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;

import org.jspecify.annotations.Nullable;

/**
 * Le fournisseur d'intelligence de la démonstration (M7.5, §8) : la
 * complétion sert les mots du CODE — l'analyseur décide, jamais les
 * chaînes ni les commentaires (B-03) —, le balayage du coach tient le
 * repli tant que la publication n'est pas arrivée. L'Entrée d'un
 * fichier Java porte les gestes dédiés du chantier C8 : le découpage
 * des chaînes et le cran des labels de cas — les autres fichiers
 * suivent les moteurs du dépôt, la démonstration ne détourne que ce
 * qu'elle possède.
 */
final class DemoIntelligence implements IntelligenceHub.Provider {

    /** La langue courante du fichier ouvert — le gate des gestes dédiés. */
    private final Supplier<@Nullable LanguageSupport> langueCourante;

    /**
     * Construit le fournisseur de la démonstration.
     *
     * @param langueCourante la langue du fichier ouvert, null sans langue
     */
    DemoIntelligence(Supplier<@Nullable LanguageSupport> langueCourante) {
        this.langueCourante = langueCourante;
    }

    /**
     * Les candidats de complétion : la voie analytique d'abord — les
     * mots du code, pas ceux des chaînes —, le balayage textuel du
     * coach en repli, le temps que la publication rattrape la frappe.
     */
    @Override
    public List<String> completionCandidates(Document doc,
            @Nullable Analysis analysis, int caret, List<String> keywords) {
        if (analysis != null) {
            List<String> candidats = CompletionMots.completer(doc, analysis,
                    caret, keywords);
            if (!candidats.isEmpty()) {
                return candidats;
            }
        }
        return PopupCoach.completionFor(doc, caret);
    }

    /**
     * L'Entrée d'un fichier Java : les gestes dédiés (C8) — le
     * découpage des chaînes, le cran des labels de cas — posés sur le
     * moteur générique, plage par plage. Les autres langues suivent le
     * moteur générique tel quel : les citations d'un attribut XML ne
     * se découpent jamais.
     */
    @Override
    public EditPlan enter(Document doc, @Nullable Analysis analysis,
            MultiSelection selection, IndentStyle style) {
        LanguageSupport langue = langueCourante.get();
        if (langue != null && langue.syntax() == JavaSyntax.INSTANCE) {
            return FrappeJava.enter(doc, analysis, selection, style);
        }
        return SmartTyping.enter(doc, analysis, selection, style);
    }
}
