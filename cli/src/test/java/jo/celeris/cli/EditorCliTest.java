package jo.celeris.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * L'éditeur en ligne de commande (M7) : analyser un fichier — langage
 * détecté, lignes, plan — et chercher un motif avec lignes et colonnes
 * exactes, sans Android (le critère de sortie « editor-cli exécuté en
 * CI »).
 */
public final class EditorCliTest {

    @TempDir
    Path dossier;

    private Path ecrire(String nom, String contenu) throws IOException {
        Path fichier = dossier.resolve(nom);
        Files.writeString(fichier, contenu, StandardCharsets.UTF_8);
        return fichier;
    }

    private static String sortieDe(String[] args) {
        ByteArrayOutputStream tampon = new ByteArrayOutputStream();
        int code = Celeris.executer(args,
                new PrintStream(tampon, true, StandardCharsets.UTF_8));
        assertEquals(0, code, "la commande doit reussir");
        return tampon.toString(StandardCharsets.UTF_8);
    }

    /** L'analyse d'un fichier Java rend le langage, les lignes et le plan. */
    @Test
    public void analyseUnFichierJava() throws IOException {
        Path fichier = ecrire("Service.java", """
                package com.exemple;

                public final class Service {

                    int compteur;
                }
                """);
        String sortie = sortieDe(new String[] {"analyse", fichier.toString()});

        assertTrue(sortie.contains("langage : java"), "langage détecté : " + sortie);
        assertTrue(sortie.contains("lignes : 6"), "six lignes : " + sortie);
        assertTrue(sortie.contains("Service"), "le plan cite la classe : " + sortie);
        assertTrue(sortie.contains("ligne 3"), "la classe vit ligne 3 : " + sortie);
    }

    /** L'analyse d'un fichier inconnu reste honnête : aucun langage, aucun plan. */
    @Test
    public void analyseUnFichierSansLangage() throws IOException {
        Path fichier = ecrire("notes.txt", "des notes libres\nsur deux lignes\n");
        String sortie = sortieDe(new String[] {"analyse", fichier.toString()});

        assertTrue(sortie.contains("langage : aucun"), "aucun langage forcé : " + sortie);
        assertTrue(sortie.contains("lignes : 2"), "deux lignes : " + sortie);
    }

    /** L'analyse traverse la voie de grammaires : le TOML est reconnu. */
    @Test
    public void analyseUnFichierToml() throws IOException {
        Path fichier = ecrire("libs.versions.toml", "[versions]\nagp = \"9.5.0\"\n");
        String sortie = sortieDe(new String[] {"analyse", fichier.toString()});

        assertTrue(sortie.contains("langage : toml"), "grammaire toml : " + sortie);
    }

    /** Le point H se départage par son contenu : les marqueurs C++
     * emportent le C++, leur absence laisse le C. */
    @Test
    public void analyseUnFichierDEnteteC() throws IOException {
        Path c = ecrire("entete.h", "#include <stdio.h>\nstruct maillon { int v; };\n");
        assertTrue(sortieDe(new String[] {"analyse", c.toString()})
                .contains("langage : c"), "le C sans marque C++ : ");

        Path cpp = ecrire("entete.hpp", "namespace o { template<typename T> struct A {}; }\n");
        assertTrue(sortieDe(new String[] {"analyse", cpp.toString()})
                .contains("langage : cpp"), "le C++ aux marqueurs : ");
    }

    /** Un gros fichier garde sa détection : l'échantillon se borne,
     * le nom suffit et la première ligne passe. */
    @Test
    public void analyseUnGrosFichier() throws IOException {
        StringBuilder gros = new StringBuilder("#!/bin/bash\n");
        for (int i = 0; i < 9000; i++) {
            gros.append("echo ligne ").append(i).append('\n');
        }
        Path fichier = ecrire("script-sans-extension", gros.toString());
        String sortie = sortieDe(new String[] {"analyse", fichier.toString()});
        assertTrue(sortie.contains("langage : shell"), "le shebang parle : " + sortie);
    }

    /** La recherche rend les occurrences avec lignes et colonnes exactes. */
    @Test
    public void rechercheLesOccurrences() throws IOException {
        Path fichier = ecrire("texte.txt", "bonjour\nle monde\nbonjour encore\n");
        String sortie = sortieDe(
                new String[] {"recherche", fichier.toString(), "bonjour"});

        assertTrue(sortie.contains("2 occurrence(s)"), "deux occurrences : " + sortie);
        assertTrue(sortie.contains("1:1"), "première occurrence ligne 1 colonne 1 : "
                + sortie);
        assertTrue(sortie.contains("3:1"), "seconde occurrence ligne 3 colonne 1 : "
                + sortie);
    }

    /** Un fichier illisible rend un code d'erreur et un message, pas une trace. */
    @Test
    public void fichierAbsentRefuse() {
        ByteArrayOutputStream tampon = new ByteArrayOutputStream();
        int code = Celeris.executer(
                new String[] {"analyse", "/chemin/absent.java"},
                new PrintStream(tampon, true, StandardCharsets.UTF_8));
        assertEquals(1, code, "code d'erreur de lecture");
        String sortie = tampon.toString(StandardCharsets.UTF_8);
        assertTrue(sortie.contains("illisible"), "message explicite : " + sortie);
    }

    /** Une commande inconnue rend l'usage et un code d'erreur distinct. */
    @Test
    public void commandeInconnueRendUsage() {
        ByteArrayOutputStream tampon = new ByteArrayOutputStream();
        int code = Celeris.executer(new String[] {"farfelu"},
                new PrintStream(tampon, true, StandardCharsets.UTF_8));
        assertEquals(2, code, "code d'erreur d'usage");
        String sortie = tampon.toString(StandardCharsets.UTF_8);
        assertTrue(sortie.contains("usage"), "l'usage s'affiche : " + sortie);
        assertTrue(sortie.contains("analyse"), "l'usage cite analyse : " + sortie);
    }
}
