package jo.celeris.android.view;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

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
final class PressePapiersClavier {

    /** Le plafond des copies, en caractères (A-20 : TransactionTooLarge
     *  au-delà d'~1 Mo d'IPC ; le même plafond que la barre). */
    private static final int MAX_CLIPBOARD_CHARS = 200_000;

    private PressePapiersClavier() {
    }

    /**
     * Sert l'action presse-papiers demandée sur la session de la vue.
     *
     * @param vue    la vue propriétaire de la session
     * @param action COPY, CUT ou PASTE
     */
    static void servir(EditorView vue, EditorAction action) {
        switch (action) {
            case COPY -> copier(vue);
            case CUT -> couper(vue);
            case PASTE -> coller(vue);
            default -> {
            }
        }
    }

    /** Copie les segments de toutes les plages (C-13) : joints par
     *  saut de ligne, dans l'ordre du document — un seul curseur
     *  reproduit la copie simple, sans jointure parasite. */
    private static void copier(EditorView vue) {
        String joint = segmentsJoints(vue);
        if (joint == null) {
            return;
        }
        poser(vue, joint);
    }

    /** Coupe : copie les segments joints puis remplace chaque plage
     *  par le vide (C-13 : les DEUX gestes sont multi). */
    private static void couper(EditorView vue) {
        String joint = segmentsJoints(vue);
        if (joint == null) {
            return;
        }
        poser(vue, joint);
        vue.session().replaceEach(java.util.Collections.nCopies(
                plagesEtendues(vue).size(), ""));
        vue.invalidate();
    }

    /** Les segments des plages ÉTENDUES, joints par \n — null si la
     *  sélection n'a aucune plage étendue (les curseurs seuls ne
     *  copient rien). */
    private static String segmentsJoints(EditorView vue) {
        List<SelRange> plages = plagesEtendues(vue);
        if (plages.isEmpty()) {
            return null;
        }
        StringBuilder joint = new StringBuilder();
        for (SelRange plage : plages) {
            if (joint.length() > 0) {
                joint.append('\n');
            }
            joint.append(vue.session().document().subText(
                    plage.start(), plage.end()));
        }
        return joint.toString();
    }

    /** Les plages étendues de la sélection (les curseurs réduits ne
     *  copient rien, ne coupent rien). */
    private static List<SelRange> plagesEtendues(EditorView vue) {
        List<SelRange> etendues = new java.util.ArrayList<>();
        for (SelRange plage : vue.session().selection().ranges()) {
            if (!plage.isEmpty()) {
                etendues.add(plage);
            }
        }
        return etendues;
    }

    /** Colle le texte du presse-papiers sur la sélection (C-13) :
     *  un clip à autant de LIGNES que de plages se distribue (ligne
     *  i sur plage i), sinon le clip entier se pose sur chaque
     *  plage — le multi-curseurs du dépôt de départ. */
    private static void coller(EditorView vue) {
        CharSequence texte = lire(vue);
        if (texte == null) {
            return;
        }
        List<SelRange> plages = vue.session().selection().ranges();
        if (plages.isEmpty()) {
            return;
        }
        List<String> lignes = java.util.Arrays.asList(
                texte.toString().split("\n", -1));
        List<String> parts;
        if (lignes.size() == plages.size() && plages.size() > 1) {
            // distribution : chaque plage reçoit SA ligne
            parts = lignes;
        } else {
            // en masse : le clip entier partout
            parts = java.util.Collections.nCopies(plages.size(),
                    texte.toString());
        }
        vue.session().replaceEach(parts);
        vue.invalidate();
    }

    /** Pose le texte au presse-papiers système, borné au plafond. */
    private static void poser(EditorView vue, String texte) {
        Object service = vue.getContext()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        if (service instanceof ClipboardManager pressePapiers) {
            pressePapiers.setPrimaryClip(ClipData.newPlainText("celeris",
                    texte.length() > MAX_CLIPBOARD_CHARS
                            ? texte.substring(0, MAX_CLIPBOARD_CHARS)
                            : texte));
        }
    }

    /** Le texte du presse-papiers système, ou null (vide ou absent). */
    private static CharSequence lire(EditorView vue) {
        Object service = vue.getContext()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        if (!(service instanceof ClipboardManager pressePapiers)) {
            return null;
        }
        ClipData contenu = pressePapiers.getPrimaryClip();
        if (contenu == null || contenu.getItemCount() == 0) {
            return null;
        }
        return contenu.getItemAt(0).coerceToText(vue.getContext());
    }
}
