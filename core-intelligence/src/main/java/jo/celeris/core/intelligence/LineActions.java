package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextEdit;
import jo.celeris.core.text.Transaction;

/**
 * Les actions de lignes (M5, A-10) : déplacer, dupliquer, joindre,
 * supprimer. L'unité du geste est le bloc de lignes touchées par la
 * sélection — chaque plage contribue ses lignes, les blocs contigus
 * fusionnent. Moteur pur : les plans sortent prêts pour la voie unique.
 */
public final class LineActions {

    private LineActions() {
    }

    /**
     * Déplace le bloc touché d'un cran dans la direction donnée : le bloc
     * échange sa place avec la ligne voisine. Un bloc bloqué au bord ne
     * produit aucune édition — le plan est vide, la sélection tient.
     *
     * @param doc       le document, jamais muté
     * @param selection la sélection courante
     * @param direction -1 vers le haut, +1 vers le bas
     * @return le plan du geste
     */
    public static EditPlan moveLines(Document doc, MultiSelection selection, int direction) {
        List<TextEdit> editions = new ArrayList<>();
        List<SelRange> apres = new ArrayList<>();
        int delta = 0;
        for (Bloc bloc : blocs(doc, selection)) {
            int premiere = bloc.premiere;
            int derniere = bloc.derniere;
            boolean bloque = direction < 0 ? premiere == 0 : derniere == doc.lineCount() - 1;
            if (!bloque) {
                String blocTexte = texteBloc(doc, premiere, derniere);
                int debut = direction < 0 ? doc.lineStart(premiere - 1)
                        : doc.lineStart(premiere);
                int fin = direction < 0 ? doc.lineEnd(derniere)
                        : doc.lineEnd(derniere + 1);
                String voisin = direction < 0 ? doc.lineText(premiere - 1)
                        : doc.lineText(derniere + 1);
                String insere = direction < 0 ? blocTexte + "\n" + voisin
                        : voisin + "\n" + blocTexte;
                editions.add(new TextEdit(debut + delta,
                        doc.subText(debut, fin), insere));
                delta += insere.length() - (fin - debut);
            }
            for (int caret : bloc.carets) {
                int ligne = doc.lineForOffset(caret);
                int colonne = caret - doc.lineStart(ligne);
                int nouvelleLigne = bloque ? ligne : ligne + direction;
                apres.add(SelRange.caretAt(doc.lineStart(nouvelleLigne) + colonne + delta));
            }
        }
        return new EditPlan(Transaction.of(editions), MultiSelection.of(apres));
    }

    /**
     * Duplique le bloc touché : la copie suit immédiatement le bloc, les
     * curseurs descendent dans la copie, colonnes conservées.
     *
     * @param doc       le document, jamais muté
     * @param selection la sélection courante
     * @return le plan du geste
     */
    public static EditPlan duplicateLines(Document doc, MultiSelection selection) {
        List<TextEdit> editions = new ArrayList<>();
        List<SelRange> apres = new ArrayList<>();
        int delta = 0;
        for (Bloc bloc : blocs(doc, selection)) {
            String blocTexte = texteBloc(doc, bloc.premiere, bloc.derniere);
            boolean derniereLigne = bloc.derniere == doc.lineCount() - 1;
            int point = derniereLigne ? doc.length() : doc.lineEnd(bloc.derniere) + 1;
            String insere = derniereLigne ? "\n" + blocTexte : blocTexte + "\n";
            editions.add(new TextEdit(point + delta, "", insere));
            for (int caret : bloc.carets) {
                apres.add(SelRange.caretAt(caret + delta + insere.length()));
            }
            delta += insere.length();
        }
        return new EditPlan(Transaction.of(editions), MultiSelection.of(apres));
    }

    /**
     * Joint les lignes du bloc : un bloc d'une ligne se colle à la
     * suivante (le saut et l'indentation de celle-ci disparaissent), un
     * bloc de plusieurs lignes fusionne en une seule — sans toucher à la
     * ligne qui suit le bloc. Le curseur se pose au point de jonction.
     *
     * @param doc       le document, jamais muté
     * @param selection la sélection courante
     * @return le plan du geste
     */
    public static EditPlan joinLines(Document doc, MultiSelection selection) {
        List<TextEdit> editions = new ArrayList<>();
        List<SelRange> apres = new ArrayList<>();
        int delta = 0;
        for (Bloc bloc : blocs(doc, selection)) {
            if (bloc.premiere == bloc.derniere) {
                if (bloc.derniere == doc.lineCount() - 1) {
                    for (int caret : bloc.carets) {
                        apres.add(SelRange.caretAt(caret + delta));
                    }
                    continue;
                }
                int jonction = doc.lineEnd(bloc.premiere);
                int debutSuivant = doc.lineStart(bloc.premiere + 1);
                String suivant = doc.lineText(bloc.premiere + 1);
                int indent = IndentEngine.leadingWhitespace(suivant).length();
                int fin = debutSuivant + indent;
                editions.add(new TextEdit(jonction + delta,
                        doc.subText(jonction, fin), ""));
                for (int k = 0; k < bloc.carets.size(); k++) {
                    apres.add(SelRange.caretAt(jonction + delta));
                }
                delta -= fin - jonction;
                continue;
            }
            int debut = doc.lineStart(bloc.premiere);
            int fin = doc.lineEnd(bloc.derniere);
            StringBuilder joint = new StringBuilder();
            for (int l = bloc.premiere; l <= bloc.derniere; l++) {
                String ligne = doc.lineText(l);
                if (l == bloc.premiere) {
                    joint.append(ligne);
                } else {
                    joint.append(ligne.substring(
                            IndentEngine.leadingWhitespace(ligne).length()));
                }
            }
            editions.add(new TextEdit(debut + delta, doc.subText(debut, fin),
                    joint.toString()));
            for (int k = 0; k < bloc.carets.size(); k++) {
                apres.add(SelRange.caretAt(doc.lineEnd(bloc.premiere) + delta));
            }
            delta += joint.length() - (fin - debut);
        }
        return new EditPlan(Transaction.of(editions), MultiSelection.of(apres));
    }

    /**
     * Supprime le bloc touché, son saut compris : le document se referme,
     * le curseur se pose là où le bloc vivait.
     *
     * @param doc       le document, jamais muté
     * @param selection la sélection courante
     * @return le plan du geste
     */
    public static EditPlan deleteLines(Document doc, MultiSelection selection) {
        List<TextEdit> editions = new ArrayList<>();
        List<SelRange> apres = new ArrayList<>();
        int delta = 0;
        for (Bloc bloc : blocs(doc, selection)) {
            int debut = doc.lineStart(bloc.premiere);
            boolean avecSaut = doc.lineEnd(bloc.derniere) + 1 <= doc.length();
            int fin;
            if (avecSaut) {
                fin = doc.lineEnd(bloc.derniere) + 1;
            } else if (debut > 0) {
                // dernier bloc sans saut final : le saut d'avant part avec
                debut -= 1;
                fin = doc.length();
            } else {
                fin = doc.length();
            }
            editions.add(new TextEdit(debut + delta, doc.subText(debut, fin), ""));
            for (int k = 0; k < bloc.carets.size(); k++) {
                apres.add(SelRange.caretAt(debut + delta));
            }
            delta -= fin - debut;
        }
        return new EditPlan(Transaction.of(editions), MultiSelection.of(apres));
    }

    /**
     * Les lignes touchées par la sélection : chaque plage donne sa ligne
     * de garde et sa ligne d'extrémité, les lignes contiguës fusionnent
     * en blocs.
     */
    static List<Bloc> blocs(Document doc, MultiSelection selection) {
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
        lignes.sort(null);
        List<Bloc> blocs = new ArrayList<>();
        for (int ligne : lignes) {
            if (!blocs.isEmpty() && blocs.get(blocs.size() - 1).derniere == ligne - 1) {
                Bloc dernier = blocs.remove(blocs.size() - 1);
                blocs.add(new Bloc(dernier.premiere, ligne, dernier.carets));
            } else {
                blocs.add(new Bloc(ligne, ligne));
            }
        }
        // les carets dans chaque bloc : les positions vides de la sélection
        List<Bloc> avecCarets = new ArrayList<>(blocs.size());
        for (Bloc bloc : blocs) {
            List<Integer> carets = new ArrayList<>();
            for (SelRange plage : selection.ranges()) {
                for (int p : new int[] {plage.caret(), plage.anchor()}) {
                    int l = doc.lineForOffset(p);
                    if (l >= bloc.premiere && l <= bloc.derniere && !carets.contains(p)) {
                        carets.add(p);
                    }
                }
            }
            carets.sort(null);
            avecCarets.add(new Bloc(bloc.premiere, bloc.derniere, carets));
        }
        return avecCarets;
    }

    private static String texteBloc(Document doc, int premiere, int derniere) {
        StringBuilder texte = new StringBuilder();
        for (int l = premiere; l <= derniere; l++) {
            if (texte.length() > 0) {
                texte.append('\n');
            }
            texte.append(doc.lineText(l));
        }
        return texte.toString();
    }

    /** Un bloc contigu de lignes, avec les positions vides qui s'y trouvent. */
    static final class Bloc {

        final int premiere;

        final int derniere;

        final List<Integer> carets;

        Bloc(int premiere, int derniere) {
            this(premiere, derniere, List.of());
        }

        Bloc(int premiere, int derniere, List<Integer> carets) {
            this.premiere = premiere;
            this.derniere = derniere;
            this.carets = List.copyOf(carets);
        }
    }
    /**
     * Tab (C1, l'analyse croisée) : la sélection multi-lignes gagne un
     * cran d'indentation sur CHACUNE de ses lignes — la sélection
     * survit au geste (mêmes lignes, décalées du cran) ; au caret
     * simple, l'indentation se pose à la coupe. Le dépôt de départ
     * indente pareil ses lignes sélectionnées.
     *
     * @param doc       le document, jamais muté
     * @param selection la sélection courante
     * @param style     le style d'indentation de l'hôte
     * @return le plan du geste
     */
    public static EditPlan tab(Document doc, MultiSelection selection,
            IndentStyle style) {
        List<TextEdit> editions = new ArrayList<>(selection.caretCount());
        List<SelRange> apres = new ArrayList<>(selection.caretCount());
        int delta = 0;
        for (SelRange plage : selection.ranges()) {
            String cran = style.indent(1);
            if (plage.isEmpty()) {
                editions.add(new TextEdit(plage.caret() + delta, "", cran));
                apres.add(SelRange.caretAt(plage.caret() + delta
                        + cran.length()));
                delta += cran.length();
                continue;
            }
            int premiereLigne = doc.lineForOffset(plage.start());
            int derniereLigne = doc.lineForOffset(
                    plage.end() - (plage.end() > plage.start() ? 1 : 0));
            for (int ligne = premiereLigne; ligne <= derniereLigne; ligne++) {
                int debut = doc.lineStart(ligne);
                if (debut >= doc.length()) {
                    continue;
                }
                editions.add(new TextEdit(debut + delta, "", cran));
                delta += cran.length();
            }
            apres.add(new SelRange(plage.start() + cran.length(),
                    plage.end() + delta));
        }
        return new EditPlan(Transaction.of(editions), MultiSelection.of(apres));
    }

    /**
     * Shift+Tab (C1) : chaque ligne de la sélection perd UN cran
     * d'indentation — au plus son indentation, jamais plus. Le
     * caret vide retire le cran qui précède la coupe.
     *
     * @param doc       le document, jamais muté
     * @param selection la sélection courante
     * @param style     le style d'indentation de l'hôte
     * @return le plan du geste
     */
    public static EditPlan untab(Document doc, MultiSelection selection,
            IndentStyle style) {
        List<TextEdit> editions = new ArrayList<>(selection.caretCount());
        List<SelRange> apres = new ArrayList<>(selection.caretCount());
        int delta = 0;
        for (SelRange plage : selection.ranges()) {
            if (plage.isEmpty()) {
                int ligne = doc.lineForOffset(plage.caret());
                String blancs = IndentEngine.leadingWhitespace(
                        doc.lineText(ligne));
                int retire = Math.min(Math.max(1, style.width()),
                        Math.min(blancs.length(),
                                plage.caret() - doc.lineStart(ligne)));
                if (retire <= 0) {
                    apres.add(SelRange.caretAt(plage.caret() + delta));
                    continue;
                }
                editions.add(new TextEdit(plage.caret() - retire + delta,
                        doc.subText(plage.caret() - retire, plage.caret()), ""));
                apres.add(SelRange.caretAt(plage.caret() - retire + delta));
                delta -= retire;
                continue;
            }
            int premiereLigne = doc.lineForOffset(plage.start());
            int derniereLigne = doc.lineForOffset(
                    plage.end() - (plage.end() > plage.start() ? 1 : 0));
            int retraitTotal = 0;
            int retraitDebut = 0;
            for (int ligne = premiereLigne; ligne <= derniereLigne; ligne++) {
                String texte = doc.lineText(ligne);
                String blancs = IndentEngine.leadingWhitespace(texte);
                int retire = Math.min(style.width(), blancs.length());
                if (retire <= 0) {
                    continue;
                }
                editions.add(new TextEdit(doc.lineStart(ligne) + delta,
                        texte.substring(0, retire), ""));
                delta -= retire;
                retraitTotal += retire;
                if (ligne == premiereLigne) {
                    retraitDebut = Math.min(retire,
                            plage.start() - doc.lineStart(ligne));
                }
            }
            apres.add(new SelRange(plage.start() - retraitDebut,
                    plage.end() - retraitTotal));
        }
        return new EditPlan(Transaction.of(editions), MultiSelection.of(apres));
    }
}
