package jo.celeris.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jo.celeris.android.host.spi.EtatLignesVcs;
import jo.celeris.android.host.spi.MarqueVcs;
import jo.celeris.android.host.spi.PlageLignes;

/**
 * Le diff ligne à ligne de la démonstration (D-03) : l'état des lignes
 * du texte courant face au texte de référence — le contenu d'origine du
 * fichier d'exemple ouvert, la « révision de référence » du terrain
 * d'essai. Préfixe et suffixe communs exclus, le milieu s'aligne ligne
 * à ligne : modifiées à l'alignement, ajoutées au-delà, supprimées par
 * plage.
 *
 * <p>L'alignement naïf par préfixe et suffixe suffit au terrain
 * d'essai : les éditions contiguës se marquent juste, sans prétendre à
 * la plus petite différence — le contrat E-02 reste servi par l'hôte,
 * ici seules les marques s'en nourrissent.</p>
 */
final class DiffLignes {

    private DiffLignes() {
    }

    /**
     * L'état des lignes du courant face à la référence.
     *
     * @param reference le texte de référence
     * @param courant   le texte courant
     * @return l'état des lignes, marques et plages supprimées
     */
    static EtatLignesVcs etat(String reference, String courant) {
        String[] anciennes = reference.split("\n", -1);
        String[] nouvelles = courant.split("\n", -1);
        int prefixe = 0;
        while (prefixe < anciennes.length && prefixe < nouvelles.length
                && anciennes[prefixe].equals(nouvelles[prefixe])) {
            prefixe++;
        }
        int suffixe = 0;
        while (suffixe < anciennes.length - prefixe
                && suffixe < nouvelles.length - prefixe
                && anciennes[anciennes.length - 1 - suffixe]
                        .equals(nouvelles[nouvelles.length - 1 - suffixe])) {
            suffixe++;
        }
        Map<Integer, MarqueVcs> marques = new HashMap<>();
        int milieuReference = anciennes.length - prefixe - suffixe;
        int milieuCourant = nouvelles.length - prefixe - suffixe;
        int alignees = Math.min(milieuReference, milieuCourant);
        for (int i = 0; i < milieuCourant; i++) {
            int ligne = prefixe + i + 1;
            marques.put(ligne, i < alignees ? MarqueVcs.MODIFIE : MarqueVcs.AJOUTE);
        }
        List<PlageLignes> supprimees = new ArrayList<>();
        if (milieuReference > milieuCourant) {
            supprimees.add(new PlageLignes(prefixe + milieuCourant + 1,
                    prefixe + milieuReference));
        }
        return EtatLignesVcs.de(marques, supprimees);
    }
}
