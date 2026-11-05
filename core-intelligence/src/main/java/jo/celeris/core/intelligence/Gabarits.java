package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextEdit;
import jo.celeris.core.text.Transaction;

/**
 * Le moteur des gabarits dynamiques (M5, A-22) : l'insertion — le rendu
 * remplace la plage, le premier trou est sélectionné — et les miroirs —
 * l'édition d'une occurrence d'une variable liée, propagée à ses
 * sœurs au même décalage relatif.
 */
public final class Gabarits {

    private Gabarits() {
    }

    /**
     * Insère le rendu d'un gabarit sur chaque plage de la sélection : le
     * premier trou de chaque rendu est sélectionné, prêt à la frappe.
     *
     * @param doc       le document, jamais muté
     * @param selection la sélection courante
     * @param gabarit   le gabarit à insérer
     * @return le plan du geste
     */
    public static EditPlan inserer(Document doc, MultiSelection selection, Gabarit gabarit) {
        List<TextEdit> editions = new ArrayList<>(selection.caretCount());
        List<SelRange> apres = new ArrayList<>(selection.caretCount());
        int delta = 0;
        for (SelRange plage : selection.ranges()) {
            String retire = doc.subText(plage.start(), plage.end());
            editions.add(new TextEdit(plage.start() + delta, retire, gabarit.rendu()));
            Trou premier = gabarit.trous().isEmpty()
                    ? null : gabarit.trous().get(0);
            if (premier == null) {
                apres.add(SelRange.caretAt(plage.start() + delta + gabarit.rendu().length()));
            } else {
                apres.add(new SelRange(plage.start() + delta + premier.debut(),
                        plage.start() + delta + premier.fin()));
            }
            delta += gabarit.rendu().length() - retire.length();
        }
        return new EditPlan(Transaction.of(editions), MultiSelection.of(apres));
    }

    /**
     * Les éditions miroirs d'une édition sur une occurrence de variable
     * liée : chaque sœur reçoit la même édition au même décalage relatif,
     * borné à ses propres limites. Les positions sont dans le repère du
     * document AVANT l'édition source — l'appelant compose les éditions
     * (source puis miroirs) avec leurs deltas cumulés.
     *
     * @param rendu       le rendu du gabarit, tel qu'inséré dans le document
     * @param occurrences toutes les occurrences de la variable liée
     * @param source      l'occurrence éditée
     * @param edition     l'édition appliquée à l'occurrence source
     * @return les éditions des occurrences sœurs, dans l'ordre du rendu
     */
    public static List<TextEdit> miroirs(String rendu, List<Trou> occurrences, Trou source,
            TextEdit edition) {
        int relatifDebut = edition.start() - source.debut();
        int relatifFin = edition.end() - source.debut();
        List<TextEdit> miroirs = new ArrayList<>(occurrences.size() - 1);
        for (Trou occurrence : occurrences) {
            if (occurrence.equals(source)) {
                continue;
            }
            int debut = occurrence.debut()
                    + Math.max(0, Math.min(relatifDebut, occurrence.longueur()));
            int fin = occurrence.debut()
                    + Math.max(0, Math.min(relatifFin, occurrence.longueur()));
            miroirs.add(new TextEdit(debut, rendu.substring(debut, fin),
                    edition.inserted()));
        }
        return miroirs;
    }
}
