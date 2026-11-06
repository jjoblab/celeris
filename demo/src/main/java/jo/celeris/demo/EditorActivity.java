package jo.celeris.demo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

import jo.celeris.android.host.spi.Arret;
import jo.celeris.android.host.spi.CadrePile;
import jo.celeris.android.host.spi.CheminDoc;
import jo.celeris.android.host.spi.EtatLignesVcs;
import jo.celeris.android.host.spi.FauxDebogueur;
import jo.celeris.android.host.spi.LigneArret;
import jo.celeris.android.view.EditorTheme;
import jo.celeris.android.view.EditorThemes;
import jo.celeris.android.view.EditorView;
import jo.celeris.android.view.FontSpec;
import jo.celeris.android.view.MarquesIntegration;
import jo.celeris.android.view.NavigationCommands;
import jo.celeris.android.widgets.OutlineView;
import jo.celeris.android.widgets.SearchBarModel;
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
    private SearchBarModel recherche;
    private OutlineView plan;
    private LinearLayout barreRechercheVue;
    private EditText champRecherche;
    private TextView compteurRecherche;
    private TextView etiquettePosition;
    private TextView etiquetteLangue;
    private TextView etiquetteArrets;
    private SharedPreferences prefs;
    private String nomFichier;
    private String reference;
    private CheminDoc chemin;
    private final FauxDebogueur debogueur = new FauxDebogueur();

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
        recherche = new SearchBarModel(session.document(), System::nanoTime);
        session.addEditListener((debut, fin, retire, insere) -> {
            majPosition();
            majMarques();
        });
        setContentView(R.layout.activity_editor);
        brancherBarre();
        brancherScene();
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

        barreRechercheVue = findViewById(R.id.barre_recherche);
        champRecherche = findViewById(R.id.champ_recherche);
        champRecherche.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int d, int n, int a) {
                // seul l'après intéresse la barre
            }

            @Override
            public void onTextChanged(CharSequence s, int d, int n, int a) {
                // seul l'après intéresse la barre
            }

            @Override
            public void afterTextChanged(Editable s) {
                recherche.setQuery(s.toString());
                majCompteurRecherche();
            }
        });
        compteurRecherche = findViewById(R.id.compteur_recherche);
        bouton(R.id.bouton_precedent, vue -> sauterRecherche(false));
        bouton(R.id.bouton_suivant, vue -> sauterRecherche(true));
        bouton(R.id.bouton_fermer_recherche, vue -> fermerRecherche());
    }

    private void bouton(int identifiant, View.OnClickListener ecouteur) {
        ImageButton bouton = findViewById(identifiant);
        bouton.setOnClickListener(ecouteur);
    }

    private boolean surMenu(MenuItem element) {
        int action = element.getItemId();
        if (action == R.id.action_recherche) {
            ouvrirRecherche();
        } else if (action == R.id.action_plan) {
            basculerPlan();
        } else if (action == R.id.action_debogage) {
            basculerDebogage();
        } else if (action == R.id.action_annuler) {
            rejouer(false);
        } else if (action == R.id.action_retablir) {
            rejouer(true);
        } else if (action == R.id.action_theme) {
            tournerTheme();
        } else if (action == R.id.action_diminuer) {
            zoomerPolice(-PAS_TAILLE);
        } else if (action == R.id.action_agrandir) {
            zoomerPolice(PAS_TAILLE);
        } else if (action == R.id.action_reglages) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else {
            return false;
        }
        return true;
    }

    private void brancherScene() {
        editeur = findViewById(R.id.editeur);
        // Le contenu du fichier ouvert doit vivre dans l'éditeur.
        editeur.setSession(session);
        // Toute animation de vue (défilement, clic) rafraîchit la
        // position ; les clics restent portés par EditorView, qui
        // redéfinit performClick.
        editeur.setOnTouchListener(suivreCurseur());
        editeur.setSurPlan(entrees -> editeur.post(() -> plan.setEntries(entrees)));
        // Le terrain d'essai du débogueur : le clic dans le champ des
        // numéros pose et retire les points d'arrêt (D-04).
        editeur.marquesGouttiere().setSurClicLigne(this::basculerArret);

        plan = findViewById(R.id.plan);
        plan.setBackgroundColor(Color.argb(235, 32, 33, 36));
        plan.getLayoutParams().width = Math.round(largeurEcran() * PART_PLAN);
        plan.setSurClic(this::sautVersEntree);

        etiquettePosition = findViewById(R.id.etat_position);
        etiquetteLangue = findViewById(R.id.etat_langue);
        etiquetteArrets = findViewById(R.id.etat_arrets);
    }

    @SuppressLint("ClickableViewAccessibility")
    private View.OnTouchListener suivreCurseur() {
        return (vue, evenement) -> {
            vue.post(this::majPosition);
            return false;
        };
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

    private void ouvrirRecherche() {
        barreRechercheVue.setVisibility(View.VISIBLE);
        List<SelRange> plages = session.selection().ranges();
        recherche.setCurrentNear(plages.isEmpty() ? 0 : plages.get(0).caret());
        champRecherche.requestFocus();
        majCompteurRecherche();
    }

    private void fermerRecherche() {
        barreRechercheVue.setVisibility(View.GONE);
        editeur.requestFocus();
    }

    private void sauterRecherche(boolean enAvant) {
        boolean trouve = enAvant ? recherche.next() : recherche.previous();
        SelRange courant = recherche.current();
        if (trouve && courant != null) {
            NavigationCommands.selectRange(editeur, courant.start(), courant.end());
        }
        majCompteurRecherche();
    }

    private void majCompteurRecherche() {
        int nombre = recherche.matches().size();
        if (nombre == 0) {
            compteurRecherche.setText(R.string.aucun_resultat);
            return;
        }
        int index = Math.max(recherche.currentIndex(), 0);
        compteurRecherche.setText(getString(R.string.compteur_recherche, index + 1, nombre));
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
