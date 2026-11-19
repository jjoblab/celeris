package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.List;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

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
        List<TextEdit> edits = new ArrayList<>();
        List<SelRange> apres = new ArrayList<>();
        int delta = 0;
        for (Bloc bloc : blocs(doc, selection)) {
            int premiere = bloc.premiere;
            int derniere = bloc.derniere;
            boolean bloque = direction < 0 ? premiere == 0 : derniere == doc.lineCount() - 1;
            if (!bloque) {
                String blocTexte = texteBloc(doc, premiere, derniere);
                int start = direction < 0 ? doc.lineStart(premiere - 1)
                        : doc.lineStart(premiere);
                int end = direction < 0 ? doc.lineEnd(derniere)
                        : doc.lineEnd(derniere + 1);
                String voisin = direction < 0 ? doc.lineText(premiere - 1)
                        : doc.lineText(derniere + 1);
                String insere = direction < 0 ? blocTexte + "\n" + voisin
                        : voisin + "\n" + blocTexte;
                edits.add(new TextEdit(start + delta,
                        doc.subText(start, end), insere));
                delta += insere.length() - (end - start);
            }
            for (int caret : bloc.carets) {
                int line = doc.lineForOffset(caret);
                int column = caret - doc.lineStart(line);
                int nouvelleLigne = bloque ? line : line + direction;
                apres.add(SelRange.caretAt(doc.lineStart(nouvelleLigne) + column + delta));
            }
        }
        return new EditPlan(Transaction.of(edits), MultiSelection.of(apres));
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
        List<TextEdit> edits = new ArrayList<>();
        List<SelRange> apres = new ArrayList<>();
        int delta = 0;
        for (Bloc bloc : blocs(doc, selection)) {
            String blocTexte = texteBloc(doc, bloc.premiere, bloc.derniere);
            boolean derniereLigne = bloc.derniere == doc.lineCount() - 1;
            int point = derniereLigne ? doc.length() : doc.lineEnd(bloc.derniere) + 1;
            String insere = derniereLigne ? "\n" + blocTexte : blocTexte + "\n";
            edits.add(new TextEdit(point + delta, "", insere));
            for (int caret : bloc.carets) {
                apres.add(SelRange.caretAt(caret + delta + insere.length()));
            }
            delta += insere.length();
        }
        return new EditPlan(Transaction.of(edits), MultiSelection.of(apres));
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
        List<TextEdit> edits = new ArrayList<>();
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
                int end = debutSuivant + indent;
                edits.add(new TextEdit(jonction + delta,
                        doc.subText(jonction, end), ""));
                for (int k = 0; k < bloc.carets.size(); k++) {
                    apres.add(SelRange.caretAt(jonction + delta));
                }
                delta -= end - jonction;
                continue;
            }
            int start = doc.lineStart(bloc.premiere);
            int end = doc.lineEnd(bloc.derniere);
            StringBuilder joint = new StringBuilder();
            for (int l = bloc.premiere; l <= bloc.derniere; l++) {
                String line = doc.lineText(l);
                if (l == bloc.premiere) {
                    joint.append(line);
                } else {
                    joint.append(line.substring(
                            IndentEngine.leadingWhitespace(line).length()));
                }
            }
            edits.add(new TextEdit(start + delta, doc.subText(start, end),
                    joint.toString()));
            for (int k = 0; k < bloc.carets.size(); k++) {
                apres.add(SelRange.caretAt(doc.lineEnd(bloc.premiere) + delta));
            }
            delta += joint.length() - (end - start);
        }
        return new EditPlan(Transaction.of(edits), MultiSelection.of(apres));
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
        List<TextEdit> edits = new ArrayList<>();
        List<SelRange> apres = new ArrayList<>();
        int delta = 0;
        for (Bloc bloc : blocs(doc, selection)) {
            int start = doc.lineStart(bloc.premiere);
            boolean avecSaut = doc.lineEnd(bloc.derniere) + 1 <= doc.length();
            int end;
            if (avecSaut) {
                end = doc.lineEnd(bloc.derniere) + 1;
            } else if (start > 0) {
                // dernier bloc sans saut final : le saut d'avant part avec
                start -= 1;
                end = doc.length();
            } else {
                end = doc.length();
            }
            edits.add(new TextEdit(start + delta, doc.subText(start, end), ""));
            for (int k = 0; k < bloc.carets.size(); k++) {
                apres.add(SelRange.caretAt(start + delta));
            }
            delta -= end - start;
        }
        return new EditPlan(Transaction.of(edits), MultiSelection.of(apres));
    }

    /**
     * Les lignes touchées par la sélection : chaque plage donne sa ligne
     * de garde et sa ligne d'extrémité, les lignes contiguës fusionnent
     * en blocs.
     */
    static List<Bloc> blocs(Document doc, MultiSelection selection) {
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
        lines.sort(null);
        List<Bloc> blocs = new ArrayList<>();
        for (int line : lines) {
            if (!blocs.isEmpty() && blocs.get(blocs.size() - 1).derniere == line - 1) {
                Bloc dernier = blocs.remove(blocs.size() - 1);
                blocs.add(new Bloc(dernier.premiere, line, dernier.carets));
            } else {
                blocs.add(new Bloc(line, line));
            }
        }
        // les carets dans chaque bloc : les positions vides de la sélection
        List<Bloc> avecCarets = new ArrayList<>(blocs.size());
        for (Bloc bloc : blocs) {
            List<Integer> carets = new ArrayList<>();
            for (SelRange range : selection.ranges()) {
                for (int p : new int[] {range.caret(), range.anchor()}) {
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
        StringBuilder text = new StringBuilder();
        for (int l = premiere; l <= derniere; l++) {
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append(doc.lineText(l));
        }
        return text.toString();
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
        List<TextEdit> edits = new ArrayList<>(selection.caretCount());
        List<SelRange> apres = new ArrayList<>(selection.caretCount());
        int delta = 0;
        for (SelRange range : selection.ranges()) {
            String cran = style.indent(1);
            if (range.isEmpty()) {
                edits.add(new TextEdit(range.caret() + delta, "", cran));
                apres.add(SelRange.caretAt(range.caret() + delta
                        + cran.length()));
                delta += cran.length();
                continue;
            }
            int premiereLigne = doc.lineForOffset(range.start());
            int derniereLigne = doc.lineForOffset(
                    range.end() - (range.end() > range.start() ? 1 : 0));
            for (int line = premiereLigne; line <= derniereLigne; line++) {
                int start = doc.lineStart(line);
                if (start >= doc.length()) {
                    continue;
                }
                edits.add(new TextEdit(start + delta, "", cran));
                delta += cran.length();
            }
            apres.add(new SelRange(range.start() + cran.length(),
                    range.end() + delta));
        }
        return new EditPlan(Transaction.of(edits), MultiSelection.of(apres));
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
        List<TextEdit> edits = new ArrayList<>(selection.caretCount());
        List<SelRange> apres = new ArrayList<>(selection.caretCount());
        int delta = 0;
        for (SelRange range : selection.ranges()) {
            if (range.isEmpty()) {
                int line = doc.lineForOffset(range.caret());
                String blancs = IndentEngine.leadingWhitespace(
                        doc.lineText(line));
                int retire = Math.min(Math.max(1, style.width()),
                        Math.min(blancs.length(),
                                range.caret() - doc.lineStart(line)));
                if (retire <= 0) {
                    apres.add(SelRange.caretAt(range.caret() + delta));
                    continue;
                }
                edits.add(new TextEdit(range.caret() - retire + delta,
                        doc.subText(range.caret() - retire, range.caret()), ""));
                apres.add(SelRange.caretAt(range.caret() - retire + delta));
                delta -= retire;
                continue;
            }
            int premiereLigne = doc.lineForOffset(range.start());
            int derniereLigne = doc.lineForOffset(
                    range.end() - (range.end() > range.start() ? 1 : 0));
            int retraitTotal = 0;
            int retraitDebut = 0;
            for (int line = premiereLigne; line <= derniereLigne; line++) {
                String text = doc.lineText(line);
                String blancs = IndentEngine.leadingWhitespace(text);
                int retire = Math.min(style.width(), blancs.length());
                if (retire <= 0) {
                    continue;
                }
                edits.add(new TextEdit(doc.lineStart(line) + delta,
                        text.substring(0, retire), ""));
                delta -= retire;
                retraitTotal += retire;
                if (line == premiereLigne) {
                    retraitDebut = Math.min(retire,
                            range.start() - doc.lineStart(line));
                }
            }
            apres.add(new SelRange(range.start() - retraitDebut,
                    range.end() - retraitTotal));
        }
        return new EditPlan(Transaction.of(edits), MultiSelection.of(apres));
    }

    /** Le collapse des lignes vides (C7) : le retour arrière sur un
     *  blanc (ou en tête de ligne) précédé d'un saut et suivi d'un
     *  autre saut retire le blanc ET le saut qui précède — « a\n\n\nb »
     *  devient « a\nb ». La ligne vide s'effondre d'un geste. */
    static Optional<TypingGesture> collapseBlankLine(Document doc, int p,
            int delta) {
        if (p < 1 || p > doc.length()) {
            return Optional.empty();
        }
        int line = doc.lineForOffset(p);
        int start = doc.lineStart(line);
        // le caret vit sur les blancs de tête (ou au début) d'une
        // ligne dont la précédente est vide
        if (p > start) {
            return Optional.empty();
        }
        if (line < 1) {
            return Optional.empty();
        }
        int debutPrecedente = doc.lineStart(line - 1);
        int finPrecedente = doc.lineEnd(line - 1);
        if (finPrecedente - debutPrecedente > 0) {
            // la précédente n'est pas vide : rien à effondrer
            return Optional.empty();
        }
        if (p >= doc.length() || doc.charAt(p) == '\n') {
            // la ligne courante est vide aussi : effondrer le saut de
            // la précédente
            return Optional.of(new TypingGesture(
                    new TextEdit(debutPrecedente + delta, "\n", ""),
                    SelRange.caretAt(debutPrecedente + delta), delta - 1));
        }
        return Optional.empty();
    }

    /**
     * La suppression avant (C8, l'analyse croisée) : la paire vide
     * sous le caret se mange ENTIÈRE — « (|) » devient « » d'un
     * geste, comme le dépôt de départ. La citation vide pareil. Tout
     * autre cas rend la main : le cœur supprime le cluster suivant.
     *
     * @param doc       le document, jamais muté
     * @param selection la sélection courante
     * @param rules     les règles du langage
     * @return le plan du geste, vide pour retomber sur le cœur
     */
    public static Optional<EditPlan> deleteForward(Document doc,
            MultiSelection selection, TypingRules rules) {
        if (selection.isEmpty()) {
            return Optional.empty();
        }
        List<TextEdit> edits = new ArrayList<>(selection.caretCount());
        List<SelRange> apres = new ArrayList<>(selection.caretCount());
        int delta = 0;
        for (SelRange range : selection.ranges()) {
            if (!range.isEmpty()) {
                return Optional.empty();
            }
            int p = range.caret();
            if (p + 1 >= doc.length()) {
                return Optional.empty();
            }
            char devant = p < doc.length() ? doc.charAt(p) : '\0';
            char apresDeux = doc.charAt(p + 1);
            boolean paireVide = rules.estOuvrant(devant)
                    && apresDeux == rules.fermantDe(devant);
            boolean citationsVides = rules.estCitation(devant) && apresDeux == devant;
            if (!paireVide && !citationsVides) {
                return Optional.empty();
            }
            edits.add(new TextEdit(p + delta,
                    deuxCaracteres(devant, apresDeux), ""));
            apres.add(SelRange.caretAt(p + delta));
            delta -= 2;
        }
        return Optional.of(new EditPlan(Transaction.of(edits),
                MultiSelection.of(apres)));
    }

    private static String deuxCaracteres(char a, char b) {
        return new String(new char[] {a, b});
    }

    /** Le geste d'une plage : édition (nulle pour le saut), sélection d'après, delta cumulé. */
    record TypingGesture(@Nullable TextEdit edit, SelRange selection, int delta) {
    }
}
