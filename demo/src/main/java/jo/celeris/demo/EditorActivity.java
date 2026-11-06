package jo.celeris.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import jo.celeris.android.host.spi.Arret;
import jo.celeris.android.host.spi.CadrePile;
import jo.celeris.android.host.spi.CheminDoc;
import jo.celeris.android.host.spi.EtatLignesVcs;
import jo.celeris.android.host.spi.FauxDebogueur;
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
 * L'écran d'éditeur de la démonstration : un fichier d'exemple ouvert
 * dans la vue Canvas complète — coloration, plis, recherche, plan,
 * thèmes, zoom et annulation. Les réglages persistés (numéros, retour
 * souple, thème, taille de police) se réappliquent au retour de
 * l'écran de réglages en direct.
 */
public final class EditorActivity extends Activity {

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
    private SharedPreferences prefs;
    private String nomFichier;
    private String reference;
    private CheminDoc chemin;
    private final FauxDebogueur debogueur = new FauxDebogueur();
    private Button boutonDebogage;

    @Override
    protected void onCreate(Bundle etat) {
        super.onCreate(etat);
        prefs = getSharedPreferences("celeris", Context.MODE_PRIVATE);
        nomFichier = getIntent().getStringExtra(EXTRA_FICHIER);
        FichierExemple fichier = FichiersExemple.parNom(
                nomFichier == null ? FichiersExemple.premier().nom() : nomFichier);
        if (fichier == null) {
            fichier = FichiersExemple.premier();
        }
        nomFichier = fichier.nom();
        reference = fichier.contenu();
        chemin = CheminDoc.de("/" + nomFichier);
        session = TextSession.of(fichier.contenu(), System::nanoTime);
        recherche = new SearchBarModel(session.document(), System::nanoTime);
        session.addEditListener((debut, fin, retire, insere) -> {
            majPosition();
            majMarques();
        });
        setContentView(construireInterface());
        appliquerPreferences();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Les réglages ont pu changer pendant la pause : réappliquer.
        appliquerPreferences();
    }

    private ViewGroup construireInterface() {
        LinearLayout racine = new LinearLayout(this);
        racine.setOrientation(LinearLayout.VERTICAL);
        float densite = getResources().getDisplayMetrics().density;
        int marge = Math.round(4 * densite);
        racine.setPadding(marge, marge, marge, marge);
        racine.addView(barreOutils());
        barreRechercheVue = barreRecherche();
        barreRechercheVue.setVisibility(View.GONE);
        racine.addView(barreRechercheVue);
        racine.addView(sceneEdition(),
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        racine.addView(barreEtat());
        return racine;
    }

    private LinearLayout barreOutils() {
        LinearLayout barre = new LinearLayout(this);
        barre.setOrientation(LinearLayout.HORIZONTAL);
        TextView titre = new TextView(this);
        titre.setText(nomFichier);
        titre.setTextSize(16);
        titre.setGravity(Gravity.CENTER_VERTICAL);
        barre.addView(titre, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        barre.addView(bouton(R.string.action_plan, vue -> basculerPlan()));
        barre.addView(bouton(R.string.action_recherche, vue -> ouvrirRecherche()));
        barre.addView(bouton(R.string.action_reglages,
                vue -> startActivity(new Intent(this, SettingsActivity.class))));
        return barre;
    }

    private LinearLayout barreRecherche() {
        LinearLayout barre = new LinearLayout(this);
        barre.setOrientation(LinearLayout.HORIZONTAL);
        champRecherche = new EditText(this);
        champRecherche.setInputType(InputType.TYPE_CLASS_TEXT);
        champRecherche.setHint(R.string.indication_recherche);
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
        barre.addView(champRecherche, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        compteurRecherche = new TextView(this);
        compteurRecherche.setGravity(Gravity.CENTER_VERTICAL);
        barre.addView(compteurRecherche, new LinearLayout.LayoutParams(
                Math.round(64 * getResources().getDisplayMetrics().density),
                ViewGroup.LayoutParams.MATCH_PARENT));
        barre.addView(bouton(R.string.action_precedent, vue -> sauterRecherche(false)));
        barre.addView(bouton(R.string.action_suivant, vue -> sauterRecherche(true)));
        barre.addView(bouton(R.string.action_fermer, vue -> fermerRecherche()));
        return barre;
    }

    private FrameLayout sceneEdition() {
        FrameLayout scene = new FrameLayout(this);
        editeur = new EditorView(this);
        scene.addView(editeur, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        editeur.setOnTouchListener((vue, evenement) -> {
            vue.post(this::majPosition);
            return false;
        });
        editeur.setSurPlan(entrees -> editeur.post(() -> plan.setEntries(entrees)));
        // Le terrain d'essai du débogueur : le clic dans le champ des
        // numéros pose et retire les points d'arrêt (D-04).
        editeur.marquesGouttiere().setSurClicLigne(this::basculerArret);
        plan = new OutlineView(this, null);
        plan.setBackgroundColor(Color.argb(235, 32, 33, 36));
        plan.setVisibility(View.GONE);
        int largeurPlan = Math.round(getWidthEcran() * PART_PLAN);
        FrameLayout.LayoutParams parametresPlan = new FrameLayout.LayoutParams(
                largeurPlan, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END);
        parametresPlan.setMargins(0, 0, 0, 0);
        scene.addView(plan, parametresPlan);
        plan.setSurClic(this::sautVersEntree);
        return scene;
    }

    private LinearLayout barreEtat() {
        LinearLayout barre = new LinearLayout(this);
        barre.setOrientation(LinearLayout.HORIZONTAL);
        etiquettePosition = new TextView(this);
        etiquettePosition.setGravity(Gravity.CENTER_VERTICAL);
        barre.addView(etiquettePosition, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        etiquetteLangue = new TextView(this);
        etiquetteLangue.setGravity(Gravity.CENTER_VERTICAL);
        barre.addView(etiquetteLangue);
        boutonDebogage = bouton(R.string.action_deboguer, vue -> basculerDebogage());
        barre.addView(boutonDebogage);
        barre.addView(bouton(R.string.action_diminuer, vue -> zoomerPolice(-PAS_TAILLE)));
        barre.addView(bouton(R.string.action_agrandir, vue -> zoomerPolice(PAS_TAILLE)));
        barre.addView(bouton(R.string.action_annuler, vue -> rejouer(false)));
        barre.addView(bouton(R.string.action_retablir, vue -> rejouer(true)));
        barre.addView(bouton(R.string.action_theme, vue -> tournerTheme()));
        return barre;
    }

    private Button bouton(int libelle, View.OnClickListener ecouteur) {
        Button bouton = new Button(this);
        bouton.setText(libelle);
        bouton.setOnClickListener(ecouteur);
        bouton.setPadding(0, 0, 0, 0);
        bouton.setMinWidth(Math.round(44 * getResources().getDisplayMetrics().density));
        return bouton;
    }

    private void appliquerPreferences() {
        int indexTheme = Math.min(Math.max(prefs.getInt(CLE_THEME, 0), 0), THEMES.length - 1);
        editeur.setTheme(THEMES[indexTheme]);
        editeur.setNumerosVisibles(prefs.getBoolean(CLE_NUMEROS, true));
        editeur.setSoftWrap(prefs.getBoolean(CLE_RETOUR, false));
        editeur.setFont(new FontSpec("mono",
                prefs.getFloat(CLE_TAILLE, TAILLE_DEFAUT)));
        LanguageSyntax langue = FichiersExemple.languePour(nomFichier);
        editeur.setLanguage(langue);
        etiquetteLangue.setText(langue == null ? nomFichier : langue.id());
        majPosition();
    }

    private void tournerTheme() {
        int courant = Math.min(Math.max(prefs.getInt(CLE_THEME, 0), 0), THEMES.length - 1);
        int suivant = (courant + 1) % THEMES.length;
        prefs.edit().putInt(CLE_THEME, suivant).apply();
        editeur.setTheme(THEMES[suivant]);
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
        boutonDebogage.setText(debogueur.ligneCourante() != null
                ? R.string.action_reprendre : R.string.action_deboguer);
        majMarques();
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

    private int getWidthEcran() {
        return getResources().getDisplayMetrics().widthPixels;
    }
}
