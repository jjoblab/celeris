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
 * en cours) : le repli conserve l'indentation de la ligne courante —
 * la discipline du dépôt de départ y gagne ses crans : l'ouvrant collé
 * au caret gagne un cran, la paire vide s'étend en deux sauts, le
 * fermant rend le retrait de sa ligne d'ouverture. Un
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
        List<TextEdit> edits = new ArrayList<>(selection.caretCount());
        List<SelRange> apres = new ArrayList<>(selection.caretCount());
        int delta = 0;
        for (SelRange range : selection.ranges()) {
            TypingGesture geste = typeRange(doc, range, typed, rules, delta);
            if (geste.edit() != null) {
                edits.add(geste.edit());
            }
            apres.add(geste.selection());
            delta = geste.delta();
        }
        return Optional.of(new EditPlan(Transaction.of(edits),
                MultiSelection.of(apres)));
    }

    /** Le geste de frappe d'une plage : entourage, paire ou saut. */
    private static TypingGesture typeRange(Document doc, SelRange range, String typed,
            TypingRules rules, int delta) {
        boolean caractere = typed.length() == 1;
        char c = caractere ? typed.charAt(0) : '\0';
        if (!range.isEmpty()) {
            String contenu = doc.subText(range.start(), range.end());
            String ferme = caractere && rules.estOuvrant(c) ? String.valueOf(rules.fermantDe(c))
                    : caractere && rules.estCitation(c) ? typed : "";
            if (!ferme.isEmpty()) {
                String insere = typed + contenu + ferme;
                return new TypingGesture(
                        new TextEdit(range.start() + delta, contenu, insere),
                        new SelRange(range.start() + delta + 1, range.end() + delta + 1),
                        delta + insere.length() - contenu.length());
            }
            return new TypingGesture(new TextEdit(range.start() + delta, contenu, typed),
                    SelRange.caretAt(range.start() + delta + typed.length()),
                    delta + typed.length() - contenu.length());
        }
        int p = range.caret();
        int pd = p + delta;
        if (caractere && !autoCloseGuard(doc, p, doc.lineForOffset(p))) {
            return insertionSimple(typed, pd, delta);
        }
        boolean ouvre = caractere && (rules.estOuvrant(c) || rules.estCitation(c));
        boolean saute = caractere && (rules.estFermant(c) || rules.estCitation(c))
                && p < doc.length() && doc.charAt(p) == c;
        if (ouvre) {
            String ferme = rules.estOuvrant(c) ? String.valueOf(rules.fermantDe(c)) : typed;
            String insere = typed + ferme;
            return new TypingGesture(new TextEdit(pd, "", insere), SelRange.caretAt(pd + 1),
                    delta + insere.length());
        }
        if (saute) {
            // le fermant déjà présent est sauté : aucune édition
            return new TypingGesture(null, SelRange.caretAt(pd + 1), delta);
        }
        return new TypingGesture(new TextEdit(pd, "", typed),
                SelRange.caretAt(pd + typed.length()), delta + typed.length());
    }

    /**
     * L'Entrée : le saut porte l'indentation attendue — la profondeur
     * lue dans l'arbre au point de coupe, le style de l'hôte. Le fermant
     * qui reste sur la ligne descend seul sous le curseur : l'Entrée
     * entre accolades produit les deux sauts — le corps au cran de
     * l'ouverture plus un, le fermant au cran de l'ouverture. Une
     * sélection non vide est remplacée par le saut indenté. Sans arbre
     * (la norme à la frappe : la publication suit l'édition), le repli
     * textuel du dépôt de départ prend le relais — l'ouvrant collé au
     * caret gagne son cran, la paire vide s'étend, le fermant rend le
     * retrait de sa ligne — jamais il ne tombe à la colonne zéro.
     *
     * @param doc       le document, jamais muté
     * @param analysis  l'analyse courante, éventuellement absente
     * @param selection la sélection courante
     * @param style     le style d'indentation de l'hôte
     * @return le plan du geste
     */
    public static EditPlan enter(Document doc, @Nullable Analysis analysis,
            MultiSelection selection, IndentStyle style) {
        List<TextEdit> edits = new ArrayList<>(selection.caretCount());
        List<SelRange> apres = new ArrayList<>(selection.caretCount());
        int delta = 0;
        for (SelRange range : selection.ranges()) {
            int coupe = range.isEmpty() ? range.caret() : range.start();
            String retire = doc.subText(range.start(), range.end());
            int line = doc.lineForOffset(coupe);
            String indentation = indentationAttendue(doc, analysis, coupe, line, style);
            // C9 : la continuation de ligne — le « * » de
            // documentation, la puce de liste, repris au saut.
            String continuation = range.isEmpty()
                    ? IndentEngine.lineContinuation(doc, line) : "";
            String reste = doc.subText(coupe, doc.lineEnd(line));
            String insere;
            if (!range.isEmpty()) {
                insere = "\n" + indentation;
            } else {
                String retraitFermant = commenceParFermant(reste)
                        ? retraitDuFermant(doc, analysis, coupe, line, style)
                        : null;
                insere = retraitFermant == null
                        ? "\n" + indentation + continuation
                        : "\n" + indentation + continuation + "\n" + retraitFermant;
            }
            edits.add(new TextEdit(range.start() + delta, retire, insere));
            apres.add(SelRange.caretAt(range.start() + delta + 1
                    + indentation.length() + continuation.length()));
            delta += insere.length() - retire.length();
        }
        return new EditPlan(Transaction.of(edits), MultiSelection.of(apres));
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
        List<TextEdit> edits = new ArrayList<>(selection.caretCount());
        List<SelRange> apres = new ArrayList<>(selection.caretCount());
        int delta = 0;
        for (SelRange range : selection.ranges()) {
            Optional<TypingGesture> geste = retourArrierePlage(doc, range, rules, style, delta);
            if (geste.isEmpty()) {
                // pas de règle : le repli global rend la main au cœur
                return Optional.empty();
            }
            TypingGesture g = geste.get();
            edits.add(g.edit());
            apres.add(g.selection());
            delta = g.delta();
        }
        return Optional.of(new EditPlan(Transaction.of(edits),
                MultiSelection.of(apres)));
    }

    /** Le retour arrière d'une plage : paire vide, citation vide, cran d'indentation. */
    private static Optional<TypingGesture> retourArrierePlage(Document doc, SelRange range,
            TypingRules rules, IndentStyle style, int delta) {
        if (!range.isEmpty()) {
            String retire = doc.subText(range.start(), range.end());
            return Optional.of(new TypingGesture(
                    new TextEdit(range.start() + delta, retire, ""),
                    SelRange.caretAt(range.start() + delta), delta - retire.length()));
        }
        int p = range.caret();
        if (p >= 1 && p < doc.length()) {
            char avant = doc.charAt(p - 1);
            char apresCaret = doc.charAt(p);
            boolean paireVide = rules.estOuvrant(avant) && apresCaret == rules.fermantDe(avant);
            boolean citationsVides = rules.estCitation(avant) && apresCaret == avant;
            if (paireVide || citationsVides) {
                return Optional.of(new TypingGesture(
                        new TextEdit(p - 1 + delta, deuxCaracteres(avant, apresCaret), ""),
                        SelRange.caretAt(p - 1 + delta), delta - 2));
            }
        }
        // C7 (l'analyse croisée) : le collapse des lignes vides —
        // LineActions y sert l'effondrement du saut qui précède.
        Optional<LineActions.TypingGesture> effondrement =
                LineActions.collapseBlankLine(doc, p, delta);
        if (effondrement.isPresent()) {
            LineActions.TypingGesture g = effondrement.get();
            return Optional.of(new TypingGesture(g.edit(), g.selection(), g.delta()));
        }
        return indentStep(doc, p, style, delta);
    }

    /** Le cran d'indentation excédentaire, ou la main au cœur. */
    private static Optional<TypingGesture> indentStep(Document doc, int p, IndentStyle style,
            int delta) {
        int line = doc.lineForOffset(p);
        int start = doc.lineStart(line);
        String blancs = IndentEngine.leadingWhitespace(doc.lineText(line));
        if (p > start && blancs.length() >= p - start) {
            int column = p - start;
            int cran = column % style.width() == 0
                    ? Math.min(style.width(), column)
                    : column % style.width();
            return Optional.of(new TypingGesture(
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
        List<TextEdit> edits = new ArrayList<>(selection.caretCount());
        List<SelRange> resultante = new ArrayList<>(selection.caretCount());
        int delta = 0;
        for (SelRange range : selection.ranges()) {
            String contenu = doc.subText(range.start(), range.end());
            String insere;
            if (parLignes && !contenu.isEmpty()) {
                StringBuilder body = new StringBuilder();
                for (String line : contenu.split("\n", -1)) {
                    if (body.length() > 0) {
                        body.append('\n');
                    }
                    body.append(style.indent(1)).append(line);
                }
                insere = avant + "\n" + body + "\n" + apres;
            } else {
                insere = avant + contenu + apres;
            }
            edits.add(new TextEdit(range.start() + delta, contenu, insere));
            resultante.add(SelRange.caretAt(range.start() + delta + insere.length()));
            delta += insere.length() - contenu.length();
        }
        return new EditPlan(Transaction.of(edits), MultiSelection.of(resultante));
    }

    /**
     * L'indentation attendue d'une coupe : la profondeur de l'arbre au
     * point de coupe, l'indentation de la ligne en repli — l'ouvrant
     * collé au caret y AJOUTE son cran (la discipline du dépôt de
     * départ : l'arbre suit la frappe, jamais il ne devance la main).
     */
    private static String indentationAttendue(Document doc, @Nullable Analysis analysis,
            int coupe, int line, IndentStyle style) {
        if (analysis != null) {
            return style.indent(IndentEngine.depthAtCaret(doc, analysis, coupe));
        }
        String retrait = IndentEngine.leadingWhitespace(doc.lineText(line));
        if (ouvrantColle(doc, coupe) && !dansLitteral(doc, coupe, line)) {
            return retrait + style.indent(1);
        }
        return retrait;
    }

    /**
     * La continuation de ligne (C9, l'analyse croisée) : le préfixe
     * que le saut porte après lui — « * » dans un bloc de
     * documentation ({@code /**} ou {@code * } en tête), la puce de
     * liste ({@code - }, {@code * }, {@code + }) reprise à
     * l'identique. Le dépôt de départ continue pareil ses lignes.
     *
     * @return le préfixe de continuation, chaîne vide sans règle
     */
    /**
     * Le retrait de la ligne du fermant (D-06) : le cran de son ouverture.
     * L'arbre qui englobe la coupe le rend ; sans arbre — la norme à la
     * frappe, la publication suit l'édition — la paire vide collée au
     * caret ({@code {|}}, {@code [|]}, {@code (|)}) restitue le retrait de
     * sa ligne ; sans paire, le fermant ne descend pas : un seul saut,
     * jamais le fermant à la colonne zéro.
     *
     * @return le retrait de la ligne du fermant, null pour le saut simple
     */
    private static @Nullable String retraitDuFermant(Document doc,
            @Nullable Analysis analysis, int coupe, int line, IndentStyle style) {
        if (analysis != null) {
            int profondeur = IndentEngine.depthAtCaret(doc, analysis, coupe);
            if (profondeur >= 1) {
                return style.indent(profondeur - 1);
            }
        }
        if (paireVideAu(doc, coupe) && !dansLitteral(doc, coupe, line)) {
            return IndentEngine.leadingWhitespace(doc.lineText(line));
        }
        return null;
    }

    /** L'insertion simple : la frappe passe telle quelle, le caret
     *  suit — le geste des cas gardés (littéral, balance monstre). */
    private static TypingGesture insertionSimple(String typed, int pd, int delta) {
        return new TypingGesture(new TextEdit(pd, "", typed),
                SelRange.caretAt(pd + typed.length()), delta + typed.length());
    }

    /** Le seuil de balance (C3) : au-delà, le suivi de paires coûte
     *  plus cher que l'éditeur n'y gagne — l'auto-close rend la main.
     *  Le dépôt de départ borne pareil ses filets de paires. */
    private static final int SEUIL_BALANCE = 50_000;

    /**
     * La garde d'auto-close (C3, l'analyse croisée) : vrai quand la
     * frappe d'un ouvrant ou d'une citation à la coupe donnée peut
     * déclencher la paire ou le dédoublement — faux devant un
     * identifiant (l'ouvrant y ouvre un appel), au sein d'un littéral
     * (chaîne ou commentaire : le délimiteur y est du texte) et sur
     * un document dont la balance de crochets dépasse le seuil.
     */
    private static boolean autoCloseGuard(Document doc, int coupe, int line) {
        if (dansLitteralEtendu(doc, coupe, line)) {
            return false;
        }
        return balanceSousSeuil(doc);
    }


    /**
     * Vrai quand la coupe vit dans un littéral ÉTENDU (C3) : la
     * parité des guillemets de la ligne (repli textuel) OU un
     * commentaire de ligne {@code //} ouvrant avant la coupe. Le
     * dépôt de départ consulte son lexeur ; sans publication, la
     * parité textuelle reste le repli fidèle.
     */
    private static boolean dansLitteralEtendu(Document doc, int coupe, int line) {
        if (dansLitteral(doc, coupe, line)) {
            return true;
        }
        int start = doc.lineStart(line);
        String text = doc.lineText(line);
        int barres = text.indexOf("//");
        return barres >= 0 && start + barres < coupe;
    }

    /** La balance de crochets du document reste sous le seuil (C3). */
    private static boolean balanceSousSeuil(Document doc) {
        if (doc.length() <= SEUIL_BALANCE) {
            return true;
        }
        int balance = 0;
        for (int i = 0; i < doc.length(); i++) {
            char c = doc.charAt(i);
            if (c == '(' || c == '[' || c == '{') {
                balance++;
                if (balance > SEUIL_BALANCE) {
                    return false;
                }
            } else if (c == ')' || c == ']' || c == '}') {
                balance--;
            }
        }
        return true;
    }

    /**
     * Vrai quand l'ouvrant collé derrière le caret ferme une paire vide
     * immédiate ({@code {|}}).
     */
    private static boolean paireVideAu(Document doc, int coupe) {
        if (coupe < 1 || coupe >= doc.length()) {
            return false;
        }
        char ouvrant = doc.charAt(coupe - 1);
        char fermant = doc.charAt(coupe);
        return (ouvrant == '{' && fermant == '}')
                || (ouvrant == '[' && fermant == ']')
                || (ouvrant == '(' && fermant == ')');
    }

    /** Vrai quand le caractère collé derrière le caret ouvre un bloc. */
    private static boolean ouvrantColle(Document doc, int coupe) {
        if (coupe < 1) {
            return false;
        }
        char avant = doc.charAt(coupe - 1);
        return avant == '{' || avant == '[' || avant == '(';
    }

    /**
     * Vrai quand la coupe vit dans un littéral de sa ligne : parité des
     * guillemets, échappés exceptés — le repli n'y gagne ni cran ni
     * descente du fermant.
     */
    private static boolean dansLitteral(Document doc, int coupe, int line) {
        boolean guillemet = false;
        for (int i = doc.lineStart(line); i < coupe; i++) {
            char c = doc.charAt(i);
            if (c == '\\' && i + 1 < coupe) {
                i++;
                continue;
            }
            if (c == '"') {
                guillemet = !guillemet;
            }
        }
        return guillemet;
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
    private record TypingGesture(@Nullable TextEdit edit, SelRange selection, int delta) {
    }
}
