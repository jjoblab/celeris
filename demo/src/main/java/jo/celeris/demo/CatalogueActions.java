package jo.celeris.demo;

import android.view.MenuItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.google.android.material.appbar.MaterialToolbar;

import jo.celeris.android.widgets.IntentionsMenuView;
import jo.celeris.core.syntax.structure.OutlineEntry;

/**
 * Le catalogue des actions de la barre d'application : la TABLE des
 * actions du menu (le canal unique de l'atelier — la toolbar et la
 * carte de débordement y lisent), la CARTE DE DÉBORDEMENT repliée
 * derrière la puce (les actions dans l'ordre du menu d'avant
 * redressement) et le MENU D'INTENTIONS au curseur. Le contenu vit
 * ici, la peau vit dans la vue : la même action court par les trois
 * peaux du kit (menu système de la toolbar, carte de débordement,
 * menu d'intentions), jamais de doublon de logique.
 */
final class CatalogueActions {

    private final EditorActivity activite;
    private final String nomFichier;
    private final AtelierLint lint;
    private final AtelierDiff diff;
    private final AtelierCorrectif correctif;
    private final AtelierAgent agent;
    private final AtelierIA ia;
    private final AtelierPopups popups;
    private final AtelierRefactor refactor;
    private final AtelierRecherche recherche;
    private final BasculeTheme basculeTheme;
    private final FeuilleReglages feuilleReglages;
    private final Supplier<List<OutlineEntry>> plan;
    private final Supplier<List<String>> lignes;
    private final AtelierBarreToolbar barreToolbar;
    private final Map<Integer, Runnable> actionsMenu = new HashMap<>();

    /**
     * Construit le catalogue sur les ateliers branchés.
     *
     * @param activite       l'activité porteuse (libellés, fin, relais)
     * @param nomFichier     le nom du fichier ouvert — titre de la barre
     * @param lint           l'atelier lint (analyse, correctifs)
     * @param diff           l'atelier des revues de fusion
     * @param correctif      l'atelier de l'aperçu d'un correctif (B-18)
     * @param agent          l'atelier de l'agent
     * @param ia             l'atelier IA (fantôme, prédiction)
     * @param popups         l'atelier des popups (complétion, survol)
     * @param refactor       l'atelier de refactorisation
     * @param recherche      l'atelier de recherche
     * @param basculeTheme   la bascule de thème
     * @param feuilleReglages la feuille des réglages
     * @param plan           le plan courant du fichier, vif au document
     * @param lignes         les lignes courantes du document, vives
     * @param barreToolbar   l'atelier de la barre (carte de débordement)
     */
    CatalogueActions(EditorActivity activite, String nomFichier,
            AtelierLint lint, AtelierDiff diff, AtelierCorrectif correctif,
            AtelierAgent agent,
            AtelierIA ia, AtelierPopups popups, AtelierRefactor refactor,
            AtelierRecherche recherche, BasculeTheme basculeTheme,
            FeuilleReglages feuilleReglages,
            Supplier<List<OutlineEntry>> plan,
            Supplier<List<String>> lignes,
            AtelierBarreToolbar barreToolbar) {
        this.activite = activite;
        this.nomFichier = nomFichier;
        this.lint = lint;
        this.diff = diff;
        this.correctif = correctif;
        this.agent = agent;
        this.ia = ia;
        this.popups = popups;
        this.refactor = refactor;
        this.recherche = recherche;
        this.basculeTheme = basculeTheme;
        this.feuilleReglages = feuilleReglages;
        this.plan = plan;
        this.lignes = lignes;
        this.barreToolbar = barreToolbar;
    }

    /** Branche la barre d'application : titre, retour, actions du
     *  menu — la table des actions se remplit, la toolbar y lit. */
    void brancher() {
        MaterialToolbar barre = activite.findViewById(R.id.barre_editeur);
        barre.setTitle(nomFichier);
        barre.setNavigationOnClickListener(vue -> activite.finish());
        barre.setOnMenuItemClickListener(this::surMenu);
        actionsMenu.put(R.id.action_recherche, recherche::ouvrir);
        actionsMenu.put(R.id.action_completion,
                popups::basculerCompletion);
        actionsMenu.put(R.id.action_intentions, popups::ouvrirIntentions);
        actionsMenu.put(R.id.action_survol, popups::ouvrirSurvol);
        actionsMenu.put(R.id.action_plan, activite::basculerPlan);
        actionsMenu.put(R.id.action_debogage, () -> {
            activite.etatDebogage.basculerPause();
            activite.majMarques();
        });
        actionsMenu.put(R.id.action_retour_ligne,
                activite::basculerRetourLigne);
        actionsMenu.put(R.id.action_annuler, () -> activite.rejouer(false));
        actionsMenu.put(R.id.action_retablir, () -> activite.rejouer(true));
        actionsMenu.put(R.id.action_lint, lint::analyser);
        actionsMenu.put(R.id.action_corriger, activite::corrigerLint);
        actionsMenu.put(R.id.action_gabarit, refactor::insererGabarit);
        actionsMenu.put(R.id.action_formater, refactor::formaterDocument);
        actionsMenu.put(R.id.action_renommer, refactor::renommerSymbole);
        actionsMenu.put(R.id.action_importer, refactor::importerListe);
        actionsMenu.put(R.id.action_apercu, () -> popups.docAncre()
                .apercu(plan.get(), lignes.get()));
        actionsMenu.put(R.id.action_diff, diff::ouvrir);
        actionsMenu.put(R.id.action_agent, agent::ouvrir);
        actionsMenu.put(R.id.action_ia_fantome, () -> ia.demanderFantome());
        actionsMenu.put(R.id.action_ia_prediction,
                () -> ia.demanderPrediction());
        actionsMenu.put(R.id.action_theme, basculeTheme::tourner);
        actionsMenu.put(R.id.action_diminuer,
                () -> activite.zoomerPolice(-EditorActivity.PAS_TAILLE));
        actionsMenu.put(R.id.action_agrandir,
                () -> activite.zoomerPolice(EditorActivity.PAS_TAILLE));
        actionsMenu.put(R.id.action_reglages, feuilleReglages::montrer);
        actionsMenu.put(R.id.action_debordement,
                () -> barreToolbar.ouvrirDebordement(itemsDebordement()));
    }

    /** Le relais du menu : l'action de l'élément choisi, si connue. */
    boolean surMenu(MenuItem element) {
        Runnable action = actionsMenu.get(element.getItemId());
        if (action == null) {
            return false;
        }
        action.run();
        return true;
    }

    /** Les items du menu de débordement de la toolbar — les actions
     *  repliées, dans l'ordre du menu d'avant redressement. */
    List<IntentionsMenuView.Item> itemsDebordement() {
        return List.of(
                itemMenu(R.string.action_completion, R.id.action_completion),
                itemMenu(R.string.action_intentions, R.id.action_intentions),
                itemMenu(R.string.action_survol, R.id.action_survol),
                itemMenu(R.string.action_lint, R.id.action_lint),
                itemMenu(R.string.action_gabarit, R.id.action_gabarit),
                itemMenu(R.string.action_formater, R.id.action_formater),
                itemMenu(R.string.action_renommer, R.id.action_renommer),
                itemMenu(R.string.action_ia_prediction,
                        R.id.action_ia_prediction),
                itemMenu(R.string.action_annuler, R.id.action_annuler),
                itemMenu(R.string.action_retablir, R.id.action_retablir),
                new IntentionsMenuView.Item(
                        activite.getString(R.string.choix_theme,
                                basculeTheme.nomCourant()),
                        actionsMenu.get(R.id.action_theme)),
                itemMenu(R.string.action_diminuer, R.id.action_diminuer),
                itemMenu(R.string.action_agrandir, R.id.action_agrandir),
                itemMenu(R.string.action_reglages, R.id.action_reglages));
    }

    /** Un item de la carte : le libellé de la chaîne, l'action du menu
     *  déjà branchée — le même canal, la carte seule change de peau. */
    private IntentionsMenuView.Item itemMenu(int libelle, int id) {
        return new IntentionsMenuView.Item(activite.getString(libelle),
                actionsMenu.get(id));
    }

    /** Les intentions du menu : les actions de l'atelier au curseur. */
    List<IntentionsMenuView.Item> intentions() {
        return List.of(
                new IntentionsMenuView.Item(
                        activite.getString(R.string.action_lint),
                        lint::analyser),
                new IntentionsMenuView.Item(
                        activite.getString(R.string.action_apercu),
                        () -> popups.docAncre().apercu(
                                plan.get(), lignes.get())),
                new IntentionsMenuView.Item(
                        activite.getString(R.string.action_diff),
                        diff::ouvrir),
                new IntentionsMenuView.Item(
                        activite.getString(R.string.action_apercu_correctif),
                        correctif::ouvrir),
                new IntentionsMenuView.Item(
                        activite.getString(R.string.action_agent),
                        agent::ouvrir),
                new IntentionsMenuView.Item(
                        activite.getString(R.string.action_ia_fantome),
                        () -> ia.demanderFantome()),
                new IntentionsMenuView.Item(
                        activite.getString(R.string.action_ia_prediction),
                        () -> ia.demanderPrediction()));
    }
}
