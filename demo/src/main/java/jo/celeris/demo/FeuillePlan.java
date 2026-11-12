package jo.celeris.demo;

import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jo.celeris.android.widgets.FeuilleBas;
import jo.celeris.android.widgets.OutlineView;
import jo.celeris.android.widgets.PopupPalette;
import jo.celeris.core.syntax.structure.OutlineEntry;


/**
 * La feuille du plan de la démonstration (ADR-018, chantier C12) : le
 * panneau latéral plein cadre migre en feuille du bas — « plan » vit
 * en feuille du bas dit la décision. Le fichier s'y parcourt, l'entrée
 * choisie y saute et la feuille se referme ; les entrées se
 * rafraîchissent à mesure que l'analyse les publie.
 */
final class FeuillePlan {

    private final AppCompatActivity activite;
    private final Supplier<PopupPalette> palettes;
    private final Consumer<OutlineEntry> surSaut;
    private final FeuilleBas feuille;
    private final OutlineView vue;

    /** Les entrées courantes du plan — rafraîchies par l'analyse. */
    private List<OutlineEntry> entrees = List.of();

    /**
     * Construit la feuille du plan — prête à s'ouvrir.
     *
     * @param activite l'activité hôte de la feuille
     * @param palettes la palette courante, dérivée à chaque ouverture
     * @param surSaut  le saut vers une entrée choisie
     */
    FeuillePlan(AppCompatActivity activite,
            Supplier<PopupPalette> palettes,
            Consumer<OutlineEntry> surSaut) {
        this.activite = activite;
        this.palettes = palettes;
        this.surSaut = surSaut;
        feuille = new FeuilleBas(activite, palettes.get());
        feuille.titre(activite.getString(R.string.titre_feuille_plan));
        feuille.libelleFermeture(
                activite.getString(R.string.action_fermer));
        vue = new OutlineView(activite, null);
        vue.setSurClic(entree -> {
            surSaut.accept(entree);
            feuille.cacher();
        });
        feuille.contenu(vue);
    }

    /**
     * Les entrées du plan se rafraîchissent — la vue ouverte suit, la
     * feuille fermée les garde pour sa prochaine ouverture.
     *
     * @param nouvelles les entrées publiées par l'analyse
     */
    void setEntrees(List<OutlineEntry> nouvelles) {
        entrees = List.copyOf(nouvelles);
        if (feuille.estOuverte()) {
            vue.setEntries(entrees);
        }
    }

    /** Bascule la feuille : fermée elle s'ouvre, ouverte elle se referme. */
    void basculer() {
        if (feuille.estOuverte()) {
            feuille.cacher();
            return;
        }
        vue.setEntries(entrees);
        feuille.setPalette(palettes.get());
        feuille.montrer(hote());
    }

    /** La touche retour rencontre la feuille — ouverte, elle se referme. */
    boolean surRetour() {
        return feuille.surRetour();
    }

    /** Le cadre de contenu de la fenêtre — toujours un FrameLayout. */
    private ViewGroup hote() {
        return (ViewGroup) activite.findViewById(android.R.id.content);
    }
}
