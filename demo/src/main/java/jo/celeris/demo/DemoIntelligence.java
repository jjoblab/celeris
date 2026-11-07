package jo.celeris.demo;

import java.util.List;

import jo.celeris.android.view.IntelligenceHub;
import jo.celeris.core.intelligence.CompletionMots;
import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.text.Document;

import org.jspecify.annotations.Nullable;

/**
 * Le fournisseur d'intelligence de la démonstration (M7.5, §8) : la
 * complétion sert les mots du CODE — l'analyseur décide, jamais les
 * chaînes ni les commentaires (B-03) —, le balayage du coach tient le
 * repli tant que la publication n'est pas arrivée. Le reste suit les
 * moteurs du dépôt : frappe intelligente, Entrée indentée, collage
 * réindenté — la démonstration ne détourne que ce qu'elle possède.
 */
final class DemoIntelligence implements IntelligenceHub.Provider {

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
}
