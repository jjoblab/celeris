package jo.celeris.core.intelligence;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.TextEdit;
import jo.celeris.core.text.Transaction;

/**
 * Le renommage lié des balises XML (C8) : pendant l'édition du NOM
 * d'une balise OUVRANTE — frappe d'un caractère ou retour arrière,
 * caret au milieu ou en fin de nom — la balise FERMANTE
 * correspondante se met à jour en miroir : même caractère
 * inséré/supprimé à la position alignée du nom fermant. Les deux
 * éditions voyagent dans UN SEUL lot d'annulation, appliquées par
 * offsets décroissants.
 *
 * <p>L'appariement ouvrante vers fermante est un empilement par
 * nom : les balises homonymes imbriquées sont respectées
 * ({@code <div><div></div></div>}), les balises auto-fermantes ne
 * comptent pas, le scan saute les commentaires, les sections CDATA
 * et les processing instructions. L'édition du nom FERMANT ne
 * touche jamais l'ouvrante : la liaison est ouvrante vers fermante,
 * jamais l'inverse — l'utilisateur qui édite la fermante sait ce
 * qu'il fait. Le caret dans un attribut, une valeur ou du texte
 * écarte la liaison ; la fermante introuvable écarte la liaison.</p>
 *
 * <p>L'algorithme descend des éditions liées du dépôt de départ
 * (registre de portage, C8 — Spécifier : l'ancien sert d'oracle).</p>
 */
public final class XmlLinkedTags {

    /** Bornes du scan avant (appariement de la fermante). */
    private static final int MATCH_SCAN_LIMIT = 200_000;

    /** Bornes du scan arrière (recherche du «&nbsp;&lt;&nbsp;» ouvrant). */
    private static final int OPEN_SCAN_LIMIT = 10_000;

    private XmlLinkedTags() {
    }

    /**
     * La frappe liée d'un caractère dans un nom de balise ouvrante :
     * le caractère s'insère au caret et son miroir à la position
     * alignée du nom fermant.
     *
     * @param doc   le document, jamais muté
     * @param caret le caret, dans le nom ouvrant
     * @param ch    le caractère frappé
     * @return le plan du geste lié, vide hors liaison
     */
    public static Optional<EditPlan> typeChar(Document doc, int caret, String ch) {
        Targets cibles = find(doc, caret);
        if (cibles == null) {
            return Optional.empty();
        }
        int miroir = cibles.closeNameStart + (caret - cibles.nameStart);
        return Optional.of(new EditPlan(Transaction.of(List.of(
                new TextEdit(miroir, "", ch),
                new TextEdit(caret, "", ch))),
                MultiSelection.caretsAt(caret + ch.length(), miroir + ch.length())));
    }

    /**
     * Le retour arrière lié dans un nom de balise ouvrante : le
     * caractère avant le caret se supprime et son miroir à la
     * position alignée du nom fermant.
     *
     * @param doc   le document, jamais muté
     * @param caret le caret, le caractère à supprimer est avant
     * @return le plan du geste lié, vide hors liaison ou au début du nom
     */
    public static Optional<EditPlan> backspace(Document doc, int caret) {
        Targets cibles = find(doc, caret);
        if (cibles == null || caret <= cibles.nameStart) {
            return Optional.empty();
        }
        int delta = caret - cibles.nameStart;
        int ouvrant = caret - 1;
        int fermant = cibles.closeNameStart + delta - 1;
        return Optional.of(new EditPlan(Transaction.of(List.of(
                new TextEdit(fermant, doc.subText(fermant, fermant + 1), ""),
                new TextEdit(ouvrant, doc.subText(ouvrant, ouvrant + 1), ""))),
                MultiSelection.caretsAt(ouvrant, fermant)));
    }

    /** Les cibles appariées : débuts des noms ouvrant et fermant. */
    private static final class Targets {
        final int nameStart;
        final int closeNameStart;

        Targets(int nameStart, int closeNameStart) {
            this.nameStart = nameStart;
            this.closeNameStart = closeNameStart;
        }
    }

    /**
     * Trouve le nom de balise ouvrante qui contient le caret (bornes
     * incluses) et sa fermante appariée — ou null.
     */
    private static @Nullable Targets find(Document doc, int caret) {
        if (caret <= 0 || caret > doc.length()) {
            return null;
        }
        int lt = openBefore(doc, caret);
        if (lt < 0) {
            return null;
        }
        int nameStart = lt + 1;
        if (nameStart >= doc.length() || doc.charAt(nameStart) == '/') {
            return null;
        }
        int nameEnd = nameStart;
        while (nameEnd < doc.length() && isXmlNameChar(doc.charAt(nameEnd))) {
            nameEnd++;
        }
        if (nameEnd == nameStart || caret < nameStart || caret > nameEnd) {
            return null;
        }
        String name = doc.subText(nameStart, nameEnd);
        int tagEnd = scanTagEnd(doc, nameEnd);
        if (tagEnd < 0 || doc.charAt(tagEnd - 1) == '/') {
            return null;
        }
        return stackMatch(doc, tagEnd + 1, name, nameStart);
    }

    /**
     * Le «&nbsp;&lt;&nbsp;» ouvrant le plus proche avant le caret —
     * une espace, un guillemet ou un «&nbsp;&gt;&nbsp;» avant de
     * l'atteindre écarte la liaison (attribut, valeur ou texte).
     */
    private static int openBefore(Document doc, int caret) {
        int limite = Math.max(0, caret - OPEN_SCAN_LIMIT);
        for (int i = caret - 1; i >= limite; i--) {
            char c = doc.charAt(i);
            if (c == '<') {
                return i;
            }
            if (c == '>' || Character.isWhitespace(c) || c == '"' || c == '\'') {
                return -1;
            }
        }
        return -1;
    }

    /**
     * L'empilement par nom jusqu'à la fermante appariée : les
     * homonymes imbriqués empilent, les auto-fermantes ne comptent
     * pas, les commentaires, CDATA et processing instructions se
     * sautent.
     */
    private static @Nullable Targets stackMatch(Document doc, int from, String name,
            int nameStart) {
        int depth = 1;
        int p = from;
        int limit = Math.min(doc.length(), from + MATCH_SCAN_LIMIT);
        while (p < limit) {
            char c = doc.charAt(p);
            if (c != '<') {
                p++;
                continue;
            }
            if (suivant(doc, p, limit, '!')) {
                p = skipUntil(doc, p + 2, limit, "-->", "]]>");
                continue;
            }
            if (suivant(doc, p, limit, '?')) {
                p = skipUntil(doc, p + 2, limit, "?>", "?>");
                continue;
            }
            if (suivant(doc, p, limit, '/')) {
                int[] passe = closePass(doc, p, limit, name, depth);
                if (passe == null) {
                    return null;
                }
                if (passe[1] == 0) {
                    return new Targets(nameStart, passe[2]);
                }
                depth = passe[1];
                p = passe[0];
                continue;
            }
            int[] passe = openPass(doc, p, limit, name, depth);
            if (passe == null) {
                return null;
            }
            depth = passe[1];
            p = passe[0];
        }
        return null;
    }

    /**
     * La passe d'une balise fermante : la prochaine position, la
     * profondeur qui en résulte, le début du nom — ou null si la
     * balise est mal formée.
     */
    private static int @Nullable [] closePass(Document doc, int p, int limit,
            String name, int depth) {
        int cn = p + 2;
        int cnEnd = cn;
        while (cnEnd < limit && isXmlNameChar(doc.charAt(cnEnd))) {
            cnEnd++;
        }
        boolean appariee = doc.subText(cn, cnEnd).equals(name);
        int end = scanTagEnd(doc, Math.max(cnEnd, p + 2));
        if (end < 0) {
            return null;
        }
        return new int[]{end + 1, appariee ? depth - 1 : depth, cn};
    }

    /**
     * La passe d'une balise ouvrante : la prochaine position, la
     * profondeur qui en résulte — l'homonyme non auto-fermante empile —
     * ou null si la balise est mal formée.
     */
    private static int @Nullable [] openPass(Document doc, int p, int limit,
            String name, int depth) {
        int on = p + 1;
        int onEnd = on;
        while (onEnd < limit && isXmlNameChar(doc.charAt(onEnd))) {
            onEnd++;
        }
        int end = scanTagEnd(doc, Math.max(onEnd, p + 1));
        if (end < 0) {
            return null;
        }
        boolean empile = doc.charAt(end - 1) != '/' && doc.subText(on, onEnd).equals(name);
        return new int[]{end + 1, empile ? depth + 1 : depth};
    }

    /** Vrai quand le caractère suivant le «&nbsp;&lt;&nbsp;» est celui donné. */
    private static boolean suivant(Document doc, int p, int limit, char c) {
        return p + 1 < limit && doc.charAt(p + 1) == c;
    }

    /**
     * La position du «&nbsp;&gt;&nbsp;» fermant la balise qui démarre
     * à partir de from — les guillemets d'attributs sont respectés.
     */
    private static int scanTagEnd(Document doc, int from) {
        boolean quote = false;
        for (int i = from; i < doc.length(); i++) {
            char c = doc.charAt(i);
            if (quote) {
                if (c == '"') {
                    quote = false;
                }
                continue;
            }
            if (c == '"') {
                quote = true;
                continue;
            }
            if (c == '>') {
                return i;
            }
        }
        return -1;
    }

    /** La position après le premier terminateur trouvé, la limite sinon. */
    private static int skipUntil(Document doc, int from, int limit,
            String premier, String second) {
        for (int i = from; i < limit; i++) {
            if (commence(doc, i, limit, premier) || commence(doc, i, limit, second)) {
                return i + premier.length();
            }
        }
        return limit;
    }

    /** Vrai quand le texte au offset commence par le terminateur donné. */
    private static boolean commence(Document doc, int offset, int limit, String fin) {
        if (offset + fin.length() > limit) {
            return false;
        }
        return doc.subText(offset, offset + fin.length()).equals(fin);
    }

    /** Vrai pour un caractère de nom XML : lettre, chiffre ou les séparateurs usuels. */
    private static boolean isXmlNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.' || c == ':';
    }
}
