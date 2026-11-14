package jo.celeris.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.widgets.IconeAction;
import jo.celeris.android.widgets.MenuEtages;
import jo.celeris.android.widgets.SelectionBarView;
import jo.celeris.core.intelligence.CommentRules;
import jo.celeris.core.intelligence.Commenter;
import jo.celeris.core.intelligence.EditPlan;
import jo.celeris.core.intelligence.LineActions;
import jo.celeris.core.language.model.LanguageConfiguration;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.text.MultiSelection;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

/**
 * L'atelier de sélection de la démonstration (M7.5, §7, reprise) : les
 * actions de la barre de sélection en transactions de session, un
 * presse-papiers LOCAL qui ne touche pas au système (les gestes de
 * la recette restent déterministes, l'atelier n'emporte rien au-dehors).
 * Les PRINCIPALES tiennent la pilule — couper, copier, tout
 * sélectionner, et COLLER SEULEMENT QUAND LE PRESSE-PAPIERS EST PLEIN
 * (l'audit du propriétaire du 2026-11-14 : l'option inutile ne se
 * promène jamais sur la pilule) ; le DÉBORDEMENT vit derrière les
 * trois points EN ÉTAGES CONTEXTUELS (le même audit : « séparé en
 * étages ou colonnes qui deviennent visibles contextuellement comme
 * les go to, l'option usages, rename ») — l'étage de NAVIGATION ne se
 * montre que sur un symbole au caret, l'étage d'ÉDITION vit toujours,
 * l'étage vide ne se montre jamais : la visibilité naît de la donnée.
 */
final class AtelierSelection {

    private final EditorView editeur;
    private final TextSession session;
    /** La langue ouverte — les règles de commentaire et le style
     *  d'indentation s'y dérivent, vives au document. */
    private final Supplier<LanguageSupport> langue;
    /** Le rappel de re-pose de la barre — le presse-papiers changé y
     *  réveille les actions vives (Coller se propose ou se retire). */
    private final Runnable reposerBarre;
    /** Les gestes de navigation de l'hôte — l'étage ne vit que si le
     *  geste existe (aller à, usages, renommage : chacun SA condition
     *  de pertinence, jamais une action morte). */
    private final List<SelectionBarView.Item> navigation = new ArrayList<>();
    /** Le presse-papiers local — jamais le système. */
    private String pressePapiers;

    /**
     * Construit l'atelier de sélection.
     *
     * @param editeur     la vue d'éditeur ouverte
     * @param session     la session d'édition courante
     * @param langue      le support du langage ouvert, vif au document
     * @param reposerBarre le rappel qui re-pose la barre ouverte
     */
    AtelierSelection(EditorView editeur, TextSession session,
            Supplier<LanguageSupport> langue, Runnable reposerBarre) {
        this.editeur = editeur;
        this.session = session;
        this.langue = langue;
        this.reposerBarre = reposerBarre;
    }

    /**
     * Pose un geste de NAVIGATION au débordement — l'étage ne se
     * montre que si AU MOINS un geste vit (la visibilité contextuelle
     * naît de la donnée, jamais d'un drapeau codé en dur).
     *
     * @param geste l'action de navigation (aller à, usages, renommer)
     */
    void poserGesteNavigation(SelectionBarView.Item geste) {
        navigation.add(geste);
    }

    /**
     * Les actions principales VIVES de la pilule — relues à chaque
     * pose de la barre : couper, copier et tout sélectionner toujours
     * (la barre ne vit que sur une sélection), COLLER SEULEMENT quand
     * le presse-papiers est plein. Chaque pastille porte SON
     * pictogramme (l'audit du 2026-11-14 : jamais la même pour
     * toutes).
     *
     * @param couper le libellé de coupe
     * @param copier le libellé de copie
     * @param coller le libellé de collage
     * @param tout   le libellé de tout-sélectionner
     * @return le fournisseur des items principaux de la barre
     */
    Supplier<List<SelectionBarView.Item>> principalesVives(String couper,
            String copier, String coller, String tout) {
        return () -> {
            List<SelectionBarView.Item> items = new ArrayList<>(4);
            items.add(new SelectionBarView.Item(couper,
                    IconeAction.COUPER, this::couper));
            items.add(new SelectionBarView.Item(copier,
                    IconeAction.COPIER, this::copier));
            if (pressePapiers != null && !pressePapiers.isEmpty()) {
                items.add(new SelectionBarView.Item(coller,
                        IconeAction.COLLER, this::coller));
            }
            items.add(new SelectionBarView.Item(tout,
                    IconeAction.TOUT, this::toutSelectionner));
            return items;
        };
    }

    /**
     * Les étages de débordement VIFS — servis par le menu des trois
     * points : l'étage de NAVIGATION ne vit que sur un SYMBOLE au
     * caret (l'audit du propriétaire du 2026-11-14 : « les go to,
     * l'option usages, rename » — la visibilité contextuelle naît de
     * la donnée), l'étage d'ÉDITION vit toujours (commenter, casse,
     * indentation, duplication), l'étage VIDE ne se montre jamais —
     * chaque rangée portant SON pictogramme.
     *
     * @param titreNav    le titre de l'étage de navigation (« GO TO »)
     * @param titreEdit   le titre de l'étage d'édition (« EDIT »)
     * @param commenter   le libellé de commentaire
     * @param majuscules  le libellé de mise en majuscules
     * @param minuscules  le libellé de mise en minuscules
     * @param indenter    le libellé d'indentation
     * @param dupliquer   le libellé de duplication
     * @return le fournisseur des étages repliés derrière la puce
     */
    Supplier<List<MenuEtages.Etage>> debordementVives(String titreNav,
            String titreEdit, String commenter, String majuscules,
            String minuscules, String indenter, String dupliquer) {
        return () -> {
            List<MenuEtages.Etage> etages = new ArrayList<>(2);
            if (symboleAuCaret() != null) {
                etages.add(new MenuEtages.Etage(titreNav, navigation));
            }
            etages.add(new MenuEtages.Etage(titreEdit, List.of(
                    new SelectionBarView.Item(commenter,
                            IconeAction.COMMENTER, this::commenter),
                    new SelectionBarView.Item(majuscules,
                            IconeAction.MAJUSCULES, this::majuscules),
                    new SelectionBarView.Item(minuscules,
                            IconeAction.MINUSCULES, this::minuscules),
                    new SelectionBarView.Item(indenter,
                            IconeAction.INDENTER, this::indenter),
                    new SelectionBarView.Item(dupliquer,
                            IconeAction.DUPLIQUER, this::dupliquer))));
            return etages;
        };
    }

    /** Le symbole au caret — null au vide entre deux mots : l'étage de
     *  navigation ne vit que sur lui. */
    private String symboleAuCaret() {
        List<SelRange> plages = session.selection().ranges();
        if (plages.isEmpty()) {
            return null;
        }
        int caret = plages.get(0).caret();
        String texte = session.document().text();
        if (caret <= 0 || caret > texte.length()) {
            return null;
        }
        int debut = caret;
        while (debut > 0 && estLettre(texte.charAt(debut - 1))) {
            debut--;
        }
        int fin = caret;
        while (fin < texte.length() && estLettre(texte.charAt(fin))) {
            fin++;
        }
        return debut == fin ? null : texte.substring(debut, fin);
    }

    /** Vrai si le caractère vit dans un identifiant. */
    private static boolean estLettre(char c) {
        return c == '_' || Character.isLetterOrDigit(c);
    }

    /**
     * La décision pure des principales : COLLER ne se pose que quand le
     * presse-papiers est plein — l'audit du propriétaire du
     * 2026-11-14 (« surtout coller ne doit être visible que quand il
     * est nécessaire »). Le banc y lit la règle sans monter la scène.
     *
     * @param pressePapiers le presse-papiers local, null pour vide
     * @param coller        le libellé de collage
     * @return vrai quand la pastille Coller se propose
     */
    static boolean collerSePropose(String pressePapiers, String coller) {
        return coller != null && pressePapiers != null
                && !pressePapiers.isEmpty();
    }

    private void couper() {
        copier();
        remplacer("");
    }

    private void copier() {
        SelRange plage = plagePrimaire();
        if (plage != null && !plage.isEmpty()) {
            pressePapiers = session.document().subText(
                    plage.start(), plage.end());
            // la barre se RE-POSE : Coller vient de se proposer
            reposerBarre.run();
        }
    }

    private void coller() {
        if (pressePapiers != null) {
            remplacer(pressePapiers);
        }
    }

    private void toutSelectionner() {
        session.setSelection(MultiSelection.of(
                new SelRange(0, session.document().length())));
    }

    /** Commente ou décommente le bloc touché — les règles du langage
     *  ouvert, le plan vide ne fait rien. */
    private void commenter() {
        LanguageSupport langueOuverte = langue.get();
        CommentRules rules = langueOuverte == null
                ? CommentRules.of(LanguageConfiguration.EMPTY)
                : CommentRules.of(langueOuverte.configuration());
        Commenter.toggle(session.document(), session.selection(), rules)
                .ifPresent(this::appliquer);
    }

    /** La sélection passe en MAJUSCULES — un geste la défait. */
    private void majuscules() {
        casser(true);
    }

    /** La sélection passe en minuscules — un geste la défait. */
    private void minuscules() {
        casser(false);
    }

    /** Le bloc touché gagne un cran d'indentation — le plan partagé de
     *  la touche Tab, jamais de doublon de logique. */
    private void indenter() {
        appliquer(AtelierTouchesCode.planIndentation(session.document(),
                session.selection(), langue));
    }

    /** Duplique le bloc touché — le moteur pur de la maison, les
     *  curseurs descendent dans la copie. */
    private void dupliquer() {
        appliquer(LineActions.duplicateLines(session.document(),
                session.selection()));
    }

    /** La casse de la sélection primaire — haut ou bas, transaction
     *  annulable, le caret suit la longueur du texte changé. */
    private void casser(boolean haut) {
        SelRange plage = plagePrimaire();
        if (plage == null || plage.isEmpty()) {
            return;
        }
        String texte = session.document().subText(plage.start(),
                plage.end());
        String change = haut ? texte.toUpperCase() : texte.toLowerCase();
        if (session.replaceRange(plage.start(), plage.end(), change)) {
            session.setSelection(MultiSelection.of(new SelRange(
                    plage.start(), plage.start() + change.length())));
            editeur.invalidate();
        }
    }

    /** Le plan du geste appliqué — la voie unique, l'image suit. */
    private void appliquer(EditPlan plan) {
        if (session.apply(plan.transaction(), plan.selection())) {
            editeur.invalidate();
        }
    }

    private void remplacer(String texte) {
        SelRange plage = plagePrimaire();
        if (plage == null) {
            return;
        }
        session.replaceRange(plage.start(), plage.end(), texte);
        editeur.invalidate();
    }

    private SelRange plagePrimaire() {
        List<SelRange> plages = session.selection().ranges();
        return plages.size() == 1 ? plages.get(0) : null;
    }
}
