package jo.celeris.demo;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.Map;
import java.util.StringJoiner;

import org.jspecify.annotations.Nullable;

import jo.celeris.android.widgets.FeuilleBas;

/**
 * Écran d'accueil de la démonstration : la carte de marque porte les
 * deux compteurs réels du dépôt (fichiers d'exemple, scénarios du
 * catalogue), puis chaque fichier devient une carte qui ouvre l'écran
 * d'éditeur complet. Les surfaces fonctionnelles vivent en popups
 * (ADR-018) : les verdicts du banc et les réglages en feuilles du
 * bas. Libellés en ressources {@code en}/{@code fr} complètes
 * (ADR-008).
 */
public final class MainActivity extends AppCompatActivity {

    /** L'adaptateur des cartes, gardé pour les resoumissions (H-04). */
    private AdaptateurFichiers adaptateur;

    /** La feuille des réglages (ADR-018, C12). */
    private FeuilleReglages feuilleReglages;

    /** La feuille des verdicts du banc — née à son premier appel. */
    private @Nullable FeuilleBas feuilleVerdicts;

    /** Les lignes de verdicts, gardées pour les réouvertures. */
    private @Nullable TextView lignesVerdicts;

    @Override
    protected void onCreate(Bundle etat) {
        super.onCreate(etat);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        barre().inflateMenu(R.menu.menu_accueil);
        barre().setOnMenuItemClickListener(element ->
                element.getItemId() == R.id.action_reglages && ouvrirReglages());

        AdaptateurFichiers adaptateur = new AdaptateurFichiers(this::ouvrirFichier);
        liste().setLayoutManager(new LinearLayoutManager(this));
        liste().setAdapter(adaptateur);
        // Les cartes doivent vivre dans l'adaptateur : une liste vide
        // masquerait tout le catalogue — rien à ouvrir, rien à tester.
        // L'assemblage du registre se paie hors fil UI (ADR-007, M7.6) :
        // les cartes se soumettent dès qu'il vit.
        LangagesDemo.dossierPaquets(dossierPaquets());
        LangagesDemo.precharger();
        this.adaptateur = adaptateur;
        soumettreCartes();

        // La feuille des réglages (ADR-018) : le paquet jouet (H-04)
        // s'y installe, les cartes se resoumettent à sa fermeture.
        feuilleReglages = new FeuilleReglages(this, () -> PalettesDemo.de(this),
                this::soumettreCartes);

        poserStatistiques();
        appliquerBordABord();
        brancherRetour();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Le paquet jouet (H-04) s'installe dans les réglages : au retour,
        // les cartes se resoumettent — la carte du langage jouet y entre
        // et en sort à chaud.
        if (adaptateur != null) {
            soumettreCartes();
        }
    }

    /** Le dossier des paquets de l'utilisateur (H-04). */
    private java.nio.file.Path dossierPaquets() {
        return getFilesDir().toPath().resolve(PaquetJouet.DOSSIER);
    }

    /** Soumet les cartes du catalogue au registre assemblé (H-04 compris). */
    private void soumettreCartes() {
        AdaptateurFichiers adaptateur = this.adaptateur;
        new Thread(() -> {
            LangagesDemo.registre();
            boolean jouet = PaquetJouet.installe(dossierPaquets());
            runOnUiThread(() -> {
                adaptateur.submitList(CarteFichier.catalogue(jouet));
                TextView fichiers = findViewById(R.id.stat_fichiers_valeur);
                fichiers.setText(String.valueOf(
                        FichiersExemple.catalogue(jouet).size()));
            });
        }, "celeris-cartes").start();
    }

    private MaterialToolbar barre() {
        return findViewById(R.id.barre_accueil);
    }

    private RecyclerView liste() {
        return findViewById(R.id.liste_fichiers);
    }

    /** Les compteurs réels : fichiers du catalogue, scénarios connus,
     *  vérifications du banc (M7.5, §9) — l'appui liste les verdicts. */
    private void poserStatistiques() {
        TextView scenarios = findViewById(R.id.stat_scenarios_valeur);
        scenarios.setText(String.valueOf(ScenarioCatalog.scenarios().size()));
        Map<String, Boolean> verdicts = BancSpecs.verdicts();
        int tenues = 0;
        for (Boolean verdict : verdicts.values()) {
            if (verdict) {
                tenues++;
            }
        }
        TextView specs = findViewById(R.id.stat_specs_valeur);
        specs.setText(getString(R.string.specs_compteur, tenues,
                verdicts.size()));
        findViewById(R.id.stat_specs).setOnClickListener(
                vue -> montrerVerdicts(verdicts));
    }

    /**
     * Les verdicts du banc, ligne à ligne — la démo qui démontre se
     * lit, en feuille du bas (ADR-018) : plus aucun dialogue système.
     */
    private void montrerVerdicts(Map<String, Boolean> verdicts) {
        FeuilleBas feuille = feuilleVerdicts;
        if (feuille == null) {
            feuille = new FeuilleBas(this, PalettesDemo.de(this));
            feuille.titre(getString(R.string.specs_titre));
            feuille.libelleFermeture(getString(R.string.action_fermer));
            lignesVerdicts = new TextView(this);
            lignesVerdicts.setTypeface(Typeface.MONOSPACE);
            feuille.contenu(lignesVerdicts);
            feuilleVerdicts = feuille;
        }
        StringJoiner lignes = new StringJoiner("\n");
        for (Map.Entry<String, Boolean> verdict : verdicts.entrySet()) {
            lignes.add(getString(verdict.getValue()
                    ? R.string.spec_verdict_tenu : R.string.spec_verdict_tombe,
                    verdict.getKey()));
        }
        lignesVerdicts.setText(lignes.toString());
        feuille.setPalette(PalettesDemo.de(this));
        feuille.montrer(hote());
    }

    private boolean ouvrirReglages() {
        feuilleReglages.montrer();
        return true;
    }

    private void ouvrirFichier(CarteFichier carte) {
        startActivity(new android.content.Intent(this, EditorActivity.class)
                .putExtra(EditorActivity.EXTRA_FICHIER, carte.nom()));
    }

    /** Le cadre de contenu de la fenêtre — toujours un FrameLayout. */
    private ViewGroup hote() {
        return (ViewGroup) findViewById(android.R.id.content);
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
        if (feuilleReglages.surRetour()) {
            return true;
        }
        FeuilleBas feuille = feuilleVerdicts;
        return feuille != null && feuille.surRetour();
    }

    /** Le chrome s'étend bord à bord : les encarts deviennent marges. */
    private void appliquerBordABord() {
        View racine = findViewById(R.id.racine_accueil);
        ViewCompat.setOnApplyWindowInsetsListener(racine, (vue, encarts) -> {
            Insets barres = encarts.getInsets(WindowInsetsCompat.Type.systemBars());
            vue.setPadding(0, barres.top, 0, barres.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
