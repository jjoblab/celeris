package jo.celeris.core.language;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Le registre des langages (core-language) : identification par nom de
 * fichier et par extension — y compris les extensions composées des
 * dialectes de construction (build.gradle.kts contre build.gradle).
 */
class RegistreLangagesTest {

    private final LanguageRegistry registre = LanguageRegistry.of(
            new Language("java", "Java", List.of("java")),
            new Language("kotlin", "Kotlin", List.of("kt")),
            new Language("kts", "Gradle KTS", List.of("gradle.kts", "kts")),
            new Language("groovy", "Groovy", List.of("gradle", "groovy")),
            new Language("xml", "XML", List.of("xml")),
            new Language("json", "JSON", List.of("json")));

    /** Les extensions simples se résolvent. */
    @Test
    void extensionsSimples() {
        assertEquals("java", registre.byFilename("Main.java").id());
        assertEquals("kotlin", registre.byFilename("fenetre.kt").id());
        assertEquals("xml", registre.byFilename("layout.xml").id());
        assertEquals("json", registre.byFilename("conf.json").id());
    }

    /** Les extensions composées préfèrent la plus longue. */
    @Test
    void extensionsComposees() {
        assertEquals("kts", registre.byFilename("build.gradle.kts").id());
        assertEquals("groovy", registre.byFilename("build.gradle").id());
        assertEquals("groovy", registre.byFilename("script.groovy").id());
        assertEquals("kts", registre.byFilename("reglages.kts").id());
    }

    /** Un nom inconnu ou sans extension ne désigne personne. */
    @Test
    void nomsInconnus() {
        assertNull(registre.byFilename("Makefile"));
        assertNull(registre.byFilename("sansextension"));
        assertNull(registre.byFilename("inconnu.xyzzy"));
    }

    /** Le chemin complet se réduit à son nom de fichier. */
    @Test
    void cheminsComplets() {
        assertEquals("java", registre.byFilename("src/main/java/Main.java").id());
        assertEquals("kts", registre.byFilename("projet/app/build.gradle.kts").id());
    }

    /** L'affichage porte un nom lisible. */
    @Test
    void nomAffichable() {
        assertEquals("Gradle KTS", registre.byFilename("build.gradle.kts").displayName());
    }
}
