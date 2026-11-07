package jo.celeris.android.host.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * L'adaptateur de production du système de fichiers virtuel (E-01) sur
 * un arbre de documents factice — mêmes tests que l'implémentation de
 * référence : ouverture, écriture aux parents implicites, listage
 * stable, sondage, et la traversée refusée à la construction du chemin.
 */
class AdaptateurStockageDocumentsTest {

    /** L'arbre en mémoire : les identifiants sont des compteurs. */
    private static final class ArbreFaux implements ArbreDocuments {
        private final Map<String, List<EntreeDocument>> dossiers = new LinkedHashMap<>();
        private final Map<String, String> fichiers = new LinkedHashMap<>();
        private int compteur;

        ArbreFaux() {
            dossiers.put("racine", new ArrayList<>());
        }

        private String nouvelId() {
            return "doc-" + compteur++;
        }

        void fichier(String idParent, String nom, String contenu) {
            String id = nouvelId();
            fichiers.put(id, contenu);
            dossiers.get(idParent).add(new EntreeDocument(nom, id, false));
        }

        void dossier(String idParent, String nom) {
            String id = nouvelId();
            dossiers.put(id, new ArrayList<>());
            dossiers.get(idParent).add(new EntreeDocument(nom, id, true));
        }

        @Override
        public List<EntreeDocument> enfants(String idDossier) {
            return List.copyOf(dossiers.getOrDefault(idDossier, List.of()));
        }

        @Override
        public String lire(String idFichier) {
            String contenu = fichiers.get(idFichier);
            if (contenu == null) {
                throw new IllegalStateException("not a file: " + idFichier);
            }
            return contenu;
        }

        @Override
        public void remplacer(String idFichier, String contenu) {
            if (!fichiers.containsKey(idFichier)) {
                throw new IllegalStateException("not a file: " + idFichier);
            }
            fichiers.put(idFichier, contenu);
        }

        @Override
        public String creer(String idParent, String nom, boolean dossier) {
            String id = nouvelId();
            if (dossier) {
                this.dossiers.put(id, new ArrayList<>());
                this.dossiers.get(idParent).add(new EntreeDocument(nom, id, true));
            } else {
                fichiers.put(id, "");
                this.dossiers.get(idParent).add(new EntreeDocument(nom, id, false));
            }
            return id;
        }

        @Override
        public boolean estFichier(String idDocument) {
            return fichiers.containsKey(idDocument);
        }
    }

    private AdaptateurStockageDocuments adaptateur(ArbreFaux arbre) {
        return new AdaptateurStockageDocuments(arbre, "racine");
    }

    @Test
    void ouvrirLitLeContenuAUCheminResolu() {
        ArbreFaux arbre = new ArbreFaux();
        arbre.dossier("racine", "src");
        arbre.dossier("doc-0", "main");
        arbre.fichier("doc-1", "App.kt", "class App");
        FichierVirtuel fichier = adaptateur(arbre).ouvrir(CheminDoc.de("/src/main/App.kt"));
        assertEquals("class App", fichier.contenu());
        assertEquals("/src/main/App.kt", fichier.chemin().ecriture());
    }

    @Test
    void ouvrirUnDossierOuUnAbsentLeveFichierAbsent() {
        ArbreFaux arbre = new ArbreFaux();
        arbre.dossier("racine", "src");
        assertThrows(FichierAbsentException.class,
                () -> adaptateur(arbre).ouvrir(CheminDoc.de("/src")));
        assertThrows(FichierAbsentException.class,
                () -> adaptateur(arbre).ouvrir(CheminDoc.de("/inconnu.txt")));
    }

    @Test
    void ecrireRemplaceLeFichierExistant() {
        ArbreFaux arbre = new ArbreFaux();
        arbre.fichier("racine", "notes.md", "un");
        AdaptateurStockageDocuments systeme = adaptateur(arbre);
        systeme.ecrire(CheminDoc.de("/notes.md"), "deux");
        assertEquals("deux", systeme.ouvrir(CheminDoc.de("/notes.md")).contenu());
    }

    @Test
    void ecrireCreeLesParentsImplicites() {
        ArbreFaux arbre = new ArbreFaux();
        AdaptateurStockageDocuments systeme = adaptateur(arbre);
        systeme.ecrire(CheminDoc.de("/a/b/c/fichier.txt"), "contenu");
        assertEquals("contenu", systeme.ouvrir(CheminDoc.de("/a/b/c/fichier.txt")).contenu());
        assertTrue(systeme.estFichier(CheminDoc.de("/a/b/c/fichier.txt")));
        assertFalse(systeme.estFichier(CheminDoc.de("/a/b/c")));
    }

    @Test
    void ecrireSurUnDossierLeveFichierAbsent() {
        ArbreFaux arbre = new ArbreFaux();
        arbre.dossier("racine", "src");
        assertThrows(FichierAbsentException.class, () -> adaptateur(arbre)
                .ecrire(CheminDoc.de("/src"), "contenu"));
    }

    @Test
    void listerRendLesFichiersDansLOrdreLexicographique() {
        ArbreFaux arbre = new ArbreFaux();
        arbre.dossier("racine", "projet");
        arbre.fichier("doc-0", "zeta.txt", "z");
        arbre.dossier("doc-0", "sous-dossier");
        arbre.fichier("doc-0", "alpha.txt", "a");
        arbre.fichier("doc-0", "milieu.txt", "m");
        List<CheminDoc> chemins = adaptateur(arbre).lister(CheminDoc.de("/projet"));
        assertEquals(List.of(
                CheminDoc.de("/projet/alpha.txt"),
                CheminDoc.de("/projet/milieu.txt"),
                CheminDoc.de("/projet/zeta.txt")), chemins);
    }

    @Test
    void listerUnFichierOuUnAbsentRendVide() {
        ArbreFaux arbre = new ArbreFaux();
        arbre.fichier("racine", "f.txt", "x");
        AdaptateurStockageDocuments systeme = adaptateur(arbre);
        assertTrue(systeme.lister(CheminDoc.de("/f.txt")).isEmpty());
        assertTrue(systeme.lister(CheminDoc.de("/absent")).isEmpty());
    }

    @Test
    void laTraverseeNePassePasAussiSurLArbreReel() {
        ArbreFaux arbre = new ArbreFaux();
        arbre.fichier("racine", "secret.txt", "jamais");
        AdaptateurStockageDocuments systeme = adaptateur(arbre);
        assertThrows(IllegalArgumentException.class,
                () -> systeme.ouvrir(CheminDoc.de("/../secret.txt")));
        assertThrows(IllegalArgumentException.class,
                () -> systeme.ecrire(CheminDoc.de("/../../etc/passwd"), "x"));
        assertFalse(systeme.estFichier(CheminDoc.de("/etc/passwd")));
    }

    @Test
    void leMemeContratQueLimplementationDeReference() {
        ArbreFaux arbre = new ArbreFaux();
        AdaptateurStockageDocuments adaptateur = adaptateur(arbre);
        MemoireSystemeFichiers reference = new MemoireSystemeFichiers();
        CheminDoc chemin = CheminDoc.de("/projet/liremoi.md");
        adaptateur.ecrire(chemin, "mêmes règles");
        reference.ecrire(chemin, "mêmes règles");
        assertEquals(reference.ouvrir(chemin).contenu(), adaptateur.ouvrir(chemin).contenu());
        assertEquals(reference.lister(chemin.parent()), adaptateur.lister(chemin.parent()));
        assertEquals(reference.estFichier(chemin), adaptateur.estFichier(chemin));
    }
}
