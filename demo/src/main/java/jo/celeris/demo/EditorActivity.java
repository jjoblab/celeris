package jo.celeris.demo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jo.celeris.android.host.spi.CheminDoc;
import jo.celeris.android.host.spi.DiagnosticHote;
import jo.celeris.android.host.spi.EtatLignesVcs;
import jo.celeris.android.view.ClasseLargeur;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.view.FontSpec;
import jo.celeris.android.view.MarquesIntegration;
import jo.celeris.android.view.NavigationCommands;
import jo.celeris.android.view.PastillesCouleur;
import jo.celeris.android.view.ReperesPreview;
import jo.celeris.android.widgets.IntentionsMenuView;
import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.core.language.LanguageSupport;
import jo.celeris.core.syntax.OutlineEntry;
import jo.celeris.core.text.Document;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

/**
 * L'atelier d'édition de la démonstration : un fichier d'exemple ouvert
 * dans la vue Canvas complète — coloration, plis, recherche, plan,
 * thèmes, zoom et annulation — porté par un vrai layout (barre
 * d'application au menu d'actions, barre d'état en police à chasse
 * fixe). Les surfaces fonctionnelles vivent en popups (ADR-018) : les
 * dix popups ancrées sur la couche, le plan, la recherche, les revues
 * et les réglages en feuilles. Les réglages persistés se réappliquent
 * à la fermeture de leur feuille.
 */
public final class EditorActivity extends AppCompatActivity {

    /** La clé de l'intention : le nom du fichier d'exemple à ouvrir. */
    public static final String EXTRA_FICHIER = "fichier";

    private static final String CLE_NUMEROS = "gouttiere.numeros";
    private static final String CLE_RETOUR = "retour.souple";
    private static final String CLE_TAILLE = "police.taille";
    private static final float TAILLE_DEFAUT = 14f;
    private static final float TAILLE_MIN = 8f;
    private static final float TAILLE_MAX = 40f;
    private static final float PAS_TAILLE = 2f;

    private EditorView editeur;
    private TextSession session;
    private TextView etiquettePosition;
    private TextView etiquetteLangue;
    private TextView etiquetteArrets;
    private SharedPreferences prefs;
    private String nomFichier;
    private String reference;
    private CheminDoc chemin;
    /** La feuille des réglages (ADR-018, C12) — l'écran plein cadre migre. */
    private FeuilleReglages feuilleReglages;
    /** La feuille du plan (ADR-018 : « plan » vit en feuille du bas). */
    private FeuillePlan feuillePlan;
    private List<ReperesPreview.ReperePreview> reperes = List.of();
    /** L'état de débogage — arrêts, pause simulée, étiquette (D-04, D-05). */
    private EtatDebogage etatDebogage;
    /** L'état des diagnostics — lint de l'hôte et serveur réel (M8.6). */
    private EtatDiagnostics etatDiagnostics;
    private AtelierLint atelierLint;
    private AtelierDiff atelierDiff;
    private AtelierAgent atelierAgent;
    private AtelierIA atelierIA;
    private AtelierPopups atelierPopups;
    private AtelierRefactor atelierRefactor;
    private AtelierRecherche atelierRecherche;
    private AtelierSelection atelierSelection;
    /** La session de langage — serveur de référence en processus (M7.5, §8)
     *  ou serveur tiers retiré pour les fichiers XML (M8.6). */
    private LanguageSession serveurLangage;
    /** Les réglages de scène (M7.5, §9) : retenue et surimpression. */
    private SceneReglages sceneReglages;
    /** La bascule de thème (D-13) — rotation, persistance, libellé. */
    private BasculeTheme basculeTheme;
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
        atelierLint = new AtelierLint(nomFichier, session, this::surLint);
        atelierDiff = new AtelierDiff(this, reference, session,
                this::paletteFeuilles, this::apresFusion);
        atelierAgent = new AtelierAgent(this, session,
                this::paletteFeuilles, this::apresFusion);
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
        // La feuille des réglages vit après la scène : sa palette se
        // dérive du thème de l'éditeur, qui nait dans la scène (ADR-018).
        feuilleReglages = new FeuilleReglages(this, this::paletteFeuilles,
                this::appliquerPreferences);
        brancherBarre();
        appliquerPreferences();
        appliquerBordABord();
        brancherRetour();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Les réglages ont pu changer pendant la pause : réappliquer.
        appliquerPreferences();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // La boucle de la surimpression se repose en coulisses.
        if (sceneReglages != null) {
            sceneReglages.reposer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // La session de langage vit avec l'activité — transports fermés d'entrée.
        if (serveurLangage != null) {
            serveurLangage.close();
        }
    }

    private FichierExemple resoudreFichier() {
        String demande = getIntent().getStringExtra(EXTRA_FICHIER);
        // Le paquet jouet (H-04) : l'échantillon ne se résout que si le
        // paquet vit au stockage — la carte n'ouvrait pas sinon.
        String nom = demande == null ? FichiersExemple.premier().nom() : demande;
        FichierExemple fichier = FichiersExemple.parNom(nom,
                PaquetJouet.installe(getFilesDir().toPath()
                        .resolve(PaquetJouet.DOSSIER)));
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
        actionsMenu.put(R.id.action_debogage, () -> {
            etatDebogage.basculerPause();
            majMarques();
        });
        actionsMenu.put(R.id.action_retour_ligne, this::basculerRetourLigne);
        actionsMenu.put(R.id.action_annuler, () -> rejouer(false));
        actionsMenu.put(R.id.action_retablir, () -> rejouer(true));
        actionsMenu.put(R.id.action_lint, this::analyserLint);
        actionsMenu.put(R.id.action_corriger, this::corrigerLint);
        actionsMenu.put(R.id.action_gabarit, atelierRefactor::insererGabarit);
        actionsMenu.put(R.id.action_formater, atelierRefactor::formaterDocument);
        actionsMenu.put(R.id.action_renommer, atelierRefactor::renommerSymbole);
        actionsMenu.put(R.id.action_importer, atelierRefactor::importerListe);
        actionsMenu.put(R.id.action_apercu, () -> atelierPopups.docAncre()
                .apercu(entreesPlan, lignesCourantes()));
        actionsMenu.put(R.id.action_diff, atelierDiff::ouvrir);
        actionsMenu.put(R.id.action_agent, atelierAgent::ouvrir);
        actionsMenu.put(R.id.action_ia_fantome, () -> atelierIA.demanderFantome());
        actionsMenu.put(R.id.action_ia_prediction, () -> atelierIA.demanderPrediction());
        actionsMenu.put(R.id.action_theme, basculeTheme::tourner);
        actionsMenu.put(R.id.action_diminuer, () -> zoomerPolice(-PAS_TAILLE));
        actionsMenu.put(R.id.action_agrandir, () -> zoomerPolice(PAS_TAILLE));
        actionsMenu.put(R.id.action_reglages, feuilleReglages::montrer);
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
        // M7.5, §9 — les réglages de scène se posent d'entrée.
        sceneReglages = new SceneReglages(editeur,
                findViewById(R.id.surimpression));
        basculeTheme = new BasculeTheme(this, editeur, prefs);
        // L'intelligence de la démonstration se pose sur la vue (M7.5,
        // §8) : frappe intelligente, Entrée indentée, collage réindenté,
        // complétion par les mots du code.
        editeur.intelligence().setProvider(new DemoIntelligence());
        atelierIA = new AtelierIA(this, editeur, session, nomFichier,
                this::paletteFeuilles, this::verdictRefactor,
                () -> feuilleReglages.montrer());
        brancherAteliers();
        // La feuille du plan vit après le thème : sa palette s'en dérive.
        feuillePlan = new FeuillePlan(this, this::paletteFeuilles,
                this::sautVersEntree);
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
                feuillePlan.setEntrees(entrees);
                atelierPopups.setPlan(entrees);
            });
        });
        // La gouttière s'adapte à la classe de largeur de la fenêtre
        // (D-19) — le pliable déplié change de classe d'un geste.
        editeur.setClasseLargeur(ClasseLargeur.de(
                getResources().getConfiguration().screenWidthDp));
        brancherGouttiere();
        // Les pastilles et repères du fichier ouvert se posent d'entrée.
        majPastillesEtReperes();
        brancherPlanEtEtiquettes();
        poserEtatsDelegues();
    }

    /** La gouttière : les arrêts au clic des numéros (D-04), le rendu
     *  d'un repère @Preview en popup ancrée au coin (C-11, ADR-018). */
    private void brancherGouttiere() {
        editeur.marquesGouttiere().setSurClicLigne(ligne -> {
            etatDebogage.basculerArret(ligne);
            majMarques();
        });
        editeur.marquesGouttiere().setSurRenduPreview(ligne ->
                atelierPopups.docAncre().renduPreview(ligne, reperes));
    }

    /** Le plan en feuille du bas (ADR-018) et les étiquettes de barre
     *  d'état se posent. */
    private void brancherPlanEtEtiquettes() {
        etiquettePosition = findViewById(R.id.etat_position);
        etiquetteLangue = findViewById(R.id.etat_langue);
        etiquetteArrets = findViewById(R.id.etat_arrets);
    }

    /**
     * Les états délégués — débogage d'un côté, diagnostics de l'autre
     * — se posent une fois la scène complète : le serveur déclaré et
     * disponible y publie ses diagnostics de validation (M8.6), le
     * serveur de référence reste muet (ADR-005 : la vue ne sait pas
     * qui a répondu — la porte de session, elle, écoute qui parle).
     */
    private void poserEtatsDelegues() {
        etatDebogage = new EtatDebogage(this, editeur, session,
                etiquetteArrets, chemin);
        etatDiagnostics = new EtatDiagnostics(this, editeur, atelierPopups,
                etiquetteArrets, chemin);
        serveurLangage.poserAuditeurDiagnostics(etatDiagnostics::surServeur);
    }

    /**
     * Les ateliers d'interaction (M7.5, §7, reprise) : l'IA (F-01 à
     * F-03), les DIX popups sur la couche posée — jamais de focus volé
     * (PIEGE-27) —, la recherche dépliable et la sélection au
     * presse-papiers local.
     */
    private void brancherAteliers() {
        // La refactorisation et la session de langage déclarée (M7.5,
        // §8 — M7.6, H-13 : la déclaration serveurs.json décide, le
        // montage exécute, le serveur de référence reste le repli).
        atelierRefactor = new AtelierRefactor(this, editeur, session,
                nomFichier, this::paletteFeuilles, this::verdictRefactor);
        LanguageSupport langueOuverte = LangagesDemo.langageDe(nomFichier,
                null, session.document().text());
        serveurLangage = DeclaredServers.open(langueOuverte);
        // L'atelier des popups : la couche posée sur l'éditeur porte
        // les dix popups, la machine à états décide qui reste ouvert.
        PopupLayer couche = findViewById(R.id.couche_popups);
        atelierPopups = new AtelierPopups(editeur, session, couche,
                basculeTheme.themeCourant(),
                prefs.getFloat(CLE_TAILLE, TAILLE_DEFAUT));
        // Le thème tourne : la palette des popups suit (le verre du
        // dépôt de départ se dérive du fond de l'éditeur).
        basculeTheme.setSurChangement(atelierPopups::surTheme);
        atelierPopups.setNomFichier(nomFichier);
        atelierPopups.configurerIntentions(intentionsAuCurseur());
        atelierPopups.brancherServeur(serveurLangage,
                "file:///" + nomFichier);
        atelierSelection = new AtelierSelection(editeur, session);
        atelierPopups.configurerActionsSelection(
                atelierSelection.principales(
                        getString(R.string.barre_couper),
                        getString(R.string.barre_copier),
                        getString(R.string.barre_coller)),
                atelierSelection.debordement(
                        getString(R.string.barre_tout)));
        atelierRecherche = new AtelierRecherche(this, editeur, session,
                this::paletteFeuilles, atelierPopups::ouvrirRecherche,
                atelierPopups::fermerRecherche);
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
        basculeTheme.appliquer();
        editeur.setNumerosVisibles(prefs.getBoolean(CLE_NUMEROS, true));
        editeur.setSoftWrap(prefs.getBoolean(CLE_RETOUR, false));
        editeur.setFont(new FontSpec("mono",
                prefs.getFloat(CLE_TAILLE, TAILLE_DEFAUT)));
        if (atelierPopups != null) {
            atelierPopups.surTailleTexte(
                    prefs.getFloat(CLE_TAILLE, TAILLE_DEFAUT));
        }
        // M7.5, §9 — la retenue d'analyse et la surimpression de
        // diagnostic, depuis les préférences.
        sceneReglages.appliquer(prefs);
        LanguageSupport langue = LangagesDemo.langageDe(nomFichier,
                null, session.document().text());
        editeur.setLanguage(langue);
        etiquetteLangue.setText(langue == null ? nomFichier : langue.displayName());
        basculeTheme.majTitre();
        majTitreRetourLigne();
        majPosition();
    }

    private void zoomerPolice(float pas) {
        float courante = prefs.getFloat(CLE_TAILLE, TAILLE_DEFAUT);
        float bornee = Math.min(Math.max(courante + pas, TAILLE_MIN), TAILLE_MAX);
        prefs.edit().putFloat(CLE_TAILLE, bornee).apply();
        editeur.setFont(new FontSpec("mono", bornee));
        if (atelierPopups != null) {
            atelierPopups.surTailleTexte(bornee);
        }
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

    /**
     * Bascule le plan en feuille du bas (ADR-018 : « plan » vit en
     * feuille du bas) : le fichier s'y parcourt, l'entrée choisie y
     * saute et la feuille se referme.
     */
    private void basculerPlan() {
        feuillePlan.basculer();
    }

    /** La palette courante des feuilles — le verre suit le thème de
     *  l'éditeur et sa taille de texte. */
    private PopupPalette paletteFeuilles() {
        return PalettesDemo.de(basculeTheme.themeCourant(),
                prefs.getFloat(CLE_TAILLE, TAILLE_DEFAUT));
    }

    /** La touche retour ferme la feuille ouverte — sinon elle poursuit
     *  son chemin vers l'hôte (ADR-018 : la feuille se referme). */
    private void brancherRetour() {
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!fermerFeuilleActive()) {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    /** Ferme la feuille ouverte, s'il y en a une — le retour s'y consomme. */
    private boolean fermerFeuilleActive() {
        return feuilleReglages.surRetour()
                || atelierRecherche.surRetour()
                || (feuillePlan != null && feuillePlan.surRetour())
                || atelierRefactor.surRetour()
                || atelierDiff.surRetour()
                || atelierAgent.surRetour()
                || atelierIA.surRetour();
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
                        () -> atelierPopups.docAncre().apercu(
                                entreesPlan, lignesCourantes())),
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

    private void analyserLint() {
        atelierLint.analyser();
    }

    /** Le relais lint : les lignes marquées accompagnent l'état. */
    private void surLint(List<DiagnosticHote> diagnostics) {
        if (etatDiagnostics != null) {
            etatDiagnostics.surLint(diagnostics, atelierLint.lignesMarquees());
        }
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
                etatDebogage.debogueur().arrets(), chemin,
                etatDebogage.debogueur().ligneCourante(),
                etatDebogage.debogueur().pileCourante());
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
}
