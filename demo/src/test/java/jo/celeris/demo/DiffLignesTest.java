package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import jo.celeris.android.host.spi.EtatLignesVcs;
import jo.celeris.android.host.spi.MarqueVcs;
import jo.celeris.android.host.spi.PlageLignes;

import org.junit.jupiter.api.Test;

/**
 * Tests du diff ligne à ligne de la démonstration (D-03) : l'état des
 * lignes du courant face à la référence — préfixe et suffixe communs
 * exclus, le milieu s'aligne ligne à ligne : modifiées à l'alignement,
 * ajoutées au-delà, supprimées par plage.
 */
final class DiffLignesTest {

    /** Les textes égaux ne produisent aucune marque ni suppression. */
    @Test
    void textesEgauxNeProduisentRien() {
        EtatLignesVcs etat = DiffLignes.etat("a\nb\nc", "a\nb\nc");
        assertTrue(etat.marques().isEmpty());
        assertTrue(etat.supprimees().isEmpty());
    }

    /** La ligne modifiée au milieu porte sa marque. */
    @Test
    void ligneModifieeAuMilieu() {
        EtatLignesVcs etat = DiffLignes.etat("a\nb\nc", "a\nB\nc");
        assertEquals(Map.of(2, MarqueVcs.MODIFIE), etat.marques());
        assertTrue(etat.supprimees().isEmpty());
    }

    /** Les lignes ajoutées au milieu sont marquées ajoutées. */
    @Test
    void lignesAjouteesAuMilieu() {
        EtatLignesVcs etat = DiffLignes.etat("a\nc", "a\nb1\nb2\nc");
        assertEquals(Map.of(2, MarqueVcs.AJOUTE, 3, MarqueVcs.AJOUTE), etat.marques());
    }

    /** Les lignes supprimées du milieu rendent leur plage. */
    @Test
    void lignesSupprimeesAuMilieu() {
        EtatLignesVcs etat = DiffLignes.etat("a\nb1\nb2\nc", "a\nc");
        assertTrue(etat.marques().isEmpty());
        assertEquals(List.of(new PlageLignes(2, 3)), etat.supprimees());
    }

    /** Le remplacement aligne les lignes communes puis marque l'excédent. */
    @Test
    void remplacementAlignePuisMarqueExcedent() {
        EtatLignesVcs etat = DiffLignes.etat("a\nb\nc", "a\nX\nY\nc");
        assertEquals(Map.of(2, MarqueVcs.MODIFIE, 3, MarqueVcs.AJOUTE), etat.marques());
    }

    /** Le texte vidé : la première ligne modifiée, les suivantes supprimées. */
    @Test
    void texteVideFaceALignes() {
        EtatLignesVcs etat = DiffLignes.etat("a\nb\nc", "");
        assertEquals(Map.of(1, MarqueVcs.MODIFIE), etat.marques());
        assertEquals(List.of(new PlageLignes(2, 3)), etat.supprimees());
    }
}
