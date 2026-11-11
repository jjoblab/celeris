package jo.celeris.android.host.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

/**
 * L'historique local par fichier (A-14) : chaque écriture archive le
 * contenu remplacé, chaque fichier garde ses révisions pour soi, la
 * restauration réécrit le passé et s'enregistre à son tour — le
 * critère de la matrice : restaurer une version antérieure,
 * l'isolement par fichier.
 */
final class HistoriqueLocalTest {

    private final AtomicLong horloge = new AtomicLong();

    private HistoriqueLocal historique() {
        return new HistoriqueLocal(new MemoireSystemeFichiers(), horloge::incrementAndGet);
    }

    /** A-14 — l'écriture archive le contenu remplacé, l'écriture sans changement n'archive rien. */
    @Test
    void lecritureArchiveLeRemplace() {
        HistoriqueLocal historique = historique();
        CheminDoc chemin = CheminDoc.de("/projet/src/Main.java");

        historique.ecrire(chemin, "class Main {}");
        assertTrue(historique.revisions(chemin).isEmpty(),
                "la création n'a pas de passé à archiver");

        historique.ecrire(chemin, "class Main { int x; }");
        historique.ecrire(chemin, "class Main { int x; int y; }");
        List<RevisionLocale> revisions = historique.revisions(chemin);
        assertEquals(2, revisions.size(), "deux contenus remplacés archivés");
        assertEquals("class Main {}", revisions.get(0).contenu());
        assertEquals("class Main { int x; }", revisions.get(1).contenu());
        assertEquals(1, revisions.get(0).horodatage());
        assertEquals(2, revisions.get(1).horodatage());

        historique.ecrire(chemin, "class Main { int x; int y; }");
        assertEquals(2, historique.revisions(chemin).size(),
                "l'écriture sans changement n'archive rien");
    }

    /** A-14 — l'isolement par fichier : l'histoire d'un fichier ne fuit pas dans un autre. */
    @Test
    void lisolementParFichier() {
        HistoriqueLocal historique = historique();
        CheminDoc main = CheminDoc.de("/projet/src/Main.java");
        CheminDoc test = CheminDoc.de("/projet/src/MainTest.java");

        historique.ecrire(main, "class Main {}");
        historique.ecrire(test, "class MainTest {}");
        historique.ecrire(main, "class Main { int x; }");
        historique.ecrire(test, "class MainTest { int y; }");

        List<RevisionLocale> revisionsMain = historique.revisions(main);
        assertEquals(1, revisionsMain.size(), "seul Main.java s'y écrit");
        assertEquals("class Main {}", revisionsMain.get(0).contenu());
        assertEquals(main, revisionsMain.get(0).chemin());

        List<RevisionLocale> revisionsTest = historique.revisions(test);
        assertEquals(1, revisionsTest.size(), "seul MainTest.java s'y écrit");
        assertEquals("class MainTest {}", revisionsTest.get(0).contenu());
    }

    /** A-14 — restaurer une version antérieure : le contenu revient, la restauration s'archive. */
    @Test
    void restaurerUneVersionAnterieure() {
        HistoriqueLocal historique = historique();
        CheminDoc chemin = CheminDoc.de("/projet/src/Main.java");

        historique.ecrire(chemin, "class Main {}");
        historique.ecrire(chemin, "class Main { int x; }");
        historique.ecrire(chemin, "class Main { int x; int y; }");

        historique.restaurer(chemin, historique.revisions(chemin).get(0));
        assertEquals("class Main {}", historique.ouvrir(chemin).contenu(),
                "la version antérieure revient");

        List<RevisionLocale> revisions = historique.revisions(chemin);
        assertEquals(3, revisions.size(), "la restauration archive le contenu remplacé");
        assertEquals("class Main { int x; int y; }", revisions.get(2).contenu());

        historique.restaurer(chemin, revisions.get(2));
        assertEquals("class Main { int x; int y; }", historique.ouvrir(chemin).contenu(),
                "la restauration se défait comme elle s'est faite");
    }

    /** A-14 — la révision d'un autre fichier refuse de se restaurer, le passé d'autrui n'est pas le sien. */
    @Test
    void laRevisionEtrangereRefuse() {
        HistoriqueLocal historique = historique();
        CheminDoc main = CheminDoc.de("/projet/src/Main.java");
        CheminDoc test = CheminDoc.de("/projet/src/MainTest.java");

        historique.ecrire(main, "class Main {}");
        historique.ecrire(test, "class MainTest {}");
        historique.ecrire(main, "class Main { int x; }");
        historique.ecrire(test, "class MainTest { int y; }");

        RevisionLocale revisionTest = historique.revisions(test).get(0);
        assertThrows(IllegalArgumentException.class,
                () -> historique.restaurer(main, revisionTest),
                "la révision de MainTest ne se restaure pas sur Main");
        RevisionLocale forgee = new RevisionLocale(main, "class Forgeree {}", 99);
        assertThrows(IllegalArgumentException.class,
                () -> historique.restaurer(main, forgee),
                "la révision forgée refuse");
        assertEquals("class Main { int x; }", historique.ouvrir(main).contenu(),
                "rien n'a changé");
    }

    /** A-14 — le fichier sans passé n'a pas de révisions ; la délégation sert le système enveloppé. */
    @Test
    void lePasseVideEtLaDelegation() {
        HistoriqueLocal historique = historique();
        CheminDoc chemin = CheminDoc.de("/projet/a.txt");
        CheminDoc dossier = CheminDoc.de("/projet");

        assertTrue(historique.revisions(chemin).isEmpty(),
                "le fichier absent n'a pas d'histoire");
        historique.ecrire(chemin, "un");
        assertTrue(historique.estFichier(chemin), "la délégation répond");
        assertEquals(List.of(chemin), historique.lister(dossier),
                "le listage délègue");
        assertEquals("un", historique.ouvrir(chemin).contenu(),
                "l'ouverture délègue");
    }
}
