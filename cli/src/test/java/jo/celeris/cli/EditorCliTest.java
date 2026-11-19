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
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int code = Celeris.executer(args,
                new PrintStream(buffer, true, StandardCharsets.UTF_8));
        assertEquals(0, code, "la commande doit reussir");
        return buffer.toString(StandardCharsets.UTF_8);
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
        String output = sortieDe(new String[] {"analyse", fichier.toString()});

        assertTrue(output.contains("langage : java"), "langage détecté : " + output);
        assertTrue(output.contains("lignes : 6"), "six lignes : " + output);
        assertTrue(output.contains("Service"), "le plan cite la classe : " + output);
        assertTrue(output.contains("ligne 3"), "la classe vit ligne 3 : " + output);
    }

    /** L'analyse d'un fichier inconnu reste honnête : aucun langage, aucun plan. */
    @Test
    public void analyseUnFichierSansLangage() throws IOException {
        Path fichier = ecrire("notes.banc", "des notes libres\nsur deux lignes\n");
        String output = sortieDe(new String[] {"analyse", fichier.toString()});

        assertTrue(output.contains("langage : aucun"), "aucun langage forcé : " + output);
        assertTrue(output.contains("lignes : 2"), "deux lignes : " + output);
    }

    /**
     * La traîne achève le tour : le texte brut vit en paquet — l'analyse
     * le nomme honnête, aucun plan, aucune coloration.
     */
    @Test
    public void analyseUnFichierTexteBrut() throws IOException {
        Path fichier = ecrire("notes.txt", "des notes libres\nsur deux lignes\n");
        String output = sortieDe(new String[] {"analyse", fichier.toString()});

        assertTrue(output.contains("langage : text"), "le texte brut est nommé : " + output);
        assertTrue(output.contains("lignes : 2"), "deux lignes : " + output);
    }

    /** L'analyse traverse la voie de grammaires : le TOML est reconnu. */
    @Test
    public void analyseUnFichierToml() throws IOException {
        Path fichier = ecrire("libs.versions.toml", "[versions]\nagp = \"9.5.0\"\n");
        String output = sortieDe(new String[] {"analyse", fichier.toString()});

        assertTrue(output.contains("langage : toml"), "grammaire toml : " + output);
    }

    /** Le Python se plie à l'indentation et se planifie par les règles de son paquet (H-08). */
    @Test
    public void analyseUnFichierPythonPlisEtPlan() throws IOException {
        Path fichier = ecrire("outils.py", """
                CATEGORIES = {
                    "code": 0,
                }

                def classifier(ligne):
                    if not ligne.strip():
                        return "vide"
                    return "code"
                """);
        String output = sortieDe(new String[] {"analyse", fichier.toString()});

        assertTrue(output.contains("langage : python"), "langage détecté : " + output);
        assertTrue(output.contains("plan : CATEGORIES"), "la constante figure : " + output);
        assertTrue(output.contains("plan : classifier"),
                "la définition figure au plan : " + output);
        assertTrue(output.contains("pli : lignes 5-8"),
                "la définition se replie : " + output);
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
        String output = sortieDe(new String[] {"analyse", fichier.toString()});
        assertTrue(output.contains("langage : shell"), "le shebang parle : " + output);
    }

    /** La recherche rend les occurrences avec lignes et colonnes exactes. */
    @Test
    public void rechercheLesOccurrences() throws IOException {
        Path fichier = ecrire("texte.txt", "bonjour\nle monde\nbonjour encore\n");
        String output = sortieDe(
                new String[] {"recherche", fichier.toString(), "bonjour"});

        assertTrue(output.contains("2 occurrence(s)"), "deux occurrences : " + output);
        assertTrue(output.contains("1:1"), "première occurrence ligne 1 colonne 1 : "
                + output);
        assertTrue(output.contains("3:1"), "seconde occurrence ligne 3 colonne 1 : "
                + output);
    }

    /** Le motif vide se refuse : la boucle d'occurrences ne tournerait
     * jamais — le code d'usage revient et le motif se nomme. */
    @Test
    public void motifVideRefuse() throws IOException {
        Path fichier = ecrire("texte.txt", "bonjour\n");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int code = Celeris.executer(
                new String[] {"recherche", fichier.toString(), ""},
                new PrintStream(buffer, true, StandardCharsets.UTF_8));
        assertEquals(2, code, "code d'erreur d'usage");
        String output = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("le motif de recherche ne peut pas être vide"),
                "le refus se dit : " + output);
    }

    /** Un fichier illisible rend un code d'erreur et un message, pas une trace. */
    @Test
    public void fichierAbsentRefuse() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int code = Celeris.executer(
                new String[] {"analyse", "/chemin/absent.java"},
                new PrintStream(buffer, true, StandardCharsets.UTF_8));
        assertEquals(1, code, "code d'erreur de lecture");
        String output = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("illisible"), "message explicite : " + output);
    }

    /** Une commande inconnue rend l'usage et un code d'erreur distinct. */
    @Test
    public void commandeInconnueRendUsage() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int code = Celeris.executer(new String[] {"farfelu"},
                new PrintStream(buffer, true, StandardCharsets.UTF_8));
        assertEquals(2, code, "code d'erreur d'usage");
        String output = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("usage"), "l'usage s'affiche : " + output);
        assertTrue(output.contains("analyse"), "l'usage cite analyse : " + output);
    }
}
