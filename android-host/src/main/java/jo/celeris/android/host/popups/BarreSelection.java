package jo.celeris.android.host.popups;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.view.ReglagesEditeur;
import jo.celeris.android.widgets.MenuDebordement;
import jo.celeris.android.widgets.MenuEtages;
import jo.celeris.android.widgets.PopupAnchor;
import jo.celeris.android.widgets.PopupController;
import jo.celeris.android.widgets.PopupManager;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.android.widgets.PopupPlacer;
import jo.celeris.android.widgets.SelectionBarView;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

/**
 * Le satellite de la barre de sélection (la pose quitte le corps de
 * l'orchestrateur, le patron des satellites — 2026-11-14) : la
 * barre, son menu de débordement et leurs gestes. Les actions se
 * posent VIVES — relues à CHAQUE pose : Coller ne se propose que
 * lorsque le presse-papiers de l'hôte est plein, l'option inutile ne
 * se promène jamais sur la pilule (l'audit du propriétaire du
 * 2026-11-14 : « surtout coller ne doit être visible que quand il
 * est nécessaire »). Le menu de débordement se pose SOUS LA
 * SÉLECTION, jamais sur elle — le caret demeure à découvert (le même
 * audit : « assures-toi que les popups ne se marchent pas dessus par
 * rapport au curseur, le caret »).
 *
 * <p>Le débordement vit en ÉTAGES CONTEXTUELS (le même audit : « je
 * veux le séparer en étages ou colonnes qui deviennent visibles
 * contextuellement comme les go to, l'option usages, rename ») : les
 * étages se relisent à CHAQUE ouverture — l'étage de navigation ne se
 * montre que sur un symbole au caret, l'étage d'édition vit toujours,
 * l'étage VIDE ne se montre jamais.</p>
 */
final class BarreSelection {

    private final EditorView editeur;
    private final TextSession session;
    private final PopupManager popups;
    private final SelectionBarView barre;
    private final MenuDebordement menu;
    private final int margePx;
    /** Les actions de la barre, VIVES au déclenchement — l'hôte qui
     *  n'en pose aucune sert les actions d'édition STANDARD (couper,
     *  copier, coller, tout copier : la revue du propriétaire du
     *  2026-11-19, « l'éditeur doit les fournir de base »). */
    private Supplier<List<SelectionBarView.Item>> actions = this::actionsEditionStandard;
    /** Les étages du débordement — vifs à l'ouverture, la visibilité
     *  contextuelle naît de la donnée. */
    private Supplier<List<MenuEtages.Etage>> debordement = List::of;

    /**
     * Construit le satellite sur le coordinateur des popups : la barre
     * naît liée à son genre, le menu de débordement au sien.
     *
     * @param editeur la vue d'éditeur ouverte (bornes de sélection)
     * @param session la session d'édition courante
     * @param popups   le coordinateur des popups de la scène
     * @param palette  la palette courante des popups
     * @param margePx  la respiration des ancres aux bords, en pixels
     * @param docs     le geste du « i » de documentation de l'hôte
     */
    BarreSelection(EditorView editeur, TextSession session,
            PopupManager popups, PopupPalette palette, int margePx,
            Runnable docs) {
        this.editeur = editeur;
        this.session = session;
        this.popups = popups;
        this.barre = new SelectionBarView(popups.couche().getContext(),
                palette);
        popups.lier(PopupController.PopupKind.SELECTION_BAR, barre);
        this.menu = new MenuDebordement(popups, palette, margePx);
        this.margePx = margePx;
        barre.setOnOverflow(this::ouvrirDebordement);
        barre.setOnDocs(docs);
    }

    /**
     * Pose les actions de la barre et les étages de son débordement —
     * les listes posées deviennent FIXES, la variante VIVE relit
     * l'état du terrain à chaque pose.
     *
     * @param principales les actions visibles de la barre
     * @param repliees    les étages repliés, servis par le menu
     */
    void configurerActions(List<SelectionBarView.Item> principales,
            List<MenuEtages.Etage> repliees) {
        actions = () -> principales;
        debordement = () -> repliees;
    }

    /**
     * Pose les actions VIVES de la barre et les étages de son
     * débordement — l'hôte relit ses fournisseurs à CHAQUE pose :
     * Coller ne se propose que lorsque le presse-papiers est plein,
     * l'étage de navigation ne se montre que sur un symbole au caret.
     *
     * @param principales le fournisseur des actions visibles
     * @param repliees le fournisseur des étages repliés
     */
    void configurerActionsVives(
            Supplier<List<SelectionBarView.Item>> principales,
            Supplier<List<MenuEtages.Etage>> repliees) {
        actions = principales;
        debordement = repliees;
    }

    /**
     * Les actions d'édition STANDARD de la barre — le DÉFAUT du
     * montage (la revue du propriétaire du 2026-11-19 : couper,
     * copier, coller « s'il existe quelque chose à coller », copier
     * tout) : la sélection se copie au PRESSE-PAPIERS SYSTÈME, la
     * barre se re-pose après chaque geste (Coller se propose dès que
     * le presse-papiers vit). Le « i » et les « ... » restent ceux du
     * montage (documentation, débordement) — l'hôte qui pose SES
     * actions remplace la rangée entière.
     *
     * @return les pastilles d'édition, au presse-papiers près
     */
    private List<SelectionBarView.Item> actionsEditionStandard() {
        List<SelectionBarView.Item> items = new ArrayList<>(4);
        items.add(new SelectionBarView.Item("Couper",
                jo.celeris.android.widgets.IconeAction.COUPER, this::couper));
        items.add(new SelectionBarView.Item("Copier",
                jo.celeris.android.widgets.IconeAction.COPIER, this::copier));
        if (!pressePapiersVide()) {
            items.add(new SelectionBarView.Item("Coller",
                    jo.celeris.android.widgets.IconeAction.COLLER, this::coller));
        }
        items.add(new SelectionBarView.Item("Tout copier",
                jo.celeris.android.widgets.IconeAction.TOUT, this::toutCopier));
        return items;
    }

    /** Les actions courantes de la barre — la sonde du banc : le
     *  fournisseur y lit sans poser la barre. */
    java.util.List<SelectionBarView.Item> actionsDeBarre() {
        return actions.get();
    }

    /** Le presse-papiers système est-il vide — sans hôte, sans texte,
     *  la pastille Coller ne vit pas. */
    private boolean pressePapiersVide() {
        Object service = popups.couche().getContext()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        if (!(service instanceof ClipboardManager pressePapiers)) {
            return true;
        }
        ClipData contenu = pressePapiers.getPrimaryClip();
        return contenu == null || contenu.getItemCount() == 0
                || contenu.getItemAt(0) == null
                || contenu.getItemAt(0).coerceToText(
                        popups.couche().getContext()) == null;
    }

    /** Le plafond des copies au presse-papiers, en caractères : la
     *  transaction IPC d'Android plafonne à ~1 Mo (A-20, l'analyse
     *  croisée) — au-delà, le système tue l'appelant
     *  (TransactionTooLargeException). Le dépôt de départ borne à
     *  200 000 : pareil ici, la copie tronque au plafond. */
    static final int MAX_CLIPBOARD_CHARS = 200_000;

    /** Copie la sélection au presse-papiers système. */
    private void copier() {
        SelRange plage = plagePrimaire();
        if (plage == null) {
            return;
        }
        poserAuPressePapiers(session.document().subText(
                plage.start(), plage.end()));
    }

    /** Coupe la sélection : copie puis remplacement par le vide. */
    private void couper() {
        SelRange plage = plagePrimaire();
        if (plage == null) {
            return;
        }
        poserAuPressePapiers(session.document().subText(
                plage.start(), plage.end()));
        session.replaceRange(plage.start(), plage.end(), "");
        editeur.invalidate();
    }

    /** Colle le presse-papiers système sur la sélection. */
    private void coller() {
        Object service = popups.couche().getContext()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        if (!(service instanceof ClipboardManager pressePapiers)) {
            return;
        }
        ClipData contenu = pressePapiers.getPrimaryClip();
        if (contenu == null || contenu.getItemCount() == 0) {
            return;
        }
        CharSequence texte = contenu.getItemAt(0).coerceToText(
                popups.couche().getContext());
        if (texte == null) {
            return;
        }
        SelRange plage = plagePrimaire();
        if (plage == null) {
            return;
        }
        session.replaceRange(plage.start(), plage.end(), texte.toString());
        editeur.invalidate();
    }

    /** Copie TOUT le document au presse-papiers système — « copier
     *  tout », la sélection devient le document entier. */
    private void toutCopier() {
        poserAuPressePapiers(session.document().text());
    }

    /** Pose le texte au presse-papiers système puis re-pose la barre :
     *  Coller vient de se proposer (ou se retirer). La pose est
     *  BORNÉE au plafond : la transaction Binder ne porte pas un
     *  document monstre (A-20). */
    private void poserAuPressePapiers(String texte) {
        Object service = popups.couche().getContext()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        if (service instanceof ClipboardManager pressePapiers) {
            pressePapiers.setPrimaryClip(ClipData.newPlainText("celeris",
                    texte.length() > MAX_CLIPBOARD_CHARS
                            ? texte.substring(0, MAX_CLIPBOARD_CHARS)
                            : texte));
        }
        poser(session.selection());
    }

    /** La plage primaire de la sélection — null sans plage. */
    private @org.jspecify.annotations.Nullable SelRange plagePrimaire() {
        List<SelRange> plages = session.selection().ranges();
        return plages.isEmpty() ? null : plages.get(0);
    }

    /** La palette tourne : la barre et son menu la suivent. */
    void surPalette(PopupPalette palette) {
        barre.setPalette(palette);
        menu.surPalette(palette);
    }

    /** Re-pose la barre ouverte — les actions vives se relisent : le
     *  presse-papiers a changé, Coller se propose ou se retire. */
    void rafraichir() {
        poser(session.selection());
    }

    /** La sélection a changé : la barre se pose au-dessus d'elle, ou
     *  se referme sans sélection unique à tenir — la PORTE des
     *  réglages peut la retirer (la table centrale décide). */
    void poser(MultiSelection selection) {
        if (!editeur.reglages().ouverte(
                ReglagesEditeur.Porte.BARRE_SELECTION)) {
            popups.fermer(PopupController.PopupKind.SELECTION_BAR);
            return;
        }
        List<SelRange> plages = selection.ranges();
        boolean visible = plages.size() == 1 && !plages.get(0).isEmpty();
        if (!visible) {
            popups.fermer(PopupController.PopupKind.SELECTION_BAR);
            return;
        }
        popups.controleur().open(PopupController.PopupKind.SELECTION_BAR);
        barre.setActions(actions.get(), List.of());
        Rect sel = editeur.selectionBounds();
        PopupPlacer.Cadre placee = PopupAnchor.centeredAboveSelection(
                sel.left, sel.right, sel.top,
                popups.taille(PopupController.PopupKind.SELECTION_BAR),
                popups.couche().getWidth(), popups.couche().getHeight(),
                margePx);
        popups.montrer(PopupController.PopupKind.SELECTION_BAR,
                placee.x(), placee.y());
        if (popups.estOuvert(PopupController.PopupKind.SELECTION_OVERFLOW)) {
            ouvrirDebordement();
        }
    }

    /** Ouvre le menu de débordement sous la SÉLECTION — le menu ne
     *  marche jamais sur le caret (l'audit du 2026-11-14) ; les étages
     *  se relisent : la visibilité contextuelle naît de la donnée. */
    private void ouvrirDebordement() {
        menu.ouvrir(barre, editeur.selectionBounds().bottom,
                debordement.get());
    }
}
