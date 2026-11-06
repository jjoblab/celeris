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
        if (rules.ligne() != null) {
            return Optional.of(lignes(doc, selection, rules.ligne()));
        }
        if (rules.blocDebut() != null && rules.blocFin() != null) {
            return bloc(doc, selection, rules.blocDebut(), rules.blocFin());
        }
        return Optional.empty();
    }

    /** Le basculement ligne à ligne : tout ou rien sur le bloc touché. */
    private static EditPlan lignes(Document doc, MultiSelection selection, String prefixe) {
        List<Integer> lignes = new ArrayList<>();
        for (SelRange plage : selection.ranges()) {
            int premiere = doc.lineForOffset(plage.start());
            int derniere = doc.lineForOffset(Math.max(plage.start(), plage.end() - 1));
            for (int l = premiere; l <= derniere; l++) {
                if (!lignes.contains(l)) {
                    lignes.add(l);
                }
            }
        }
        boolean auMoinsUne = false;
        boolean toutesCommentees = true;
        for (int l : lignes) {
            String texte = doc.lineText(l);
            if (texte.isBlank()) {
                continue;
            }
            auMoinsUne = true;
            if (!debutSignificatif(texte).startsWith(prefixe.strip())) {
                toutesCommentees = false;
            }
        }
        List<TextEdit> editions = new ArrayList<>();
        List<SelRange> apres = new ArrayList<>();
        List<int[]> tournures = new ArrayList<>();
        int delta = 0;
        boolean commenter = auMoinsUne && !toutesCommentees;
        for (int l : lignes) {
            String texte = doc.lineText(l);
            if (texte.isBlank()) {
                continue;
            }
            int debutSignificatif = doc.lineStart(l) + indentation(texte);
            if (commenter) {
                editions.add(new TextEdit(debutSignificatif + delta, "", prefixe));
                tournures.add(new int[] {debutSignificatif, 0, prefixe.length()});
                delta += prefixe.length();
            } else if (auMoinsUne && debutSignificatif(texte).startsWith(prefixe.strip())) {
                editions.add(new TextEdit(debutSignificatif + delta, prefixe, ""));
                tournures.add(new int[] {debutSignificatif, prefixe.length(), 0});
                delta -= prefixe.length();
            }
        }
        for (SelRange plage : selection.ranges()) {
            apres.add(new SelRange(projeter(plage.anchor(), tournures),
                    projeter(plage.caret(), tournures)));
        }
        return new EditPlan(Transaction.of(editions), MultiSelection.of(apres));
    }

    /**
     * Projette une position au travers des tournures : avant, inchangée ;
     * dedans, ramenée au début ; après, décalée du différentiel.
     */
    private static int projeter(int position, List<int[]> tournures) {
        int resultat = position;
        for (int[] tournure : tournures) {
            int fin = tournure[0] + tournure[1];
            if (resultat >= fin) {
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
        List<TextEdit> editions = new ArrayList<>();
        List<SelRange> apres = new ArrayList<>();
        int delta = 0;
        for (SelRange plage : selection.ranges()) {
            int debut = plage.start();
            int fin = plage.end();
            if (debut == fin) {
                // plage vide : la ligne entière sert de sélection
                int ligne = doc.lineForOffset(debut);
                debut = doc.lineStart(ligne) + indentation(doc.lineText(ligne));
                fin = doc.lineEnd(ligne);
            }
            String contenu = doc.subText(debut, fin);
            boolean deja = contenu.startsWith(debutBloc) && contenu.endsWith(finBloc)
                    && contenu.length() >= debutBloc.length() + finBloc.length();
            String insere;
            SelRange apresPlage;
            if (deja) {
                insere = contenu.substring(debutBloc.length(),
                        contenu.length() - finBloc.length());
                apresPlage = new SelRange(debut + delta,
                        debut + delta + insere.length());
            } else {
                insere = debutBloc + contenu + finBloc;
                apresPlage = new SelRange(debut + delta + debutBloc.length(),
                        debut + delta + debutBloc.length() + contenu.length());
            }
            editions.add(new TextEdit(debut + delta, contenu, insere));
            apres.add(apresPlage);
            delta += insere.length() - contenu.length();
        }
        return Optional.of(new EditPlan(Transaction.of(editions),
                MultiSelection.of(apres)));
    }

    private static String debutSignificatif(String ligne) {
        return ligne.substring(indentation(ligne));
    }

    private static int indentation(String ligne) {
        return IndentEngine.leadingWhitespace(ligne).length();
    }
}
