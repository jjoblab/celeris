package jo.celeris.demo;

import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import java.util.List;
import java.util.function.Supplier;

import jo.celeris.android.view.EditorView;
import jo.celeris.android.view.NavigationCommands;
import jo.celeris.android.widgets.FeuilleBas;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.android.widgets.SearchBarModel;
import jo.celeris.core.text.SelRange;
import jo.celeris.core.text.TextSession;

/**
 * L'atelier de recherche de la démonstration (A-20, facette barre) :
 * la barre dépliable migre en feuille du bas (ADR-018, chantier C12) —
 * le champ, le compteur et les sauts y vivent, le moteur reste dans
 * {@link SearchBarModel}. L'état d'ouverture se coordonne toujours chez
 * l'atelier des popups (M7.5, §7) : la feuille ne se ferme que sur
 * demande — bouton, voile, glissement ou retour — et le champ garde
 * son focus (PIEGE-27 ne concerne pas la feuille : elle vit hors de la
 * couche, c'est une surface modale).
 */
final class AtelierRecherche {

    private final AppCompatActivity activite;
    private final EditorView editeur;
    private final TextSession session;
    private final SearchBarModel recherche;
    private final Supplier<PopupPalette> palettes;
    private final Runnable surOuverture;
    private final Runnable surFermeture;

    private FeuilleBas feuille;
    private EditText champ;
    private TextView compteur;

    /**
     * Construit l'atelier de recherche.
     *
     * @param activite     l'activité hôte
     * @param editeur      la vue d'éditeur ouverte
     * @param session      la session d'édition courante
     * @param palettes     la palette courante, dérivée à chaque ouverture
     * @param surOuverture le rappel d'ouverture (état des popups)
     * @param surFermeture le rappel de fermeture (état des popups)
     */
    AtelierRecherche(AppCompatActivity activite, EditorView editeur,
            TextSession session, Supplier<PopupPalette> palettes,
            Runnable surOuverture, Runnable surFermeture) {
        this.activite = activite;
        this.editeur = editeur;
        this.session = session;
        this.palettes = palettes;
        this.recherche = new SearchBarModel(session.document(),
                System::nanoTime);
        this.surOuverture = surOuverture;
        this.surFermeture = surFermeture;
    }

    /** Branche la feuille de recherche : champ, compteur, sauts. */
    void brancher() {
        feuille = new FeuilleBas(activite, palettes.get());
        feuille.titre(activite.getString(R.string.action_recherche));
        feuille.libelleFermeture(activite.getString(R.string.action_fermer));
        View contenu = activite.getLayoutInflater()
                .inflate(R.layout.feuille_recherche, null);
        feuille.contenu(contenu);
        champ = contenu.findViewById(R.id.champ_recherche);
        compteur = contenu.findViewById(R.id.compteur_recherche);
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
                recherche.setQuery(s.toString());
                majCompteur();
            }
        });
        bouton(R.id.bouton_precedent, vue -> sauter(false));
        bouton(R.id.bouton_suivant, vue -> sauter(true));
        bouton(R.id.bouton_fermer_recherche, vue -> fermer());
        // Toute fermeture — bouton, voile, glissement, retour — rend le
        // focus à l'éditeur et referme l'état des popups : la feuille
        // ne se ferme que sur demande (M7.5, §7).
        feuille.setSurFermeture(() -> {
            editeur.requestFocus();
            surFermeture.run();
        });
    }

    /** Ouvre la feuille — le champ prend le focus, l'état des popups suit. */
    void ouvrir() {
        feuille.setPalette(palettes.get());
        List<SelRange> plages = session.selection().ranges();
        recherche.setCurrentNear(plages.isEmpty() ? 0 : plages.get(0).caret());
        feuille.montrer(hote());
        champ.requestFocus();
        majCompteur();
        surOuverture.run();
    }

    /** Ferme la feuille — sur demande seule (M7.5, §7), l'état suit. */
    void fermer() {
        feuille.cacher();
    }

    /** La touche retour rencontre la feuille — ouverte, elle se referme. */
    boolean surRetour() {
        return feuille != null && feuille.surRetour();
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
        ImageButton bouton = feuille.findViewById(identifiant);
        bouton.setOnClickListener(ecouteur);
    }

    /** Le cadre de contenu de la fenêtre — toujours un FrameLayout. */
    private ViewGroup hote() {
        return (ViewGroup) activite.findViewById(android.R.id.content);
    }
}
