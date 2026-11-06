package jo.celeris.android.host.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests du système de fichiers virtuel de référence (E-01) :
 * ouverture, écriture, listage — et le refus de la traversée à chaque
 * opération, le chemin étant déjà validé par construction.
 */
final class MemoireSystemeFichiersTest {

    /** Écrire puis ouvrir : le contenu revient intact, le fichier existe. */
    @Test
    void ecrirePuisOuvrir() {
        MemoireSystemeFichiers systeme = new MemoireSystemeFichiers();
        CheminDoc chemin = CheminDoc.de("/projet/src/Main.java");

        systeme.ecrire(chemin, "class Main {}");
        assertTrue(systeme.estFichier(chemin));
        assertEquals("class Main {}", systeme.ouvrir(chemin).contenu());
        assertEquals(chemin, systeme.ouvrir(chemin).chemin());
    }

    /** Ouvrir un fichier absent lève l'exception dédiée, jamais null. */
    @Test
    void ouvrirAbsentLeve() {
        MemoireSystemeFichiers systeme = new MemoireSystemeFichiers();
        assertThrows(FichierAbsentException.class,
                () -> systeme.ouvrir(CheminDoc.de("/absent.txt")));
        assertFalse(systeme.estFichier(CheminDoc.de("/absent.txt")));
    }

    /** Écrire crée les parents implicites ; lister sert le dossier. */
    @Test
    void ecrireCreeLesParentsEtListerSertLeDossier() {
        MemoireSystemeFichiers systeme = new MemoireSystemeFichiers();
        systeme.ecrire(CheminDoc.de("/projet/a.txt"), "un");
        systeme.ecrire(CheminDoc.de("/projet/b.txt"), "deux");
        systeme.ecrire(CheminDoc.de("/projet/src/c.txt"), "trois");

        List<CheminDoc> enfants = systeme.lister(CheminDoc.de("/projet"));
        assertEquals(2, enfants.size(), "a.txt et b.txt ; src n'est pas un fichier");
        assertTrue(enfants.contains(CheminDoc.de("/projet/a.txt")));
        assertTrue(enfants.contains(CheminDoc.de("/projet/b.txt")));
        assertEquals(1, systeme.lister(CheminDoc.de("/projet/src")).size());
        assertEquals(1, systeme.lister(CheminDoc.racine()).size());
    }

    /** Réécrire remplace : une seule version, la dernière. */
    @Test
    void reecrireRemplace() {
        MemoireSystemeFichiers systeme = new MemoireSystemeFichiers();
        CheminDoc chemin = CheminDoc.de("/a.txt");
        systeme.ecrire(chemin, "avant");
        systeme.ecrire(chemin, "apres");
        assertEquals("apres", systeme.ouvrir(chemin).contenu());
    }

    /** La traversée ne s'y glisse pas : le chemin se construit normalisé. */
    @Test
    void cheminDeTraverseeImpossibleAConstruire() {
        MemoireSystemeFichiers systeme = new MemoireSystemeFichiers();
        systeme.ecrire(CheminDoc.de("/projet/a.txt"), "un");

        // le « .. » borné se résout : le même fichier, jamais un autre
        assertTrue(systeme.estFichier(CheminDoc.de("/projet/a.txt/../a.txt")));
        // le dossier n'est pas un fichier
        assertFalse(systeme.estFichier(CheminDoc.de("/projet/a.txt/..")));
        // le « .. » démesuré ne construit même pas de chemin
        assertThrows(IllegalArgumentException.class,
                () -> systeme.estFichier(CheminDoc.de("/projet/../..")));
    }
}
