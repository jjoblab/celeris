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

    /** Copie la première plage de la sélection au presse-papiers. */
    private static void copier(EditorView vue) {
        SelRange plage = plagePrimaire(vue);
        if (plage == null) {
            return;
        }
        poser(vue, vue.session().document().subText(
                plage.start(), plage.end()));
    }

    /** Coupe : copie puis remplacement de la plage par le vide. */
    private static void couper(EditorView vue) {
        SelRange plage = plagePrimaire(vue);
        if (plage == null) {
            return;
        }
        poser(vue, vue.session().document().subText(
                plage.start(), plage.end()));
        vue.session().replaceRange(plage.start(), plage.end(), "");
        vue.invalidate();
    }

    /** Colle le texte du presse-papiers sur la sélection. */
    private static void coller(EditorView vue) {
        CharSequence texte = lire(vue);
        if (texte == null) {
            return;
        }
        SelRange plage = plagePrimaire(vue);
        if (plage == null) {
            return;
        }
        vue.session().replaceRange(plage.start(), plage.end(),
                texte.toString());
        vue.invalidate();
    }

    /** La première plage de la sélection — null sans plage (un
     *  curseur seul ne copie rien, ne coupe rien). */
    private static SelRange plagePrimaire(EditorView vue) {
        List<SelRange> plages = vue.session().selection().ranges();
        return plages.isEmpty() ? null : plages.get(0);
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
