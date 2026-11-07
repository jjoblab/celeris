package jo.celeris.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Map;
import java.util.StringJoiner;

/**
 * Écran d'accueil de la démonstration : la carte de marque porte les
 * deux compteurs réels du dépôt (fichiers d'exemple, scénarios du
 * catalogue), puis chaque fichier devient une carte qui ouvre l'écran
 * d'éditeur complet. Libellés en ressources {@code en}/{@code fr}
 * complètes (ADR-008).
 */
public final class MainActivity extends AppCompatActivity {

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
        adaptateur.submitList(CarteFichier.catalogue());

        poserStatistiques();
        appliquerBordABord();
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
        TextView fichiers = findViewById(R.id.stat_fichiers_valeur);
        fichiers.setText(String.valueOf(FichiersExemple.catalogue().size()));
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

    /** Les verdicts du banc, ligne à ligne — la démo qui démontre se lit. */
    private void montrerVerdicts(Map<String, Boolean> verdicts) {
        StringJoiner lignes = new StringJoiner("\n");
        for (Map.Entry<String, Boolean> verdict : verdicts.entrySet()) {
            lignes.add(getString(verdict.getValue()
                    ? R.string.spec_verdict_tenu : R.string.spec_verdict_tombe,
                    verdict.getKey()));
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.specs_titre)
                .setMessage(lignes.toString())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private boolean ouvrirReglages() {
        startActivity(new Intent(this, SettingsActivity.class));
        return true;
    }

    private void ouvrirFichier(CarteFichier carte) {
        startActivity(new Intent(this, EditorActivity.class)
                .putExtra(EditorActivity.EXTRA_FICHIER, carte.nom()));
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
