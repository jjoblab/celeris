package jo.celeris.core.intelligence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.syntax.Analysis;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextEdit;
import jo.celeris.core.text.Transaction;

/**
 * Le moteur de décision de la frappe intelligente (M5) : la fermeture
 * automatique des paires et citations (A-05), l'indentation à l'Entrée
 * (A-06), le retour arrière (A-07), l'entourage de la sélection (A-08).
 * Moteur pur : il lit le document et l'analyse, rend un
 * {@link EditPlan}, ne mute jamais — l'application appartient à la
 * session, par la voie unique (ADR-003).
 *
 * <p>La profondeur d'indentation se lit dans l'arbre
 * ({@link IndentEngine#depthAtCaret}) : les nœuds structurants qui
 * englobent le caret. L'arbre peut être absent (langage inconnu, analyse
 * en cours) : le repli conserve l'indentation de la ligne courante. Un
 * arbre périmé d'une édition à peine frappée peut décaler le retrait
 * d'un cran jusqu'à la ré-analyse — borné, documenté, jamais faux de
 * plus d'un cran.</p>
 */
public final class SmartTyping {

    private SmartTyping() {
    }

    /**
     * La frappe d'un texte, curseur par curseur : l'ouvrant insère sa
     * paire (curseur au milieu), la citation se dédouble, l'ouvrant ou la
     * citation sur une sélection l'entoure (le contenu reste sélectionné),
     * le fermant déjà présent sous le curseur est sauté — pas de double
     * fermeture. Tout autre texte s'insère simplement.
     *
     * @param doc       le document, jamais muté
     * @param selection la sélection courante
     * @param typed     le texte frappé (un caractère pour les règles)
     * @param rules     les règles du langage
     * @return le plan du geste, vide si la sélection est vide
     */
    public static Optional<EditPlan> insert(Document doc, MultiSelection selection,
            String typed, TypingRules rules) {
        if (selection.isEmpty() || typed.isEmpty()) {
            return Optional.empty();
        }
        List<TextEdit> editions = new ArrayList<>(selection.caretCount());
        List<SelRange> apres = new ArrayList<>(selection.caretCount());
        int delta = 0;
        for (SelRange plage : selection.ranges()) {
            Geste geste = frappePlage(doc, plage, typed, rules, delta);
            if (geste.edition() != null) {
                editions.add(geste.edition());
            }
            apres.add(geste.selection());
            delta = geste.delta();
        }
        return Optional.of(new EditPlan(Transaction.of(editions),
                MultiSelection.of(apres)));
    }

    /** Le geste de frappe d'une plage : entourage, paire ou saut. */
    private static Geste frappePlage(Document doc, SelRange plage, String typed,
            TypingRules rules, int delta) {
        boolean caractere = typed.length() == 1;
        char c = caractere ? typed.charAt(0) : '\0';
        if (!plage.isEmpty()) {
            String contenu = doc.subText(plage.start(), plage.end());
            String ferme = caractere && rules.estOuvrant(c) ? String.valueOf(rules.fermantDe(c))
                    : caractere && rules.estCitation(c) ? typed : "";
            if (!ferme.isEmpty()) {
                String insere = typed + contenu + ferme;
                return new Geste(
                        new TextEdit(plage.start() + delta, contenu, insere),
                        new SelRange(plage.start() + delta + 1, plage.end() + delta + 1),
                        delta + insere.length() - contenu.length());
            }
            return new Geste(new TextEdit(plage.start() + delta, contenu, typed),
                    SelRange.caretAt(plage.start() + delta + typed.length()),
                    delta + typed.length() - contenu.length());
        }
        int p = plage.caret();
        int pd = p + delta;
        boolean ouvre = caractere && (rules.estOuvrant(c) || rules.estCitation(c));
        boolean saute = caractere && (rules.estFermant(c) || rules.estCitation(c))
                && p < doc.length() && doc.charAt(p) == c;
        if (ouvre) {
            String ferme = rules.estOuvrant(c) ? String.valueOf(rules.fermantDe(c)) : typed;
            String insere = typed + ferme;
            return new Geste(new TextEdit(pd, "", insere), SelRange.caretAt(pd + 1),
                    delta + insere.length());
        }
        if (saute) {
            // le fermant déjà présent est sauté : aucune édition
            return new Geste(null, SelRange.caretAt(pd + 1), delta);
        }
        return new Geste(new TextEdit(pd, "", typed),
                SelRange.caretAt(pd + typed.length()), delta + typed.length());
    }

    /**
     * L'Entrée : le saut porte l'indentation attendue — la profondeur
     * lue dans l'arbre au point de coupe, le style de l'hôte. Le fermant
     * qui reste sur la ligne descend seul sous le curseur : l'Entrée
     * entre accolades produit les deux sauts. Une sélection non vide est
     * remplacée par le saut indenté. Sans arbre, la ligne courante prête
     * son indentation.
     *
     * @param doc       le document, jamais muté
     * @param analysis  l'analyse courante, éventuellement absente
     * @param selection la sélection courante
     * @param style     le style d'indentation de l'hôte
     * @return le plan du geste
     */
    public static EditPlan enter(Document doc, @Nullable Analysis analysis,
            MultiSelection selection, IndentStyle style) {
        List<TextEdit> editions = new ArrayList<>(selection.caretCount());
        List<SelRange> apres = new ArrayList<>(selection.caretCount());
        int delta = 0;
        for (SelRange plage : selection.ranges()) {
            int coupe = plage.isEmpty() ? plage.caret() : plage.start();
            String retire = doc.subText(plage.start(), plage.end());
            int ligne = doc.lineForOffset(coupe);
            String indentation = indentationAttendue(doc, analysis, coupe, ligne, style);
            String reste = doc.subText(coupe, doc.lineEnd(ligne));
            String insere;
            if (!plage.isEmpty()) {
                insere = "\n" + indentation;
            } else if (commenceParFermant(reste) && !indentation.isEmpty()) {
                int profondeur = IndentEngine.depthAtCaret(doc, analysis, coupe);
                insere = "\n" + indentation + "\n" + style.indent(profondeur - 1);
            } else {
                insere = "\n" + indentation;
            }
            editions.add(new TextEdit(plage.start() + delta, retire, insere));
            apres.add(SelRange.caretAt(plage.start() + delta + 1 + indentation.length()));
            delta += insere.length() - retire.length();
        }
        return new EditPlan(Transaction.of(editions), MultiSelection.of(apres));
    }

    /**
     * Le retour arrière intelligent : la paire vide disparaît entière, la
     * citation vide aussi, l'indentation excédentaire se retire d'un
     * cran. Tout autre cas rend la main : la session retombe sur sa
     * suppression par clusters de graphèmes (ADR-003) — le repli est
     * global, geste partagé par tous les curseurs.
     *
     * @param doc       le document, jamais muté
     * @param selection la sélection courante
     * @param rules     les règles du langage
     * @param style     le style d'indentation de l'hôte
     * @return le plan du geste, vide pour retomber sur le cœur
     */
    public static Optional<EditPlan> backspace(Document doc, MultiSelection selection,
            TypingRules rules, IndentStyle style) {
        if (selection.isEmpty()) {
            return Optional.empty();
        }
        List<TextEdit> editions = new ArrayList<>(selection.caretCount());
        List<SelRange> apres = new ArrayList<>(selection.caretCount());
        int delta = 0;
        for (SelRange plage : selection.ranges()) {
            Optional<Geste> geste = retourArrierePlage(doc, plage, rules, style, delta);
            if (geste.isEmpty()) {
                // pas de règle : le repli global rend la main au cœur
                return Optional.empty();
            }
            Geste g = geste.get();
            editions.add(g.edition());
            apres.add(g.selection());
            delta = g.delta();
        }
        return Optional.of(new EditPlan(Transaction.of(editions),
                MultiSelection.of(apres)));
    }

    /** Le retour arrière d'une plage : paire vide, citation vide, cran d'indentation. */
    private static Optional<Geste> retourArrierePlage(Document doc, SelRange plage,
            TypingRules rules, IndentStyle style, int delta) {
        if (!plage.isEmpty()) {
            String retire = doc.subText(plage.start(), plage.end());
            return Optional.of(new Geste(
                    new TextEdit(plage.start() + delta, retire, ""),
                    SelRange.caretAt(plage.start() + delta), delta - retire.length()));
        }
        int p = plage.caret();
        if (p >= 1 && p < doc.length()) {
            char avant = doc.charAt(p - 1);
            char apresCaret = doc.charAt(p);
            boolean paireVide = rules.estOuvrant(avant) && apresCaret == rules.fermantDe(avant);
            boolean citationsVides = rules.estCitation(avant) && apresCaret == avant;
            if (paireVide || citationsVides) {
                return Optional.of(new Geste(
                        new TextEdit(p - 1 + delta, deuxCaracteres(avant, apresCaret), ""),
                        SelRange.caretAt(p - 1 + delta), delta - 2));
            }
        }
        return cranIndentation(doc, p, style, delta);
    }

    /** Le cran d'indentation excédentaire, ou la main au cœur. */
    private static Optional<Geste> cranIndentation(Document doc, int p, IndentStyle style,
            int delta) {
        int ligne = doc.lineForOffset(p);
        int debut = doc.lineStart(ligne);
        String blancs = IndentEngine.leadingWhitespace(doc.lineText(ligne));
        if (p > debut && blancs.length() >= p - debut) {
            int colonne = p - debut;
            int cran = colonne % style.width() == 0
                    ? Math.min(style.width(), colonne)
                    : colonne % style.width();
            return Optional.of(new Geste(
                    new TextEdit(p - cran + delta, doc.subText(p - cran, p), ""),
                    SelRange.caretAt(p - cran + delta), delta - cran));
        }
        return Optional.empty();
    }

    /**
     * Entourer la sélection (A-08) : par une paire en ligne (curseur
     * après le fermant), ou par un bloc multiligne (le contenu gagne un
     * cran entre les délimiteurs). Chaque plage de la sélection est
     * entourée.
     *
     * @param doc        le document, jamais muté
     * @param selection  la sélection à entourer
     * @param avant      le délimiteur d'ouverture
     * @param apres      le délimiteur de fermeture
     * @param parLignes  vrai pour le bloc multiligne
     * @param style      le style d'indentation de l'hôte
     * @return le plan du geste
     */
    public static EditPlan surround(Document doc, MultiSelection selection,
            String avant, String apres, boolean parLignes, IndentStyle style) {
        List<TextEdit> editions = new ArrayList<>(selection.caretCount());
        List<SelRange> resultante = new ArrayList<>(selection.caretCount());
        int delta = 0;
        for (SelRange plage : selection.ranges()) {
            String contenu = doc.subText(plage.start(), plage.end());
            String insere;
            if (parLignes && !contenu.isEmpty()) {
                StringBuilder corps = new StringBuilder();
                for (String ligne : contenu.split("\n", -1)) {
                    if (corps.length() > 0) {
                        corps.append('\n');
                    }
                    corps.append(style.indent(1)).append(ligne);
                }
                insere = avant + "\n" + corps + "\n" + apres;
            } else {
                insere = avant + contenu + apres;
            }
            editions.add(new TextEdit(plage.start() + delta, contenu, insere));
            resultante.add(SelRange.caretAt(plage.start() + delta + insere.length()));
            delta += insere.length() - contenu.length();
        }
        return new EditPlan(Transaction.of(editions), MultiSelection.of(resultante));
    }

    /**
     * L'indentation attendue d'une coupe : la profondeur de l'arbre au
     * point de coupe, l'indentation de la ligne en repli.
     */
    private static String indentationAttendue(Document doc, @Nullable Analysis analysis,
            int coupe, int ligne, IndentStyle style) {
        if (analysis != null) {
            return style.indent(IndentEngine.depthAtCaret(doc, analysis, coupe));
        }
        return IndentEngine.leadingWhitespace(doc.lineText(ligne));
    }

    /**
     * Vrai si le reste de ligne commence par un crochet fermant.
     */
    private static boolean commenceParFermant(String reste) {
        for (int i = 0; i < reste.length(); i++) {
            char c = reste.charAt(i);
            if (c == ' ' || c == '\t') {
                continue;
            }
            return c == '}' || c == ']' || c == ')';
        }
        return false;
    }

    private static String deuxCaracteres(char a, char b) {
        return new String(new char[] {a, b});
    }

    /** Le geste d'une plage : édition (nulle pour le saut), sélection d'après, delta cumulé. */
    private record Geste(@Nullable TextEdit edition, SelRange selection, int delta) {
    }
}
