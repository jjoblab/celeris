package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests des cartes de fichiers de l'accueil : chaque fichier du
 * catalogue doit porter un badge lisible (monogramme, couleur) et un
 * sous-titre honnête (langage, compte de lignes) — la carte est la
 * promesse de ce que l'éditeur ouvrira.
 */
final class CarteFichierTest {

    @Test
    void leCatalogueCouvreExactementLesFichiersDExemple() {
        assertEquals(FichiersExemple.catalogue().size(),
                CarteFichier.catalogue().size(),
                "une carte par fichier d'exemple, ni plus ni moins");
    }

    @Test
    void chaqueCartePorteUnBadgeComplet() {
        for (CarteFichier carte : CarteFichier.catalogue()) {
            assertFalse(carte.monogramme().isBlank(),
                    carte.nom() + " : monogramme vide");
            assertTrue(carte.monogramme().length() <= 2,
                    carte.nom() + " : monogramme trop long pour le badge");
            assertNotEquals(0, carte.couleur(),
                    carte.nom() + " : couleur de badge absente");
            assertFalse(carte.langage().isBlank(),
                    carte.nom() + " : langage affiché vide");
            assertTrue(carte.lignes() > 0,
                    carte.nom() + " : compte de lignes invivable");
        }
    }

    @Test
    void lesLignesSeComptentSansLaLigneVideFinale() {
        FichierExemple fichier = new FichierExemple("Essai.kt", "une\ndeux\n");
        assertEquals(2, CarteFichier.depuis(fichier).lignes(),
                "le saut final ne compte pas pour une ligne");
    }

    @Test
    void leJavaSeReconnait() {
        CarteFichier carte = CarteFichier.depuis(
                new FichierExemple("ServiceUtilisateur.java", "class A {}\n"));
        assertEquals("Java", carte.langage());
        assertEquals("Jv", carte.monogramme());
    }

    @Test
    void extensionInconnueCarteEnTexteSimple() {
        CarteFichier carte = CarteFichier.depuis(new FichierExemple("notes", "une ligne"));
        assertEquals("Text", carte.langage());
        assertEquals("Tx", carte.monogramme());
        assertEquals(1, carte.lignes());
    }

    @Test
    void lesNomsRestentCeuxDuCatalogue() {
        List<String> attendus = FichiersExemple.catalogue().stream()
                .map(FichierExemple::nom).toList();
        assertEquals(attendus, CarteFichier.catalogue().stream()
                .map(CarteFichier::nom).toList());
    }
}
