package jo.celeris.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;

import jo.celeris.core.syntax.LanguageSyntax;

import org.junit.jupiter.api.Test;

/**
 * Tests du catalogue de fichiers d'exemple : la démonstration ouvre de
 * vrais fichiers de chaque langage P0 de la matrice (C-14, C-15, C-16),
 * avec l'analyseur dédié ou la grammaire déclarative qui va avec —
 * l'écran d'éditeur ne doit jamais se retrouver sans matière à tester.
 */
final class FichiersExempleTest {

    @Test
    void catalogueCompletNomsUniques() {
        List<FichierExemple> catalogue = FichiersExemple.catalogue();
        assertTrue(catalogue.size() >= 6,
                "au moins les six langages P0 : Kotlin, Java, XML, KTS, TOML, JSON");
        assertEquals(catalogue.size(), catalogue.stream()
                .map(FichierExemple::nom).distinct().count(), "noms uniques");
    }

    @Test
    void chaqueFichierPorteUnContenuNonVideEtUneLangue() {
        for (FichierExemple fichier : FichiersExemple.catalogue()) {
            assertFalse(fichier.nom().isBlank(), "nom non vide");
            assertTrue(fichier.contenu().length() > 40,
                    fichier.nom() + " : contenu trop court pour tester l'éditeur");
            assertNotNull(FichiersExemple.languePour(fichier.nom()),
                    fichier.nom() + " : aucun analyseur associé");
        }
    }

    @Test
    void langageDetecteParExtension() {
        assertEquals("kotlin", FichiersExemple.languePour("Calculateur.kt").id());
        assertEquals("java", FichiersExemple.languePour("Service.java").id());
        assertEquals("xml", FichiersExemple.languePour("strings.xml").id());
        assertEquals("kts", FichiersExemple.languePour("build.gradle.kts").id());
        assertEquals("toml", FichiersExemple.languePour("libs.versions.toml").id());
        assertEquals("json", FichiersExemple.languePour("donnees.json").id());
    }

    @Test
    void extensionInconnueSansLangue() {
        assertNull(FichiersExemple.languePour("inconnu.xyz"));
        assertNull(FichiersExemple.languePour("sansextension"));
    }

    @Test
    void leCatalogueCouvreChaqueExtensionAnnoncee() {
        List<String> extensions = FichiersExemple.catalogue().stream()
                .map(FichierExemple::nom)
                .map(nom -> nom.substring(nom.lastIndexOf('.') + 1)
                        .toLowerCase(Locale.ROOT))
                .toList();
        for (String attendue : List.of("kt", "java", "xml", "kts", "toml", "json")) {
            assertTrue(extensions.contains(attendue),
                    "extension absente du catalogue : " + attendue);
        }
    }
}
