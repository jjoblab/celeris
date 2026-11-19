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
 * La chaîne outillée de la cli (G-03) : diagnostiquer (les plages
 * malformées, ligne et colonne), formater (le texte formaté seul sur la
 * sortie) et refactorer renommer (le fichier renommé, les conflits
 * signalés sans être tranchés) — la sortie est déterministe, les codes
 * de sortie disent le verdict.
 */
public final class CelerisOutilsTest {

    @TempDir
    Path dossier;

    private Path ecrire(String nom, String contenu) throws IOException {
        Path fichier = dossier.resolve(nom);
        Files.writeString(fichier, contenu, StandardCharsets.UTF_8);
        return fichier;
    }

    private static String sortieDe(int codeAttendu, String[] args) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int code = Celeris.executer(args,
                new PrintStream(buffer, true, StandardCharsets.UTF_8));
        assertEquals(codeAttendu, code, "code de sortie inattendu");
        return buffer.toString(StandardCharsets.UTF_8);
    }

    /** G-03 — diagnostiquer : les plages malformées se disent, ligne et colonne. */
    @Test
    public void diagnostiqueUnFichierMalforme() throws IOException {
        Path fichier = ecrire("Cassee.java", """
                class Cassee {
                    void f( {
                    }
                }
                """);
        String output = sortieDe(3,
                new String[] {"diagnostiquer", fichier.toString()});
        assertTrue(output.contains("malformed structure"),
                "la malformation se dit : " + output);
        assertTrue(output.contains("1:1 "),
                "la plage malformée vit ligne 1 : " + output);
        assertTrue(output.trim().endsWith("1 diagnostic(s)"),
                "le décompte clôt : " + output);
    }

    /** G-03 — diagnostiquer un fichier sain : aucun diagnostic, code 0. */
    @Test
    public void diagnostiqueUnFichierSain() throws IOException {
        Path fichier = ecrire("Sain.java", """
                class Sain {
                    void f() {
                    }
                }
                """);
        String output = sortieDe(0,
                new String[] {"diagnostiquer", fichier.toString()});
        assertTrue(output.trim().endsWith("0 diagnostic(s)"),
                "aucun diagnostic : " + output);
    }

    /** G-03 — formater : le texte formaté paraît seul sur la sortie. */
    @Test
    public void formateUnFichierJava() throws IOException {
        Path fichier = ecrire("Banc.java", """
                class Banc {
                void f() {
                int x = 1;
                    }
                }
                """);
        String output = sortieDe(0, new String[] {"formater", fichier.toString()});
        assertEquals("""
                class Banc {
                    void f() {
                        int x = 1;
                    }
                }
                """, output, "le texte formaté, seul sur la sortie");
    }

    /** G-03 — formater un fichier sans langage analysable refuse : code 1. */
    @Test
    public void formateUnFichierSansLangageRefuse() throws IOException {
        Path fichier = ecrire("inconnu.quelconque", "n'importe quoi");
        String output = sortieDe(1, new String[] {"formater", fichier.toString()});
        assertTrue(output.contains("langage sans analyse"),
                "le refus se dit : " + output);
    }

    /** G-03 — refactorer renommer : le fichier renommé paraît, chaque occurrence suit. */
    @Test
    public void refactoreUnRenommage() throws IOException {
        Path fichier = ecrire("Achat.java", """
                class Achat {
                    int quantite;

                    int total() {
                        return quantite * 2;
                    }
                }
                """);
        String output = sortieDe(0, new String[] {"refactorer",
                fichier.toString(), "renommer", "quantite", "nombre"});
        assertEquals("""
                class Achat {
                    int nombre;

                    int total() {
                        return nombre * 2;
                    }
                }
                """, output, "le fichier renommé, seul sur la sortie");
    }

    /** G-03 — le renommage ambigu refuse : deux déclarations du même nom, code 4. */
    @Test
    public void leRenommageAmbiguRefuse() throws IOException {
        Path fichier = ecrire("Double.java", """
                class Double {
                    int homonyme;

                    int homonyme() {
                        return 1;
                    }
                }
                """);
        String output = sortieDe(4, new String[] {"refactorer",
                fichier.toString(), "renommer", "homonyme", "autre"});
        assertTrue(output.contains("conflit : homonyme AMBIGUITE"),
                "l'ambiguïté se dit : " + output);
    }

    /** G-03 — le renommage en collision refuse : le nouveau nom déjà pris, code 4. */
    @Test
    public void leRenommageEnCollisionRefuse() throws IOException {
        Path fichier = ecrire("Troc.java", """
                class Troc {
                    int prix;
                    int seuil;

                    int total() {
                        return prix + seuil;
                    }
                }
                """);
        String output = sortieDe(4, new String[] {"refactorer",
                fichier.toString(), "renommer", "prix", "seuil"});
        assertTrue(output.contains("conflit : seuil COLLISION"),
                "la collision se dit : " + output);
    }

    /** G-03 — l'usage incomplet rend l'usage, code 2 ; le sous-commande inconnue aussi. */
    @Test
    public void lUsageIncompletRendUsage() throws IOException {
        String output = sortieDe(2, new String[] {"refactorer", "a.java",
                "bouger", "x", "y"});
        assertTrue(output.startsWith("usage :"),
                "l'usage se rend : " + output);
        assertTrue(output.contains("diagnostiquer <fichier>"),
                "l'usage cite les nouvelles commandes : " + output);
    }
}
