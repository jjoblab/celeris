package jo.celeris.android.view.internal.input;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import jo.celeris.android.view.EditorAction;
import jo.celeris.android.view.internal.access.EditorAccess;

import java.util.List;

import jo.celeris.core.text.SelRange;

/**
 * Les actions presse-papiers du clavier (C-12, l'analyse croisée) :
 * Ctrl+C copie, Ctrl+X coupe, Ctrl+V colle — sur le presse-papiers
 * SYSTÈME, comme la barre de sélection tactile (le même service, la
 * même vérité). La pose est bornée comme celle de la barre : la
 * transaction Binder ne porte pas un document monstre.
 *
 * <p>C-13 : le presse-papiers est MULTI-CURSEURS. Copier emporte les
 * segments de TOUTES les plages, joints par saut de ligne ; coller
 * DISTRIBUE le clip quand il a autant de lignes que de curseurs
 * (ligne i sur curseur i), sinon le clip entier se pose sur chaque
 * curseur — la même sémantique que les éditeurs modernes, la parité
 * avec le dépôt de départ.</p>
 *
 * <p>Porteur mince : la vue y délègue ses actions clavier, la barre
 * de sélection garde sa voie tactile propre — jamais deux sources de
 * vérité du presse-papiers.</p>
 */
public final class KeyboardClipboard  {

    /** Le plafond des copies, en caractères (A-20 : TransactionTooLarge
     *  au-delà d'~1 Mo d'IPC ; le même plafond que la barre). */
    private static final int MAX_CLIPBOARD_CHARS = 200_000;

    private KeyboardClipboard() {
    }

    /**
     * Sert l'action presse-papiers demandée sur la session de la vue.
     *
     * @param vue    la vue propriétaire de la session
     * @param action COPY, CUT ou PASTE
     */
    static void servir(EditorAccess acces, EditorAction action) {
        switch (action) {
            case COPY -> copier(acces);
            case CUT -> couper(acces);
            case PASTE -> coller(acces);
            default -> {
            }
        }
    }

    /** Copie les segments de toutes les plages (C-13) : joints par
     *  saut de ligne, dans l'ordre du document — un seul curseur
     *  reproduit la copie simple, sans jointure parasite. */
    private static void copier(EditorAccess acces) {
        String joint = segmentsJoints(acces);
        if (joint == null) {
            return;
        }
        place(acces, joint);
    }

    /** Coupe : copie les segments joints puis remplace chaque plage
     *  par le vide (C-13 : les DEUX gestes sont multi). */
    private static void couper(EditorAccess acces) {
        String joint = segmentsJoints(acces);
        if (joint == null) {
            return;
        }
        place(acces, joint);
        acces.session().replaceEach(java.util.Collections.nCopies(
                plagesEtendues(acces).size(), ""));
        acces.invalidate();
    }

    /** Les segments des plages ÉTENDUES, joints par \n — null si la
     *  sélection n'a aucune plage étendue (les curseurs seuls ne
     *  copient rien). */
    private static String segmentsJoints(EditorAccess acces) {
        List<SelRange> ranges = plagesEtendues(acces);
        if (ranges.isEmpty()) {
            return null;
        }
        StringBuilder joint = new StringBuilder();
        for (SelRange range : ranges) {
            if (joint.length() > 0) {
                joint.append('\n');
            }
            joint.append(acces.session().document().subText(
                    range.start(), range.end()));
        }
        return joint.toString();
    }

    /** Les plages étendues de la sélection (les curseurs réduits ne
     *  copient rien, ne coupent rien). */
    private static List<SelRange> plagesEtendues(EditorAccess acces) {
        List<SelRange> etendues = new java.util.ArrayList<>();
        for (SelRange range : acces.session().selection().ranges()) {
            if (!range.isEmpty()) {
                etendues.add(range);
            }
        }
        return etendues;
    }

    /** Colle le texte du presse-papiers sur la sélection (C-13) :
     *  un clip à autant de LIGNES que de plages se distribue (ligne
     *  i sur plage i), sinon le clip entier se pose sur chaque
     *  plage — le multi-curseurs du dépôt de départ. */
    private static void coller(EditorAccess acces) {
        CharSequence text = read(acces);
        if (text == null) {
            return;
        }
        List<SelRange> ranges = acces.session().selection().ranges();
        if (ranges.isEmpty()) {
            return;
        }
        List<String> lines = java.util.Arrays.asList(
                text.toString().split("\n", -1));
        List<String> parts;
        if (lines.size() == ranges.size() && ranges.size() > 1) {
            // distribution : chaque plage reçoit SA ligne
            parts = lines;
        } else {
            // en masse : le clip entier partout
            parts = java.util.Collections.nCopies(ranges.size(),
                    text.toString());
        }
        acces.session().replaceEach(parts);
        acces.invalidate();
    }

    /** Pose le texte au presse-papiers système, borné au plafond. */
    private static void place(EditorAccess acces, String text) {
        Object service = acces.getContext()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        if (service instanceof ClipboardManager pressePapiers) {
            pressePapiers.setPrimaryClip(ClipData.newPlainText("celeris",
                    text.length() > MAX_CLIPBOARD_CHARS
                            ? text.substring(0, MAX_CLIPBOARD_CHARS)
                            : text));
        }
    }

    /** Le texte du presse-papiers système, ou null (vide ou absent). */
    private static CharSequence read(EditorAccess acces) {
        Object service = acces.getContext()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        if (!(service instanceof ClipboardManager pressePapiers)) {
            return null;
        }
        ClipData contenu = pressePapiers.getPrimaryClip();
        if (contenu == null || contenu.getItemCount() == 0) {
            return null;
        }
        return contenu.getItemAt(0).coerceToText(acces.getContext());
    }
}
