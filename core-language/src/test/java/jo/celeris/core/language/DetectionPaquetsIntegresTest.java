package jo.celeris.core.language;

import jo.celeris.core.language.model.LanguageDescriptor;
import jo.celeris.core.language.model.LanguageSupport;
import jo.celeris.core.language.packs.GrammarFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Le tableau de détection des paquets intégrés (M7.6) : plus de trente
 * cas couvrant chaque rang de la précédence — réglage de l'utilisateur,
 * nom exact, motif de nom, extension composée la plus longue, indice de
 * contenu (le {@code .h} de C ou C++), première ligne. Un rang sans
 * langage intégré (le nom exact) se couvre par un support de banc
 * ajouté au registre intégré — la règle se prouve sur les mêmes
 * données plus une.
 */
final class DetectionPaquetsIntegresTest {

    /** La fabrique de banc : la couleur exacte n'importe pas ici. */
    private static final GrammarFactory FABRIQUE =
            (id, json) -> jo.celeris.core.syntax.clike.JavaSyntax.INSTANCE;

    private static LanguageRegistry registre() {
        LanguageSupport makefile = LanguageSupport.builder(new LanguageDescriptor(
                "makefile", "Makefile", List.of(), List.of("mk"),
                List.of("Makefile"), List.of(), null, List.of(), null, null,
                "Mk", 0xFF000000)).build();
        return LanguagePlatform.with(FABRIQUE)
                .builtinPacks()
                .support(makefile)
                .build();
    }

    private static String idDe(LanguageDetection detection) {
        return assertInstanceOf(LanguageDetection.Resolved.class, detection)
                .support().id();
    }

    @Test
    void leTableauDesExtensions() {
        LanguageRegistry registre = registre();
        assertEquals("java", idDe(registre.detect("Service.java", null, null)));
        assertEquals("java", idDe(registre.detect("/chemin/Service.java", null, null)));
        assertEquals("kotlin", idDe(registre.detect("Conversions.kt", null, null)));
        assertEquals("kts", idDe(registre.detect("settings.kts", null, null)));
        assertEquals("groovy", idDe(registre.detect("build.gradle", null, null)));
        assertEquals("groovy", idDe(registre.detect("script.groovy", null, null)));
        assertEquals("xml", idDe(registre.detect("strings.xml", null, null)));
        assertEquals("json", idDe(registre.detect("catalogue.json", null, null)));
        assertEquals("toml", idDe(registre.detect("libs.versions.toml", null, null)));
        assertEquals("yaml", idDe(registre.detect("ci.yaml", null, null)));
        assertEquals("yaml", idDe(registre.detect("ci.yml", null, null)));
        assertEquals("properties", idDe(registre.detect("gradle.properties", null, null)));
        assertEquals("markdown", idDe(registre.detect("README.md", null, null)));
        assertEquals("markdown", idDe(registre.detect("NOTES.markdown", null, null)));
        assertEquals("python", idDe(registre.detect("outils.py", null, null)));
        assertEquals("python", idDe(registre.detect("script.pyw", null, null)));
        assertEquals("shell", idDe(registre.detect("install.sh", null, null)));
        assertEquals("shell", idDe(registre.detect("run.bash", null, null)));
        assertEquals("shell", idDe(registre.detect("run.zsh", null, null)));
        assertEquals("sql", idDe(registre.detect("migration.sql", null, null)));
        assertEquals("c", idDe(registre.detect("main.c", null, null)));
        assertEquals("cpp", idDe(registre.detect("engine.cpp", null, null)));
        assertEquals("cpp", idDe(registre.detect("engine.cc", null, null)));
        assertEquals("cpp", idDe(registre.detect("engine.cxx", null, null)));
        assertEquals("cpp", idDe(registre.detect("head.hpp", null, null)));
        assertEquals("cpp", idDe(registre.detect("head.hxx", null, null)));
        assertEquals("proguard", idDe(registre.detect("rules.pro", null, null)));
    }

    @Test
    void leMotifDeNomGagneSurLextension() {
        LanguageRegistry registre = registre();
        assertEquals("kts", idDe(registre.detect("build.gradle.kts", null, null)));
        assertEquals("kts", idDe(registre.detect("app.build.gradle.kts", null, null)));
    }

    @Test
    void leNomExactGagneSurToutSaufLeReglage() {
        LanguageRegistry registre = registre();
        assertEquals("makefile", idDe(registre.detect("Makefile", null, null)));
        assertEquals("makefile", idDe(registre.detect("makefile", null, null)));
    }

    @Test
    void leReglageDeLutilisateurBattLeNomExact() {
        LanguageRegistry registre = LanguagePlatform.with(FABRIQUE)
                .builtinPacks()
                .support(LanguageSupport.builder(new LanguageDescriptor(
                        "makefile", "Makefile", List.of(), List.of("mk"),
                        List.of("Makefile"), List.of(), null, List.of(), null,
                        null, "Mk", 0xFF000000)).build())
                .userChoice("Makefile", "python")
                .build();
        assertEquals("python", idDe(registre.detect("Makefile", null, null)));
    }

    @Test
    void lextensionBattLaPremiereLigne() {
        LanguageRegistry registre = registre();
        assertEquals("python", idDe(registre.detect("outils.py", "#!/bin/bash", null)));
    }

    @Test
    void laPremiereLigneSertSansExtensionConnue() {
        LanguageRegistry registre = registre();
        assertEquals("python", idDe(registre.detect("script", "#!/usr/bin/env python", null)));
        assertEquals("python", idDe(registre.detect("outil", "#!/usr/bin/python3", null)));
        assertEquals("shell", idDe(registre.detect("script", "#!/bin/bash", null)));
        assertEquals("shell", idDe(registre.detect("script", "#!/usr/bin/env zsh", null)));
    }

    @Test
    void lindiceDeContenuDepartageLePointH() {
        LanguageRegistry registre = registre();
        assertEquals("cpp", idDe(registre.detect("entete.h", null,
                "namespace outils { template<typename T> struct A {}; };")));
        assertEquals("cpp", idDe(registre.detect("entete.h", null,
                "#include <vector>\nusing namespace std;")));
        assertEquals("c", idDe(registre.detect("entete.h", null,
                "#include <stdio.h>\nstruct maillon { int valeur; };")));
        assertEquals("c", idDe(registre.detect("entete.h", null, null)));
    }

    @Test
    void sansLangageNiLigneLaDetectionEstSansObjet() {
        LanguageRegistry registre = registre();
        assertInstanceOf(LanguageDetection.Unresolved.class,
                registre.detect("notes", "bonjour", null));
        assertInstanceOf(LanguageDetection.Unresolved.class,
                registre.detect("fichier.xyz", null, null));
        assertNull(registre.byFilename("fichier.xyz"));
    }

    @Test
    void leDegenerateNeSeDetectePasPlusQueSaFamille() {
        LanguageRegistry registre = registre();
        LanguageDetection detection = registre.detect("archive.tar.gz", null, null);
        assertTrue(detection instanceof LanguageDetection.Unresolved,
                "aucun langage ne revendique .gz");
    }
}
