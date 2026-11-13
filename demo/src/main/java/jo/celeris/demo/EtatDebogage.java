package jo.celeris.demo;

import android.view.View;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jo.celeris.android.host.spi.Arret;
import jo.celeris.android.host.spi.CadrePile;
import jo.celeris.android.host.spi.CheminDoc;
import jo.celeris.android.host.spi.FauxDebogueur;
import jo.celeris.android.host.spi.ValeurEnLigne;
import jo.celeris.android.view.EditorView;
import jo.celeris.core.text.TextSession;

/**
 * L'état de débogage de la démonstration (D-04, D-05) : les points
 * d'arrêt posés au clic de gouttière, la pause simulée au premier
 * arrêt actif — à la cinquième ligne sinon — avec sa pile de deux
 * cadres, et l'étiquette de barre qui publie pause ou arrêts.
 */
final class EtatDebogage {

    private final EditorActivity activite;
    private final EditorView editeur;
    private final TextSession session;
    private final TextView etiquette;
    private final CheminDoc chemin;
    private final FauxDebogueur debogueur = new FauxDebogueur();

    EtatDebogage(EditorActivity activite, EditorView editeur,
            TextSession session, TextView etiquette, CheminDoc chemin) {
        this.activite = activite;
        this.editeur = editeur;
        this.session = session;
        this.etiquette = etiquette;
        this.chemin = chemin;
    }

    /** Pose ou retire le point d'arrêt de la ligne cliquée (D-04). */
    void basculerArret(int ligne) {
        boolean absent = debogueur.arrets().stream()
                .noneMatch(arret -> arret.fichier().equals(chemin)
                        && arret.ligne() == ligne + 1);
        if (absent) {
            debogueur.poserArret(chemin, ligne + 1);
        } else {
            debogueur.retirerArret(chemin, ligne + 1);
        }
        majEtat();
    }

    /**
     * Bascule la pause simulée (D-05) : sans pause, l'exécution s'arrête
     * au premier arrêt actif du fichier — à la cinquième ligne sinon —
     * avec une pile de deux cadres ; en pause, l'exécution reprend. La
     * pause fraîche y sert ses valeurs en ligne : les affectations
     * littérales de SA ligne se lisent (E-03) — déterministe, jamais
     * deviné.
     */
    void basculerPause() {
        if (debogueur.ligneCourante() != null) {
            debogueur.simulerReprise();
        } else {
            Arret premier = debogueur.arrets().stream()
                    .filter(arret -> arret.fichier().equals(chemin)
                            && arret.actif())
                    .findFirst().orElse(null);
            int ligne = premier != null ? premier.ligne()
                    : Math.min(5, session.document().lineCount());
            debogueur.simulerPause(chemin, ligne, List.of(
                    new CadrePile("Exemple", "corps", chemin.nom(), ligne),
                    new CadrePile("Exemple", "appelant", chemin.nom(),
                            Math.max(1, ligne - 2))));
            for (ValeurEnLigne valeur : valeursDePause(ligne,
                    session.document().lineText(ligne - 1))) {
                debogueur.enregistrerValeurEnLigne(valeur.ligne(),
                        valeur.expression(), valeur.valeur());
            }
        }
        majEtat();
    }

    /** Les valeurs de pause de la ligne : les affectations écrites
     *  « ident = valeur » s'y lisent — déterministe, jamais deviné
     *  (la pause ne devine pas ce que la ligne ne dit pas). */
    static List<ValeurEnLigne> valeursDePause(int ligne, String texte) {
        Pattern affectation = Pattern.compile(
                "([\\w$]+)\\s*=\\s*([\\w.\\-\"]+)");
        Matcher corps = affectation.matcher(texte);
        java.util.ArrayList<ValeurEnLigne> valeurs =
                new java.util.ArrayList<>();
        while (corps.find() && valeurs.size() < 3) {
            valeurs.add(new ValeurEnLigne(ligne, corps.group(1),
                    corps.group(2)));
        }
        return List.copyOf(valeurs);
    }

    /** Le débogueur simulé — les marques d'intégration y lisent. */
    FauxDebogueur debogueur() {
        return debogueur;
    }

    /** L'étiquette et le menu suivent la pause : pause, reprise, arrêts. */
    void majEtat() {
        jo.celeris.android.host.spi.LigneArret pause =
                debogueur.ligneCourante();
        if (pause != null) {
            etiquette.setText(activite.getString(
                    R.string.etat_pause, pause.ligne()));
            etiquette.setVisibility(View.VISIBLE);
        } else {
            long nombre = debogueur.arrets().stream()
                    .filter(arret -> arret.fichier().equals(chemin)).count();
            etiquette.setText(activite.getResources().getQuantityString(
                    R.plurals.etat_arrets, (int) nombre, (int) nombre));
            etiquette.setVisibility(nombre == 0 ? View.GONE : View.VISIBLE);
        }
        MaterialToolbar barre = activite.findViewById(R.id.barre_editeur);
        barre.getMenu().findItem(R.id.action_debogage)
                .setIcon(pause == null ? R.drawable.ic_debogage
                        : R.drawable.ic_reprendre)
                .setTitle(pause == null ? R.string.action_deboguer
                        : R.string.action_reprendre);
    }
}
