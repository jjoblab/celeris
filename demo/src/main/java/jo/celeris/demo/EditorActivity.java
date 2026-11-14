package jo.celeris.demo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

import jo.celeris.android.host.popups.LanguageSession;
import jo.celeris.android.host.popups.PalettesPopups;
import jo.celeris.android.host.popups.PopupsEditeur;
import jo.celeris.android.host.spi.CheminDoc;
import jo.celeris.android.host.spi.DiagnosticHote;
import jo.celeris.android.host.spi.EtatLignesVcs;
import jo.celeris.android.host.spi.LigneArret;
import jo.celeris.android.host.spi.ValeurEnLigne;
import jo.celeris.android.view.ClasseLargeur;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.view.FontSpec;
import jo.celeris.android.view.MarquesIntegration;
import jo.celeris.android.view.NavigationCommands;
import jo.celeris.android.view.PastillesCouleur;
import jo.celeris.android.view.ReperesPreview;
import jo.celeris.android.view.ValeurLigne;
import jo.celeris.android.widgets.BarreToolbar;
import jo.celeris.android.widgets.PopupLayer;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.core.intelligence.ResultatsAller;
import jo.celeris.core.language.model.LanguageSupport;

import jo.celeris.core.syntax.structure.OutlineEntry;

import jo.celeris.core.text.Document;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

/**
 * L'atelier d'édition de la démonstration : un fichier d'exemple ouvert
 * dans la vue Canvas complète — coloration, plis, recherche, plan,
 * thèmes, zoom et annulation — porté par un vrai layout (barre
 * d'application au menu d'actions, barre d'état en police à chasse
 * fixe). Les surfaces fonctionnelles vivent en popups (ADR-018) : les
 * popups de l'éditeur ancrées sur la couche, le plan, la recherche, les revues
 * et les réglages en feuilles. Les réglages persistés se réappliquent
 * à la fermeture de leur feuille.
 */
public final class EditorActivity extends AppCompatActivity {

    /** La clé de l'intention : le nom du fichier d'exemple à ouvrir. */
    public static final String EXTRA_FICHIER = "fichier";

    private static final String CLE_NUMEROS = "gouttiere.numeros";
    private static final String CLE_RETOUR = "retour.souple";
    private static final String CLE_TAILLE = "police.taille";
    private static final String CLE_LIGATURES = "police.ligatures";
    private static final float TAILLE_DEFAUT = 14f;
    private static final float TAILLE_MIN = 8f;
    private static final float TAILLE_MAX = 40f;
    static final float PAS_TAILLE = 2f;

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
    /** La feuille de l'aller à (ADR-018, catégorie C — « Go to File /
     *  Class / Symbol ») : la recherche floue y répond, le choix saute. */
    private FeuilleAller feuilleAller;
    private List<ReperesPreview.ReperePreview> reperes = List.of();
    /** L'état de débogage — arrêts, pause simulée, étiquette (D-04, D-05).
     *  Visible du paquet : le catalogue des actions y bascule la pause. */
    EtatDebogage etatDebogage;
    /** L'état des diagnostics — lint de l'hôte et serveur réel (M8.6). */
    private EtatDiagnostics etatDiagnostics;
    private AtelierLint atelierLint;
    private AtelierDiff atelierDiff;
    private AtelierAgent atelierAgent;
    private AtelierIA atelierIA;
    private PopupsEditeur popups;
    /** Le pont provisoire des surfaces réveillées — la démonstration
     *  teste les cartes du kit sur le manager de l'éditeur, le temps
     *  du déménagement chez l'orchestrateur. */
    private AtelierRefactor atelierRefactor;
    private AtelierRecherche atelierRecherche;
    private AtelierSelection atelierSelection;
    /** La barre d'application du kit (le design du dépôt de
     *  départ) : la carte du kit remplace le menu système de
     *  débordement, la barre de touches vit au-dessus du clavier. */
    private BarreToolbar barreToolbar;
    /** La session de langage — serveur de référence en processus (M7.5,
     *  §8) ou serveur déclaré disponible par l'hôte (M7.6, H-13). */
    private LanguageSession serveurLangage;
    /** Les réglages de scène (M7.5, §9) : retenue et surimpression. */
    private SceneReglages sceneReglages;
    /** La bascule de thème (D-13) — rotation, persistance, libellé. */
    private BasculeTheme basculeTheme;
    private List<OutlineEntry> entreesPlan = List.of();
    /** Le catalogue des actions (menu, carte de débordement,
     *  intentions) — visible du paquet, monté après la scène. */
    private CatalogueActions catalogue;
    /** L'atelier de l'aperçu d'un correctif (B-18) : le correctif
     *  proposé sur la sélection se prévise avant l'acceptation. */
    private AtelierCorrectif atelierCorrectif;

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
        atelierDiff = new AtelierDiff(this, reference, session, this::paletteFeuilles, this::apresFusion);
        // L'atelier de l'aperçu du correctif (B-18) : le serveur se lit à
        // l'ouverture, l'uri suit le fichier ouvert.
        atelierCorrectif = new AtelierCorrectif(this, session, this::paletteFeuilles,
                this::apresFusion, () -> serveurLangage, () -> "file:///" + nomFichier);
        atelierAgent = new AtelierAgent(this, session, this::paletteFeuilles,
                this::apresFusion);
        session.addEditListener((debut, fin, retire, insere) -> {
            majPosition();
            majMarques();
            majPastillesEtReperes();
            // les touches de code se ré-évaluent à la frappe : l'état
            // activé suit le document
            if (barreToolbar != null) {
                barreToolbar.barreTouches().refreshEnabled();
            }
            // La frappe suivante refuse le fantôme (F-09) : l'atelier IA
            // suit s'il est déjà en place.
            if (atelierIA != null) {
                atelierIA.surEdition();
            }
        });
        setContentView(R.layout.activity_editor);
        brancherScene();
        // La feuille des réglages vit après la scène : sa palette se dérive
        // du thème de l'éditeur, qui nait dans la scène (ADR-018).
        feuilleReglages = new FeuilleReglages(this, this::paletteFeuilles,
                this::appliquerPreferences);
        // Le catalogue des actions suit la scène : les ateliers s'y
        // trouvent montés — le menu, la carte et les intentions y lisent.
        catalogue = new CatalogueActions(this, nomFichier, atelierLint,
                atelierDiff, atelierCorrectif, atelierAgent, atelierIA, popups,
                atelierRefactor, atelierRecherche, basculeTheme,
                feuilleReglages, () -> entreesPlan, this::lignesCourantes, barreToolbar);
        catalogue.brancher();
        popups.configurerIntentions(catalogue.intentions());
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
        // Les popups se détachent — l'observateur du layout suit.
        popups.detacher();
        barreToolbar.detacher();
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
        editeur.intelligence().setProvider(new DemoIntelligence(this::langueOuverte));
        atelierIA = new AtelierIA(this, editeur, session, nomFichier, this::paletteFeuilles,
                this::verdictRefactor, () -> feuilleReglages.montrer());
        brancherAteliers();
        // La feuille du plan vit après le thème : sa palette s'en dérive.
        feuilleAller = new FeuilleAller(this, this::paletteFeuilles,
                this::sauterAller, () -> entreesPlan, this::nomsDuCatalogue);
        feuillePlan = new FeuillePlan(this, this::paletteFeuilles, this::sautVersEntree);
        editeur.setSurPlan(entrees -> {
            entreesPlan = entrees;
            editeur.post(() -> {
                feuillePlan.setEntrees(entrees);
                popups.setPlan(entrees);
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
        brancherSuiviTactile();
        poserEtatsDelegues();
    }

    /** Toute animation de vue (défilement, clic) rafraîchit la
     *  position et les états des touches de code — les clics restent
     *  portés par EditorView, qui redéfinit performClick. */
    private void brancherSuiviTactile() {
        editeur.setOnTouchListener((vue, evenement) -> {
            vue.post(this::majPosition);
            // les touches de code se ré-évaluent au geste du doigt —
            // l'état activé suit le caret
            if (barreToolbar != null) {
                vue.post(() -> barreToolbar.barreTouches()
                        .refreshEnabled());
            }
            return false;
        });
    }

    /** La gouttière : les arrêts à l'APPUIS LONG (D-04, la reprise), le
     *  curseur au clic des numéros, le repère @Preview au coin (C-11). */
    private void brancherGouttiere() {
        // l'appui long de gouttière bascule l'arrêt — l'hôte y décide
        editeur.setSurBasculeArret(ligne -> {
            etatDebogage.basculerArret(ligne);
            majMarques();
        });
        editeur.marquesGouttiere().setSurClicLigne(this::poserCurseurALigne);
        editeur.marquesGouttiere().setSurRenduPreview(ligne ->
                popups.docAncre().renduPreview(ligne, reperes));
    }

    /** Pose le curseur au début de la ligne visée (clic de numéros). */
    private void poserCurseurALigne(int ligne) {
        var document = session.document();
        int borne = Math.min(Math.max(0, ligne), document.lineCount() - 1);
        session.setSelection(jo.celeris.core.text.MultiSelection.of(
                SelRange.caretAt(document.lineStart(borne))));
        editeur.invalidate();
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
        etatDiagnostics = new EtatDiagnostics(this, editeur, popups,
                etiquetteArrets, chemin);
        serveurLangage.poserAuditeurDiagnostics(etatDiagnostics::surServeur);
    }

    /** Les ateliers d'interaction : l'IA, les popups de l'éditeur sur
     *  la couche (PIEGE-27 : jamais de focus volé), la recherche
     *  dépliable et la sélection au presse-papiers local. */
    private void brancherAteliers() {
        // La refactorisation et le serveur déclaré (servers.json décide).
        atelierRefactor = new AtelierRefactor(this, editeur, session,
                nomFichier, this::paletteFeuilles, this::verdictRefactor);
        serveurLangage = DeclaredServers.open(langueOuverte());
        // la couche posée sur l'éditeur porte les popups de l'éditeur.
        PopupLayer couche = findViewById(R.id.couche_popups);
        popups = new PopupsEditeur(editeur, session, couche,
                basculeTheme.themeCourant(),
                prefs.getFloat(CLE_TAILLE, TAILLE_DEFAUT), new CoachDemo());
        brancherGestesEditeur();
        // le verre des popups se dérive du fond de l'éditeur — la barre
        // de touches suit la même source.
        basculeTheme.setSurChangement(theme -> {
            popups.surTheme(theme);
            if (barreToolbar != null) {
                barreToolbar.surPalette(paletteFeuilles());
            }
        });
        popups.setNomFichier(nomFichier);
        popups.brancherServeur(serveurLangage, "file:///" + nomFichier,
                langueOuverte());
        atelierSelection = new AtelierSelection(editeur, session,
                this::langueOuverte, popups::rafraichirBarreSelection);
        // les actions VIVES : Coller ne se pose que lorsque le
        // presse-papiers est plein, le débordement porte commenter,
        // casse, indentation et duplication — chaque action SON
        // pictogramme (l'audit du 2026-11-14)
        popups.configurerActionsSelectionVives(
                atelierSelection.principalesVives(
                        getString(R.string.barre_couper),
                        getString(R.string.barre_copier),
                        getString(R.string.barre_coller),
                        getString(R.string.barre_tout)),
                atelierSelection.debordementVives(
                        getString(R.string.barre_commenter),
                        getString(R.string.barre_majuscules),
                        getString(R.string.barre_minuscules),
                        getString(R.string.barre_indenter),
                        getString(R.string.barre_dupliquer)));
        atelierRecherche = new AtelierRecherche(this, editeur, session,
                this::paletteFeuilles, popups::ouvrirRecherche,
                popups::fermerRecherche);
        atelierRecherche.brancher();
        brancherSurvolDoubleTap();
        brancherToolbar();
    }

    /** Les gestes de l'hôte se posent chez l'orchestrateur — le pont
     *  est mort : toutes les surfaces réveillées vivent chez
     *  l'éditeur, la démonstration ne teste plus qu'elle-même. */
    private void brancherGestesEditeur() {
        popups.configurerGabaritMethode(atelierRefactor::insererGabarit);
        popups.configurerRelanceLint(atelierLint::analyser);
    }

    /** Le survol se sert AU DOUBLE-TAP et au « i » de la barre (le
     *  redressement du 2026-11-13) : le color picker d'abord. */
    private void brancherSurvolDoubleTap() {
        editeur.setSurDoubleTap(offset ->
                popups.ouvrirColorPicker(offset)
                        || popups.ouvrirSurvolAuDoubleTap(offset));
        // la délégation clavier (chantier d'unification) : les touches
        // matérielles remontent aux popups AVANT la table de raccourcis
        editeur.setSurTouchePopup(popups::surTouche);
    }

    /** Le dépôt de départ : le débordement de la toolbar vit dans la
     *  CARTE du kit, la barre de touches au-dessus du clavier. */
    private void brancherToolbar() {
        barreToolbar = new BarreToolbar(
                popups.manager(), paletteFeuilles(),
                getString(R.string.menu_plus));
        barreToolbar.poserBarreTouches(
                new AtelierTouchesCode(this, editeur, session,
                        this::langueOuverte).touches());
    }

    /** Le support du langage ouvert — la voie de la démonstration :
     *  le nom du fichier y décide, le texte porte les déclarations. */
    private LanguageSupport langueOuverte() {
        return LangagesDemo.langageDe(nomFichier, null,
                session.document().text());
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
        // D-14 — la police à ligatures de la démonstration (Fira
        // Code, OFL — attribuée au NOTICE) : posée sous sa famille,
        // la mesure et le tracé la suivent ; ligatures coupées sans
        // la bascule, la famille revient au mono de la plateforme.
        Typeface fira = ResourcesCompat.getFont(this,
                R.font.fira_code_regular);
        boolean ligatures = prefs.getBoolean(CLE_LIGATURES, false);
        if (fira != null) {
            editeur.poserPolice("fira", fira);
        }
        editeur.setLigatures(ligatures);
        editeur.setNumerosVisibles(prefs.getBoolean(CLE_NUMEROS, true));
        editeur.setSoftWrap(prefs.getBoolean(CLE_RETOUR, false));
        editeur.setFont(new FontSpec(fira != null && ligatures
                ? "fira" : "mono",
                prefs.getFloat(CLE_TAILLE, TAILLE_DEFAUT)));
        if (popups != null) {
            popups.surTailleTexte(
                    prefs.getFloat(CLE_TAILLE, TAILLE_DEFAUT));
        }
        // M7.5, §9 — la retenue d'analyse et la surimpression de
        // diagnostic, depuis les préférences.
        sceneReglages.appliquer(prefs);
        LanguageSupport langue = langueOuverte();
        editeur.setLanguage(langue);
        etiquetteLangue.setText(langue == null ? nomFichier : langue.displayName());
        basculeTheme.majTitre();
        majTitreRetourLigne();
        majPosition();
    }

    void zoomerPolice(float pas) {
        float courante = prefs.getFloat(CLE_TAILLE, TAILLE_DEFAUT);
        float bornee = Math.min(Math.max(courante + pas, TAILLE_MIN), TAILLE_MAX);
        prefs.edit().putFloat(CLE_TAILLE, bornee).apply();
        editeur.setFont(new FontSpec("mono", bornee));
        if (popups != null) {
            popups.surTailleTexte(bornee);
        }
    }

    void rejouer(boolean enAvant) {
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
    void basculerPlan() {
        feuillePlan.basculer();
    }

    /** Bascule l'aller à en feuille du bas (ADR-018, catégorie C) :
     *  la requête tape, la floue répond — le choix saute au symbole du
     *  fichier ouvert, ou ouvre le fichier du catalogue. */
    void basculerAller() {
        feuilleAller.basculer();
    }

    /** Le saut de l'aller à : le symbole porte au plan ouvert, le
     *  fichier ouvre SA page — l'activité relance l'édition. */
    private void sauterAller(ResultatsAller.Resultat choix) {
        if (ResultatsAller.GENRE_SYMBOLE.equals(choix.genre())) {
            for (OutlineEntry entree : entreesPlan) {
                if (entree.name().equals(choix.libelle())) {
                    NavigationCommands.placeCaret(editeur,
                            entree.startOffset());
                    return;
                }
            }
            return;
        }
        if (choix.libelle().equals(nomFichier)) {
            return;
        }
        Intent ouverture = new Intent(this, EditorActivity.class);
        ouverture.putExtra(EXTRA_FICHIER, choix.libelle());
        startActivity(ouverture);
    }

    /** Les noms des fichiers du catalogue — l'aller à fichier y sert. */
    private List<String> nomsDuCatalogue() {
        List<String> noms = new ArrayList<>();
        for (FichierExemple fichier : FichiersExemple.catalogue()) {
            noms.add(fichier.nom());
        }
        return noms;
    }

    /** La palette courante des feuilles — le verre suit le thème de
     *  l'éditeur et sa taille de texte. */
    private PopupPalette paletteFeuilles() {
        return PalettesPopups.de(basculeTheme.themeCourant(),
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
                || (feuilleAller != null && feuilleAller.surRetour())
                || atelierRefactor.surRetour()
                || atelierDiff.surRetour()
                || atelierCorrectif.surRetour()
                || atelierAgent.surRetour()
                || atelierIA.surRetour();
    }

    /**
     * Bascule le retour à la ligne souple (A-18) depuis l'atelier :
     * l'état se persiste — les réglages et le retour de l'atelier
     * relisent le même état — et le libellé du menu le dit.
     */
    void basculerRetourLigne() {
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

    private void sautVersEntree(OutlineEntry entree) {
        NavigationCommands.placeCaret(editeur, entree.startOffset());
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
    void corrigerLint() {
        int corriges = atelierLint.corriger();
        if (corriges > 0) {
            etiquetteArrets.setText(getString(R.string.lint_correctifs, corriges));
            popups.notifier(
                    getString(R.string.titre_notification),
                    getString(R.string.lint_correctifs, corriges));
        } else {
            etiquetteArrets.setText(R.string.lint_propre);
            popups.notifier(
                    getString(R.string.titre_notification),
                    getString(R.string.lint_propre));
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
    void majMarques() {
        if (editeur == null) {
            return;
        }
        EtatLignesVcs etat = DiffLignes.etat(reference, session.document().text());
        MarquesIntegration marques = AdaptateurMarques.depuis(etat,
                etatDebogage.debogueur().arrets(), chemin,
                etatDebogage.debogueur().ligneCourante(), etatDebogage.debogueur().pileCourante());
        editeur.marquesGouttiere().setIntegration(marques);
        annoncerPauseSiPausee();
        majValeursEnLigne();
        editeur.invalidate();
    }

    /** Les valeurs de débogage en ligne (E-03) : la pause les pose en
     *  fin de ligne — le rendu de la vue (expression = valeur), la
     *  reprise les retire. La vue ne connaît pas le débogueur :
     *  l'activité transpose. */
    private void majValeursEnLigne() {
        LigneArret pause = etatDebogage.debogueur().ligneCourante();
        List<ValeurLigne> posees = new ArrayList<>();
        if (pause != null) {
            List<ValeurEnLigne> valeurs = etatDebogage.debogueur()
                    .valeursEnLigne(pause.ligne());
            for (ValeurEnLigne valeur : valeurs) {
                posees.add(new ValeurLigne(valeur.ligne(),
                        valeur.expression() + " = " + valeur.valeur()));
            }
        }
        editeur.setValeursEnLigne(posees);
    }

    /** La pause de débogage s'annonce — la reprise aussi : la bulle du
     *  coin bas-droit porte l'événement réel, l'horloge la referme. */
    private void annoncerPauseSiPausee() {
        LigneArret pause = etatDebogage.debogueur().ligneCourante();
        if (pause != null) {
            popups.notifier(getString(R.string.titre_notification),
                    getString(R.string.etat_pause, pause.ligne()));
        }
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
