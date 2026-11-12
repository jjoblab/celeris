package jo.celeris.core.language;

import jo.celeris.core.language.model.LanguageDescriptor;
import jo.celeris.core.language.model.LanguageSupport;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.junit.jupiter.api.Test;

/**
 * Les tests du registre (M7.6) : la précédence de détection — réglage
 * de l'utilisateur, nom exact, motif, extension composée la plus
 * longue, indice de contenu, première ligne — et les garanties de
 * construction : identifiants et noms exacts uniques, choix résolus,
 * motifs compilables.
 */
final class LanguageRegistryTest {

    private static LanguageSupport support(String id, String nomAffichable,
            List<String> extensions, List<String> filenames,
            List<String> patterns, @Nullable String firstLine,
            @Nullable String contentHint) {
        LanguageDescriptor descriptor = new LanguageDescriptor(id, nomAffichable,
                List.of(), extensions, filenames, patterns, firstLine, List.of(),
                null, contentHint, id.substring(0, 1), 0xFF000000);
        return LanguageSupport.builder(descriptor).build();
    }

    private static LanguageRegistry registre(LanguageSupport... supports) {
        LanguageRegistry.Builder batisseur = LanguageRegistry.builder();
        for (LanguageSupport support : supports) {
            batisseur.support(support);
        }
        return batisseur.build();
    }

    private static LanguageSupport resolu(LanguageDetection detection) {
        return assertInstanceOf(LanguageDetection.Resolved.class, detection).support();
    }

    @Test
    void leNomExactGagneSurLextension() {
        LanguageRegistry registre = registre(
                support("dockerfile", "Dockerfile", List.of("dockerfile"),
                        List.of("dockerfile"), List.of(), null, null),
                support("texte", "Texte", List.of("txt"), List.of(), List.of(),
                        null, null));
        assertEquals("dockerfile",
                resolu(registre.detect("dockerfile", null, null)).id());
    }

    @Test
    void leMotifGagneSurLextensionComposee() {
        LanguageRegistry registre = registre(
                support("kts", "KTS", List.of("kts"), List.of(),
                        List.of("*.gradle.kts"), null, null),
                support("groovy", "Groovy", List.of("groovy", "gradle"), List.of(),
                        List.of(), null, null));
        assertEquals("kts", resolu(registre.detect("build.gradle.kts", null, null)).id());
        assertEquals("groovy", resolu(registre.detect("build.gradle", null, null)).id());
        assertEquals("kts", resolu(registre.detect("settings.gradle.kts", null, null)).id());
    }

    @Test
    void lextensionComposeeLaPlusLongueGagne() {
        LanguageRegistry registre = registre(
                support("kts", "KTS", List.of("kts"), List.of(), List.of(),
                        null, null),
                support("groovy", "Groovy", List.of("gradle", "gradle.kts"), List.of(),
                        List.of(), null, null));
        assertEquals("groovy",
                resolu(registre.detect("build.gradle.kts", null, null)).id());
    }

    @Test
    void leReglageDeLutilisateurBattTout() {
        LanguageSupport dockerfile = support("dockerfile", "Dockerfile",
                List.of("dockerfile"), List.of("dockerfile"), List.of(), null, null);
        LanguageRegistry registre = LanguageRegistry.builder()
                .support(dockerfile)
                .support(support("texte", "Texte", List.of("txt"), List.of(),
                        List.of(), null, null))
                .userChoice("dockerfile", "texte")
                .build();
        assertEquals("texte",
                resolu(registre.detect("dockerfile", null, null)).id());
    }

    @Test
    void lindiceDeContenuDepartageLesEgalites() {
        LanguageRegistry registre = registre(
                support("c", "C", List.of("h"), List.of(), List.of(), null, null),
                support("cpp", "C++", List.of("h", "cpp"), List.of(), List.of(),
                        null, "template\\s*<|namespace\\s+\\w+|std::"));
        assertEquals("cpp", resolu(registre.detect("entete.h", null,
                "namespace outils { class A {}; }")).id());
        assertEquals("c", resolu(registre.detect("entete.h", null,
                "struct maillon { int valeur; };")).id());
    }

    @Test
    void leSansIndiceEstLeDefautDeSonExtension() {
        LanguageRegistry registre = registre(
                support("c", "C", List.of("h"), List.of(), List.of(), null, null),
                support("cpp", "C++", List.of("h", "cpp"), List.of(), List.of(),
                        null, "template\\s*<"));
        assertEquals("c", resolu(registre.detect("entete.h", null, null)).id());
        assertEquals("c", resolu(registre.detect("entete.h", null,
                "struct maillon { int valeur; };")).id());
    }

    @Test
    void deuxCandidatsSansIndiceRestentAmbigus() {
        LanguageRegistry registre = registre(
                support("c", "C", List.of("h"), List.of(), List.of(), null,
                        "#include\\s*<stdio\\.h>"),
                support("cpp", "C++", List.of("h", "cpp"), List.of(), List.of(),
                        null, "template\\s*<"));
        LanguageDetection detection = registre.detect("entete.h", null, null);
        LanguageDetection.Ambiguous ambigue =
                assertInstanceOf(LanguageDetection.Ambiguous.class, detection);
        assertEquals(2, ambigue.candidates().size());
    }

    @Test
    void laPremiereLigneSertSansExtension() {
        LanguageRegistry registre = registre(
                support("python", "Python", List.of("py"), List.of(), List.of(),
                        "^#!.*python", null),
                support("shell", "Shell", List.of("sh"), List.of(), List.of(),
                        "^#!.*(ba|z)?sh", null));
        assertEquals("python", resolu(registre.detect("script", "#!/usr/bin/python", null)).id());
        assertEquals("shell", resolu(registre.detect("script", "#!/bin/bash", null)).id());
        assertInstanceOf(LanguageDetection.Unresolved.class,
                registre.detect("script", "bonjour", null));
    }

    @Test
    void lextensionBattLaPremiereLigne() {
        LanguageRegistry registre = registre(
                support("python", "Python", List.of("py"), List.of(), List.of(),
                        "^#!.*(ba|z)?sh", null),
                support("shell", "Shell", List.of("sh"), List.of(), List.of(),
                        "^#!.*(ba|z)?sh", null));
        assertEquals("python", resolu(registre.detect("outil.py", "#!/bin/sh", null)).id());
    }

    @Test
    void leCheminCompletSeRameneAuNom() {
        LanguageRegistry registre = registre(
                support("java", "Java", List.of("java"), List.of(), List.of(),
                        null, null));
        assertEquals("java",
                resolu(registre.detect("/home/z/Service.java", null, null)).id());
        assertEquals("java",
                resolu(registre.detect("C:\\sources\\Service.java", null, null)).id());
    }

    @Test
    void sansLangageLaDetectionEstSansObjet() {
        LanguageRegistry registre = registre(
                support("java", "Java", List.of("java"), List.of(), List.of(),
                        null, null));
        assertInstanceOf(LanguageDetection.Unresolved.class,
                registre.detect("notes.txt", null, null));
        assertNull(registre.byFilename("notes.txt"));
        assertEquals("java", resolu(registre.detect("A.java", null, null)).id());
    }

    @Test
    void deuxLangagesNeRevendiquentJamaisLeMemeNomExact() {
        LanguageSupport premier = support("premier", "Premier", List.of(),
                List.of("Dockerfile"), List.of(), null, null);
        LanguageSupport second = support("second", "Second", List.of(),
                List.of("dockerfile"), List.of(), null, null);
        IllegalArgumentException echec = assertThrows(IllegalArgumentException.class,
                () -> registre(premier, second));
        assertTrue(String.valueOf(echec.getMessage()).contains("dockerfile"));
    }

    @Test
    void deuxIdentifiantsIdentiquesSontRefuses() {
        LanguageSupport premier = support("java", "Java", List.of("java"),
                List.of(), List.of(), null, null);
        LanguageSupport second = support("java", "Java encore", List.of("j"),
                List.of(), List.of(), null, null);
        IllegalArgumentException echec = assertThrows(IllegalArgumentException.class,
                () -> registre(premier, second));
        assertTrue(String.valueOf(echec.getMessage()).contains("duplicate language id"));
    }

    @Test
    void unChoixDutilisateurInconnuEstRefuseALaConstruction() {
        IllegalArgumentException echec = assertThrows(IllegalArgumentException.class,
                () -> LanguageRegistry.builder()
                        .support(support("java", "Java", List.of("java"), List.of(),
                                List.of(), null, null))
                        .userChoice("bizzare.txt", "inconnu")
                        .build());
        assertTrue(String.valueOf(echec.getMessage()).contains("unknown language"));
    }

    @Test
    void uneRegexInvalideDuDescripteurEstRefuseeAvecPrecision() {
        IllegalArgumentException echec = assertThrows(IllegalArgumentException.class,
                () -> registre(support("piege", "Piège", List.of("x"), List.of(),
                        List.of(), "(?P<nom", null)));
        assertTrue(String.valueOf(echec.getMessage()).contains("piege"));
        assertTrue(String.valueOf(echec.getMessage()).contains("firstLine"));
    }

    @Test
    void unMotifDeNomAuJokerInterroge() {
        LanguageRegistry registre = registre(
                support("makefile", "Makefile", List.of("mk"), List.of(),
                        List.of("Makefile.???"), null, null));
        assertEquals("makefile",
                resolu(registre.detect("Makefile.abc", null, null)).id());
        assertInstanceOf(LanguageDetection.Unresolved.class,
                registre.detect("Makefile.trop-long", null, null));
    }

    @Test
    void lOrdreDesSupportsResteCeluiDeLinscription() {
        LanguageRegistry registre = registre(
                support("java", "Java", List.of("java"), List.of(), List.of(),
                        null, null),
                support("kotlin", "Kotlin", List.of("kt"), List.of(), List.of(),
                        null, null));
        List<String> ids = new ArrayList<>();
        registre.supports().forEach(support -> ids.add(support.id()));
        assertEquals(List.of("java", "kotlin"), ids);
        assertEquals("kotlin", resolu(registre.detect("A.kt", null, null)).id());
    }
}
