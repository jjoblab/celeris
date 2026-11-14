package jo.celeris.demo;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jo.celeris.android.widgets.FeuilleBas;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.core.syntax.structure.OutlineEntry;

/**
 * La feuille de l'aller à de la démonstration (catégorie C de
 * l'inventaire — « Go to File / Class / Symbol », manquant) : le
 * modèle du plan (FeuillePlan) y sert — une feuille du bas (ADR-018,
 * la surface DENSE vit en feuille), le champ tape quelques lettres,
 * la recherche floue (B-13) répond — les résultats se rendent en
 * rangees cliquables, le choix SAUTE et la feuille se referme. Le
 * champ tient le focus (PIEGE-27 ne concerne pas la feuille : elle
 * vit hors de la couche, c'est une surface modale).
 *
 * <p>La frontière tient : la feuille ne connaît NI le catalogue, NI le
 * plan — le modèle pur ({@link ResultatsAller}) décide, l'activité
 * fournit les sources et exécute le saut.</p>
 */
final class FeuilleAller {

    /** Le nombre maximal de résultats montrés — la feuille reste
     *  lisible, jamais une liste qui mange l'écran. */
    private static final int PLAFOND = 8;

    private final AppCompatActivity activite;
    private final Supplier<PopupPalette> palettes;
    private final Consumer<ResultatsAller.Resultat> surChoix;
    private final Supplier<List<OutlineEntry>> plan;
    private final Supplier<List<String>> fichiers;
    private final FeuilleBas feuille;
    private final EditText champ;
    private final LinearLayout rangees;
    /** Le genre localisé des résultats — les clés du modèle pur. */
    private final String libelleFichier;
    private final String libelleSymbole;
    private final String aucunResultat;

    /**
     * Construit la feuille de l'aller à — prête à s'ouvrir.
     *
     * @param activite l'activité hôte de la feuille
     * @param palettes la palette courante, dérivée à chaque ouverture
     * @param surChoix le saut du résultat choisi (l'activité l'exécute)
     * @param plan     le plan courant du fichier ouvert
     * @param fichiers les noms des fichiers du catalogue
     */
    FeuilleAller(AppCompatActivity activite, Supplier<PopupPalette> palettes,
            Consumer<ResultatsAller.Resultat> surChoix,
            Supplier<List<OutlineEntry>> plan,
            Supplier<List<String>> fichiers) {
        this.activite = activite;
        this.palettes = palettes;
        this.surChoix = surChoix;
        this.plan = plan;
        this.fichiers = fichiers;
        feuille = new FeuilleBas(activite, palettes.get());
        feuille.titre(activite.getString(R.string.titre_feuille_aller));
        feuille.libelleFermeture(activite.getString(R.string.action_fermer));
        View contenu = activite.getLayoutInflater()
                .inflate(R.layout.feuille_aller, null);
        feuille.contenu(contenu);
        champ = contenu.findViewById(R.id.champ_aller);
        rangees = contenu.findViewById(R.id.rangees_aller);
        libelleFichier = activite.getString(R.string.genre_fichier_aller);
        libelleSymbole = activite.getString(R.string.genre_symbole_aller);
        aucunResultat = activite.getString(R.string.aucun_resultat);
        champ.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int d, int n, int a) {
                // seul l'après intéresse la feuille
            }

            @Override
            public void onTextChanged(CharSequence s, int d, int n, int a) {
                // seul l'après intéresse la feuille
            }

            @Override
            public void afterTextChanged(Editable s) {
                majResultats(s.toString());
            }
        });
    }

    /** Bascule la feuille : fermée elle s'ouvre (le champ prend le
     *  focus, le catalogue s'y sert en raccourci), ouverte elle se
     *  referme. */
    void basculer() {
        if (feuille.estOuverte()) {
            feuille.cacher();
            return;
        }
        feuille.setPalette(palettes.get());
        majResultats(champ.getText().toString());
        feuille.montrer(hote());
        champ.requestFocus();
    }

    /** La touche retour rencontre la feuille — ouverte, elle se referme. */
    boolean surRetour() {
        return feuille.surRetour();
    }

    /** Re-bâtir les rangees des résultats — la requête muette sert le
     *  catalogue, la floue répond par score. */
    private void majResultats(String requete) {
        rangees.removeAllViews();
        List<ResultatsAller.Resultat> resultats = ResultatsAller.recherche(
                requete, fichiers.get(), plan.get(), PLAFOND);
        if (resultats.isEmpty()) {
            TextView vide = new TextView(activite);
            vide.setText(aucunResultat);
            vide.setPaddingRelative(16, 12, 16, 12);
            vide.setTextSize(13f);
            rangees.addView(vide);
            return;
        }
        for (ResultatsAller.Resultat resultat : resultats) {
            rangees.addView(rangee(resultat));
        }
    }

    /** La rangee cliquable d'un résultat : le genre localisé en
     *  préfixe, le libellé au corps, le détail en encre douce. */
    private TextView rangee(ResultatsAller.Resultat resultat) {
        TextView rangee = new TextView(activite);
        String genre = resultat.genre().equals(ResultatsAller.GENRE_FICHIER)
                ? libelleFichier : libelleSymbole;
        rangee.setText(genre + " · " + resultat.libelle()
                + (resultat.detail().isEmpty() ? "" : " · " + resultat.detail()));
        rangee.setPaddingRelative(16, 14, 16, 14);
        rangee.setTextSize(15f);
        rangee.setOnClickListener(vue -> {
            feuille.cacher();
            surChoix.accept(resultat);
        });
        return rangee;
    }

    /** Le cadre de contenu de la fenêtre — toujours un FrameLayout. */
    private ViewGroup hote() {
        return (ViewGroup) activite.findViewById(android.R.id.content);
    }
}
