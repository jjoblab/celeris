package jo.celeris.demo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import jo.celeris.android.host.spi.Arret;
import jo.celeris.android.host.spi.CadrePile;
import jo.celeris.android.host.spi.CheminDoc;
import jo.celeris.android.host.spi.EtatLignesVcs;
import jo.celeris.android.host.spi.LigneArret;
import jo.celeris.android.host.spi.MarqueVcs;
import jo.celeris.android.view.EtatArret;
import jo.celeris.android.view.MarqueLigne;
import jo.celeris.android.view.MarquesIntegration;

/**
 * L'adaptateur des marques d'intégration de la démonstration (D-03,
 * D-04, D-05) : l'état des contrats de l'hôte — lignes numérotées à
 * partir de un — devient les marques de la vue — lignes numérotées à
 * partir de zéro. Les arrêts et les cadres de pile se filtrent au
 * fichier ouvert : les autres fichiers ne se peignent pas ici.
 */
final class AdaptateurMarques {

    private AdaptateurMarques() {
    }

    /**
     * Les marques de la vue depuis l'état de l'hôte.
     *
     * @param etat     l'état des lignes VCS du fichier ouvert
     * @param arrets   tous les arrêts posés, tous fichiers confondus
     * @param fichier  le chemin du fichier ouvert
     * @param pause    le lieu de la pause, null hors pause
     * @param pile     la pile courante, du plus récent au plus ancien
     * @return les marques pour la vue
     */
    static MarquesIntegration depuis(EtatLignesVcs etat, List<Arret> arrets,
            CheminDoc fichier, @Nullable LigneArret pause, List<CadrePile> pile) {
        Map<Integer, MarqueLigne> marques = new HashMap<>();
        etat.marques().forEach((ligne, marque) ->
                marques.put(ligne - 1, traduire(marque)));
        Map<Integer, EtatArret> etatsArrets = new HashMap<>();
        for (Arret arret : arrets) {
            if (arret.fichier().equals(fichier)) {
                etatsArrets.put(arret.ligne() - 1,
                        arret.actif() ? EtatArret.POSE : EtatArret.DESACTIVE);
            }
        }
        Integer ligneCourante = pause != null && pause.fichier().equals(fichier)
                ? pause.ligne() - 1 : null;
        Set<Integer> lignesPile = new HashSet<>();
        for (CadrePile cadre : pile) {
            if (cadre.ligne() > 0 && cadre.fichier().equals(fichier.nom())) {
                lignesPile.add(cadre.ligne() - 1);
            }
        }
        return MarquesIntegration.of(marques, etatsArrets, ligneCourante, lignesPile);
    }

    /** La marque de la vue pour la marque de l'hôte. */
    private static MarqueLigne traduire(MarqueVcs marque) {
        return switch (marque) {
            case MODIFIE -> MarqueLigne.MODIFIE;
            case AJOUTE -> MarqueLigne.AJOUTE;
            case INCHANGE -> MarqueLigne.INCHANGE;
        };
    }
}
