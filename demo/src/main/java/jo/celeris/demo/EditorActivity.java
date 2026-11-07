package jo.celeris.demo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jo.celeris.android.host.spi.Arret;
import jo.celeris.android.host.spi.CadrePile;
import jo.celeris.android.host.spi.CheminDoc;
import jo.celeris.android.host.spi.DiagnosticHote;
import jo.celeris.android.host.spi.EtatLignesVcs;
import jo.celeris.android.host.spi.FauxDebogueur;
import jo.celeris.android.host.spi.LigneArret;
import jo.celeris.android.view.ClasseLargeur;
import jo.celeris.android.view.EditorTheme;
import jo.celeris.android.view.EditorThemes;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.view.FontSpec;
import jo.celeris.android.view.MarquesIntegration;
import jo.celeris.android.view.NavigationCommands;
import jo.celeris.android.view.PastillesCouleur;
import jo.celeris.android.view.ReperesPreview;
import jo.celeris.android.widgets.ApercuDefinition;
import jo.celeris.android.widgets.IntentionsMenuView;
import jo.celeris.android.widgets.OutlineView;
import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.core.syntax.LanguageSyntax;
import jo.celeris.core.syntax.OutlineEntry;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

/**
 * L'atelier d'édition de la démonstration : un fichier d'exemple ouvert
 * dans la vue Canvas complète — coloration, plis, recherche, plan,
 * thèmes, zoom et annulation — porté par un vrai layout (barre
 * d'application au menu d'actions, barre de recherche dépliable, barre
 * d'état en police à chasse fixe). Les réglages persistés se
 * réappliquent au retour de l'écran de réglages en direct.
 */
public final class EditorActivity extends AppCompatActivity {

    /** La clé de l'intention : le nom du fichier d'exemple à ouvrir. */
    public static final String EXTRA_FICHIER = "fichier";

    private static final String CLE_THEME = "theme.index";
    private static final String CLE_NUMEROS = "gouttiere.numeros";
    private static final String CLE_RETOUR = "retour.souple";
    private static final String CLE_TAILLE = "police.taille";
    private static final float TAILLE_DEFAUT = 14f;
    private static final float TAILLE_MIN = 8f;
    private static final float TAILLE_MAX = 40f;
    private static final float PAS_TAILLE = 2f;
    private static final EditorTheme[] THEMES = {
            EditorThemes.CLAIR, EditorThemes.SOMBRE, EditorThemes.CONTRASTE_ELEVE };
    private static final float PART_PLAN = 0.45f;

    private EditorView editeur;
    private TextSession session;
    private OutlineView plan;
    private TextView etiquettePosition;
    private TextView etiquetteLangue;
    private TextView etiquetteArrets;
    private SharedPreferences prefs;
    private String nomFichier;
    private String reference;
    private CheminDoc chemin;
    private final FauxDebogueur debogueur = new FauxDebogueur();
    private List<ReperesPreview.ReperePreview> reperes = List.of();
    private AtelierLint atelierLint;
    private AtelierDiff atelierDiff;
    private AtelierAgent atelierAgent;
    private AtelierIA atelierIA;
    private AtelierPopups atelierPopups;
    private AtelierRefactor atelierRefactor;
    private AtelierRecherche atelierRecherche;
    private AtelierSelection atelierSelection;
    private List<OutlineEntry> entreesPlan = List.of();
    private final Map<Integer, Runnable> actionsMenu = new HashMap<>();

    @Override
    protected void onCreate(Bundle etat) {
        super.onCreate(etat);
        EdgeToEdge.enable(this);
        prefs = getSharedPreferences("celeris", Context.MODE_PRIVATE);
        FichierExemple fichier = resoudreFichier();
        nomFichier = fichier.nom();
        reference = fichier.contenu();
        chemin = CheminDoc.de("/" + nomFichier);
        session = TextSession.of(fichier.contenu(), System::nanoTime);
        atelierLint = new AtelierLint(nomFichier, session, this::etatLint);
        atelierDiff = new AtelierDiff(this, reference, session, this::apresFusion);
        atelierAgent = new AtelierAgent(this, session, this::apresFusion);
        session.addEditListener((debut, fin, retire, insere) -> {
            majPosition();
            majMarques();
            majPastillesEtReperes();
            // La frappe suivante refuse le fantôme (F-09) — l'atelier IA
            // suit s'il est déjà en place.
            if (atelierIA != null) {
                atelierIA.surEdition();
            }
        });
        setContentView(R.layout.activity_editor);
        brancherScene();
        brancherBarre();
        appliquerPreferences();
        appliquerBordABord();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Les réglages ont pu changer pendant la pause : réappliquer.
        appliquerPreferences();
    }

    private FichierExemple resoudreFichier() {
        String demande = getIntent().getStringExtra(EXTRA_FICHIER);
        FichierExemple fichier = FichiersExemple.parNom(
                demande == null ? FichiersExemple.premier().nom() : demande);
        return fichier == null ? FichiersExemple.premier() : fichier;
    }

    private void brancherBarre() {
        MaterialToolbar barre = findViewById(R.id.barre_editeur);
        barre.setTitle(nomFichier);
        barre.setNavigationOnClickListener(vue -> finish());
        barre.setOnMenuItemClickListener(this::surMenu);
        actionsMenu.put(R.id.action_recherche, atelierRecherche::ouvrir);
        actionsMenu.put(R.id.action_completion,
                atelierPopups::basculerCompletion);
        actionsMenu.put(R.id.action_intentions, atelierPopups::ouvrirIntentions);
        actionsMenu.put(R.id.action_survol, atelierPopups::ouvrirSurvol);
        actionsMenu.put(R.id.action_plan, this::basculerPlan);
        actionsMenu.put(R.id.action_debogage, this::basculerDebogage);
        actionsMenu.put(R.id.action_retour_ligne, this::basculerRetourLigne);
        actionsMenu.put(R.id.action_annuler, () -> rejouer(false));
        actionsMenu.put(R.id.action_retablir, () -> rejouer(true));
        actionsMenu.put(R.id.action_lint, this::analyserLint);
        actionsMenu.put(R.id.action_corriger, this::corrigerLint);
        actionsMenu.put(R.id.action_gabarit, atelierRefactor::insererGabarit);
        actionsMenu.put(R.id.action_formater, atelierRefactor::formaterDocument);
        actionsMenu.put(R.id.action_renommer, atelierRefactor::renommerSymbole);
        actionsMenu.put(R.id.action_importer, atelierRefactor::importerListe);
        actionsMenu.put(R.id.action_apercu, this::apercuDefinition);
        actionsMenu.put(R.id.action_diff, atelierDiff::ouvrir);
        actionsMenu.put(R.id.action_agent, atelierAgent::ouvrir);
        actionsMenu.put(R.id.action_ia_fantome, () -> atelierIA.demanderFantome());
        actionsMenu.put(R.id.action_ia_prediction, () -> atelierIA.demanderPrediction());
        actionsMenu.put(R.id.action_theme, this::tournerTheme);
        actionsMenu.put(R.id.action_diminuer, () -> zoomerPolice(-PAS_TAILLE));
        actionsMenu.put(R.id.action_agrandir, () -> zoomerPolice(PAS_TAILLE));
        actionsMenu.put(R.id.action_reglages,
                () -> startActivity(new Intent(this, SettingsActivity.class)));
    }

    private boolean surMenu(MenuItem element) {
        Runnable action = actionsMenu.get(element.getItemId());
        if (action == null) {
            return false;
        }
        action.run();
        return true;
    }

    private void brancherScene() {
        editeur = findViewById(R.id.editeur);
        // Le contenu du fichier ouvert doit vivre dans l'éditeur.
        editeur.setSession(session);
        // L'intelligence de la démonstration se pose sur la vue (M7.5,
        // §8) : frappe intelligente, Entrée indentée, collage réindenté,
        // complétion par les mots du code.
        editeur.intelligence().setProvider(new DemoIntelligence());
        atelierIA = new AtelierIA(this, editeur, session, nomFichier);
        brancherAteliers();
        // Toute animation de vue (défilement, clic) rafraîchit la
        // position ; les clics restent portés par EditorView, qui
        // redéfinit performClick.
        editeur.setOnTouchListener((vue, evenement) -> {
            vue.post(this::majPosition);
            return false;
        });
        editeur.setSurPlan(entrees -> {
            entreesPlan = entrees;
            editeur.post(() -> {
                plan.setEntries(entrees);
                atelierPopups.setPlan(entrees);
            });
        });
        // La gouttière s'adapte à la classe de largeur de la fenêtre
        // (D-19) — le pliable déplié change de classe d'un geste.
        editeur.setClasseLargeur(ClasseLargeur.de(
                getResources().getConfiguration().screenWidthDp));
        // Le terrain d'essai du débogueur : le clic dans le champ des
        // numéros pose et retire les points d'arrêt (D-04).
        editeur.marquesGouttiere().setSurClicLigne(this::basculerArret);
        // Le rendu d'un repère @Preview reste la part de l'hôte (C-11)
        // — la démonstration répond par sa propre scène.
        editeur.marquesGouttiere().setSurRenduPreview(this::renduPreview);
        // Les pastilles et repères du fichier ouvert se posent d'entrée.
        majPastillesEtReperes();

        plan = findViewById(R.id.plan);
        plan.setBackgroundColor(Color.argb(235, 32, 33, 36));
        plan.getLayoutParams().width = Math.round(largeurEcran() * PART_PLAN);
        plan.setSurClic(this::sautVersEntree);

        etiquettePosition = findViewById(R.id.etat_position);
        etiquetteLangue = findViewById(R.id.etat_langue);
        etiquetteArrets = findViewById(R.id.etat_arrets);
    }

    /**
     * Les ateliers d'interaction (M7.5, §7, reprise) : l'IA (F-01 à
     * F-03), les DIX popups sur la couche posée — jamais de focus volé
     * (PIEGE-27) —, la recherche dépliable et la sélection au
     * presse-papiers local.
     */
    private void brancherAteliers() {
        // L'atelier de refactorisation (M7.5, §8) : gabarit, formatage,
        // renommage et import — chaque geste une transaction annulable.
        atelierRefactor = new AtelierRefactor(this, editeur, session,
                nomFichier, this::verdictRefactor);
        // L'atelier des popups : la couche posée sur l'éditeur porte
        // les dix popups, la machine à états décide qui reste ouvert.
        PopupLayer couche = findViewById(R.id.couche_popups);
        atelierPopups = new AtelierPopups(editeur, session, couche,
                indexThemeCourant() != 0);
        atelierPopups.setNomFichier(nomFichier);
        atelierPopups.configurerIntentions(intentionsAuCurseur());
        atelierSelection = new AtelierSelection(editeur, session);
        atelierPopups.configurerActionsSelection(atelierSelection.items(
                getString(R.string.barre_couper),
                getString(R.string.barre_copier),
                getString(R.string.barre_coller),
                getString(R.string.barre_tout)));
        atelierRecherche = new AtelierRecherche(this, editeur, session,
                atelierPopups::ouvrirRecherche, atelierPopups::fermerRecherche);
        atelierRecherche.brancher();
    }

    /** Le chrome s'étend bord à bord : encarts système et clavier
     *  deviennent des marges, la barre d'état reste au-dessus du clavier. */
    private void appliquerBordABord() {
        View racine = findViewById(R.id.racine_editeur);
        ViewCompat.setOnApplyWindowInsetsListener(racine, (vue, encarts) -> {
            Insets barres = encarts.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets clavier = encarts.getInsets(WindowInsetsCompat.Type.ime());
            vue.setPadding(0, barres.top, 0, Math.max(barres.bottom, clavier.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void appliquerPreferences() {
        int indexTheme = indexThemeCourant();
        editeur.setTheme(THEMES[indexTheme]);
        editeur.setNumerosVisibles(prefs.getBoolean(CLE_NUMEROS, true));
        editeur.setSoftWrap(prefs.getBoolean(CLE_RETOUR, false));
        editeur.setFont(new FontSpec("mono",
                prefs.getFloat(CLE_TAILLE, TAILLE_DEFAUT)));
        LanguageSyntax langue = FichiersExemple.languePour(nomFichier);
        editeur.setLanguage(langue);
        etiquetteLangue.setText(langue == null ? nomFichier : langue.id());
        majTitreTheme();
        majTitreRetourLigne();
        majPosition();
    }

    private int indexThemeCourant() {
        return Math.min(Math.max(prefs.getInt(CLE_THEME, 0), 0), THEMES.length - 1);
    }

    private void tournerTheme() {
        int suivant = (indexThemeCourant() + 1) % THEMES.length;
        prefs.edit().putInt(CLE_THEME, suivant).apply();
        editeur.setTheme(THEMES[suivant]);
        majTitreTheme();
    }

    private void majTitreTheme() {
        MaterialToolbar barre = findViewById(R.id.barre_editeur);
        barre.getMenu().findItem(R.id.action_theme)
                .setTitle(getString(R.string.choix_theme, nomTheme()));
    }

    private String nomTheme() {
        return getString(switch (indexThemeCourant()) {
            case 1 -> R.string.theme_sombre;
            case 2 -> R.string.theme_contraste;
            default -> R.string.theme_clair;
        });
    }

    private void zoomerPolice(float pas) {
        float courante = prefs.getFloat(CLE_TAILLE, TAILLE_DEFAUT);
        float bornee = Math.min(Math.max(courante + pas, TAILLE_MIN), TAILLE_MAX);
        prefs.edit().putFloat(CLE_TAILLE, bornee).apply();
        editeur.setFont(new FontSpec("mono", bornee));
    }

    private void rejouer(boolean enAvant) {
        if (enAvant) {
            session.redo();
        } else {
            session.undo();
        }
        editeur.invalidate();
        majPosition();
    }

    private void basculerPlan() {
        plan.setVisibility(plan.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    /**
     * Bascule le retour à la ligne souple (A-18) depuis l'atelier :
     * l'état se persiste — les réglages et le retour de l'atelier
     * relisent le même état — et le libellé du menu le dit.
     */
    private void basculerRetourLigne() {
        boolean actif = !prefs.getBoolean(CLE_RETOUR, false);
        prefs.edit().putBoolean(CLE_RETOUR, actif).apply();
        editeur.setSoftWrap(actif);
        majTitreRetourLigne();
    }

    /** Le libellé du menu du retour à la ligne suit l'état courant. */
    private void majTitreRetourLigne() {
        boolean actif = prefs.getBoolean(CLE_RETOUR, false);
        MaterialToolbar barre = findViewById(R.id.barre_editeur);
        barre.getMenu().findItem(R.id.action_retour_ligne)
                .setTitle(getString(R.string.choix_retour_ligne,
                        getString(actif ? R.string.retour_actif
                                : R.string.retour_inactif)));
    }

    /** Les intentions du menu : les actions de l'atelier au curseur. */
    private List<IntentionsMenuView.Item> intentionsAuCurseur() {
        return List.of(
                new IntentionsMenuView.Item(getString(R.string.action_lint),
                        this::analyserLint),
                new IntentionsMenuView.Item(
                        getString(R.string.action_apercu),
                        this::apercuDefinition),
                new IntentionsMenuView.Item(getString(R.string.action_diff),
                        atelierDiff::ouvrir),
                new IntentionsMenuView.Item(getString(R.string.action_agent),
                        atelierAgent::ouvrir),
                new IntentionsMenuView.Item(
                        getString(R.string.action_ia_fantome),
                        () -> atelierIA.demanderFantome()),
                new IntentionsMenuView.Item(
                        getString(R.string.action_ia_prediction),
                        () -> atelierIA.demanderPrediction()));
    }

    private void sautVersEntree(OutlineEntry entree) {
        NavigationCommands.placeCaret(editeur, entree.startOffset());
    }

    /** Pose ou retire le point d'arrêt de la ligne cliquée (D-04). */
    private void basculerArret(int ligne) {
        boolean absent = debogueur.arrets().stream()
                .noneMatch(arret -> arret.fichier().equals(chemin)
                        && arret.ligne() == ligne + 1);
        if (absent) {
            debogueur.poserArret(chemin, ligne + 1);
        } else {
            debogueur.retirerArret(chemin, ligne + 1);
        }
        majEtatDebogage();
        majMarques();
    }

    /**
     * Bascule la pause simulée (D-05) : sans pause, l'exécution s'arrête
     * au premier arrêt actif du fichier — à la cinquième ligne sinon —
     * avec une pile de deux cadres ; en pause, l'exécution reprend.
     */
    private void basculerDebogage() {
        if (debogueur.ligneCourante() != null) {
            debogueur.simulerReprise();
        } else {
            Arret premier = debogueur.arrets().stream()
                    .filter(arret -> arret.fichier().equals(chemin) && arret.actif())
                    .findFirst().orElse(null);
            int ligne = premier != null ? premier.ligne()
                    : Math.min(5, session.document().lineCount());
            debogueur.simulerPause(chemin, ligne, List.of(
                    new CadrePile("Exemple", "corps", chemin.nom(), ligne),
                    new CadrePile("Exemple", "appelant", chemin.nom(),
                            Math.max(1, ligne - 2))));
        }
        majEtatDebogage();
        majMarques();
    }

    private void majEtatDebogage() {
        LigneArret pause = debogueur.ligneCourante();
        if (pause != null) {
            etiquetteArrets.setText(getString(R.string.etat_pause, pause.ligne()));
            etiquetteArrets.setVisibility(View.VISIBLE);
        } else {
            long nombre = debogueur.arrets().stream()
                    .filter(arret -> arret.fichier().equals(chemin)).count();
            etiquetteArrets.setText(getResources().getQuantityString(
                    R.plurals.etat_arrets, (int) nombre, (int) nombre));
            etiquetteArrets.setVisibility(nombre == 0 ? View.GONE : View.VISIBLE);
        }
        MaterialToolbar barre = findViewById(R.id.barre_editeur);
        barre.getMenu().findItem(R.id.action_debogage)
                .setIcon(pause == null ? R.drawable.ic_debogage : R.drawable.ic_reprendre)
                .setTitle(pause == null ? R.string.action_deboguer : R.string.action_reprendre);
    }

    private void analyserLint() {
        atelierLint.analyser();
    }

    /** Le verdict des ateliers : la barre d'état le publie. */
    private void verdictRefactor(CharSequence message) {
        etiquetteArrets.setText(message);
        etiquetteArrets.setVisibility(View.VISIBLE);
    }

    /**
     * Applique tous les correctifs mécaniques disponibles (C-12) :
     * chaque correctif devient une transaction de remplacement — donc
     * annulable d'un geste —, l'état se rafraîchit.
     */
    private void corrigerLint() {
        int corriges = atelierLint.corriger();
        if (corriges > 0) {
            etiquetteArrets.setText(getString(R.string.lint_correctifs, corriges));
        } else {
            etiquetteArrets.setText(R.string.lint_propre);
        }
        etiquetteArrets.setVisibility(View.VISIBLE);
        editeur.invalidate();
        majMarques();
    }

    /** L'état lint de la barre de statut : les lignes se marquent. */
    private void etatLint(List<DiagnosticHote> diagnostics) {
        editeur.marquesGouttiere().setErreursImportees(
                atelierLint.lignesMarquees());
        atelierPopups.setDiagnostics(diagnostics);
        if (!diagnostics.isEmpty()) {
            etiquetteArrets.setText(getResources().getQuantityString(
                    R.plurals.etat_lint, diagnostics.size(), diagnostics.size()));
        } else {
            etiquetteArrets.setText(R.string.lint_propre);
        }
        etiquetteArrets.setVisibility(View.VISIBLE);
        editeur.invalidate();
    }

    /**
     * Rafraîchit les pastilles de couleur (C-05) et les repères
     * {@code @Preview} (C-11) : le balayage des littéraux et des
     * annotations, une pastille par ligne porteuse, un repère par
     * annotation.
     */
    private void majPastillesEtReperes() {
        if (editeur == null) {
            return;
        }
        List<String> lignes = lignesCourantes();
        editeur.marquesGouttiere().setPastilles(
                PastillesCouleur.desLignes(lignes));
        reperes = ReperesPreview.desLignes(lignes);
        int[] lignesRepere = new int[reperes.size()];
        for (int i = 0; i < reperes.size(); i++) {
            lignesRepere[i] = reperes.get(i).ligne();
        }
        editeur.marquesGouttiere().setReperesPreview(lignesRepere);
        editeur.invalidate();
    }

    private List<String> lignesCourantes() {
        return FauxLint.lignes(session.document().text());
    }

    /**
     * L'aperçu rapide de la définition du symbole au curseur (D-11) :
     * la déclaration se lit dans une fenêtre flottante — sans quitter
     * le fichier.
     */
    private void apercuDefinition() {
        List<SelRange> plages = session.selection().ranges();
        int curseur = plages.isEmpty() ? 0 : plages.get(0).caret();
        ApercuDefinition apercu = ApercuDefinition.de(entreesPlan,
                lignesCourantes(),
                ApercuDefinition.symboleAu(session.document().text(), curseur),
                curseur);
        if (apercu == null) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.titre_apercu, apercu.nom()))
                .setMessage(String.join("\n", apercu.extrait()))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /**
     * Les revues par blocs (D-12, F-04) vivent dans leurs ateliers ;
     * après une fusion appliquée, la scène se rafraîchit — marques,
     * pastilles et repères compris.
     */
    private void apresFusion() {
        editeur.invalidate();
        majMarques();
        majPastillesEtReperes();
    }

    /**
     * Le rendu délégué d'un repère {@code @Preview} (C-11) : la
     * démonstration joue l'hôte et rend le composable dans sa propre
     * scène — un contrat vérifié, pas un rendu Compose embarqué.
     */
    private void renduPreview(int ligne) {
        String composable = "";
        for (ReperesPreview.ReperePreview repere : reperes) {
            if (repere.ligne() == ligne) {
                composable = repere.libelle();
                break;
            }
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.titre_rendu_preview))
                .setMessage(getString(R.string.message_rendu_preview,
                        composable, ligne + 1))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /**
     * Rafraîchit les marques d'intégration de la gouttière : le diff
     * ligne à ligne face au contenu d'origine (D-03), les arrêts (D-04)
     * et les repères d'exécution (D-05).
     */
    private void majMarques() {
        if (editeur == null) {
            return;
        }
        EtatLignesVcs etat = DiffLignes.etat(reference, session.document().text());
        MarquesIntegration marques = AdaptateurMarques.depuis(etat,
                debogueur.arrets(), chemin, debogueur.ligneCourante(),
                debogueur.pileCourante());
        editeur.marquesGouttiere().setIntegration(marques);
        editeur.invalidate();
    }

    private void majPosition() {
        Document document = session.document();
        List<SelRange> plages = session.selection().ranges();
        int curseur = plages.isEmpty() ? 0 : plages.get(0).caret();
        int ligne = document.lineForOffset(curseur);
        int colonne = curseur - document.lineStart(ligne) + 1;
        etiquettePosition.setText(getString(R.string.position_curseur,
                ligne + 1, colonne));
    }

    private int largeurEcran() {
        return getResources().getDisplayMetrics().widthPixels;
    }
}
