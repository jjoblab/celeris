package jo.celeris.android.host.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests de l'analyseur de piles d'appel (E-06) : chaque cadre « at »
 * rend sa classe, sa méthode, son fichier et sa ligne exacte — le saut
 * lien `fichier:ligne` s'y nourrit.
 */
final class AnalyseurPileTest {

    /** Les cadres se convertissent : fichier et ligne exacts. */
    @Test
    void cadresConvertis() {
        List<CadrePile> cadres = AnalyseurPile.parser("""
                Exception in thread "main" java.lang.IllegalArgumentException: nom requis
                    at com.exemple.app.ServiceUtilisateur.inscrire(ServiceUtilisateur.java:18)
                    at com.exemple.app.Main.main(Main.java:7)""");

        assertEquals(2, cadres.size());
        assertEquals("com.exemple.app.ServiceUtilisateur", cadres.get(0).classe());
        assertEquals("inscrire", cadres.get(0).methode());
        assertEquals("ServiceUtilisateur.java", cadres.get(0).fichier());
        assertEquals(18, cadres.get(0).ligne());
        assertEquals("Main.java", cadres.get(1).fichier());
        assertEquals(7, cadres.get(1).ligne());
    }

    /** Les lignes hors cadre — en-tête, causes, messages — ne produisent rien. */
    @Test
    void horsCadreIgnore() {
        List<CadrePile> cadres = AnalyseurPile.parser("""
                java.lang.IllegalStateException: etat invalide
                    at com.exemple.A.b(A.java:42)
                Caused by: java.lang.NullPointerException: valeur absente
                    at com.exemple.C.d(C.java:9)
                    ... 3 more""");

        assertEquals(2, cadres.size(), "seuls les cadres « at » comptent");
        assertEquals("A.java", cadres.get(0).fichier());
        assertEquals("C.java", cadres.get(1).fichier());
    }

    /** Le cadre sans numéro de ligne — code natif — garde la ligne zéro. */
    @Test
    void cadreSansLigne() {
        List<CadrePile> cadres =
                AnalyseurPile.parser("    at java.lang.ClassLoader.loadClass(ClassLoader.java:-1)");
        assertEquals(1, cadres.size());
        assertEquals("ClassLoader.java", cadres.get(0).fichier());
        assertEquals(0, cadres.get(0).ligne(), "ligne inconnue : zéro, jamais devinée");
    }

    /** L'entrée vide rend vide. */
    @Test
    void entreeVide() {
        assertEquals(0, AnalyseurPile.parser("").size());
    }
}
