package jo.celeris.demo;

import jo.celeris.core.intelligence.EditPlan;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextEdit;
import jo.celeris.core.text.Transaction;

import org.jspecify.annotations.Nullable;

/**
 * L'entourage de la sélection (catégorie D de l'inventaire — « Surround
 * With / Live Templates », le geste dormait, le moteur naît) : la
 * sélection non vide se ceint de son avant et de son après — try/catch,
 * if, les constructions du terrain —, le bloc entouré gagne un cran
 * d'indentation, la sélection se reporte sur le bloc. Une transaction
 * annulable d'un geste, la voie unique (ADR-003) — jamais le document
 * muté avant l'application.
 */
final class Entourer {

    /** Le cran d'indentation que l'entourage donne au bloc ceint. */
    private static final String INDENT = "    ";

    private Entourer() {
    }

    /**
     * Le plan d'entourage de la sélection : l'avant ouvre, l'après
     * ferme, le bloc ceint gagne un cran d'indentation par ligne
     * portante — la sélection se reporte sur le bloc ceint (l'objet du
     * geste reste l'objet de la sélection).
     *
     * @param doc       le document, jamais muté
     * @param selection la sélection courante — une plage NON VIDE
     * @param avant     le texte qui ouvre (par exemple
     *                  {@code "try {"})
     * @param apres     le texte qui ferme (par exemple
     *                  {@code "} finally {"} ou {@code "}"})
     * @return le plan du geste, null si la sélection est vide ou
     *         multiple — rien à entourer
     */
    static @Nullable EditPlan entourer(Document doc, MultiSelection selection,
            String avant, String apres) {
        if (selection.ranges().size() != 1 || selection.ranges().get(0).isEmpty()) {
            return null;
        }
        SelRange plage = selection.ranges().get(0);
        int debut = plage.start();
        int fin = plage.end();
        String bloc = doc.subText(debut, fin);
        String blocCeint = indente(bloc);
        String ceint = avant + "\n" + blocCeint + "\n" + apres;
        int blocDebut = debut + avant.length() + 1;
        int blocFin = blocDebut + blocCeint.length();
        return new EditPlan(
                Transaction.of(new TextEdit(debut, bloc, ceint)),
                MultiSelection.of(new SelRange(blocDebut, blocFin)));
    }

    /** Le bloc indenté : chaque ligne portante gagne son cran, les
     *  vides restent vides — l'entourage ne respire pas dans le vide. */
    private static String indente(String bloc) {
        StringBuilder rendu = new StringBuilder(bloc.length() + 16);
        for (String ligne : bloc.split("\n", -1)) {
            if (rendu.length() > 0) {
                rendu.append('\n');
            }
            if (!ligne.isBlank()) {
                rendu.append(INDENT);
            }
            rendu.append(ligne);
        }
        return rendu.toString();
    }
}
