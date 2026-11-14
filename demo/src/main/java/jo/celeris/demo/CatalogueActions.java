package jo.celeris.demo;

import android.view.MenuItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.google.android.material.appbar.MaterialToolbar;

import jo.celeris.android.host.popups.PopupsEditeur;
import jo.celeris.android.widgets.BarreToolbar;
import jo.celeris.android.widgets.IconeAction;
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
    private final PopupsEditeur popups;
    private final AtelierRefactor refactor;
    private final AtelierRecherche recherche;
    private final BasculeTheme basculeTheme;
    private final Supplier<List<OutlineEntry>> plan;
    private final Supplier<List<String>> lignes;
    private final BarreToolbar barreToolbar;
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
     * @param popups         l'orchestrateur des popups (complétion, survol)
     * @param refactor       l'atelier de refactorisation
     * @param recherche      l'atelier de recherche
     * @param basculeTheme   la bascule de thème
     * @param plan           le plan courant du fichier, vif au document
     * @param lignes         les lignes courantes du document, vives
     * @param barreToolbar   la barre du kit (carte de débordement)
     */
    CatalogueActions(EditorActivity activite, String nomFichier,
            AtelierLint lint, AtelierDiff diff, AtelierCorrectif correctif,
            AtelierAgent agent,
            AtelierIA ia, PopupsEditeur popups,
            AtelierRefactor refactor,
            AtelierRecherche recherche, BasculeTheme basculeTheme,
            Supplier<List<OutlineEntry>> plan,
            Supplier<List<String>> lignes,
            BarreToolbar barreToolbar) {
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
        brancherActions();
    }

    /** La table des actions — chaque identifiant y trouve son geste
     *  (le menu système, la carte de débordement et le menu d'intentions
     *  y lisent, jamais de doublon de logique). */
    private void brancherActions() {
        actionsMenu.put(R.id.action_recherche, recherche::ouvrir);
        actionsMenu.put(R.id.action_completion,
                popups::basculerCompletion);
        actionsMenu.put(R.id.action_intentions, popups::ouvrirIntentions);
        actionsMenu.put(R.id.action_survol, popups::ouvrirSurvol);
        actionsMenu.put(R.id.action_usages, popups::ouvrirUsages);
        actionsMenu.put(R.id.action_type_info, popups::ouvrirTypeInfo);
        actionsMenu.put(R.id.action_hierarchie, popups::ouvrirHierarchie);
        actionsMenu.put(R.id.action_aller, activite::basculerAller);
        actionsMenu.put(R.id.action_doc, popups::ouvrirDocExterne);
        actionsMenu.put(R.id.action_generer, popups::ouvrirGabarits);
        actionsMenu.put(R.id.action_supprimer_sur, popups::supprimerSur);
        actionsMenu.put(R.id.action_deplacer_dans, popups::deplacerDans);
        actionsMenu.put(R.id.action_masquer_inspection,
                popups::masquerInspection);
        actionsMenu.put(R.id.action_executer, popups::executerFichier);
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
        actionsMenu.put(R.id.action_reglages, activite::ouvrirReglages);
        actionsMenu.put(R.id.action_debordement,
                () -> barreToolbar.ouvrirDebordement(itemsDebordement()));
    }

    /** Le relais du menu : l'action de l'élément choisi, si connue. Le
     *  tap au menu coupe l'intelligence sous voile (chantier
     *  d'unification) : une complétion ne survit jamais sous une
     *  feuille modale, le chrome demeure. */
    boolean surMenu(MenuItem element) {
        Runnable action = actionsMenu.get(element.getItemId());
        if (action == null) {
            return false;
        }
        popups.fermerSousVoile();
        action.run();
        return true;
    }

    /** Les items du menu de débordement de la toolbar — les actions
     *  repliées, dans l'ordre du menu d'avant redressement, chacune
     *  portant SON pictogramme (l'audit du 2026-11-14 : jamais la
     *  même icône pour toutes) : la moitié ÉDITION d'abord, la moitié
     *  APPARENCE ensuite. */
    List<IntentionsMenuView.Item> itemsDebordement() {
        List<IntentionsMenuView.Item> items = new ArrayList<>(edition());
        items.addAll(apparence());
        return items;
    }

    /** La moitié ÉDITION du débordement — complétion, intentions,
     *  survol, usages, type, hiérarchie, aller à, documentation,
     *  génération, suppression, déplacement, masquage, exécution,
     *  lint, gabarit, formatage, renommage, prédiction. */
    private List<IntentionsMenuView.Item> edition() {
        return List.of(
                itemMenu(R.string.action_completion, R.id.action_completion,
                        IconeAction.COMPLETER),
                itemMenu(R.string.action_intentions, R.id.action_intentions,
                        IconeAction.INTENTIONS),
                itemMenu(R.string.action_survol, R.id.action_survol,
                        IconeAction.SURVOL),
                itemMenu(R.string.action_usages, R.id.action_usages,
                        IconeAction.USAGES),
                itemMenu(R.string.action_type_info, R.id.action_type_info,
                        IconeAction.TYPE_INFO),
                itemMenu(R.string.action_hierarchie, R.id.action_hierarchie,
                        IconeAction.HIERARCHIE),
                itemMenu(R.string.action_aller, R.id.action_aller,
                        IconeAction.ALLER_A),
                itemMenu(R.string.action_doc, R.id.action_doc,
                        IconeAction.DOC_EXTERNE),
                itemMenu(R.string.action_generer, R.id.action_generer,
                        IconeAction.GENERER),
                itemMenu(R.string.action_supprimer_sur, R.id.action_supprimer_sur,
                        IconeAction.SUPPRIMER),
                itemMenu(R.string.action_deplacer_dans, R.id.action_deplacer_dans,
                        IconeAction.DEPLACER),
                itemMenu(R.string.action_masquer_inspection,
                        R.id.action_masquer_inspection, IconeAction.MASQUER),
                itemMenu(R.string.action_executer, R.id.action_executer,
                        IconeAction.EXECUTER),
                itemMenu(R.string.action_lint, R.id.action_lint,
                        IconeAction.LINT),
                itemMenu(R.string.action_gabarit, R.id.action_gabarit,
                        IconeAction.GABARIT),
                itemMenu(R.string.action_formater, R.id.action_formater,
                        IconeAction.FORMATER),
                itemMenu(R.string.action_renommer, R.id.action_renommer,
                        IconeAction.RENOMMER),
                itemMenu(R.string.action_ia_prediction,
                        R.id.action_ia_prediction, IconeAction.IA_PREDICTION));
    }

    /** La moitié APPARENCE du débordement — annulation, rétablissement,
     *  thème, taille du texte, réglages. */
    private List<IntentionsMenuView.Item> apparence() {
        return List.of(
                itemMenu(R.string.action_annuler, R.id.action_annuler,
                        IconeAction.ANNULER),
                itemMenu(R.string.action_retablir, R.id.action_retablir,
                        IconeAction.RETABLIR),
                new IntentionsMenuView.Item(
                        activite.getString(R.string.choix_theme,
                                basculeTheme.nomCourant()),
                        IconeAction.THEME,
                        actionsMenu.get(R.id.action_theme)),
                itemMenu(R.string.action_diminuer, R.id.action_diminuer,
                        IconeAction.DIMINUER),
                itemMenu(R.string.action_agrandir, R.id.action_agrandir,
                        IconeAction.AGRANDIR),
                itemMenu(R.string.action_reglages, R.id.action_reglages,
                        IconeAction.REGLAGES));
    }

    /** Un item de la carte : le libellé de la chaîne, l'action du menu
     *  déjà branchée, SON pictogramme — le même canal, la carte seule
     *  change de peau. */
    private IntentionsMenuView.Item itemMenu(int libelle, int id,
            IconeAction picto) {
        return new IntentionsMenuView.Item(activite.getString(libelle), picto,
                actionsMenu.get(id));
    }

    /** Les intentions du menu : les actions de l'atelier au
     *  curseur — le libellé lu par la voie courte, chacune SON
     *  pictogramme (l'ampoule vit à l'entrée du menu SEULEMENT,
     *  l'icône de l'IDE de référence — jamais la même pour toutes). */
    List<IntentionsMenuView.Item> intentions() {
        return List.of(
                new IntentionsMenuView.Item(libelle(R.string.action_lint),
                        IconeAction.LINT, lint::analyser),
                new IntentionsMenuView.Item(libelle(R.string.action_apercu),
                        IconeAction.APERCU, () -> popups.docAncre().apercu(
                                plan.get(), lignes.get())),
                new IntentionsMenuView.Item(libelle(R.string.action_usages),
                        IconeAction.USAGES, popups::ouvrirUsages),
                new IntentionsMenuView.Item(libelle(R.string.action_type_info),
                        IconeAction.TYPE_INFO, popups::ouvrirTypeInfo),
                new IntentionsMenuView.Item(libelle(R.string.action_hierarchie),
                        IconeAction.HIERARCHIE, popups::ouvrirHierarchie),
                new IntentionsMenuView.Item(libelle(R.string.action_doc),
                        IconeAction.DOC_EXTERNE, popups::ouvrirDocExterne),
                new IntentionsMenuView.Item(libelle(R.string.action_generer),
                        IconeAction.GENERER, popups::ouvrirGabarits),
                new IntentionsMenuView.Item(libelle(R.string.action_supprimer_sur),
                        IconeAction.SUPPRIMER, popups::supprimerSur),
                new IntentionsMenuView.Item(libelle(R.string.action_deplacer_dans),
                        IconeAction.DEPLACER, popups::deplacerDans),
                new IntentionsMenuView.Item(
                        libelle(R.string.action_masquer_inspection),
                        IconeAction.MASQUER, popups::masquerInspection),
                new IntentionsMenuView.Item(libelle(R.string.action_executer),
                        IconeAction.EXECUTER, popups::executerFichier),
                new IntentionsMenuView.Item(libelle(R.string.action_diff),
                        IconeAction.DIFF, diff::ouvrir),
                new IntentionsMenuView.Item(
                        libelle(R.string.action_apercu_correctif),
                        IconeAction.CORRECTIF, correctif::ouvrir),
                new IntentionsMenuView.Item(libelle(R.string.action_agent),
                        IconeAction.AGENT, agent::ouvrir),
                new IntentionsMenuView.Item(libelle(R.string.action_ia_fantome),
                        IconeAction.IA_FANTOME, () -> ia.demanderFantome()),
                new IntentionsMenuView.Item(
                        libelle(R.string.action_ia_prediction),
                        IconeAction.IA_PREDICTION,
                        () -> ia.demanderPrediction()));
    }

    /** Le libellé de la chaîne — la voie courte des intentions. */
    private String libelle(int ressource) {
        return activite.getString(ressource);
    }
}
