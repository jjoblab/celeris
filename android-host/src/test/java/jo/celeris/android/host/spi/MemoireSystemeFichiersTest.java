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
        InMemoryFileSystem systeme = new InMemoryFileSystem();
        CheminDoc chemin = CheminDoc.of("/projet/src/Main.java");

        systeme.write(chemin, "class Main {}");
        assertTrue(systeme.estFichier(chemin));
        assertEquals("class Main {}", systeme.open(chemin).contenu());
        assertEquals(chemin, systeme.open(chemin).chemin());
    }

    /** Ouvrir un fichier absent lève l'exception dédiée, jamais null. */
    @Test
    void ouvrirAbsentLeve() {
        InMemoryFileSystem systeme = new InMemoryFileSystem();
        assertThrows(FichierAbsentException.class,
                () -> systeme.open(CheminDoc.of("/absent.txt")));
        assertFalse(systeme.estFichier(CheminDoc.of("/absent.txt")));
    }

    /** Écrire crée les parents implicites ; lister sert le dossier. */
    @Test
    void ecrireCreeLesParentsEtListerSertLeDossier() {
        InMemoryFileSystem systeme = new InMemoryFileSystem();
        systeme.write(CheminDoc.of("/projet/a.txt"), "un");
        systeme.write(CheminDoc.of("/projet/b.txt"), "deux");
        systeme.write(CheminDoc.of("/projet/src/c.txt"), "trois");

        List<CheminDoc> enfants = systeme.lister(CheminDoc.of("/projet"));
        assertEquals(2, enfants.size(), "a.txt et b.txt ; src n'est pas un fichier");
        assertTrue(enfants.contains(CheminDoc.of("/projet/a.txt")));
        assertTrue(enfants.contains(CheminDoc.of("/projet/b.txt")));
        assertEquals(1, systeme.lister(CheminDoc.of("/projet/src")).size());
        assertEquals(0, systeme.lister(CheminDoc.racine()).size(),
                "la racine ne contient aucun fichier direct : « projet » est un dossier");
    }

    /** Réécrire remplace : une seule version, la dernière. */
    @Test
    void reecrireRemplace() {
        InMemoryFileSystem systeme = new InMemoryFileSystem();
        CheminDoc chemin = CheminDoc.of("/a.txt");
        systeme.write(chemin, "avant");
        systeme.write(chemin, "apres");
        assertEquals("apres", systeme.open(chemin).contenu());
    }

    /** La traversée ne s'y glisse pas : le chemin se construit normalisé. */
    @Test
    void cheminDeTraverseeImpossibleAConstruire() {
        InMemoryFileSystem systeme = new InMemoryFileSystem();
        systeme.write(CheminDoc.of("/projet/a.txt"), "un");

        // le « .. » borné se résout : le même fichier, jamais un autre
        assertTrue(systeme.estFichier(CheminDoc.of("/projet/a.txt/../a.txt")));
        // le dossier n'est pas un fichier
        assertFalse(systeme.estFichier(CheminDoc.of("/projet/a.txt/..")));
        // le « .. » démesuré ne construit même pas de chemin
        assertThrows(IllegalArgumentException.class,
                () -> systeme.estFichier(CheminDoc.of("/projet/../..")));
    }
}
