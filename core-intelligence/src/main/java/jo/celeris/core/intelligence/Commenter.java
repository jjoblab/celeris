package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextEdit;
import jo.celeris.core.text.Transaction;

/**
 * Le basculement du commentaire (M5, A-11) : ligne par ligne quand le
 * langage a un préfixe — tout le bloc est commenté si une ligne ne
 * l'est pas, tout est décommenté si toutes le sont, la ligne vide ne
 * compte pas — par bloc quand le langage n'a que des délimiteurs : la
 * sélection est entourée, ou dégagée si elle l'est déjà. JSON et les
 * langages inconnus masquent l'action.
 */
public final class Commenter {

    private Commenter() {
    }

    /**
     * Bascule le commentaire des lignes ou du bloc touché.
     *
     * @param doc       le document, jamais muté
     * @param selection la sélection courante
     * @param rules     les règles du langage
     * @return le plan du geste, vide si l'action est masquée
     */
    public static Optional<EditPlan> toggle(Document doc, MultiSelection selection,
            CommentRules rules) {
        if (rules.line() != null) {
            return Optional.of(lines(doc, selection, rules.line()));
        }
        if (rules.blocDebut() != null && rules.blocFin() != null) {
            return bloc(doc, selection, rules.blocDebut(), rules.blocFin());
        }
        return Optional.empty();
    }

    /** Le basculement ligne à ligne : tout ou rien sur le bloc touché. */
    private static EditPlan lines(Document doc, MultiSelection selection, String prefixe) {
        List<Integer> lines = touchedLines(doc, selection);
        boolean auMoinsUne = false;
        boolean toutesCommentees = true;
        for (int l : lines) {
            String text = doc.lineText(l);
            if (text.isBlank()) {
                continue;
            }
            auMoinsUne = true;
            if (!debutSignificatif(text).startsWith(prefixe.strip())) {
                toutesCommentees = false;
            }
        }
        List<TextEdit> edits = new ArrayList<>();
        List<SelRange> apres = new ArrayList<>();
        List<int[]> tournures = new ArrayList<>();
        int delta = 0;
        boolean commenter = auMoinsUne && !toutesCommentees;
        for (int l : lines) {
            String text = doc.lineText(l);
            if (text.isBlank()) {
                continue;
            }
            int debutSignificatif = doc.lineStart(l) + indentation(text);
            if (commenter) {
                edits.add(new TextEdit(debutSignificatif + delta, "", prefixe));
                tournures.add(new int[] {debutSignificatif, 0, prefixe.length()});
                delta += prefixe.length();
            } else if (auMoinsUne && debutSignificatif(text).startsWith(prefixe.strip())) {
                // le préfixe réellement présent part : « //x » sans espace
                // se décommente, la transaction ne lève jamais
                String retire = prefixePresent(debutSignificatif(text), prefixe);
                edits.add(new TextEdit(debutSignificatif + delta, retire, ""));
                tournures.add(new int[] {debutSignificatif, retire.length(), 0});
                delta -= retire.length();
            }
        }
        for (SelRange range : selection.ranges()) {
            apres.add(new SelRange(projeter(range.anchor(), tournures),
                    projeter(range.caret(), tournures)));
        }
        return new EditPlan(Transaction.of(edits), MultiSelection.of(apres));
    }

    /**
     * Projette une position au travers des tournures : avant, inchangée ;
     * dedans, ramenée au début ; après, décalée du différentiel.
     */
    private static int projeter(int position, List<int[]> tournures) {
        int resultat = position;
        for (int[] tournure : tournures) {
            int end = tournure[0] + tournure[1];
            if (resultat >= end) {
                resultat += tournure[2] - tournure[1];
            } else if (resultat > tournure[0]) {
                resultat = tournure[0];
            }
        }
        return resultat;
    }

    /** Le basculement par bloc : entourer ou dégager la sélection. */
    private static Optional<EditPlan> bloc(Document doc, MultiSelection selection,
            String debutBloc, String finBloc) {
        List<TextEdit> edits = new ArrayList<>();
        List<SelRange> apres = new ArrayList<>();
        int delta = 0;
        for (SelRange range : selection.ranges()) {
            int start = range.start();
            int end = range.end();
            if (start == end) {
                // plage vide : la ligne entière sert de sélection
                int line = doc.lineForOffset(start);
                start = doc.lineStart(line) + indentation(doc.lineText(line));
                end = doc.lineEnd(line);
            }
            String contenu = doc.subText(start, end);
            boolean deja = contenu.startsWith(debutBloc) && contenu.endsWith(finBloc)
                    && contenu.length() >= debutBloc.length() + finBloc.length();
            String insere;
            SelRange apresPlage;
            if (deja) {
                insere = contenu.substring(debutBloc.length(),
                        contenu.length() - finBloc.length());
                apresPlage = new SelRange(start + delta,
                        start + delta + insere.length());
            } else {
                insere = debutBloc + contenu + finBloc;
                apresPlage = new SelRange(start + delta + debutBloc.length(),
                        start + delta + debutBloc.length() + contenu.length());
            }
            edits.add(new TextEdit(start + delta, contenu, insere));
            apres.add(apresPlage);
            delta += insere.length() - contenu.length();
        }
        return Optional.of(new EditPlan(Transaction.of(edits),
                MultiSelection.of(apres)));
    }

    /** Les lignes du bloc touché, sans doublon, dans l'ordre. */
    private static List<Integer> touchedLines(Document doc, MultiSelection selection) {
        List<Integer> lines = new ArrayList<>();
        for (SelRange range : selection.ranges()) {
            int premiere = doc.lineForOffset(range.start());
            int derniere = doc.lineForOffset(Math.max(range.start(), range.end() - 1));
            for (int l = premiere; l <= derniere; l++) {
                if (!lines.contains(l)) {
                    lines.add(l);
                }
            }
        }
        return lines;
    }

    private static String debutSignificatif(String line) {
        return line.substring(indentation(line));
    }

    /** Le préfixe réellement présent sur une ligne commentée : la
     * forme canonique si la ligne la porte, la forme nue sinon — le
     * retrait ne lève jamais la transaction sur un document légal. */
    private static String prefixePresent(String significatif, String prefixe) {
        return significatif.startsWith(prefixe) ? prefixe : prefixe.strip();
    }

    private static int indentation(String line) {
        return IndentEngine.leadingWhitespace(line).length();
    }
}
