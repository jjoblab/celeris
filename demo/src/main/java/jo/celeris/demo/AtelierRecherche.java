package jo.celeris.demo;

import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import java.util.List;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.view.NavigationCommands;
import jo.celeris.android.widgets.SearchBarModel;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

/**
 * L'atelier de recherche de la démonstration (A-20, facette barre) :
 * la barre dépliable porte le champ, le compteur et les sauts — le
 * moteur vit dans {@link SearchBarModel}. L'état d'ouverture se
 * coordonne chez l'atelier des popups (M7.5, §7) : la barre ne se
 * ferme que sur demande — les rappels d'ouverture et de fermeture y
 * courent, le champ garde son focus (PIEGE-27 : la couche
 * l'interdit, la barre reste l'affaire de l'activité).
 */
final class AtelierRecherche {

    private final AppCompatActivity activite;
    private final EditorView editeur;
    private final TextSession session;
    private final SearchBarModel recherche;
    private final Runnable surOuverture;
    private final Runnable surFermeture;

    private LinearLayout barreVue;
    private EditText champ;
    private TextView compteur;

    /**
     * Construit l'atelier de recherche.
     *
     * @param activite     l'activité hôte
     * @param editeur      la vue d'éditeur ouverte
     * @param session      la session d'édition courante
     * @param surOuverture le rappel d'ouverture (état des popups)
     * @param surFermeture le rappel de fermeture (état des popups)
     */
    AtelierRecherche(AppCompatActivity activite, EditorView editeur,
            TextSession session, Runnable surOuverture, Runnable surFermeture) {
        this.activite = activite;
        this.editeur = editeur;
        this.session = session;
        this.recherche = new SearchBarModel(session.document(),
                System::nanoTime);
        this.surOuverture = surOuverture;
        this.surFermeture = surFermeture;
    }

    /** Branche la barre dépliable : champ, compteur, sauts, fermeture. */
    void brancher() {
        barreVue = activite.findViewById(R.id.barre_recherche);
        champ = activite.findViewById(R.id.champ_recherche);
        compteur = activite.findViewById(R.id.compteur_recherche);
        champ.addTextChangedListener(new TextWatcher() {
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
                majCompteur();
            }
        });
        bouton(R.id.bouton_precedent, vue -> sauter(false));
        bouton(R.id.bouton_suivant, vue -> sauter(true));
        bouton(R.id.bouton_fermer_recherche, vue -> fermer());
    }

    /** Ouvre la barre près de la position courante — le champ prend le
     *  focus, l'état des popups suit. */
    void ouvrir() {
        barreVue.setVisibility(View.VISIBLE);
        List<SelRange> plages = session.selection().ranges();
        recherche.setCurrentNear(plages.isEmpty() ? 0 : plages.get(0).caret());
        champ.requestFocus();
        majCompteur();
        surOuverture.run();
    }

    /** Ferme la barre — sur demande seule (M7.5, §7), l'état suit. */
    void fermer() {
        barreVue.setVisibility(View.GONE);
        editeur.requestFocus();
        surFermeture.run();
    }

    private void sauter(boolean enAvant) {
        boolean trouve = enAvant ? recherche.next() : recherche.previous();
        SelRange courant = recherche.current();
        if (trouve && courant != null) {
            NavigationCommands.selectRange(editeur, courant.start(),
                    courant.end());
        }
        majCompteur();
    }

    private void majCompteur() {
        int nombre = recherche.matches().size();
        if (nombre == 0) {
            compteur.setText(R.string.aucun_resultat);
            return;
        }
        int index = Math.max(recherche.currentIndex(), 0);
        compteur.setText(activite.getString(
                R.string.compteur_recherche, index + 1, nombre));
    }

    private void bouton(int identifiant, View.OnClickListener ecouteur) {
        ImageButton bouton = activite.findViewById(identifiant);
        bouton.setOnClickListener(ecouteur);
    }
}
