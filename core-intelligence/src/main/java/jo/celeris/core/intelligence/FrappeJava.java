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
 * La frappe intelligente dédiée à Java (chantier C8) : les deux gestes
 * que le moteur générique ne connaît pas — le découpage des chaînes à
 * l'Entrée (le caret dans un littéral de chaîne ferme la chaîne, pose
 * la concaténation et la rouvre à la ligne) et le cran des labels de
 * cas (l'Entrée qui suit « case … : », « case … -&gt; » ou
 * « default: » gagne un cran — le corps du cas vit sous son label,
 * l'arbre ne le dit pas). Moteur pur : il lit le document, rend un
 * {@link EditPlan}, ne mute jamais — l'application appartient à la
 * session, par la voie unique (ADR-003).
 *
 * <p>Le reste du temps, chaque plage retombe sur le moteur générique
 * ({@link SmartTyping#enter}) — les gestes dédiés ne changent jamais
 * le comportement des autres. La lecture des chaînes est textuelle,
 * ligne par ligne : elle ne dépend d'aucune analyse, elle survit à
 * l'arbre périmé d'une édition à peine frappée.</p>
 */
public final class FrappeJava {

    private FrappeJava() {
    }

    /**
     * L'Entrée dédiée Java : chaque plage de la sélection reçoit son
     * geste — le découpage de chaîne pour un caret dans un littéral,
     * le cran du label de cas pour une coupe en fin de label, le
     * moteur générique pour tout le reste.
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
            final int deltaCumule = delta;
            Geste geste = gesteDedie(doc, plage, style, deltaCumule)
                    .orElseGet(() -> gesteGenerique(doc, analysis, plage,
                            style, deltaCumule));
            editions.add(geste.edition());
            apres.add(geste.selection());
            delta = geste.delta();
        }
        return new EditPlan(Transaction.of(editions), MultiSelection.of(apres));
    }

    /**
     * Le geste dédié d'une plage, s'il y en a un : le découpage de
     * chaîne, sinon le cran de label de cas.
     */
    private static Optional<Geste> gesteDedie(Document doc, SelRange plage,
            IndentStyle style, int delta) {
        int coupe = plage.isEmpty() ? plage.caret() : plage.start();
        if (plage.isEmpty()) {
            Optional<Geste> decoupe = decoupeChaine(doc, coupe, style, delta);
            if (decoupe.isPresent()) {
                return decoupe;
            }
        }
        return cranLabelDeCas(doc, coupe, plage, style, delta);
    }

    /**
     * Le geste générique d'une plage : le plan du moteur générique
     * pour cette plage seule, relogé au delta cumulé — une plage
     * toujours exactement une édition, un caret.
     */
    private static Geste gesteGenerique(Document doc, @Nullable Analysis analysis,
            SelRange plage, IndentStyle style, int delta) {
        EditPlan plan = SmartTyping.enter(doc, analysis,
                MultiSelection.of(plage), style);
        TextEdit edition = plan.transaction().edits().get(0);
        TextEdit reloge = new TextEdit(edition.start() + delta,
                edition.removed(), edition.inserted());
        int caret = plan.selection().carets().get(0) + delta;
        return new Geste(reloge, SelRange.caretAt(caret),
                delta + edition.inserted().length() - edition.removed().length());
    }

    /**
     * Le découpage d'une chaîne à l'Entrée : le caret vit STRICTEMENT
     * dans un littéral de chaîne de sa ligne — après la citation
     * ouvrante, avant la fermante, jamais dans une séquence d'échappée
     * — la chaîne se ferme au caret, la concaténation se pose, la
     * chaîne se rouvre à la ligne, un cran sous la sienne. Le caret
     * se pose dans la chaîne rouverte, après sa citation ouvrante.
     *
     * <p>Les blocs de texte ({@code """}) refusent — leur découpage
     * n'est pas une concaténation. Le littéral non fermé sur la ligne
     * refuse — on ne découpe pas une chaîne ouverte. La ligne de
     * commentaire ({@code //}, {@code /*}) ne compte pas.</p>
     *
     * @param doc   le document, jamais muté
     * @param caret la position du curseur
     * @param style le style d'indentation de l'hôte
     * @param delta le delta cumulé des gestes précédents
     * @return le geste de découpage, vide pour laisser la main
     */
    private static Optional<Geste> decoupeChaine(Document doc, int caret,
            IndentStyle style, int delta) {
        int ligne = doc.lineForOffset(caret);
        int debut = doc.lineStart(ligne);
        if (caret <= debut || caret > doc.lineEnd(ligne)) {
            return Optional.empty();
        }
        EtatChaine etat = etatChaineAvant(doc, debut, caret);
        if (!etat.dansChaine || etat.echappe) {
            return Optional.empty();
        }
        if (!chaineFermeeApres(doc, caret, doc.lineEnd(ligne))) {
            return Optional.empty();
        }
        String indentation = IndentEngine.leadingWhitespace(doc.lineText(ligne))
                + style.indent(1);
        String insere = "\" +\n" + indentation + "\"";
        int position = caret + delta;
        return Optional.of(new Geste(new TextEdit(position, "", insere),
                SelRange.caretAt(position + insere.length()),
                delta + insere.length()));
    }

    /** L'état des citations d'une ligne à une position. */
    private record EtatChaine(boolean dansChaine, boolean echappe) {
    }

    /**
     * L'état des citations avant le caret : la parité des citations
     * non échappées dit si le caret vit dans une chaîne ; l'échappée
     * pendante dit s'il vit dans une séquence. Le bloc de texte
     * ({@code """}) ouvre trois fois la citation — l'état devient
     * faux, le découpage refuse.
     */
    private static EtatChaine etatChaineAvant(Document doc, int debut,
            int caret) {
        boolean dansChaine = false;
        boolean echappe = false;
        for (int i = debut; i < caret; i++) {
            char c = doc.charAt(i);
            if (echappe) {
                echappe = false;
                continue;
            }
            if (c == '\\') {
                echappe = true;
                continue;
            }
            if (c == '"') {
                if (!dansChaine && estBlocTexte(doc, i)) {
                    // trois citations : l'état de parité ne dit plus rien
                    return new EtatChaine(false, true);
                }
                dansChaine = !dansChaine;
            }
        }
        return new EtatChaine(dansChaine, echappe);
    }

    /** Vrai quand la citation à {@code i} ouvre un bloc de texte. */
    private static boolean estBlocTexte(Document doc, int i) {
        return i + 2 < doc.length()
                && doc.charAt(i + 1) == '"' && doc.charAt(i + 2) == '"';
    }

    /**
     * Vrai quand la chaîne ouverte au caret se ferme sur sa ligne :
     * une citation non échappée vit entre le caret et la fin de ligne.
     */
    private static boolean chaineFermeeApres(Document doc, int caret, int fin) {
        boolean echappe = false;
        for (int i = caret; i < fin; i++) {
            char c = doc.charAt(i);
            if (echappe) {
                echappe = false;
                continue;
            }
            if (c == '\\') {
                echappe = true;
                continue;
            }
            if (c == '"') {
                return true;
            }
        }
        return false;
    }

    /**
     * Le cran des labels de cas : la coupe tombe en fin de label —
     * le texte de la ligne jusqu'à la coupe, dépouillé de ses blancs
     * de queue, finit par « : » ou « -&gt; » et commence par le mot
     * clé « case » ou « default ». La nouvelle ligne gagne un cran
     * sur le label : le corps du cas vit sous son étiquette.
     *
     * <p>La ligne de commentaire ou de doc ne compte pas — un
     * « // case X: » n'est pas un label. Le mot de tête seul
     * décide : « integer x: » n'est pas un label non plus.</p>
     *
     * @param doc   le document, jamais muté
     * @param coupe la position de coupe
     * @param plage la plage remplacée par le saut
     * @param style le style d'indentation de l'hôte
     * @param delta le delta cumulé des gestes précédents
     * @return le geste du cran, vide pour laisser la main
     */
    private static Optional<Geste> cranLabelDeCas(Document doc, int coupe,
            SelRange plage, IndentStyle style, int delta) {
        int ligne = doc.lineForOffset(coupe);
        int debut = doc.lineStart(ligne);
        String avant = doc.subText(debut, coupe).stripTrailing();
        if (avant.isBlank()) {
            return Optional.empty();
        }
        if (avant.startsWith("//") || avant.startsWith("/*")
                || avant.startsWith("*")) {
            return Optional.empty();
        }
        if (!avant.endsWith(":") && !avant.endsWith("->")) {
            return Optional.empty();
        }
        String motDeTete = motDeTete(avant.stripLeading());
        if (!"case".equals(motDeTete) && !"default".equals(motDeTete)) {
            return Optional.empty();
        }
        String indentation = IndentEngine.leadingWhitespace(doc.lineText(ligne))
                + style.indent(1);
        String retire = doc.subText(plage.start(), plage.end());
        String insere = "\n" + indentation;
        return Optional.of(new Geste(
                new TextEdit(plage.start() + delta, retire, insere),
                SelRange.caretAt(plage.start() + delta + insere.length()),
                delta + insere.length() - retire.length()));
    }

    /**
     * Le premier mot d'un texte : ses caractères d'identifiant de
     * tête, en minuscules — le mot clé qui décide.
     */
    private static String motDeTete(String texte) {
        StringBuilder mot = new StringBuilder();
        for (int i = 0; i < texte.length(); i++) {
            char c = texte.charAt(i);
            if (!Character.isJavaIdentifierPart(c)) {
                break;
            }
            mot.append(Character.toLowerCase(c));
        }
        return mot.toString();
    }

    /** Le geste d'une plage : édition, sélection d'après, delta cumulé. */
    private record Geste(TextEdit edition, SelRange selection, int delta) {
    }
}
