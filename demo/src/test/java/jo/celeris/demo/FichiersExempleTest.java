package jo.celeris.demo;

import jo.celeris.core.language.model.LanguageSupport;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;



import org.junit.jupiter.api.Test;

/**
 * Tests du catalogue de fichiers d'exemple : la démonstration ouvre de
 * vrais fichiers de chaque langage P0 de la matrice (C-14, C-15, C-16),
 * et le langage de chacun se détecte par le registre de la plateforme
 * (M7.6) — l'écran d'éditeur ne doit jamais se retrouver sans matière
 * à tester.
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
            assertNotNull(LangagesDemo.langageDe(fichier.nom(), null,
                    fichier.contenu()),
                    fichier.nom() + " : aucun langage détecté");
        }
    }

    @Test
    void langageDetecteParLeRegistre() {
        assertEquals("kotlin", langageDe("Calculateur.kt"));
        assertEquals("java", langageDe("Service.java"));
        assertEquals("xml", langageDe("strings.xml"));
        assertEquals("kts", langageDe("build.gradle.kts"));
        assertEquals("groovy", langageDe("build.gradle"));
        assertEquals("toml", langageDe("libs.versions.toml"));
        assertEquals("json", langageDe("donnees.json"));
        assertEquals("markdown", langageDe("README.md"));
        assertEquals("python", langageDe("outils.py"));
        assertEquals("yaml", langageDe("dependances.yml"));
        assertEquals("sql", langageDe("inventaire.sql"));
        assertEquals("proguard", langageDe("regles.pro"));
    }

    /** L'identifiant du langage détecté d'un nom de fichier du banc. */
    private static String langageDe(String nom) {
        LanguageSupport support = LangagesDemo.langageDe(nom, null, null);
        return support == null ? "" : support.id();
    }

    @Test
    void extensionInconnueSansLangue() {
        assertNull(LangagesDemo.langageDe("inconnu.xyz", null, null));
        assertNull(LangagesDemo.langageDe("sansextension", null, null));
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
